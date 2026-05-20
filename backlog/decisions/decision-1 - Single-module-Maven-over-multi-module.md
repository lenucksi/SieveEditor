---
id: decision-1
title: Single-module Maven over multi-module
date: '2026-05-20 14:58'
status: accepted
---
## Context

Original project used multi-module Maven structure with parent-child aggregation. Added complexity for a single deliverable (the desktop editor).

## Decision

Merged all modules into a single-module Maven project. Removed parent POM, simplified dependency management.

## Consequences

Simpler build, faster compilation, easier CI configuration. Single JAR with all dependencies (uber-JAR via maven-assembly-plugin).

