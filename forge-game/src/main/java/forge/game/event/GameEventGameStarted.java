package forge.game.event;

import forge.game.GameType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.util.Lang;
import forge.util.TextUtil;

public record GameEventGameStarted(GameType gameType, PlayerView firstTurn, Iterable<PlayerView> players) implements GameEvent {

    public GameEventGameStarted(GameType gameType, Player firstTurn, Iterable<Player> players) {
        this(gameType, PlayerView.get(firstTurn), PlayerView.getCollection(players));
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
