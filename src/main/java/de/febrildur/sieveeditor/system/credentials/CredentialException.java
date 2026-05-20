package de.febrildur.sieveeditor.system.credentials;
// SPDX-FileCopyrightText: 2025 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

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
