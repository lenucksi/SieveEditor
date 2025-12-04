#!/bin/bash
# Build SieveEditor Flatpak locally
#
# This script builds a Flatpak package that can be installed and run
# on any Linux distribution with Flatpak support.
#
# REQUIREMENTS:
#   - flatpak-builder (install: sudo pacman -S flatpak-builder)
#   - Freedesktop runtime and SDK
#
# USAGE:
#   ./build-flatpak.sh        # Build Flatpak
#   ./build-flatpak.sh install  # Build and install
#   ./build-flatpak.sh run      # Build, install, and run
#
# OUTPUT:
#   - Build directory: flatpak-build/
#   - Repository: flatpak-repo/
#   - Bundle: SieveEditor.flatpak

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR/.."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if flatpak-builder is installed
if ! command -v flatpak-builder &> /dev/null; then
    echo -e "${RED}Error: flatpak-builder is not installed${NC}"
    echo ""
    echo "Install with:"
    echo "  sudo pacman -S flatpak-builder"
    echo ""
    exit 1
fi

# Check if flatpak is installed
if ! command -v flatpak &> /dev/null; then
    echo -e "${RED}Error: flatpak is not installed${NC}"
    echo ""
    echo "Install with:"
    echo "  sudo pacman -S flatpak"
    echo ""
    exit 1
fi

echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  Building SieveEditor Flatpak${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""

# Step 1: Ensure JAR exists
echo -e "${YELLOW}Step 1/5: Checking JAR file...${NC}"
if [ ! -f "target/SieveEditor-jar-with-dependencies.jar" ]; then
    echo "JAR not found, building..."
    if ! command -v mvn &> /dev/null; then
        echo -e "${RED}Error: Maven not installed. Cannot build JAR.${NC}"
        exit 1
    fi
    mvn clean package -DskipTests
fi
echo -e "${GREEN}✓ JAR file ready${NC}"
echo ""

# Step 2: Install Freedesktop runtime if needed
echo -e "${YELLOW}Step 2/5: Checking Freedesktop runtime...${NC}"
MISSING_RUNTIME=false
if ! flatpak list --runtime | grep -q "org.freedesktop.Platform.*25.08"; then
    echo "Missing: org.freedesktop.Platform//25.08"
    MISSING_RUNTIME=true
fi
if ! flatpak list --runtime | grep -q "org.freedesktop.Sdk.*25.08"; then
    echo "Missing: org.freedesktop.Sdk//25.08"
    MISSING_RUNTIME=true
fi
if ! flatpak list --runtime | grep -q "org.freedesktop.Sdk.Extension.openjdk21.*25.08"; then
    echo "Missing: org.freedesktop.Sdk.Extension.openjdk21//25.08"
    MISSING_RUNTIME=true
fi

if [ "$MISSING_RUNTIME" = true ]; then
    echo "Installing missing runtimes..."
    flatpak install -y flathub org.freedesktop.Platform//25.08 org.freedesktop.Sdk//25.08 org.freedesktop.Sdk.Extension.openjdk21//25.08
    echo -e "${GREEN}✓ Runtimes installed${NC}"
else
    echo -e "${GREEN}✓ All runtimes already installed${NC}"
fi
echo ""

# Step 3: Build Flatpak
echo -e "${YELLOW}Step 3/5: Building Flatpak...${NC}"
# Create symlinks so flatpak-builder can find files with manifest's relative paths
mkdir -p flatpak/flatpak
ln -sf ../target flatpak/target
ln -sf ../io.github.lenucksi.SieveEditor.desktop flatpak/flatpak/io.github.lenucksi.SieveEditor.desktop
ln -sf ../io.github.lenucksi.SieveEditor.png flatpak/flatpak/io.github.lenucksi.SieveEditor.png
ln -sf ../io.github.lenucksi.SieveEditor.metainfo.xml flatpak/flatpak/io.github.lenucksi.SieveEditor.metainfo.xml
flatpak-builder --force-clean --repo=flatpak-repo flatpak-build flatpak/io.github.lenucksi.SieveEditor.yml
# Clean up symlinks
rm -rf flatpak/target flatpak/flatpak
echo -e "${GREEN}✓ Flatpak built successfully${NC}"
echo ""

# Step 4: Lint the manifest and build (optional but recommended)
echo -e "${YELLOW}Step 4/6: Linting Flatpak manifest and build...${NC}"

# Check if linter is available (either as native command or Flatpak)
LINTER_AVAILABLE=false
if command -v flatpak-builder-lint &> /dev/null; then
    LINTER_CMD="flatpak-builder-lint"
    LINTER_AVAILABLE=true
elif flatpak list --app 2>/dev/null | grep -q "org.flatpak.Builder"; then
    LINTER_CMD="flatpak run --command=flatpak-builder-lint org.flatpak.Builder"
    LINTER_AVAILABLE=true
fi

if $LINTER_AVAILABLE; then
    echo "Linting manifest..."
    $LINTER_CMD manifest flatpak/io.github.lenucksi.SieveEditor.yml || true
    echo ""
    echo "Linting repository..."
    $LINTER_CMD repo flatpak-repo || true
    echo -e "${GREEN}✓ Linting complete${NC}"
else
    echo "Linter not installed (optional)"
    echo "To install: flatpak install flathub org.flatpak.Builder"
    echo -e "${YELLOW}⊘ Skipping lint${NC}"
fi
echo ""

# Step 5: Create bundle (optional but useful for distribution)
echo -e "${YELLOW}Step 5/6: Creating Flatpak bundle...${NC}"
flatpak build-bundle flatpak-repo SieveEditor.flatpak io.github.lenucksi.SieveEditor
BUNDLE_SIZE=$(du -h SieveEditor.flatpak | cut -f1)
echo -e "${GREEN}✓ Bundle created: SieveEditor.flatpak ($BUNDLE_SIZE)${NC}"
echo ""

# Step 6: Install if requested
if [ "$1" = "install" ] || [ "$1" = "run" ]; then
    echo -e "${YELLOW}Step 6/6: Installing Flatpak...${NC}"
    flatpak install -y --user --reinstall flatpak-repo io.github.lenucksi.SieveEditor
    echo -e "${GREEN}✓ Flatpak installed${NC}"
    echo ""

    if [ "$1" = "run" ]; then
        echo -e "${GREEN}Launching SieveEditor...${NC}"
        echo ""
        flatpak run io.github.lenucksi.SieveEditor
    fi
else
    echo -e "${YELLOW}Step 6/6: Skipping installation${NC}"
    echo ""
fi

echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  Build Complete!${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo "Build artifacts:"
echo "  • Repository: flatpak-repo/"
echo "  • Build dir:  flatpak-build/"
echo "  • Bundle:     SieveEditor.flatpak"
echo ""
echo "To install:"
echo "  flatpak install --user flatpak-repo io.github.lenucksi.SieveEditor"
echo ""
echo "To install from bundle:"
echo "  flatpak install --user SieveEditor.flatpak"
echo ""
echo "To run:"
echo "  flatpak run io.github.lenucksi.SieveEditor"
echo ""
echo "To uninstall:"
echo "  flatpak uninstall io.github.lenucksi.SieveEditor"
echo ""
