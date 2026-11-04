# Pair-device Edge Function

Deploy with `supabase functions deploy pair-device`. Supabase provides `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` to Edge Functions; do not add the service-role key to the Android app or browser.

## Flow
1. A signed-in dashboard owner calls `create_device_pairing_code()` using their Supabase user session. The function returns an eight-digit code valid for ten minutes.
2. The owner enters that code into the companion app.
3. The app sends `POST /functions/v1/pair-device` with JSON: `{"code":"12345678","displayName":"My Android","platform":"android","osVersion":"15"}`.
4. The function hashes the code, atomically claims it once, creates a device record and returns a random device token. The database stores only the token hash.

The companion must store the returned token securely (Android Keystore) and must never log or display it. The token is not yet used for data uploads; implement a separate authenticated ingestion endpoint before enabling uploads. Pairing is not proof of permission to access data—the owner still chooses what to share in the companion app.

Apply `supabase/migrations/20261104_device_pairing.sql` after `supabase/schema.sql`. This migration and function have not yet been executed against a live Supabase project.
