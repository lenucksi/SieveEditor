#!/usr/bin/env python3
"""
Update AppStream metainfo.xml with release notes from CHANGELOG.md

This script extracts release information from Release Please's CHANGELOG.md
and converts it to AppStream XML format for the metainfo file.

Usage:
    python scripts/update-metainfo-release.py [version]

If version is not specified, it updates the latest release from CHANGELOG.md
"""

import re
import sys
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path
from typing import Optional, Dict, List


def compare_versions(v1: str, v2: str) -> int:
    """
    Compare two semantic version strings.

    Args:
        v1: First version (e.g., "1.0.2")
        v2: Second version (e.g., "1.0.0")

    Returns:
        -1 if v1 < v2, 0 if equal, 1 if v1 > v2
    """
    def normalize(v):
        """Convert version string to list of integers."""
        try:
            return [int(x) for x in v.split('.')]
        except:
            return [0]

    parts1 = normalize(v1)
    parts2 = normalize(v2)

    # Pad to same length
    max_len = max(len(parts1), len(parts2))
    parts1 += [0] * (max_len - len(parts1))
    parts2 += [0] * (max_len - len(parts2))

    for p1, p2 in zip(parts1, parts2):
        if p1 < p2:
            return -1
        elif p1 > p2:
            return 1

    return 0


def parse_changelog(changelog_path: Path, version: Optional[str] = None) -> Optional[Dict[str, any]]:
    """
    Parse CHANGELOG.md and extract release information.

    Args:
        changelog_path: Path to CHANGELOG.md
        version: Specific version to extract (None = latest)

    Returns:
        Dict with 'version', 'date', and 'sections' keys
    """
    with open(changelog_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Pattern to match version headers: ## [1.0.2](...) (2025-12-04)
    version_pattern = r'^## \[([^\]]+)\]\([^)]+\) \(([0-9]{4}-[0-9]{2}-[0-9]{2})\)'

    # Find all version blocks
    lines = content.split('\n')
    releases = []
    current_release = None
    current_section = None

    for line in lines:
        # Check for version header
        version_match = re.match(version_pattern, line)
        if version_match:
            if current_release:
                releases.append(current_release)

            current_release = {
                'version': version_match.group(1),
                'date': version_match.group(2),
                'sections': {}
            }
            current_section = None
            continue

        # Check for section headers (### Features, ### Bug Fixes, etc.)
        section_match = re.match(r'^### (.+)$', line)
        if section_match and current_release:
            section_name = section_match.group(1)
            # Skip "Miscellaneous Chores" and internal release commits
            if section_name not in ['Miscellaneous Chores']:
                current_section = section_name
                if current_section not in current_release['sections']:
                    current_release['sections'][current_section] = []
            else:
                current_section = None
            continue

        # Check for bullet points
        bullet_match = re.match(r'^\* (.+)$', line)
        if bullet_match and current_release and current_section:
            # Extract the text and remove commit links
            text = bullet_match.group(1)
            # Remove commit hashes and links like ([abc1234](...))
            text = re.sub(r'\s*\([a-f0-9]{7,}\)', '', text)
            text = re.sub(r'\s*\(\[([a-f0-9]{7,})\]\([^)]+\)\)', '', text)

            # Remove scope prefixes like "**feat:**", "**security:**", "**file:**"
            # Format is **scope:** where the colon is inside the bold markers
            text = re.sub(r'^\*\*[^*]+:\*\*\s*', '', text)

            # Capitalize first letter
            if text:
                text = text[0].upper() + text[1:]

            # Skip internal release commits
            if not text.startswith('release:') and not text.startswith('Release:'):
                current_release['sections'][current_section].append(text)

    # Add the last release
    if current_release:
        releases.append(current_release)

    # Filter out releases with no meaningful sections
    releases = [r for r in releases if r['sections']]

    if not releases:
        return None

    # Return specific version or latest
    if version:
        for release in releases:
            if release['version'] == version:
                return release
        return None
    else:
        return releases[0]  # Latest release


def convert_to_appstream_xml(release: Dict[str, any]) -> str:
    """
    Convert release information to AppStream XML description format.

    Args:
        release: Dict with 'version', 'date', and 'sections' keys

    Returns:
        XML string for the <description> element content
    """
    # Map section names to user-friendly titles
    section_titles = {
        'Features': 'New Features',
        'Bug Fixes': 'Bug Fixes',
        'Performance Improvements': 'Performance Improvements',
        'Security': 'Security',
        'Dependencies': 'Dependencies',
        'Documentation': 'Documentation',
        'security': 'Security',
        '⚠ BREAKING CHANGES': 'Breaking Changes'
    }

    xml_parts = []

    for section_name, items in release['sections'].items():
        if not items:
            continue

        # Get friendly title
        title = section_titles.get(section_name, section_name)

        # Add section as paragraph + list
        if len(release['sections']) > 1:
            xml_parts.append(f'    <p>{title}:</p>')

        xml_parts.append('    <ul>')
        for item in items:
            # Escape XML special characters
            item = item.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
            xml_parts.append(f'      <li>{item}</li>')
        xml_parts.append('    </ul>')

    return '\n'.join(xml_parts)


def update_metainfo(metainfo_path: Path, release: Dict[str, any], description_xml: str) -> None:
    """
    Update the metainfo.xml file with new release information.

    Args:
        metainfo_path: Path to metainfo.xml
        release: Release information dict
        description_xml: AppStream XML description content
    """
    # Parse the XML file
    ET.register_namespace('', 'http://www.w3.org/2000/01/rdf-schema#')
    tree = ET.parse(metainfo_path)
    root = tree.getroot()

    # Find or create releases element
    releases = root.find('releases')
    if releases is None:
        releases = ET.SubElement(root, 'releases')

    # Check if this version already exists
    existing_release = None
    for rel in releases.findall('release'):
        if rel.get('version') == release['version']:
            existing_release = rel
            break

    if existing_release is not None:
        # Update existing release
        release_elem = existing_release
        # Remove old description if exists
        old_desc = release_elem.find('description')
        if old_desc is not None:
            release_elem.remove(old_desc)
    else:
        # Create new release element
        release_elem = ET.Element('release')
        release_elem.set('version', release['version'])
        release_elem.set('date', release['date'])

        # Find correct insertion position (releases should be newest first)
        # Compare by date, then by version as fallback
        insert_pos = 0
        for i, existing in enumerate(releases.findall('release')):
            existing_date = existing.get('date', '')
            existing_version = existing.get('version', '')

            # Skip comments and other non-element nodes
            if not isinstance(existing.tag, str):
                continue

            # Compare dates (newer should come first)
            if release['date'] < existing_date:
                insert_pos = i + 1
            elif release['date'] == existing_date:
                # Same date, compare versions (newer first)
                if compare_versions(release['version'], existing_version) < 0:
                    insert_pos = i + 1
            else:
                # Our release is newer, insert here
                break

        # Insert at calculated position
        releases.insert(insert_pos, release_elem)

    # Parse and add description
    # We need to parse the description XML and add it as elements
    description_elem = ET.SubElement(release_elem, 'description')

    # Parse the XML string we created
    desc_wrapper = ET.fromstring(f'<desc>{description_xml}</desc>')
    for child in desc_wrapper:
        description_elem.append(child)

    # Write back to file with proper formatting
    # Use indentation for readability
    indent_xml(root)

    # Write to file
    tree.write(metainfo_path, encoding='utf-8', xml_declaration=True)

    # Fix the XML declaration (ElementTree doesn't format it nicely)
    with open(metainfo_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Ensure proper XML declaration
    if not content.startswith('<?xml version="1.0" encoding="UTF-8"?>'):
        content = re.sub(r'^<\?xml[^?]*\?>', '<?xml version="1.0" encoding="UTF-8"?>', content)

    with open(metainfo_path, 'w', encoding='utf-8') as f:
        f.write(content)


def indent_xml(elem, level=0):
    """Add pretty-printing indentation to XML elements."""
    indent = "\n" + "  " * level
    if len(elem):
        if not elem.text or not elem.text.strip():
            elem.text = indent + "  "
        if not elem.tail or not elem.tail.strip():
            elem.tail = indent
        for child in elem:
            indent_xml(child, level + 1)
        if not child.tail or not child.tail.strip():
            child.tail = indent
    else:
        if level and (not elem.tail or not elem.tail.strip()):
            elem.tail = indent


def main():
    """Main entry point."""
    # Determine project root (script is in scripts/)
    script_dir = Path(__file__).parent
    project_root = script_dir.parent

    changelog_path = project_root / 'CHANGELOG.md'
    metainfo_path = project_root / 'flatpak' / 'io.github.lenucksi.SieveEditor.metainfo.xml'

    # Check if files exist
    if not changelog_path.exists():
        print(f"Error: CHANGELOG.md not found at {changelog_path}", file=sys.stderr)
        sys.exit(1)

    if not metainfo_path.exists():
        print(f"Error: metainfo.xml not found at {metainfo_path}", file=sys.stderr)
        sys.exit(1)

    # Get version from command line if provided
    version = sys.argv[1] if len(sys.argv) > 1 else None

    # Parse changelog
    release = parse_changelog(changelog_path, version)

    if not release:
        if version:
            print(f"Error: Version {version} not found in CHANGELOG.md", file=sys.stderr)
        else:
            print("Error: No releases found in CHANGELOG.md", file=sys.stderr)
        sys.exit(1)

    print(f"Processing release {release['version']} ({release['date']})")

    # Convert to AppStream XML
    description_xml = convert_to_appstream_xml(release)

    print("Generated description XML:")
    print(description_xml)

    # Update metainfo.xml
    update_metainfo(metainfo_path, release, description_xml)

    print(f"\n✓ Updated {metainfo_path}")


if __name__ == '__main__':
    main()
