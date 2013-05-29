package forge.game.event;

import forge.Card;

public class GameEventCardTapped extends GameEvent {
    public final boolean tapped;
    public final Card card;

    public GameEventCardTapped(final Card card, final boolean tapped) {
        this.tapped = tapped;
        this.card = card;
    }
    
    
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
