---
id: TASK-35.4
title: Autocomplete Integration
status: Done
assignee:
  - '@agent'
created_date: '2026-07-14 15:41'
updated_date: '2026-07-14 15:47'
labels: []
dependencies: []
parent_task_id: TASK-35
priority: medium
ordinal: 39000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
AutoComplete-Bibliothek von RSyntaxTextArea einbinden und einen CompletionProvider für Sieve-Sprachelemente bauen.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [x] #1 com.fifesoft:autocomplete als Maven-Dependency hinzugefügt
- [x] #2 SieveCompletionProvider liefert alle Sieve-Commands, Tests und Tags als Vorschläge
- [x] #3 CompletionProvider zeigt Sieve-Schlüsselwörter mit Ctrl+Space an
- [x] #4 Autocomplete-Fenster öffnet sich automatisch bei Eingabe (AutoActivationEnabled)
- [x] #5 Installation auf der RSyntaxTextArea in Application.java
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented SieveCompletionProvider and integrated in Application.java
<!-- SECTION:NOTES:END -->
