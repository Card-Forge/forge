package forge.game.event;

import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.util.TextUtil;

public record GameEventPlayerPriority(Player turn, PhaseType phase, Player priority) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return TextUtil.concatWithSpace("Priority -", priority.getName());
    }
}
