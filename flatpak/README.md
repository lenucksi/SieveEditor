# Flatpak Packaging Files

This directory contains files required for Flatpak packaging.

## Files

- **`de.febrildur.sieveeditor.desktop`** - FreeDesktop desktop entry
  - Defines application name, icon, categories
  - Used by desktop environments to show the app in menus

- **`de.febrildur.sieveeditor.metainfo.xml`** - AppStream metadata
  - Application description, screenshots, release notes
  - Required for Flathub submission
  - Used by software centers (GNOME Software, KDE Discover)

- **`de.febrildur.sieveeditor.svg`** - Application icon (vector source)
  - 256x256 icon in SVG format
  - Blue gradient background with envelope and filter funnel
  - Source file for PNG conversion

- **`de.febrildur.sieveeditor.png`** - Application icon (raster)
  - 256x256 PNG format (required for Flatpak)
  - **TODO:** Convert from SVG using ImageMagick, Inkscape, or online tool

## Converting Icon

The PNG icon needs to be generated from the SVG source:

### Option 1: ImageMagick
```bash
convert -background none -density 300 de.febrildur.sieveeditor.svg \
  -resize 256x256 de.febrildur.sieveeditor.png
```

### Option 2: Inkscape
```bash
inkscape de.febrildur.sieveeditor.svg \
  --export-type=png \
  --export-filename=de.febrildur.sieveeditor.png \
  --export-width=256 --export-height=256
```

### Option 3: rsvg-convert
```bash
rsvg-convert -w 256 -h 256 de.febrildur.sieveeditor.svg \
  -o de.febrildur.sieveeditor.png
```

### Option 4: Online Converter
- Upload SVG to https://cloudconvert.com/svg-to-png
- Set output to 256x256
- Download and save as `de.febrildur.sieveeditor.png`

## Testing

After converting the icon, verify it:

```bash
file de.febrildur.sieveeditor.png
# Should output: PNG image data, 256 x 256, 8-bit/color RGBA

identify de.febrildur.sieveeditor.png
# Should show: de.febrildur.sieveeditor.png PNG 256x256
```

## Building Flatpak

See `../de.febrildur.sieveeditor.yml` for the Flatpak manifest.

Build locally:
```bash
# From project root
flatpak-builder --force-clean build-dir de.febrildur.sieveeditor.yml
flatpak build-bundle build-dir SieveEditor.flatpak de.febrildur.sieveeditor
```

Install:
```bash
flatpak install --user SieveEditor.flatpak
flatpak run de.febrildur.sieveeditor
```

## Submitting to Flathub

See `../FLATPAK-PACKAGING-REPORT.md` for complete submission instructions.

Prerequisites:
- [x] Icon converted to PNG
- [ ] Screenshot created and committed
- [ ] Flatpak tested locally

Then submit PR to https://github.com/flathub/flathub
