package forge.game.event;

import forge.game.card.Card;
import forge.game.card.CardCollectionView;

public record GameEventFlipOntoBattlefield(Card flipped, Card target, CardCollectionView hit, int timesFlipped, boolean finished) implements GameEvent {
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) { return visitor.visit(this); }
}