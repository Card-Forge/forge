package forge.card.trigger;

import java.util.HashMap;
import java.util.Map;

import forge.Card;
import forge.card.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TriggerTransformed extends Trigger {

    public TriggerTransformed(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#performTest(java.util.Map)
     */
    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        if (this.getMapParams().containsKey("ValidCard")) {
            if (!matchesValid(runParams2.get("Transformer"), this.getMapParams().get("ValidCard").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#getCopy()
     */
    @Override
    public final Trigger getCopy() {
        final Trigger copy = new TriggerTransformed(this.getMapParams(), this.getHostCard(), this.isIntrinsic());

        if (this.getOverridingAbility() != null) {
            copy.setOverridingAbility(this.getOverridingAbility());
        }
        copy.setName(this.getName());
        copy.setID(this.getId());

        return copy;
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#setTriggeringObjects(forge.card.spellability.SpellAbility)
     */
    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        sa.setTriggeringObject("Transformer", this.getRunParams().get("Transformer"));
    }

}
