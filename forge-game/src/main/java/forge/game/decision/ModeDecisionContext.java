package forge.game.decision;

import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;

import java.util.List;
import java.util.Objects;

/** Callback-local metadata for one ordinary Forge Charm mode selection. */
public final class ModeDecisionContext {
    private final SpellAbility ability;
    private final List<AbilitySub> possible;
    private final Player choosingPlayer;
    private final int min;
    private final int max;
    private final boolean allowRepeat;
    private final ActionContinuation continuation;
    private final Integer subdecisionIndex;

    ModeDecisionContext(final SpellAbility ability, final List<AbilitySub> possible, final Player choosingPlayer,
            final int min, final int max, final boolean allowRepeat, final ActionContinuation continuation,
            final Integer subdecisionIndex) {
        this.ability = Objects.requireNonNull(ability);
        this.possible = List.copyOf(possible);
        this.choosingPlayer = Objects.requireNonNull(choosingPlayer);
        this.min = min;
        this.max = max;
        this.allowRepeat = allowRepeat;
        this.continuation = continuation;
        this.subdecisionIndex = subdecisionIndex;
    }

    public int getChoosingPlayerId() {
        return choosingPlayer.getId();
    }

    public int getActivatingPlayerId() {
        return ability.getActivatingPlayer().getId();
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public boolean isAllowRepeat() {
        return allowRepeat;
    }

    public Long getDecisionSequenceId() {
        return continuation == null ? null : continuation.getDecisionSequenceId();
    }

    public Integer getSubdecisionIndex() {
        return subdecisionIndex;
    }

    SpellAbility getAbility() {
        return ability;
    }

    List<AbilitySub> getPossible() {
        return possible;
    }

    Player getChoosingPlayer() {
        return choosingPlayer;
    }

    ActionContinuation getContinuation() {
        return continuation;
    }
}
