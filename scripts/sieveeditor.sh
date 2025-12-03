#!/bin/bash
# SieveEditor launcher
#
# This script automatically builds the application if needed and launches it.
# HiDPI scaling is handled automatically by FlatLaf.
#
# USAGE:
#   ./sieveeditor.sh                   # Launch SieveEditor (builds if needed)
#   ./sieveeditor.sh -v                # Launch with verbose logging
#   ./sieveeditor.sh --backend prompt  # Use specific credential backend
#
# REQUIREMENTS:
#   - Java 21 or later
#   - Maven 3.6+ (only needed if JAR doesn't exist)

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

# Launch application with font rendering options
exec java \
    -Dawt.toolkit.name=WLToolkit \
    -Dsun.java2d.vulkan=True \
    -Dawt.useSystemAAFontSettings=lcd \
    -Dswing.aatext=true \
    -jar "$JARFILE" "$@"
