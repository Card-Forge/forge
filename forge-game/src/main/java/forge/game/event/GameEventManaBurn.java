package forge.game.event;

import forge.game.player.Player;

// This special event denotes loss of mana due to phase end
public class GameEventManaBurn extends GameEvent {

    public final Player player;
    public final boolean causedLifeLoss;
    public final int amount;

    /**
     * TODO: Write javadoc for Constructor.
     * @param dealDamage 
     * @param burn 
     */
    public GameEventManaBurn(Player who, int burn, boolean dealDamage) {
        player = who;
    	amount = burn;
        causedLifeLoss = dealDamage;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
