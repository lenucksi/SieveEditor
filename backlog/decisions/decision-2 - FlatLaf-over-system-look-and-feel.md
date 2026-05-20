---
id: decision-2
title: FlatLaf over system look-and-feel
date: '2026-05-20 14:58'
status: accepted
---
## Context

Swing's system L&F has poor HiDPI/4K display support. Users with modern displays had blurry or tiny UI elements.

## Decision

Migrated to FlatLaf 3.7.1, a modern cross-platform Look-and-Feel with excellent HiDPI support, dark/light themes, and active development.

## Consequences

Sharp, scalable UI on 4K/Retina displays. Built-in dark mode. Continuous improvements from FlatLaf upstream.

