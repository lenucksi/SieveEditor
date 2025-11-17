# CI/CD and Packaging Strategy - 2025 Best Practices

**Date:** 2025-11-15
**Project:** SieveEditor
**Audience:** Development team

## Executive Summary

This document outlines the comprehensive CI/CD strategy for SieveEditor based on 2025 best practices, including:
- **Continuous Integration** with automated testing on every commit/PR
- **Cross-platform packaging** for Linux (Flatpak, DEB, RPM), Windows (MSI, EXE), macOS (DMG, PKG)
- **Security hardening** with SLSA Level 3 attestations, code signing, and OpenSSF Scorecard
- **Automated releases** with changelog generation and contributor attribution
- **Local test execution** for developer productivity

---

## Table of Contents

1. [Security & Supply Chain Best Practices](#security--supply-chain-best-practices)
2. [Continuous Integration Strategy](#continuous-integration-strategy)
3. [Cross-Platform Build Matrix](#cross-platform-build-matrix)
4. [Packaging Strategy](#packaging-strategy)
5. [Code Signing & Attestation](#code-signing--attestation)
6. [Release Automation](#release-automation)
7. [Local Development Workflow](#local-development-workflow)
8. [Implementation Roadmap](#implementation-roadmap)

---

## Security & Supply Chain Best Practices

### 1. SLSA Level 3 Compliance (2025 Standard)

**What is SLSA?**
- SLSA (Supply-chain Levels for Software Artifacts) is a security framework from OpenSSF
- Level 3 provides provenance from a hardened, tamper-resistant build platform
- Protects against tampering during the build process

**GitHub Artifact Attestations (Dec 2024)**
- GitHub's native solution for SLSA Level 3 compliance
- Automatically generates cryptographic attestations for build artifacts
- Uses Sigstore for signing and verification
- **Key Feature:** Simpler than previous slsa-github-generator approach

**Implementation:**
```yaml
# In GitHub Actions workflow
- uses: actions/attest-build-provenance@v2
  with:
    subject-path: 'app/target/*.jar'
```

**Benefits:**
- Verifiable build provenance
- Tamper detection
- Supply chain transparency
- Compliance with enterprise security requirements

### 2. OpenSSF Scorecard

**Purpose:** Automated security scanning for GitHub repositories

**Checks Include:**
- Dangerous GitHub Actions workflow patterns
- Branch protection enforcement
- Code review requirements
- Dependency update tools (Dependabot)
- Signed commits and releases
- Token permissions (principle of least privilege)
- Vulnerability disclosure

**Implementation:**
```yaml
# .github/workflows/scorecard.yml
- uses: ossf/scorecard-action@v2
  with:
    results_file: results.sarif
    publish_results: true
```

**Permissions Required:**
- `id-token: write` (for OIDC token to verify authenticity)
- `contents: read`
- `security-events: write` (to upload to GitHub Security tab)

### 3. Short-Lived Tokens & OIDC

**GitHub OIDC Integration:**
- Eliminates need for long-lived credentials
- Tokens automatically expire after job completion
- Works with cloud providers (AWS, Azure, GCP)

**Best Practices:**
```yaml
permissions:
  id-token: write   # For OIDC token
  contents: read    # Read-only by default
  packages: write   # Only when publishing
```

**Principle of Least Privilege:**
- Grant minimum permissions per job
- Use `permissions:` at job level, not workflow level
- Avoid `permissions: write-all`

### 4. Dependency Management

**Dependabot Configuration:**
```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "maven"
    directory: "/app"
    schedule:
      interval: "weekly"
    open-pull-requests-limit: 10
```

**Maven Dependency Security:**
- Use Maven Enforcer Plugin for version control
- OWASP Dependency-Check for vulnerability scanning
- Verify checksums and signatures

---

## Continuous Integration Strategy

### 1. Test Matrix Configuration

**Multi-Platform Testing:**
```yaml
strategy:
  matrix:
    os: [ubuntu-latest, windows-latest, macos-latest]
    java-version: [21]
```

**Why Test on All Platforms?**
- File path differences (Windows vs Unix)
- Encoding issues
- Platform-specific dependencies
- Swing/AWT rendering differences

### 2. CI Triggers

**On Every Commit:**
```yaml
on:
  push:
    branches: [main, develop, 'feature/**']
  pull_request:
    branches: [main, develop]
```

**PR Requirements:**
- All tests must pass
- Code coverage thresholds met (when JaCoCo working)
- Security scans pass
- No high/critical vulnerabilities

### 3. Test Execution Strategy

**Fast Feedback Loop:**
1. Unit tests first (fastest)
2. Integration tests (if applicable)
3. Build verification
4. Package creation (on release only)

**Caching Strategy:**
```yaml
- uses: actions/cache@v4
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
```

**Timeout Protection:**
```yaml
timeout-minutes: 20  # Fail fast on hanging tests
```

---

## Cross-Platform Build Matrix

### 1. Platform-Specific Requirements

#### Linux (Ubuntu)
**Build Requirements:**
- OpenJDK 21
- `rpm-build` (for RPM packages)
- `fakeroot` (for DEB packages)
- `flatpak-builder` (for Flatpak)

**Output Formats:**
- `.deb` (Debian/Ubuntu)
- `.rpm` (Fedora/RedHat)
- `.flatpak` (Universal Linux)

#### Windows
**Build Requirements:**
- OpenJDK 21
- WiX Toolset 3.0+ (for MSI)
- Code signing certificate (optional but recommended)

**Output Formats:**
- `.msi` (Installer)
- `.exe` (Portable)

#### macOS
**Build Requirements:**
- OpenJDK 21
- Xcode Command Line Tools
- Code signing certificate (required for distribution)
- Notarization credentials

**Output Formats:**
- `.dmg` (Disk Image)
- `.pkg` (Installer)

### 2. jpackage Configuration

**Universal jpackage Arguments:**
```bash
jpackage \
  --input app/target \
  --main-jar SieveEditor-jar-with-dependencies.jar \
  --main-class de.febrildur.sieveeditor.Application \
  --name SieveEditor \
  --app-version 0.0.1 \
  --vendor "SieveEditor Project" \
  --description "ManageSieve script editor" \
  --java-options '-Dsun.java2d.uiScale=2.0'  # HiDPI support
```

**Platform-Specific Additions:**

**Linux (DEB):**
```bash
--type deb \
--linux-shortcut \
--linux-menu-group "Network;Email" \
--linux-app-category "Network"
```

**Linux (RPM):**
```bash
--type rpm \
--linux-shortcut \
--linux-menu-group "Applications/Internet"
```

**Windows (MSI):**
```bash
--type msi \
--win-dir-chooser \
--win-menu \
--win-shortcut \
--win-menu-group "SieveEditor"
```

**macOS (DMG):**
```bash
--type dmg \
--mac-package-name "SieveEditor" \
--mac-package-identifier "de.febrildur.sieveeditor"
```

### 3. Limitation: Cross-Compilation Not Supported

**Critical Constraint:**
- jpackage CANNOT cross-compile
- Windows packages MUST be built on Windows
- Linux packages MUST be built on Linux
- macOS packages MUST be built on macOS

**GitHub Actions Solution:**
```yaml
strategy:
  matrix:
    include:
      - os: ubuntu-latest
        package-type: deb
      - os: ubuntu-latest
        package-type: rpm
      - os: windows-latest
        package-type: msi
      - os: macos-latest
        package-type: dmg
```

---

## Packaging Strategy

### 1. Linux Packaging

#### DEB/RPM (Traditional)
**Advantages:**
- Native package managers
- System integration
- Automatic updates via apt/dnf

**Disadvantages:**
- Requires separate packages for Debian/Ubuntu vs Fedora/RHEL
- Dependency management complexity

#### Flatpak (Universal)
**Advantages:**
- Single package for all distros
- Sandboxed execution
- Flathub distribution
- Automatic updates

**Challenges for Java Apps:**
- Need OpenJDK extension: `org.freedesktop.Sdk.Extension.openjdk21`
- Read-only `/app` directory (conflicts with self-updating apps)
- Potential permission issues for `~/.sieveprofiles`

**Recommended Flatpak Manifest:**
```yaml
# de.febrildur.sieveeditor.yml
app-id: de.febrildur.sieveeditor
runtime: org.freedesktop.Platform
runtime-version: '23.08'
sdk: org.freedesktop.Sdk
sdk-extensions:
  - org.freedesktop.Sdk.Extension.openjdk21
command: sieveeditor
finish-args:
  - --socket=x11
  - --share=network  # Required for ManageSieve
  - --filesystem=~/.sieveprofiles:create  # Profile storage
modules:
  - name: sieveeditor
    buildsystem: simple
    build-commands:
      - install -Dm755 SieveEditor.jar /app/bin/SieveEditor.jar
      - install -Dm755 sieveeditor /app/bin/sieveeditor
    sources:
      - type: file
        path: app/target/SieveEditor-jar-with-dependencies.jar
        dest-filename: SieveEditor.jar
```

**Decision:**
- Provide DEB + RPM for traditional users
- Provide Flatpak for universal Linux support
- Document limitations (no self-updates in Flatpak)

### 2. Windows Packaging

#### MSI Installer
**Advantages:**
- Native Windows installer
- Appears in Add/Remove Programs
- Can set file associations
- Supports upgrades

**Requirements:**
- WiX Toolset 3.0+
- Code signing certificate (optional)

**Best Practices:**
- Set upgrade GUID for in-place upgrades
- Create Start Menu shortcut
- Add uninstaller
- Request network permissions

#### EXE (Portable)
**Alternative:** Create portable .exe for users who prefer no installation

### 3. macOS Packaging

#### DMG (Recommended)
**Advantages:**
- Standard macOS distribution
- Drag-to-Applications UX
- Easy to distribute

**Requirements:**
- **Code signing REQUIRED** for distribution
- **Notarization REQUIRED** for macOS 10.15+

**Notarization Process:**
```bash
# Sign the app
codesign --deep --force --verify --verbose \
  --sign "Developer ID Application: YOUR_NAME" \
  SieveEditor.app

# Create DMG
hdiutil create -volname "SieveEditor" \
  -srcfolder SieveEditor.app -ov SieveEditor.dmg

# Notarize
xcrun notarytool submit SieveEditor.dmg \
  --apple-id "your@email.com" \
  --team-id "TEAM_ID" \
  --password "app-specific-password" \
  --wait

# Staple the ticket
xcrun stapler staple SieveEditor.dmg
```

**Challenge:** Requires Apple Developer Program membership ($99/year)

---

## Code Signing & Attestation

### 1. Artifact Attestations (All Platforms)

**GitHub Native Solution (2024):**
```yaml
- name: Attest Build Provenance
  uses: actions/attest-build-provenance@v2
  with:
    subject-path: |
      app/target/*.jar
      app/target/*.deb
      app/target/*.rpm
      app/target/*.msi
      app/target/*.dmg
```

**Verification:**
```bash
# Verify attestation
gh attestation verify SieveEditor.jar \
  --owner lenucksi \
  --repo SieveEditor
```

**Benefits:**
- Cryptographic proof of build origin
- Verifies artifact hasn't been tampered with
- SLSA Level 3 compliance
- No additional tools required

### 2. JAR Signing (Java-Specific)

**Why Sign JARs?**
- Verifies publisher identity
- Prevents tampering
- Required for some enterprise deployments
- Enables security manager policies

**Implementation:**
```yaml
- name: Sign JAR
  run: |
    jarsigner -keystore ${{ secrets.KEYSTORE_FILE }} \
      -storepass ${{ secrets.KEYSTORE_PASSWORD }} \
      -keypass ${{ secrets.KEY_PASSWORD }} \
      app/target/SieveEditor-jar-with-dependencies.jar \
      sieveeditor
```

**Requirements:**
- Java keystore (.jks or .p12)
- Store credentials in GitHub Secrets
- Consider using timestamping server

### 3. Platform-Specific Signing

#### Windows Code Signing
**Certificate Types:**
- EV (Extended Validation) - Best, no SmartScreen warnings
- OV (Organization Validation) - Good
- Self-signed - Not recommended (triggers warnings)

**Signing Process:**
```powershell
signtool sign /f certificate.pfx /p password /tr http://timestamp.digicert.com SieveEditor.msi
```

#### macOS Code Signing
**Requirements:**
- Apple Developer Program membership
- Developer ID Application certificate
- App-specific password for notarization

**Hardened Runtime:**
```bash
codesign --deep --force --options runtime \
  --entitlements entitlements.plist \
  --sign "Developer ID Application: NAME" \
  SieveEditor.app
```

**Entitlements (entitlements.plist):**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN">
<plist version="1.0">
<dict>
    <key>com.apple.security.network.client</key>
    <true/>  <!-- Required for ManageSieve connections -->
</dict>
</plist>
```

---

## Release Automation

### 1. Release Please (Conventional Commits)

**What is Release Please?**
- Automates version bumps, CHANGELOG generation, and GitHub releases
- Parses Conventional Commit messages
- Creates release PRs automatically

**Commit Message Format:**
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat:` New feature (minor version bump)
- `fix:` Bug fix (patch version bump)
- `feat!:` or `fix!:` Breaking change (major version bump)
- `docs:` Documentation only
- `test:` Adding tests
- `chore:` Maintenance

**Examples:**
```
feat: add multi-profile support

Allow users to manage multiple ManageSieve server profiles
with separate credentials.

Closes #42
```

```
fix!: remove insecure SSL validation

BREAKING CHANGE: SSL certificate validation is now enforced.
Self-signed certificates will be rejected. Use valid
certificates or import CA into system trust store.

Fixes #23
```

**Workflow Configuration:**
```yaml
- uses: googleapis/release-please-action@v4
  with:
    release-type: maven
    package-name: sieveeditor
```

**What It Does:**
1. Scans commits since last release
2. Generates CHANGELOG.md
3. Bumps version in pom.xml
4. Creates Release PR
5. When PR merged, creates GitHub Release

### 2. Release Artifacts

**Attach to GitHub Release:**
- `SieveEditor-${version}.jar` (Uber JAR)
- `SieveEditor-${version}.deb` (Debian/Ubuntu)
- `SieveEditor-${version}.rpm` (Fedora/RHEL)
- `SieveEditor-${version}.msi` (Windows)
- `SieveEditor-${version}.dmg` (macOS)
- `SieveEditor-${version}.flatpak` (Universal Linux)
- `checksums.txt` (SHA256 hashes)
- `attestation.intoto.jsonl` (SLSA provenance)

**Generate Checksums:**
```bash
sha256sum SieveEditor-* > checksums.txt
```

### 3. Release Notes Automation

**Auto-Generated Sections:**
```markdown
## What's Changed

### Features
* Add multi-profile support by @contributor in #42
* Implement find/replace with regex by @contributor in #45

### Bug Fixes
* Fix array index out of bounds on no row selection by @contributor in #57
* Fix tokenizer bug with number+letter combinations by @contributor in #58

### Security
* Remove hardcoded encryption key by @contributor in #23
* Enable SSL certificate validation by @contributor in #24

## New Contributors
* @newcontributor made their first contribution in #42

**Full Changelog**: https://github.com/lenucksi/SieveEditor/compare/v0.0.1...v0.0.2
```

### 4. Publication Channels

**GitHub Releases:**
- Primary distribution
- Download links for all platforms
- Includes attestations

**Future Considerations:**
- **Maven Central** - For library users
- **Flathub** - For Flatpak distribution
- **Homebrew** - For macOS users (`brew install sieveeditor`)
- **Chocolatey** - For Windows users (`choco install sieveeditor`)
- **Snapcraft** - Alternative Linux universal packaging

---

## Local Development Workflow

### 1. Running Tests Locally

**Quick Test Run:**
```bash
cd app
mvn test
```

**With Coverage Report:**
```bash
cd app
mvn clean test
# View report: app/target/site/jacoco/index.html
```

**Specific Test Class:**
```bash
mvn test -Dtest=PropertiesSieveTest
```

**Specific Test Method:**
```bash
mvn test -Dtest=PropertiesSieveTest#shouldSaveAndLoadProperties
```

### 2. Building Packages Locally

**Build JAR:**
```bash
mvn clean package
# Output: app/target/SieveEditor-jar-with-dependencies.jar
```

**Build Native Package (requires platform-specific tools):**

**Linux (DEB):**
```bash
# Prerequisites: Java 21
sudo apt-get install fakeroot

# Build
jpackage --input app/target \
  --main-jar SieveEditor-jar-with-dependencies.jar \
  --main-class de.febrildur.sieveeditor.Application \
  --name SieveEditor \
  --app-version 0.0.1 \
  --type deb \
  --linux-shortcut

# Output: sieveeditor_0.0.1_amd64.deb
```

**Linux (RPM):**
```bash
# Prerequisites
sudo dnf install rpm-build

# Build (same as DEB but --type rpm)
```

**Linux (Flatpak):**
```bash
# Prerequisites
sudo apt-get install flatpak flatpak-builder

# Build
flatpak-builder --force-clean build-dir de.febrildur.sieveeditor.yml
flatpak build-bundle build-dir SieveEditor.flatpak de.febrildur.sieveeditor

# Install locally
flatpak install SieveEditor.flatpak
```

**Windows (MSI):**
```powershell
# Prerequisites: WiX Toolset 3.0+
# Download: https://wixtoolset.org/

# Build
jpackage --input app\target `
  --main-jar SieveEditor-jar-with-dependencies.jar `
  --main-class de.febrildur.sieveeditor.Application `
  --name SieveEditor `
  --app-version 0.0.1 `
  --type msi `
  --win-menu `
  --win-shortcut

# Output: SieveEditor-0.0.1.msi
```

**macOS (DMG):**
```bash
# Prerequisites: Xcode Command Line Tools
xcode-select --install

# Build
jpackage --input app/target \
  --main-jar SieveEditor-jar-with-dependencies.jar \
  --main-class de.febrildur.sieveeditor.Application \
  --name SieveEditor \
  --app-version 0.0.1 \
  --type dmg

# Output: SieveEditor-0.0.1.dmg
```

### 3. Pre-Commit Checks

**Create `.git/hooks/pre-commit`:**
```bash
#!/bin/bash
echo "Running tests before commit..."
cd app && mvn test -q

if [ $? -ne 0 ]; then
    echo "❌ Tests failed. Commit aborted."
    exit 1
fi

echo "✅ All tests passed."
exit 0
```

**Make executable:**
```bash
chmod +x .git/hooks/pre-commit
```

### 4. Local Security Scanning

**OWASP Dependency Check:**
```bash
mvn org.owasp:dependency-check-maven:check
# Report: app/target/dependency-check-report.html
```

**Check for Outdated Dependencies:**
```bash
mvn versions:display-dependency-updates
```

---

## Implementation Roadmap

### Phase 1: CI Foundation (Week 1)
- ✅ Create `.github/workflows/ci.yml`
- ✅ Configure test matrix (Linux, Windows, macOS)
- ✅ Set up Maven caching
- ✅ Add test reporting
- ✅ Configure OpenSSF Scorecard

### Phase 2: Basic Packaging (Week 2)
- ✅ Create `.github/workflows/package.yml`
- ✅ Implement jpackage for DEB, RPM, MSI, DMG
- ✅ Test package installation on each platform
- ✅ Add artifact attestations

### Phase 3: Release Automation (Week 3)
- ✅ Set up release-please
- ✅ Configure Conventional Commits
- ✅ Create release workflow
- ✅ Automate CHANGELOG generation
- ✅ Publish artifacts to GitHub Releases

### Phase 4: Code Signing (Week 4)
- ✅ Set up JAR signing
- ⏳ Obtain Windows code signing certificate
- ⏳ Obtain macOS Developer ID certificate
- ✅ Implement platform-specific signing
- ✅ Add notarization for macOS

### Phase 5: Flatpak & Distribution (Week 5-6)
- ✅ Create Flatpak manifest
- ✅ Test Flatpak builds
- ✅ Submit to Flathub (optional)
- ⏳ Create Homebrew formula
- ⏳ Create Chocolatey package

### Phase 6: Documentation & Maintenance
- ✅ Document build process
- ✅ Create contributor guidelines
- ✅ Set up Dependabot
- ✅ Configure branch protection rules

---

## Security Checklist

- [ ] OpenSSF Scorecard enabled
- [ ] Dependabot configured for Maven
- [ ] Branch protection on main (require PR, require reviews)
- [ ] CODEOWNERS file created
- [ ] Security policy (SECURITY.md) documented
- [ ] Artifact attestations for all releases
- [ ] JAR signing enabled
- [ ] Platform-specific code signing (Windows, macOS)
- [ ] Vulnerability scanning in CI
- [ ] Least-privilege permissions in workflows
- [ ] No hardcoded secrets (use GitHub Secrets)
- [ ] OIDC tokens instead of long-lived credentials
- [ ] Signed commits encouraged (not enforced initially)

---

## Cost Considerations

### Free (GitHub Free/Pro)
- GitHub Actions (2000 minutes/month for private repos)
- GitHub Releases
- Artifact Attestations
- OpenSSF Scorecard
- Dependabot

### Paid Services (Optional)
- **Windows Code Signing Certificate:** $50-300/year (EV: $500+/year)
- **Apple Developer Program:** $99/year (required for macOS distribution)
- **Additional CI minutes:** $0.008/minute (Linux), $0.016/minute (Windows), $0.08/minute (macOS)

### Recommendations
- Start with JAR + attestations (free)
- Add Linux packages (DEB/RPM/Flatpak) - free
- Windows MSI without signing initially (triggers SmartScreen warning)
- macOS DMG requires paid Developer Program

---

## References

1. **GitHub Artifact Attestations (Dec 2024)**
   - https://github.blog/enterprise-software/devsecops/enhance-build-security-and-reach-slsa-level-3-with-github-artifact-attestations/
   - https://docs.github.com/en/actions/security-for-github-actions/using-artifact-attestations

2. **SLSA Framework**
   - https://slsa.dev/
   - https://github.com/slsa-framework/slsa-github-generator

3. **OpenSSF Scorecard**
   - https://scorecard.dev/
   - https://github.com/ossf/scorecard-action

4. **jpackage Documentation (Java 21)**
   - https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html
   - https://docs.oracle.com/en/java/javase/21/jpackage/

5. **Release Please**
   - https://github.com/googleapis/release-please-action
   - https://www.conventionalcommits.org/

6. **Flatpak Packaging**
   - https://docs.flatpak.org/
   - https://github.com/flathub/flathub

---

## Conclusion

This comprehensive CI/CD strategy provides:
- ✅ **Security:** SLSA Level 3, attestations, signing, Scorecard
- ✅ **Automation:** Tests on every commit, automatic releases
- ✅ **Distribution:** Cross-platform packages for Linux, Windows, macOS
- ✅ **Transparency:** Automated changelogs, contributor attribution
- ✅ **Developer Experience:** Local testing, pre-commit hooks

**Next Steps:**
1. Review and approve this strategy
2. Implement Phase 1 (CI Foundation)
3. Set up repository secrets for signing
4. Test workflows on feature branch
5. Roll out to main branch with branch protection
