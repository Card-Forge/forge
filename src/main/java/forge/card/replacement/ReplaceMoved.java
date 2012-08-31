package forge.card.replacement;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;

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
    public ReplaceMoved(final HashMap<String, String> mapParams, final Card host) {
        super(mapParams, host);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.HashMap)
     */
    @Override
    public boolean canReplace(HashMap<String, Object> runParams) {
        if (!runParams.get("Event").equals("Moved")) {
            return false;
        }
        if (this.getMapParams().containsKey("ValidCard")) {
            if (!matchesValid(runParams.get("Affected"), this.getMapParams().get("ValidCard").split(","), this.getHostCard())) {
                return false;
            }
        }
        if (this.getMapParams().containsKey("Origin")) {
            ZoneType z = ZoneType.smartValueOf(this.getMapParams().get("Origin"));
            if (z != (ZoneType) runParams.get("Origin")) {
                return false;
            }
        }
        if (this.getMapParams().containsKey("Destination")) {
            ZoneType z = ZoneType.smartValueOf(this.getMapParams().get("Destination"));
            if (z != (ZoneType) runParams.get("Destination")) {
                return false;
            }
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#getCopy()
     */
    @Override
    public ReplacementEffect getCopy() {
        ReplacementEffect res = new ReplaceMoved(this.getMapParams(), this.getHostCard());
        res.setOverridingAbility(this.getOverridingAbility());
        res.setActiveZone(validHostZones);
        res.setLayer(getLayer());
        return res;
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#setReplacingObjects(java.util.HashMap, forge.card.spellability.SpellAbility)
     */
    @Override
    public void setReplacingObjects(HashMap<String, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject("Card", runParams.get("Affected"));
    }

}
