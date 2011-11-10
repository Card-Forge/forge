package forge.card.staticability;

import java.util.HashMap;

import forge.Card;
import forge.Player;
import forge.card.spellability.SpellAbility;

/**
 * The Class StaticAbility_PreventDamage.
 */
public class StaticAbilityCantTarget {

    /**
     * TODO Write javadoc for this method.
     * 
     * @param stAb
     *            a StaticAbility
     * @param source
     *            the source
     * @param target
     *            the target
     * @param damage
     *            the damage
     * @param isCombat
     *            the is combat
     * @return the int
     */
    public static boolean applyCantTargetAbility(final StaticAbility stAb, final Card card, SpellAbility sa) {
        final HashMap<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();
        final Card source = sa.getSourceCard();
        final Player activator = sa.getActivatingPlayer();

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
            return false;
        }
        
        if (params.containsKey("ValidSource")
                && !source.isValid(params.get("ValidSource").split(","), hostCard.getController(), hostCard)) {
            return false;
        }

        if (params.containsKey("Activator") && (activator != null)
                && !activator.isValid(params.get("Activator"), hostCard.getController(), hostCard)) {
            return false;
        }

        return true;
    }

}
