package forge.game.decision;

import forge.game.spellability.SpellAbility;

/** Raised instead of silently omitting a priority action outside the supported legality slice. */
public final class UnsupportedPriorityActionException extends IllegalStateException {
    public UnsupportedPriorityActionException(final SpellAbility ability, final String reason) {
        super("Unsupported priority action for " + ability.getHostCard().getName() + ": " + reason);
    }
}
