#!/bin/bash
#
# Local Test Script
# Run this script to execute the full test suite locally, matching CI behavior
#

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo "============================================"
echo "SieveEditor - Local Test Runner"
echo "============================================"
echo ""

# Check Java version
echo "Checking Java version..."
java -version 2>&1 | head -n 1

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ ERROR: Java 21 or higher required. Found: Java $JAVA_VERSION"
    exit 1
fi
echo "✅ Java version OK"
echo ""

# Check Maven
echo "Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo "❌ ERROR: Maven not found. Please install Maven 3.6+"
    exit 1
fi
mvn --version | head -n 1
echo "✅ Maven found"
echo ""

# Initialize submodules if needed
echo "============================================"
echo "Step 1: Initializing git submodules"
echo "============================================"
cd "$PROJECT_ROOT"
if [ ! -f "lib/ManageSieveJ/pom.xml" ]; then
    echo "ManageSieveJ submodule not initialized, initializing now..."
    git submodule update --init --recursive
    echo "✅ Submodules initialized"
else
    echo "✅ Submodules already initialized"
fi
echo ""

# Build ManageSieveJ library
echo "============================================"
echo "Step 2: Building ManageSieveJ library"
echo "============================================"
cd "$PROJECT_ROOT/lib/ManageSieveJ"
mvn clean install -DskipTests -q
if [ $? -eq 0 ]; then
    echo "✅ ManageSieveJ built successfully"
else
    echo "❌ ManageSieveJ build failed"
    exit 1
fi
echo ""

# Run tests
echo "============================================"
echo "Step 3: Running tests"
echo "============================================"
cd "$PROJECT_ROOT/app"
mvn test -B

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ All tests passed!"
else
    echo ""
    echo "❌ Tests failed"
    exit 1
fi
echo ""

# Build JAR
echo "============================================"
echo "Step 4: Building JAR"
echo "============================================"
mvn package -DskipTests -B -q

if [ -f "$PROJECT_ROOT/app/target/SieveEditor-jar-with-dependencies.jar" ]; then
    echo "✅ JAR built successfully"
    echo "   Location: app/target/SieveEditor-jar-with-dependencies.jar"
    ls -lh "$PROJECT_ROOT/app/target/SieveEditor-jar-with-dependencies.jar"
else
    echo "❌ JAR build failed"
    exit 1
fi
echo ""

# Optional: Coverage report
if [ "$1" == "--coverage" ]; then
    echo "============================================"
    echo "Step 5: Generating coverage report"
    echo "============================================"
    # Uncomment when JaCoCo is working
    # mvn jacoco:report
    # echo "Coverage report: app/target/site/jacoco/index.html"
    echo "⏳ Coverage reporting not yet enabled (JaCoCo needs configuration)"
    echo ""
fi

# Optional: Security scan
if [ "$1" == "--security" ]; then
    echo "============================================"
    echo "Step 6: Running security scan"
    echo "============================================"
    mvn org.owasp:dependency-check-maven:check
    echo "Security report: app/target/dependency-check-report.html"
    echo ""
fi

echo "============================================"
echo "✅ All checks passed!"
echo "============================================"
echo ""
echo "Next steps:"
echo "  - Run application: java -jar app/target/SieveEditor-jar-with-dependencies.jar"
echo "  - View test reports: app/target/surefire-reports/"
echo "  - Run with coverage: $0 --coverage"
echo "  - Run security scan: $0 --security"
echo ""
