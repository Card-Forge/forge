package forge.card.replacement;

import java.util.HashMap;

import forge.AllZoneUtil;
import forge.Card;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceDamage extends ReplacementEffect {

    /**
     * TODO: Write javadoc for Constructor.
     * @param map
     * @param host
     */
    public ReplaceDamage(HashMap<String, String> map, Card host) {
        super(map, host);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.HashMap)
     */
    @Override
    public boolean canReplace(HashMap<String, Object> runParams) {
        if(!runParams.get("Event").equals("DamageDone")) {
            return false;
        }
        if(mapParams.containsKey("ValidSource")) {
            if(!this.matchesValid(runParams.get("DamageSource"), mapParams.get("ValidSource").split(","), hostCard)) {
                return false;
            }
        }
        if(mapParams.containsKey("ValidTarget")) {
            if(!this.matchesValid(runParams.get("Affected"), mapParams.get("ValidTarget").split(","), hostCard)) {
                return false;
            }
        }
        if(mapParams.containsKey("DamageAmount")) {
            String full = mapParams.get("DamageAmount");
            String operator = full.substring(0,2);
            String operand = full.substring(2);
            int intoperand = 0;
            try {
                intoperand = Integer.parseInt(operand);
            } catch(NumberFormatException e) {
                intoperand = CardFactoryUtil.xCount(hostCard, hostCard.getSVar(operand));
            }
            
            if(!AllZoneUtil.compare((Integer)runParams.get("DamageAmount"), operator, intoperand)) {
                return false;
            }
        }
        if(mapParams.containsKey("IsCombat")) {
            if(mapParams.get("IsCombat").equals("True")) {
                if(!((Boolean)runParams.get("IsCombat"))) {
                    return false;
                }
            }
            else {
                if((Boolean)runParams.get("IsCombat")) {
                    return false;
                }
            }
        }
        
        
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#getCopy()
     */
    @Override
    public ReplacementEffect getCopy() {
        return new ReplaceDamage(this.mapParams,this.hostCard);
    }
    
    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#setReplacingObjects(java.util.HashMap, forge.card.spellability.SpellAbility)
     */
    @Override
    public void setReplacingObjects(HashMap<String,Object> runParams, SpellAbility sa) {
        sa.setReplacingObject("DamageAmount", runParams.get("DamageAmount"));
        sa.setReplacingObject("Target", runParams.get("Affected"));
        sa.setReplacingObject("Source", runParams.get("DamageSource"));
    }

}
