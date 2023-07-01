package forge.game.event;

import forge.game.card.Card;

import java.util.Arrays;
import java.util.Collection;

public class GameEventCardRegenerated extends GameEvent {

    public final Collection<Card> cards;
    public GameEventCardRegenerated(Card affected) {
        cards = Arrays.asList(affected);
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
