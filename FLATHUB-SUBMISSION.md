# Flathub Submission Guide

Quick guide to publish SieveEditor on Flathub.

## Prerequisites

- [x] Flatpak builds and runs locally
- [x] Screenshots added to metainfo
- [x] All linter warnings reviewed (remaining warnings are acceptable)
- [ ] Test on multiple distributions

## Submission Steps

### 1. Fork Flathub Repository

```bash
# Go to https://github.com/flathub/flathub
# Click "Fork" button
```

### 2. Create Application Repository

```bash
git clone git@github.com:YOUR_USERNAME/flathub.git
cd flathub
git submodule add https://github.com/flathub/de.febrildur.sieveeditor.git
```

### 3. Prepare Manifest for Flathub

Create new repository: `https://github.com/flathub/de.febrildur.sieveeditor`

Copy these files to the new repo:

- `de.febrildur.sieveeditor.yml` (manifest)
- `flatpak/de.febrildur.sieveeditor.desktop`
- `flatpak/de.febrildur.sieveeditor.metainfo.xml`
- `flatpak/de.febrildur.sieveeditor.png`

**Important Changes for Flathub:**

```yaml
# In de.febrildur.sieveeditor.yml, change source to GitHub release:
sources:
  - type: archive
    url: https://github.com/lenucksi/SieveEditor/releases/download/v0.0.1/SieveEditor-jar-with-dependencies.jar
    sha256: <calculate-this>
    dest-filename: SieveEditor-jar-with-dependencies.jar
```

### 4. Calculate SHA256

```bash
cd SieveEditor
mvn clean package
sha256sum target/SieveEditor-jar-with-dependencies.jar
```

### 5. Create GitHub Release

1. Go to <https://github.com/lenucksi/SieveEditor/releases>
2. Click "Create a new release"
3. Tag: `v0.0.1`
4. Upload: `target/SieveEditor-jar-with-dependencies.jar`
5. Publish release

### 6. Submit to Flathub

```bash
cd flathub/de.febrildur.sieveeditor
git add .
git commit -m "Initial submission of SieveEditor"
git push origin main

# Create PR at https://github.com/flathub/flathub
# Title: "New app: SieveEditor"
# Description: Link to your app repo
```

### 7. Review Process

- Flathub team reviews manifest
- Automated tests run
- May request changes
- Once approved, app appears on Flathub within 24h

## Alternative: Flathub Beta

For faster testing, submit to beta first:

- <https://github.com/flathub-infra/flathub-beta-stats>
- Same process, less strict review
- Good for getting feedback

## Post-Submission

After approval:

```bash
flatpak install flathub de.febrildur.sieveeditor
```

Your app will appear at: `https://flathub.org/apps/de.febrildur.sieveeditor`

## References

- [Flathub Submission Guide](https://docs.flathub.org/docs/for-app-authors/submission)
- [App Requirements](https://docs.flathub.org/docs/for-app-authors/requirements)
- [Linter Docs](https://docs.flathub.org/docs/for-app-authors/linter)
