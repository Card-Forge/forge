package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_Unequip class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Trigger_Unequip extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Unequip.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger_Unequip(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        Card equipped = (Card) runParams2.get("Card");
        Card equipment = (Card) runParams2.get("Equipment");

        if (mapParams.containsKey("ValidCard")) {
            if (!equipped.isValid(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
                return false;
            }
        }

        if (mapParams.containsKey("ValidEquipment")) {
            if (!equipment.isValid(mapParams.get("ValidEquipment").split(","), hostCard.getController(), hostCard)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        Trigger copy = new Trigger_Unequip(mapParams, hostCard, isIntrinsic);
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
        sa.setTriggeringObject("Equipment", runParams.get("Equipment"));
    }
}
