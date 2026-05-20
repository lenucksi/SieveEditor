---
id: TASK-29
title: Extract ActionConnect dialog logic for testability
status: To Do
assignee: []
created_date: '2026-05-20 19:58'
labels: []
dependencies:
  - TASK-24
priority: high
ordinal: 29000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ActionConnect (2.4% coverage, 668 instr) has 300+ lines of Swing dialog construction inside actionPerformed(). The dialog creates JComboBox, JTextField, JPasswordField, JButton instances and wires up 5+ ActionListeners inline. Zero testability without a display.

Refactoring plan:
1. Extract ConnectionDialogModel - data class holding all dialog state (server, port, username, password, selected profile, profiles list)
2. Extract ConnectionDialogView interface for the Swing dialog
3. Extract ConnectionDialogPresenter with all business logic (save/load profile, connect, rename, delete)
4. ActionConnect.actionPerformed() creates dialog, delegates to presenter
5. Write presenter tests with Mockito: profile CRUD, connect flow, error handling
6. The dialog construction itself becomes thin: just layout + wiring

Reference patterns: MVP with Humble Dialog pattern from Swing testability research.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria
<!-- AC:BEGIN -->
- [ ] #1 ConnectionDialogModel extracted with all dialog state
- [ ] #2 ConnectionDialogView interface extracted
- [ ] #3 ConnectionDialogPresenter handles all business logic
- [ ] #4 Existing ActionConnect tests still pass
- [ ] #5 Presenter achieves ≥ 80% instruction coverage
- [ ] #6 ActionConnect.actionPerformed() is ≤ 60 lines
- [ ] #7 Full test suite passes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Extract  class: server, port, username, password, selectedProfile, profiles list, currentDisplayedProfile
2. Extract  interface: show(), close(), showError(String), getFieldValues()→Model, setFieldValues(Model)
3. Extract : handleOk(), handleProfileChange(), handleNewProfile(), handleDeleteProfile(), handleRenameProfile(), handleProfileSwitch()
4. Move all PropertiesSieve interaction from anonymous listeners into Presenter methods
5. Slim down ActionConnect.actionPerformed to ~50 lines: create dialog → create presenter → wire
6. Write tests:
   - Presenter.handleOk() with valid data → saves profile, connects, saves last used
   - Presenter.handleOk() with invalid port → shows error
   - Presenter.handleNewProfile() with valid name → adds to list
   - Presenter.handleNewProfile() with duplicate name → shows error
   - Presenter.handleDeleteProfile() → confirms and removes
   - Presenter.handleRenameProfile() → validates and renames
   - Profile switch → saves current, loads new
7. Run mvn test + jacoco:report, verify ActionConnect ≥ 80%
<!-- SECTION:PLAN:END -->
