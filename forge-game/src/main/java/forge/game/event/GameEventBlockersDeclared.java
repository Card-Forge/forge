package forge.game.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Multimap;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.util.Lang;
import forge.util.TextUtil;

public record GameEventBlockersDeclared(Player defendingPlayer, Map<GameEntity, Multimap<Card, Card>> blockers) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        List<Card> blockerCards = new ArrayList<>();
        for (Multimap<Card, Card> vv : blockers.values()) {
            blockerCards.addAll(vv.values());
        }
        return TextUtil.concatWithSpace(defendingPlayer.getName(),"declared", String.valueOf(blockerCards.size()),"blockers:", Lang.joinHomogenous(blockerCards) );
    }
}
