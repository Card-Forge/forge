package forge.game.event;

import forge.game.card.Card;

import java.util.Collection;
import java.util.List;

public class GameEventRemoveSummoningSickness extends GameEvent {

    public final Collection<Card> cards;

    public GameEventRemoveSummoningSickness(List<Card> affected) {
        cards = affected;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
