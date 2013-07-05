package forge.game.event;

import org.apache.commons.lang.StringUtils;

import forge.Card;

/**
 * This means card's characteristics have changed on server, clients must re-request them
 */
public class GameEventCardStatsChanged extends GameEvent {

    public final Card card;
    public GameEventCardStatsChanged(Card affected) {
        card = affected;
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
        return String.format("Card state changes: %s (%s) %d/%d", card.getName(), StringUtils.join(card.getType(), ' '), card.getNetAttack(), card.getNetDefense() );
    }

}
