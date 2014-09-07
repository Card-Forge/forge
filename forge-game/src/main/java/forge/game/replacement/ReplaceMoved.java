package forge.game.replacement;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Map;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceMoved extends ReplacementEffect {

    /**
     * 
     * TODO: Write javadoc for Constructor.
     * @param mapParams &emsp; HashMap<String, String>
     * @param host &emsp; Card
     */
    public ReplaceMoved(final Map<String, String> mapParams, final Card host, final boolean intrinsic) {
        super(mapParams, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.HashMap)
     */
    @Override
    public boolean canReplace(Map<String, Object> runParams) {
        if (!runParams.get("Event").equals("Moved")) {
            return false;
        }
        if (this.getMapParams().containsKey("ValidCard")) {
            if (!matchesValid(runParams.get("Affected"), this.getMapParams().get("ValidCard").split(","), this.getHostCard())) {
                return false;
            }
        }
        
        boolean matchedZone = false;
        if (this.getMapParams().containsKey("Origin")) {
            for(ZoneType z : ZoneType.listValueOf(this.getMapParams().get("Origin"))) {
                if(z == (ZoneType) runParams.get("Origin"))
                    matchedZone =  true;
            }
            
            if(!matchedZone)
            {
                return false;
            }
        }        
        
        if (this.getMapParams().containsKey("Destination")) {
            matchedZone = false;
            for(ZoneType z : ZoneType.listValueOf(this.getMapParams().get("Destination"))) {
                if(z == (ZoneType) runParams.get("Destination"))
                    matchedZone =  true;
            }
            
            if(!matchedZone)
            {
                return false;
            }
        }
        
        
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#setReplacingObjects(java.util.HashMap, forge.card.spellability.SpellAbility)
     */
    @Override
    public void setReplacingObjects(Map<String, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject("Card", runParams.get("Affected"));
        sa.setReplacingObject("CardLKI", runParams.get("CardLKI"));
    }

}
