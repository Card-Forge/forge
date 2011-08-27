package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;
import java.util.Map;


/**
 * <p>Trigger_Blocks class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Trigger_Blocks extends Trigger {

    /**
     * <p>Constructor for Trigger_Blocks.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_Blocks(HashMap<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        if (mapParams.containsKey("ValidCard")) {
            if (!matchesValid(runParams.get("Blocker"), mapParams.get("ValidCard").split(","), hostCard)) {
                return false;
            }
        }
        if (mapParams.containsKey("ValidBlocked")) {
            if (!matchesValid(runParams2.get("Attacker"), mapParams.get("ValidBlocked").split(","), hostCard)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_Blocks(mapParams, hostCard, isIntrinsic);
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
        sa.setTriggeringObject("Blocker", runParams.get("Blocker"));
        sa.setTriggeringObject("Attacker", runParams.get("Attacker"));
    }
}
