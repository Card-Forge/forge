package forge.game.event;

import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.util.Lang;
import forge.util.TextUtil;

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
        return TextUtil.concatWithSpace(Lang.getInstance().getPossesive(player.getName()),"mana pool",  mode.toString(), "-", TextUtil.addSuffix(mana.toString()," "));
    }
}
