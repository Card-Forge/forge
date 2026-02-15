package forge.game.event;

import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.util.TextUtil;

public record GameEventPlayerPriority(PlayerView turn, PhaseType phase, PlayerView priority) implements GameEvent {

    public GameEventPlayerPriority(Player turn, PhaseType phase, Player priority) {
        this(PlayerView.get(turn), phase, PlayerView.get(priority));
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return TextUtil.concatWithSpace("Priority -", priority.toString());
    }
}
