# Test Strategy and Implementation Plan

## Executive Summary

The SieveEditor project currently has **ZERO** tests. This document outlines a comprehensive strategy to add test coverage, including unit tests, integration tests, and end-to-end tests. The strategy prioritizes testing critical security vulnerabilities and high-severity bugs first.

## Current State

### Test Coverage

- **Unit Tests:** 0
- **Integration Tests:** 0
- **End-to-End Tests:** 0
- **Test Framework:** None configured
- **Coverage:** 0%

### Testability Issues

The codebase has significant testability problems:

1. **No Dependency Injection**
   - All dependencies created via `new` in constructors
   - Cannot mock dependencies for testing
   - Tight coupling throughout

2. **Static Dependencies**
   - JOptionPane, SwingUtilities, etc.
   - Cannot test without full Swing environment
   - No abstraction layer

3. **Mixed Concerns**
   - UI logic mixed with business logic
   - No separation between layers
   - Cannot test logic independently

4. **Hardcoded Values**
   - File paths: `~/.sieveproperties`
   - Encryption key: `"KNQ4VnqF24WLe4HZJ9fB9Sth"`
   - Cannot test without affecting real data

5. **Constructors Do Too Much**
   - Application constructor creates entire UI
   - Cannot instantiate for testing
   - Side effects in initialization

## Testing Framework Selection

### Recommended Stack

```xml
<!-- Add to pom.xml -->
<dependencies>
    <!-- Unit Testing -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.1</version>
        <scope>test</scope>
    </dependency>

    <!-- Mocking Framework -->
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.8.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.8.0</version>
        <scope>test</scope>
    </dependency>

    <!-- Assertions -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.24.2</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-swing-junit</artifactId>
        <version>3.17.1</version>
        <scope>test</scope>
    </dependency>

    <!-- Test Containers for Integration Tests -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>1.19.3</version>
        <scope>test</scope>
    </dependency>

    <!-- Mock Server for ManageSieve Protocol -->
    <dependency>
        <groupId>org.mock-server</groupId>
        <artifactId>mockserver-netty</artifactId>
        <version>5.15.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Surefire for unit tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.2.3</version>
        </plugin>

        <!-- Failsafe for integration tests -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-failsafe-plugin</artifactId>
            <version>3.2.3</version>
            <executions>
                <execution>
                    <goals>
                        <goal>integration-test</goal>
                        <goal>verify</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>

        <!-- JaCoCo for coverage -->
        <plugin>
            <groupId>org.jacoco</groupId>
            <artifactId>jacoco-maven-plugin</artifactId>
            <version>0.8.11</version>
            <executions>
                <execution>
                    <goals>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
                <execution>
                    <id>report</id>
                    <phase>test</phase>
                    <goals>
                        <goal>report</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Why These Tools?

- **JUnit 5:** Modern, feature-rich, industry standard
- **Mockito:** Powerful mocking, works well with JUnit 5
- **AssertJ:** Fluent assertions, better error messages
- **AssertJ-Swing:** GUI testing for Swing applications
- **TestContainers:** Real integration tests with containerized services
- **MockServer:** Mock ManageSieve server responses
- **JaCoCo:** Code coverage reporting

## Refactoring for Testability

Before writing comprehensive tests, refactor code to be testable:

### 1. Introduce Interfaces

```java
// New: SieveServerConnection.java
public interface SieveServerConnection {
    void connect(String server, int port, String username, String password)
        throws IOException, ParseException;
    void putScript(String scriptName, String scriptBody)
        throws IOException, ParseException;
    Collection<SieveScript> getListScripts()
        throws IOException, ParseException;
    String getScript(SieveScript script)
        throws IOException, ParseException;
    String checkScript(String scriptBody)
        throws IOException, ParseException;
    void activateScript(String scriptName)
        throws IOException, ParseException;
    void deactivateScript(String scriptName)
        throws IOException, ParseException;
    void logout()
        throws IOException, ParseException;
    boolean isLoggedIn();
}

// ConnectAndListScripts implements SieveServerConnection
public class ConnectAndListScripts implements SieveServerConnection {
    // ... existing implementation
}
```

### 2. Introduce Configuration Interface

```java
// New: SieveConfiguration.java
public interface SieveConfiguration {
    String getServer();
    int getPort();
    String getUsername();
    String getPassword();

    void setServer(String server);
    void setPort(int port);
    void setUsername(String username);
    void setPassword(String password);

    void load() throws IOException;
    void write() throws IOException;
}

// PropertiesSieve implements SieveConfiguration
```

### 3. Extract Dialog Factory

```java
// New: DialogFactory.java
public interface DialogFactory {
    void showError(Component parent, String message, String title);
    void showInfo(Component parent, String message, String title);
    boolean showConfirm(Component parent, String message, String title);
    String showInputDialog(Component parent, String message, String initialValue);
}

// SwingDialogFactory.java - production implementation
public class SwingDialogFactory implements DialogFactory {
    @Override
    public void showError(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }
    // ... other methods
}

// TestDialogFactory.java - test implementation
public class TestDialogFactory implements DialogFactory {
    private String lastMessage;
    private String nextInput = "";

    @Override
    public void showError(Component parent, String message, String title) {
        lastMessage = message;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setNextInput(String input) {
        this.nextInput = input;
    }

    @Override
    public String showInputDialog(Component parent, String message, String initialValue) {
        return nextInput;
    }
}
```

### 4. Dependency Injection in Application

```java
// Refactored Application.java
public class Application extends JFrame {
    private final SieveConfiguration config;
    private final DialogFactory dialogFactory;
    private SieveServerConnection server;

    // Constructor for testing
    public Application(SieveConfiguration config, DialogFactory dialogFactory) {
        this.config = config;
        this.dialogFactory = dialogFactory;
        initialize();
    }

    // Constructor for production
    public Application() {
        this(new PropertiesSieve(), new SwingDialogFactory());
    }

    private void initialize() {
        try {
            config.load();
        } catch (IOException e) {
            dialogFactory.showError(null,
                "Failed to load settings: " + e.getMessage(),
                "Initialization Error");
            throw new RuntimeException("Application initialization failed", e);
        }

        setupUI();
    }

    // Setter for testing
    void setServer(SieveServerConnection server) {
        this.server = server;
    }
}
```

## Test Structure

```text
src/
├── main/java/de/febrildur/sieveeditor/
│   ├── Application.java
│   ├── actions/
│   └── system/
└── test/java/de/febrildur/sieveeditor/
    ├── unit/                          # Unit tests
    │   ├── ApplicationTest.java
    │   ├── actions/
    │   │   ├── ActionConnectTest.java
    │   │   ├── ActionLoadScriptTest.java
    │   │   ├── ActionSaveScriptTest.java
    │   │   └── ActionReplaceTest.java
    │   └── system/
    │       ├── ConnectAndListScriptsTest.java
    │       ├── PropertiesSieveTest.java
    │       └── SieveTokenMakerTest.java
    ├── integration/                   # Integration tests
    │   ├── ServerConnectionIT.java
    │   ├── ScriptManagementIT.java
    │   └── EndToEndIT.java
    ├── security/                      # Security-specific tests
    │   ├── SSLValidationTest.java
    │   ├── EncryptionTest.java
    │   └── InjectionTest.java
    └── testutil/                      # Test utilities
        ├── TestDialogFactory.java
        ├── MockSieveServer.java
        └── TestConfiguration.java
```

## Test Categories

### 1. Unit Tests

Test individual classes in isolation with mocked dependencies.

#### Example: PropertiesSieveTest.java

```java
package de.febrildur.sieveeditor.unit.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.*;

class PropertiesSieveTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldSaveAndLoadProperties() throws Exception {
        // Given
        Path configFile = tempDir.resolve(".sieveproperties");
        PropertiesSieve config = new PropertiesSieve(configFile.toString());

        config.setServer("mail.example.com");
        config.setPort(4190);
        config.setUsername("testuser");
        config.setPassword("testpass");

        // When
        config.write();

        PropertiesSieve loaded = new PropertiesSieve(configFile.toString());
        loaded.load();

        // Then
        assertThat(loaded.getServer()).isEqualTo("mail.example.com");
        assertThat(loaded.getPort()).isEqualTo(4190);
        assertThat(loaded.getUsername()).isEqualTo("testuser");
        assertThat(loaded.getPassword()).isEqualTo("testpass");
    }

    @Test
    void shouldThrowExceptionForInvalidPort() {
        PropertiesSieve config = new PropertiesSieve();

        assertThatThrownBy(() -> config.setPort(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Port must be between 1 and 65535");
    }

    @Test
    void shouldHandleEmptyPassword() throws Exception {
        PropertiesSieve config = new PropertiesSieve();
        config.setPassword("");

        assertThat(config.getPassword()).isEmpty();
    }
}
```

#### Example: ConnectAndListScriptsTest.java

```java
package de.febrildur.sieveeditor.unit.system;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import com.fluffypeople.managesieve.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ConnectAndListScriptsTest {

    @Test
    void shouldValidateServerParameter() {
        ConnectAndListScripts connection = new ConnectAndListScripts();

        assertThatThrownBy(() ->
            connection.connect(null, 4190, "user", "pass"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Server cannot be empty");
    }

    @Test
    void shouldValidatePortRange() {
        ConnectAndListScripts connection = new ConnectAndListScripts();

        assertThatThrownBy(() ->
            connection.connect("server", 70000, "user", "pass"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Port must be between 1 and 65535");
    }

    @Test
    void shouldThrowExceptionWhenAuthenticationFails() {
        // Test authentication failure handling
        // Requires refactoring to inject ManageSieveClient
    }
}
```

#### Example: ActionSaveScriptTest.java

```java
package de.febrildur.sieveeditor.unit.actions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.awt.event.ActionEvent;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActionSaveScriptTest {

    @Mock
    private Application mockApp;

    @Mock
    private DialogFactory mockDialogFactory;

    @Test
    void shouldShowSuccessMessageWhenSaveSucceeds() {
        // Given
        ActionSaveScript action = new ActionSaveScript(mockApp);
        when(mockApp.save()).thenReturn(true);
        when(mockApp.getDialogFactory()).thenReturn(mockDialogFactory);

        // When
        action.actionPerformed(new ActionEvent(this, 0, "save"));

        // Then
        verify(mockApp).save();
        verify(mockApp).updateStatus();
        verify(mockDialogFactory).showInfo(any(), eq("Script saved."), anyString());
    }

    @Test
    void shouldNotShowSuccessMessageWhenSaveFails() {
        // Given
        ActionSaveScript action = new ActionSaveScript(mockApp);
        when(mockApp.save()).thenReturn(false);
        when(mockApp.getDialogFactory()).thenReturn(mockDialogFactory);

        // When
        action.actionPerformed(new ActionEvent(this, 0, "save"));

        // Then
        verify(mockApp).save();
        verify(mockDialogFactory, never()).showInfo(any(), contains("saved"), anyString());
    }
}
```

### 2. Security Tests

Specific tests for security vulnerabilities.

#### Example: SSLValidationTest.java

```java
package de.febrildur.sieveeditor.security;

import org.junit.jupiter.api.Test;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import static org.assertj.core.api.Assertions.*;

class SSLValidationTest {

    @Test
    void shouldRejectSelfSignedCertificates() {
        // Test that SSL validation is enabled
        // Should fail to connect to server with self-signed cert
        // This test ensures the fix is in place
    }

    @Test
    void shouldRejectExpiredCertificates() {
        // Test that expired certificates are rejected
    }

    @Test
    void shouldRejectCertificatesWithWrongHostname() {
        // Test hostname verification
    }

    @Test
    void shouldUseTLS12OrHigher() throws Exception {
        // Given
        SSLContext context = SSLContext.getInstance("TLSv1.3");

        // Then
        assertThat(context.getProtocol()).isIn("TLSv1.3", "TLSv1.2");
    }

    @Test
    void shouldNotAcceptAllCertificates() {
        // Ensure no "trust all" TrustManager exists in production code
        // This is a static analysis test or code review item

        // After fix, this code should not be in the codebase:
        // X509TrustManager with empty checkServerTrusted()
    }
}
```

#### Example: EncryptionTest.java

```java
package de.febrildur.sieveeditor.security;

import org.junit.jupiter.api.Test;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import static org.assertj.core.api.Assertions.*;

class EncryptionTest {

    @Test
    void shouldNotHaveHardcodedEncryptionKey() {
        // This test verifies the fix is in place
        // Read source code or use reflection to check no hardcoded key
        // Or verify key is loaded from external source
    }

    @Test
    void shouldUseStrongEncryptionAlgorithm() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("test");
        encryptor.setAlgorithm("PBEWithHmacSHA512AndAES_256");

        String encrypted = encryptor.encrypt("password");
        String decrypted = encryptor.decrypt(encrypted);

        assertThat(decrypted).isEqualTo("password");
        assertThat(encrypted).isNotEqualTo("password");
    }

    @Test
    void shouldNotStorePlaintextPasswordsInMemory() {
        // Test that passwords are stored as char[] not String
        // Use reflection to verify field types
    }
}
```

#### Example: InjectionTest.java

```java
package de.febrildur.sieveeditor.security;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class InjectionTest {

    @Test
    void shouldRejectInvalidScriptNames() {
        Application app = new Application(
            new TestConfiguration(),
            new TestDialogFactory()
        );

        // Test various injection attempts
        assertThatThrownBy(() ->
            app.validateScriptName("../../../etc/passwd"))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
            app.validateScriptName("script;rm -rf /"))
            .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() ->
            app.validateScriptName("script\0null"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldAcceptValidScriptNames() {
        Application app = new Application(
            new TestConfiguration(),
            new TestDialogFactory()
        );

        assertThatCode(() ->
            app.validateScriptName("my-script.sieve"))
            .doesNotThrowAnyException();

        assertThatCode(() ->
            app.validateScriptName("script_123"))
            .doesNotThrowAnyException();
    }
}
```

### 3. Bug Regression Tests

Tests that verify specific bugs are fixed.

#### Example: Bug57ArrayIndexTest.java

```java
package de.febrildur.sieveeditor.unit.actions;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

/**
 * Regression test for bug in ActionActivateDeactivateScript.java:57
 * Issue: getSelectedRow() returns -1 if no row selected, causing ArrayIndexOutOfBoundsException
 */
class Bug57ArrayIndexTest {

    @Test
    void shouldHandleNoRowSelected() {
        // Given
        Application app = createTestApplication();
        ActionActivateDeactivateScript action = new ActionActivateDeactivateScript(app);

        // Create table with no selection
        JTable table = new JTable();
        table.clearSelection(); // Ensures getSelectedRow() returns -1

        // When/Then - should not throw exception
        assertThatCode(() ->
            action.handleActivate(table, new String[][]{{"script1", "false"}}))
            .doesNotThrowAnyException();

        // Should show error message to user
        TestDialogFactory dialogFactory = (TestDialogFactory) app.getDialogFactory();
        assertThat(dialogFactory.getLastMessage())
            .contains("Please select a script");
    }
}
```

#### Example: Bug176TokenizerTest.java

```java
package de.febrildur.sieveeditor.unit.system;

import org.junit.jupiter.api.Test;
import org.fife.ui.rsyntaxtextarea.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Regression test for bug in SieveTokenMaker.java:176
 * Issue: Modifying loop variable inside forEach has no effect
 */
class Bug176TokenizerTest {

    @Test
    void shouldCorrectlyTokenizeNumbers() {
        // Given
        SieveTokenMaker tokenMaker = new SieveTokenMaker();
        Segment segment = new Segment("if size :over 100K".toCharArray(), 0, 19);

        // When
        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Then - verify number is tokenized correctly
        int tokenCount = 0;
        while (token != null && token.getType() != TokenTypes.NULL) {
            if (token.getLexeme().equals("100")) {
                assertThat(token.getType())
                    .isEqualTo(TokenTypes.LITERAL_NUMBER_DECIMAL_INT);
            }
            token = token.getNextToken();
            tokenCount++;
        }

        assertThat(tokenCount).isGreaterThan(0);
    }

    @Test
    void shouldHandleNumbersFollowedByLetters() {
        // Test edge case where number is immediately followed by letter
        SieveTokenMaker tokenMaker = new SieveTokenMaker();
        Segment segment = new Segment("100K".toCharArray(), 0, 4);

        Token token = tokenMaker.getTokenList(segment, TokenTypes.NULL, 0);

        // Should have number token followed by identifier token
        assertThat(token.getLexeme()).isEqualTo("100");
        assertThat(token.getType()).isEqualTo(TokenTypes.LITERAL_NUMBER_DECIMAL_INT);

        token = token.getNextToken();
        assertThat(token.getLexeme()).isEqualTo("K");
    }
}
```

### 4. Integration Tests

Test interactions between components with real or mock servers.

#### Example: ServerConnectionIT.java

```java
package de.febrildur.sieveeditor.integration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import static org.assertj.core.api.Assertions.*;

class ServerConnectionIT {

    // Use TestContainers to spin up real ManageSieve server
    static GenericContainer<?> sieveServer = new GenericContainer<>("jumanjiman/cyrus:latest")
        .withExposedPorts(4190);

    @BeforeAll
    static void startServer() {
        sieveServer.start();
    }

    @Test
    void shouldConnectToRealServer() throws Exception {
        // Given
        ConnectAndListScripts connection = new ConnectAndListScripts();
        String host = sieveServer.getHost();
        Integer port = sieveServer.getMappedPort(4190);

        // When
        connection.connect(host, port, "cyrus", "cyrus");

        // Then
        assertThat(connection.isLoggedIn()).isTrue();
    }

    @Test
    void shouldUploadAndDownloadScript() throws Exception {
        // Test full upload/download cycle with real server
    }
}
```

#### Example: EndToEndIT.java

```java
package de.febrildur.sieveeditor.integration;

import org.junit.jupiter.api.Test;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.core.GenericTypeMatcher;

class EndToEndIT {

    @Test
    void shouldConnectLoadEditAndSaveScript() {
        // Given - start application
        Application app = new Application();
        FrameFixture window = new FrameFixture(app);
        window.show();

        // When - click Connect
        window.menuItem("connect").click();

        // Fill in connection dialog
        window.textBox("server").enterText("localhost");
        window.textBox("port").enterText("4190");
        window.textBox("username").enterText("testuser");
        window.textBox("password").enterText("testpass");
        window.button("OK").click();

        // Load script
        window.menuItem("loadScript").click();
        window.comboBox("scripts").selectItem("vacation");
        window.button("OK").click();

        // Edit script
        window.textBox("editor").enterText("# Modified script\n");

        // Save script
        window.menuItem("save").click();

        // Then - verify success message
        window.optionPane().requireMessage("Script saved.");
    }
}
```

### 5. GUI Tests

Test Swing UI interactions.

#### Example: ActionConnectUITest.java

```java
package de.febrildur.sieveeditor.unit.actions;

import org.junit.jupiter.api.Test;
import org.assertj.swing.fixture.DialogFixture;
import static org.assertj.core.api.Assertions.*;

class ActionConnectUITest {

    @Test
    void shouldShowPasswordFieldNotTextField() {
        // Given
        Application app = new Application();
        ActionConnect action = new ActionConnect(app);

        // When
        action.actionPerformed(null);

        // Then - find the dialog
        DialogFixture dialog = new DialogFixture(/* find dialog */);

        // Verify password field type
        assertThat(dialog.textBox("password"))
            .isInstanceOf(JPasswordField.class);
    }
}
```

## Bug-Specific Test Cases

For each bug identified in the analysis:

| Bug ID | File | Line | Test Class | Test Method |
|--------|------|------|------------|-------------|
| BUG-001 | ActionReplace.java | 48-49 | ActionReplaceTest | shouldFindNextWhenButtonClicked |
| BUG-002 | ActionActivateDeactivateScript.java | 91 | ActionActivateDeactivateScriptTest | shouldShowCorrectMessageAfterRename |
| BUG-003 | Application.java | 119 | ApplicationTest | shouldThrowExceptionWhenServerNull |
| BUG-004 | Application.java | 122 | ApplicationTest | shouldThrowExceptionWhenScriptNull |
| BUG-005 | ActionSaveScript.java | 21-23 | ActionSaveScriptTest | shouldNotShowSuccessWhenSaveFails |
| BUG-006 | ActionSaveScriptAs.java | 20-23 | ActionSaveScriptAsTest | shouldHandleNullInput |
| BUG-007 | ActionActivateDeactivateScript.java | 57 | ActionActivateDeactivateScriptTest | shouldHandleNoRowSelected |
| BUG-008 | SieveTokenMaker.java | 176 | SieveTokenMakerTest | shouldCorrectlyTokenizeNumbers |
| BUG-009 | ActionCheckScript.java | 26 | ActionCheckScriptTest | shouldHandleNullServer |

## Coverage Goals

### Phase 1 (Week 1-2): Critical Path

- **Target:** 40% overall coverage
- Focus: Security-critical code, bug fixes
- Priority files:
  - ConnectAndListScripts.java: 80% coverage
  - PropertiesSieve.java: 80% coverage
  - Application.java save/load methods: 90% coverage

### Phase 2 (Week 3-4): Core Features

- **Target:** 60% overall coverage
- Focus: All action classes, main application logic
- Priority files:
  - All actions/*.java: 70% coverage
  - Application.java: 60% coverage

### Phase 3 (Week 5-6): Complete Coverage

- **Target:** 80% overall coverage
- Focus: Edge cases, error handling, UI interactions
- All files: 70%+ coverage

## Test Execution Strategy

### Local Development

```bash
# Run all tests
mvn test

# Run only unit tests
mvn test -Dtest="*Test"

# Run only integration tests
mvn verify -Dit.test="*IT"

# Run with coverage
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### CI/CD Integration

```yaml
# .github/workflows/test.yml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 11
        uses: actions/setup-java@v3
        with:
          java-version: '11'

      - name: Run unit tests
        run: mvn test

      - name: Run integration tests
        run: mvn verify

      - name: Generate coverage report
        run: mvn jacoco:report

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: target/site/jacoco/jacoco.xml

      - name: Check coverage threshold
        run: |
          mvn jacoco:check -Djacocodatafile=target/jacoco.exec \
            -Drules.coverage.line.minimum=0.70
```

## Test Data Management

### Mock Data

Create test fixtures for common scenarios:

```java
// src/test/java/de/febrildur/sieveeditor/testutil/TestData.java
public class TestData {

    public static SieveScript createTestScript(String name, boolean active) {
        SieveScript script = new SieveScript();
        script.setName(name);
        script.setActive(active);
        return script;
    }

    public static String createTestScriptContent() {
        return """
            require ["fileinto"];

            if header :contains "subject" "spam" {
                fileinto "Spam";
                stop;
            }
            """;
    }

    public static SieveConfiguration createTestConfiguration() {
        TestConfiguration config = new TestConfiguration();
        config.setServer("localhost");
        config.setPort(4190);
        config.setUsername("testuser");
        config.setPassword("testpass");
        return config;
    }
}
```

### Test Server

Create mock ManageSieve server for integration tests:

```java
// src/test/java/de/febrildur/sieveeditor/testutil/MockSieveServer.java
public class MockSieveServer {
    private final MockServerClient mockServer;
    private final Map<String, String> scripts = new HashMap<>();
    private String activeScript = null;

    public MockSieveServer(int port) {
        mockServer = ClientAndServer.startClientAndServer(port);
        setupMockResponses();
    }

    private void setupMockResponses() {
        // Mock CAPABILITY response
        mockServer.when(
            request().withPath("/")
        ).respond(
            response().withBody("\"IMPLEMENTATION\" \"MockSieve 1.0\"\r\n" +
                               "\"SIEVE\" \"fileinto reject envelope\"\r\n" +
                               "OK\r\n")
        );

        // Mock AUTHENTICATE response
        // Mock LISTSCRIPTS response
        // Mock GETSCRIPT response
        // Mock PUTSCRIPT response
        // etc.
    }

    public void addScript(String name, String content, boolean active) {
        scripts.put(name, content);
        if (active) {
            activeScript = name;
        }
    }

    public void stop() {
        mockServer.stop();
    }
}
```

## Testing Best Practices

### 1. Test Naming

Use descriptive names that explain what is being tested:

```java
// Good
@Test
void shouldThrowExceptionWhenServerIsNull() { }

@Test
void shouldShowSuccessMessageAfterSuccessfulSave() { }

// Bad
@Test
void test1() { }

@Test
void testSave() { }
```

### 2. AAA Pattern

Always use Arrange-Act-Assert pattern:

```java
@Test
void shouldEncryptPassword() {
    // Arrange (Given)
    PropertiesSieve config = new PropertiesSieve();
    String plainPassword = "mypassword";

    // Act (When)
    config.setPassword(plainPassword);
    config.write();
    config.load();

    // Assert (Then)
    assertThat(config.getPassword()).isEqualTo(plainPassword);
}
```

### 3. One Assertion Per Test

Focus tests on single behavior:

```java
// Good - separate tests
@Test
void shouldSetServer() {
    config.setServer("example.com");
    assertThat(config.getServer()).isEqualTo("example.com");
}

@Test
void shouldSetPort() {
    config.setPort(4190);
    assertThat(config.getPort()).isEqualTo(4190);
}

// Acceptable - related assertions
@Test
void shouldSaveAndLoadAllProperties() {
    config.setServer("example.com");
    config.setPort(4190);
    config.write();

    PropertiesSieve loaded = new PropertiesSieve();
    loaded.load();

    assertThat(loaded.getServer()).isEqualTo("example.com");
    assertThat(loaded.getPort()).isEqualTo(4190);
}
```

### 4. Test Independence

Each test should be independent and not rely on execution order:

```java
// Bad - tests depend on order
static PropertiesSieve config;

@BeforeAll
static void setup() {
    config = new PropertiesSieve();
}

@Test
void test1() {
    config.setServer("example.com"); // Modifies shared state
}

@Test
void test2() {
    // Assumes test1 ran first!
    assertThat(config.getServer()).isEqualTo("example.com");
}

// Good - independent tests
@Test
void test1() {
    PropertiesSieve config = new PropertiesSieve();
    config.setServer("example.com");
    assertThat(config.getServer()).isEqualTo("example.com");
}

@Test
void test2() {
    PropertiesSieve config = new PropertiesSieve();
    config.setPort(4190);
    assertThat(config.getPort()).isEqualTo(4190);
}
```

## Implementation Timeline

### Week 1: Setup and Infrastructure

- Add test dependencies to pom.xml
- Create test directory structure
- Set up JaCoCo for coverage
- Configure CI/CD pipeline
- Write first 5 unit tests (smoke tests)

### Week 2: Security Tests

- Write all security tests
- Test SSL validation (after fix)
- Test encryption (after fix)
- Test injection prevention (after fix)
- **Goal:** All security vulnerabilities have tests

### Week 3: Bug Regression Tests

- Write tests for all HIGH severity bugs
- Write tests for all CRITICAL bugs
- **Goal:** All major bugs have regression tests

### Week 4: Core Functionality Tests

- Test Application.java
- Test all action classes
- Test ConnectAndListScripts
- Test PropertiesSieve
- **Goal:** 40% coverage

### Week 5-6: Complete Coverage

- Test SieveTokenMaker
- Test UI components
- Integration tests
- End-to-end tests
- **Goal:** 80% coverage

## Success Metrics

### Quantitative

- 80%+ code coverage
- All CRITICAL and HIGH bugs have tests
- All security fixes have tests
- 0 failing tests in CI/CD
- All tests run in < 5 minutes

### Qualitative

- Tests are maintainable
- Tests document expected behavior
- Tests catch regressions
- Developers can run tests locally easily
- New features include tests

## Maintenance

### Adding New Features

1. Write test first (TDD)
2. Implement feature
3. Ensure test passes
4. Check coverage increased

### Fixing Bugs

1. Write failing test that reproduces bug
2. Fix the bug
3. Ensure test passes
4. Add to regression test suite

### Refactoring

1. Ensure all tests pass before refactoring
2. Refactor code
3. Ensure all tests still pass
4. Update tests if behavior changed (not just implementation)

## Conclusion

This test strategy provides a comprehensive approach to adding tests to a previously untested codebase. By prioritizing security and critical bugs first, we ensure the most important issues are covered early. The phased approach allows incremental progress while maintaining momentum.
