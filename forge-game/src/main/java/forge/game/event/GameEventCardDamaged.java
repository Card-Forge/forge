package forge.game.event;

import forge.game.card.CardView;

public record GameEventCardDamaged(CardView card, CardView source, int amount, DamageType type) implements GameEvent {

    public enum DamageType {
        Normal, 
        M1M1Counters, 
        Deathtouch, 
        LoyaltyLoss
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
        return "" + source + " dealt " + amount + " " + type + " damage to " + card;
    }
}
