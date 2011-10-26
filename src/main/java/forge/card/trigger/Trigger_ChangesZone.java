package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_ChangesZone class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Trigger_ChangesZone extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_ChangesZone.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger_ChangesZone(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
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
    public final Trigger getCopy() {
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
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Card", runParams.get("Card"));
    }
}
