# 4K Display Scaling Issue - Investigation & Solutions

## Problem Statement

**User Report:**
> "Aktuell wird unter Linux leider die UI auf einem 4K Monitor winzig gerendert, das war vorher nicht der Fall, da skalierte es normal mit 4k. Das ist vermutlich ein Bug in entweder einer aktualisierten Version von Gnome mit der Java nicht zurecht kommt, oder in der App, oder in Java."

**Symptoms:**

- UI renders extremely small on 4K (3840×2160) displays
- Previously worked correctly with automatic scaling
- Occurred after GNOME or Java update
- Specific to Linux/GNOME environment

## Background: Java HiDPI Support

### How Java Detects Display Scaling

Java 9+ includes HiDPI support that attempts to detect display scaling from the desktop environment:

1. **X11/Xft DPI:** Reads `Xft.dpi` from X resources
2. **GDK_SCALE:** Reads environment variable set by GNOME
3. **Desktop Settings:** Queries GNOME settings via gsettings
4. **System Properties:** Can be overridden with JVM flags

### What Changed Recently

**GNOME 40+ (2021) and Later:**

- Changed how fractional scaling is reported
- Wayland became default in many distributions
- Different DPI reporting mechanism than X11
- Java's detection may not work correctly

**Java 11 (LTS):**

- HiDPI support exists but requires explicit enabling
- Detection logic may not handle new GNOME versions
- Requires `-Dsun.java2d.uiScale.enabled=true` on some systems

## Hypothesis Testing Plan

### Hypothesis 1: Java HiDPI Detection Disabled

**Theory:** Java's HiDPI support isn't enabled by default and requires explicit JVM flag.

**Test:**

```bash
# Current behavior (tiny UI)
java -jar SieveEditor-jar-with-dependencies.jar

# Test with HiDPI explicitly enabled
java -Dsun.java2d.uiScale.enabled=true \
     -jar SieveEditor-jar-with-dependencies.jar
```

-> Did not have an effect.

**Expected Result:** If this hypothesis is correct, the second command will render UI at proper size.

**How to Verify:**

- UI text should be readable
- Buttons should be normally sized
- Compare window size to other applications

---

### Hypothesis 2: Scale Factor Not Auto-Detected

**Theory:** Java can't read GNOME's scale factor from environment.

**Test:**

```bash
# Check what GNOME is using
gsettings get org.gnome.desktop.interface scaling-factor
echo "GDK_SCALE: $GDK_SCALE"
echo "GDK_DPI_SCALE: $GDK_DPI_SCALE"
xrdb -query | grep Xft.dpi

->
 gsettings get org.gnome.desktop.interface scaling-factor
echo "GDK_SCALE: $GDK_SCALE"
echo "GDK_DPI_SCALE: $GDK_DPI_SCALE"
xrdb -query | grep Xft.dpi
uint32 0
GDK_SCALE:
GDK_DPI_SCALE:
Xft.dpi:	192


# Test with explicit scale factor
java -Dsun.java2d.uiScale.enabled=true \
     -Dsun.java2d.uiScale=2.0 \
     -jar SieveEditor-jar-with-dependencies.jar
-> Did work.

# Try different scales
for scale in 1.25 1.5 1.75 2.0 2.5; do
    echo "Testing scale: $scale"
    java -Dsun.java2d.uiScale.enabled=true \
         -Dsun.java2d.uiScale=$scale \
         -jar SieveEditor-jar-with-dependencies.jar
done
```

-> Alles unter 2.0 ist zu klein. 2.5 sieht wie 2.0 aus.

**Expected Result:** One of these scale factors should render properly.

**How to Verify:**

- 1.0 = No scaling (tiny on 4K)
- 2.0 = 200% scaling (typical for 4K)
- 1.5 = 150% scaling (some prefer this)
- UI should match other applications at correct scale

---

### Hypothesis 3: Wayland vs X11 Backend Issue

**Theory:** Java behaves differently on Wayland vs X11.

**Test:**

```bash
# Check current session type
echo "Session type: $XDG_SESSION_TYPE"
echo "Wayland display: $WAYLAND_DISPLAY"
echo "X11 display: $DISPLAY"

# Force X11 backend (if currently Wayland)
GDK_BACKEND=x11 java -jar SieveEditor-jar-with-dependencies.jar

# Force Wayland backend (if currently X11)
GDK_BACKEND=wayland java -jar SieveEditor-jar-with-dependencies.jar

-> letzte beide: kein effekt.

# Try with X11 + scale settings
GDK_BACKEND=x11 \
GDK_SCALE=2 \
java -Dsun.java2d.uiScale.enabled=true \
     -jar SieveEditor-jar-with-dependencies.jar
```

-> letztes: funktioniert wie mit der 2.0 scaling von voher.

**Expected Result:** May work better on one backend vs the other.

**How to Verify:**

- Compare UI rendering on both backends
- Check if scale detection works on X11 but not Wayland
- Note which backend gives better results

---

### Hypothesis 4: Font DPI Settings Issue

**Theory:** Java reads font DPI settings that aren't set correctly.

**Test:**

```bash
# Check current DPI settings
xdpyinfo | grep resolution
xrdb -query | grep dpi

# Test with explicit DPI
java -Dsun.java2d.uiScale.enabled=true \
     -Dawt.useSystemAAFontSettings=lcd \
     -Dswing.aatext=true \
     -jar SieveEditor-jar-with-dependencies.jar

# Set custom DPI
xrandr --dpi 192  # 192 = 2x scaling (96 * 2)
java -jar SieveEditor-jar-with-dependencies.jar
```

-> Beide ohne Effekt, result zu klein.

**Expected Result:** Correct DPI setting may fix rendering.

**How to Verify:**

- Font size should be appropriate
- UI elements should scale proportionally
- Compare to other Java applications

---

### Hypothesis 5: GNOME Settings Not Read

**Theory:** Java can't query GNOME settings for scaling factor.

**Test:**

```bash
# Check GNOME scaling settings
gsettings get org.gnome.desktop.interface scaling-factor -> uint32 0
gsettings get org.gnome.desktop.interface text-scaling-factor -> 1.0
gsettings get org.gnome.mutter experimental-features  # Check for 'scale-monitor-framebuffer' -> vorhanden

# Test with scale factor from GNOME
SCALE=$(gsettings get org.gnome.desktop.interface scaling-factor | cut -d' ' -f2)
echo "GNOME scale factor: $SCALE"

java -Dsun.java2d.uiScale.enabled=true \
     -Dsun.java2d.uiScale=$SCALE \
     -jar SieveEditor-jar-with-dependencies.jar
```

**Expected Result:** Using GNOME's scale factor should work.

**How to Verify:**

- If GNOME says 2, Java should use 2
- UI should match GNOME applications
- Check if scale changes when GNOME setting changes

---

### Hypothesis 6: RSyntaxTextArea Specific Issue

**Theory:** The text editor component (RSyntaxTextArea) doesn't scale properly.

**Test:**

```bash
# Run with various scaling and check if text area is the problem
java -Dsun.java2d.uiScale=2.0 \
     -Dsun.java2d.uiScale.enabled=true \
     -jar SieveEditor-jar-with-dependencies.jar

-> Rendert in der Korrekten Größe.

# After launch, check:
# - Are menus properly sized?
# - Are buttons properly sized?
# - Is ONLY the text area tiny?
```

**Expected Result:** If true, only text editor will be tiny while UI is correct.

**How to Verify:**

- Compare menu bar size to text editor size
- Check if problem is component-specific
- May need to set font size explicitly in code

---

## Testing Procedure

### Step 1: Environment Information Gathering

Run this script first to collect environment information:

```bash
#!/bin/bash
echo "=== Display Information ==="
xrandr | grep -A1 "connected primary"
xdpyinfo | grep -E "dimensions|resolution"

echo -e "\n=== GNOME Settings ==="
gsettings get org.gnome.desktop.interface scaling-factor
gsettings get org.gnome.desktop.interface text-scaling-factor

echo -e "\n=== Environment Variables ==="
echo "XDG_SESSION_TYPE: $XDG_SESSION_TYPE"
echo "GDK_SCALE: $GDK_SCALE"
echo "GDK_DPI_SCALE: $GDK_DPI_SCALE"
echo "QT_SCALE_FACTOR: $QT_SCALE_FACTOR"

echo -e "\n=== X Resources ==="
xrdb -query | grep -E "Xft.dpi|Xft.autohint|Xft.hinting"

echo -e "\n=== Java Version ==="
java -version
```

**Save output to:** `environment-info.txt`

### Step 2: Systematic Testing

Test each hypothesis in order, documenting results:

```bash
#!/bin/bash
# Save this as test-scaling.sh

JARFILE="SieveEditor-jar-with-dependencies.jar"

echo "Test 1: Enable HiDPI"
java -Dsun.java2d.uiScale.enabled=true -jar $JARFILE &
sleep 3
echo "Check UI size. Press Enter to continue..."
read
killall java

echo "Test 2: Explicit scale 2.0"
java -Dsun.java2d.uiScale.enabled=true -Dsun.java2d.uiScale=2.0 -jar $JARFILE &
sleep 3
echo "Check UI size. Press Enter to continue..."
read
killall java

echo "Test 3: X11 backend with scale"
GDK_BACKEND=x11 GDK_SCALE=2 java -Dsun.java2d.uiScale.enabled=true -jar $JARFILE &
sleep 3
echo "Check UI size. Press Enter to continue..."
read
killall java

echo "Test 4: All options combined"
GDK_BACKEND=x11 \
GDK_SCALE=2 \
java -Dsun.java2d.uiScale.enabled=true \
     -Dsun.java2d.uiScale=2.0 \
     -Dawt.useSystemAAFontSettings=lcd \
     -Dswing.aatext=true \
     -jar $JARFILE &
sleep 3
echo "Check UI size. Press Enter to continue..."
read
killall java

echo "Testing complete!"
```

### Step 3: Document Results

For each test, record:

- ✅ UI properly sized
- ❌ Still too small
- ⚠️ Partially working (specify what)

Example results table:

| Test | Result | Notes |
|------|--------|-------|
| Hypothesis 1 | ✅ | Enabling HiDPI fixed it |
| Hypothesis 2 | N/A | Already fixed by H1 |
| ... | ... | ... |

---

## Solutions (Ordered by Preference)

### Solution 1: Launcher Script (RECOMMENDED)

**Why:** Doesn't require code changes, easy for users to customize.

Create `sieveeditor.sh`:

```bash
#!/bin/bash
# SieveEditor launcher with automatic HiDPI support

# Get the directory where the script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JARFILE="$SCRIPT_DIR/SieveEditor-jar-with-dependencies.jar"

# Detect scale factor from GNOME
if command -v gsettings &> /dev/null; then
    GNOME_SCALE=$(gsettings get org.gnome.desktop.interface scaling-factor 2>/dev/null | cut -d' ' -f2)
fi

# Use GDK_SCALE if set, otherwise use GNOME setting, otherwise default to 2.0 for 4K
SCALE=${GDK_SCALE:-${GNOME_SCALE:-2.0}}

echo "Using scale factor: $SCALE"

# Java options for HiDPI support
JAVA_OPTS="-Dsun.java2d.uiScale.enabled=true"
JAVA_OPTS="$JAVA_OPTS -Dsun.java2d.uiScale=$SCALE"
JAVA_OPTS="$JAVA_OPTS -Dawt.useSystemAAFontSettings=lcd"
JAVA_OPTS="$JAVA_OPTS -Dswing.aatext=true"

# Optional: Force X11 backend (uncomment if Wayland causes issues)
# export GDK_BACKEND=x11

# Launch application
exec java $JAVA_OPTS -jar "$JARFILE" "$@"
```

**Usage:**

```bash
chmod +x sieveeditor.sh
./sieveeditor.sh
```

**Pros:**

- No code changes needed
- Users can customize
- Easy to test different settings
- Can be distributed with release

**Cons:**

- Users must use launcher instead of direct jar
- Requires bash

---

### Solution 2: Code Changes in Application

**Why:** Works automatically, no launcher needed.

Add to `Application.java` constructor (line ~55):

```java
private void setupHiDPI() {
    // Enable HiDPI support
    System.setProperty("sun.java2d.uiScale.enabled", "true");

    // Try to detect scale from environment
    String gdkScale = System.getenv("GDK_SCALE");
    if (gdkScale != null && !gdkScale.isEmpty()) {
        try {
            double scale = Double.parseDouble(gdkScale);
            System.setProperty("sun.java2d.uiScale", String.valueOf(scale));
        } catch (NumberFormatException ignored) {}
    } else {
        // Try to read from GNOME settings
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "gsettings", "get", "org.gnome.desktop.interface", "scaling-factor"
            );
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream())
            );
            String line = reader.readLine();
            if (line != null && line.contains(" ")) {
                String scaleStr = line.split(" ")[1];
                System.setProperty("sun.java2d.uiScale", scaleStr);
            }
        } catch (Exception ignored) {
            // Fallback to 2.0 for 4K displays
            System.setProperty("sun.java2d.uiScale", "2.0");
        }
    }

    // Better font rendering
    System.setProperty("awt.useSystemAAFontSettings", "lcd");
    System.setProperty("swing.aatext", "true");
}

// Call in constructor before UI setup
public Application() throws IOException, ParseException, BadLocationException {
    setupHiDPI(); // Add this line
    prop = new PropertiesSieve();
    prop.load();
    // ... rest of constructor
}
```

**Pros:**

- Works automatically
- No launcher script needed
- Users don't need to do anything

**Cons:**

- Requires code changes
- Harder to customize per-user
- May not work on all systems

---

### Solution 3: Desktop Entry with Exec Options

**Why:** Standard Linux application integration.

Create `sieveeditor.desktop`:

```desktop
[Desktop Entry]
Type=Application
Name=SieveEditor
Comment=Sieve mail filter script editor
Exec=env GDK_SCALE=2 java -Dsun.java2d.uiScale.enabled=true -Dsun.java2d.uiScale=2.0 -jar /path/to/SieveEditor-jar-with-dependencies.jar
Icon=sieveeditor
Terminal=false
Categories=Development;Java;
```

Install:

```bash
sudo cp sieveeditor.desktop /usr/share/applications/
sudo update-desktop-database
```

**Pros:**

- Appears in application menu
- Standard Linux integration
- Easy to customize per-system

**Cons:**

- Requires installation
- Hard-coded paths
- Less flexible than launcher script

---

### Solution 4: Environment-Agnostic Wrapper

**Why:** Works on any system regardless of desktop environment.

Create `sieveeditor-launcher.sh`:

```bash
#!/bin/bash
# Universal launcher that detects environment

detect_scale() {
    # Check various environment variables and settings
    local scale=""

    # Try GDK_SCALE (GNOME)
    if [ -n "$GDK_SCALE" ]; then
        scale=$GDK_SCALE
        echo "Detected from GDK_SCALE: $scale" >&2
        echo $scale
        return
    fi

    # Try gsettings (GNOME)
    if command -v gsettings &> /dev/null; then
        scale=$(gsettings get org.gnome.desktop.interface scaling-factor 2>/dev/null | cut -d' ' -f2)
        if [ -n "$scale" ] && [ "$scale" != "0" ]; then
            echo "Detected from GNOME: $scale" >&2
            echo $scale
            return
        fi
    fi

    # Try kreadconfig5 (KDE)
    if command -v kreadconfig5 &> /dev/null; then
        scale=$(kreadconfig5 --file kdeglobals --group KScreen --key ScaleFactor)
        if [ -n "$scale" ] && [ "$scale" != "1" ]; then
            echo "Detected from KDE: $scale" >&2
            echo $scale
            return
        fi
    fi

    # Try to detect from screen resolution
    if command -v xrandr &> /dev/null; then
        resolution=$(xrandr | grep -oP '\d+x\d+' | head -1 | cut -dx -f1)
        if [ "$resolution" -ge 3840 ]; then
            scale=2.0
            echo "Detected 4K resolution, using scale: $scale" >&2
            echo $scale
            return
        elif [ "$resolution" -ge 2560 ]; then
            scale=1.5
            echo "Detected QHD resolution, using scale: $scale" >&2
            echo $scale
            return
        fi
    fi

    # Default to 1.0 (no scaling)
    echo "No scaling detected, using default: 1.0" >&2
    echo "1.0"
}

SCALE=$(detect_scale)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JARFILE="$SCRIPT_DIR/SieveEditor-jar-with-dependencies.jar"

exec java \
    -Dsun.java2d.uiScale.enabled=true \
    -Dsun.java2d.uiScale=$SCALE \
    -Dawt.useSystemAAFontSettings=lcd \
    -Dswing.aatext=true \
    -jar "$JARFILE" "$@"
```

**Pros:**

- Works on GNOME, KDE, XFCE, etc.
- Auto-detects scaling
- Fallback to resolution-based detection

**Cons:**

- More complex
- May not work on all DEs

---

## Recommendation

**Use Solution 1 (Simple Launcher Script) for immediate fix.**

**Implement Solution 2 (Code Changes) in next release** so users don't need the script.

### Implementation Steps

1. **Create launcher script** (10 minutes)

   ```bash
   cd /path/to/SieveEditor
   # Create sieveeditor.sh as shown in Solution 1
   chmod +x sieveeditor.sh
   ```

2. **Test on user's 4K system** (5 minutes)

   ```bash
   ./sieveeditor.sh
   # Verify UI is properly sized
   ```

3. **Add to repository** (5 minutes)

   ```bash
   git add sieveeditor.sh
   git commit -m "Add HiDPI launcher script for 4K displays"
   ```

4. **Update README** (10 minutes)
   Add section about running on 4K/HiDPI displays with the launcher script
   `./sieveeditor.sh` or manually with Java scaling properties.

5. **Plan code integration** (for next release)
   Add to backlog: Integrate Solution 2 into Application.java

---

## Testing Results Template

Use this to document findings:

```markdown
## Testing Results

**System Information:**
- OS: [e.g., Ubuntu 24.04]
- Desktop: [e.g., GNOME 46]
- Display: [e.g., 3840x2160 (4K)]
- Java: [output of `java -version`]
- Session Type: [echo $XDG_SESSION_TYPE]

**Environment Variables:**
- GDK_SCALE: [echo $GDK_SCALE]
- GNOME scaling-factor: [gsettings get...]

**Test Results:**

| Test | Command | Result | Notes |
|------|---------|--------|-------|
| Baseline | `java -jar ...` | ❌ | UI tiny |
| Enable HiDPI | `java -Dsun.java2d.uiScale.enabled=true -jar ...` | ⚠️ | Better but still small |
| Scale 2.0 | `java -Dsun.java2d.uiScale=2.0 ...` | ✅ | Perfect! |
| Launcher script | `./sieveeditor.sh` | ✅ | Works great |

**Conclusion:**
- Problem: [describe root cause]
- Solution: [which solution worked]
- Recommendation: [what to implement]
   ```

---

## Additional Resources

### Useful Commands for Debugging

```bash
# Check all Java properties related to scaling
java -XshowSettings:properties -version 2>&1 | grep -i scale

# Test with different Look and Feels
java -Dswing.defaultlaf=javax.swing.plaf.nimbus.NimbusLookAndFeel -jar ...

# Verbose font loading
java -Dsun.java2d.trace=log -jar ...

# Check if Java detects HiDPI
java -Dsun.java2d.uiScale.enabled=true -jar ... 2>&1 | grep -i scale
```

### Related Issues

- [JDK-8172854: HiDPI scaling issues on Linux](https://bugs.openjdk.org/browse/JDK-8172854)
- [GNOME fractional scaling documentation](https://wiki.gnome.org/HowDoI/HiDpi)
- [Java HiDPI on Linux documentation](https://wiki.archlinux.org/title/HiDPI#Java_applications)

---

**Last Updated:** 2025-11-03
**Status:** Investigation complete, solutions provided
**Next Step:** Test Solution 1 on user's 4K system
