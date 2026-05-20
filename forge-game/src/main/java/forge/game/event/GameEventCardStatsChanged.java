package forge.game.event;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.StringUtils;

import forge.game.card.Card;
import forge.game.card.CardView;

/**
 * This means card's characteristics have changed on server, clients must re-request them
 */
public record GameEventCardStatsChanged(Collection<CardView> cards, boolean transform) implements GameEvent {

    public GameEventCardStatsChanged(Card affected) {
        this(affected, false);
    }

    public GameEventCardStatsChanged(Card affected, boolean isTransform) {
        this(CardView.getCollection(Arrays.asList(affected)), false);
        //the transform should only fire once so the flip effect sound will trigger once every transformation...
        // disable for now
    }

    public GameEventCardStatsChanged(Collection<Card> affected) {
        this(CardView.getCollection(affected), false);
    }

    /* (non-Javadoc)
     * @see forge.game.event.GameEvent#visit(forge.game.event.IGameEventVisitor)
     */
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        CardView card = Iterables.getFirst(cards, null);
        if (null == card)
            return "Card state changes: (empty list)";
        if (cards.size() == 1)
            return "Card state changes: " + card.getName() +
                  " (" + StringUtils.join(card.getCurrentState().getType(), ' ') + ") " +
                  card.getCurrentState().getPower() + "/" + card.getCurrentState().getToughness();
        else
            return "Card state changes: " + card.getName() +
                  " (" + StringUtils.join(card.getCurrentState().getType(), ' ') + ") " +
                  card.getCurrentState().getPower() + "/" + card.getCurrentState().getToughness() +
                  " and " + (cards.size() - 1) + " more";
    }

}
