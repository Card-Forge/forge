package forge.game.event;

import forge.game.player.Player;

public record GameEventCardModeChosen(Player player, String cardName, String mode, boolean log, boolean random) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
