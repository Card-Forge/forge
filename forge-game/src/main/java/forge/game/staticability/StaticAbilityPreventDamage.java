/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.staticability;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;

import java.util.Map;

/**
 * The Class StaticAbility_PreventDamage.
 */
public class StaticAbilityPreventDamage {

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
    public static int applyPreventDamageAbility(final StaticAbility stAb, final Card source, final GameEntity target,
            final int damage, final boolean isCombat, final boolean isTest) {
        final Map<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();
        int restDamage = damage;

        if (params.containsKey("Source")
                && !source.isValid(params.get("Source").split(","), hostCard.getController(), hostCard, null)) {
            return restDamage;
        }

        if (params.containsKey("Target")
                && !target.isValid(params.get("Target").split(","), hostCard.getController(), hostCard, null)) {
            return restDamage;
        }

        if (params.containsKey("CombatDamage") && params.get("CombatDamage").equals("True") && !isCombat) {
            return restDamage;
        }

        if (params.containsKey("CombatDamage") && params.get("CombatDamage").equals("False") && isCombat) {
            return restDamage;
        }

        if (params.containsKey("MaxDamage") && (Integer.parseInt(params.get("MaxDamage")) < damage)) {
            return restDamage;
        }

        if (params.containsKey("SourceSharesColorWithTarget")) {
            if (!(target instanceof Card) || source.equals(target)) {
                return restDamage;
            }
            Card targetCard = (Card) target;
            if (!source.sharesColorWith(targetCard)) {
                return restDamage;
            }
        }

        if (params.containsKey("Optional")) { //Assume if param is present it should be optional
            if (!isTest) {
                final String logic = params.containsKey("AILogic") ? params.get("AILogic") : "";
                final String message = "Apply the effect of " + hostCard + "? (Affected: " + target + ")";
                boolean confirmed = hostCard.getController().getController().confirmStaticApplication(hostCard, target, logic, message);
    
                if (!confirmed) {
                    return restDamage;
                }
            } else { //test
                if (!hostCard.getController().equals(target)) {
                    return restDamage;
                }
            }
        }

        // no amount means all
        if (!params.containsKey("Amount") || params.get("Amount").equals("All")) {
            return 0;
        }

        if (params.get("Amount").matches("[0-9][0-9]?")) {
            restDamage = restDamage - Integer.parseInt(params.get("Amount"));
        } else if (params.get("Amount").matches("HalfUp")) {
            restDamage = restDamage - (int) (Math.ceil(restDamage / 2.0));
        } else if (params.get("Amount").matches("HalfDown")) {
            restDamage = restDamage - (int) (Math.floor(restDamage / 2.0));
        } else {
            restDamage = restDamage - CardFactoryUtil.xCount(hostCard, hostCard.getSVar(params.get("Amount")));
        }

        if (restDamage < 0) {
            return 0;
        }

        return restDamage;
    }

}
