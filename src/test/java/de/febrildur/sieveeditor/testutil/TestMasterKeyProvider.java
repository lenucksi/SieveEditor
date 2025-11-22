package de.febrildur.sieveeditor.testutil;

import de.febrildur.sieveeditor.system.credentials.MasterKeyProvider;

/**
 * A non-interactive MasterKeyProvider for use in tests.
 * Returns a fixed master key without any GUI dialogs.
 */
public class TestMasterKeyProvider implements MasterKeyProvider {

	private static final String DEFAULT_TEST_KEY = "test-master-key-for-unit-tests";
	private String masterKey;

	public TestMasterKeyProvider() {
		this(DEFAULT_TEST_KEY);
	}

	public TestMasterKeyProvider(String masterKey) {
		this.masterKey = masterKey;
	}

	@Override
	public String getName() {
		return "Test Provider";
	}

	@Override
	public String getDescription() {
		return "Non-interactive provider for unit tests";
	}

	@Override
	public String getMasterKey() {
		return masterKey;
	}

	@Override
	public void setMasterKey(String masterKey) {
		this.masterKey = masterKey;
	}

	@Override
	public boolean isAvailable() {
		return true;
	}
}
