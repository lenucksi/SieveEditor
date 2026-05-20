---
id: doc-2
title: CI/CD Strategy Overview
type: guide
created_date: '2026-05-20 14:58'
updated_date: '2026-05-20 14:59'
---
# CI/CD Strategy Overview

## 11 Workflows

| Workflow | Trigger | Purpose |
|---|---|---|
| ci.yml | Push/PR master/develop/feature | 3 OS test matrix, JaCoCo, SonarCloud |
| codeql.yml | Push/PR + weekly | Java + Actions security scanning |
| scorecard.yml | Weekly Monday | OpenSSF Scorecard |
| package.yml | Release + manual | JAR, DEB, RPM, MSI, DMG, Flatpak, SLSA attest |
| release.yml | Push master | Release Please automated versioning |
| dependency-review.yml | PR | Dependency vulnerability check |
| actionlint.yml | Push/PR | Workflow linting |
| push-to-flathub.yml | Release | Flathub publish |
| update-flathub-release.yml | Release published | Flathub release sync |
| update-flatpak-deps.yml | Weekly | Flatpak dep refresh |
| update-metainfo-release.yml | Release PR | AppStream metainfo update |

## Security
- SLSA Level 3 attestations
- Harden-Runner (step-security) on every job
- Pinned action SHAs
- Least-privilege permissions
- Dependabot + Renovate

**Source:** dev-docs/CI-CD-STRATEGY-2025.md, .github/workflows/
