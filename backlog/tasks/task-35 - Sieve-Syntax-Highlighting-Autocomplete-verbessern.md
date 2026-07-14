---
id: TASK-35
title: Sieve Syntax Highlighting & Autocomplete verbessern
status: To Do
assignee: []
created_date: 2026-07-14 15:41
updated_date: 2026-07-14 16:07
labels: []
dependencies: []
references:
  - docs/subagent-research-reports/sieve-tokenmaker-analysis.md
priority: high
ordinal: 35000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Das SieveTokenMaker ist rudimentär (nur 'if' als Keyword). Basierend auf der Subagent-Analyse (docs/subagent-research-reports/sieve-tokenmaker-analysis.md) soll das komplette Sieve-Sprach-Syntax-Highlighting inkl. aller gängigen RFC-Erweiterungen implementiert werden. Zusätzlich kommt ein CompletionProvider für Autocomplete dazu.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [x] #1 Alle Sieve-Commands, Tests, Tags und Operatoren werden mit korrekten TokenTypes hervorgehoben
- [x] #2 Tags mit :Prefix (:contains, :is, :over, etc.) werden als ANNOTATION hervorgehoben
- [x] #3 Block-Kommentare /* */ werden unterstützt
- [x] #4 Zahlen mit Quantifier (100K, 5M, 1G) bleiben ein Token
- [x] #5 Escape-Sequenzen in Strings werden korrekt behandelt
- [x] #6 Autocomplete funktioniert für alle Sieve-Konstrukte
- [x] #7 Bestehende Tests bleiben grün, neue Tests decken die Erweiterungen ab
- [x] #8 Separator/Bracket-Matching funktioniert für [], (), {}, ;
<!-- AC:END -->

## Definition of Done

<!-- DOD:BEGIN -->

- [ ] #1 mvn test läuft durch
- [ ] #2 Alle ACs sind angehakt
- [ ] #3 TokenMaker-Testabdeckung >90%
<!-- DOD:END -->
