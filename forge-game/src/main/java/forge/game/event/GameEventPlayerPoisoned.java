package forge.game.event;

import forge.game.player.Player;
import forge.game.player.PlayerView;

/**
 *
 *
 */
public record GameEventPlayerPoisoned(PlayerView receiver, PlayerView source, int oldValue, int amount) implements GameEvent {

    public GameEventPlayerPoisoned(Player receiver, Player source, int oldValue, int amount) {
        this(PlayerView.get(receiver), PlayerView.get(source), oldValue, amount);
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
