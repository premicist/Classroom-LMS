# Architecture freeze: ClassroomViewModel

**Status:** Frozen for new feature logic.

## Rule

`app/src/main/java/com/example/ui/viewmodel/ClassroomViewModel.kt` is a large god class (~1.8k lines; do not grow further).

**Do not add** new screens, CRUD flows, sync paths, or export formats into this file.

Allowed only:
- Bugfixes that cannot be isolated elsewhere
- Refactors that extract code out of it
- Wiring calls to new focused ViewModels / UseCases

## Where work belongs / Status

| Concern | Target | Status |
|---------|--------|--------|
| Exams & Exam Marks | `ExamViewModel` (`@HiltViewModel`) | ✅ Extracted & wired (Exam ownership fully removed from ClassroomViewModel) |
| Attendance, homework day ops | `AttendanceViewModel` (`@HiltViewModel`) | ✅ Extracted & wired |
| Grading / submissions | `GradingViewModel` (`@HiltViewModel`) | ✅ Extracted & wired |
| Planner / lesson plans / daily log / timetable | `PlannerViewModel` (`@HiltViewModel`) | ✅ Extracted & wired |
| Discipline / interventions / live assessment | `StudentSupportViewModel` (`@HiltViewModel`) | ✅ Extracted & wired |
| Sheets / cloud sync | `SyncViewModel` (`@HiltViewModel`) | ✅ Extracted & wired |
| Analytics & custom reports | `AnalyticsViewModel` (`@HiltViewModel`) | ✅ Extracted & wired |
| PDF / CSV export | existing util classes; called from domain ViewModels | ✅ Complete |
| Shared roster / active classroom | reactive flow observation via `PreferencesManager` | ✅ Complete |

Hilt dependency injection is now active (`LmsApplication`, `AppModule`, `@AndroidEntryPoint` on `MainActivity`). All future ViewModel extractions should be created with `@HiltViewModel`.
