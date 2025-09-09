package forge.game.event;

import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.util.Lang;
import forge.util.TextUtil;

public record GameEventTurnPhase(Player playerTurn, PhaseType phase, String phaseDesc) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        String playerName = Lang.getInstance().getPossesive(playerTurn.getName());
        return TextUtil.concatWithSpace(playerName,"turn,", phaseDesc+phase.nameForUi, "phase");
    }
}
