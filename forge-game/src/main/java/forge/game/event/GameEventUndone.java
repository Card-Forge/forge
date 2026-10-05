package forge.game.event;

import java.util.Collection;

import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.util.TextUtil;

/** The game was put back to an earlier point; everything shown has to be redrawn. */
public record GameEventUndone(PlayerView undoingPlayer, PlayerView playerTurn, PhaseType phase,
                              Collection<PlayerView> players, Collection<CardView> cards) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return TextUtil.concatWithSpace(String.valueOf(undoingPlayer), "undid their last action");
    }
}
