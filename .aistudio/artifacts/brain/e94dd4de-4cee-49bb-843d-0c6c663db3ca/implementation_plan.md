# Classroom LMS — Full Architecture Freeze & Domain Decoupling Plan

A comprehensive architectural modernization sprint completing the decoupling roadmap defined in `ARCHITECTURE_FREEZE.md`. This sprint extracts the remaining domains into dedicated `@HiltViewModel` classes: **`StudentSupportViewModel`**, **`SyncViewModel`**, and **`AnalyticsViewModel`**, leaving `ClassroomViewModel` as a lean session coordinator.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed for this implementation:

- **Confirmed Decision 1 (Scope)**: Full Architecture Freeze Completion across all remaining domains:
  1. **Discipline, Interventions & Live Assessment** (`StudentSupportViewModel`)
  2. **Google Sheets Cloud Sync** (`SyncViewModel`)
  3. **Classroom Analytics & Custom Reports** (`AnalyticsViewModel`)
- **Confirmed Decision 2 (Student Support & Reporting)**:
  - Extract and wire `StudentSupportViewModel` via Hilt.
  - Implement formatted discipline incident slips (PDF export) and student trajectory charts directly inside the student detail workflow.
- **Confirmed Decision 3 (Zero Regression & Compatibility)**:
  - All extracted ViewModels observe reactive flows (`PreferencesManager.activeClassroomIdFlow`, Room DAOs).
  - Existing UI screens maintain backward compatibility while leveraging Hilt injection.

---

## 1. Overview & Core Concept

- **What It Does**: Concludes the transition of Classroom LMS from a monolithic ViewModel architecture into a fully modular, decoupled MVVM architecture powered by Dagger Hilt.
- **Target Persona**: Educators and school administrators managing student behavior, interventions, multi-device Google Sheets sync, and comprehensive school auditing reports.
- **Key Value**: 
  - Complete elimination of state cross-talk between student disciplinary logs, cloud sync operations, and academic views.
  - High performance, reactive UI updates without triggering full-screen recompositions.
  - Isolated unit testing for each domain.

---

## 2. User Experience & Visual Design

- **Discipline & Behavioral Incident Management**:
  - Intuitive dialog for recording disciplinary incidents (date, incident category, description, corrective actions, parent notification status).
  - Quick action to export an official **Discipline Incident Slip PDF** for administration and parent meetings.
- **Student Progress Trajectory Charts**:
  - Visual trajectory graphs displaying academic score trends, attendance consistency, and behavioral metrics in `StudentDetailDialog`.
- **Cloud Synchronization Hub**:
  - Clear sync state indicators (idle, authenticating, syncing roster, pushing grades/attendance, success/error).
- **Custom Report Generation Center**:
  - Multi-category report builder (Attendance, Gradebook, Interventions, NEB Standing) with live preview and PDF export.

---

## 3. Key Architectural Decisions

- **Decision 1: Provide Discipline, Intervention, and LiveAssessment DAOs in `AppModule`**
  - *Chosen Approach*: Ensure `DisciplineDao`, `InterventionDao`, and `LiveAssessmentDao` are provided as singletons in `AppModule.kt`.
  - *Why*: Provides clean, direct constructor injection into `StudentSupportViewModel`.

- **Decision 2: Dedicated Domain UI States**
  - *Chosen Approach*: Create `StudentSupportUiState`, `SyncUiState`, and `AnalyticsUiState`.
  - *Why*: Isolates domain-specific loading spinners, dialog states, and notification messages.

- **Decision 3: Maintain Unified Experience with Dual-Binding**
  - *Chosen Approach*: UI components accept extracted ViewModels via `hiltViewModel()` default parameters while preserving existing callback contracts for dialog sheets.
  - *Why*: Guarantees zero runtime regressions across drawer navigation, top bar actions, and bottom sheets.

---

## 4. Technical Architecture & Component Structure

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                 AppModule                                   │
│                        (Hilt Singleton Container)                           │
├──────────────────────────────────────┬──────────────────────────────────────┤
│ • AppDatabase & PreferencesManager   │ • DisciplineDao & InterventionDao    │
│ • StudentDao & ClassroomDao          │ • LiveAssessmentDao & SyncRepository │
└──────────┬───────────────────────────┼──────────────────────────┬───────────┘
           │                           │                          │
           ▼                           ▼                          ▼
┌──────────────────────┐    ┌─────────────────────┐    ┌──────────────────────┐
│StudentSupportViewModel│   │    SyncViewModel    │    │  AnalyticsViewModel  │
│   (@HiltViewModel)   │    │  (@HiltViewModel)   │    │   (@HiltViewModel)   │
├──────────────────────┤    ├─────────────────────┤    ├──────────────────────┤
│• Discipline logs     │    │• Google OAuth auth  │    │• Student trajectories│
│• Action interventions│    │• Roster pull sync   │    │• Class difficulty map│
│• Live assessments    │    │• Grades/attend push │    │• Custom PDF reports  │
│• Incident slip PDF   │    │• Conflict alerts    │    │• Export preview      │
└──────────┬───────────┘    └──────────┬──────────┘    └──────────┬───────────┘
           │                           │                          │
           ▼                           ▼                          ▼
┌──────────────────────┐    ┌─────────────────────┐    ┌──────────────────────┐
│ StudentDetailDialog  │    │ LinkGoogleSheetDlg  │    │   AnalyticsScreen    │
│ DisciplineLogDialog  │    │ ClassroomHeaderSync │    │ GenerateReportDialog │
│ InterventionDialog   │    │                     │    │                      │
└──────────────────────┘    └─────────────────────┘    └──────────────────────┘
```

---

## 5. Phased Implementation Plan

### Step 1: Expand AppModule & Repositories
- Register `DisciplineDao`, `InterventionDao`, and `LiveAssessmentDao` providers in `AppModule.kt`.
- Ensure `AuthManager` and `SyncRepository` are cleanly accessible.

### Step 2: Implement `StudentSupportViewModel` & Incident Reporting
- Create `StudentSupportViewModel.kt` with `StudentSupportUiState`.
- Move discipline CRUD, intervention plans, and live classroom formative assessment logic.
- Implement incident slip PDF generation and student trajectory calculation.
- Wire `StudentDetailDialog`, `DisciplineLogDialog`, and `InterventionDialog`.

### Step 3: Implement `SyncViewModel` & Google Sheets Pipeline
- Create `SyncViewModel.kt` with `SyncUiState`.
- Manage Google Sign-In state, OAuth token retrieval, roster synchronization, and two-way data push.
- Wire `LinkGoogleSheetDialog` and sync trigger actions in `ClassroomHeader`.

### Step 4: Implement `AnalyticsViewModel` & Custom Report Center
- Create `AnalyticsViewModel.kt` with `AnalyticsUiState`.
- Manage classroom analytics, student trajectory calculations, difficulty areas, and custom report builder logic.
- Wire `AnalyticsScreen` and `GenerateReportDialog`.

### Step 5: Finalize `ClassroomViewModel` & Verification
- Streamline `ClassroomViewModel` to serve solely as a session coordinator.
- Finalize `ARCHITECTURE_FREEZE.md` marking all domains as completed.
- Verify the build with `compile_applet` and execute unit test suite via `:app:testDebugUnitTest`.
