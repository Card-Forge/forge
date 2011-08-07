package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * <p>Trigger_LandPlayed class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Trigger_LandPlayed extends Trigger {

    /**
     * <p>Constructor for Trigger_LandPlayed.</p>
     *
     * @param n a {@link java.lang.String} object.
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_LandPlayed(String n, HashMap<String, String> params, Card host) {
        super(n, params, host);
    }

    /**
     * <p>Constructor for Trigger_LandPlayed.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_LandPlayed(HashMap<String, String> params, Card host) {
        super(params, host);
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_LandPlayed(name, mapParams, hostCard);
        copy.setID(ID);

        if (this.overridingAbility != null) {
            copy.setOverridingAbility(overridingAbility);
        }

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
