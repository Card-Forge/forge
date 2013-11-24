package forge.game.event;

import forge.game.card.Card;

public class GameEventCardDamaged extends GameEvent {
    
    public enum DamageType {
        Normal, 
        M1M1Counters, 
        Deathtouch, 
        LoyaltyLoss;
    }

    public final Card card;
    public final Card source;
    public final int amount;
    public final DamageType type;

    public GameEventCardDamaged(Card card, Card src, int damageToAdd, DamageType damageType) {
        this.card = card;
        source = src;
        amount = damageToAdd;
        type = damageType;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
