begin;
insert into auth.users(id,email,raw_user_meta_data) values
 ('11111111-1111-4111-8111-111111111111','plans-user@example.invalid','{}'),
 ('22222222-2222-4222-8222-222222222222','plans-other@example.invalid','{}'),
 ('33333333-3333-4333-8333-333333333333','plans-admin@example.invalid','{}');
update public.app_users set role='admin',status='approved' where id='33333333-3333-4333-8333-333333333333';
set local role authenticated;
select set_config('request.jwt.claim.sub','11111111-1111-4111-8111-111111111111',true);
select public.request_access('trial_7');
do $$ begin
 if public.has_active_access() then raise exception 'Request improperly grants access'; end if;
 begin perform public.admin_set_plan('paid_7',99,true); raise exception 'FAIL: user changed price'; exception when raise_exception then if SQLERRM like 'FAIL:%' then raise; end if; end;
 begin update public.app_users set access_until=now()+interval '1 year'; raise exception 'FAIL: user changed expiry'; exception when insufficient_privilege then null; end;
end; $$;
select set_config('request.jwt.claim.sub','33333333-3333-4333-8333-333333333333',true);
select public.admin_grant_access('11111111-1111-4111-8111-111111111111','trial_7','');
do $$ begin
 if (select access_until from public.app_users where id='11111111-1111-4111-8111-111111111111')<>now()+interval '7 days' then raise exception 'Trial must start at approval'; end if;
 begin perform public.admin_grant_access('11111111-1111-4111-8111-111111111111','trial_7','');raise exception 'FAIL: repeated trial';exception when raise_exception then if SQLERRM like 'FAIL:%' then raise;end if;end;
 begin perform public.admin_grant_access('11111111-1111-4111-8111-111111111111','paid_7','pay1');raise exception 'FAIL: unset price accepted';exception when raise_exception then if SQLERRM like 'FAIL:%' then raise;end if;end;
end; $$;
select public.admin_set_plan('paid_7',99,true);
select public.admin_set_plan('paid_30',299,true);
select public.admin_grant_access('11111111-1111-4111-8111-111111111111','paid_7','pay1');
select public.admin_grant_access('11111111-1111-4111-8111-111111111111','paid_30','pay2');
do $$ begin
 if (select access_until from public.app_users where id='11111111-1111-4111-8111-111111111111')<>now()+interval '44 days' then raise exception 'Renewal did not preserve remaining days';end if;
 begin perform public.admin_grant_access('11111111-1111-4111-8111-111111111111','paid_7','pay1');raise exception 'FAIL: duplicate payment';exception when unique_violation then null;end;
 begin perform public.admin_grant_access('11111111-1111-4111-8111-111111111111','paid_7','');raise exception 'FAIL: missing payment accepted';exception when raise_exception then if SQLERRM like 'FAIL:%' then raise;end if;end;
end; $$;
select set_config('request.jwt.claim.sub','11111111-1111-4111-8111-111111111111',true);
insert into public.trade_history(symbol,side,trade_date,quantity,price,fees) values('TCS','BUY',current_date-1,10,100,10);
do $$ begin
 if not public.has_active_access() then raise exception 'Approved user lacks access';end if;
 if (select count(*) from public.trade_history)<>1 then raise exception 'Own trade not readable';end if;
 begin insert into public.trade_history(user_id,symbol,side,trade_date,quantity,price) values('22222222-2222-4222-8222-222222222222','TCS','BUY',current_date-1,1,1);raise exception 'FAIL: cross-user insert';exception when insufficient_privilege then null;end;
end; $$;
update public.trade_history set notes='edited';
select set_config('request.jwt.claim.sub','22222222-2222-4222-8222-222222222222',true);
do $$ declare changed int;begin
 if (select count(*) from public.trade_history)<>0 then raise exception 'Cross-user data leak';end if;
 update public.trade_history set notes='hacked';get diagnostics changed=row_count;if changed<>0 then raise exception 'Cross-user edit';end if;
end; $$;
select set_config('request.jwt.claim.sub','33333333-3333-4333-8333-333333333333',true);
do $$ begin
 if (select count(*) from public.trade_history)<>1 then raise exception 'Admin cannot review trades';end if;
 if (select count(*) from public.trade_audit where user_id='11111111-1111-4111-8111-111111111111')<>2 then raise exception 'Trade audit missing';end if;
end; $$;
reset role;
update public.app_users set access_until=now() where id='11111111-1111-4111-8111-111111111111';
set local role authenticated;
select set_config('request.jwt.claim.sub','11111111-1111-4111-8111-111111111111',true);
do $$ begin
 if public.has_active_access() then raise exception 'Expiry boundary grants access';end if;
 if (select count(*) from public.trade_history)<>0 then raise exception 'Expired user reads protected trades';end if;
 begin insert into public.trade_history(symbol,side,trade_date,quantity,price) values('TCS','BUY',current_date-1,1,1);raise exception 'FAIL: expired user wrote trade';exception when insufficient_privilege then null;end;
end; $$;
reset role;
rollback;
select 'Plan, payment, expiry, history privacy and audit tests passed; all fixtures rolled back' as result;
