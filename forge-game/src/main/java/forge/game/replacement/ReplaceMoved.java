package forge.game.replacement;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Map;

import com.google.common.collect.Sets;

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
        final Player controller = getHostCard().getController();
        final Card affected = (Card) runParams.get("Affected");

        if (hasParam("ValidCard")) {
            if (!matchesValid(affected, getParam("ValidCard").split(","), getHostCard())) {
                return false;
            }
        }

        if (hasParam("ValidLKI")) {
            if (!matchesValid(runParams.get("CardLKI"), getParam("ValidLKI").split(","), getHostCard())) {
                return false;
            }
        }

        boolean matchedZone = false;
        if (hasParam("Origin")) {
            for(ZoneType z : ZoneType.listValueOf(getParam("Origin"))) {
                if(z == (ZoneType) runParams.get("Origin"))
                    matchedZone =  true;
            }

            if(!matchedZone)
            {
                return false;
            }
        }        

        if (hasParam("Destination")) {
            matchedZone = false;
            ZoneType zt = (ZoneType) runParams.get("Destination");
            for(ZoneType z : ZoneType.listValueOf(getParam("Destination"))) {
                if(z == zt)
                    matchedZone =  true;
            }

            if(!matchedZone)
            {
                return false;
            }

            if (zt.equals(ZoneType.Battlefield) && getHostCard().equals(affected)) {
                // would be an etb replacement effect that enters the battlefield
                Card lki = CardUtil.getLKICopy(affected);
                lki.setLastKnownZone(lki.getController().getZone(zt));

                CardCollection preList = new CardCollection(lki);
                getHostCard().getGame().getAction().checkStaticAbilities(false, Sets.newHashSet(lki), preList);

                // check if when entering the battlefield would still has this RE or is suppressed
                if (!lki.hasReplacementEffect(this) || lki.getReplacementEffect(getId()).isSuppressed()) {
                    return false;
                }
            }
        }
        
        if (hasParam("ExcludeDestination")) {
            matchedZone = false;
            for(ZoneType z : ZoneType.listValueOf(getParam("ExcludeDestination"))) {
                if(z == (ZoneType) runParams.get("Destination"))
                    matchedZone =  true;
            }
            
            if(matchedZone)
            {
                return false;
            }
        }
        
        if (hasParam("Fizzle")) {
            // if Replacement look for Fizzle
            if (!runParams.containsKey("Fizzle")) {
                return false;
            }
            Boolean val = (Boolean) runParams.get("Fizzle");
            if ("True".equals(getParam("Fizzle")) != val) {
                return false;
            }
        }
        
        if (hasParam("ValidStackSa")) {
            if (!runParams.containsKey("StackSa")) {
                return false;
            }
            if (!((SpellAbility)runParams.get("StackSa")).isValid(getParam("ValidStackSa").split(","), getHostCard().getController(), getHostCard(), null)) {
                return false;
            }
        }

        if (hasParam("Cause")) {
            if (!runParams.containsKey("Cause")) {
                return false;
            }
            SpellAbility cause = (SpellAbility) runParams.get("Cause");
            if (cause == null) {
                return false;
            }
            if (!cause.isValid(getParam("Cause").split(","), controller, getHostCard(), null)) {
                return false;
            }
        }

        if (hasParam("NotCause")) {
            if (runParams.containsKey("Cause")) {
                SpellAbility cause = (SpellAbility) runParams.get("Cause");
                if (cause != null) {
                    if (cause.isValid(getParam("NotCause").split(","), controller, getHostCard(), null)) {
                        return false;
                    }
                }
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
        sa.setReplacingObject("Cause", runParams.get("Cause"));
    }

}
