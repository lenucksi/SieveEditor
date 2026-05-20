#!/bin/bash
# Wrapper script for running SieveEditor tests
# Uses Xvfb if no display is available, otherwise runs directly
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

if [ -z "$DISPLAY" ] && command -v xvfb-run &> /dev/null; then
    echo "No display detected, using Xvfb..."
    exec xvfb-run --auto-servernum mvn "$@"
else
    exec mvn "$@"
fi
