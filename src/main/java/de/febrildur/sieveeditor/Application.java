package de.febrildur.sieveeditor;

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
import org.fife.ui.rtextarea.RTextScrollPane;

import com.fluffypeople.managesieve.ParseException;
import com.fluffypeople.managesieve.SieveScript;

import de.febrildur.sieveeditor.actions.ActionActivateDeactivateScript;
import de.febrildur.sieveeditor.actions.ActionCheckScript;
import de.febrildur.sieveeditor.actions.ActionConnect;
import de.febrildur.sieveeditor.actions.ActionLoadScript;
import de.febrildur.sieveeditor.actions.ActionOpenLocalScript;
import de.febrildur.sieveeditor.actions.ActionReplace;
import de.febrildur.sieveeditor.actions.ActionSaveLocalScript;
import de.febrildur.sieveeditor.actions.ActionSaveScript;
import de.febrildur.sieveeditor.actions.ActionSaveScriptAs;
import de.febrildur.sieveeditor.actions.InsertMenuBuilder;
import de.febrildur.sieveeditor.system.ConnectAndListScripts;
import de.febrildur.sieveeditor.system.PropertiesSieve;
import de.febrildur.sieveeditor.system.SieveTokenMaker;

public class Application extends JFrame {

	private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

	private ConnectAndListScripts server;
	private PropertiesSieve prop;
	private RSyntaxTextArea textArea;
	private de.febrildur.sieveeditor.ui.RuleNavigatorPanel ruleNavigator;
	private SieveScript script;

	private AbstractAction actionConnect = new ActionConnect(this);
	private AbstractAction actionDisconnect = new AbstractAction("Disconnect") {
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
				setTitle("Sieve Editor");
				updateStatus();
			}
		}
	};
	private AbstractAction actionActivateDeactivateScript = new ActionActivateDeactivateScript(this);
	private AbstractAction actionLoadScript = new ActionLoadScript(this);
	private AbstractAction actionCheckScript = new ActionCheckScript(this);
	private AbstractAction actionSaveScript = new ActionSaveScript(this);
	private AbstractAction actionSaveScriptAs = new ActionSaveScriptAs(this);
	private AbstractAction actionReplace = new ActionReplace(this);
	private AbstractAction actionOpenLocal = new ActionOpenLocalScript(this);
	private AbstractAction actionSaveLocal = new ActionSaveLocalScript(this);
	private AbstractAction actionQuit = new AbstractAction("Quit") {
		@Override
		public void actionPerformed(java.awt.event.ActionEvent e) {
			if (server != null) {
				try {
					server.logout();
				} catch (IOException | ParseException ex) {
					// Ignore logout errors
				}
			}
			System.exit(0);
		}
	};

	public Application() {
		this(null);
	}

	public Application(String forcedBackend) {

		// Run migration once
		PropertiesSieve.migrateOldProperties();

		// Load last used profile
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

		file.add(new JMenuItem(actionOpenLocal)).setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
		file.add(new JMenuItem(actionSaveLocal)).setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));
		file.addSeparator();
		file.add(new JMenuItem(actionQuit)).setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_DOWN_MASK));

		// Sieve menu - server operations
		JMenu sieve = new JMenu("Sieve");
		menu.add(sieve);

		sieve.add(new JMenuItem(actionConnect)).setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK));
		sieve.add(new JMenuItem(actionDisconnect));
		sieve.addSeparator();
		sieve.add(new JMenuItem(actionActivateDeactivateScript));
		sieve.add(new JMenuItem(actionLoadScript)).setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
		sieve.add(new JMenuItem(actionCheckScript));
		sieve.add(new JMenuItem(actionSaveScript)).setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));
		sieve.add(new JMenuItem(actionSaveScriptAs));

		// Edit menu
		JMenu edit = new JMenu("Edit");
		menu.add(edit);

		edit.add(new JMenuItem(actionReplace)).setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK));

		// Insert menu - templates
		InsertMenuBuilder insertMenuBuilder = new InsertMenuBuilder(this);
		menu.add(insertMenuBuilder.createInsertMenu());

		setJMenuBar(menu);

		JPanel cp = new JPanel(new BorderLayout());

		AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
		atmf.putMapping("text/sieve", SieveTokenMaker.class.getCanonicalName());

		textArea = new RSyntaxTextArea(20, 60);
		textArea.setSyntaxEditingStyle("text/sieve");
		textArea.setCodeFoldingEnabled(true);

		// Set a properly scaled monospace font for the editor
		// Base size 13pt scales with FlatLaf's UIScale for HiDPI displays
		int scaledFontSize = UIScale.scale(13);
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, scaledFontSize));

		RTextScrollPane sp = new RTextScrollPane(textArea);

		// Create rule navigator panel
		ruleNavigator = new de.febrildur.sieveeditor.ui.RuleNavigatorPanel();
		ruleNavigator.setJumpToLineCallback(this::jumpToLine);

		// Create split pane with navigator on left, editor on right
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, ruleNavigator, sp);
		splitPane.setDividerLocation(250); // 250px for navigator
		splitPane.setResizeWeight(0.0); // Give all extra space to editor

		cp.add(splitPane);

		setContentPane(cp);
		setTitle("Sieve Editor");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		// Set a reasonable minimum window size
		setMinimumSize(new java.awt.Dimension(UIScale.scale(600), UIScale.scale(400)));
		setLocationRelativeTo(null);
		updateStatus();
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

		// Parse command-line arguments
		boolean verbose = false;
		String forcedBackend = null;

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("-v") || arg.equals("--verbose")) {
				verbose = true;
			} else if (arg.equals("--backend") && i + 1 < args.length) {
				forcedBackend = args[++i];
			} else if (arg.equals("-h") || arg.equals("--help")) {
				printHelp();
				System.exit(0);
			}
		}

		// Configure logging level
		if (verbose) {
			enableVerboseLogging();
			LOGGER.log(Level.INFO, "Verbose logging enabled");
		}

		// Set global forced backend BEFORE creating any PropertiesSieve instances
		// This ensures ALL instances throughout the app respect the command-line choice
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
		System.out.println("  -v, --verbose           Enable verbose logging");
		System.out.println("  --backend <type>        Force specific credential backend");
		System.out.println("                          Types: keepassxc, keychain, prompt");
		System.out.println("  -h, --help              Show this help message");
		System.out.println();
		System.out.println("Examples:");
		System.out.println("  java -jar SieveEditor.jar -v");
		System.out.println("  java -jar SieveEditor.jar --backend keychain");
		System.out.println("  java -jar SieveEditor.jar -v --backend prompt");
	}

	private static void enableVerboseLogging() {
		// Set root logger to INFO level
		Logger rootLogger = Logger.getLogger("");
		rootLogger.setLevel(Level.ALL);

		// Configure console handler
		for (var handler : rootLogger.getHandlers()) {
			if (handler instanceof ConsoleHandler) {
				handler.setLevel(Level.ALL);
			}
		}

		// Set our package loggers to FINE level for detailed output
		Logger.getLogger("de.febrildur.sieveeditor").setLevel(Level.FINE);
		Logger.getLogger("de.febrildur.sieveeditor.system.credentials").setLevel(Level.FINE);
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
	 * @param filename The filename for display in title bar
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

	public Object getScriptName() {
		return script.getName();
	}

	public void updateStatus() {
		actionConnect.setEnabled(true);
		actionDisconnect.setEnabled(server != null);
		actionActivateDeactivateScript.setEnabled(server != null);
		actionLoadScript.setEnabled(server != null);
		actionCheckScript.setEnabled(server != null);
		actionSaveScript.setEnabled(server != null && script != null);
		actionSaveScriptAs.setEnabled(server != null);
		actionQuit.setEnabled(true);
	}

	/**
	 * Jumps to a specific line in the script editor and highlights it.
	 *
	 * @param lineNumber the 1-based line number to jump to
	 */
	public void jumpToLine(int lineNumber) {
		if (textArea == null || lineNumber < 1) {
			return;
		}

		try {
			// Convert 1-based line number to 0-based for RSyntaxTextArea
			int zeroBasedLine = lineNumber - 1;

			// Get the offset of the line start
			int lineStartOffset = textArea.getLineStartOffset(zeroBasedLine);
			int lineEndOffset = textArea.getLineEndOffset(zeroBasedLine);

			// Move caret to the line
			textArea.setCaretPosition(lineStartOffset);

			// Select the entire line to highlight it
			textArea.setSelectionStart(lineStartOffset);
			textArea.setSelectionEnd(lineEndOffset - 1); // -1 to exclude newline

			// Ensure the line is visible (scroll to it)
			textArea.requestFocusInWindow();
		} catch (javax.swing.text.BadLocationException e) {
			// Line number is out of range - ignore silently
			// This can happen if error message refers to a line that doesn't exist
		}
	}

	/**
	 * Updates the rule navigator with the current script content.
	 */
	public void updateRuleNavigator() {
		if (ruleNavigator != null) {
			ruleNavigator.updateRules(getScriptText());
		}
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
