package de.febrildur.sieveeditor.system.credentials;

/**
 * Exception thrown when credential storage/retrieval operations fail.
 */
public class CredentialException extends Exception {

	public CredentialException(String message) {
		super(message);
	}

	public CredentialException(String message, Throwable cause) {
		super(message, cause);
	}
}
