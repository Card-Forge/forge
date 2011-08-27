package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Trigger_BecomesTarget class.</p>
 *
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class Trigger_BecomesTarget extends Trigger {

    /**
     * <p>Constructor for Trigger_BecomesTarget.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_BecomesTarget(HashMap<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }


    /** {@inheritDoc} */
    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        if (mapParams.containsKey("SourceType")) {
            SpellAbility sa = (SpellAbility) runParams2.get("SourceSA");
            if (mapParams.get("SourceType").equalsIgnoreCase("spell")) {
                if (!sa.isSpell()) {
                    return false;
                }
            } else if (mapParams.get("SourceType").equalsIgnoreCase("ability")) {
                if (!sa.isAbility()) {
                    return false;
                }
            }
        }
        if (mapParams.containsKey("ValidSource")) {
            if (!matchesValid(((SpellAbility) runParams2.get("SourceSA")).getSourceCard(), mapParams.get("ValidSource").split(","), hostCard)) {
                return false;
            }
        }
        if (mapParams.containsKey("ValidTarget")) {
            if (!matchesValid(runParams2.get("Target"), mapParams.get("ValidTarget").split(","), hostCard)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_BecomesTarget(mapParams, hostCard, isIntrinsic);
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
        sa.setTriggeringObject("SourceSA", runParams.get("SourceSA"));
        sa.setTriggeringObject("Source", ((SpellAbility) runParams.get("SourceSA")).getSourceCard());
        sa.setTriggeringObject("Target", runParams.get("Target"));
    }
}
