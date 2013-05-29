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
    public <T, U> U visit(IGameEventVisitor<T, U> visitor, T params) {
        return visitor.visit(this, params);
    }
}
