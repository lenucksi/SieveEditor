# SieveEditor - Build Dependencies

This document lists all dependencies required for building SieveEditor locally in various formats.

## Core Build Requirements

### Always Required

| Tool | Purpose | Check Command | Install (Arch) |
|------|---------|---------------|----------------|
| **Java 21 JDK** | Compile and run Java code | `java -version` | `sudo pacman -S jdk21-openjdk` |
| **Maven 3.6+** | Build system and dependency management | `mvn --version` | `sudo pacman -S maven` |
| **Git** | Version control | `git --version` | `sudo pacman -S git` |

### Verify Core Setup

```bash
# Check Java version (should show 21)
java -version

# Check Maven version (should show 3.6+)
mvn --version

# Check Git
git --version
```

## Package-Specific Dependencies

### JAR (Universal Java)

**No additional dependencies** - Just Maven and Java 21.

```bash
# Build
mvn clean package

# Output: target/SieveEditor-jar-with-dependencies.jar
```

---

### DEB Package (Debian/Ubuntu)

| Tool | Purpose | Install (Arch) | Install (Debian/Ubuntu) |
|------|---------|----------------|-------------------------|
| **jpackage** | Create native packages | Included in JDK 21 | Included in JDK 21 |
| **fakeroot** | Package creation | `sudo pacman -S fakeroot` | `sudo apt install fakeroot` |

```bash
# Build DEB
jpackage \
  --input target \
  --main-jar SieveEditor-jar-with-dependencies.jar \
  --main-class de.febrildur.sieveeditor.Application \
  --name SieveEditor \
  --app-version 0.0.1 \
  --type deb
```

---

### RPM Package (Fedora/RHEL/Arch)

| Tool | Purpose | Install (Arch) | Install (Fedora) |
|------|---------|----------------|------------------|
| **jpackage** | Create native packages | Included in JDK 21 | Included in JDK 21 |
| **rpm-tools** | RPM package creation | `sudo pacman -S rpm-tools` | `sudo dnf install rpm-build` |

```bash
# Build RPM
jpackage \
  --input target \
  --main-jar SieveEditor-jar-with-dependencies.jar \
  --main-class de.febrildur.sieveeditor.Application \
  --name SieveEditor \
  --app-version 0.0.1 \
  --type rpm
```

---

### Flatpak (Universal Linux)

| Tool | Purpose | Install (Arch) | Required |
|------|---------|----------------|----------|
| **flatpak** | Runtime system | `sudo pacman -S flatpak` | ✅ Yes |
| **flatpak-builder** | Build Flatpaks | `sudo pacman -S flatpak-builder` | ✅ Yes |
| **org.flatpak.Builder** | Linter (optional) | `flatpak install flathub org.flatpak.Builder` | ⚠️ Optional |

#### Flatpak Runtimes (auto-installed by script)

```bash
# These are automatically installed by build-flatpak.sh
flatpak install flathub org.freedesktop.Platform//24.08
flatpak install flathub org.freedesktop.Sdk//24.08
flatpak install flathub org.freedesktop.Sdk.Extension.openjdk21//24.08
```

#### Build Flatpak

```bash
# Automated (recommended)
./build-flatpak.sh

# Manual
flatpak-builder --force-clean --repo=flatpak-repo flatpak-build de.febrildur.sieveeditor.yml
flatpak build-bundle flatpak-repo SieveEditor.flatpak de.febrildur.sieveeditor
```

#### Flatpak-builder-lint (Optional Quality Tool)

The linter validates your Flatpak manifest and built packages. It's available as a Flatpak:

```bash
# Install linter (optional)
flatpak install flathub org.flatpak.Builder

# Lint manifest manually
flatpak run --command=flatpak-builder-lint org.flatpak.Builder \
  manifest de.febrildur.sieveeditor.yml

# Lint built repository manually
flatpak run --command=flatpak-builder-lint org.flatpak.Builder \
  repo flatpak-repo
```

**Note:** The build script (`build-flatpak.sh`) automatically detects and uses the linter if available, but will proceed without it if not installed.

---

### Windows MSI (Windows only)

| Tool | Purpose | Install (Windows) |
|------|---------|-------------------|
| **JDK 21** | Java compiler | Download from [Adoptium](https://adoptium.net/) |
| **Maven** | Build system | `choco install maven` |
| **WiX Toolset** | MSI creation | `choco install wixtoolset` |
| **jpackage** | Package creator | Included in JDK 21 |

```powershell
# Build MSI (Windows only)
jpackage `
  --input target `
  --main-jar SieveEditor-jar-with-dependencies.jar `
  --main-class de.febrildur.sieveeditor.Application `
  --name SieveEditor `
  --app-version 0.0.1 `
  --type msi
```

---

### macOS DMG (macOS only)

| Tool | Purpose | Install (macOS) |
|------|---------|-----------------|
| **JDK 21** | Java compiler | `brew install openjdk@21` |
| **Maven** | Build system | `brew install maven` |
| **jpackage** | Package creator | Included in JDK 21 |

```bash
# Build DMG (macOS only)
jpackage \
  --input target \
  --main-jar SieveEditor-jar-with-dependencies.jar \
  --main-class de.febrildur.sieveeditor.Application \
  --name SieveEditor \
  --app-version 1.0.0 \
  --type dmg
```

---

## Development Dependencies (Optional)

### Code Quality Tools

```bash
# OWASP Dependency Check (security scanning)
mvn org.owasp:dependency-check-maven:check

# JaCoCo Coverage Report
mvn jacoco:report

# Spotless (code formatting - if configured)
mvn spotless:check
mvn spotless:apply
```

### Git Hooks (Conventional Commits)

```bash
# Enable project git hooks
git config core.hooksPath .githooks

# Dependencies: none (pure bash scripts)
```

---

## Quick Install Commands

### Arch Linux

```bash
# Core dependencies
sudo pacman -S jdk21-openjdk maven git

# Flatpak packaging
sudo pacman -S flatpak flatpak-builder

# RPM packaging (if needed)
sudo pacman -S rpm-tools fakeroot

# Optional: Flatpak linter (as Flatpak app)
flatpak install flathub org.flatpak.Builder
```

### Debian/Ubuntu

```bash
# Core dependencies
sudo apt update
sudo apt install openjdk-21-jdk maven git

# DEB packaging
sudo apt install fakeroot

# Flatpak packaging
sudo apt install flatpak flatpak-builder
```

### Fedora/RHEL

```bash
# Core dependencies
sudo dnf install java-21-openjdk-devel maven git

# RPM packaging
sudo dnf install rpm-build

# Flatpak packaging
sudo dnf install flatpak flatpak-builder
```

### macOS

```bash
# Install Homebrew if needed
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Core dependencies
brew install openjdk@21 maven git

# Note: jpackage is included in OpenJDK 21
```

### Windows

```powershell
# Install Chocolatey if needed
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))

# Core dependencies
choco install openjdk21 maven git

# MSI packaging
choco install wixtoolset
```

---

## Verification Script

Save as `check-dependencies.sh`:

```bash
#!/bin/bash
# Check if all build dependencies are installed

echo "Checking core dependencies..."

check_command() {
    if command -v $1 &> /dev/null; then
        echo "✓ $1 installed: $($1 --version 2>&1 | head -1)"
    else
        echo "✗ $1 NOT installed"
        return 1
    fi
}

check_command java
check_command mvn
check_command git

echo ""
echo "Checking packaging tools..."
check_command jpackage || echo "  (jpackage is part of JDK 21)"

echo ""
echo "Checking optional tools..."
check_command flatpak || echo "  (Install for Flatpak packaging)"
check_command flatpak-builder || echo "  (Install for Flatpak packaging)"
check_command rpm || echo "  (Install for RPM packaging)"
check_command fakeroot || echo "  (Install for DEB packaging)"

echo ""
echo "Checking Flatpak runtimes..."
if command -v flatpak &> /dev/null; then
    if flatpak list --runtime | grep -q "org.freedesktop.Platform.*24.08"; then
        echo "✓ Freedesktop Platform 24.08 installed"
    else
        echo "✗ Freedesktop Platform 24.08 NOT installed"
        echo "  Run: flatpak install flathub org.freedesktop.Platform//24.08"
    fi
fi
```

Make executable and run:

```bash
chmod +x check-dependencies.sh
./check-dependencies.sh
```

---

## Minimal Setup (JAR only)

To just build and run the JAR (no native packaging):

```bash
# Install only
sudo pacman -S jdk21-openjdk maven

# Build and run
mvn clean package
java -jar target/SieveEditor-jar-with-dependencies.jar
```

---

## Recommended Setup for Development

```bash
# Core tools
sudo pacman -S jdk21-openjdk maven git

# Flatpak (universal Linux packaging)
sudo pacman -S flatpak flatpak-builder

# Enable git hooks
git config core.hooksPath .githooks
```

---

## Troubleshooting

### "JAVA_HOME not set"

```bash
# Find Java installation
which java

# Set JAVA_HOME (add to ~/.bashrc or ~/.zshrc)
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
export PATH=$JAVA_HOME/bin:$PATH
```

### "Maven command not found"

```bash
# Arch
sudo pacman -S maven

# Verify
mvn --version
```

### "jpackage: command not found"

jpackage is included in JDK 21. If missing:

```bash
# Verify JDK 21 is installed
java -version

# Check if jpackage exists
which jpackage

# If not found, ensure JAVA_HOME/bin is in PATH
export PATH=$JAVA_HOME/bin:$PATH
```

### Flatpak runtime download fails

```bash
# Add Flathub remote if missing
flatpak remote-add --if-not-exists flathub https://flathub.org/repo/flathub.flatpakrepo

# Try again
flatpak install flathub org.freedesktop.Platform//24.08
```

---

## Summary Table

| Format | Platform | Required Tools | Build Script |
|--------|----------|----------------|--------------|
| **JAR** | All | Java 21, Maven | `mvn clean package` |
| **DEB** | Debian/Ubuntu | + fakeroot | `jpackage --type deb` |
| **RPM** | Fedora/RHEL/Arch | + rpm-tools | `jpackage --type rpm` |
| **Flatpak** | All Linux | + flatpak, flatpak-builder | `./build-flatpak.sh` |
| **MSI** | Windows | + WiX Toolset | `jpackage --type msi` |
| **DMG** | macOS | (none extra) | `jpackage --type dmg` |

---

## CI/CD Dependencies

GitHub Actions automatically provides all dependencies. See `.github/workflows/package.yml` for details.

For local CI testing with [act](https://github.com/nektos/act):

```bash
# Install act
sudo pacman -S act

# Run workflows locally
act -j build-jar
```

---

**Last Updated:** 2025-11-17
**Project Version:** 0.0.1-SNAPSHOT
