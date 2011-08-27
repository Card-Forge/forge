package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * <p>Trigger_Phase class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Trigger_Phase extends Trigger {

    /**
     * <p>Constructor for Trigger_Phase.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_Phase(HashMap<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(java.util.Map<String, Object> runParams2) {
        if (mapParams.containsKey("Phase")) {
            if (mapParams.get("Phase").contains(",")) {
                boolean found = false;
                for (String s : mapParams.get("Phase").split(",")) {
                    if (s.equals(runParams2.get("Phase"))) {
                        found = true;
                        break;
                    }
                }

                if (!found)
                    return false;
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
    public Trigger getCopy() {
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
    public void setTriggeringObjects(SpellAbility sa) {
        sa.setTriggeringObject("Player", runParams.get("Player"));
    }
}
