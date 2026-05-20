---
id: decision-4
title: Flatpak as primary distribution format
date: '2026-05-20 14:58'
status: accepted
---
## Context

Cross-platform distribution needed for Linux (primary target), with Windows and macOS as secondary. Flatpak provides sandboxed deployment with automatic updates.

## Decision

Flatpak is primary distribution on Flathub. Also build JAR, DEB, RPM, MSI, DMG via jpackage in CI. GitHub Releases hosts all formats.

## Consequences

Linux users install from Flathub with automatic updates. Self-contained sandboxed environment. Full network access required for ManageSieve.

