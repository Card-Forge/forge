package forge.card.replacement;

import java.util.Map;

import forge.Card;
import forge.card.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceTurnFaceUp extends ReplacementEffect {

    /**
     * 
     * TODO: Write javadoc for Constructor.
     * @param mapParams &emsp; HashMap<String, String>
     * @param host &emsp; Card
     */
    public ReplaceTurnFaceUp(final Map<String, String> mapParams, final Card host) {
        super(mapParams, host);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.HashMap)
     */
    @Override
    public boolean canReplace(Map<String, Object> runParams) {
        if (!runParams.get("Event").equals("TurnFaceUp")) {
            return false;
        }
        if (this.getMapParams().containsKey("ValidCard")) {
            if (!matchesValid(runParams.get("Affected"), this.getMapParams().get("ValidCard").split(","), this.getHostCard())) {
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
        ReplacementEffect res = new ReplaceTurnFaceUp(this.getMapParams(), this.getHostCard());
        res.setOverridingAbility(this.getOverridingAbility());
        res.setActiveZone(validHostZones);
        res.setLayer(getLayer());
        return res;
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#setReplacingObjects(java.util.HashMap, forge.card.spellability.SpellAbility)
     */
    @Override
    public void setReplacingObjects(Map<String, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject("Card", runParams.get("Affected"));
    }

}
