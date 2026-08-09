package forge.game.decision;

import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/** Immutable, player-safe metadata for one atomic TARGET request. */
public final class TargetDecisionContext {
    private final int choosingPlayerId;
    private final int targetGroupIndex;
    private final int selectedTargetCount;
    private final int minTargets;
    private final int maxTargets;
    private final Long decisionSequenceId;
    private final Integer subdecisionIndex;
    private final Player choosingPlayer;
    private final ActionContinuation continuation;
    private final SpellAbility ability;

    TargetDecisionContext(final SpellAbility ability, final Player choosingPlayer, final int targetGroupIndex,
            final int selectedTargetCount, final int minTargets, final int maxTargets,
            final ActionContinuation continuation, final Integer subdecisionIndex) {
        this.ability = ability;
        this.choosingPlayer = choosingPlayer;
        this.choosingPlayerId = choosingPlayer.getId();
        this.targetGroupIndex = targetGroupIndex;
        this.selectedTargetCount = selectedTargetCount;
        this.minTargets = minTargets;
        this.maxTargets = maxTargets;
        this.continuation = continuation;
        this.decisionSequenceId = continuation == null ? null : continuation.getDecisionSequenceId();
        this.subdecisionIndex = subdecisionIndex;
    }

    public int getChoosingPlayerId() {
        return choosingPlayerId;
    }

    public int getTargetGroupIndex() {
        return targetGroupIndex;
    }

    public int getSelectedTargetCount() {
        return selectedTargetCount;
    }

    public int getMinTargets() {
        return minTargets;
    }

    public int getMaxTargets() {
        return maxTargets;
    }

    /** Returns {@code null} when the target operation is not inside a priority-action continuation. */
    public Long getDecisionSequenceId() {
        return decisionSequenceId;
    }

    /** Returns {@code null} when the target operation is not inside a priority-action continuation. */
    public Integer getSubdecisionIndex() {
        return subdecisionIndex;
    }

    public boolean hasActionContinuation() {
        return continuation != null;
    }

    Player getChoosingPlayer() {
        return choosingPlayer;
    }

    ActionContinuation getContinuation() {
        return continuation;
    }

    SpellAbility getAbility() {
        return ability;
    }
}
