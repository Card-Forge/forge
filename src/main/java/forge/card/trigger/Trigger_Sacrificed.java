package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * <p>Trigger_Sacrificed class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Trigger_Sacrificed extends Trigger {

    /**
     * <p>Constructor for Trigger_Sacrificed.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_Sacrificed(HashMap<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(java.util.Map<String, Object> runParams) {
        Card sac = (Card) runParams.get("Card");
        if (mapParams.containsKey("ValidPlayer")) {
            if (!matchesValid(sac.getController(), mapParams.get("ValidPlayer").split(","), hostCard)) {
                return false;
            }
        }
        if (mapParams.containsKey("ValidCard")) {
            if (!sac.isValidCard(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_Sacrificed(mapParams, hostCard, isIntrinsic);
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
