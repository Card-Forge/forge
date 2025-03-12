package forge.game.event;

import forge.card.CardStateName;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.util.CardTranslation;
import forge.util.Lang;

public class GameEventDoorChanged extends GameEvent {
    public final Player activatingPlayer;
    public final Card card;
    public final CardStateName state;
    public boolean unlock;

    public GameEventDoorChanged(Player player, Card c, CardStateName state, boolean unlock) {
        activatingPlayer = player;
        card = c;
        this.state = state;
        this.unlock = unlock;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        String doorName = CardTranslation.getTranslatedName(card.getState(state));

        StringBuilder sb = new StringBuilder();
        sb.append(activatingPlayer);
        sb.append(" ");
        sb.append(unlock ? "unlocks" : "locks");
        sb.append(" ");
        sb.append(Lang.getInstance().getPossessedObject(doorName, "Door"));
        return sb.toString();
    }
}
