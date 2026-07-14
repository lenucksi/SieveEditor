---
id: TASK-35.3
title: "text: Heredoc & Cross-Line State Tracking"
status: Done
assignee:
  - "@agent"
created_date: 2026-07-14 15:41
updated_date: 2026-07-14 15:56
completed_date: 2026-07-14 15:56
labels: []
dependencies: []
parent_task_id: TASK-35
priority: medium
ordinal: 38000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Multi-line String-Support für text:-Heredoc-Syntax. Erfordert Cross-Line-State-Tracking via startTokenType, da der Terminator (^. am Zeilenanfang) erst in folgenden Zeilen erkannt werden kann.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [x] #1 text:-Heredoc-Start wird erkannt (text: am Ende einer Zeile)
- [x] #2 Content-Zeilen werden als LITERAL_BACKQUOTE tokenisiert
- [x] #3 Terminator (Zeile mit nur einem .) beendet den Heredoc
- [x] #4 Dot-Stuffing (.. → .) wird korrekt behandelt
- [x] #5 State wird über startTokenType zeilenübergreifend getrackt
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented text: heredoc multi-line string support with cross-line state tracking via inMultilineString field. Detects text: at end of line, tokenizes content lines as LITERAL_BACKQUOTE, terminates on line with just ., handles dot-stuffing (.. not treated as terminator)
<!-- SECTION:NOTES:END -->
