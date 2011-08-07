package forge.card.trigger;

import forge.Card;
import forge.CardList;
import forge.card.spellability.SpellAbility;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Trigger_Attacks class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Trigger_Attacks extends Trigger {

    /**
     * <p>Constructor for Trigger_Attacks.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger_Attacks(HashMap<String, String> params, Card host) {
        super(params, host);
    }

    /** {@inheritDoc} */
    @Override
    public boolean performTest(Map<String, Object> runParams) {
        if (mapParams.containsKey("ValidCard")) {
            if (!matchesValid(runParams.get("Attacker"), mapParams.get("ValidCard").split(","), hostCard)) {
                return false;
            }
        }

        if (mapParams.containsKey("Alone")) {
            CardList otherAttackers = (CardList) runParams.get("OtherAttackers");
            if (otherAttackers == null) {
                return false;
            }
            if (mapParams.get("Alone").equals("True")) {
                if (otherAttackers.size() != 0) {
                    return false;
                }
            } else {
                if (otherAttackers.size() == 0) {
                    return false;
                }
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public Trigger getCopy() {
        Trigger copy = new Trigger_Attacks(mapParams, hostCard);
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
        sa.setTriggeringObject("Attacker", runParams.get("Attacker"));
    }
}
