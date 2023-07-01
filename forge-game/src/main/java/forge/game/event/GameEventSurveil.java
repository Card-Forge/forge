package forge.game.event;

import forge.game.player.Player;

public class GameEventSurveil extends GameEvent {

    public final Player player;
    public final int toLibrary, toGraveyard;

    public GameEventSurveil(Player player, int toLibrary, int toGraveyard) {
        this.player = player;
        this.toLibrary = toLibrary;
        this.toGraveyard = toGraveyard;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
