package forge.game.event;

import forge.game.player.Player;
import forge.util.Lang;
import forge.util.TextUtil;

public class GameEventPlayerShardsChanged extends GameEvent {
    public final Player player;
    public final int oldShards;
    public final int newShards;

    public GameEventPlayerShardsChanged(Player who, int oldValue, int newValue) {
        player = who;
        oldShards = oldValue;
        newShards = newValue;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return TextUtil.concatWithSpace(Lang.getInstance().getPossesive(player.getName()),"shards changed:",  String.valueOf(oldShards),"->", String.valueOf(newShards));
    }
}
