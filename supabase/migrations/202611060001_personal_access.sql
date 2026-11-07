begin;
create table public.profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null default '',
  created_at timestamptz not null default now()
);
create table public.app_access (
  user_id uuid primary key references auth.users(id) on delete cascade,
  enabled boolean not null default false
);
alter table public.profiles enable row level security;
alter table public.app_access enable row level security;
-- Explicit privileges are as important as policies. No client writes, including profiles.
revoke all on public.profiles, public.app_access from public, anon, authenticated;
grant select on public.profiles, public.app_access to authenticated;
grant all on public.profiles, public.app_access to service_role;
create policy access_read_own on public.app_access for select to authenticated
  using (user_id = (select auth.uid()));
create policy profile_read_own_authorized on public.profiles for select to authenticated
  using (user_id = (select auth.uid()) and exists (
    select 1 from public.app_access a where a.user_id = (select auth.uid()) and a.enabled
  ));
create function public.provision_personal_user() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
  insert into public.profiles(user_id) values (new.id);
  insert into public.app_access(user_id, enabled) values (new.id, false);
  return new;
end;
$$;
revoke all on function public.provision_personal_user() from public, anon, authenticated;
create trigger provision_personal_user after insert on auth.users
for each row execute function public.provision_personal_user();
-- Existing accounts also start without access.
insert into public.profiles(user_id) select id from auth.users on conflict do nothing;
insert into public.app_access(user_id, enabled) select id, false from auth.users on conflict do nothing;
commit;
