# GitHub Actions Workflows

This directory contains CI/CD workflows for SieveEditor.

## Workflows

### 1. CI - Continuous Integration (`ci.yml`)

**Trigger:** Every push and pull request

**Purpose:** Test code on all platforms

**Jobs:**
- **test:** Run tests on Linux, Windows, macOS with Java 21
- **test-summary:** Aggregate results

**Matrix:**
- OS: ubuntu-latest, windows-latest, macos-latest
- Java: 21

**Artifacts:**
- Test reports (30 days)
- JAR file (7 days)

### 2. Scorecard - Security Scanning (`scorecard.yml`)

**Trigger:** Weekly on Monday + manual

**Purpose:** OpenSSF security health metrics

**Checks:**
- Dangerous GitHub Actions patterns
- Branch protection
- Code review requirements
- Dependency updates
- Signed commits/releases
- Token permissions
- Vulnerability disclosure

**Output:** GitHub Security tab (SARIF format)

### 3. Package - Cross-Platform Builds (`package.yml`)

**Trigger:** Manual dispatch + release published

**Purpose:** Build native packages for all platforms

**Jobs:**
- **build-jar:** Create uber JAR
- **package-linux:** Build DEB and RPM
- **package-windows:** Build MSI
- **package-macos:** Build DMG
- **checksums:** Generate SHA256 checksums

**Artifacts:**
- `SieveEditor.jar` (all platforms)
- `SieveEditor.deb` (Ubuntu/Debian)
- `SieveEditor.rpm` (Fedora/RHEL)
- `SieveEditor.msi` (Windows)
- `SieveEditor.dmg` (macOS)
- `checksums.txt`

**Attestations:** SLSA Level 3 provenance for all artifacts

### 4. Release - Automated Releases (`release.yml`)

**Trigger:** Push to main branch

**Purpose:** Automate version bumps, changelogs, and releases

**Process:**
1. **release-please** analyzes commits since last release
2. Creates/updates Release PR with:
   - Version bump (based on conventional commits)
   - Updated CHANGELOG.md
   - Updated pom.xml
3. When Release PR merged:
   - Creates GitHub Release
   - Triggers package workflow
   - Uploads all platform packages
   - Adds installation instructions

**Conventional Commits:**
- `feat:` → minor version bump
- `fix:` → patch version bump
- `feat!:` or `fix!:` → major version bump

## Security Features

### SLSA Level 3 Compliance

All build artifacts include cryptographic attestations:

```yaml
- uses: actions/attest-build-provenance@v2
  with:
    subject-path: 'path/to/artifact'
```

**Verify attestations:**
```bash
gh attestation verify SieveEditor.jar --owner lenucksi --repo SieveEditor
```

### Least Privilege Permissions

Each workflow specifies minimal required permissions:

```yaml
permissions:
  contents: read      # Default
  id-token: write     # For OIDC/attestations
  security-events: write  # For security scans
```

### Short-Lived Tokens

- No long-lived credentials stored
- OIDC tokens auto-expire after job
- GitHub Secrets for sensitive data

### Dependency Management

- **Dependabot:** Automated vulnerability scanning and updates
- **Dependency pinning:** Actions pinned to SHA (recommended)

## Local Testing

Run the same tests as CI locally:

```bash
# Quick test
./scripts/test-local.sh

# With coverage
./scripts/test-local.sh --coverage

# With security scan
./scripts/test-local.sh --security
```

## Troubleshooting

### CI Fails on Windows

Check for:
- Path separators (`/` vs `\`)
- Line endings (CRLF vs LF)
- Case-sensitive file names

### Package Workflow Fails

- **Linux:** Ensure `fakeroot` and `rpm-build` installed
- **Windows:** Ensure WiX Toolset available
- **macOS:** Ensure Xcode Command Line Tools installed

### Release Not Created

Check:
1. Commits follow Conventional Commits format
2. Push to `main` branch
3. Release PR merged (not closed)

### Attestations Fail

Ensure workflow has:
```yaml
permissions:
  id-token: write
  attestations: write
```

## Workflow Diagrams

### CI Flow
```
Push/PR → Checkout → Setup Java → Cache → Build Library → Run Tests → Upload Results
```

### Release Flow
```
Push to main → Release Please → Create/Update PR → Merge → Create Release → Package → Upload Assets
```

## Best Practices

1. **Test Locally First:** Use `./scripts/test-local.sh` before pushing
2. **Conventional Commits:** Follow format for automatic releases
3. **Security:** Never commit secrets to workflows
4. **Performance:** Use caching for Maven dependencies
5. **Monitoring:** Check Actions tab for failures

## References

- [GitHub Actions Docs](https://docs.github.com/en/actions)
- [SLSA Framework](https://slsa.dev/)
- [OpenSSF Scorecard](https://scorecard.dev/)
- [Release Please](https://github.com/googleapis/release-please-action)
- [Conventional Commits](https://www.conventionalcommits.org/)
