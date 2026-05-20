package de.febrildur.sieveeditor.system;
// SPDX-FileCopyrightText: 2026 Lenucksi
//
// SPDX-License-Identifier: LGPL-3.0-or-later

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class AppDirectoryServiceTest {

    private static final String SIEVEEDITOR_TEST_DIR = "sieveeditor.test.dir";

    @TempDir
    Path tempDir;

    private String originalTestDir;
    private String originalUserHome;

    @BeforeEach
    void setUp() {
        originalTestDir = System.getProperty(SIEVEEDITOR_TEST_DIR);
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toAbsolutePath().toString());
        System.setProperty(SIEVEEDITOR_TEST_DIR, tempDir.toAbsolutePath().toString());
    }

    @AfterEach
    void tearDown() {
        if (originalTestDir != null) {
            System.setProperty(SIEVEEDITOR_TEST_DIR, originalTestDir);
        } else {
            System.clearProperty(SIEVEEDITOR_TEST_DIR);
        }
        if (originalUserHome != null) {
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    void shouldReturnProfilesDir() {
        Path profilesDir = AppDirectoryService.getProfilesDir();
        assertThat(profilesDir).isNotNull();
        assertThat(profilesDir.getFileName().toString()).isEqualTo("profiles");
    }

    @Test
    void shouldCreateProfilesDirIfNotExists() {
        Path profilesDir = AppDirectoryService.getProfilesDir();
        assertThat(Files.exists(profilesDir)).isTrue();
    }

    @Test
    void shouldReturnUserConfigDir() {
        Path configDir = AppDirectoryService.getUserConfigDir();
        assertThat(configDir).isNotNull();
        assertThat(Files.exists(configDir)).isTrue();
    }

    @Test
    void shouldReturnLegacyProfilesDir() {
        Path legacyDir = AppDirectoryService.getLegacyProfilesDir();
        assertThat(legacyDir).isNotNull();
        assertThat(legacyDir.getFileName().toString()).isEqualTo(".sieveprofiles");
    }

    @Test
    void shouldReturnUserDataDir() {
        Path dataDir = AppDirectoryService.getUserDataDir();
        assertThat(dataDir).isNotNull();
        assertThat(Files.exists(dataDir)).isTrue();
    }

    @Test
    void shouldNotNeedMigrationWhenLegacyDirDoesNotExist() {
        assertThat(AppDirectoryService.needsMigration()).isFalse();
    }

    @Test
    void shouldSetSecureFilePermissions() throws IOException {
        Path testFile = tempDir.resolve("test.properties");
        Files.writeString(testFile, "test content");
        AppDirectoryService.setSecureFilePermissions(testFile);
        assertThat(Files.exists(testFile)).isTrue();
    }

    @Test
    void shouldReuseExistingProfilesDir() {
        Path first = AppDirectoryService.getProfilesDir();
        Path second = AppDirectoryService.getProfilesDir();
        assertThat(second).isEqualTo(first);
    }

    // ===== Migration Detection Edge Cases =====

    @Test
    void shouldNeedMigrationWhenLegacyDirHasProperties() throws IOException {
        Path legacyDir = AppDirectoryService.getLegacyProfilesDir();
        Files.createDirectories(legacyDir);
        Files.createFile(legacyDir.resolve("test.properties"));

        assertThat(AppDirectoryService.needsMigration()).isTrue();
    }

    @Test
    void shouldNotNeedMigrationWhenLegacyDirHasNoProperties() throws IOException {
        Path legacyDir = AppDirectoryService.getLegacyProfilesDir();
        Files.createDirectories(legacyDir);
        Files.createFile(legacyDir.resolve("test.txt"));

        assertThat(AppDirectoryService.needsMigration()).isFalse();
    }

    @Test
    void shouldNotNeedMigrationWhenLegacyDirIsEmpty() throws IOException {
        Path legacyDir = AppDirectoryService.getLegacyProfilesDir();
        Files.createDirectories(legacyDir);

        assertThat(AppDirectoryService.needsMigration()).isFalse();
    }

    @Test
    void shouldNotThrowWhenLegacyPathIsNotADirectory() throws IOException {
        Path legacyDir = AppDirectoryService.getLegacyProfilesDir();
        Files.createDirectories(legacyDir.getParent());
        Files.createFile(legacyDir);

        assertThat(AppDirectoryService.needsMigration()).isFalse();
    }

    // ===== Data Directory Edge Cases =====

    @Test
    void shouldGetUserDataDir() {
        Path dataDir = AppDirectoryService.getUserDataDir();
        assertThat(dataDir).isNotNull();
        assertThat(Files.exists(dataDir)).isTrue();
    }

    // ===== File Permission Edge Cases =====

    @Test
    void shouldHandleSetSecureFilePermissionsOnNonExistentFile() {
        Path nonExistentFile = tempDir.resolve("nonexistent.properties");
        assertThatCode(() -> AppDirectoryService.setSecureFilePermissions(nonExistentFile))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowWhenDataDirCannotBeCreated() {
        System.clearProperty(SIEVEEDITOR_TEST_DIR);
        Path parentDir = tempDir.resolve("nonwritable");
        parentDir.toFile().mkdirs();
        parentDir.toFile().setWritable(false);
        System.setProperty("user.home", parentDir.toString());

        assertThatThrownBy(() -> AppDirectoryService.getUserDataDir())
                .isInstanceOf(RuntimeException.class);

        parentDir.toFile().setWritable(true);
    }
}
