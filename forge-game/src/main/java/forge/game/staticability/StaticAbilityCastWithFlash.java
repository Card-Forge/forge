package forge.game.staticability;

import java.util.List;

import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class StaticAbilityCastWithFlash {
    public static boolean applyWithFlashAbility(final StaticAbility stAb, final SpellAbility sa, final Card card, final Player activator) {
        final Card hostCard = stAb.getHostCard();

        if (stAb.hasParam("ValidCard")
                && !card.isValid(stAb.getParam("ValidCard").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }

        if (stAb.hasParam("ValidSA")
                && !sa.isValid(stAb.getParam("ValidSA").split(","), hostCard.getController(), hostCard, null)) {
            return false;
        }
        
        if (stAb.hasParam("Caster") && (activator != null)
                && !activator.isValid(stAb.getParam("Caster"), hostCard.getController(), hostCard, null)) {
            return false;
        }

        if (stAb.hasParam("Targeting")) {
            if (!sa.usesTargeting()) {
                return false;
            }
            boolean found = false;
            String[] valids = stAb.getParam("Targeting").split(",");
            for (GameObject ga : sa.getTargets()) {
                if (ga.isValid(valids, hostCard.getController(), hostCard, null)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        if (stAb.hasParam("Origin")) {
            List<ZoneType> src = ZoneType.listValueOf(stAb.getParam("Origin"));
            if (!src.contains(hostCard.getGame().getZoneOf(card).getZoneType())) {
                return false;
            }
        }

        return true;
    }
}
