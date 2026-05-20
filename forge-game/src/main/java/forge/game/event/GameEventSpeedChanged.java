package forge.game.event;

import forge.game.player.Player;
import forge.game.player.PlayerView;

public record GameEventSpeedChanged(PlayerView player, int oldValue, int newValue) implements GameEvent {

    public GameEventSpeedChanged(Player player, int oldValue, int newValue) {
        this(PlayerView.get(player), oldValue, newValue);
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
