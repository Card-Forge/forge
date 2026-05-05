package forge.gamemodes.match;

/** How long a smart-suggestion decline suppresses the suggestion. Persisted via {@link Enum#name()}. */
public enum DeclineScope {
    NEVER("lblDeclScopeNever"),
    ALWAYS("lblDeclScopeAlways"),
    STACK("lblDeclScopeStack"),
    TURN("lblDeclScopeTurn");

    private final String labelKey;

    DeclineScope(String labelKey) {
        this.labelKey = labelKey;
    }

    /** Localizer key for the dropdown label. */
    public String labelKey() { return labelKey; }

    /** Parse stored FPref value. Unknown / null → {@link #NEVER}. */
    public static DeclineScope fromPref(String s) {
        if (s == null || s.isEmpty()) return NEVER;
        try { return valueOf(s); } catch (IllegalArgumentException ignored) { return NEVER; }
    }
}
