package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * <p>Trigger_LifeLost class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Trigger_LifeLost extends Trigger {

    /**
     * <p>Constructor for Trigger_LifeLost.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_LifeLost(HashMap<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(java.util.Map<String, Object> runParams2) {
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
        Trigger copy = new Trigger_LifeLost(mapParams, hostCard, isIntrinsic);
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
        sa.setTriggeringObject("LifeAmount", runParams.get("LifeAmount"));
        sa.setTriggeringObject("Player", runParams.get("Player"));
    }
}
