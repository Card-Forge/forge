package forge.game.event;

import com.google.common.collect.Multimap;

import forge.game.card.Card;
import forge.game.player.Player;

public record GameEventAnteCardsSelected(Multimap<Player, Card> cards) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}