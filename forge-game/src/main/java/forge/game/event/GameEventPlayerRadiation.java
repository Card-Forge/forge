package forge.game.event;

import forge.game.player.Player;

public record GameEventPlayerRadiation(Player receiver, Player source, int change) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
