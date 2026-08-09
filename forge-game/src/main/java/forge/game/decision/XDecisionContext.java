package forge.game.decision;

import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/** Immutable public metadata for one live Forge X announcement. */
public final class XDecisionContext {
    private final SpellAbility ability;
    private final Player choosingPlayer;
    private final int rawMin;
    private final int rawMax;
    private final ActionContinuation continuation;
    private final Integer subdecisionIndex;

    XDecisionContext(final SpellAbility ability, final Player choosingPlayer, final int rawMin,
            final int rawMax, final ActionContinuation continuation, final Integer subdecisionIndex) {
        this.ability = ability;
        this.choosingPlayer = choosingPlayer;
        this.rawMin = rawMin;
        this.rawMax = rawMax;
        this.continuation = continuation;
        this.subdecisionIndex = subdecisionIndex;
    }

    SpellAbility getAbility() {
        return ability;
    }

    Player getChoosingPlayer() {
        return choosingPlayer;
    }

    public int getChoosingPlayerId() {
        return choosingPlayer.getId();
    }

    public String getVariableName() {
        return "X";
    }

    public int getRawMin() {
        return rawMin;
    }

    public int getRawMax() {
        return rawMax;
    }

    public Long getDecisionSequenceId() {
        return continuation == null ? null : continuation.getDecisionSequenceId();
    }

    public Integer getSubdecisionIndex() {
        return subdecisionIndex;
    }
}
