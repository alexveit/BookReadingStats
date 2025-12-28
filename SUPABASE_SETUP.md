# Supabase Integration Guide

## Overview

This guide explains how to set up cloud sync for Book Reading Stats using Supabase as the backend.

**Features:**
- Email/password authentication
- Google Sign-In
- Cloud sync (Supabase = source of truth)
- Local cache (Room) for fast UI
- Data persists across devices

---

## Step 1: Supabase Project Setup

1. Go to [supabase.com](https://supabase.com) and create an account
2. Create a new project
3. Save your database password!
4. Wait for project to initialize (~2 min)

---

## Step 2: Run Database Schema

1. In Supabase Dashboard, go to **SQL Editor**
2. Click **New Query**
3. Paste the contents of `schema.sql`
4. Click **Run**

This creates:
- `books` table
- `reading_sessions` table
- Row Level Security policies (users can only see their own data)
- Auto-update triggers

---

## Step 3: Google Sign-In Setup

Follow the steps in `GOOGLE_SIGNIN_SETUP.md`:

1. Create Google Cloud project
2. Create Android OAuth client (with SHA-1)
3. Create Web OAuth client
4. Enable Google provider in Supabase
5. Note your Web Client ID

---

## Step 4: Get Your Supabase Credentials

In Supabase Dashboard → **Settings → API**, copy:

- **Project URL**: `https://xxxxx.supabase.co`
- **anon public key**: `eyJhbGciOiJIUzI1NiIs...`

---

## Step 5: Android Configuration

### 5.1 Update SupabaseConfig

In `app/src/main/java/com/bookstats/data/remote/SupabaseConfig.kt`, replace the placeholders:

```kotlin
private const val SUPABASE_URL = "https://your-project-id.supabase.co"
private const val SUPABASE_ANON_KEY = "your-anon-key-here"
const val GOOGLE_WEB_CLIENT_ID = "your-web-client-id.apps.googleusercontent.com"
```

### 5.2 Deep Link for Auth Callback

The app already has the deep link configured in `AndroidManifest.xml`:

```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="bookstats" android:host="auth-callback" />
</intent-filter>
```

---

## Step 6: Test It!

1. Build and run the app
2. You should see the login screen
3. Try:
   - Sign up with email
   - Sign in with Google
4. Add a book and verify it appears in Supabase Dashboard → Table Editor

---

## Migrating Existing Data

If you have existing local data you want to upload to Supabase:

1. Sign in to the app
2. The current implementation will fetch from Supabase (empty)
3. You'll need to manually upload existing data or add a migration function

**Option: Add data upload function**
```kotlin
suspend fun uploadLocalDataToSupabase() {
    // Get all local books without remoteId
    val localBooks = bookDao.getAllBooks().first().filter { it.remoteId == null }

    localBooks.forEach { book ->
        // Insert to Supabase
        val remoteBook = remoteDataSource.insertBook(BookInsertDto(...))

        // Update local with remoteId
        bookDao.updateBook(book.copy(remoteId = remoteBook.id))

        // Upload sessions for this book
        // ...
    }
}
```

---

## Troubleshooting

### "Invalid API key"
- Double-check SUPABASE_URL and SUPABASE_ANON_KEY

### "User not found" on sign in
- Make sure you signed up first, or use Google Sign-In

### Google Sign-In Error 10
- SHA-1 fingerprint mismatch
- Run `./gradlew signingReport` and update Google Cloud Console

### Data not syncing
- Check internet connection
- Look at Supabase Dashboard → Logs for errors
- Make sure RLS policies are in place

---

## Architecture Summary

```
┌──────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  (Compose Screens, ViewModels)                       │
└──────────────────────────────────────────────────────┘
                         │
                         ▼
┌──────────────────────────────────────────────────────┐
│                 Repository Layer                     │
│  BookRepositoryImpl                                  │
│  - Fetches from Supabase (source of truth)          │
│  - Caches to Room (local speed)                     │
│  - Handles offline gracefully                        │
└──────────────────────────────────────────────────────┘
          │                           │
          ▼                           ▼
┌─────────────────────┐    ┌─────────────────────┐
│   Room Database     │    │     Supabase        │
│   (Local Cache)     │    │  (Source of Truth)  │
│                     │    │                     │
│  - Fast reads       │    │  - Cloud storage    │
│  - Offline access   │    │  - Cross-device     │
│  - Auto-sync        │    │  - Auth             │
└─────────────────────┘    └─────────────────────┘
```

---

## Security Notes

- **Never commit** your Supabase keys to git (use `local.properties` or environment variables for production)
- The `anon` key is safe to include in the app (it's rate-limited and RLS-protected)
- Row Level Security ensures users only see their own data
- All database queries are filtered by `auth.uid()`
