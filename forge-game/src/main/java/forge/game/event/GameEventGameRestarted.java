package forge.game.event;

import forge.game.player.PlayerView;

public record GameEventGameRestarted(PlayerView whoRestarted) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
