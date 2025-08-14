package forge.game.event;

import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.util.Lang;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventManaPool extends GameEvent {
    public final Player player;
    public final EventValueChangeType mode;
    public final Mana mana;

    public GameEventManaPool(Player owner, EventValueChangeType changeMode, Mana mana) {
        this.mana = mana;
        player = owner;
        mode = changeMode;
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
        StringBuilder sb = new StringBuilder(Lang.getInstance().getPossessedObject(player.getName(), "mana pool"));
        sb.append(" ").append(mode);
        switch (mode) {
        case Added:
        case Removed:
            sb.append(" - ").append(mana);
            break;
        default:
            break;
        
        }
        return sb.toString();
    }
}
