package forge.gamemodes.match;

import forge.localinstance.properties.ForgePreferences.FPref;

import java.util.EnumSet;
import java.util.Set;

import static forge.gamemodes.match.DeclineScope.ALWAYS;
import static forge.gamemodes.match.DeclineScope.NEVER;
import static forge.gamemodes.match.DeclineScope.STACK;
import static forge.gamemodes.match.DeclineScope.TURN;

/** A user-facing yield suggestion. Owns the metadata that constrains its UX:
 *  which decline scopes apply, and which FPref persists the chosen scope. */
public enum SuggestionType {
    STACK_YIELD(EnumSet.of(NEVER, ALWAYS, STACK, TURN), FPref.YIELD_DECLINE_SCOPE_STACK_YIELD),
    NO_ACTIONS (EnumSet.of(NEVER, ALWAYS, TURN),        FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS);

    private final Set<DeclineScope> allowedScopes;
    private final FPref scopePref;

    SuggestionType(Set<DeclineScope> allowedScopes, FPref scopePref) {
        this.allowedScopes = allowedScopes;
        this.scopePref = scopePref;
    }

    public Set<DeclineScope> allowedScopes() { return allowedScopes; }
    public FPref scopePref()                 { return scopePref; }
}
