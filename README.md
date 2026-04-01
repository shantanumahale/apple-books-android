# Apple Books for Android

An Android e-book reader app inspired by Apple Books. Supports **PDF** and **EPUB** formats with reading progress tracking, customizable themes/fonts, and collection management.

Built with Jetpack Compose, Clean Architecture (MVVM), Hilt, Room, and Kotlin Coroutines.

## Features

- **PDF & EPUB reading** — PDF via `PdfRenderer` with bitmap caching; EPUB via WebView with CSS injection
- **Library management** — Add books via file picker or share intent, search by title/author, grid/list toggle
- **Collections** — Create, rename, and organize books into collections
- **Reading progress** — Automatically saves and restores your position in each book
- **Customizable reader** — 4 reading themes (White, Sepia, Gray, Dark), 7+ fonts, adjustable font size (12–32px), page turn effects (Curl, Slide)
- **Intent handling** — Open PDF/EPUB files directly from other apps

## Tech Stack

| Component       | Library                          |
|-----------------|----------------------------------|
| UI              | Jetpack Compose (BOM 2024.12.01) |
| Navigation      | Navigation Compose 2.8.5        |
| DI              | Hilt 2.54 + KSP                 |
| Database        | Room 2.6.1                      |
| Preferences     | DataStore Preferences 1.1.1     |
| Image Loading   | Coil 2.7.0                      |
| Async           | Coroutines 1.9.0                |
| Page Animation  | Page Curl Effect 1.3.2          |

**Min SDK**: 26 &nbsp;|&nbsp; **Target SDK**: 35 &nbsp;|&nbsp; **Java**: 17 &nbsp;|&nbsp; **Kotlin**: 2.1.0

## Project Structure

```
app/src/main/java/com/applebooks/android/
├── App.kt                        # Application class (@HiltAndroidApp)
├── MainActivity.kt               # Single activity, handles PDF/EPUB intents
│
├── di/                           # Dependency Injection
│   ├── AppModule.kt              # Room DB, DAOs, DataStore providers
│   └── RepositoryModule.kt       # Repository bindings
│
├── domain/                       # Domain Layer
│   ├── model/                    # Book, Collection, ReadingProgress, enums
│   └── repository/               # Repository interfaces
│
├── data/                         # Data Layer
│   ├── local/
│   │   ├── db/                   # Room database, DAOs, entities
│   │   └── preferences/          # DataStore preferences
│   └── repository/               # Repository implementations
│
├── reader/                       # Book Readers
│   ├── pdf/                      # PdfRenderer-based reader + bitmap cache
│   └── epub/                     # WebView-based reader + EPUB parser
│
├── ui/                           # UI Layer (Compose)
│   ├── library/                  # Library screen + ViewModel
│   ├── collections/              # Collections screens + ViewModels
│   ├── bookdetail/               # Book detail bottom sheet
│   ├── settings/                 # Settings screen + ViewModel
│   ├── navigation/               # NavGraph + route definitions
│   ├── components/               # Shared composables
│   └── theme/                    # Color, Theme, Typography
│
└── util/                         # Utilities
    ├── FileMetadataExtractor.kt  # PDF/EPUB metadata parsing
    ├── CoverExtractor.kt         # Cover image extraction & caching
    └── UriPermissionManager.kt   # SAF permission handling
```

## Prerequisites

- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK 17**
- **Android SDK 35** (install via SDK Manager)

## Getting Started

1. **Clone the repository**

   ```bash
   git clone https://github.com/<your-username>/apple-books-android.git
   cd apple-books-android
   ```

2. **Open in Android Studio**

   File > Open > select the project root directory.

3. **Sync Gradle**

   Android Studio will automatically prompt you to sync. If not, click **Sync Project with Gradle Files** in the toolbar.

4. **Run on a device or emulator**

   Select a device/emulator (API 26+) and click **Run** or:

   ```bash
   ./gradlew installDebug
   ```

## Building

### Debug APK

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK

1. Create a `keystore.jks` file (or use an existing one):

   ```bash
   keytool -genkey -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release
   ```

2. Add signing config to `app/build.gradle.kts`:

   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("../keystore.jks")
               storePassword = System.getenv("KEYSTORE_PASSWORD")
               keyAlias = "release"
               keyPassword = System.getenv("KEY_PASSWORD")
           }
       }
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
           }
       }
   }
   ```

3. Build:

   ```bash
   KEYSTORE_PASSWORD=<password> KEY_PASSWORD=<password> ./gradlew assembleRelease
   ```

   Output: `app/build/outputs/apk/release/app-release.apk`

### Release Bundle (AAB) for Google Play

```bash
KEYSTORE_PASSWORD=<password> KEY_PASSWORD=<password> ./gradlew bundleRelease
```

Output: `app/build/outputs/bundle/release/app-release.aab`

## Useful Gradle Commands

| Command                        | Description                    |
|--------------------------------|--------------------------------|
| `./gradlew assembleDebug`      | Build debug APK                |
| `./gradlew assembleRelease`    | Build release APK              |
| `./gradlew bundleRelease`      | Build release AAB              |
| `./gradlew installDebug`       | Build and install on device    |
| `./gradlew test`               | Run unit tests                 |
| `./gradlew connectedAndroidTest` | Run instrumented tests       |
| `./gradlew clean`              | Clean build outputs            |
| `./gradlew lint`               | Run lint checks                |

## License

This project is for personal/educational use.
