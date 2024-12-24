package forge.game.event;

import forge.game.card.Card;

public class GameEventSprocketUpdate extends GameEvent {

    public final Card contraption;
    public final int oldSprocket;
    public final int sprocket;

    public GameEventSprocketUpdate(Card contraption, int oldSprocket, int sprocket) {
        this.contraption = contraption;
        this.oldSprocket = oldSprocket;
        this.sprocket = sprocket;
    }


    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
