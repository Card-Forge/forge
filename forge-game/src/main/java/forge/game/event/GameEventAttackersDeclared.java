package forge.game.event;

import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import forge.game.GameEntity;
import forge.game.GameEntityView;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;

public record GameEventAttackersDeclared(PlayerView player, Multimap<GameEntityView, CardView> attackersMap) implements GameEvent {

    public GameEventAttackersDeclared(Player player, Multimap<GameEntity, Card> attackersMap) {
        this(PlayerView.get(player), convertMap(attackersMap));
    }

    private static Multimap<GameEntityView, CardView> convertMap(Multimap<GameEntity, Card> map) {
        Multimap<GameEntityView, CardView> result = HashMultimap.create();
        for (Map.Entry<GameEntity, Card> entry : map.entries()) {
            result.put(GameEntityView.get(entry.getKey()), CardView.get(entry.getValue()));
        }
        return result;
    }

    /* (non-Javadoc)
     * @see forge.game.event.GameEvent#visit(forge.game.event.IGameEventVisitor)
     */
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + player + " declared attackers: " + attackersMap;
    }
}
