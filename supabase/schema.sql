create extension if not exists pgcrypto;
create table if not exists public.devices (
 id uuid primary key default gen_random_uuid(),
 owner_id uuid not null references auth.users(id) on delete cascade,
 display_name text not null,
 platform text not null check (platform in ('android','ios')),
 status text not null default 'pending' check (status in ('pending','online','offline','revoked')),
 os_version text,
 battery_percent int check (battery_percent between 0 and 100),
 storage_free_bytes bigint,
 last_seen_at timestamptz,
 created_at timestamptz not null default now(),
 updated_at timestamptz not null default now()
);
create table if not exists public.device_consents (
 id uuid primary key default gen_random_uuid(),
 device_id uuid not null references public.devices(id) on delete cascade,
 owner_id uuid not null references auth.users(id) on delete cascade,
 permission text not null check(permission in ('location','usage_summary','screen_share','shared_files')),
 granted boolean not null default false,
 granted_at timestamptz,
 revoked_at timestamptz,
 created_at timestamptz not null default now()
);
create table if not exists public.location_points (
 id uuid primary key default gen_random_uuid(),
 device_id uuid not null references public.devices(id) on delete cascade,
 owner_id uuid not null references auth.users(id) on delete cascade,
 latitude double precision not null check(latitude between -90 and 90),
 longitude double precision not null check(longitude between -180 and 180),
 recorded_at timestamptz not null default now()
);
create table if not exists public.audit_events (
 id uuid primary key default gen_random_uuid(),
 owner_id uuid not null references auth.users(id) on delete cascade,
 device_id uuid references public.devices(id) on delete set null,
 event_type text not null,
 details jsonb not null default '{}'::jsonb,
 created_at timestamptz not null default now()
);
alter table public.devices enable row level security;
alter table public.device_consents enable row level security;
alter table public.location_points enable row level security;
alter table public.audit_events enable row level security;
create policy "Owners manage their devices" on public.devices for all using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
create policy "Owners view consent records" on public.device_consents for select using (auth.uid() = owner_id);
create policy "Owners view location history" on public.location_points for select using (auth.uid() = owner_id);
create policy "Owners view audit events" on public.audit_events for select using (auth.uid() = owner_id);
create index if not exists devices_owner_idx on public.devices(owner_id);
create index if not exists location_device_time_idx on public.location_points(device_id, recorded_at desc);
create index if not exists audit_owner_time_idx on public.audit_events(owner_id, created_at desc);
-- Never expose a Supabase service-role key in browser code. Device ingestion requires a verified companion service.
