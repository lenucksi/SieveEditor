---
id: doc-1
title: Flatpak Packaging Guide
type: guide
created_date: '2026-05-20 14:58'
updated_date: '2026-05-20 14:59'
---
# Flatpak Packaging Guide

## App ID
`io.github.lenucksi.SieveEditor`

## Runtime
- Platform: org.freedesktop.Platform 25.08
- SDK: org.freedesktop.Sdk with OpenJDK 21 extension

## Build
`flatpak-builder build flatpak/io.github.lenucksi.SieveEditor.yml --force-clean`
`flatpak-builder --repo=flatpak-repo build flatpak/io.github.lenucksi.SieveEditor.yml`

## Automation
- `update-flatpak-deps.yml`: Weekly Maven dependency refresh for offline Flatpak builds
- `push-to-flathub.yml`: Pushes new releases to Flathub
- `update-flathub-release.yml`: Updates Flathub on release published
- Signed repo hosted on GitHub Pages

## Requirements
- Network access (for ManageSieve protocol)
- X11 socket, IPC shared, Vulkan enabled

**Source:** dev-docs/FLATPAK-PACKAGING-REPORT.md, flatpak/README.md
