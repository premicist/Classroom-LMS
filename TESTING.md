# Testing & Quality Sprint Guide

## How to Run Unit Tests

To run unit tests locally from the command line:

```bash
./gradlew :app:testDebugUnitTest
```

All ViewModel unit tests (including `AttendanceViewModelTest`, `ExamViewModelTest`, and `GradingViewModelTest`) run deterministically using `kotlinx-coroutines-test`, `Robolectric`, and in-memory Fake DAOs without needing physical devices, emulator, or network access.

## Automated CI Pipeline (GitHub Actions)

GitHub Actions workflow (`.github/workflows/android.yml`) executes the following automated checks on every push and pull request to `master`:

1. **Static Analysis (Lint)**: `./gradlew lintDebug`
2. **Unit Tests**: `./gradlew :app:testDebugUnitTest`
3. **Build Debug APK**: `./gradlew assembleDebug`
4. **Build Release APK**: Generates dummy `app/google-services.json` in CI runner and compiles `./gradlew :app:assembleRelease --stacktrace` (validating R8 code/resource shrinking and release compilation).

## Manual Verification

Teacher UI smoke testing on device/emulator remains manual:
- Verify active classroom switching and biometric lock on app launch.
- Smoke test attendance marking and exam score entry in UI tabs.
- Verify PDF report generation actions.
