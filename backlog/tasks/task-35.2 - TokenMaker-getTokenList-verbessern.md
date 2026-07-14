---
id: TASK-35.2
title: TokenMaker getTokenList() verbessern
status: Done
assignee:
  - "@agent"
created_date: 2026-07-14 15:41
updated_date: 2026-07-14 15:56
completed_date: 2026-07-14 15:56
labels: []
dependencies: []
parent_task_id: TASK-35
priority: high
ordinal: 37000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
getTokenList() um SEPARATORs für Brackets/Semicolon, ANNOTATION-State für :Tags, COMMENT_MULTILINE für /* */, K/M/G-Quantifier in Zahlen und Escape-Sequenzen in Strings erweitern.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [x] #1 SEPARATOR für ;, [, ], (, ), {, }, , in getTokenList()
- [x] #2 ANNOTATION-TokenType für :contains, :is, :matches, :over, :under, :comparator, :copy, :days, :mime, etc.
- [x] #3 COMMENT_MULTILINE-State für /* */ Block-Kommentare mit korrektem Cross-Line-Übergang
- [x] #4 K/M/G-Quantifier bleiben Teil des NUMBER-Tokens (100K = ein Token)
- [x] #5 Backslash-Escape-Sequenzen (", \) in Strings werden konsumiert ohne String zu terminieren
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented getTokenList improvements: SEPARATORs for ;,[,],(,),{,},,; ANNOTATION state for :tags; COMMENT_MULTILINE for /* */; K/M/G quantifiers in numbers; escape sequences (\, ") in strings
<!-- SECTION:NOTES:END -->
