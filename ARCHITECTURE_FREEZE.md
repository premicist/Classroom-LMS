# Architecture freeze: ClassroomViewModel

**Status:** Frozen for new feature logic.

## Rule

`app/src/main/java/com/example/ui/viewmodel/ClassroomViewModel.kt` is a god class (~1.7k lines).

**Do not add** new screens, CRUD flows, sync paths, or export formats into this file.

Allowed only:
- Bugfixes that cannot be isolated elsewhere
- Refactors that extract code out of it
- Wiring calls to new focused ViewModels / UseCases

## Where new work should go

| Concern | Target |
|---------|--------|
| Attendance, homework day ops | `AttendanceViewModel` + UseCases |
| Grading / submissions | `GradingViewModel` |
| Planner / lesson plans / daily log | `PlannerViewModel` |
| Discipline / interventions | `StudentSupportViewModel` |
| Sheets / cloud sync | `SyncRepository` + thin ViewModel |
| PDF / CSV export | existing util classes; call from feature VM |
| Shared roster / active classroom | small session holder, not one mega state forever |

Next architectural step (separate task): Hilt + one ViewModel per major nav tab.
