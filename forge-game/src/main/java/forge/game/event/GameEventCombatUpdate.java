package forge.game.event;

import java.util.List;

import forge.game.card.Card;

public record GameEventCombatUpdate(List<Card> attackers, List<Card> blockers) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
