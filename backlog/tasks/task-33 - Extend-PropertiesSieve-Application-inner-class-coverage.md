---
id: TASK-33
title: Extend PropertiesSieve + Application inner class coverage
status: Done
assignee: []
created_date: '2026-05-20 19:59'
updated_date: '2026-05-20 22:14'
labels: []
dependencies:
  - TASK-26
priority: medium
ordinal: 33000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
PropertiesSieve (72.3%, 896 instr) still has untested paths in encryption algorithm selection (algorithm tiers) and port handling. Application (61.6%, 940 instr) has 5+ anonymous inner classes with low/zero coverage (ComponentAdapter 36.9%, Runnable 0%, AbstractAction 27-50%).

Refactoring plan:

1. PropertiesSieve: Test createEncryptor() algorithm switching paths with reflection
2. Application inner classes: Extract named inner classes or test via reflection
3. Add edge case tests for port validation, null handling
4. Target: PropertiesSieve 90%+, Application 75%+
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [ ] #1 PropertiesSieve encryption algorithm tier switching tested
- [ ] #2 PropertiesSieve port edge cases tested
- [ ] #3 Application.inner classes ComponentAdapter and DocumentListener tested
- [ ] #4 PropertiesSieve ≥ 90% instruction coverage
- [ ] #5 Application ≥ 75% instruction coverage
- [ ] #6 Full test suite passes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. PropertiesSieve encryption:
   - createEncryptor() has algorithm tier selection (Tier 1: AES-CBC-PBE, Tier 2: AES-CBC, Tier 3: AES)
   - Use reflection or configuration manipulation to trigger each tier
   - Test that each tier produces an encryptor that can encrypt/decrypt
2. PropertiesSieve edge cases:
   - Port parsing edge cases (negative, overflow, zero)
   - File permissions edge cases (read-only directory)
   - Migration edge cases (corrupt legacy file, mixed content)
3. Application inner classes:
   - ComponentAdapter: test componentResized event triggers resize logic
   - Runnable (initComponents): hard to test, document limitation
   - AbstractAction instances: test via reflection on the action map
   - DocumentListener: test via programmatic text changes
4. Write tests using @TempDir + reflection + system property manipulation
5. Run mvn test + jacoco:report, verify targets met
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
PropertiesSieve 72.3%→93.3% (encryption tiers tested). Application 61.6%→80.3% (inner class tests). 21 new tests.
<!-- SECTION:FINAL_SUMMARY:END -->
