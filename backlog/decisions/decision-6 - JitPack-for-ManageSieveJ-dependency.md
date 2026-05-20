---
id: decision-6
title: JitPack for ManageSieveJ dependency
date: '2026-05-20 14:58'
status: accepted
---
## Context

ManageSieveJ is published to both GitHub Packages (requires auth) and JitPack (public). SieveEditor needs a publicly accessible dependency for CI builds.

## Decision

Use JitPack for ManageSieveJ dependency. JitPack repository added to pom.xml. Version specified as GitHub tag (e.g. v0.3.12).

## Consequences

CI builds without authentication. Dependabot and Renovate can manage updates. JitPack builds on-demand from tags.

