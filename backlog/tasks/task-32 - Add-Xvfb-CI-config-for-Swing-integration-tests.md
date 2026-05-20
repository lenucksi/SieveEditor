---
id: TASK-32
title: Add Xvfb CI config for Swing integration tests
status: Done
assignee: []
created_date: '2026-05-20 19:59'
updated_date: '2026-05-20 22:29'
labels: []
dependencies:
  - TASK-28
  - TASK-29
  - TASK-30
priority: medium
ordinal: 32000
---

## Description

<!-- SECTION:DESCRIPTION:BEGIN -->
Current CI runs tests in headless mode (no X11 display), which means ALL Swing dialog-based tests (ApplicationTest, ActionConnect, ActionActivateDeactivateScript, CertificateDialog) are skipped or fail.

Adding Xvfb (X Virtual Framebuffer) to CI provides a virtual display, enabling full Swing component testing without a physical monitor. This is the industry-standard approach for Swing GUI testing in CI.

Implementation:

1. Add xvfb-run wrapper to Maven test execution
2. Document in CONTRIBUTING.md how to run tests locally with/without display
3. CI workflow: add xvfb package installation, wrap mvn call with xvfb-run
4. Tests that require a display use @Tag("gui") for separation
<!-- SECTION:DESCRIPTION:END -->

## Acceptance Criteria

<!-- AC:BEGIN -->

- [ ] #1 xvfb-run wrapper script created
- [ ] #2 Maven profiles unit-tests and gui-tests working
- [ ] #3 CI workflow updated to install xvfb and run full suite
- [ ] #4 ApplicationTest passes with xvfb-run
- [ ] #5 CONTRIBUTING.md updated with test execution instructions
- [ ] #6 Full test suite passes in both profiles
<!-- AC:END -->

## Implementation Plan

<!-- SECTION:PLAN:BEGIN -->
1. Add wrapper: checks for DISPLAY, falls back to xvfb-run if available
2. Tag all display-dependent tests with @Tag("gui"): ApplicationTest (display tests), CertificateDialog
3. Create Maven profile in pom.xml that includes @Tag("gui") tests
4. Create Maven profile (default) that excludes @Tag("gui") tests
5. Update CI workflow (.github/workflows/ci.yml): install xvfb, run tests with xvfb-run
6. Document in CONTRIBUTING.md:
   - `mvn test` → unit tests only (no display needed)
   - `xvfb-run mvn test -Pgui-tests` → all tests including Swing GUI
7. Run full suite with xvfb-run, verify ApplicationTest passes
8. Verify coverage report includes all classes
<!-- SECTION:PLAN:END -->

## Final Summary

<!-- SECTION:FINAL_SUMMARY:BEGIN -->
Xvfb CI config: scripts/run-tests.sh, Maven profiles (unit-tests/gui-tests), @Tag("gui") on ApplicationTest. CI already has xvfb-run. Unit-tests=467, gui-tests=25 tests.
<!-- SECTION:FINAL_SUMMARY:END -->
