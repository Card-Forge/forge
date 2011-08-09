package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * <p>Trigger_Unequip class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Trigger_Unequip extends Trigger {

    /**
     * <p>Constructor for Trigger_Unequip.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_Unequip(HashMap<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(java.util.Map<String, Object> runParams) {
        Card equipped = (Card) runParams.get("Card");
        Card equipment = (Card) runParams.get("Equipment");

        if (mapParams.containsKey("ValidCard")) {
            if (!equipped.isValidCard(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
                return false;
            }
        }

        if (mapParams.containsKey("ValidEquipment")) {
            if (!equipment.isValidCard(mapParams.get("ValidEquipment").split(","), hostCard.getController(), hostCard)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
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
    public void setTriggeringObjects(SpellAbility sa) {
        sa.setTriggeringObject("Card", runParams.get("Card"));
        sa.setTriggeringObject("Equipment", runParams.get("Equipment"));
    }
}
