package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_LandPlayed class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Trigger_LandPlayed extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_LandPlayed.
     * </p>
     * 
     * @param n
     *            a {@link java.lang.String} object.
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger_LandPlayed(final String n,
            final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(n, params, host, intrinsic);
    }

    /**
     * <p>
     * Constructor for Trigger_LandPlayed.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger_LandPlayed(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        Trigger copy = new Trigger_LandPlayed(name, mapParams, hostCard, isIntrinsic);
        copy.setID(ID);

        if (this.overridingAbility != null) {
            copy.setOverridingAbility(overridingAbility);
        }

        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Card", runParams.get("Card"));
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        if (mapParams.containsKey("ValidCard")) {
            if (!matchesValid(runParams2.get("Card"), mapParams.get("ValidCard").split(","), hostCard)) {
                return false;
            }
        }
        return true;
    }

}
