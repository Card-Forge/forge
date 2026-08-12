package forge.game.event;

import forge.card.CardStateName;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.util.Lang;

public record GameEventDoorChanged(PlayerView activatingPlayer, CardView card, CardStateName state, boolean unlock) implements GameEvent {

    public GameEventDoorChanged(Player activatingPlayer, Card card, CardStateName state, boolean unlock) {
        this(PlayerView.get(activatingPlayer), CardView.get(card), state, unlock);
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        String doorName = card.getCurrentState().getName();

        StringBuilder sb = new StringBuilder();
        sb.append(activatingPlayer);
        sb.append(" ");
        sb.append(unlock ? "unlocks" : "locks");
        sb.append(" ");
        sb.append(Lang.getInstance().getPossessedObject(doorName, "Door"));
        return sb.toString();
    }
}
