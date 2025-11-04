#!/bin/bash
# SieveEditor launcher with HiDPI support for 4K displays
#
# This script solves the tiny UI problem on 4K displays by setting
# the correct Java scaling parameters.

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Find JAR file (check multi-module location first, then fallback)
if [ -f "$SCRIPT_DIR/app/target/SieveEditor-jar-with-dependencies.jar" ]; then
    JARFILE="$SCRIPT_DIR/app/target/SieveEditor-jar-with-dependencies.jar"
elif [ -f "$SCRIPT_DIR/target/SieveEditor-jar-with-dependencies.jar" ]; then
    JARFILE="$SCRIPT_DIR/target/SieveEditor-jar-with-dependencies.jar"
else
    echo "Error: JAR file not found"
    echo "Please run './build.sh' first to build the application."
    exit 1
fi

# Detect scale factor
# Based on test results: Xft.dpi is 192 (2x scaling), GNOME scaling-factor is 0
# We'll use a simple approach: check Xft.dpi or default to 2.0 for 4K

SCALE=2.0  # Default for 4K displays

# Try to detect from Xft.dpi if available
if command -v xrdb &> /dev/null; then
    DPI=$(xrdb -query 2>/dev/null | grep "Xft.dpi:" | awk '{print $2}')
    if [ -n "$DPI" ] && [ "$DPI" -gt 0 ]; then
        # Calculate scale from DPI (96 is standard DPI, 192 = 2x, 144 = 1.5x, etc.)
        SCALE=$(awk "BEGIN {printf \"%.1f\", $DPI/96}")
        echo "Detected DPI: $DPI, using scale: $SCALE"
    fi
fi

# Allow override via environment variable
if [ -n "$SIEVE_SCALE" ]; then
    SCALE=$SIEVE_SCALE
    echo "Using custom scale from SIEVE_SCALE: $SCALE"
fi

echo "Starting SieveEditor with scale factor: $SCALE"

# Java options for HiDPI support
JAVA_OPTS="-Dsun.java2d.uiScale.enabled=true"
JAVA_OPTS="$JAVA_OPTS -Dsun.java2d.uiScale=$SCALE"
JAVA_OPTS="$JAVA_OPTS -Dawt.useSystemAAFontSettings=lcd"
JAVA_OPTS="$JAVA_OPTS -Dswing.aatext=true"

# Launch application
exec java $JAVA_OPTS -jar "$JARFILE" "$@"
