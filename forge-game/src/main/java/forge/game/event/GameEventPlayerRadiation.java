package forge.game.event;

import forge.game.player.Player;

public class GameEventPlayerRadiation extends GameEvent {
    public final Player receiver;
    public final Player source;
    public final int change;

    public GameEventPlayerRadiation(Player recv, Player src, int chng) {
        receiver = recv;
        source = src;
        change = chng;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
