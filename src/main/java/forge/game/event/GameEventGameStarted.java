package forge.game.event;

import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventGameStarted extends GameEvent {

    public final Player firstTurn;
    public final Iterable<Player> players;

    public GameEventGameStarted(Player firstTurn, Iterable<Player> players) {
        super();
        this.firstTurn = firstTurn;
        this.players = players;
    }

    
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
