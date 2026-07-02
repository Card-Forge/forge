package forge.game.event;

import java.util.List;
import java.util.stream.Collectors;

import forge.game.card.Card;
import forge.game.card.CardView;

public record GameEventCombatUpdate(List<CardView> attackers, List<CardView> blockers) implements GameEvent {

    public static GameEventCombatUpdate fromCards(List<Card> attackers, List<Card> blockers) {
        return new GameEventCombatUpdate(
            attackers == null ? null : attackers.stream().map(CardView::get).collect(Collectors.toList()),
            blockers == null ? null : blockers.stream().map(CardView::get).collect(Collectors.toList())
        );
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
