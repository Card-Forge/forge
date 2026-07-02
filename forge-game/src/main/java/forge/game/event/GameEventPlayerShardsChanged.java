package forge.game.event;

import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.util.Lang;
import forge.util.TextUtil;

public record GameEventPlayerShardsChanged(PlayerView player, int oldShards, int newShards) implements GameEvent {

    public GameEventPlayerShardsChanged(Player player, int oldShards, int newShards) {
        this(PlayerView.get(player), oldShards, newShards);
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
