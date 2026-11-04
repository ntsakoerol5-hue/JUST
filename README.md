# JUST — Private Phone Management

A consent-first device-management dashboard and Android companion starter for phones that are deliberately enrolled by their owners.

## Current state
The repository contains the initial Next.js dashboard, Supabase schema and an Android companion starter under `android/`. The Android starter can open the system photo and document pickers, request contacts/location permissions when the user taps the relevant buttons, and display basic device information. It does not upload or transmit phone data. Dashboard device cards still contain illustrative demo values only; no real phone is enrolled or monitored.

## Run the web dashboard
1. Install Node.js 20 or later.
2. Run `npm install`.
3. Copy `.env.example` to `.env.local`.
4. Add your Supabase project URL and anon/public key.
5. Run `npm run dev` and open http://localhost:3000.

## Open the Android companion
Open the `android/` folder in Android Studio, allow Gradle sync, then run the `app` configuration on an Android 8.0+ test device. See [android/README.md](android/README.md) for included functionality and limitations. This starter has not yet been built or tested on a physical device.

## Supabase setup
1. Create a project at https://supabase.com.
2. Open SQL Editor and run `supabase/schema.sql`.
3. Configure email authentication and secure redirect URLs.
4. Set `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY` in local and Vercel environments.
Never put the service-role key in frontend code, Android builds or committed files.

## Vercel deployment
Import this repository at https://vercel.com/new, select Next.js, add the two environment variables, and deploy. Future pushes to the connected branch trigger deployments.

## Device pairing backend scaffold
A one-time pairing backend is now included. After creating your Supabase project and applying `supabase/schema.sql`:
1. Apply `supabase/migrations/20261104_device_pairing.sql` in the Supabase SQL Editor.
2. Deploy the Edge Function with `supabase functions deploy pair-device` using the Supabase CLI.
3. A signed-in dashboard session can call `create_device_pairing_code()` to create an eight-digit code valid for ten minutes.
4. The companion sends the code to the `pair-device` Edge Function. The code is single-use; the function stores only a hash of the device token and returns the token once.

This is backend scaffolding only: the dashboard login/code-generation UI and Android pairing screen/token storage are not yet connected. No live Supabase project has been configured or tested.

## Next integration stages
- Connect the signed-in dashboard pairing UI and Android pairing screen to the one-time-code flow.
- Add authenticated upload endpoints for owner-selected photos, files and contact data, with encryption, server-side validation, short-lived credentials and revocation.
- Add foreground location sharing only after a clear opt-in, with a visible sharing state and stop control.
- Add an iOS companion using Apple's PhotoKit, Contacts, document picker and Core Location permission flows.
- For company-owned Android devices, investigate Android Enterprise managed-device enrolment and a properly configured device-policy controller.

## Security and platform limits
Android and iOS intentionally restrict access to private phone data. JUST does not bypass permissions, silently extract messages/calls, hide its presence, activate sensors covertly, or control a personal phone without the owner's approval. Android Enterprise and Apple MDM are legitimate management frameworks, not permission-bypass mechanisms. Owners must be able to see, limit and revoke sharing.

Before production, add verified device identity, signed short-lived ingestion tokens, server-side validation, rate limits, retention controls, MFA and independent security review.
