package forge.card.trigger;

import forge.Card;
import forge.Counters;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * <p>Trigger_CounterAdded class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Trigger_CounterAdded extends Trigger {

    /**
     * <p>Constructor for Trigger_CounterAdded.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_CounterAdded(HashMap<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(java.util.Map<String, Object> runParams2) {
        Card addedTo = (Card) runParams2.get("Card");
        Counters addedType = (Counters) runParams2.get("CounterType");

        if (mapParams.containsKey("ValidCard")) {
            if (!addedTo.isValid(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
                return false;
            }
        }

        if (mapParams.containsKey("CounterType")) {
            String type = mapParams.get("CounterType");
            if (!type.equals(addedType.toString())) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_CounterAdded(mapParams, hostCard, isIntrinsic);
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
