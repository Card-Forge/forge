package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * <p>Trigger_LifeGained class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Trigger_LifeGained extends Trigger {

    /**
     * <p>Constructor for Trigger_LifeGained.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_LifeGained(HashMap<String, String> params, Card host) {
        super(params, host);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(java.util.Map<String, Object> runParams) {
        if (mapParams.containsKey("ValidPlayer")) {
            if (!matchesValid(runParams.get("Player"), mapParams.get("ValidPlayer").split(","), hostCard)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_LifeGained(mapParams, hostCard);
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
