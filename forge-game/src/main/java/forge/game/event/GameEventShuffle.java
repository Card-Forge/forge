package forge.game.event;

import forge.game.player.Player;
import forge.util.Lang;
import forge.util.TextUtil;

public record GameEventShuffle(Player player) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return TextUtil.concatWithSpace(player.toString(), Lang.joinVerb(player.getName(), "shuffle"), "their library");
    }
}
