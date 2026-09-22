-- Preserve the inspected legacy email-only profiles in a private schema.
-- Refuse automatic conversion if verified Auth accounts already exist.
begin;
do $$
begin
 if to_regclass('public.app_users') is not null then
  if exists(select 1 from auth.users) then
   raise exception 'Existing Auth users require an explicit identity migration';
  end if;
  lock table public.app_users in access exclusive mode;
  create schema if not exists app_private;
  revoke all on schema app_private from public, anon, authenticated;
  revoke all on table public.app_users from public, anon, authenticated;
  alter table public.app_users set schema app_private;
 end if;
end;
$$;
create table public.app_users (
 id uuid primary key references auth.users(id) on delete cascade,
 full_name text not null, mobile_number text not null default '', email text not null,
 role text not null default 'user' check (role in ('user','admin')),
 status text not null default 'pending' check (status in ('pending','approved','rejected')),
 approved_by text, approved_at timestamptz, created_at timestamptz not null default now()
);
alter table public.app_users enable row level security;
revoke all on public.app_users from anon, authenticated;
grant select on public.app_users to authenticated;
grant update (status, approved_by, approved_at) on public.app_users to authenticated;
create function public.is_app_admin() returns boolean
language sql stable security definer set search_path = '' as $$
 select exists(select 1 from public.app_users where id=auth.uid() and role='admin' and status='approved');
$$;
revoke all on function public.is_app_admin() from public;
grant execute on function public.is_app_admin() to authenticated;
create policy own_profile_or_admin on public.app_users for select to authenticated
 using(id=auth.uid() or public.is_app_admin());
create policy admin_approval on public.app_users for update to authenticated
 using(public.is_app_admin() and role <> 'admin') with check(public.is_app_admin() and role <> 'admin');
create function public.create_app_profile() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
 insert into public.app_users(id,full_name,mobile_number,email)
 values(new.id,coalesce(new.raw_user_meta_data->>'full_name',''),coalesce(new.raw_user_meta_data->>'mobile_number',''),coalesce(new.email,''));
 return new;
end;
$$;
revoke all on function public.create_app_profile() from public;
create trigger create_app_profile after insert on auth.users for each row execute function public.create_app_profile();
commit;
