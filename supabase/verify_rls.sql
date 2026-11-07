-- Run ONLY in a test project after migration. Replace both UUID placeholders with two
-- different Auth users created in Dashboard. Everything below rolls back.
begin;
insert into public.app_access(user_id, enabled) values
 ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', true),
 ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', true)
on conflict (user_id) do update set enabled = true;
set local role authenticated;
select set_config('request.jwt.claim.sub', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', true);
do $$ begin
 if (select count(*) from public.app_access) <> 1 then raise exception 'A access RLS failed'; end if;
 if (select count(*) from public.profiles) <> 1 then raise exception 'A profile RLS failed'; end if;
 if exists (select 1 from public.profiles where user_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb') then raise exception 'A reads B'; end if;
 if has_table_privilege(current_user, 'public.app_access', 'UPDATE') or
    has_table_privilege(current_user, 'public.app_access', 'INSERT') or
    has_table_privilege(current_user, 'public.app_access', 'DELETE') or
    has_table_privilege(current_user, 'public.profiles', 'UPDATE') then raise exception 'unexpected write GRANT'; end if;
 begin
  update public.app_access set enabled = true where user_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
  raise exception 'write unexpectedly succeeded';
 exception when insufficient_privilege then null;
 end;
end $$;
select set_config('request.jwt.claim.sub', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', true);
do $$ begin
 if (select count(*) from public.app_access) <> 1 then raise exception 'B access RLS failed'; end if;
 if (select count(*) from public.profiles) <> 1 then raise exception 'B profile RLS failed'; end if;
 if exists (select 1 from public.app_access where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa') then raise exception 'B reads A'; end if;
 begin
  update public.profiles set display_name = 'intruder' where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
  raise exception 'profile write unexpectedly succeeded';
 exception when insufficient_privilege then null;
 end;
end $$;
reset role;
update public.app_access set enabled = false where user_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
set local role authenticated;
do $$ begin
 if exists (select 1 from public.profiles) then raise exception 'blocked user reads profiles'; end if;
 if (select enabled from public.app_access) is distinct from false then raise exception 'blocked user access check failed'; end if;
end $$;
reset role;
set local role anon;
do $$ begin
 if has_table_privilege(current_user, 'public.app_access', 'SELECT') or
    has_table_privilege(current_user, 'public.profiles', 'SELECT') then raise exception 'anon GRANT'; end if;
end $$;
rollback;
