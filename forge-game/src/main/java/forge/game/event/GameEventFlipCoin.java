package forge.game.event;

import forge.game.player.PlayerView;

public record GameEventFlipCoin(PlayerView flipper, boolean heads, boolean startingToss) implements GameEvent {
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return flipper + (heads ? " flipped heads" : " flipped tails");
    }
}