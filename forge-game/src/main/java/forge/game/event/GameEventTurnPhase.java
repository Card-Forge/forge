package forge.game.event;

import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.util.Lang;
import forge.util.TextUtil;

public record GameEventTurnPhase(PlayerView playerTurn, PhaseType phase, String phaseDesc,
                                 boolean hasTokens, boolean hasCreatures) implements GameEvent {

    public GameEventTurnPhase(PlayerView playerTurn, PhaseType phase, String phaseDesc) {
        this(playerTurn, phase, phaseDesc, false, false);
    }

    public GameEventTurnPhase(Player playerTurn, PhaseType phase, String phaseDesc) {
        this(PlayerView.get(playerTurn), phase, phaseDesc,
                !playerTurn.getTokensInPlay().isEmpty(),
                !playerTurn.getCreaturesInPlay().isEmpty());
    }

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
