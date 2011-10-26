package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_TapsForMana class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Trigger_TapsForMana extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_TapsForMana.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger_TapsForMana(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        Card tapper = (Card) runParams2.get("Card");

        if (mapParams.containsKey("ValidCard")) {
            if (!tapper.isValid(mapParams.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
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
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Card", runParams.get("Card"));
        sa.setTriggeringObject("Player", runParams.get("Player"));
        sa.setTriggeringObject("Produced", runParams.get("Produced"));
    }
}
