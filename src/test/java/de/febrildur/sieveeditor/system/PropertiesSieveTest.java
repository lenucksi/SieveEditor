package de.febrildur.sieveeditor.system;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

// Note: AppDirectoryService is in the same package, no import needed

/**
 * Comprehensive test suite for PropertiesSieve class.
 * Tests profile management, encryption, and file I/O operations.
 *
 * Note: Test mode for MasterKeyProviderFactory is enabled globally via TestModeExtension.
 */
class PropertiesSieveTest {

    private PropertiesSieve properties;
    private String originalUserHome;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Save original user.home and set to temp directory for testing
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        // Clean up any existing files from previous test runs (Windows isolation issue)
        cleanupProfilesDirectory();

        properties = new PropertiesSieve();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up all profile files to prevent test pollution on Windows
        cleanupProfilesDirectory();

        // Restore original user.home
        System.setProperty("user.home", originalUserHome);
    }

    private void cleanupProfilesDirectory() throws IOException {
        Path profilesDir = tempDir.resolve(".local/share/sieveeditor/profiles");
        if (Files.exists(profilesDir)) {
            try (var stream = Files.walk(profilesDir)) {
                stream.sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
            }
        }
    }

    // ===== Basic Get/Set Operations =====

    @Test
    void shouldGetAndSetServer() {
        // When
        properties.setServer("mail.example.com");

        // Then
        assertThat(properties.getServer()).isEqualTo("mail.example.com");
    }

    @Test
    void shouldGetAndSetPort() {
        // When
        properties.setPort(4190);

        // Then
        assertThat(properties.getPort()).isEqualTo(4190);
    }

    @Test
    void shouldGetAndSetUsername() {
        // When
        properties.setUsername("testuser");

        // Then
        assertThat(properties.getUsername()).isEqualTo("testuser");
    }

    @Test
    void shouldGetAndSetPassword() {
        // When
        properties.setPassword("secretpassword");

        // Then
        assertThat(properties.getPassword()).isEqualTo("secretpassword");
    }

    // ===== File I/O Operations =====

    @Test
    void shouldSaveAndLoadProperties() throws IOException {
        // Given
        properties.setServer("mail.example.com");
        properties.setPort(4190);
        properties.setUsername("testuser");
        properties.setPassword("testpass");

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getServer()).isEqualTo("mail.example.com");
        assertThat(loaded.getPort()).isEqualTo(4190);
        assertThat(loaded.getUsername()).isEqualTo("testuser");
        assertThat(loaded.getPassword()).isEqualTo("testpass");
    }

    @Test
    void shouldCreateProfilesDirectoryIfNotExists() throws IOException {
        // Given - PropertiesSieve uses AppDirectoryService which creates XDG directories
        // The directory is created automatically when PropertiesSieve is instantiated
        Path profilesDir = AppDirectoryService.getProfilesDir();

        // Then - Directory should exist (created by PropertiesSieve in setUp or by getProfilesDir)
        assertThat(profilesDir.toFile()).exists().isDirectory();
    }

    @Test
    void shouldCreateNewFileOnLoad() throws IOException {
        // Given - Use actual profile path from AppDirectoryService
        Path profilesDir = AppDirectoryService.getProfilesDir();
        Path profileFile = profilesDir.resolve("default.properties");

        // When
        properties.load();

        // Then - File should be created
        assertThat(profileFile.toFile()).exists();
    }

    @Test
    void shouldLoadDefaultValuesWhenFileIsEmpty() throws IOException {
        // When
        properties.load();

        // Then
        assertThat(properties.getServer()).isEmpty();
        assertThat(properties.getPort()).isEqualTo(4190); // Default port
        assertThat(properties.getUsername()).isEmpty();
        assertThat(properties.getPassword()).isEmpty();
    }

    @Test
    void shouldHandleEmptyPassword() throws IOException {
        // Given
        properties.setServer("example.com");
        properties.setPort(4190);
        properties.setUsername("user");
        properties.setPassword("");

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getPassword()).isEmpty();
    }

    // ===== Profile Management =====

    @Test
    void shouldHandleMultipleProfiles() throws IOException {
        // Given - Create two different profiles
        PropertiesSieve profile1 = new PropertiesSieve("work");
        profile1.setServer("work.example.com");
        profile1.setPort(4190);
        profile1.setUsername("work.user");
        profile1.setPassword("workpass");
        profile1.write();

        PropertiesSieve profile2 = new PropertiesSieve("personal");
        profile2.setServer("personal.example.com");
        profile2.setPort(2000);
        profile2.setUsername("personal.user");
        profile2.setPassword("personalpass");
        profile2.write();

        // When - Load each profile
        PropertiesSieve loadedWork = new PropertiesSieve("work");
        loadedWork.load();

        PropertiesSieve loadedPersonal = new PropertiesSieve("personal");
        loadedPersonal.load();

        // Then - Verify profiles are isolated
        assertThat(loadedWork.getServer()).isEqualTo("work.example.com");
        assertThat(loadedWork.getPort()).isEqualTo(4190);
        assertThat(loadedWork.getUsername()).isEqualTo("work.user");

        assertThat(loadedPersonal.getServer()).isEqualTo("personal.example.com");
        assertThat(loadedPersonal.getPort()).isEqualTo(2000);
        assertThat(loadedPersonal.getUsername()).isEqualTo("personal.user");
    }

    @Test
    void shouldGetAvailableProfiles() throws IOException {
        // Given - Create multiple profiles
        new PropertiesSieve("profile1").write();
        new PropertiesSieve("profile2").write();
        new PropertiesSieve("profile3").write();

        // When
        List<String> profiles = PropertiesSieve.getAvailableProfiles();

        // Then
        assertThat(profiles)
            .hasSize(3)
            .contains("profile1", "profile2", "profile3");
    }

    @Test
    void shouldReturnDefaultProfileWhenNoneExist() {
        // Given - No profiles directory exists yet
        System.setProperty("user.home", tempDir.resolve("nonexistent").toString());

        // When
        List<String> profiles = PropertiesSieve.getAvailableProfiles();

        // Then
        assertThat(profiles).containsExactly("default");
    }

    @Test
    void shouldReturnDefaultProfileWhenDirectoryIsEmpty() throws IOException {
        // Given - Directory already exists from PropertiesSieve constructor in setUp()
        // Just ensure it's empty (delete any files created by setUp)
        File profilesDir = tempDir.resolve(".sieveprofiles").toFile();
        if (profilesDir.exists()) {
            for (File f : profilesDir.listFiles()) {
                if (f.isFile()) {
                    f.delete();
                }
            }
        }

        // When
        List<String> profiles = PropertiesSieve.getAvailableProfiles();

        // Then
        assertThat(profiles).containsExactly("default");
    }

    @Test
    void shouldReturnSortedProfiles() throws IOException {
        // Given - Create profiles in random order
        new PropertiesSieve("zebra").write();
        new PropertiesSieve("apple").write();
        new PropertiesSieve("banana").write();

        // When
        List<String> profiles = PropertiesSieve.getAvailableProfiles();

        // Then
        assertThat(profiles).containsExactly("apple", "banana", "zebra");
    }

    @Test
    void shouldCheckIfProfileExists() throws IOException {
        // Given
        new PropertiesSieve("existing").write();

        // When/Then
        assertThat(PropertiesSieve.profileExists("existing")).isTrue();
        assertThat(PropertiesSieve.profileExists("nonexistent")).isFalse();
    }

    // ===== Last Used Profile =====

    @Test
    void shouldSaveAndRetrieveLastUsedProfile() throws IOException {
        // When
        PropertiesSieve.saveLastUsedProfile("myprofile");

        // Then
        assertThat(PropertiesSieve.getLastUsedProfile()).isEqualTo("myprofile");
    }

    @Test
    void shouldReturnDefaultWhenLastUsedFileDoesNotExist() throws IOException {
        // Given - Ensure no last used file exists (may have been created by other tests)
        Path configDir = AppDirectoryService.getUserConfigDir();
        Path lastUsedFile = configDir.resolve(".lastused");
        Files.deleteIfExists(lastUsedFile);

        // When - No last used file exists
        String lastUsed = PropertiesSieve.getLastUsedProfile();

        // Then
        assertThat(lastUsed).isEqualTo("default");
    }

    @Test
    void shouldReturnDefaultWhenLastUsedFileIsCorrupt() throws IOException {
        // Given - Create corrupt last used file
        File profilesDir = tempDir.resolve(".sieveprofiles").toFile();
        profilesDir.mkdirs();
        File lastUsedFile = new File(profilesDir, ".lastused");
        Files.writeString(lastUsedFile.toPath(), ""); // Empty file

        // When
        String lastUsed = PropertiesSieve.getLastUsedProfile();

        // Then
        assertThat(lastUsed).isEqualTo("default");
    }

    @Test
    void shouldTrimWhitespaceFromLastUsedProfile() throws IOException {
        // Given - Write to actual config directory used by PropertiesSieve
        Path configDir = AppDirectoryService.getUserConfigDir();
        Path lastUsedFile = configDir.resolve(".lastused");
        Files.writeString(lastUsedFile, "  myprofile  \n");

        // When
        String lastUsed = PropertiesSieve.getLastUsedProfile();

        // Then
        assertThat(lastUsed).isEqualTo("myprofile");
    }

    // ===== Migration =====

    @Test
    void shouldMigrateOldPropertiesFile() throws IOException {
        // Given - Create old legacy file in the legacy location
        Path legacyDir = AppDirectoryService.getLegacyProfilesDir();
        Files.createDirectories(legacyDir);
        Path oldFile = legacyDir.resolve("migrationtest.properties");
        Files.writeString(oldFile, "sieve.server=oldserver.com\nsieve.port=4190");

        // When
        PropertiesSieve.migrateOldProperties();

        // Then - Should copy to new profiles directory
        Path newFile = AppDirectoryService.getProfilesDir().resolve("migrationtest.properties");
        assertThat(newFile.toFile()).exists();
        String content = Files.readString(newFile);
        assertThat(content).contains("oldserver.com");

        // Cleanup
        Files.deleteIfExists(oldFile);
        Files.deleteIfExists(newFile);
    }

    @Test
    void shouldNotOverwriteExistingDefaultProfile() throws IOException {
        // Given - Create both old file and existing default profile
        File oldFile = tempDir.resolve(".sieveproperties").toFile();
        Files.writeString(oldFile.toPath(), "sieve.server=oldserver.com");

        PropertiesSieve existing = new PropertiesSieve("default");
        existing.setServer("existingserver.com");
        existing.write();

        // When
        PropertiesSieve.migrateOldProperties();

        // Then - Should NOT overwrite existing profile
        PropertiesSieve loaded = new PropertiesSieve("default");
        loaded.load();
        assertThat(loaded.getServer()).isEqualTo("existingserver.com");
    }

    @Test
    void shouldSkipMigrationWhenOldFileNotExists() {
        // Given - No old file exists

        // When/Then - Should not throw exception
        assertThatCode(() -> PropertiesSieve.migrateOldProperties())
            .doesNotThrowAnyException();
    }

    // ===== Encryption Tests =====

    @Test
    void shouldEncryptPasswordWhenSaving() throws IOException {
        // Given
        properties.setPassword("plaintextpassword");

        // When
        properties.write();

        // Then - Read raw file and verify password is encrypted
        Path profileFile = AppDirectoryService.getProfilesDir().resolve("default.properties");
        String rawContent = Files.readString(profileFile);

        assertThat(rawContent).doesNotContain("plaintextpassword");
        assertThat(rawContent).contains("ENC("); // Jasypt encrypted format
    }

    @Test
    void shouldDecryptPasswordWhenLoading() throws IOException {
        // Given
        properties.setPassword("mysecretpassword");
        properties.write();

        // When
        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getPassword()).isEqualTo("mysecretpassword");
    }

    @Test
    void shouldHandleCorruptedEncryptedPassword() throws IOException {
        // Given - Manually write corrupt encrypted password
        File profileFile = tempDir.resolve(".sieveprofiles/default.properties").toFile();
        profileFile.getParentFile().mkdirs();
        Files.writeString(profileFile.toPath(),
            "sieve.server=example.com\n" +
            "sieve.port=4190\n" +
            "sieve.user=user\n" +
            "sieve.password=ENC(CORRUPT_ENCRYPTED_DATA_INVALID)");

        // When
        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then - Should return empty password on decryption failure
        assertThat(loaded.getPassword()).isEmpty();
    }

    // ===== Edge Cases and Error Handling =====

    @Test
    void shouldHandleNullValues() {
        // When/Then - Should handle nulls gracefully
        assertThatCode(() -> {
            properties.setServer(null);
            properties.setUsername(null);
            properties.setPassword(null);
            properties.write();
        }).doesNotThrowAnyException();
    }

    @Test
    void shouldHandleSpecialCharactersInServer() throws IOException {
        // Given
        properties.setServer("mail.example-test.com:4190");

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getServer()).isEqualTo("mail.example-test.com:4190");
    }

    @Test
    void shouldHandleSpecialCharactersInUsername() throws IOException {
        // Given
        properties.setUsername("user@example.com");

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getUsername()).isEqualTo("user@example.com");
    }

    @Test
    void shouldHandleSpecialCharactersInPassword() throws IOException {
        // Given
        properties.setPassword("p@ssw0rd!#$%^&*()");

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getPassword()).isEqualTo("p@ssw0rd!#$%^&*()");
    }

    @Test
    void shouldHandleVeryLongPassword() throws IOException {
        // Given - 256 character password
        String longPassword = "a".repeat(256);
        properties.setPassword(longPassword);

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getPassword()).isEqualTo(longPassword);
    }

    @Test
    void shouldHandleUnicodeInPassword() throws IOException {
        // Given
        properties.setPassword("пароль密码🔒");

        // When
        properties.write();

        PropertiesSieve loaded = new PropertiesSieve();
        loaded.load();

        // Then
        assertThat(loaded.getPassword()).isEqualTo("пароль密码🔒");
    }

    @Test
    void shouldHandleProfileNameWithSpecialCharacters() throws IOException {
        // Given
        PropertiesSieve profile = new PropertiesSieve("test-profile_123");
        profile.setServer("example.com");

        // When
        profile.write();

        PropertiesSieve loaded = new PropertiesSieve("test-profile_123");
        loaded.load();

        // Then
        assertThat(loaded.getServer()).isEqualTo("example.com");
    }

    @Test
    void shouldNotListNonPropertiesFiles() throws IOException {
        // Given - Create some non-.properties files
        File profilesDir = tempDir.resolve(".sieveprofiles").toFile();
        profilesDir.mkdirs();
        new File(profilesDir, "readme.txt").createNewFile();
        new File(profilesDir, ".lastused").createNewFile();

        new PropertiesSieve("valid").write();

        // When
        List<String> profiles = PropertiesSieve.getAvailableProfiles();

        // Then - Should only list .properties files
        assertThat(profiles).containsExactly("valid");
    }

    // ===== Profile Deletion Tests =====

    @Test
    void shouldDeleteProfileByName() throws IOException {
        // Given - Create a profile
        PropertiesSieve profile = new PropertiesSieve("todelete");
        profile.setServer("example.com");
        profile.setPort(4190);
        profile.write();

        // Verify profile exists
        assertThat(PropertiesSieve.profileExists("todelete")).isTrue();
        assertThat(PropertiesSieve.getAvailableProfiles()).contains("todelete");

        // When
        PropertiesSieve.deleteProfile("todelete");

        // Then
        assertThat(PropertiesSieve.profileExists("todelete")).isFalse();
        assertThat(PropertiesSieve.getAvailableProfiles()).doesNotContain("todelete");
    }

    @Test
    void shouldDeleteProfileFile() throws IOException {
        // Given - Create a profile
        PropertiesSieve profile = new PropertiesSieve("todelete");
        profile.write();

        Path profileFile = AppDirectoryService.getProfilesDir().resolve("todelete.properties");
        assertThat(profileFile.toFile()).exists();

        // When
        PropertiesSieve.deleteProfile("todelete");

        // Then - File should be deleted
        assertThat(profileFile.toFile()).doesNotExist();
    }

    @Test
    void shouldNotThrowExceptionWhenDeletingNonexistentProfile() {
        // When/Then - Should not throw exception
        assertThatCode(() -> PropertiesSieve.deleteProfile("nonexistent"))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldReturnTrueWhenProfileDeleted() throws IOException {
        // Given
        new PropertiesSieve("todelete").write();

        // When
        boolean result = PropertiesSieve.deleteProfile("todelete");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenProfileDoesNotExist() {
        // When
        boolean result = PropertiesSieve.deleteProfile("nonexistent");

        // Then
        assertThat(result).isFalse();
    }
}
