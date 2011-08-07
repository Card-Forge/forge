package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * <p>Trigger_Phase class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Trigger_Phase extends Trigger {

    /**
     * <p>Constructor for Trigger_Phase.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_Phase(HashMap<String, String> params, Card host) {
        super(params, host);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(java.util.Map<String, Object> runParams) {
        if (mapParams.containsKey("Phase")) {
            if (mapParams.get("Phase").contains(",")) {
                boolean found = false;
                for (String s : mapParams.get("Phase").split(",")) {
                    if (s.equals(runParams.get("Phase"))) {
                        found = true;
                        break;
                    }
                }

                if (!found)
                    return false;
            } else {
                if (!mapParams.get("Phase").equals(runParams.get("Phase"))) {
                    return false;
                }
            }
        }
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
        Trigger copy = new Trigger_Phase(mapParams, hostCard);
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
