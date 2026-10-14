# Architecture boundaries

## ClassroomViewModel

`app/src/main/java/com/aistudio/classroomlms/ui/viewmodel/ClassroomViewModel.kt`
coordinates the active classroom session and shared classroom UI state. Keep new
domain behavior out of this ViewModel.

Allowed changes:

- Bug fixes and changes to existing coordination behavior
- Extracting existing logic into a focused ViewModel, repository, or use case
- Wiring the coordinator to focused feature ViewModels

New CRUD, sync, export, and other feature logic belongs in a focused owner with
tests for its behavior.

## Current feature owners

| Concern | Owner |
|---------|-------|
| Attendance and homework day operations | `AttendanceViewModel` |
| Grading and submissions | `GradingViewModel` |
| Exams and exam marks | `ExamViewModel` |
| Planner, lesson plans, daily logs, and schedules | `PlannerViewModel` |
| Discipline, interventions, and live assessment | `StudentSupportViewModel` |
| Google Sheets linking and transfer actions | `SyncViewModel` and `SyncRepository` |
| Analytics and custom reports | `AnalyticsViewModel` |
| Roster and classroom management | `RosterViewModel` and `ClassroomRepository` |
| Database backup and restore | `SettingsViewModel` and `DatabaseBackupManager` |
| PDF and spreadsheet formatting | focused utility/export classes |

All application ViewModels use Hilt constructor injection. The Room database
uses explicit migrations; do not introduce destructive migrations for user data.

## Product and data boundaries

- The app remains usable without Google sign-in; Google authorization is only
  needed for Google Sheets.
- Google Sheets account, linking, upload, and download controls belong in
  Settings. Upload and download are separate actions.
- Classes navigation owns create, switch, edit, archive, and restore operations.
- Archiving hides a class from active class lists without deleting its records.
- Attendance, grades, planner data, and other day-to-day work remain local-first.
- Do not send student records or other classroom PII to product analytics.

## Known follow-up

`AuthManager` still uses the deprecated Google Sign-In / `GoogleAuthUtil` token
flow to request Sheets scope. Keep that flow optional and isolated from local
classroom use. Replace it only after validating the current Google Authorization
API path against the app's required Sheets scopes and consent behavior.
