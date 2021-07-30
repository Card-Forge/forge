package forge.game.event;

import forge.game.player.Player;
import forge.util.Lang;
import forge.util.TextUtil;

public class GameEventPlayerLivesChanged extends GameEvent {
    public final Player player;
    public final int oldLives;
    public final int newLives;

    public GameEventPlayerLivesChanged(Player who, int oldValue, int newValue) {
        player = who;
        oldLives = oldValue;
        newLives = newValue;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return TextUtil.concatWithSpace(Lang.getInstance().getPossesive(player.getName()),"lives changed:",  String.valueOf(oldLives),"->", String.valueOf(newLives));
    }
}
