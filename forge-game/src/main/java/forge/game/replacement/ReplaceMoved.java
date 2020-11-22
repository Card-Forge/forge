package forge.game.replacement;

import forge.game.ability.AbilityKey;
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
    public boolean canReplace(Map<AbilityKey, Object> runParams) {

        if (hasParam("ValidCard")) {
            if (!matchesValid(runParams.get(AbilityKey.Affected), getParam("ValidCard").split(","), getHostCard())) {
                return false;
            }
        }

        if (hasParam("ValidLKI")) {
            if (!matchesValid(runParams.get(AbilityKey.CardLKI), getParam("ValidLKI").split(","), getHostCard())) {
                return false;
            }
        }

        if (hasParam("Origin")) {
            ZoneType zt = (ZoneType) runParams.get(AbilityKey.Origin);
            if (!ZoneType.listValueOf(getParam("Origin")).contains(zt)) {
                return false;
            }
        }        

        if (hasParam("Destination")) {
            ZoneType zt = (ZoneType) runParams.get(AbilityKey.Destination);
            if (!ZoneType.listValueOf(getParam("Destination")).contains(zt)) {
                return false;
            }
        }
        
        if (hasParam("ExcludeDestination")) {
            ZoneType zt = (ZoneType) runParams.get(AbilityKey.Destination);
            if (ZoneType.listValueOf(getParam("ExcludeDestination")).contains(zt)) {
                return false;
            }
        }
        
        if (hasParam("Fizzle")) {
            // if Replacement look for Fizzle
            if (!runParams.containsKey(AbilityKey.Fizzle)) {
                return false;
            }
            Boolean val = (Boolean) runParams.get(AbilityKey.Fizzle);
            if ("True".equals(getParam("Fizzle")) != val) {
                return false;
            }
        }
        
        if (hasParam("ValidStackSa")) {
            if (!matchesValid(runParams.get(AbilityKey.StackSa), getParam("ValidStackSa").split(","), getHostCard())) {
                return false;
            }
        }

        if (hasParam("Cause")) {
            if (!matchesValid(runParams.get(AbilityKey.Cause), getParam("Cause").split(","), getHostCard())) {
                return false;
            }
        }

        if (hasParam("NotCause")) {
            if (matchesValid(runParams.get(AbilityKey.Cause), getParam("NotCause").split(","), getHostCard())) {
                return false;
            }
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#setReplacingObjects(java.util.HashMap, forge.card.spellability.SpellAbility)
     */
    @Override
    public void setReplacingObjects(Map<AbilityKey, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject(AbilityKey.Card, runParams.get(AbilityKey.Affected));
        sa.setReplacingObject(AbilityKey.CardLKI, runParams.get(AbilityKey.CardLKI));
        sa.setReplacingObject(AbilityKey.Cause, runParams.get(AbilityKey.Cause));
    }

}
