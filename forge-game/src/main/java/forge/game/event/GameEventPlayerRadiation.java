package forge.game.event;

import forge.game.card.Card;
import forge.game.player.Player;

public class GameEventPlayerRadiation extends GameEvent {
    public final Player receiver;
    public final Card source;
    public final int change;

    public GameEventPlayerRadiation(Player recv, Card src, int chng) {
        receiver = recv;
        source = src;
        change = chng;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
