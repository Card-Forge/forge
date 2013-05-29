package forge.game.event;

import forge.Card;
import forge.game.player.Player;

public class GameEventLandPlayed extends GameEvent {

    public final Player Player;
    public final Card Land;

    public GameEventLandPlayed(Player player, Card land) {
        Player = player;
        Land = land;

    }

    
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
