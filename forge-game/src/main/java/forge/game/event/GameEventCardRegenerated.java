package forge.game.event;

import forge.game.card.Card;
import forge.game.card.CardView;

import java.util.Collection;
import java.util.Collections;

public record GameEventCardRegenerated(Collection<CardView> cards) implements GameEvent {

    public GameEventCardRegenerated(Card affected) {
        this(Collections.singletonList(CardView.get(affected)));
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
