package forge.game.decision;

import forge.game.spellability.SpellAbility;

/** Raised when a live Forge target operation cannot be exported without changing its semantics or leaking data. */
public final class UnsupportedTargetDecisionException extends IllegalStateException {
    public UnsupportedTargetDecisionException(final SpellAbility ability, final String reason) {
        super("Unsupported ForgeRL TARGET decision for " + ability.getHostCard().getName() + ": " + reason);
    }
}
