package de.febrildur.sieveeditor.templates;

import de.febrildur.sieveeditor.system.AppDirectoryService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing Sieve script templates.
 *
 * Provides built-in templates for common Sieve patterns and supports
 * user-defined templates from a platform-specific directory.
 *
 * User templates location:
 * - Linux: ~/.local/share/sieveeditor/templates/
 * - Windows: %LOCALAPPDATA%/febrildur/sieveeditor/templates/
 * - macOS: ~/Library/Application Support/sieveeditor/templates/
 */
public class TemplateService {

    private static final Logger LOGGER = Logger.getLogger(TemplateService.class.getName());
    private static final String TEMPLATES_SUBDIR = "templates";

    private final List<SieveTemplate> builtinTemplates;

    public TemplateService() {
        this.builtinTemplates = createBuiltinTemplates();
    }

    /**
     * Gets all built-in templates.
     */
    public List<SieveTemplate> getBuiltinTemplates() {
        return Collections.unmodifiableList(builtinTemplates);
    }

    /**
     * Gets user-defined templates from the templates directory.
     */
    public List<SieveTemplate> getUserTemplates() {
        List<SieveTemplate> templates = new ArrayList<>();
        Path templatesDir = getTemplatesDirectory();

        if (!Files.exists(templatesDir)) {
            return templates;
        }

        try {
            Files.list(templatesDir)
                    .filter(p -> p.toString().endsWith(".sieve"))
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path);
                            String name = path.getFileName().toString()
                                    .replace(".sieve", "");
                            templates.add(SieveTemplate.user(name, content));
                        } catch (IOException e) {
                            LOGGER.log(Level.WARNING, "Failed to read template: " + path, e);
                        }
                    });
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to list templates directory", e);
        }

        return templates;
    }

    /**
     * Gets all templates (built-in and user-defined).
     */
    public List<SieveTemplate> getAllTemplates() {
        List<SieveTemplate> all = new ArrayList<>(builtinTemplates);
        all.addAll(getUserTemplates());
        return all;
    }

    /**
     * Gets the platform-specific templates directory.
     */
    public Path getTemplatesDirectory() {
        return AppDirectoryService.getUserDataDir().resolve(TEMPLATES_SUBDIR);
    }

    /**
     * Ensures the templates directory exists.
     */
    public void ensureTemplatesDirectoryExists() {
        Path dir = getTemplatesDirectory();
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
                LOGGER.log(Level.INFO, "Created templates directory: {0}", dir);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to create templates directory", e);
            }
        }
    }

    /**
     * Creates the set of built-in templates.
     */
    private List<SieveTemplate> createBuiltinTemplates() {
        List<SieveTemplate> templates = new ArrayList<>();

        // Basic spam filtering
        templates.add(SieveTemplate.builtin(
                "Spam Filter to Folder",
                "Move spam-flagged emails to Spam folder",
                """
                require ["fileinto"];

                # Move spam to Spam folder
                if header :contains "X-Spam-Flag" "YES" {
                    fileinto "Spam";
                    stop;
                }
                """));

        // Vacation auto-reply
        templates.add(SieveTemplate.builtin(
                "Vacation Auto-Reply",
                "Send automatic out-of-office replies",
                """
                require ["vacation"];

                # Auto-reply for vacation
                vacation :days 7 :subject "Out of Office"
                "I am currently out of office and will return on [DATE].
                Your email will be read upon my return.";
                """));

        // File by subject keyword
        templates.add(SieveTemplate.builtin(
                "Fileinto by Subject",
                "Route emails to folder based on subject keywords",
                """
                require ["fileinto"];

                # File emails by subject keyword
                if header :contains "subject" "[KEYWORD]" {
                    fileinto "[FOLDER]";
                    stop;
                }
                """));

        // File by sender address
        templates.add(SieveTemplate.builtin(
                "Fileinto by Sender",
                "Route emails from specific sender to folder",
                """
                require ["fileinto"];

                # File emails from specific sender
                if address :is "from" "sender@example.com" {
                    fileinto "[FOLDER]";
                    stop;
                }
                """));

        // Reject large messages
        templates.add(SieveTemplate.builtin(
                "Reject by Size",
                "Reject emails larger than specified size",
                """
                require ["reject"];

                # Reject emails larger than 10MB
                if size :over 10M {
                    reject "Message too large. Please send files via file sharing.";
                }
                """));

        // Discard by sender
        templates.add(SieveTemplate.builtin(
                "Discard by Sender",
                "Silently discard emails from specific sender",
                """
                # Silently discard emails from specific sender
                if address :is "from" "spam@example.com" {
                    discard;
                    stop;
                }
                """));

        // Mailing list filter
        templates.add(SieveTemplate.builtin(
                "Mailing List Filter",
                "Route mailing list emails to dedicated folder",
                """
                require ["fileinto", "mailbox"];

                # Route mailing list emails by List-Id header
                if header :contains "List-Id" "listname.example.com" {
                    fileinto :create "INBOX.Lists.[LISTNAME]";
                    stop;
                }
                """));

        // Priority flagging
        templates.add(SieveTemplate.builtin(
                "Priority Flagging",
                "Flag high-priority messages from important senders",
                """
                require ["imap4flags", "fileinto"];

                # Flag and file VIP messages
                if anyof (
                    address :is "from" "boss@company.com",
                    address :is "from" "ceo@company.com",
                    header :contains "X-Priority" "1"
                ) {
                    setflag "\\\\Flagged";
                    fileinto "INBOX.Priority";
                    stop;
                }
                """));

        // Notification filtering
        templates.add(SieveTemplate.builtin(
                "Notification Filter",
                "Route automated notifications to separate folder",
                """
                require ["fileinto"];

                # Detect and file automated notifications
                if anyof (
                    header :contains "from" "noreply@",
                    header :contains "from" "no-reply@",
                    header :is "Precedence" "bulk",
                    header :is "Precedence" "list"
                ) {
                    fileinto "INBOX.Notifications";
                    stop;
                }
                """));

        // Domain-based filtering
        templates.add(SieveTemplate.builtin(
                "Filter by Domain",
                "Route emails from specific domain to folder",
                """
                require ["fileinto"];

                # File all emails from a domain
                if address :domain "from" "example.com" {
                    fileinto "INBOX.Example";
                    stop;
                }
                """));

        // Multiple condition filter
        templates.add(SieveTemplate.builtin(
                "Multiple Conditions",
                "Complex filter with AND/OR conditions",
                """
                require ["fileinto", "imap4flags"];

                # Complex filter: high priority from specific domain
                if allof (
                    address :domain "from" "important-client.com",
                    anyof (
                        header :contains "subject" "urgent",
                        header :contains "X-Priority" "1"
                    )
                ) {
                    setflag "\\\\Flagged";
                    fileinto "INBOX.Urgent";
                    stop;
                }
                """));

        // Duplicate detection (Dovecot)
        templates.add(SieveTemplate.builtin(
                "Duplicate Detection",
                "Detect and file duplicate messages (Dovecot)",
                """
                require ["duplicate", "fileinto"];

                # Detect duplicate messages within 24 hours
                if duplicate :seconds 86400 {
                    fileinto "INBOX.Duplicates";
                    stop;
                }
                """));

        // Subaddress/plus addressing
        templates.add(SieveTemplate.builtin(
                "Subaddress Routing",
                "Route emails by plus-address tag (user+tag@domain)",
                """
                require ["subaddress", "fileinto", "variables"];

                # Route by plus-address tag
                if subaddress :matches "to" "*" {
                    set :lower "tag" "${1}";
                    fileinto "INBOX.${tag}";
                    stop;
                }
                """));

        // Complete starter script
        templates.add(SieveTemplate.builtin(
                "Complete Starter Script",
                "Full script with common rules and structure",
                """
                require [
                    "fileinto",
                    "mailbox",
                    "imap4flags"
                ];

                # ===== SPAM FILTERING =====
                if header :contains "X-Spam-Flag" "YES" {
                    fileinto "Spam";
                    stop;
                }

                # ===== PRIORITY RULES =====
                # VIP senders get flagged
                if address :is "from" ["boss@company.com", "important@example.com"] {
                    setflag "\\\\Flagged";
                    fileinto :create "INBOX.Priority";
                    stop;
                }

                # ===== MAILING LISTS =====
                if header :contains "List-Id" "mylist" {
                    fileinto :create "INBOX.Lists.MyList";
                    stop;
                }

                # ===== NOTIFICATIONS =====
                if anyof (
                    header :contains "from" "noreply@",
                    header :is "Precedence" "bulk"
                ) {
                    fileinto :create "INBOX.Notifications";
                    stop;
                }

                # ===== DEFAULT: Keep in Inbox =====
                keep;
                """));

        return templates;
    }
}
