# Phone Guard — Private Phone Management
A consent-first device-management dashboard for Android and iOS.

## Current state
This repository contains the initial Next.js dashboard, responsive styling, environment template and a Supabase schema. Device cards contain illustrative demo values only. No real phone is enrolled or monitored.

## Local development
1. Install Node.js 20 or later.
2. Run `npm install`.
3. Copy `.env.example` to `.env.local`.
4. Add your Supabase project URL and anon/public key.
5. Run `npm run dev` and open http://localhost:3000.

## Supabase setup
1. Create a project at https://supabase.com.
2. Open SQL Editor and run `supabase/schema.sql`.
3. Configure email authentication and secure redirect URLs.
4. Set `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY` in local and Vercel environments.
Never put the service-role key in frontend code or commit secrets.

## Vercel deployment
Import this repository at https://vercel.com/new, select Next.js, add the two environment variables, and deploy. Future pushes to the connected branch trigger deployments.

## Device integration roadmap
- Android: companion application with explicit runtime permissions; use Android Enterprise/MDM only for legitimately enrolled managed devices.
- iOS: use Apple-supported MDM and user-approved system frameworks. iOS does not permit unrestricted third-party remote access.
- Use one-time enrollment, device-owner consent, visible sharing indicators, revocation, least-privilege tokens and auditable access.
- Do not implement covert installation, permission bypass, message/call extraction, hidden recording or control without the device owner's approval.

## Security
RLS policies restrict user-owned records. Before production, add verified device identity, signed short-lived ingestion tokens, server-side validation, rate limits, retention controls, MFA and security review.
