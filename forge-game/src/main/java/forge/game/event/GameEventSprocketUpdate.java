package forge.game.event;

import forge.game.card.Card;

public record GameEventSprocketUpdate(Card contraption, int oldSprocket, int sprocket) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
