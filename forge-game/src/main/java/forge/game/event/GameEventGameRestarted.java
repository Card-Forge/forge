package forge.game.event;

import forge.game.player.Player;
import forge.game.player.PlayerView;

public record GameEventGameRestarted(PlayerView whoRestarted) implements GameEvent {

    public GameEventGameRestarted(Player whoRestarted) {
        this(PlayerView.get(whoRestarted));
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
