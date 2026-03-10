package forge.game.event;

import forge.game.player.Player;
import forge.game.player.PlayerView;

public record GameEventPlayerRadiation(PlayerView receiver, PlayerView source, int change) implements GameEvent {

    public GameEventPlayerRadiation(Player receiver, Player source, int change) {
        this(PlayerView.get(receiver), PlayerView.get(source), change);
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
