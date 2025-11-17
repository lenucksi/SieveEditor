#!/bin/bash
# Check if all build dependencies are installed for SieveEditor

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "======================================"
echo "  SieveEditor Dependency Check"
echo "======================================"
echo ""

check_command() {
    local cmd=$1
    local optional=$2

    if command -v $cmd &> /dev/null; then
        local version=$($cmd --version 2>&1 | head -1 | sed 's/^[^0-9]*//')
        echo -e "${GREEN}✓${NC} $cmd installed: $version"
        return 0
    else
        if [ "$optional" = "optional" ]; then
            echo -e "${YELLOW}○${NC} $cmd NOT installed (optional)"
        else
            echo -e "${RED}✗${NC} $cmd NOT installed"
        fi
        return 1
    fi
}

check_java_version() {
    if command -v java &> /dev/null; then
        local version=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}')
        local major=$(echo $version | cut -d. -f1)

        if [ "$major" -ge 21 ]; then
            echo -e "${GREEN}✓${NC} Java $version (>= 21 required)"
            return 0
        else
            echo -e "${RED}✗${NC} Java $version (need >= 21)"
            return 1
        fi
    else
        echo -e "${RED}✗${NC} Java NOT installed"
        return 1
    fi
}

check_maven_version() {
    if command -v mvn &> /dev/null; then
        local version=$(mvn --version 2>&1 | head -1 | awk '{print $3}')
        echo -e "${GREEN}✓${NC} Maven $version"
        return 0
    else
        echo -e "${RED}✗${NC} Maven NOT installed"
        return 1
    fi
}

# Track failures
CORE_OK=true
FLATPAK_OK=true

echo "Core Dependencies (Required for JAR):"
echo "--------------------------------------"
check_java_version || CORE_OK=false
check_maven_version || CORE_OK=false
check_command git || CORE_OK=false
echo ""

echo "Packaging Tools:"
echo "--------------------------------------"
check_command jpackage optional
check_command fakeroot optional
check_command rpm optional
echo ""

echo "Flatpak Tools:"
echo "--------------------------------------"
check_command flatpak optional || FLATPAK_OK=false
check_command flatpak-builder optional || FLATPAK_OK=false

# Check for linter (either native or Flatpak)
if command -v flatpak-builder-lint &> /dev/null; then
    echo -e "${GREEN}✓${NC} flatpak-builder-lint (native)"
elif command -v flatpak &> /dev/null && flatpak list --app 2>/dev/null | grep -q "org.flatpak.Builder"; then
    echo -e "${GREEN}✓${NC} flatpak-builder-lint (via org.flatpak.Builder)"
else
    echo -e "${YELLOW}○${NC} flatpak-builder-lint NOT installed (optional)"
fi
echo ""

# Check Flatpak runtimes
if command -v flatpak &> /dev/null && command -v flatpak-builder &> /dev/null; then
    echo "Flatpak Runtimes:"
    echo "--------------------------------------"

    if flatpak list --runtime 2>/dev/null | grep -q "org.freedesktop.Platform.*24.08"; then
        echo -e "${GREEN}✓${NC} org.freedesktop.Platform 24.08"
    else
        echo -e "${YELLOW}○${NC} org.freedesktop.Platform 24.08 NOT installed"
        echo "  (Will be auto-installed by build-flatpak.sh)"
        FLATPAK_OK=false
    fi

    if flatpak list --runtime 2>/dev/null | grep -q "org.freedesktop.Sdk.*24.08"; then
        echo -e "${GREEN}✓${NC} org.freedesktop.Sdk 24.08"
    else
        echo -e "${YELLOW}○${NC} org.freedesktop.Sdk 24.08 NOT installed"
        echo "  (Will be auto-installed by build-flatpak.sh)"
        FLATPAK_OK=false
    fi

    if flatpak list --runtime 2>/dev/null | grep -q "org.freedesktop.Sdk.Extension.openjdk21"; then
        echo -e "${GREEN}✓${NC} org.freedesktop.Sdk.Extension.openjdk21"
    else
        echo -e "${YELLOW}○${NC} OpenJDK 21 extension NOT installed"
        echo "  (Will be auto-installed by build-flatpak.sh)"
        FLATPAK_OK=false
    fi
    echo ""
fi

# Check JAVA_HOME
echo "Environment:"
echo "--------------------------------------"
if [ -n "$JAVA_HOME" ]; then
    echo -e "${GREEN}✓${NC} JAVA_HOME set: $JAVA_HOME"
else
    echo -e "${YELLOW}○${NC} JAVA_HOME not set (optional)"
fi
echo ""

# Summary
echo "======================================"
echo "  Summary"
echo "======================================"
if $CORE_OK; then
    echo -e "${GREEN}✓ Core dependencies OK${NC}"
    echo "  You can build JAR with: mvn clean package"
else
    echo -e "${RED}✗ Core dependencies MISSING${NC}"
    echo "  Install with: sudo pacman -S jdk21-openjdk maven git"
fi
echo ""

if command -v flatpak &> /dev/null && command -v flatpak-builder &> /dev/null; then
    if $FLATPAK_OK; then
        echo -e "${GREEN}✓ Flatpak dependencies OK${NC}"
        echo "  You can build Flatpak with: ./build-flatpak.sh"
    else
        echo -e "${YELLOW}○ Flatpak dependencies PARTIAL${NC}"
        echo "  Run ./build-flatpak.sh to auto-install missing runtimes"
    fi
else
    echo -e "${YELLOW}○ Flatpak tools not installed${NC}"
    echo "  Install with: sudo pacman -S flatpak flatpak-builder"
fi
echo ""

# Exit code
if $CORE_OK; then
    exit 0
else
    exit 1
fi
