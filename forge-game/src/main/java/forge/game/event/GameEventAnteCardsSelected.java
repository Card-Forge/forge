package forge.game.event;

import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;

public record GameEventAnteCardsSelected(Multimap<PlayerView, CardView> cards) implements GameEvent {

    public static GameEventAnteCardsSelected fromCards(Multimap<Player, Card> cards) {
        return new GameEventAnteCardsSelected(convertMap(cards));
    }

    private static Multimap<PlayerView, CardView> convertMap(Multimap<Player, Card> map) {
        Multimap<PlayerView, CardView> result = HashMultimap.create();
        for (Map.Entry<Player, Card> entry : map.entries()) {
            result.put(PlayerView.get(entry.getKey()), CardView.get(entry.getValue()));
        }
        return result;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
