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

/**
 * Comprehensive test suite for PropertiesSieve class.
 * Tests profile management, encryption, and file I/O operations.
 */
class PropertiesSieveTest {

    private PropertiesSieve properties;
    private String originalUserHome;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Save original user.home and set to temp directory for testing
        originalUserHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        properties = new PropertiesSieve();
    }

    @AfterEach
    void tearDown() {
        // Restore original user.home
        System.setProperty("user.home", originalUserHome);
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
        // Given - Use a different temp directory to avoid collision with setUp()
        // setUp() already creates a PropertiesSieve instance which creates the directory
        Path newTempDir = Files.createTempDirectory("sieve-test-isolated");
        try {
            String originalUserHome = System.getProperty("user.home");
            System.setProperty("user.home", newTempDir.toString());

            File profilesDir = new File(newTempDir.toFile(), ".sieveprofiles");
            assertThat(profilesDir).doesNotExist();

            // When
            PropertiesSieve props = new PropertiesSieve("test");

            // Then
            assertThat(profilesDir).exists().isDirectory();

            // Cleanup
            System.setProperty("user.home", originalUserHome);
        } finally {
            // Clean up temp directory
            java.util.Comparator<Path> reverseOrder = java.util.Comparator.reverseOrder();
            Files.walk(newTempDir)
                .sorted(reverseOrder)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }

    @Test
    void shouldCreateNewFileOnLoad() throws IOException {
        // Given
        File profilesDir = new File(tempDir.toFile(), ".sieveprofiles");
        File profileFile = new File(profilesDir, "default.properties");
        assertThat(profileFile).doesNotExist();

        // When
        properties.load();

        // Then
        assertThat(profileFile).exists();
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
    void shouldReturnDefaultWhenLastUsedFileDoesNotExist() {
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
        // Given
        File profilesDir = tempDir.resolve(".sieveprofiles").toFile();
        profilesDir.mkdirs();
        File lastUsedFile = new File(profilesDir, ".lastused");
        Files.writeString(lastUsedFile.toPath(), "  myprofile  \n");

        // When
        String lastUsed = PropertiesSieve.getLastUsedProfile();

        // Then
        assertThat(lastUsed).isEqualTo("myprofile");
    }

    // ===== Migration =====

    @Test
    void shouldMigrateOldPropertiesFile() throws IOException {
        // Given - Create old .sieveproperties file
        File oldFile = tempDir.resolve(".sieveproperties").toFile();
        Files.writeString(oldFile.toPath(), "sieve.server=oldserver.com\nsieve.port=4190");

        // When
        PropertiesSieve.migrateOldProperties();

        // Then - Should copy to default.properties
        File newFile = tempDir.resolve(".sieveprofiles/default.properties").toFile();
        assertThat(newFile).exists();
        String content = Files.readString(newFile.toPath());
        assertThat(content).contains("oldserver.com");
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
        File profileFile = tempDir.resolve(".sieveprofiles/default.properties").toFile();
        String rawContent = Files.readString(profileFile.toPath());

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
}
