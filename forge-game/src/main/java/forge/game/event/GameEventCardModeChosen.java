package forge.game.event;

import forge.game.player.Player;
import forge.game.player.PlayerView;

public record GameEventCardModeChosen(PlayerView player, String cardName, String mode, boolean log, boolean random) implements GameEvent {

    public GameEventCardModeChosen(Player player, String cardName, String mode, boolean log, boolean random) {
        this(PlayerView.get(player), cardName, mode, log, random);
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
