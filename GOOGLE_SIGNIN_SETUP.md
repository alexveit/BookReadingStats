# Google Sign-In Setup for Supabase

## Step 1: Google Cloud Console

1. Go to https://console.cloud.google.com/
2. Create a new project (or select existing)
3. Go to "APIs & Services" → "Credentials"
4. Click "Create Credentials" → "OAuth client ID"
5. Select "Android" as application type
6. Fill in:
   - Name: "Book Reading Stats Android"
   - Package name: `com.bookstats`
   - SHA-1 certificate fingerprint (see below)

### Getting SHA-1 Fingerprint

In Android Studio terminal, run:

```bash
# For debug builds:
./gradlew signingReport
```

Or manually:
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the SHA-1 value (looks like: `AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD`)

## Step 2: Create Web Client ID (Required for Supabase)

1. Still in Google Cloud Console → Credentials
2. Click "Create Credentials" → "OAuth client ID"
3. Select "Web application"
4. Name: "Book Reading Stats Web Client"
5. Add Authorized redirect URI:
   ```
   https://YOUR_SUPABASE_PROJECT_ID.supabase.co/auth/v1/callback
   ```
   (Replace YOUR_SUPABASE_PROJECT_ID with your actual project ID)
6. Click "Create"
7. Copy the "Client ID" (looks like: `123456789-abcdefg.apps.googleusercontent.com`)

## Step 3: Enable Google Provider in Supabase

1. Go to your Supabase Dashboard
2. Authentication → Providers
3. Find "Google" and enable it
4. Paste your Web Client ID
5. Paste your Web Client Secret
6. Save

## Step 4: Update Android Code

In your `local.properties` file (don't commit this!):
```
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-anon-key-here
GOOGLE_WEB_CLIENT_ID=your-web-client-id.apps.googleusercontent.com
```

## Troubleshooting

- **Error 10**: SHA-1 fingerprint mismatch. Make sure you added the correct SHA-1.
- **Error 12500**: Google Play Services issue. Test on real device or updated emulator.
- **Redirect URI mismatch**: Double-check the callback URL in Google Console matches Supabase.
