package forge.card.staticability;

import java.util.HashMap;

import forge.Card;
import forge.Constant;
import forge.Constant.Zone;
import forge.Player;
import forge.card.spellability.SpellAbility;

/**
 * The Class StaticAbility_PreventDamage.
 */
public class StaticAbilityCantTarget {

    /**
     * Apply can't target ability.
     *
     * @param staticAbility the static ability
     * @param card the card
     * @param spellAbility the spell Ability
     * @return true, if successful
     */
    public static boolean applyCantTargetAbility(final StaticAbility staticAbility, final Card card, final SpellAbility spellAbility) {
        final HashMap<String, String> params = staticAbility.getMapParams();
        final Card hostCard = staticAbility.getHostCard();
        final Card source = spellAbility.getSourceCard();
        final Player activator = spellAbility.getActivatingPlayer();

        if (params.containsKey("AffectedZone")) {
            if (!card.isInZone(Zone.smartValueOf(params.get("AffectedZone")))) {
                return false;
            }
        } else { // default zone is battlefield
            if (!card.isInZone(Constant.Zone.Battlefield)) {
                return false;
            }
        }

        if (params.containsKey("Spell") && !spellAbility.isSpell()) {
            return false;
        }

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
