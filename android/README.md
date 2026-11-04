# JUST Android companion — starter

This is the first Android companion-app scaffold for JUST. It is designed for an Android phone owner to open the app and choose what to share.

## Included in this starter
- Android system Photo Picker for user-selected photos and videos.
- Android document picker for user-selected files.
- Runtime contacts permission, with a local contact count demonstration.
- Runtime foreground location permission request (no background tracking).
- Basic device model, Android version and battery information.
- Visible actions and status messages.

**This starter does not upload or transmit any phone data.** Selected photo/file URIs and contact details are not sent to a server. A secure, authenticated pairing and upload service must be implemented and tested before any dashboard can receive shared data.

## Build
Open the `android/` folder in Android Studio (current stable version), allow Gradle sync, then run the `app` configuration on an Android 8.0+ test device.

## Important platform limits
- Android's Photo Picker and document picker let the user choose specific items without granting broad storage access.
- Contacts and location use Android runtime permissions. The owner can deny or revoke them in Android Settings.
- This app does not request background location, accessibility-service control, device-admin privileges, root access or hidden permissions.
- Enterprise-wide management requires a legitimate Android Enterprise managed-device enrolment and a separately configured device-policy controller. It is not a way to bypass personal-device security.
- This starter has not yet been built or tested on a physical device. Do not treat it as production-ready.

## Next engineering stage
Implement QR/one-time-code pairing, authenticated API endpoints, short-lived device credentials, encrypted uploads, server-side validation, consent records and revocation. Never put Supabase service-role credentials in the Android app.
