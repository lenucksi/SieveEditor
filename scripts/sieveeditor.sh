#!/bin/bash
# SPDX-FileCopyrightText: 2025 Lenucksi
#
# SPDX-License-Identifier: LGPL-3.0-or-later
# SieveEditor launcher
#
# Uses JetBrains Runtime (JBR) for native Wayland support when available.
# Falls back to system Java with X11 if JBR is not installed.
#
# USAGE:
#   ./sieveeditor.sh                   # Launch SieveEditor (builds if needed)
#   ./sieveeditor.sh -v                # Launch with verbose logging
#   ./sieveeditor.sh --backend prompt  # Use specific credential backend
#
# REQUIREMENTS:
#   - Java 21 or later (system) OR JBR in jbr/jre/ (recommended for Wayland)
#   - Maven 3.6+ (only needed if JAR doesn't exist)

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JARFILE="$SCRIPT_DIR/../target/SieveEditor-jar-with-dependencies.jar"
JBR_HOME="$SCRIPT_DIR/../jbr/jre"

JBR_FLAGS=(
    -Dawt.toolkit.name=WLToolkit
    -Dsun.java2d.vulkan=True
    -Dawt.useSystemAAFontSettings=lcd
    -Dswing.aatext=true
)

build_jar() {
    echo "==================================================="
    echo "  SieveEditor JAR not found - building project..."
    echo "==================================================="
    echo ""

    if ! command -v mvn &> /dev/null; then
        echo "Error: Maven (mvn) is not installed or not in PATH"
        echo "Please install Maven 3.6+ to build this project"
        exit 1
    fi

    echo "Running: mvn clean package -DskipTests"
    echo ""
    mvn clean package -DskipTests

    if [ ! -f "$JARFILE" ]; then
        echo ""
        echo "Error: Build completed but JAR file not found at:"
        echo "  $JARFILE"
        exit 1
    fi

    echo ""
    echo "==================================================="
    echo "  Build completed successfully!"
    echo "==================================================="
    echo ""
}

# Build if JAR doesn't exist
if [ ! -f "$JARFILE" ]; then
    build_jar
fi

# Check for JBR
if [ -x "$JBR_HOME/bin/java" ]; then
    JAVA_CMD="$JBR_HOME/bin/java"
    JAVA_FLAGS=("${JBR_FLAGS[@]}")
    echo "Using JBR (Wayland native): $("$JAVA_CMD" -version 2>&1 | head -1)"
else
    JAVA_CMD="java"
    JAVA_FLAGS=()
    echo "JBR not found at $JBR_HOME"
    echo "  Install JBR: ./scripts/download-jbr.sh"
    echo "  Falling back to system Java (X11 via XWayland)"
    echo ""
fi

exec "$JAVA_CMD" \
    "${JAVA_FLAGS[@]}" \
    -jar "$JARFILE" "$@"
