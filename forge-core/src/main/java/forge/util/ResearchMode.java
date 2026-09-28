package forge.util;

/**
 * Global switch for research-only behavior in the MTG Forge for Research fork.
 *
 * <p>Research mode is disabled by default. It can be enabled for a process with
 * either the JVM system property {@code -Dforge.research=true} or the
 * environment variable {@code FORGE_RESEARCH_MODE=true}. The JVM property
 * takes precedence when both are present.</p>
 *
 * <p>All research-only code paths should be guarded by
 * {@link #isEnabled()} so normal Forge behavior remains unchanged when the
 * mode is disabled.</p>
 */
public final class ResearchMode {
    public static final String SYSTEM_PROPERTY = "forge.research";
    public static final String ENVIRONMENT_VARIABLE = "FORGE_RESEARCH_MODE";

    private static final boolean ENABLED = parseEnabled(
            System.getProperty(SYSTEM_PROPERTY, System.getenv(ENVIRONMENT_VARIABLE)));

    private ResearchMode() {
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    private static boolean parseEnabled(String value) {
        if (value == null) {
            return false;
        }
        return switch (value.trim().toLowerCase()) {
            case "1", "true", "yes", "on" -> true;
            default -> false;
        };
    }
}
