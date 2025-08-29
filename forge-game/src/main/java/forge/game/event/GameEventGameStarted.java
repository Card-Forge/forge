package forge.game.event;

import forge.game.GameType;
import forge.game.player.Player;
import forge.util.Lang;
import forge.util.TextUtil;

public record GameEventGameStarted(GameType gameType, Player firstTurn, Iterable<Player> players) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return TextUtil.concatWithSpace(gameType.toString(),"game between", Lang.joinHomogenous(players), "started.", firstTurn.toString(), "goes first ");
    }

}
