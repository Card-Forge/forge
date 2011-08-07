package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Trigger_AttackerBlocked class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Trigger_AttackerBlocked extends Trigger {

    /**
     * <p>Constructor for Trigger_AttackerBlocked.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_AttackerBlocked(HashMap<String, String> params, Card host) {
        super(params, host);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(Map<String, Object> runParams) {
        if (mapParams.containsKey("ValidCard")) {
            if (!matchesValid(runParams.get("Attacker"), mapParams.get("ValidCard").split(","), hostCard)) {
                return false;
            }
        }
        if (mapParams.containsKey("ValidBlocker")) {
            if (!matchesValid(runParams.get("Blocker"), mapParams.get("ValidBlocker").split(","), hostCard)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_AttackerBlocked(mapParams, hostCard);
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
        sa.setTriggeringObject("Attacker", runParams.get("Attacker"));
        sa.setTriggeringObject("Blocker", runParams.get("Blocker"));
        sa.setTriggeringObject("NumBlockers", runParams.get("NumBlockers"));
    }
}
