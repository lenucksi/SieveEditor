---
id: decision-5
title: LGPL-3.0-only license
date: '2026-05-20 14:58'
status: accepted
---
## Context

Project is a desktop application built on ManageSieveJ (MIT) and various other libraries. LGPL allows proprietary linking while keeping the application itself LGPL.

## Decision

Licensed under LGPL-3.0-only. Allows integration with proprietary software while ensuring the editor itself stays open source.

## Consequences

Compatible with LGPL and MIT dependencies. Proprietary uses must comply with LGPL terms. Flathub requires LGPL-3.0 license declaration.

