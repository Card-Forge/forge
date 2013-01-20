package forge.card.trigger;

import java.util.Map;

import forge.Card;
import forge.card.spellability.SpellAbility;
import forge.game.PlanarDice;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TriggerPlanarDice extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_RollPlanarDice.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerPlanarDice(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }
    
    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#performTest(java.util.Map)
     */
    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        if (this.getMapParams().containsKey("ValidPlayer")) {
            if (!matchesValid(runParams2.get("Player"), this.getMapParams().get("ValidPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        
        if (this.getMapParams().containsKey("Result")) {
            PlanarDice cond = PlanarDice.smartValueOf(this.getMapParams().get("Result"));
            if(cond != ((PlanarDice)runParams2.get("Result"))) {
                return false;
            }
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#getCopy()
     */
    @Override
    public Trigger getCopy() {
        final Trigger copy = new TriggerPlanarDice(this.getMapParams(), this.getHostCard(), this.isIntrinsic());
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
        //THE BLACKEST TRIGGERINGOBJECT FOR THE MOST BRUTAL OF ALL TRIGGERS! NOOOOOTHIIIIIING!
    }

}
