#!/bin/bash
# SPDX-FileCopyrightText: 2025 Lenucksi
#
# SPDX-License-Identifier: LGPL-3.0-or-later

# Script to add SPDX headers to all Java and shell source files.
# Extracts copyright years from git history per-file.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
LICENSE_ID="LGPL-3.0-or-later"

find_files() {
    find "$ROOT_DIR/src/main/java" "$ROOT_DIR/src/test/java" -name "*.java" -type f
    find "$ROOT_DIR" -maxdepth 2 -name "*.sh" -type f ! -path "*/target/*" ! -path "*/.git/*"
}

build_header_java() {
    local file="$1"
    local years_authors
    years_authors=$(git -C "$ROOT_DIR" log --follow --format="%ad|%an" --date=format:%Y "$file" 2>/dev/null | sort -u)
    if [ -z "$years_authors" ]; then
        echo "// SPDX-FileCopyrightText: $(date +%Y) Lenucksi"
        echo "//"
        echo "// SPDX-License-Identifier: $LICENSE_ID"
        return
    fi
    echo "$years_authors" | awk -F'|' '
    {
        if (!($2 in seen)) { seen[$2] = 1; order[++idx] = $2 }
        years[$2] = years[$2] ? years[$2] ", " $1 : $1
    }
    END {
        for (i = 1; i <= idx; i++) {
            author = order[i]
            print "// SPDX-FileCopyrightText: " years[author] " " author
        }
        print "//"
        print "// SPDX-License-Identifier: " "'"$LICENSE_ID"'"
    }'
}

build_header_sh() {
    local file="$1"
    local years_authors
    years_authors=$(git -C "$ROOT_DIR" log --follow --format="%ad|%an" --date=format:%Y "$file" 2>/dev/null | sort -u)
    if [ -z "$years_authors" ]; then
        echo "# SPDX-FileCopyrightText: $(date +%Y) Lenucksi"
        echo "#"
        echo "# SPDX-License-Identifier: $LICENSE_ID"
        return
    fi
    echo "$years_authors" | awk -F'|' '
    {
        if (!($2 in seen)) { seen[$2] = 1; order[++idx] = $2 }
        years[$2] = years[$2] ? years[$2] ", " $1 : $1
    }
    END {
        for (i = 1; i <= idx; i++) {
            author = order[i]
            print "# SPDX-FileCopyrightText: " years[author] " " author
        }
        print "#"
        print "# SPDX-License-Identifier: " "'"$LICENSE_ID"'"
    }'
}

main() {
    local modified_files=()
    while IFS= read -r file; do
        if grep -q "SPDX-License-Identifier" "$file" 2>/dev/null; then
            echo "SKIP: $file (already has header)"
            continue
        fi
        echo "ADDING: $file"
        modified_files+=("$file")
        case "$file" in
            *.java)
                build_header_java "$file" > /tmp/spdx-header.txt
                sed -i "1r /tmp/spdx-header.txt" "$file"
                ;;
            *.sh)
                build_header_sh "$file" > /tmp/spdx-header.txt
                sed -i "1r /tmp/spdx-header.txt" "$file"
                ;;
        esac
    done < <(find_files)
    echo ""
    echo "=== Modified files ==="
    for f in "${modified_files[@]}"; do
        echo "  $f"
    done
    rm -f /tmp/spdx-header.txt
}

main
