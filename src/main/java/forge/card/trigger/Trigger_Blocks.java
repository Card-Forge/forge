package forge.card.trigger;

import java.util.HashMap;
import java.util.Map;

import forge.Card;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_Blocks class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Trigger_Blocks extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Blocks.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger_Blocks(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        if (this.getMapParams().containsKey("ValidCard")) {
            if (!this.matchesValid(runParams2.get("Blocker"), this.getMapParams().get("ValidCard").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        if (this.getMapParams().containsKey("ValidBlocked")) {
            if (!this.matchesValid(runParams2.get("Attacker"), this.getMapParams().get("ValidBlocked").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        final Trigger copy = new Trigger_Blocks(this.getMapParams(), this.getHostCard(), this.isIntrinsic());
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
        sa.setTriggeringObject("Blocker", this.getRunParams().get("Blocker"));
        sa.setTriggeringObject("Attacker", this.getRunParams().get("Attacker"));
    }
}
