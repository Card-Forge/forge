package forge.card.trigger;

import java.util.HashMap;
import java.util.Map;

import forge.Card;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_BecomesTarget class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class Trigger_BecomesTarget extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_BecomesTarget.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger_BecomesTarget(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        if (this.getMapParams().containsKey("SourceType")) {
            final SpellAbility sa = (SpellAbility) runParams2.get("SourceSA");
            if (this.getMapParams().get("SourceType").equalsIgnoreCase("spell")) {
                if (!sa.isSpell()) {
                    return false;
                }
            } else if (this.getMapParams().get("SourceType").equalsIgnoreCase("ability")) {
                if (!sa.isAbility()) {
                    return false;
                }
            }
        }
        if (this.getMapParams().containsKey("ValidSource")) {
            if (!this.matchesValid(((SpellAbility) runParams2.get("SourceSA")).getSourceCard(), this.getMapParams()
                    .get("ValidSource").split(","), this.getHostCard())) {
                return false;
            }
        }
        if (this.getMapParams().containsKey("ValidTarget")) {
            if (!this.matchesValid(runParams2.get("Target"), this.getMapParams().get("ValidTarget").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        final Trigger copy = new Trigger_BecomesTarget(this.getMapParams(), this.getHostCard(), this.isIntrinsic());
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
        sa.setTriggeringObject("SourceSA", this.getRunParams().get("SourceSA"));
        sa.setTriggeringObject("Source", ((SpellAbility) this.getRunParams().get("SourceSA")).getSourceCard());
        sa.setTriggeringObject("Target", this.getRunParams().get("Target"));
    }
}
