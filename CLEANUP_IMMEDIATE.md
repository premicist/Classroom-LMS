# Immediate Cleanup & Hardening Summary

This document records the immediate hardening and hygiene changes applied to the repository.

## 1. Summary of Changes

- **`.gitignore`**: Replaced root `.gitignore` with a standard Android exclusion list covering build outputs, IDE config, OS artifacts, and secrets (`.env`, `google-services.json`, `*.jks`, `keystore.properties`).
- **`app/build.gradle.kts`**:
  - Documented package identity choices (`namespace` vs `applicationId`).
  - Enabled release minification (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`).
  - Set debug build type to `isMinifyEnabled = false`.
- **`app/proguard-rules.pro`**: Updated R8/ProGuard rules to preserve Kotlin metadata, Coroutines internal factories, Jetpack Compose, Room entities/DAOs, Moshi JSON codegen, Retrofit HTTP annotations, Firebase SDK classes, and all `com.example.data.**` entities.
- **`ARCHITECTURE_FREEZE.md`**: Created architecture freeze rules for `ClassroomViewModel.kt` to prevent further growth of the god class and map out target feature ViewModels/UseCases. Added corresponding KDoc header to `ClassroomViewModel.kt`.
- **`README.md`**: Replaced with clean, developer-focused documentation covering setup, local run steps, release builds, signing environment variables, and security practices.

## 2. Package Identity Notes

- **`applicationId`**: Remains `com.aistudio.classroomlms.kxpyv` (matches Google Play Store and Firebase config).
- **`namespace`**: Remains `com.example` (matches current Kotlin/Java package hierarchy under `src/main/java`).
- **`FileProvider`**: Declared as `${applicationId}.fileprovider` in `AndroidManifest.xml` — kept unchanged.

## 3. Git Hygiene Commands

To untrack local noise and secrets previously cached in the Git index without removing local developer files:

```bash
cp app/google-services.json /tmp/google-services.json.backup 2>/dev/null || true
git rm --cached -r .idea/ 2>/dev/null || true
git rm --cached local.properties 2>/dev/null || true
git rm --cached ClassroomLMS-v1.3.0.apk 2>/dev/null || true
git rm --cached app/google-services.json 2>/dev/null || true
cp /tmp/google-services.json.backup app/google-services.json 2>/dev/null || true
```

Stage and commit only intentional files:

```bash
git add .gitignore app/build.gradle.kts app/proguard-rules.pro \
  README.md CLEANUP_IMMEDIATE.md ARCHITECTURE_FREEZE.md \
  app/src/main/java/com/example/ui/viewmodel/ClassroomViewModel.kt
git status
git commit -m "chore: harden gitignore, enable release minify, freeze ViewModel growth"
```

## 4. Verification Checklist

- [x] `./gradlew :app:assembleDebug` builds successfully
- [x] `./gradlew :app:assembleRelease` builds successfully with R8 minification enabled
- [ ] Smoke-test release APK installation and feature execution on target device or emulator
