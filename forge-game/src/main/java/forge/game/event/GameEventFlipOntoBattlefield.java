package forge.game.event;

import forge.game.card.CardView;
import java.util.List;

public record GameEventFlipOntoBattlefield(CardView flipped, CardView target, List<CardView> hit, int timesFlipped, boolean finished) implements GameEvent {
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) { return visitor.visit(this); }
}