---
id: decision-7
title: Conventional commits for automated changelog
date: '2026-05-20 14:58'
status: accepted
---
## Context

Need for automated changelog generation and semantic versioning. Manual changelogs were inconsistent and error-prone.

## Decision

Enforce conventional commits format (feat, fix, docs, test, chore, refactor, perf, security, deps, build, ci). release-please uses commit history for automated version bumps and changelog entries.

## Consequences

Automated changelog generation with proper sections. Consistent commit history. PRs require conventional commits (enforced by CI).

