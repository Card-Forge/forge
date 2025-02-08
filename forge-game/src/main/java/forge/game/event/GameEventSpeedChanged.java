package forge.game.event;

import forge.game.player.Player;

public class GameEventSpeedChanged extends GameEvent {

    public final Player player;
    public final int deltaSpeed;

    public GameEventSpeedChanged(Player affected, int deltaSpeed) {
        this.player = affected;
        this.deltaSpeed = deltaSpeed;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
