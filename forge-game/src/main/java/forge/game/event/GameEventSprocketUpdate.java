package forge.game.event;

import forge.game.card.Card;
import forge.game.card.CardView;

public record GameEventSprocketUpdate(CardView contraption, int oldSprocket, int sprocket) implements GameEvent {

    public GameEventSprocketUpdate(Card contraption, int oldSprocket, int sprocket) {
        this(CardView.get(contraption), oldSprocket, sprocket);
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
