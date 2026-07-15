// aislop-ignore-file complexity/function-too-long -- Swing-Konstruktor mit vielen GUI-Initialisierungen
package de.febrildur.sieveeditor;
// SPDX-FileCopyrightText: 2019, 2020, 2024 Zwixx
// SPDX-FileCopyrightText: 2025 Claude
// SPDX-FileCopyrightText: 2025 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.InaccessibleObjectException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.UIScale;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.folding.CurlyFoldParser;
import org.fife.ui.rsyntaxtextarea.folding.FoldParserManager;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.fluffypeople.managesieve.ParseException;
import com.fluffypeople.managesieve.SieveScript;

import de.febrildur.sieveeditor.actions.ActionActivateDeactivateScript;
import de.febrildur.sieveeditor.actions.ActionCheckScript;
import de.febrildur.sieveeditor.actions.ActionConnect;
import de.febrildur.sieveeditor.actions.ActionOpenLocalScript;
import de.febrildur.sieveeditor.actions.ActionReplace;
import de.febrildur.sieveeditor.actions.ActionSaveLocalScript;
import de.febrildur.sieveeditor.actions.ActionSaveScript;
import de.febrildur.sieveeditor.actions.ActionSaveScriptAs;
import de.febrildur.sieveeditor.actions.ActionZoom;
import de.febrildur.sieveeditor.actions.ActionZoom.ZoomOperation;
import de.febrildur.sieveeditor.actions.InsertMenuBuilder;
import de.febrildur.sieveeditor.parser.SieveParser;
import de.febrildur.sieveeditor.system.ConnectAndListScripts;
import de.febrildur.sieveeditor.system.PropertiesSieve;
import de.febrildur.sieveeditor.system.SieveTokenMaker;

public class Application extends JFrame {

	private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

	private ConnectAndListScripts server;
	private PropertiesSieve prop;
	private RSyntaxTextArea textArea;
	private de.febrildur.sieveeditor.ui.RuleNavigatorPanel ruleNavigator;
	private javax.swing.Timer parserDebounceTimer;
	private de.febrildur.sieveeditor.ui.SearchPanel searchPanel;
	private JSplitPane mainSplitPane; // Horizontal scrollPanelit between editor and navigator
	private boolean userHasManuallyResizedDivider = false; // Track if user manually resized
	private boolean isAdjustingDividerProgrammatically = false; // Flag to prevent false positives
	private SieveScript script;
	private RTextScrollPane scrollPane;

	private AbstractAction actionConnect = new ActionConnect(this);
	private AbstractAction actionDisconnect = new AbstractAction("Disconnect") {
		{
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK));
		}
		@Override
		public void actionPerformed(java.awt.event.ActionEvent e) {
			if (server != null) {
				try {
					server.logout();
				} catch (IOException | ParseException ex) {
					// Ignore logout errors
				}
				server = null;
				script = null;
				textArea.setText("");
				ruleNavigator.clear(); // Clear the navigator panel
				setTitle("Sieve Editor");
				updateStatus();
			}
		}
	};
	private AbstractAction actionActivateDeactivateScript = new ActionActivateDeactivateScript(this);

	private AbstractAction actionCheckScript = new ActionCheckScript(this);
	private AbstractAction actionSaveScript = new ActionSaveScript(this);
	private AbstractAction actionSaveScriptAs = new ActionSaveScriptAs(this);
	private AbstractAction actionReplace = new ActionReplace(this);
	private AbstractAction actionOpenLocal = new ActionOpenLocalScript(this);
	private AbstractAction actionSaveLocal = new ActionSaveLocalScript(this);
	private AbstractAction actionZoomIn;
	private AbstractAction actionZoomOut;
	private AbstractAction actionZoomReset;

		private AbstractAction actionQuit = new AbstractAction("Quit") {
		{
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));
		}
		@Override
		public void actionPerformed(java.awt.event.ActionEvent e) {
			if (server != null) {
				try {
					server.logout();
				} catch (IOException | ParseException ex) {
					// Ignore logout errors
				}
			}
			de.febrildur.sieveeditor.system.jbr.JBRSystemUtils.tryCompactMemory();
			dispose();
			System.exit(0);
		}
	};

	public Application() {
		this(null);
	}

	public Application(String forcedBackend) {

		PropertiesSieve.migrateOldProperties();
		String lastProfile = PropertiesSieve.getLastUsedProfile();
		prop = new PropertiesSieve(lastProfile, forcedBackend);

		try {
			prop.load();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getClass().getName() + ": " + e.getMessage());
			return;
		}

		JMenuBar menu = new JMenuBar();

		// File menu - local file operations
		JMenu file = new JMenu("File");
		menu.add(file);

		file.add(new JMenuItem(actionOpenLocal));
		file.add(new JMenuItem(actionSaveLocal));
		file.addSeparator();
		file.add(new JMenuItem(actionQuit));

		// Sieve menu - server operations
		JMenu sieve = new JMenu("Sieve");
		menu.add(sieve);

		sieve.add(new JMenuItem(actionConnect));
		sieve.add(new JMenuItem(actionDisconnect));
		sieve.addSeparator();
		sieve.add(new JMenuItem(actionActivateDeactivateScript));
		sieve.add(new JMenuItem(actionCheckScript));
		sieve.add(new JMenuItem(actionSaveScript));
		sieve.add(new JMenuItem(actionSaveScriptAs));

		// Edit menu
		JMenu edit = new JMenu("Edit");
		menu.add(edit);

		edit.add(new JMenuItem(actionReplace));

		// Insert menu - templates
		InsertMenuBuilder insertMenuBuilder = new InsertMenuBuilder(this);
		menu.add(insertMenuBuilder.createInsertMenu());

		// Help menu
		JMenu help = new JMenu("Help");
		menu.add(help);

		JMenuItem aboutItem = new JMenuItem("About SieveEditor...");
		aboutItem.addActionListener(e -> de.febrildur.sieveeditor.ui.AboutDialog.showAboutDialog(this));
		help.add(aboutItem);

		setJMenuBar(menu);

		JPanel cp = new JPanel(new BorderLayout());

		AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
		atmf.putMapping("text/sieve", SieveTokenMaker.class.getCanonicalName());
		FoldParserManager.get().addFoldParserMapping("text/sieve", new CurlyFoldParser());

		textArea = new RSyntaxTextArea(20, 60);
		textArea.setSyntaxEditingStyle("text/sieve");
		textArea.setCodeFoldingEnabled(true);

		// Editor typing aids (require getCurlyBracesDenoteCodeBlocks() → true in SieveTokenMaker)
		textArea.setBracketMatchingEnabled(true);
		textArea.setCloseCurlyBraces(true);
		textArea.setInsertPairedCharacters(true);

		// Highlight all occurrences of a selected identifier
		textArea.setMarkOccurrences(true);

		// Syntax validation parser (brace/paren/bracket balance, string quotes)
		textArea.addParser(new SieveParser());

		// Set a properly scaled monospaced font for the editor.
		// Base size 13pt (or saved preference) scales with FlatLaf's UIScale for HiDPI.
		// Read saved font size from profile; default is 13
		int logicalFontSize = prop.getFontSize();
		int scaledFontSize = UIScale.scale(logicalFontSize);

		// Prefer JBR's bundled JetBrains Mono (discovered from java.home/lib/fonts/),
		// fall back to standard MONOSPACED when running on non-JBR JDK
		Font jbrMono = de.febrildur.sieveeditor.system.jbr.JBRFontSupport.getJbrMonoFont();
		Font baseFont;
		if (jbrMono != null) {
			baseFont = jbrMono.deriveFont(Font.PLAIN, scaledFontSize);
		} else {
			baseFont = new Font(Font.MONOSPACED, Font.PLAIN, scaledFontSize);
		}

		// Apply JBR font features (ligatures, calt, zero) — always on with JetBrains Mono
		textArea.setFont(de.febrildur.sieveeditor.system.jbr.JBRFontSupport.deriveEditorFont(baseFont));

		scrollPane = new RTextScrollPane(textArea);

		// Configure line number gutter font size (+2pt relative to editor font)
		int gutterFontSize = UIScale.scale(logicalFontSize + 2);
		if (jbrMono != null) {
			scrollPane.getGutter().setLineNumberFont(jbrMono.deriveFont(Font.PLAIN, gutterFontSize));
		} else {
			scrollPane.getGutter().setLineNumberFont(new Font(Font.MONOSPACED, Font.PLAIN, gutterFontSize));
		}
		// Enable icon row header for parser error markers in the gutter
		scrollPane.getGutter().setIconRowHeaderEnabled(true);
		// Use modern triangle-style fold indicators instead of +/- (classic)
		scrollPane.getGutter().setFoldIndicatorStyle(
			org.fife.ui.rtextarea.FoldIndicatorStyle.MODERN);

		// Register global keyboard shortcuts using WHEN_IN_FOCUSED_WINDOW scope
		// This ensures keystrokes work even when focus is in the text editor
		registerGlobalKeystroke(actionOpenLocal);
		registerGlobalKeystroke(actionSaveLocal);
		registerGlobalKeystroke(actionQuit);
		registerGlobalKeystroke(actionConnect);
		registerGlobalKeystroke(actionDisconnect);
		registerGlobalKeystroke(actionActivateDeactivateScript);
		registerGlobalKeystroke(actionCheckScript);
		registerGlobalKeystroke(actionSaveScript);
		registerGlobalKeystroke(actionSaveScriptAs);
		registerGlobalKeystroke(actionReplace);

		// Zoom actions with Ctrl++/Ctrl+-/Ctrl+0
		actionZoomIn = new ActionZoom(this, scrollPane, ZoomOperation.IN, "Zoom In",
				KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK));
		actionZoomOut = new ActionZoom(this, scrollPane, ZoomOperation.OUT, "Zoom Out",
				KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK));
		actionZoomReset = new ActionZoom(this, scrollPane, ZoomOperation.RESET, "Reset Zoom",
				KeyStroke.getKeyStroke(KeyEvent.VK_0, KeyEvent.CTRL_DOWN_MASK));

		registerGlobalKeystroke(actionZoomIn);
		registerGlobalKeystroke(actionZoomOut);
		registerGlobalKeystroke(actionZoomReset);

		// Also bind numpad + and - for zoom in/out
		textArea.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK), "zoomInNumpad");
		textArea.getActionMap().put("zoomInNumpad", actionZoomIn);
		textArea.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
			.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, KeyEvent.CTRL_DOWN_MASK), "zoomOutNumpad");
		textArea.getActionMap().put("zoomOutNumpad", actionZoomOut);

		// View menu - zoom controls (must be after zoom actions are initialized)
		JMenu view = new JMenu("View");
		view.add(new JMenuItem(actionZoomIn));
		view.add(new JMenuItem(actionZoomOut));
		view.addSeparator();
		view.add(new JMenuItem(actionZoomReset));
		menu.add(view, 3); // Insert after Edit (index 3: File=0, Sieve=1, Edit=2, View=3)

		// Create search panel (docked above navigator)
		searchPanel = new de.febrildur.sieveeditor.ui.SearchPanel();
		searchPanel.setTargetEditor(textArea);

		ruleNavigator = new de.febrildur.sieveeditor.ui.RuleNavigatorPanel();
		ruleNavigator.setJumpToLineCallback(this::jumpToLine);

		// Setup auto-regeneration of navigator on text changes (debounced)
		setupNavigatorAutoUpdate();

		// Create vertical scrollPanelit for right side: search panel on top, navigator below
		JSplitPane rightSidePane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchPanel, ruleNavigator);
		rightSidePane.setResizeWeight(0.0); // Search panel gets fixed size, navigator gets extra scrollPaneace
		rightSidePane.setDividerLocation(200); // Search+replace panel height

		// Create main horizontal scrollPanelit: editor on left, right pane on right
		mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, rightSidePane);
		mainSplitPane.setResizeWeight(1.0); // Give all extra scrollPaneace to editor
		mainSplitPane.setDividerLocation(-200); // 200px for right side pane (negative = from right)

		// Track manual divider resizing by user
		mainSplitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
			// Only mark as manually resized if this is a user action (not programmatic)
			if (!isAdjustingDividerProgrammatically && mainSplitPane.isVisible()) {
				// User manually moved the divider
				userHasManuallyResizedDivider = true;
			}
		});

		cp.add(mainSplitPane);

		setContentPane(cp);
		setTitle("Sieve Editor");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowClosed(java.awt.event.WindowEvent e) {
				System.exit(0);
			}
		});
		pack();

		// Apply JBR window enhancements (graceful fallback on non-JBR runtimes)
		de.febrildur.sieveeditor.system.jbr.JBRWindowDecorations.applyCustomTitleBar(this, 0f);
		de.febrildur.sieveeditor.system.jbr.JBRRoundedCorners.apply(this);

		// Install autocomplete overlay (JLayeredPane-based, works on all platforms)
		LOGGER.fine("Installing autocomplete overlay on textArea: " + textArea.getClass().getName());
		new de.febrildur.sieveeditor.system.LayeredCompletionOverlay(
			textArea,
			new de.febrildur.sieveeditor.system.SieveCompletionProvider(),
			this);
		LOGGER.fine("LayeredCompletionOverlay installed");

		// Set a reasonable minimum window size
		setMinimumSize(new java.awt.Dimension(UIScale.scale(600), UIScale.scale(400)));
		setLocationRelativeTo(null);

		// Add window resize listener to re-adjust warning panel sizing and navigator width
		addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent e) {
				// Re-run warning panel auto-sizing on window resize
				if (ruleNavigator != null) {
					ruleNavigator.reapplyWarningPanelSize();

					if (!userHasManuallyResizedDivider && ruleNavigator.isWidthAutoSized()
							&& mainSplitPane != null && mainSplitPane.getWidth() > 0) {
						SwingUtilities.invokeLater(() -> {
							int recommendedWidth = ruleNavigator.getRecommendedWidth();
							isAdjustingDividerProgrammatically = true;
							try {
								mainSplitPane.setDividerLocation(
									mainSplitPane.getWidth() - recommendedWidth - mainSplitPane.getDividerSize()
								);
							} finally {
								isAdjustingDividerProgrammatically = false;
							}
						});
					}
				}
			}
		});

		updateStatus();
	}

	/**
	 * Installs ErrorLineNumberList to show red line numbers for parser error lines.
	 * Uses reflection to inject into Gutter's private lineNumberList field.
	 */
	/**
	 * Registers a global keyboard shortcut for the given action.
	 * Uses WHEN_IN_FOCUSED_WINDOW scope to ensure keystrokes work even when
	 * focus is in the text editor (RSyntaxTextArea).
	 *
	 * @param action the action to register (must have ACCELERATOR_KEY set)
	 */
	private void registerGlobalKeystroke(AbstractAction action) {
		KeyStroke keyStroke = (KeyStroke) action.getValue(AbstractAction.ACCELERATOR_KEY);
		if (keyStroke != null) {
			String actionKey = action.getValue(AbstractAction.NAME) + "_global";
			textArea.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW)
				.put(keyStroke, actionKey);
			textArea.getActionMap().put(actionKey, action);
		}
	}

	public PropertiesSieve getProp() {
		return prop;
	}

	public void setProp(PropertiesSieve prop) {
		this.prop = prop;
	}

	public static void main(String[] args) {
		// Set application name for Linux desktop integration (GNOME dock, etc.)
		// This must be done before any AWT/Swing components are created
		setLinuxAppName();

		// Initialize FlatLaf look-and-feel with automatic HiDPI scaling
		// This must be called before creating any Swing components
		FlatLightLaf.setup();

		// Suppress JBR Wayland input method log spam that appears when
		// AutoCompletion popup windows are shown/hidden on Wayland.
		Logger.getLogger("sun.awt.wl.im.text_input_unstable_v3").setLevel(Level.OFF);

		boolean verbose = false;
		String forcedBackend = null;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.matches("-v+") || arg.equals("--verbose")) {
				verbose = true;
			} else if (arg.equals("--backend") && i + 1 < args.length) {
				forcedBackend = args[++i];
			} else if (arg.equals("-h") || arg.equals("--help")) {
				printHelp();
				System.exit(0);
			}
		}

		// Count verbosity level: -v=1, -vv=2, -vvv=3
		int verbosity = 0;
		if (verbose) {
			for (String arg : args) {
				if (arg.equals("--verbose")) {
					verbosity = 1;
				} else if (arg.matches("-v+")) {
					// Each v adds one level
					int count = 0;
					for (int j = 1; j < arg.length() && arg.charAt(j) == 'v'; j++) {
						count++;
					}
					verbosity = Math.max(verbosity, count);
				}
			}
		}

		if (verbose) {
			enableVerboseLogging(verbosity);
			LOGGER.log(Level.INFO, "Verbose logging enabled (level {0})", verbosity);
		}

		// Log which autocomplete library version is loaded (confirms patched build)
		try {
			java.net.URL acUrl = org.fife.ui.autocomplete.AutoCompletion.class
				.getProtectionDomain().getCodeSource().getLocation();
			LOGGER.log(Level.INFO, "AutoCompletion library: {0}", acUrl);
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Could not determine AutoCompletion library source", e);
		}

		// Log JBR API availability (graceful fallback if not on full JBR)
		de.febrildur.sieveeditor.system.jbr.JBRSupport.logStatus();

		// Set global forced backend BEFORE creating any PropertiesSieve instances
		// This ensures ALL instances throughout the app rescrollPaneect the command-line choice
		if (forcedBackend != null) {
			de.febrildur.sieveeditor.system.credentials.MasterKeyProviderFactory.setGlobalForcedBackend(forcedBackend);
			LOGGER.log(Level.INFO, "Global forced backend set to: {0}", forcedBackend);
		}

		// Launch application
		final String backend = forcedBackend;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Application(backend).setVisible(true);
			}
		});
	}

	private static void printHelp() {
		System.out.println("SieveEditor - ManageSieve script editor");
		System.out.println();
		System.out.println("Usage: java -jar SieveEditor.jar [options]");
		System.out.println();
		System.out.println("Options:");
		System.out.println("  -v, -vv, -vvv        Enable verbose logging (more vs = more detail)");
		System.out.println("  --verbose            Alias for -v");
		System.out.println("  --backend <type>     Force credential backend");
		System.out.println("                          Types: keepassxc, keychain, prompt");
		System.out.println("  -h, --help              Show this help message");
		System.out.println();
		System.out.println("Examples:");
		System.out.println("  java -jar SieveEditor.jar -v");
		System.out.println("  java -jar SieveEditor.jar --backend keychain");
		System.out.println("  java -jar SieveEditor.jar -v --backend prompt");
	}

	private static void enableVerboseLogging(int level) {
		Level ourLevel;
		if (level >= 3) {
			ourLevel = Level.FINEST;
		} else if (level >= 2) {
			ourLevel = Level.FINER;
		} else {
			ourLevel = Level.FINE;
		}

		// Only set ConsoleHandler level (don't touch root logger — avoids AWT internals spam)
		for (var handler : Logger.getLogger("").getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				handler.setLevel(ourLevel);
			}
		}

		// Set our package loggers to the requested level
		Logger.getLogger("de.febrildur.sieveeditor").setLevel(ourLevel);
		Logger.getLogger("de.febrildur.sieveeditor.system").setLevel(ourLevel);
	}

	public ConnectAndListScripts getServer() {
		return server;
	}

	public void setServer(ConnectAndListScripts server) {
		this.server = server;
	}

	public void setScript(SieveScript script) throws IOException, ParseException {
		this.script = script;
		textArea.setText(server.getScript(script));
		updateRuleNavigator();
	}

	/**
	 * Loads a local script file for editing (offline mode).
	 *
	 * This puts the application in "local mode" where:
	 * - script is null (not connected to server)
	 * - Window title shows local filename
	 * - Server operations are disabled
	 *
	 * @param content  The script content to load
	 * @param filename The filename for discrollPanelay in title bar
	 */
	public void loadLocalScript(String content, String filename) {
		textArea.setText(content);
		script = null;
		setTitle("Sieve Editor - " + filename + " (Local)");
		updateStatus();
		updateRuleNavigator();
	}

	public void save() {
		save(script.getName());
	}

	public void save(String name) {
		try {
			server.putScript(name, textArea.getText());
		} catch (IOException | ParseException e) {
			JOptionPane.showMessageDialog(this, e.getClass().getName() + ": " + e.getMessage());
		}
	}

	public String getScriptText() {
		return textArea.getText();
	}

	public RSyntaxTextArea getScriptArea() {
		return textArea;
	}

	public de.febrildur.sieveeditor.ui.SearchPanel getSearchPanel() {
		return searchPanel;
	}

	public Object getScriptName() {
		return script.getName();
	}

	public void updateStatus() {
		actionConnect.setEnabled(true);
		actionDisconnect.setEnabled(server != null);
		actionActivateDeactivateScript.setEnabled(server != null);

		actionCheckScript.setEnabled(server != null);
		actionSaveScript.setEnabled(server != null && script != null);
		actionSaveScriptAs.setEnabled(server != null);
		actionQuit.setEnabled(true);
	}

	public void jumpToLine(int lineNumber) {
		if (textArea == null || lineNumber < 1) {
			return;
		}

		try {
			int zeroBasedLine = lineNumber - 1;
			int lineStartOffset = textArea.getLineStartOffset(zeroBasedLine);
			int lineEndOffset = textArea.getLineEndOffset(zeroBasedLine);
			textArea.setCaretPosition(lineStartOffset);
			textArea.setSelectionStart(lineStartOffset);
			textArea.setSelectionEnd(lineEndOffset - 1);
			java.awt.Rectangle lineRect = textArea.modelToView(lineStartOffset);
			if (lineRect != null) {
				java.awt.Rectangle visibleRect = textArea.getVisibleRect();
				lineRect.height = visibleRect.height;
				textArea.scrollRectToVisible(lineRect);
			}
			textArea.requestFocusInWindow();
		} catch (javax.swing.text.BadLocationException e) {
		}
	}

	public void updateRuleNavigator() {
		if (ruleNavigator != null) {
			ruleNavigator.updateRules(getScriptText());

			if (!ruleNavigator.isWidthAutoSized()) {
			SwingUtilities.invokeLater(() -> {
					if (mainSplitPane != null && mainSplitPane.getWidth() > 0) {
						int recommendedWidth = ruleNavigator.getRecommendedWidth();
						isAdjustingDividerProgrammatically = true;
						try {
							mainSplitPane.setDividerLocation(mainSplitPane.getWidth() - recommendedWidth - mainSplitPane.getDividerSize());
							ruleNavigator.markWidthAutoSized();
						} finally {
							isAdjustingDividerProgrammatically = false;
						}
					}
				});
			}
		}
	}


	private void setupNavigatorAutoUpdate() {
		parserDebounceTimer = new javax.swing.Timer(500, e -> {
			updateRuleNavigator();
		});
		parserDebounceTimer.setRepeats(false); // Only fire once after delay

		// Add document listener to textArea
		textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			@Override
			public void insertUpdate(javax.swing.event.DocumentEvent e) {
				scheduleNavigatorUpdate();
			}

			@Override
			public void removeUpdate(javax.swing.event.DocumentEvent e) {
				scheduleNavigatorUpdate();
			}

			@Override
			public void changedUpdate(javax.swing.event.DocumentEvent e) {
				scheduleNavigatorUpdate();
			}

			private void scheduleNavigatorUpdate() {
				// Restart the timer on each change (debounce)
				if (parserDebounceTimer.isRunning()) {
					parserDebounceTimer.restart();
				} else {
					parserDebounceTimer.start();
				}
			}
		});
	}

	/**
	 * Sets the application name for Linux desktop integration.
	 *
	 * On X11, this sets the WM_CLASS property so that GNOME Shell, Unity, and other
	 * desktop environments can properly associate the application window with its
	 * .desktop launcher file for dock/taskbar integration.
	 *
	 * The name must match the StartupWMClass value in the .desktop file.
	 */
	private static void setLinuxAppName() {
		// Only needed on Linux with X11
		String os = System.getProperty("os.name", "").toLowerCase();
		if (!os.contains("linux")) {
			return;
		}

		try {
			java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
			Class<?> toolkitClass = toolkit.getClass();

			// Check if this is the X11 toolkit (sun.awt.X11.XToolkit)
			if (toolkitClass.getName().equals("sun.awt.X11.XToolkit")) {
				java.lang.reflect.Field awtAppClassNameField =
					toolkitClass.getDeclaredField("awtAppClassName");
				awtAppClassNameField.setAccessible(true);
				// Must match StartupWMClass in .desktop file
				awtAppClassNameField.set(toolkit, "de.febrildur.sieveeditor.Application");
				LOGGER.log(Level.FINE, "Set X11 WM_CLASS to de.febrildur.sieveeditor.Application");
			}
		} catch (NoSuchFieldException | IllegalAccessException | SecurityException
				| InaccessibleObjectException e) {
			// Not critical - app will still work, just may not integrate with dock
			// InaccessibleObjectException occurs on Java 17+ due to module encapsulation
			LOGGER.log(Level.FINE, "Could not set X11 WM_CLASS: {0}", e.getMessage());
		}
	}
}
