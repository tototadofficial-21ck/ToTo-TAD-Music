-- Isolated test fixture UUIDs only; all changes rolled back.
begin;
update public.app_access set enabled=true;
set local role authenticated;
select set_config('request.jwt.claim.sub','aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',true);
do $$ declare n int; begin
 update public.profiles set display_name='Account A', avatar_path='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/avatar.jpg', profile_completed=true
 where user_id=auth.uid();
 get diagnostics n=row_count;
 if n<>1 then raise exception 'own profile update denied'; end if;
 update public.profiles set display_name='intruder' where user_id='bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
 get diagnostics n=row_count;
 if n<>0 then raise exception 'cross account update'; end if;
 begin
  update public.profiles set created_at=now(); raise exception 'created_at writable';
 exception when insufficient_privilege then null; end;
 begin
  update public.profiles set user_id='bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'; raise exception 'identity writable';
 exception when insufficient_privilege then null; end;
 begin
  update public.profiles set avatar_path='bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb/avatar.jpg'; raise exception 'foreign avatar accepted';
 exception when check_violation then null; end;
 begin
  insert into public.app_access(user_id,enabled) values(auth.uid(),true); raise exception 'access writable';
 exception when insufficient_privilege then null; end;
 insert into storage.objects(bucket_id,name) values('toto-avatars',auth.uid()::text||'/avatar.jpg');
 begin
  insert into storage.objects(bucket_id,name) values('toto-avatars','bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb/avatar.jpg');
  raise exception 'foreign upload accepted';
 exception when insufficient_privilege then null; end;
end $$;
select set_config('request.jwt.claim.sub','bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',true);
do $$ declare n int; begin
 if exists(select 1 from public.profiles where display_name='Account A') then raise exception 'B reads A'; end if;
 if exists(select 1 from storage.objects) then raise exception 'B reads A avatar'; end if;
 update storage.objects set name=auth.uid()::text||'/avatar.jpg'; get diagnostics n=row_count;
 if n<>0 then raise exception 'B changes A avatar'; end if;
 update public.profiles set display_name='Account B' where user_id=auth.uid();
 get diagnostics n=row_count; if n<>1 then raise exception 'B update denied'; end if;
 insert into storage.objects(bucket_id,name) values('toto-avatars',auth.uid()::text||'/avatar.jpg');
 update storage.objects set name=name where bucket_id='toto-avatars';
 get diagnostics n=row_count; if n<>1 then raise exception 'B updates wrong avatars'; end if;
end $$;
reset role;
update public.app_access set enabled=false where user_id='bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb';
set local role authenticated;
do $$ declare n int; begin
 if exists(select 1 from public.profiles) or exists(select 1 from storage.objects) then raise exception 'locked read'; end if;
 update public.profiles set display_name='blocked'; get diagnostics n=row_count;
 if n<>0 then raise exception 'locked write'; end if;
 begin
  insert into storage.objects(bucket_id,name) values('toto-avatars',auth.uid()::text||'/other.jpg'); raise exception 'locked upload';
 exception when insufficient_privilege then null; end;
end $$;
reset role;
rollback;
