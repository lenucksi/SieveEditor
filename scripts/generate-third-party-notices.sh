#!/bin/bash
# SPDX-FileCopyrightText: 2026 Lenucksi
#
# SPDX-License-Identifier: LGPL-3.0-or-later
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo "============================================"
echo "  Generating Third-Party Dependency Notices"
echo "============================================"
echo ""

cd "$PROJECT_DIR"

if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed."
    exit 1
fi

echo "Step 1: Generating third-party license report..."
mvn license:add-third-party -Pgenerate-notices

echo ""
echo "Step 2: Copying notice to project root..."
if [ -f "target/notices/THIRD-PARTY.txt" ]; then
    cp "target/notices/THIRD-PARTY.txt" "THIRD-PARTY.txt"
    echo "Copied to THIRD-PARTY.txt"
else
    echo "Warning: target/notices/THIRD-PARTY.txt not found."
    echo "Check the output above for build errors."
fi

echo ""
echo "Done. Third-party notices generated in:"
echo "  - THIRD-PARTY.txt (project root)"
echo "  - target/notices/THIRD-PARTY.txt (build output)"
