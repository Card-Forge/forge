package forge.game.event;

import forge.game.player.Player;

public class GameEventCardModeChosen extends GameEvent {

    public final Player player;
    public final String cardName;
    public final String mode;
    public final boolean log;
    public final boolean random;

    public GameEventCardModeChosen(Player player, String cardName, String mode, boolean log, boolean random) {
        this.player = player;
        this.cardName = cardName;
        this.mode = mode;
        this.log = log;
        this.random = random;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
