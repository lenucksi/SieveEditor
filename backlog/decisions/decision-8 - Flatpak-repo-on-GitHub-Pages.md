---
id: decision-8
title: Flatpak repo on GitHub Pages
date: '2026-05-20 14:58'
status: accepted
---
## Context

Flatpak repository needs to be hosted for users to install the app. Traditional options include self-hosting or Flathub-only. GitHub Pages provides free static hosting.

## Decision

Host signed Flatpak repository on GitHub Pages. GPG-signed with public key checked into repo. Automatically updated on release. Also published to Flathub.

## Consequences

Users can install directly from GitHub Pages repo without Flathub. Provides fallback if Flathub publishing has delays. GPG signing ensures integrity.

