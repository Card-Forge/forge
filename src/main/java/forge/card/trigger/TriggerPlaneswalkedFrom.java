package forge.card.trigger;

import java.util.Map;

import forge.Card;
import forge.card.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TriggerPlaneswalkedFrom extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_PlaneswalkedTo.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerPlaneswalkedFrom(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }
    
    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#performTest(java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("ValidCard")) {
            for(Card moved : (Iterable<Card>)runParams2.get("Card"))
            {
                if (moved.isValid(this.mapParams.get("ValidCard").split(","), this.getHostCard().getController(),
                        this.getHostCard())) {
                    return true;
                }
            }
            return false;
        }
        
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#getCopy()
     */
    @Override
    public Trigger getCopy() {
        final Trigger copy = new TriggerPlaneswalkedFrom(this.mapParams, this.getHostCard(), this.isIntrinsic());
        if (this.getOverridingAbility() != null) {
            copy.setOverridingAbility(this.getOverridingAbility());
        }

        copyFieldsTo(copy);
        return copy;
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#setTriggeringObjects(forge.card.spellability.SpellAbility)
     */
    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        sa.setTriggeringObject("Card", this.getRunParams().get("Card"));
    }

}
