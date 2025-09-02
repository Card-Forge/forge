package forge.game.event;

import forge.game.player.Player;

/** 
 * 
 *
 */
public record GameEventPlayerPoisoned(Player receiver, Player source, int oldValue, int amount) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
