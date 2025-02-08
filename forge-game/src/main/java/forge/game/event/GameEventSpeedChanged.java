package forge.game.event;

import forge.game.player.Player;

public class GameEventSpeedChanged extends GameEvent {

    public final Player player;
    public final int oldValue;
    public final int newValue;

    public GameEventSpeedChanged(Player affected, int oldValue, int newValue) {
        player = affected;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
