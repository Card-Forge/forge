package forge.game.event;

import com.google.common.collect.Iterables;
import forge.game.card.Card;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;

/**
 * This means card's characteristics have changed on server, clients must re-request them
 */
public class GameEventCardStatsChanged extends GameEvent {

    public final Collection<Card> cards;
    public GameEventCardStatsChanged(Card affected) {
        cards = Arrays.asList(affected);
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
        if ( null == card )
            return "Card state changes: (empty list)";
        if( cards.size() == 1) 
            return String.format("Card state changes: %s (%s) %d/%d", card.getName(), StringUtils.join(card.getType(), ' '), card.getNetPower(), card.getNetToughness() );
        else
            return String.format("Card state changes: %s (%s) %d/%d and %d more", card.getName(), StringUtils.join(card.getType(), ' '), card.getNetPower(), card.getNetToughness(), cards.size() - 1 );
    }

}
