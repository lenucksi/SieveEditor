package de.febrildur.sieveeditor.testutil;

import de.febrildur.sieveeditor.system.credentials.MasterKeyProviderFactory;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 Extension that sets up test mode for all tests in a class.
 * This prevents GUI dialogs from appearing during tests.
 *
 * Usage:
 * <pre>
 * &#64;ExtendWith(TestModeExtension.class)
 * class MyTest {
 *     // ...
 * }
 * </pre>
 *
 * Or register globally in junit-platform.properties or via ServiceLoader.
 */
public class TestModeExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

	private static boolean started = false;

	@Override
	public void beforeAll(ExtensionContext context) {
		// Initialize test mode once for all tests
		if (!started) {
			started = true;
			MasterKeyProviderFactory.setTestMode(new TestMasterKeyProvider());

			// Register cleanup callback when JVM shuts down
			context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL)
				.put("test-mode-cleanup", this);
		}
	}

	@Override
	public void close() {
		// Cleanup when all tests are done
		MasterKeyProviderFactory.clearTestMode();
		started = false;
	}
}
