package forge.game.event;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;

import forge.game.card.Card;

/**
 * This means card's characteristics have changed on server, clients must re-request them
 */
public class GameEventCardStatsChanged extends GameEvent {

    public final Collection<Card> cards;
    public boolean transform = false;
    public GameEventCardStatsChanged(Card affected) {
        this(affected, false);
    }

    public GameEventCardStatsChanged(Card affected, boolean isTransform) {
        cards = Arrays.asList(affected);
        //the transform should only fire once so the flip effect sound will trigger once every transformation...
        // disable for now
        transform = false;
    }

    public GameEventCardStatsChanged(Collection<Card> affected) {
        cards = affected;
    }

    /* (non-Javadoc)
     * @see forge.game.event.GameEvent#visit(forge.game.event.IGameEventVisitor)
     */
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        // TODO Auto-generated method stub
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        Card card = Iterables.getFirst(cards, null);
        if (null == card)
            return "Card state changes: (empty list)";
        if (cards.size() == 1) 
            return "Card state changes: " + card.getName() +
                  " (" + StringUtils.join(card.getType(), ' ') + ") " +
                  card.getNetPower() + "/" + card.getNetToughness();
        else
            return "Card state changes: " + card.getName() +
                  " (" + StringUtils.join(card.getType(), ' ') + ") " +
                  card.getNetPower() + "/" + card.getNetToughness() +
                  " and " + (cards.size() - 1) + " more";
    }

}
