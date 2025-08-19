package forge.game.event;

import forge.game.card.Card;

public class GameEventCardSacrificed extends GameEvent {
    public final Card card;

    /**
     * TODO: Write javadoc for Constructor.
     * @param card
     */
    public GameEventCardSacrificed(Card card) {
        this.card = card;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + card.getController() + " sacrificed " + card;
    }
}
