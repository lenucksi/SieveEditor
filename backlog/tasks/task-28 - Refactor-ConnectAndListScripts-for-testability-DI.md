---
id: TASK-28
title: Refactor ConnectAndListScripts for testability (DI)
status: Done
assignee: []
created_date: '2026-05-20 19:58'
updated_date: '2026-05-20 22:14'
labels: []
dependencies:
  - TASK-24
priority: high
ordinal: 28000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
ConnectAndListScripts (12.8% coverage, 743 instr) creates ManageSieveClient internally in connect(), making all script operations (putScript, getListScripts, getScript, checkScript, activateScript, deactivateScript, rename, deleteScript) impossible to unit test without a real server.

Refactoring plan:

1. Extract interface SieveConnection with all public methods
2. Add constructor that accepts a SieveConnection factory
3. Keep default no-arg constructor using real factory for production
4. Add tests with mocked SieveConnection covering all script operations
5. Test auto-reconnect logic, keep-alive lifecycle, error handling

This is the single highest-impact refactoring for coverage: 743 instructions, currently 12.8% → target 85%.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [ ] #1 SieveConnection interface extracted covering all public operations
- [ ] #2 ManageSieveClientFactory interface extracted
- [ ] #3 Constructor injection of factory available, no-arg constructor still works
- [ ] #4 All 20+ existing ConnectAndListScripts tests still pass
- [ ] #5 New tests achieve ≥ 85% instruction coverage on ConnectAndListScripts
- [ ] #6 Full test suite (mvn test) passes with 0 failures
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Extract interface from public methods
2. Extract interface with single method
3. Add constructor + keep no-arg constructor for production
4. Remove field creation from connect(), delegate to factory
5. Write comprehensive tests:
   - mock factory returns mock ManageSieveClient + mock ManageSieveResponse
   - putScript/getListScripts/getScript/checkScript returns correct data
   - activateScript/deactivateScript/rename/deleteScript delegate correctly
   - error handling: when response.isOk() is false, IOException thrown
   - connection lifecycle: connect → operation → logout
   - auto-reconnect: ensureConnection reconnects when client disconnected
   - keep-alive: timer fires NOOP, disable/enable lifecycle
6. Run mvn test + jacoco:report, verify ConnectAndListScripts ≥ 85%
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
ConnectAndListScripts DI refactor: extracted SieveConnectionFactory interface, constructor injection. Coverage 12.8%→84.1%. 31 new tests (56 total). Highest single coverage gain.
<!-- SECTION:FINAL_SUMMARY:END -->
