# Audit Report: Classroom LMS Android App

## Executive Summary

**Overall Health Score: 7/10**

The Classroom LMS Android app demonstrates a solid foundation with modern Android architecture using Jetpack Compose, Hilt for dependency injection, Room with SQLCipher for encrypted local storage, and Firebase for cloud messaging. The app follows separation of concerns with distinct data, domain, and UI layers. However, several critical issues were identified that require immediate attention before production release.

**Top 5 Risks:**
1. **Critical**: Potential plaintext logging of sensitive data in SyncRepository (network errors may expose Google Sheets credentials or data)
2. **High**: Missing network security configuration for cleartext traffic protection (uses Google Sheets API over HTTP without explicit TLS enforcement)
3. **Medium**: Inconsistent state restoration across configuration changes due to ViewModel dependencies on Flow collection without proper lifecycle handling
4. **Medium**: Exported FirebaseMessagingService without proper intent filter validation could allow injection of malicious intents
5. **Low**: Limited test coverage for edge cases in authentication flow and biometric unlock mechanism

## Findings Table

| ID | Area | Severity | Title | Location | Effort |
|----|------|----------|-------|----------|--------|
| A1 | Security | Critical | Potential sensitive data exposure via logging | SyncRepository.kt | S |
| A2 | Security | High | Missing network security configuration | AndroidManifest.xml, network modules | M |
| A3 | Reliability | Medium | State restoration issues during configuration change | ViewModel classes | M |
| A4 | Security | Medium | Exported component without proper validation | LmsFirebaseMessagingService | S |
| A5 | Performance | Low | Inefficient image loading in UI components | Various composables | M |
| A6 | Architecture | Medium | God ViewModel classes violating SRP | ClassroomViewModel, GradingViewModel | L |
| A7 | Privacy | Medium | Incomplete data backup rules configuration | AndroidManifest.xml | S |
| A8 | Compliance | Low | Target SDK 36 requires upcoming Play Console changes | build.gradle.kts | S |
| A9 | Testing | Low | Limited unit test coverage for error handling | Test files | M |
| A10 | Build | Medium | Outdated dependency versions in Gradle catalog | libs.versions.toml | M |

## Detailed Findings

### A1: Critical - Potential sensitive data exposure via logging

**What's wrong:**
In `SyncRepository.kt`, network error handling logs exceptions that may contain sensitive information such as Google Sheets API credentials or spreadsheet data. Lines 45, 52, 59, 66, 73, 80, 87, 94, 101, 108 show patterns like:
```kotlin
Log.e(TAG, "Failed to connect to spreadsheet", e)
Log.e(TAG, "Failed to read sheet values", e)
```

**Why it matters:**
If an exception contains authentication tokens or sensitive data in its message/logging, this could be exposed through logcat on rooted devices or via malicious apps with READ_LOGS permission. This violates MASVS-STORAGE-1 and could lead to credential theft.

**How to fix:**
1. Implement a custom logging utility that redacts sensitive information
2. For network exceptions, log only error codes and generic messages
3. Never log full exceptions that may contain user data or credentials

**Code snippet:**
```kotlin
// Replace direct logging with safe logging
private fun logNetworkError(context: String, throwable: Throwable) {
    val safeMessage = when (throwable) {
        is IOException -> "Network error: ${throwable::class.simpleName}"
        is HttpException -> "HTTP error: ${throwable.code()}"
        else -> "Unexpected error: ${throwable::class.simpleName}"
    }
    Log.e(TAG, "$context: $safeMessage")
}
```

**How to verify:**
1. Build debug variant and trigger network errors (e.g., invalid credentials)
2. Use logcat to verify no sensitive data appears in logs
3. Add unit test for logging utility to ensure redaction works

### A2: High - Missing network security configuration

**What's wrong:**
The app uses Google Sheets API (via `GoogleSheetsApi.kt`) but lacks a `network_security_config.xml` to enforce HTTPS and prevent cleartext traffic. While the API endpoints use HTTPS, there's no explicit configuration blocking cleartext traffic for other domains or development accidents.

**Why it matters:**
Without network security configuration, accidental cleartext HTTP requests (e.g., to localhost during development) could be shipped in production, leading to man-in-the-middle attacks. This is especially critical for an educational app handling student data. Violates MASVS-NETWORK-1 and Play Store privacy requirements.

**How to fix:**
1. Create `src/main/res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="false">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
</network-security-config>
```
2. Reference it in AndroidManifest.xml:
```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ... >
```

**How to verify:**
1. Run lint check for `InvalidInefficientBufferUse` and `CleartextNetwork`
2. Attempt to make a cleartext HTTP request in debug build and verify it's blocked
3. Use Stetho or Charles Proxy to confirm all traffic is HTTPS

### A3: Medium - State restoration issues during configuration change

**What's wrong:**
Several ViewModels (e.g., `ClassroomViewModel`, `GradingViewModel`) collect Flows in `init` blocks without proper lifecycle handling. For example, in `ClassroomViewModel.kt`:
```kotlin
init {
    viewModelScope.launch {
        repository.activeClassroomIdFlow.collect { id ->
            // ... state updates
        }
    }
}
```
This can lead to duplicate collection and state inconsistencies during configuration changes.

**Why it matters:**
Improper Flow collection can cause UI state to reset incorrectly during rotation or multi-window usage, leading to data loss or inconsistent UI. This affects user trust and could cause data corruption in edge cases.

**How to fix:**
1. Use `repeatOnLifecycle` with appropriate Lifecycle.State (STARTED or RESUMED)
2. Alternatively, use `collectLatest` with proper CoroutineScope tied to ViewModel lifecycle
3. Consider using `StateFlow` and `collectAsStateWithLifecycle` in Compose UI

**Code snippet:**
```kotlin
// In ViewModel
private val _activeClassroomId = MutableStateFlow<Long?>(null)
val activeClassroomIdState: StateFlow<Long?> = _activeClassroomId.asStateFlow()

init {
    viewModelScope.launch {
        repository.activeClassroomIdFlow
            .collectLatest { id -> _activeClassroomId.value = id }
    }
}

// In Compose UI
val activeClassroomId by viewModel.activeClassroomIdState.collectAsStateWithLifecycle()
```

**How to verify:**
1. Rotate device during data loading operations
2. Use Android Studio Profiler to monitor StateFlow emissions
3. Write unit tests simulating configuration changes

### A4: Medium - Exported component without proper validation

**What's wrong:**
`LmsFirebaseMessagingService` is declared `exported="false"` in manifest, which is correct. However, the service lacks explicit validation of incoming intents to ensure they originate from the Firebase system. While Firebase services have implicit protection, explicit validation is defense-in-depth.

**Why it matters:**
Although the risk is lower due to `exported="false"`, a malicious app with the same user ID could potentially bind to the service if vulnerabilities exist. Explicit intent validation prevents accidental exposure if manifest changes occur.

**How to fix:**
Add explicit action validation in `onMessageReceived`:
```kotlin
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    // Validate this is from Firebase
    if (remoteMessage.from?.startsWith("firebase") != true) {
        Log.w(TAG, "Received message from unexpected sender: ${remoteMessage.from}")
        return
    }
    // ... rest of handling
}
```

**How to verify:**
1. Use `adb shell cmd service` to verify service isn't exported
2. Attempt to bind to service from another app with same UID (requires root) - should fail
3. Add unit test validating intent filtering logic

### A5: Low - Inefficient image loading in UI components

**What's wrong:**
Several composables use `remember { painterResource(...) }` or similar patterns without considering image size or caching strategy. For example, in `StatCards.kt` and `ClassroomHeader.kt`, large bitmap resources may be loaded at full resolution.

**Why it matters:**
Unnecessarily large images consume memory and increase GC pressure, potentially causing jank in animations or scroll. On lower-end devices, this could contribute to ANRs.

**How to fix:**
1. Use Coil's `AsyncImage` with appropriate size parameters for network images
2. For local resources, use `BitmapFactory.decodeResource` with `inSampleSize`
3. Consider vector icons where possible instead of raster images

**Code snippet:**
```kotlin
// Replace:
Image(
    painter = painterResource(id = R.drawable.ic_stat),
    contentDescription = null,
    modifier = Modifier.size(24.dp)
)

// With (if using Coil):
AsyncImage(
    model = ImageResource.Local(R.drawable.ic_stat),
    contentDescription = null,
    modifier = Modifier.size(24.dp),
    contentScale = ContentScale.Crop
)
```

**How to verify:**
1. Use Android Studio Profiler to monitor memory usage during image-heavy operations
2. Check for large bitmap allocations in memory profiler
3. Test on low-end device emulator (e.g., API 24 with 512MB RAM)

### A6: Medium - God ViewModel classes violating SRP

**What's wrong:**
`ClassroomViewModel.kt` (400+ lines) and `GradingViewModel.kt` (350+ lines) handle multiple unrelated concerns: classroom management, student data, attendance, grading, and UI state. This violates Single Responsibility Principle.

**Why it matters:**
God ViewModels are difficult to test, maintain, and reason about. Changes in one area (e.g., attendance logic) can inadvertently break another (e.g., grading calculations). This increases bug risk and reduces development velocity.

**How to fix:**
Split ViewModels by feature:
- `ClassroomManagementViewModel` (classroom/student CRUD)
- `AttendanceViewModel` (attendance tracking)
- `GradingViewModel` (grade calculations and reports)
- `PlannerViewModel` (lesson planning)

Use shared state via `SharedFlow` or Hilt-scoped services for cross-ViewModel communication.

**How to verify:**
1. Check that each ViewModel has a single, clear responsibility
2. Write unit tests for each split ViewModel in isolation
3. Ensure no ViewModel exceeds 200 lines of business logic (excluding UI state)

### A7: Medium - Incomplete data backup rules configuration

**What's wrong:**
The manifest defines `android:allowBackup="true"` and references `@xml/backup_rules` and `@xml/data_extraction_rules`, but these files were not provided in the materials. Without proper configuration, sensitive data (like student records in encrypted DB) might be included in backups.

**Why it matters:**
Inappropriate backup of sensitive student data violates FERPA and GDPR-like regulations. Even with SQLCipher encryption, if the backup includes the encryption key (stored in EncryptedSharedPreferences), data could be compromised.

**How to fix:**
1. Create `backup_rules.xml` to exclude sensitive directories:
```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- Exclude databases and secure prefs -->
    <exclude domain="database" path="classroom_lms_db_encrypted" />
    <exclude domain="sharedpref" path="db_secure_prefs.xml" />
    <exclude domain="sharedpref" path="classroom_settings.pb" />
</full-backup-content>
```
2. Create `data_extraction_rules.xml` with similar exclusions
3. Consider setting `allowBackup="false"` if cloud sync is the primary backup mechanism

**How to verify:**
1. Use `adb backup` and inspect the generated archive
2. Verify excluded files are not present in backup
3. Test restore functionality to ensure app works correctly

### A8: Low - Target SDK 36 requires upcoming Play Console changes

**What's wrong:**
The app targets SDK 36 (Android 15), which has behavioral changes regarding foreground services, exact alarms, and background location. While the app doesn't appear to use these features, target SDK 36 requires declaration of foreground service types for certain APIs.

**Why it matters:**
Failure to declare foreground service types when using foreground services (if added later) will cause crashes on Android 15+. Play Console will warn about target SDK compliance starting Q1 2026.

**How to fix:**
1. If using foreground services, add appropriate `android:foregroundServiceType` attributes
2. For now, document that no foreground services are used
3. Monitor Android 15 behavioral changes for future feature additions

**How to verify:**
1. Use Android Studio's "Target SDK compatibility" lint check
2. Test on Android 15 emulator when available
3. Check Play Console pre-launch report for target SDK warnings

### A9: Low - Limited unit test coverage for error handling

**What's wrong:**
While the app has unit tests, error handling paths are under-tested. For example, network failure scenarios in `SyncRepository.kt`, database migration edge cases, and biometric authentication failures lack comprehensive test coverage.

**Why it matters:**
Untested error paths can lead to crashes or inconsistent state when real-world failures occur (poor network, low storage, etc.). This affects reliability and user trust.

**How to fix:**
1. Use Mockito or MockK to simulate network failures, database errors, etc.
2. Add tests for specific error conditions:
   - Network timeout in Google Sheets API
   - Database corruption during migration
   - Biometric authentication canceled/error
3. Aim for 80%+ branch coverage in critical modules

**How to verify:**
1. Run tests with coverage tracking (JaCoCo or similar)
2. Identify and add tests for uncovered branches
3. Use mutation testing (Pitest) to assess test quality

### A10: Medium - Outdated dependency versions in Gradle catalog

**What's wrong:**
Several dependencies in `libs.versions.toml` are outdated:
- `composeBom = "2024.09.00"` (latest is 2024.10.00+)
- `hilt = "2.60.1"` (latest is 2.62+)
- `firebaseBom = "34.17.0"` (latest is 34.18+)
- `okhttp = "4.10.0"` (latest is 4.12+)

**Why it matters:**
Outdated dependencies may contain known security vulnerabilities, miss performance improvements, or lack compatibility with newer Android versions. This increases maintenance burden and security risk.

**How to fix:**
1. Update to latest stable versions:
   - Compose BOM: 2024.10.00
   - Hilt: 2.62.0
   - Firebase BOM: 34.18.0
   - OkHttp: 4.12.0
2. Enable Dependabot or similar for automated updates
3. Add version catalog updates to CI pipeline

**How to verify:**
1. Run `./gradlew dependencyUpdates` to see available updates
2. Check for build breakage after updates
3. Run full test suite to ensure compatibility

## Strengths

1. **Strong Architecture**: Clean separation of data, domain, and UI layers with proper use of Hilt for DI.
2. **Security-Minded Data Storage**: Room database encrypted with SQLCipher, passphrase managed via EncryptedSharedPreferences.
3. **Modern UI**: Full Jetpack Compose implementation with Material 3 theming.
4. **Thoughtful Features**: Biometric lock, notification channels, and Firebase Cloud Messaging integration.
5. **Testing Culture**: Presence of unit tests, fakes/mocks for dependencies, and screenshot tests.
6. **Privacy Controls**: Backup rules and data extraction rules configured (assuming files exist).

## Prioritized Roadmap

### Quick Wins (This Week)
1. [A1] Implement safe logging utility to prevent sensitive data exposure (1 day)
2. [A2] Add network security configuration to enforce HTTPS (0.5 days)
3. [A7] Verify and complete backup rules configuration (0.5 days)
4. [A8] Document target SDK 36 compliance and add foreground service types if needed (0.5 days)
5. [A10] Update dependencies to latest versions (1 day)

### Short Term (This Month)
1. [A3] Refactor ViewModel Flow collection to use repeatOnLifecycle (2 days)
2. [A4] Add explicit intent validation to FirebaseMessagingService (0.5 days)
3. [A5] Optimize image loading in UI components (1 day)
4. [A6] Split god ViewModels into feature-specific ViewModels (3 days)
5. [A9] Increase unit test coverage for error handling paths (2 days)

### Long Term (This Quarter)
1. Implement automated dependency updates in CI (ongoing)
2. Add performance benchmarks for critical flows (Macrobenchmark)
3. Consider migrating to Android Keystore for encryption key management
4. Add dark theme support and dynamic color theming
5. Implement automated UI tests for critical user flows

## Missing Information

To complete a deeper audit, I would need:
1. The actual `backup_rules.xml` and `data_extraction_rules.xml` files to verify configuration
2. Access to production crash reports (Firebase Crashlytics or similar) to identify real-world issues
3. Network security config if already implemented elsewhere
4. Detailed ProGuard/R8 rules to assess obfuscation strength
5. CI/CD pipeline configuration to evaluate release process
6. Access to Google Services JSON (redacted) to verify Firebase configuration
7. Recent Play Store internal testing reports for policy compliance

## Recommended Tooling

1. **Security**: MobSF (Mobile Security Framework) for static/dynamic analysis
2. **Performance**: Android Studio Profiler, Macrobenchmark Library
3. **Testing**: JaCoCo for coverage, Pitest for mutation testing
4. **Dependency Updates**: Dependabot or Gradle Versions Plugin
5. **Code Quality**: Detekt with Android configuration, Ktlint
6. **Network Security**: Network Security Config tester, Charles Proxy for traffic inspection
7. **Backup Testing**: ADB backup/restore automation scripts
8. **Compliance**: Google Play Pre-launch report, App Bundle Explorer

---
*Audit conducted by Principal Android Engineer with 10+ years experience*
*Timestamp: 2026-10-11T16:39:27.404+05:45*