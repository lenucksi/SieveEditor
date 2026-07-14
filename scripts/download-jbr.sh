#!/bin/bash
# SPDX-FileCopyrightText: 2026 Lenucksi
#
# SPDX-License-Identifier: LGPL-3.0-or-later
# Download JetBrains Runtime (JBR) for native Wayland support
#
# Downloads JBR vanilla build from GitHub Releases, verifies SHA512,
# and extracts to jbr/jre/ in the project root.
#
# USAGE:
#   ./download-jbr.sh              # Download and extract JBR
#   ./download-jbr.sh --check      # Check if JBR is installed and valid
#   ./download-jbr.sh --version    # Print installed JBR version
#   ./download-jbr.sh --path       # Print path to JBR java binary

set -e

JBR_VERSION="25.0.3"
JBR_BUILD="b508.16"

JBR_FILE="jbr-${JBR_VERSION}-linux-x64-${JBR_BUILD}.tar.gz"
JBR_DIR_NAME="jbr-${JBR_VERSION}-linux-x64-${JBR_BUILD}"

# SHA-512 checksum (from JetBrains release checksum file)
JBR_SHA512="f936d2a4048485d3cb552c26f69b41f13eba0f75de1178c9ff6185547755c336d0ef5af9641f004e916d0d0414e34d1e9f9d12c08d262038cbca517dcad0dc96"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
JBR_DIR="$PROJECT_DIR/jbr"
JRE_DIR="$JBR_DIR/jre"
DOWNLOAD_DIR="$JBR_DIR/download"
JAVA_BINARY="$JRE_DIR/bin/java"

check_sha512() {
    local file="$1"
    local expected="$2"
    echo "Checking SHA-512 of $(basename "$file")..."
    local actual
    actual=$(sha512sum "$file" | cut -d' ' -f1)
    if [ "$actual" != "$expected" ]; then
        echo "ERROR: SHA-512 mismatch!"
        echo "  Expected: $expected"
        echo "  Actual:   $actual"
        return 1
    fi
    echo "SHA-512 matches."
    return 0
}

do_check() {
    if [ -x "$JAVA_BINARY" ]; then
        local version
        version=$("$JAVA_BINARY" -version 2>&1 | head -1)
        echo "JBR found: $JAVA_BINARY"
        echo "  $version"
        return 0
    else
        echo "JBR not installed."
        echo "  Run: $0"
        return 1
    fi
}

do_version() {
    if [ -x "$JAVA_BINARY" ]; then
        "$JAVA_BINARY" -version 2>&1 | head -1 | sed 's/.*"\(.*\)"/\1/'
        return 0
    fi
    echo "not-installed"
    return 1
}

do_path() {
    if [ -x "$JAVA_BINARY" ]; then
        echo "$JAVA_BINARY"
        return 0
    fi
    echo "not-found"
    return 1
}

case "${1:-}" in
    --check)
        do_check
        exit $?
        ;;
    --version)
        do_version
        exit $?
        ;;
    --path)
        do_path
        exit $?
        ;;
    --help|-h)
        sed -n '/^# USAGE:/,/^$/{ s/^# \?//; p }' "$0"
        exit 0
        ;;
esac

mkdir -p "$DOWNLOAD_DIR"
mkdir -p "$JRE_DIR"

DOWNLOAD_URL="https://cache-redirector.jetbrains.com/intellij-jbr/$JBR_FILE"
CACHED_TARBALL="$DOWNLOAD_DIR/$JBR_FILE"

do_download() {
    if [ -f "$CACHED_TARBALL" ]; then
        echo "Cached tarball found at $CACHED_TARBALL"
        if check_sha512 "$CACHED_TARBALL" "$JBR_SHA512"; then
            return 0
        fi
        echo "Cached tarball checksum mismatch, re-downloading..."
        rm -f "$CACHED_TARBALL"
    fi

    echo "Downloading JBR $JBR_VERSION ($JBR_BUILD)..."
    echo "  URL: $DOWNLOAD_URL"
    curl -#L -o "$CACHED_TARBALL" "$DOWNLOAD_URL"
    echo ""

    check_sha512 "$CACHED_TARBALL" "$JBR_SHA512"
}

do_extract() {
    echo "Extracting to $JRE_DIR..."

    local tmpdir
    tmpdir=$(mktemp -d)
    tar -xzf "$CACHED_TARBALL" -C "$tmpdir"

    rm -rf "$JRE_DIR"
    mv "$tmpdir/$JBR_DIR_NAME" "$JRE_DIR"
    rm -rf "$tmpdir"

    chmod +x "$JAVA_BINARY"

    echo "JBR extracted to: $JRE_DIR"
}

do_cleanup() {
    rm -rf "$DOWNLOAD_DIR"
}

echo "=== JetBrains Runtime Download ==="
echo "Version: $JBR_VERSION (build $JBR_BUILD)"
echo ""

do_download
do_extract
do_cleanup

echo ""
echo "=== Done ==="
echo "JBR Java binary: $JAVA_BINARY"
echo ""
"$JAVA_BINARY" -version 2>&1
echo ""
echo "To use with SieveEditor:"
echo "  ./scripts/sieveeditor.sh"
echo ""
