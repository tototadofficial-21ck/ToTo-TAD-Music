begin;
alter table public.profiles
  add column avatar_path text,
  add column profile_completed boolean not null default false,
  add constraint profile_name_length check (char_length(display_name) <= 32),
  add constraint profile_avatar_owned check (avatar_path is null or avatar_path = user_id::text || '/avatar.jpg');
-- Column grants prevent changing identity, creation date or access rights.
grant update(display_name, avatar_path, profile_completed) on public.profiles to authenticated;
create policy profile_update_own_authorized on public.profiles for update to authenticated
using (user_id = (select auth.uid()) and exists (
 select 1 from public.app_access a where a.user_id = (select auth.uid()) and a.enabled
)) with check (user_id = (select auth.uid()) and exists (
 select 1 from public.app_access a where a.user_id = (select auth.uid()) and a.enabled
));
insert into storage.buckets(id, name, public, file_size_limit, allowed_mime_types)
values ('toto-avatars', 'toto-avatars', false, 2097152, array['image/jpeg'])
on conflict (id) do update set public=false, file_size_limit=2097152, allowed_mime_types=array['image/jpeg'];
create policy toto_avatar_select on storage.objects for select to authenticated
using (bucket_id='toto-avatars' and name=(select auth.uid())::text || '/avatar.jpg'
 and exists(select 1 from public.app_access a where a.user_id=(select auth.uid()) and a.enabled));
create policy toto_avatar_insert on storage.objects for insert to authenticated
with check (bucket_id='toto-avatars' and name=(select auth.uid())::text || '/avatar.jpg'
 and exists(select 1 from public.app_access a where a.user_id=(select auth.uid()) and a.enabled));
create policy toto_avatar_update on storage.objects for update to authenticated
using (bucket_id='toto-avatars' and name=(select auth.uid())::text || '/avatar.jpg'
 and exists(select 1 from public.app_access a where a.user_id=(select auth.uid()) and a.enabled))
with check (bucket_id='toto-avatars' and name=(select auth.uid())::text || '/avatar.jpg'
 and exists(select 1 from public.app_access a where a.user_id=(select auth.uid()) and a.enabled));
commit;
