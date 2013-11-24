package forge.game.event;

import forge.game.card.Card;
import forge.game.player.Player;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventPlayerDamaged extends GameEvent {
    public final Player target;
    public final Card source;
    public final int amount;
    final public boolean infect;
    public final boolean combat;

    /**
     * TODO: Write javadoc for Constructor.
     * @param player
     * @param source
     * @param amount
     * @param isCombat
     * @param infect
     */
    public GameEventPlayerDamaged(Player player, Card source, int amount, boolean isCombat, boolean infect) {
        target = player;
        this.source = source;
        this.amount = amount;
        combat = isCombat;
        this.infect = infect;
    }

    /* (non-Javadoc)
     * @see forge.game.event.GameEvent#visit(forge.game.event.IGameEventVisitor)
     */
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
