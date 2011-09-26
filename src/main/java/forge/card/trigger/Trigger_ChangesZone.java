package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * <p>Trigger_ChangesZone class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Trigger_ChangesZone extends Trigger {

    /**
     * <p>Constructor for Trigger_ChangesZone.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_ChangesZone(HashMap<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(java.util.Map<String, Object> runParams2) {
        if (mapParams.containsKey("Origin")) {
            if (!mapParams.get("Origin").equals("Any")) {
                if (mapParams.get("Origin") == null) {
                    return false;
                }
                if (!mapParams.get("Origin").equals((String) runParams2.get("Origin"))) {
                    return false;
                }
            }
        }

        if (mapParams.containsKey("Destination")) {
            if (!mapParams.get("Destination").equals("Any")) {
                if (!mapParams.get("Destination").equals((String) runParams2.get("Destination"))) {
                    return false;
                }
            }
        }

        if (mapParams.containsKey("ValidCard")) {
            Card moved = (Card) runParams2.get("Card");
            if (!moved.isValid(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_ChangesZone(mapParams, hostCard, isIntrinsic);
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
}
