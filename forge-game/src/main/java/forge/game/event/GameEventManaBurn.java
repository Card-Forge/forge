package forge.game.event;

import forge.game.player.Player;

// This special event denotes loss of mana due to phase end
public record GameEventManaBurn(Player player, boolean causedLifeLoss, int amount) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
