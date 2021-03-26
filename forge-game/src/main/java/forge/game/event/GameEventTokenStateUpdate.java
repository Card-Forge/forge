package forge.game.event;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import forge.game.card.Card;

public class GameEventTokenStateUpdate extends GameEvent {

    public final Collection<Card> cards;
    public GameEventTokenStateUpdate(Card affected) {
        cards = Arrays.asList(affected);
    }

    public GameEventTokenStateUpdate(List<Card> affected) {
        cards = affected;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
