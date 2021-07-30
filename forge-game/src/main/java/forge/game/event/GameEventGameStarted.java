package forge.game.event;

import forge.game.GameType;
import forge.game.player.Player;
import forge.util.Lang;
import forge.util.TextUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventGameStarted extends GameEvent {

    public final Player firstTurn;
    public final Iterable<Player> players;
    public final GameType gameType;

    public GameEventGameStarted(GameType type, Player firstTurn, Iterable<Player> players) {
        super();
        this.gameType = type;
        this.firstTurn = firstTurn;
        this.players = players;
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
        return TextUtil.concatWithSpace(gameType.toString(),"game between", Lang.joinHomogenous(players), "started.", firstTurn.toString(), "goes first ");
    }

}
