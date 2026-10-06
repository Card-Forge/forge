package forge.game.event;

import java.util.Collection;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.util.TextUtil;

/**
 * The game is being put back to an earlier state: fired with {@code start} before, and without after.
 * Afterwards anything may differ, so the second one carries everything there is to redraw.
 */
public record GameEventSnapshotRestored(boolean start, Collection<PlayerView> players,
                                        Collection<CardView> cards) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        if (start) {
            return TextUtil.concatWithSpace("Undo Snapshot Restoration Started");
        }

        return TextUtil.concatWithSpace("Undo Snapshot Restored");
    }
}
