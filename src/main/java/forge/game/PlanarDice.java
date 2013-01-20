package forge.game;

import java.util.HashMap;
import java.util.TreeMap;

import forge.Singletons;
import forge.card.trigger.TriggerType;
import forge.game.player.Player;

/** 
 * Represents the planar dice for Planechase games.
 *
 */
public enum PlanarDice {
    Planeswalk,
    Chaos,
    Blank;
    
    public static PlanarDice roll(Player roller)
    {
        PlanarDice res = Blank;
        int i = forge.util.MyRandom.getRandom().nextInt(6);
        if (i == 0)
            res = Planeswalk;
        if (i == 1)
            res = Chaos;
        
        HashMap<String,Object> runParams = new HashMap<String,Object>();
        runParams.put("Player", roller);
        runParams.put("Result", res);
        Singletons.getModel().getGame().getTriggerHandler().runTrigger(TriggerType.PlanarDice, runParams,false);
    
        
        return res;
    }
    
    /**
     * Parses a string into an enum member.
     * @param string to parse
     * @return enum equivalent
     */
    public static PlanarDice smartValueOf(String value) {

        final String valToCompate = value.trim();
        for (final PlanarDice v : PlanarDice.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        throw new RuntimeException("Element " + value + " not found in TriggerType enum");
    }
}
