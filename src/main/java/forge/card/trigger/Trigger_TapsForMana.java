package forge.card.trigger;

import forge.Card;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;

/**
 * <p>Trigger_TapsForMana class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Trigger_TapsForMana extends Trigger {

    /**
     * <p>Constructor for Trigger_TapsForMana.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_TapsForMana(HashMap<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(java.util.Map<String, Object> runParams2) {
        Card tapper = (Card) runParams2.get("Card");

        if (mapParams.containsKey("ValidCard")) {
            if (!tapper.isValidCard(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_TapsForMana(mapParams, hostCard, isIntrinsic);
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
        sa.setTriggeringObject("Player", runParams.get("Player"));
        sa.setTriggeringObject("Produced", runParams.get("Produced"));
    }
}
