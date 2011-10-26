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
public class Trigger_Phase extends Trigger {

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
    public Trigger_Phase(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        if (mapParams.containsKey("Phase")) {
            if (mapParams.get("Phase").contains(",")) {
                boolean found = false;
                for (String s : mapParams.get("Phase").split(",")) {
                    if (s.equals(runParams2.get("Phase"))) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    return false;
                }
            } else {
                if (!mapParams.get("Phase").equals(runParams2.get("Phase"))) {
                    return false;
                }
            }
        }
        if (mapParams.containsKey("ValidPlayer")) {
            if (!matchesValid(runParams2.get("Player"), mapParams.get("ValidPlayer").split(","), hostCard)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        Trigger copy = new Trigger_Phase(mapParams, hostCard, isIntrinsic);
        if (overridingAbility != null) {
            copy.setOverridingAbility(overridingAbility);
        }
        copy.setName(name);
        copy.setID(ID);

        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Player", runParams.get("Player"));
    }
}
