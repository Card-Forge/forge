package forge.game.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import forge.game.GameEntity;
import forge.game.GameEntityView;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.util.Lang;
import forge.util.TextUtil;

public record GameEventBlockersDeclared(PlayerView defendingPlayer, Map<GameEntityView, Multimap<CardView, CardView>> blockers) implements GameEvent {

    public GameEventBlockersDeclared(Player defendingPlayer, Map<GameEntity, Multimap<Card, Card>> blockers) {
        this(PlayerView.get(defendingPlayer), convertMap(blockers));
    }

    private static Map<GameEntityView, Multimap<CardView, CardView>> convertMap(Map<GameEntity, Multimap<Card, Card>> map) {
        Map<GameEntityView, Multimap<CardView, CardView>> result = new HashMap<>();
        for (Map.Entry<GameEntity, Multimap<Card, Card>> entry : map.entrySet()) {
            Multimap<CardView, CardView> innerResult = HashMultimap.create();
            for (Map.Entry<Card, Card> innerEntry : entry.getValue().entries()) {
                innerResult.put(CardView.get(innerEntry.getKey()), CardView.get(innerEntry.getValue()));
            }
            result.put(GameEntityView.get(entry.getKey()), innerResult);
        }
        return result;
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
        List<CardView> blockerCards = new ArrayList<>();
        for (Multimap<CardView, CardView> vv : blockers.values()) {
            blockerCards.addAll(vv.values());
        }
        return TextUtil.concatWithSpace(defendingPlayer.getName(), "declared", String.valueOf(blockerCards.size()), "blockers:", Lang.joinHomogenous(blockerCards));
    }
}
