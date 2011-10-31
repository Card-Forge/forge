package forge.card.trigger;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.CardList;
import forge.Player;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_Attacks class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Trigger_Attacks extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Attacks.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger_Attacks(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        if (mapParams.containsKey("ValidCard")) {
            if (!matchesValid(runParams2.get("Attacker"), mapParams.get("ValidCard").split(","), hostCard)) {
                return false;
            }
        }
        
        if (mapParams.containsKey("Attacked")) {            
            if (mapParams.get("Attacked").equals("Player") && StringUtils.isNumeric(runParams2.get("Attacked").toString()) 
                    && Integer.parseInt(runParams2.get("Attacked").toString()) > 0)
                return false;
        }

        if (mapParams.containsKey("Alone")) {
            CardList otherAttackers = (CardList) runParams2.get("OtherAttackers");
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
    public final Trigger getCopy() {
        Trigger copy = new Trigger_Attacks(mapParams, hostCard, isIntrinsic);
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
        sa.setTriggeringObject("Attacker", runParams.get("Attacker"));
    }
}
