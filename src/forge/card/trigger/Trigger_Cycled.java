package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * <p>Trigger_Cycled class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Trigger_Cycled extends Trigger {

    /**
     * <p>Constructor for Trigger_Cycled.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_Cycled(HashMap<String, String> params, Card host) {
        super(params, host);
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_Cycled(mapParams, hostCard);
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
        sa.setTriggeringObject("Card", runParams.get("Card"));
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(java.util.Map<String, Object> runParams) {
        if (mapParams.containsKey("ValidCard")) {
            if (!matchesValid(runParams.get("Card"), mapParams.get("ValidCard").split(","), hostCard)) {
                return false;
            }
        }
        return true;
    }

}
