# Integrated Search Panel Architecture

## Overview

The search functionality has been redesigned from a modal/modeless dialog to an **integrated, always-visible panel** docked in the right-side UI alongside the RuleNavigatorPanel.

## Architecture

### Component Structure

```text
Application (JFrame)
├── Main Horizontal JSplitPane
│   ├── Left: Editor (RTextScrollPane with RSyntaxTextArea)
│   └── Right: Vertical JSplitPane
│       ├── Top: SearchPanel (always visible)
│       └── Bottom: RuleNavigatorPanel
```

### SearchPanel Component

**File**: `src/main/java/de/febrildur/sieveeditor/ui/SearchPanel.java`

**Design Principles**:

- **Always visible**: No dialog to open/close
- **Compact layout**: Minimal vertical space usage (~140px)
- **Non-blocking**: Part of the main UI, never blocks editor
- **Self-contained**: All search logic encapsulated

**Layout**:

```text
┌─ Find ────────────────────┐
│ Text: [____________]      │
│ [Next] [Previous]         │
│ ☐ Regex  ☐ Match Case    │
└───────────────────────────┘
```

**Key Features**:

1. **Search field with Enter support**: Press Enter to find next
2. **Next/Previous buttons**: Explicit navigation controls
3. **Regex & Match Case options**: Inline checkboxes
4. **Auto-focus editor**: After successful search, focus returns to editor for immediate editing
5. **Target editor injection**: `setTargetEditor(RSyntaxTextArea)` connects to the main editor

### Integration Points

#### Application.java Changes

1. **Field addition** (line 56):

   ```java
   private de.febrildur.sieveeditor.ui.SearchPanel searchPanel;
   ```

2. **Initialization** (lines 182-184):

   ```java
   searchPanel = new de.febrildur.sieveeditor.ui.SearchPanel();
   searchPanel.setTargetEditor(textArea);
   ```

3. **Layout integration** (lines 190-198):

   ```java
   // Vertical split for right side
   JSplitPane rightSidePane = new JSplitPane(
       JSplitPane.VERTICAL_SPLIT,
       searchPanel,
       ruleNavigator
   );
   rightSidePane.setResizeWeight(0.0);  // Search fixed, navigator grows
   rightSidePane.setDividerLocation(140); // 140px for search panel

   // Main horizontal split
   JSplitPane splitPane = new JSplitPane(
       JSplitPane.HORIZONTAL_SPLIT,
       sp,
       rightSidePane
   );
   splitPane.setResizeWeight(1.0); // Editor gets extra space
   splitPane.setDividerLocation(-200); // 200px total for right side
   ```

4. **Accessor method** (lines 354-356):

   ```java
   public de.febrildur.sieveeditor.ui.SearchPanel getSearchPanel() {
       return searchPanel;
   }
   ```

#### ActionReplace.java Simplification

**Before**: 150+ lines with dialog creation, positioning, state management
**After**: 28 lines - just focuses the integrated search panel

```java
@Override
public void actionPerformed(ActionEvent e) {
    parentFrame.getSearchPanel().focusSearchField();
}
```

**Keyboard shortcut**: Ctrl+F still works, now focuses search panel instead of opening dialog

## Advantages Over Dialog Approach

### User Experience

1. **Always accessible**: No need to remember to open/close
2. **Persistent state**: Search text and options remain visible
3. **No window management**: No positioning, z-order, or focus issues
4. **Faster workflow**: One less UI element to manage
5. **IDE-like feel**: Matches modern editor conventions (VS Code, IntelliJ)

### Code Quality

1. **Simpler**: 80% less code in ActionReplace
2. **More maintainable**: Search logic centralized in SearchPanel
3. **Reusable**: SearchPanel is a standalone component
4. **No singleton complexity**: No dialog lifecycle management
5. **Clear separation**: UI component vs action handler

### Technical

1. **No threading issues**: Panel is part of EDT from the start
2. **No focus battles**: Panel doesn't compete with main window
3. **Native layout management**: JSplitPane handles resizing
4. **Consistent with navigator**: Same pattern as RuleNavigatorPanel

## Implemented Features

### Replace Functionality ✅

- **Replace field**: "With:" text field for replacement text
- **Replace button**: Replaces current match and finds next
- **Replace All button**: Replaces all occurrences in one operation
- Shows count after Replace All (e.g., "Replaced 5 occurrences")

### Keyboard Shortcuts ✅

- **F3**: Find Next (works anywhere in editor)
- **Shift+F3**: Find Previous
- **Ctrl+F**: Focus search field (existing)
- **Enter in Find field**: Find Next
- **Enter in Replace field**: Replace current match

### Tooltips ✅

All UI elements have helpful tooltips showing keyboard shortcuts and functionality:

- **Find field**: "Text to search for (Ctrl+F to focus, Enter to find next)"
- **With field**: "Replacement text (Enter to replace current match)"
- **Next button**: "Find next occurrence (F3)"
- **Previous button**: "Find previous occurrence (Shift+F3)"
- **Replace button**: "Replace current match and find next"
- **Replace All button**: "Replace all occurrences in document"

### Panel Layout (200px height)

```text
┌─ Find & Replace ──────────┐
│ Find: [____________]      │
│ With: [____________]      │
│ [Next] [Previous]         │
│ [Replace] [Replace All]   │
│ ☐ Regex  ☐ Match Case    │
└───────────────────────────┘
```

## Future Enhancements

Possible additional improvements:

1. **Search history**: Dropdown with recent searches
2. **Collapsible**: Add collapse/expand capability to save space
3. **Find in files**: Extend to search across multiple scripts
4. **Highlight all**: Option to highlight all matches
5. **Results count**: Show "Match 3 of 12"
6. **Ctrl+H shortcut**: Alternative shortcut for replace operations

## Migration Notes

If users were familiar with the dialog approach:

- Ctrl+F still works (now focuses search panel)
- All same functionality (regex, match case, forward/backward)
- Improved UX: no need to close dialog to edit
- Search field always visible for quick reference

## Testing

All existing tests pass without modification - search functionality is backward compatible at the API level.
