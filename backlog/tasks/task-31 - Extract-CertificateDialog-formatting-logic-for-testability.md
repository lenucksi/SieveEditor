---
id: TASK-31
title: Extract CertificateDialog formatting logic for testability
status: Done
assignee: []
created_date: '2026-05-20 19:59'
updated_date: '2026-05-20 22:14'
labels: []
dependencies:
  - TASK-23
priority: medium
ordinal: 31000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
CertificateDialog (0% coverage, 500 instr) extends JDialog and cannot be instantiated in headless mode. Its fingerprint formatting logic and certificate info extraction are tightly coupled to Swing components. However, 60% of the instruction volume is pure data formatting: fingerprint display, certificate detail extraction, date formatting.

Refactoring plan:

1. Extract CertificateInfoFormatter: static methods for formatting cert details
   - getFormattedFingerprintMultiline(String fingerprint) already exists as private
   - ExtractCertificateDetails: subject, issuer, validity dates, serial from X509Certificate
2. Move formatFingerprintMultiline() into formatter (currently private)
3. Write tests for CertificateInfoFormatter covering all formatting edge cases
4. The JDialog itself remains untestable in headless mode but becomes a thin shell

Current CertificateDialog: 500 instr, 0% coverage
After refactoring: ~200 instr in dialog (0%), ~300 instr in formatter (100%)
Goal: 60% effective coverage on the class.
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [ ] #1 CertificateInfoFormatter class created
- [ ] #2 formatFingerprintMultiline made accessible and fully tested
- [ ] #3 All existing CertificateDialogTest enum + multiline tests still pass
- [ ] #4 New formatter achieves 100% instruction coverage
- [ ] #5 CertificateDialog effective coverage ≥ 60%
- [ ] #6 Full test suite passes
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Create class with package-private access
2. Move from CertificateDialog to formatter
3. Extract methods: getSubjectDisplay(), getIssuerDisplay(), getValidityPeriod(), getSerialDisplay(), getFingerprintDisplay()
4. Write tests:
   - formatFingerprintMultiline with 32 hex pairs → 4 lines of 8 pairs each
   - formatFingerprintMultiline with 8 pairs → single line
   - formatFingerprintMultiline with 0 pairs → empty string
   - formatFingerprintMultiline with odd number of pairs
   - Subject display with null/empty/unicode
   - Issuer display with various DN formats
   - Validity period formatting
   - Serial number formatting
5. Slim down CertificateDialog.initComponents() to use formatter
6. Run mvn test + jacoco:report, verify ≥ 60% on CertificateDialog package
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
CertificateInfoFormatter extracted from CertificateDialog. 100% coverage (20 tests). Dialog reduced to thin Swing shell.
<!-- SECTION:FINAL_SUMMARY:END -->
