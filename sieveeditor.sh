#!/bin/bash
# SieveEditor launcher with HiDPI support for 4K displays
#
# This script automatically builds the application if needed and launches it
# with the correct Java scaling parameters for high-resolution displays.
#
# USAGE:
#   ./sieveeditor.sh           # Launch SieveEditor (builds if needed)
#   SIEVE_SCALE=3.0 ./sieveeditor.sh  # Custom scale factor
#
# REQUIREMENTS:
#   - Java 21 or later
#   - Maven 3.6+ (only needed if JAR doesn't exist)
#
# The script will automatically:
#   - Check if the JAR file exists
#   - Build the project with 'mvn clean package -DskipTests' if needed
#   - Detect your display DPI and set appropriate scaling
#   - Launch the application with optimized Java parameters

set -e  # Exit on error

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Define JAR file location
JARFILE="$SCRIPT_DIR/target/SieveEditor-jar-with-dependencies.jar"

# Build if JAR doesn't exist
if [ ! -f "$JARFILE" ]; then
    echo "==================================================="
    echo "  SieveEditor JAR not found - building project..."
    echo "==================================================="
    echo ""

    # Check if Maven is available
    if ! command -v mvn &> /dev/null; then
        echo "Error: Maven (mvn) is not installed or not in PATH"
        echo "Please install Maven 3.6+ to build this project"
        exit 1
    fi

    # Build the project
    echo "Running: mvn clean package -DskipTests"
    echo ""
    cd "$SCRIPT_DIR"
    mvn clean package -DskipTests

    # Verify JAR was created
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
