begin;
insert into auth.users(id,email,raw_user_meta_data) values
('11111111-1111-4111-8111-111111111111','rls-user@example.invalid','{}'),
('22222222-2222-4222-8222-222222222222','rls-other@example.invalid','{}'),
('33333333-3333-4333-8333-333333333333','rls-admin@example.invalid','{}');
update public.app_users set role='admin',status='approved' where id='33333333-3333-4333-8333-333333333333';
set local role authenticated;
select set_config('request.jwt.claim.sub','11111111-1111-4111-8111-111111111111',true);
do $$
declare changed integer;
begin
 if (select count(*) from public.app_users) <> 1 then raise exception 'User can read another profile'; end if;
 begin
  update public.app_users set status='approved';
  raise exception 'User can self-approve';
 exception when insufficient_privilege then null;
 end;
 begin
  update public.app_users set role='admin';
  raise exception 'User can update roles';
 exception when insufficient_privilege then null;
 end;
 begin
  perform count(*) from app_private.app_users;
  raise exception 'User can read private archive';
 exception when insufficient_privilege then null;
 end;
end;
$$;
select set_config('request.jwt.claim.sub','33333333-3333-4333-8333-333333333333',true);
do $$
declare changed integer;
begin
 perform public.admin_grant_access('11111111-1111-4111-8111-111111111111','trial_7','');
 if not exists(select 1 from public.app_users where id='11111111-1111-4111-8111-111111111111' and access_until>now()) then raise exception 'Admin cannot approve a user'; end if;
end;
$$;
reset role;
rollback;
select 'RLS checks passed; test accounts rolled back' as result;
