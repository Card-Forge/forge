package forge.game.event;

import forge.game.player.Player;
import forge.util.Lang;
import forge.util.TextUtil;

public record GameEventPlayerLivesChanged(Player player, int oldLives, int newLives) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return TextUtil.concatWithSpace(Lang.getInstance().getPossesive(player.getName()),"lives changed:",  String.valueOf(oldLives),"->", String.valueOf(newLives));
    }
}
