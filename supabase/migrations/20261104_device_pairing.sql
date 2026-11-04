-- JUST one-time device pairing.
-- Apply after supabase/schema.sql. Pairing codes are created by a signed-in dashboard
-- owner and can be claimed once by the companion Edge Function.

alter table public.devices
  add column if not exists device_token_hash text unique;

create table if not exists public.device_pairing_codes (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  code_hash text not null unique,
  expires_at timestamptz not null,
  used_at timestamptz,
  created_at timestamptz not null default now()
);

alter table public.device_pairing_codes enable row level security;

create policy "Owners can view their pairing requests"
  on public.device_pairing_codes for select
  using (auth.uid() = owner_id);

create or replace function public.create_device_pairing_code()
returns table(pairing_code text, expires_at timestamptz)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_code text;
  v_expiry timestamptz := now() + interval '10 minutes';
begin
  if auth.uid() is null then
    raise exception 'Authentication required';
  end if;

  loop
    v_code := lpad(floor(random() * 100000000)::bigint::text, 8, '0');
    begin
      insert into public.device_pairing_codes(owner_id, code_hash, expires_at)
      values (auth.uid(), encode(digest(v_code, 'sha256'), 'hex'), v_expiry);
      exit;
    exception when unique_violation then
      -- Generate another code if this one already exists.
    end;
  end loop;

  return query select v_code, v_expiry;
end;
$$;

revoke all on function public.create_device_pairing_code() from public;
grant execute on function public.create_device_pairing_code() to authenticated;

create or replace function public.claim_device_pairing(
  p_code_hash text,
  p_device_token_hash text,
  p_display_name text,
  p_platform text,
  p_os_version text
)
returns table(device_id uuid, owner_id uuid)
language plpgsql
security definer
set search_path = public
as $$
declare
  v_owner_id uuid;
  v_device_id uuid;
begin
  if p_platform not in ('android', 'ios') then
    raise exception 'Unsupported platform';
  end if;
  if length(trim(p_display_name)) < 1 or length(p_display_name) > 100 then
    raise exception 'Invalid device name';
  end if;
  if p_code_hash !~ '^[a-f0-9]{64}$' or p_device_token_hash !~ '^[a-f0-9]{64}$' then
    raise exception 'Invalid credential format';
  end if;

  update public.device_pairing_codes
     set used_at = now()
   where code_hash = p_code_hash
     and used_at is null
     and expires_at > now()
  returning device_pairing_codes.owner_id into v_owner_id;

  if v_owner_id is null then
    raise exception 'Pairing code is invalid, expired, or already used';
  end if;

  insert into public.devices(owner_id, display_name, platform, status, os_version, device_token_hash, last_seen_at)
  values (v_owner_id, trim(p_display_name), p_platform, 'online', left(coalesce(p_os_version, ''), 40), p_device_token_hash, now())
  returning id into v_device_id;

  insert into public.audit_events(owner_id, device_id, event_type, details)
  values (v_owner_id, v_device_id, 'device_paired', jsonb_build_object('platform', p_platform));

  return query select v_device_id, v_owner_id;
end;
$$;

revoke all on function public.claim_device_pairing(text, text, text, text, text) from public, anon, authenticated;
grant execute on function public.claim_device_pairing(text, text, text, text, text) to service_role;

create index if not exists pairing_owner_created_idx
  on public.device_pairing_codes(owner_id, created_at desc);
