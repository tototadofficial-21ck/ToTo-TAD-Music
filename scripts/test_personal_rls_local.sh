#!/usr/bin/env bash
# Isolated local PostgreSQL test; never connects to a Supabase project.
set -eu
TOTO_PGDIR=$(mktemp -d /tmp/toto-auth-pg.XXXXXX)
initdb -D "$TOTO_PGDIR/data" -A trust -U toto_owner --no-locale > "$TOTO_PGDIR/init.log"
pg_ctl -D "$TOTO_PGDIR/data" -l "$TOTO_PGDIR/server.log" -o "-F -p 55439 -h '' -k $TOTO_PGDIR" -w start
trap 'pg_ctl -D "$TOTO_PGDIR/data" -m fast -w stop; rm -rf "$TOTO_PGDIR"' EXIT
export PGHOST="$TOTO_PGDIR" PGPORT=55439 PGUSER=toto_owner PGDATABASE=postgres
psql -v ON_ERROR_STOP=1 <<'SQL'
create role anon nologin;
create role authenticated nologin;
create role service_role nologin bypassrls;
create schema auth;
create table auth.users(id uuid primary key);
create function auth.uid() returns uuid language sql stable as $$ select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid $$;
grant usage on schema auth to anon, authenticated, service_role;
-- Existing account before migration tests default-deny backfill.
insert into auth.users(id) values ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');
SQL
psql -v ON_ERROR_STOP=1 -f supabase/migrations/202611060001_personal_access.sql
psql -v ON_ERROR_STOP=1 <<'SQL'
insert into auth.users(id) values ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');
do $$ begin
 if (select count(*) from public.app_access where enabled=false) <> 2 then raise exception 'default grant unexpectedly enabled'; end if;
 if (select count(*) from public.profiles) <> 2 then raise exception 'profile trigger failed'; end if;
end $$;
SQL
psql -v ON_ERROR_STOP=1 -f supabase/verify_rls.sql
psql -v ON_ERROR_STOP=1 <<'SQL'
begin;
delete from auth.users where id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
do $$ begin
 if exists (select 1 from public.app_access where user_id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa') or exists (select 1 from public.profiles where user_id='aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa') then raise exception 'cascade failed'; end if;
end $$;
rollback;
SQL
# Minimal Storage schema mock exercises the new policies/GRANTs without network access.
psql -v ON_ERROR_STOP=1 <<'SQL'
create schema storage;
create table storage.buckets(id text primary key, name text, public boolean, file_size_limit bigint, allowed_mime_types text[]);
create table storage.objects(id bigint generated always as identity primary key, bucket_id text references storage.buckets(id), name text);
alter table storage.objects enable row level security;
grant usage on schema storage to authenticated;
grant select,insert,update,delete on storage.objects to authenticated;
grant usage on all sequences in schema storage to authenticated;
SQL
psql -v ON_ERROR_STOP=1 -f supabase/migrations/202611070001_account_profiles.sql
psql -v ON_ERROR_STOP=1 -f supabase/verify_account_profiles.sql
