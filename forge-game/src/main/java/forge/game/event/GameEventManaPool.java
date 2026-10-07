package forge.game.event;

import java.util.Set;

import forge.card.MagicColor;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.util.Lang;

public record GameEventManaPool(PlayerView player, EventValueChangeType mode, Set<MagicColor.Color> colors) implements GameEvent {

    public GameEventManaPool(Player player, EventValueChangeType mode, Set<MagicColor.Color> colors) {
        this(PlayerView.get(player), mode, colors);
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
            sb.append(" - ").append(colors);
            break;
        default:
            break;
        
        }
        return sb.toString();
    }
}
