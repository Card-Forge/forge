package forge.game.event;

import forge.card.CardStateName;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.util.Lang;

public record GameEventDoorChanged(Player activatingPlayer, Card card, CardStateName state, boolean unlock) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        String doorName = card.getState(state).getTranslatedName();

        StringBuilder sb = new StringBuilder();
        sb.append(activatingPlayer);
        sb.append(" ");
        sb.append(unlock ? "unlocks" : "locks");
        sb.append(" ");
        sb.append(Lang.getInstance().getPossessedObject(doorName, "Door"));
        return sb.toString();
    }
}
