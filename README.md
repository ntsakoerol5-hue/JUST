# JUST — Private Phone Management

A consent-first device-management dashboard and Android companion.

## Current state
The repository includes a Next.js dashboard with Supabase email sign-in and one-time pairing-code generation, a Supabase pairing migration and Edge Function, and an Android companion pairing screen. The Android app can submit the code and stores the returned device token using AndroidX encrypted preferences.

**No live Supabase project has been configured or tested.** The app currently registers a device only; photos, files, contacts and location are not uploaded to the dashboard. Local picker/permission demonstrations are not data synchronisation.

## Configure Supabase
1. Create a Supabase project.
2. Run `supabase/schema.sql` in the SQL Editor.
3. Run `supabase/migrations/20261104_device_pairing.sql`.
4. Set the dashboard environment variables `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY`.
5. Deploy the function from the repository root using the Supabase CLI: `supabase functions deploy pair-device`.
6. Ensure email authentication is enabled and configure the allowed redirect URLs.

The pairing Edge Function uses the server-side `SUPABASE_SERVICE_ROLE_KEY` supplied by Supabase. Never copy that key into the browser, Android app, or Git repository. The function is configured with JWT verification disabled because the one-time random pairing code is its credential; the Supabase public API key remains required in the `apikey` header.

## Configure and run Android
1. Open the `android/` folder in Android Studio.
2. Create `android/local.properties` with the Supabase project URL and public anon/publishable key:

```properties
JUST_SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
JUST_SUPABASE_ANON_KEY=YOUR_SUPABASE_PUBLISHABLE_OR_ANON_KEY
```

3. Sync Gradle and run the app on an Android 8.0+ test device.
4. Sign in to the JUST dashboard, generate a pairing code, and enter it in the companion app.

`android/local.properties` is ignored by Git. Use only a public anon/publishable key in the mobile app.

## Run the dashboard
Install Node.js 20+, run `npm install`, copy `.env.example` to `.env.local`, set the two public Supabase variables, then run `npm run dev`.

## Next integration stages
- Load real paired-device records in the dashboard and add server-side revoke controls.
- Add authenticated, encrypted upload endpoints for files/photos and selected contact exports.
- Add clear, revocable foreground location sharing.
- Build the iOS companion using Apple's approved system frameworks.

## Security and platform limits
Android and iOS restrict access to private phone data. JUST does not bypass permissions, secretly extract messages/calls, hide its presence, activate sensors covertly, or control personal phones without the owner's approval. The companion must make sharing visible and revocable. Before production, add rate limits, verified device identity, token rotation, upload validation, retention controls, MFA and independent security review.
