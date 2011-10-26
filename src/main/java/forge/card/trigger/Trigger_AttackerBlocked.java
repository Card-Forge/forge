package forge.card.trigger;

import java.util.HashMap;
import java.util.Map;

import forge.Card;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_AttackerBlocked class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Trigger_AttackerBlocked extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_AttackerBlocked.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger_AttackerBlocked(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        if (mapParams.containsKey("ValidCard")) {
            if (!matchesValid(runParams2.get("Attacker"), mapParams.get("ValidCard").split(","), hostCard)) {
                return false;
            }
        }
        if (mapParams.containsKey("ValidBlocker")) {
            if (!matchesValid(runParams2.get("Blocker"), mapParams.get("ValidBlocker").split(","), hostCard)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        Trigger copy = new Trigger_AttackerBlocked(mapParams, hostCard, isIntrinsic);
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
        sa.setTriggeringObject("Attacker", runParams.get("Attacker"));
        sa.setTriggeringObject("Blocker", runParams.get("Blocker"));
        sa.setTriggeringObject("NumBlockers", runParams.get("NumBlockers"));
    }
}
