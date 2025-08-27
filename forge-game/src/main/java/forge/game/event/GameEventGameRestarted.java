package forge.game.event;

import forge.game.player.Player;

public record GameEventGameRestarted(Player whoRestarted) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
