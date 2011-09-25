package forge.card.staticAbility;

import java.util.HashMap;

import forge.Card;
import forge.GameEntity;

public class StaticAbility_PreventDamage {

    /**
     * 
     * TODO Write javadoc for this method.
     * @param stAb a StaticAbility
     */
    public static int applyPreventDamageAbility(final StaticAbility stAb, Card source, GameEntity target, int damage) {
        HashMap<String, String> params = stAb.getMapParams();
        Card hostCard = stAb.getHostCard();
        int restDamage = damage;
        
        if(params.containsKey("Source") && !source.isValid(params.get("Source"), hostCard.getController(), hostCard)) {
            return restDamage;
        }
        
        if(params.containsKey("Target") && !target.isValid(params.get("Target"), hostCard.getController(), hostCard)) {
            return restDamage;
        }
        
        if(!params.containsKey("Amount") || params.get("Amount").equals("All")) {
            return 0;
        }
        
        return restDamage;
    }

}
