package forge.game.event;

import forge.game.card.Card;

import java.util.Arrays;
import java.util.Collection;

public record GameEventCardRegenerated(Collection<Card> cards) implements GameEvent {
    public GameEventCardRegenerated(Card affected) {
        this(Arrays.asList(affected));
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
