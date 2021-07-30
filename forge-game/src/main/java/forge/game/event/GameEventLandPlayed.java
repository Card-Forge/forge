package forge.game.event;

import forge.game.card.Card;
import forge.game.player.Player;

public class GameEventLandPlayed extends GameEvent {

    public final Player player;
    public final Card land;

    public GameEventLandPlayed(Player player, Card land) {
        this.player = player;
        this.land = land;

    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
