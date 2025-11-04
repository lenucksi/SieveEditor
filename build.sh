#!/bin/bash
# SieveEditor multi-module build script
# Builds ManageSieveJ and SieveEditor in one command

set -e

echo "Building SieveEditor (multi-module)..."
echo "  - lib/ManageSieveJ (Java 11 fork)"
echo "  - app (SieveEditor application)"
echo ""

mvn clean package -Dmaven.javadoc.skip=true -DskipTests

echo ""
echo "Build complete!"
echo "Output: app/target/SieveEditor-jar-with-dependencies.jar"
echo ""
echo "Run with: ./sieveeditor.sh"
echo "Or: java -jar app/target/SieveEditor-jar-with-dependencies.jar"
