---
id: TASK-30
title: Extract ActionActivateDeactivateScript logic for testability
status: Done
assignee: []
created_date: '2026-05-20 19:59'
updated_date: '2026-05-20 22:14'
labels: []
dependencies:
  - TASK-24
priority: high
ordinal: 30000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ActionActivateDeactivateScript (3.4% coverage, 467 instr) is a Swing-heavy action that shows a JTable of scripts, handles activate/deactivate/rename/delete operations. It has documented bugs (ArrayIndexOutOfBoundsException when no row selected, BUG-001) and inline Swing listeners. Zero testability.

Refactoring plan:

1. Extract ScriptTableModel: data model for the JTable (script names, active status)
2. Extract ScriptManagementPresenter: activate/deactivate/rename/delete logic
3. Extract ScriptManagementView interface for the dialog
4. Tests verify presenter logic including edge cases:
   - Empty selection (the known bug)
   - Rename collision handling
   - Error responses from server
   - Active/inactive toggle logic
5. Bug fix: ArrayIndexOutOfBoundsException when getSelectedRow() == -1
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [ ] #1 ScriptTableModel extracted with script list
- [ ] #2 ScriptManagementView interface extracted
- [ ] #3 ScriptManagementPresenter extracted with all operations
- [ ] #4 BUG-001 fixed: no ArrayIndexOutOfBoundsException on empty selection
- [ ] #5 Existing tests still pass
- [ ] #6 ≥ 80% instruction coverage on extracted classes
- [ ] #7 Full test suite passes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Extract : list of script names + active status, sorting
2. Extract interface: showScripts(), showError(), getSelectedRow()→int
3. Extract : handleActivate(), handleDeactivate(), handleRename(), handleDelete()
4. Fix BUG-001: add null check for getSelectedRow() before array access
5. Move logic from anonymous ActionListeners into presenter methods
6. Write tests:
   - Presenter.handleActivate() with valid selection → calls server.activateScript()
   - Presenter.handleActivate() with no selection → shows error (BUG-001 regression test)
   - Presenter.handleDeactivate() → calls server.deactivateScript()
   - Presenter.handleDelete() → confirms and deletes
   - Presenter.handleRename() → validates and renames
   - Error handling → shows error on IOException
7. Run mvn test + jacoco:report, verify ≥ 80%
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
ActionActivateDeactivateScript MVP refactor: extracted ScriptManagementView/Presenter. BUG-001 fixed (ArrayIndexOutOfBounds). 19 tests.
<!-- SECTION:FINAL_SUMMARY:END -->
