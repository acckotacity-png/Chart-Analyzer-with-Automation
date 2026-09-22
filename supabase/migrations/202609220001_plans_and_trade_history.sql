begin;
create table public.access_plans (
 code text primary key check(code in ('trial_7','paid_7','paid_30')),
 name text not null, days integer not null check(days in (7,30)),
 price_inr numeric(12,2) check(price_inr >= 0), enabled boolean not null default false,
 check ((code='trial_7' and days=7 and price_inr=0) or (code='paid_7' and days=7) or (code='paid_30' and days=30)),
 check (not enabled or price_inr is not null)
);
insert into public.access_plans values ('trial_7','7-day free trial',7,0,true),('paid_7','7 days',7,null,false),('paid_30','30 days (1 month)',30,null,false);
alter table public.app_users add column access_until timestamptz, add column plan_code text references public.access_plans(code), add column trial_used boolean not null default false;
-- All access changes now go through audited, admin-only functions.
revoke update(status,approved_by,approved_at) on public.app_users from authenticated;
drop policy admin_approval on public.app_users;
create function public.has_active_access() returns boolean language sql stable security definer set search_path='' as $$
 select exists(select 1 from public.app_users where id=auth.uid() and status='approved' and (role='admin' or access_until>now()));
$$;
revoke all on function public.has_active_access() from public;
grant execute on function public.has_active_access() to authenticated;
create table public.access_requests (
 id uuid primary key default gen_random_uuid(), user_id uuid not null references public.app_users(id),
 plan_code text not null references public.access_plans(code), status text not null default 'pending' check(status in ('pending','approved','rejected')),
 created_at timestamptz not null default now(), resolved_at timestamptz
);
create unique index one_pending_access_request on public.access_requests(user_id) where status='pending';
create table public.access_events (
 id uuid primary key default gen_random_uuid(), user_id uuid not null references public.app_users(id),
 admin_id uuid not null references public.app_users(id), action text not null check(action in ('grant','revoke')),
 plan_code text references public.access_plans(code), price_inr numeric(12,2), starts_at timestamptz, ends_at timestamptz,
 payment_reference text not null default '', created_at timestamptz not null default now()
);
create unique index one_paid_payment_reference on public.access_events(user_id,payment_reference) where action='grant' and plan_code<>'trial_7';
create table public.trade_history (
 id uuid primary key default gen_random_uuid(), user_id uuid not null default auth.uid() references public.app_users(id),
 symbol text not null check(symbol ~ '^[A-Z0-9&._-]{1,30}$'), exchange text not null default 'NSE' check(exchange in ('NSE','BSE')),
 side text not null check(side in ('BUY','SELL')), trade_date date not null,
 quantity numeric(18,6) not null check(quantity>0 and quantity<=1000000000),
 price numeric(18,6) not null check(price>0 and price<=1000000000),
 fees numeric(18,2) not null default 0 check(fees>=0 and fees<=1000000000),
 notes text not null default '' check(length(notes)<=2000), created_at timestamptz not null default now(), updated_at timestamptz not null default now()
);
create index trade_history_user_date on public.trade_history(user_id,trade_date,id);
create table public.trade_audit (
 id bigint generated always as identity primary key, trade_id uuid not null, user_id uuid not null,
 actor_id uuid, action text not null, old_record jsonb, new_record jsonb, created_at timestamptz not null default now()
);
create function public.audit_trade_change() returns trigger language plpgsql security definer set search_path='' as $$
begin
 if TG_OP <> 'DELETE' then
  if new.trade_date > (now() at time zone 'Asia/Kolkata')::date then raise exception 'Trade date cannot be in the future'; end if;
  new.updated_at=now();
 end if;
 insert into public.trade_audit(trade_id,user_id,actor_id,action,old_record,new_record)
 values(coalesce(new.id,old.id),coalesce(new.user_id,old.user_id),auth.uid(),TG_OP,
 case when TG_OP <> 'INSERT' then to_jsonb(old) end,case when TG_OP <> 'DELETE' then to_jsonb(new) end);
 if TG_OP='DELETE' then return old; end if;
 return new;
end;
$$;
revoke all on function public.audit_trade_change() from public;
create trigger trade_change before insert or update or delete on public.trade_history for each row execute function public.audit_trade_change();

alter table public.access_plans enable row level security;
alter table public.access_requests enable row level security;
alter table public.access_events enable row level security;
alter table public.trade_history enable row level security;
alter table public.trade_audit enable row level security;
revoke all on public.access_plans,public.access_requests,public.access_events,public.trade_history,public.trade_audit from anon,authenticated;
grant select on public.access_plans,public.access_requests,public.access_events,public.trade_history,public.trade_audit to authenticated;
grant insert(user_id,symbol,exchange,side,trade_date,quantity,price,fees,notes) on public.trade_history to authenticated;
grant update(symbol,exchange,side,trade_date,quantity,price,fees,notes) on public.trade_history to authenticated;
grant delete on public.trade_history to authenticated;
create policy plan_read on public.access_plans for select to authenticated using(true);
create policy request_read on public.access_requests for select to authenticated using(user_id=auth.uid() or public.is_app_admin());
create policy event_read on public.access_events for select to authenticated using(user_id=auth.uid() or public.is_app_admin());
create policy trade_read on public.trade_history for select to authenticated using((user_id=auth.uid() and public.has_active_access()) or public.is_app_admin());
create policy trade_insert on public.trade_history for insert to authenticated with check(user_id=auth.uid() and public.has_active_access());
create policy trade_update on public.trade_history for update to authenticated using(user_id=auth.uid() and public.has_active_access()) with check(user_id=auth.uid() and public.has_active_access());
create policy trade_delete on public.trade_history for delete to authenticated using(user_id=auth.uid() and public.has_active_access());
create policy audit_read on public.trade_audit for select to authenticated using((user_id=auth.uid() and public.has_active_access()) or public.is_app_admin());

create function public.admin_set_plan(p_code text,p_price numeric,p_enabled boolean) returns void language plpgsql security definer set search_path='' as $$
begin
 if not public.is_app_admin() then raise exception 'Admin access required'; end if;
 if p_code not in ('paid_7','paid_30') or p_price is null or p_price<0 or p_price>9999999999 then raise exception 'Invalid plan price'; end if;
 update public.access_plans set price_inr=p_price,enabled=p_enabled where code=p_code;
end;
$$;
create function public.request_access(p_plan text) returns void language plpgsql security definer set search_path='' as $$
begin
 if auth.uid() is null then raise exception 'Sign in required'; end if;
 if not exists(select 1 from public.access_plans where code=p_plan and enabled and price_inr is not null) then raise exception 'Plan is unavailable'; end if;
 if p_plan='trial_7' and exists(select 1 from public.app_users where id=auth.uid() and trial_used) then raise exception 'Free trial already used'; end if;
 insert into public.access_requests(user_id,plan_code) values(auth.uid(),p_plan)
 on conflict(user_id) where status='pending' do update set plan_code=excluded.plan_code,created_at=now();
end;
$$;
create function public.admin_grant_access(p_user uuid,p_plan text,p_reference text default '') returns timestamptz language plpgsql security definer set search_path='' as $$
declare target public.app_users; plan public.access_plans; start_time timestamptz; end_time timestamptz;
begin
 if not public.is_app_admin() then raise exception 'Admin access required'; end if;
 select * into target from public.app_users where id=p_user for update;
 if not found or target.role='admin' then raise exception 'Invalid target user'; end if;
 select * into plan from public.access_plans where code=p_plan and enabled for share;
 if not found or plan.price_inr is null then raise exception 'Set and enable plan price first'; end if;
 if p_plan='trial_7' and target.trial_used then raise exception 'Free trial already used'; end if;
 if p_plan='trial_7' and target.access_until>now() then raise exception 'Existing active access cannot be replaced by a trial'; end if;
 if p_plan<>'trial_7' and length(trim(coalesce(p_reference,'')))=0 then raise exception 'Record confirmed payment/reference before paid activation'; end if;
 if length(p_reference)>500 then raise exception 'Reference too long'; end if;
 start_time=case when p_plan='trial_7' or target.status<>'approved' then now() else greatest(now(),coalesce(target.access_until,now())) end;
 end_time=start_time+make_interval(days=>plan.days);
 update public.app_users set status='approved',plan_code=p_plan,access_until=end_time,trial_used=trial_used or p_plan='trial_7',approved_by=(select email from public.app_users where id=auth.uid()),approved_at=now() where id=p_user;
 update public.access_requests set status='approved',resolved_at=now() where user_id=p_user and status='pending';
 insert into public.access_events(user_id,admin_id,action,plan_code,price_inr,starts_at,ends_at,payment_reference)
 values(p_user,auth.uid(),'grant',p_plan,plan.price_inr,start_time,end_time,trim(coalesce(p_reference,'')));
 return end_time;
end;
$$;
create function public.admin_revoke_access(p_user uuid) returns void language plpgsql security definer set search_path='' as $$
begin
 if not public.is_app_admin() then raise exception 'Admin access required'; end if;
 update public.app_users set status='rejected',access_until=now() where id=p_user and role<>'admin';
 if not found then raise exception 'Invalid target user'; end if;
 update public.access_requests set status='rejected',resolved_at=now() where user_id=p_user and status='pending';
 insert into public.access_events(user_id,admin_id,action) values(p_user,auth.uid(),'revoke');
end;
$$;
revoke all on function public.admin_set_plan(text,numeric,boolean),public.request_access(text),public.admin_grant_access(uuid,text,text),public.admin_revoke_access(uuid) from public;
grant execute on function public.admin_set_plan(text,numeric,boolean),public.request_access(text),public.admin_grant_access(uuid,text,text),public.admin_revoke_access(uuid) to authenticated;
commit;
