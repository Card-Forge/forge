package forge.game.event;

import forge.Card;
import forge.game.player.Player;

public class LandPlayedEvent extends Event{

    public final Player Player;
    public final Card Land;

    public LandPlayedEvent(Player player, Card land) {
        Player = player;
        Land = land;
        
    }
    
}