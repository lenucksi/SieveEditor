---
id: TASK-35.1
title: TokenMaker Keyword-Map erweitern
status: Done
assignee:
  - '@agent'
created_date: '2026-07-14 15:41'
updated_date: '2026-07-14 15:44'
labels: []
dependencies: []
parent_task_id: TASK-35
priority: high
ordinal: 36000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
getWordsToHighlight() mit allen Sieve-Keywords befüllen, kategorisiert nach TokenTypes. TokenMap auf case-insensitive umstellen.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [x] #1 RESERVED_WORD: if, elsif, else, stop, return, break, foreverypart, require
- [x] #2 FUNCTION: keep, discard, fileinto, redirect, reject, ereject, vacation, notify, denotify, addflag, removeflag, setflag, set, include, global, mailbox, duplicate, cancel, convert, replace, enclose, extracttext, deleteheader, addheader
- [x] #3 RESERVED_WORD_2: address, envelope, exists, header, size, body, date, currentdate, environment, spamtest, virustest, hasflag, mailboxexists, metadata, metadataexists, servermetadata, servermetadataexists, ihave, regex, list, true, false
- [x] #4 OPERATOR: allof, anyof, not
- [x] #5 TokenMap(true) für case-insensitive Erkennung
<!-- AC:END -->

## Implementation Notes

<!-- SECTION:NOTES:BEGIN -->
Implemented complete keyword map with all categories
<!-- SECTION:NOTES:END -->
