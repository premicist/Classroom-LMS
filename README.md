# Classroom LMS

Classroom LMS is an offline-first Android application designed for teachers to manage classroom rosters, attendance, homework, grading, daily logs, lesson plans, and student support.

View app in AI Studio: [AI Studio App Link](https://ai.studio/apps/b498d84a-3f44-4d5b-8315-c415b245499d)

## Local Setup & Run

1. **Open in Android Studio**: Open Android Studio and select **Open**, choosing this repository directory.
2. **Firebase Configuration**: Download your `google-services.json` file from your Firebase console and place it at `app/google-services.json`. *(This file is gitignored — never commit credentials!)*
3. **Environment Variables**: Create a root `.env` file based on `.env.example` and set `GEMINI_API_KEY` if using Gemini AI features:
   ```bash
   cp .env.example .env
   ```
4. **Run Debug Build**: Select a physical device or emulator and run the `debug` build configuration from Android Studio or command line:
   ```bash
   ./gradlew :app:assembleDebug
   ```

## Release Build & Minification

Release builds have R8 code minification and resource shrinking enabled (`isMinifyEnabled = true`, `isShrinkResources = true`).

To build a release APK:
```bash
./gradlew :app:assembleRelease
```

### Signing Environment Variables

Release signing configuration expects the following environment variables (falls back to local upload key file if unset):
- `KEYSTORE_PATH`: Path to your upload keystore file (defaults to `${rootDir}/my-upload-key.jks`)
- `STORE_PASSWORD`: Keystore password
- `KEY_PASSWORD`: Key password for alias `upload`

Always smoke-test release builds after assembling to verify R8 keep rules for reflection or serialization.

## Security Practices

Never commit the following sensitive or machine-local files to version control:
- `.env` and `.env.local`
- `local.properties`
- `*.jks` / `*.keystore`
- `app/google-services.json`
- `keystore.properties` or `secrets.properties`

## Architecture & Maintenance

- **Architecture Freeze**: `ClassroomViewModel.kt` is currently frozen for new feature logic. Refer to [ARCHITECTURE_FREEZE.md](ARCHITECTURE_FREEZE.md) before adding new capabilities.
- **Cleanup Summary**: For details on package identity and git hygiene, see [CLEANUP_IMMEDIATE.md](CLEANUP_IMMEDIATE.md).
