package forge.game.event;

import forge.game.player.Player;

public class GameEventSpeedUp extends GameEvent {

    public final Player player;

    public GameEventSpeedUp(Player affected) {
        player = affected;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
