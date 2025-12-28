# Book Reading Stats

An Android app for tracking your book reading progress with timed sessions, statistics, and cloud sync.

## Features

- **Reading Timer** - Track reading sessions with start/pause/resume functionality
- **Phone Call Detection** - Automatically pauses timer during phone calls
- **Progress Tracking** - See pages read, time spent, and completion percentage per book
- **Statistics Dashboard** - View total reading time, pages read, and reading pace
- **Progress Charts** - Visualize daily/weekly/monthly reading activity
- **Google Books Integration** - Search for books and auto-fill metadata (title, author, cover, page count)
- **Cloud Sync** - Sync data across devices with Supabase backend
- **Offline Support** - Works offline with local Room database, syncs when online
- **Google Sign-In** - Authenticate with your Google account

## Screenshots

<!-- Add screenshots here -->
*Coming soon*

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **Architecture:** MVVM with Clean Architecture
- **Local Database:** Room
- **Cloud Backend:** Supabase (PostgreSQL + Auth)
- **Dependency Injection:** Hilt
- **Image Loading:** Coil
- **API:** Google Books API for book metadata

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/BookReadingStats.git
   ```

2. Open the project in Android Studio

3. Create a `local.properties` file in the project root (if not exists):
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   ```

4. Build and run on an emulator or device

### Configuration

#### Supabase Setup (Required for cloud sync)

1. Create a project at [supabase.com](https://supabase.com)
2. Run the SQL schema from `schema.sql` in the SQL Editor
3. Update `SupabaseConfig.kt` with your credentials:
   ```kotlin
   private const val SUPABASE_URL = "https://your-project.supabase.co"
   private const val SUPABASE_ANON_KEY = "your-anon-key"
   ```

See [SUPABASE_SETUP.md](SUPABASE_SETUP.md) for detailed instructions.

#### Google Sign-In Setup (Optional)

See [GOOGLE_SIGNIN_SETUP.md](GOOGLE_SIGNIN_SETUP.md) for configuring Google authentication.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                         │
│         Jetpack Compose + ViewModels                │
└─────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────┐
│                 Domain Layer                        │
│            Use Cases + Models                       │
└─────────────────────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────┐
│                  Data Layer                         │
│    Repository + Local (Room) + Remote (Supabase)   │
└─────────────────────────────────────────────────────┘
```

## Project Structure

```
app/src/main/java/com/bookstats/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # Supabase client, DTOs, Google Books API
│   ├── repository/     # Repository implementations
│   └── sync/           # Sync manager for offline support
├── domain/
│   └── model/          # Domain models
├── service/            # Background timer service
├── ui/
│   ├── components/     # Reusable Compose components
│   ├── navigation/     # Navigation graph
│   ├── screens/        # Feature screens
│   │   ├── auth/       # Login/signup
│   │   ├── books/      # Book list, add book
│   │   ├── bookdetail/ # Book details, edit
│   │   ├── chart/      # Reading progress charts
│   │   ├── sessions/   # Reading session history
│   │   ├── statistics/ # Overall statistics
│   │   └── timer/      # Reading timer
│   ├── theme/          # Material 3 theming
│   └── util/           # UI utilities
└── di/                 # Hilt modules
```

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [Google Books API](https://developers.google.com/books) for book metadata
- [Supabase](https://supabase.com) for backend services
- [Material Design 3](https://m3.material.io/) for design guidelines
