package de.febrildur.sieveeditor.templates;

/**
 * Represents a Sieve script template with metadata.
 */
public class SieveTemplate {

    private final String name;
    private final String description;
    private final String content;
    private final boolean builtin;

    public SieveTemplate(String name, String description, String content, boolean builtin) {
        this.name = name;
        this.description = description;
        this.content = content;
        this.builtin = builtin;
    }

    /**
     * Creates a built-in template.
     */
    public static SieveTemplate builtin(String name, String description, String content) {
        return new SieveTemplate(name, description, content, true);
    }

    /**
     * Creates a user-defined template.
     */
    public static SieveTemplate user(String name, String content) {
        return new SieveTemplate(name, "User template", content, false);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getContent() {
        return content;
    }

    public boolean isBuiltin() {
        return builtin;
    }

    @Override
    public String toString() {
        return name;
    }
}
