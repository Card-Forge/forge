package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_Phase class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerPhase extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Phase.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerPhase(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        if (this.getMapParams().containsKey("Phase")) {
            if (this.getMapParams().get("Phase").contains(",")) {
                boolean found = false;
                for (final String s : this.getMapParams().get("Phase").split(",")) {
                    if (s.equals(runParams2.get("Phase"))) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    return false;
                }
            } else {
                if (!this.getMapParams().get("Phase").equals(runParams2.get("Phase"))) {
                    return false;
                }
            }
        }
        if (this.getMapParams().containsKey("ValidPlayer")) {
            if (!this.matchesValid(runParams2.get("Player"), this.getMapParams().get("ValidPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        final Trigger copy = new TriggerPhase(this.getMapParams(), this.getHostCard(), this.isIntrinsic());
        if (this.getOverridingAbility() != null) {
            copy.setOverridingAbility(this.getOverridingAbility());
        }
        copy.setName(this.getName());
        copy.setID(this.getId());

        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Player", this.getRunParams().get("Player"));
    }
}
