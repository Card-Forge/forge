package forge.game.event;

import com.google.common.collect.Multimap;

import forge.game.card.Card;
import forge.game.player.Player;

public class GameEventAnteCardsSelected extends GameEvent {
    public final Multimap<Player, Card> cards;
    public GameEventAnteCardsSelected(Multimap<Player, Card> list) {
        cards = list;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}