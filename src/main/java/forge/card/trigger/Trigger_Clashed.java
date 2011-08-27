package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * <p>Trigger_Clashed class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Trigger_Clashed extends Trigger {

    /**
     * <p>Constructor for Trigger_Clashed.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_Clashed(HashMap<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(java.util.Map<String, Object> runParams2) {
        if (mapParams.containsKey("ValidPlayer")) {
            if (!matchesValid(runParams.get("Player"), mapParams.get("ValidPlayer").split(","), hostCard)) {
                return false;
            }
        }

        if (mapParams.containsKey("Won")) {
            if (!mapParams.get("Won").equals(runParams2.get("Won")))
                return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_Clashed(mapParams, hostCard, isIntrinsic);
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
        //No triggered-variables for you :(
    }
}
