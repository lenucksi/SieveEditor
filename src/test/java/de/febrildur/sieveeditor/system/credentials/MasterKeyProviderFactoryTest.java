package de.febrildur.sieveeditor.system.credentials;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import de.febrildur.sieveeditor.system.AppDirectoryService;
import de.febrildur.sieveeditor.testutil.TestMasterKeyProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class MasterKeyProviderFactoryTest {

    private static final String EXPECTED_KEY = "test-master-key-for-unit-tests";

    @BeforeEach
    @AfterEach
    void ensureTestMode() {
        MasterKeyProviderFactory.setTestMode(new TestMasterKeyProvider(EXPECTED_KEY));
    }

    @Nested
    @DisplayName("Test mode")
    class TestMode {

        @Test
        void shouldBeEnabledByTestModeExtension() {
            assertThat(MasterKeyProviderFactory.isTestMode()).isTrue();
        }

        @Test
        void shouldSetAndClearTestMode() throws CredentialException {
            MasterKeyProviderFactory.clearTestMode();
            assertThat(MasterKeyProviderFactory.isTestMode()).isFalse();

            assertThat(MasterKeyProviderFactory.create("prompt"))
                    .isInstanceOf(UserPromptMasterKeyProvider.class);

            MasterKeyProviderFactory.setTestMode(new TestMasterKeyProvider(EXPECTED_KEY));
            assertThat(MasterKeyProviderFactory.isTestMode()).isTrue();
        }
    }

    @Nested
    @DisplayName("Provider creation")
    class ProviderCreation {

        @Test
        void shouldReturnTestProviderWhenCreateCalledInTestMode() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create())
                    .isInstanceOf(TestMasterKeyProvider.class);
        }

        @Test
        void shouldReturnTestProviderWhenCreateWithPromptArgInTestMode() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create("prompt"))
                    .isInstanceOf(TestMasterKeyProvider.class);
        }

        @Test
        void shouldReturnTestProviderWhenCreateWithKeepassxcArgInTestMode() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create("keepassxc"))
                    .isInstanceOf(TestMasterKeyProvider.class);
        }

        @Test
        void shouldReturnTestProviderWhenCreateWithKeychainArgInTestMode() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create("keychain"))
                    .isInstanceOf(TestMasterKeyProvider.class);
        }
    }

    @Nested
    @DisplayName("Global forced backend")
    class GlobalForcedBackend {

        @Test
        void shouldSetAndClearGlobalForcedBackend() {
            MasterKeyProviderFactory.setGlobalForcedBackend("prompt");
            MasterKeyProviderFactory.setGlobalForcedBackend(null);
            MasterKeyProviderFactory.setGlobalForcedBackend("keepassxc");
            MasterKeyProviderFactory.setGlobalForcedBackend(null);
        }

        @Test
        void shouldStillReturnTestProviderWithGlobalForcedBackendSet() throws CredentialException {
            MasterKeyProviderFactory.setGlobalForcedBackend("keychain");
            try {
                assertThat(MasterKeyProviderFactory.create())
                        .isInstanceOf(TestMasterKeyProvider.class);
            } finally {
                MasterKeyProviderFactory.setGlobalForcedBackend(null);
            }
        }

        @Test
        void shouldUseGlobalForcedBackendWhenTestModeIsCleared() throws CredentialException {
            MasterKeyProviderFactory.clearTestMode();
            MasterKeyProviderFactory.setGlobalForcedBackend("prompt");
            try {
                assertThat(MasterKeyProviderFactory.create())
                        .isInstanceOf(UserPromptMasterKeyProvider.class);
            } finally {
                MasterKeyProviderFactory.setGlobalForcedBackend(null);
                MasterKeyProviderFactory.setTestMode(new TestMasterKeyProvider(EXPECTED_KEY));
            }
        }
    }

    @Nested
    @DisplayName("Backend argument mapping")
    class BackendArgumentMapping {

        @BeforeEach
        void disableTestMode() {
            MasterKeyProviderFactory.clearTestMode();
        }

        @AfterEach
        void reenableTestMode() {
            MasterKeyProviderFactory.setTestMode(new TestMasterKeyProvider(EXPECTED_KEY));
        }

        @Test
        void shouldCreateUserPromptForPromptArg() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create("prompt"))
                    .isInstanceOf(UserPromptMasterKeyProvider.class);
        }

        @Test
        void shouldCreateUserPromptForKeepassxcArg() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create("keepassxc"))
                    .isInstanceOf(UserPromptMasterKeyProvider.class);
        }

        @Test
        void shouldCreateUserPromptForKeychainArg() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create("keychain"))
                    .isInstanceOf(UserPromptMasterKeyProvider.class);
        }

        @Test
        void shouldCreateUserPromptForUnknownArg() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create("unknown"))
                    .isInstanceOf(UserPromptMasterKeyProvider.class);
        }

        @Test
        void shouldCreateUserPromptForManualArg() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create("manual"))
                    .isInstanceOf(UserPromptMasterKeyProvider.class);
        }

        @Test
        void shouldCreateUserPromptForPasswordArg() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create("password"))
                    .isInstanceOf(UserPromptMasterKeyProvider.class);
        }

        @Test
        void shouldCreateUserPromptForKeepassArg() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create("keepass"))
                    .isInstanceOf(UserPromptMasterKeyProvider.class);
        }

        @Test
        void shouldCreateUserPromptForOsArg() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create("os"))
                    .isInstanceOf(UserPromptMasterKeyProvider.class);
        }

        @Test
        void shouldCreateUserPromptForSystemArg() throws CredentialException {
            assertThat(MasterKeyProviderFactory.create("system"))
                    .isInstanceOf(UserPromptMasterKeyProvider.class);
        }
    }

    @Nested
    @DisplayName("Saved backend preference")
    class SavedBackendPreference {

        @TempDir
        Path tempDir;

        @BeforeEach
        void disableTestMode() {
            MasterKeyProviderFactory.clearTestMode();
        }

        @AfterEach
        void reenableTestMode() {
            MasterKeyProviderFactory.setTestMode(new TestMasterKeyProvider(EXPECTED_KEY));
        }

        @Test
        void shouldLoadSavedBackendPreference() throws Exception {
            try (var appDirMock = mockStatic(AppDirectoryService.class)) {
                Path configDir = tempDir.resolve("config");
                Files.createDirectories(configDir);
                appDirMock.when(AppDirectoryService::getUserConfigDir).thenReturn(configDir);

                Files.writeString(configDir.resolve(".storage-backend"), "Manual Password Entry");

                assertThat(MasterKeyProviderFactory.create())
                        .isInstanceOf(UserPromptMasterKeyProvider.class);
            }
        }
    }
}
