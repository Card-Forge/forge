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
package forge.game.replacement;

import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;

import java.util.Map;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceDamage extends ReplacementEffect {

    /**
     * TODO: Write javadoc for Constructor.
     *
     * @param map the map
     * @param host the host
     */
    public ReplaceDamage(Map<String, String> map, Card host, boolean intrinsic) {
        super(map, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.HashMap)
     */
    @Override
    public boolean canReplace(Map<String, Object> runParams) {
        if (!runParams.get("Event").equals("DamageDone")) {
            return false;
        }
        if (!(runParams.containsKey("Prevention") == (hasParam("PreventionEffect") || hasParam("Prevent")))) {
            return false;
        }
        if (((Integer) runParams.get("DamageAmount")) == 0) {
            // If no actual damage is dealt, there is nothing to replace
            return false;
        }
        if (hasParam("ValidSource")) {
            String validSource = getParam("ValidSource");
            validSource = AbilityUtils.applyAbilityTextChangeEffects(validSource, this);	
            if (!matchesValid(runParams.get("DamageSource"), validSource.split(","), getHostCard())) {
                return false;
            }
        }
        if (hasParam("ValidTarget")) {
            String validTarget = getParam("ValidTarget");
            validTarget = AbilityUtils.applyAbilityTextChangeEffects(validTarget, this);
            if (!matchesValid(runParams.get("Affected"), validTarget.split(","), getHostCard())) {
                return false;
            }
        }
        if (hasParam("ValidCause")) {
            if (!runParams.containsKey("Cause")) {
                return false;
            }
            SpellAbility cause = (SpellAbility) runParams.get("Cause");
            String validCause = getParam("ValidCause");
            validCause = AbilityUtils.applyAbilityTextChangeEffects(validCause, this);
            if (!matchesValid(cause, validCause.split(","), getHostCard())) {
                return false;
            }
            if (hasParam("CauseIsSource")) {
                if (!cause.getHostCard().equals(runParams.get("DamageSource"))) {
                    return false;
                }
            }
        }
        if (hasParam("RelativeToSource")) {
            Card source = (Card) runParams.get("DamageSource");
            String validRelative = getParam("RelativeToSource");
            validRelative = AbilityUtils.applyAbilityTextChangeEffects(validRelative, this);
            if (!matchesValid(runParams.get("DamageTarget"), validRelative.split(","), source)) {
                return false;
            }
        }
        if (hasParam("DamageAmount")) {
            String full = getParam("DamageAmount");
            String operator = full.substring(0, 2);
            String operand = full.substring(2);
            int intoperand = 0;
            try {
                intoperand = Integer.parseInt(operand);
            } catch (NumberFormatException e) {
                intoperand = CardFactoryUtil.xCount(getHostCard(), getHostCard().getSVar(operand));
            }

            if (!Expressions.compare((Integer) runParams.get("DamageAmount"), operator, intoperand)) {
                return false;
            }
        }
        if (hasParam("IsCombat")) {
            if (getParam("IsCombat").equals("True")) {
                if (!((Boolean) runParams.get("IsCombat"))) {
                    return false;
                }
            } else {
                if ((Boolean) runParams.get("IsCombat")) {
                    return false;
                }
            }
        }
        if (hasParam("IsEquipping") && !getHostCard().isEquipping()) {
            return false;
        }

        // check for DamageRedirection, the Thing where the damage is redirected to must be a creature or planeswalker or a player 
        if (hasParam("DamageTarget")) {
            String def = getParam("DamageTarget");

            for (Player p : AbilityUtils.getDefinedPlayers(hostCard, def, null)) {
                if (!p.getGame().getPlayers().contains(p)) {
                    return false;
                }
            }
            for (Card c : AbilityUtils.getDefinedCards(hostCard, def, null)) {
                if (!c.isCreature() && !c.isPlaneswalker()) {
                    return false;
                }
                if (!c.isInPlay()) {
                    return false;
                }
            }
        }

        return true;
    }


    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#setReplacingObjects(java.util.HashMap, forge.card.spellability.SpellAbility)
     */
    @Override
    public void setReplacingObjects(Map<String, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject("DamageAmount", runParams.get("DamageAmount"));
        sa.setReplacingObject("Target", runParams.get("Affected"));
        sa.setReplacingObject("Source", runParams.get("DamageSource"));
    }

}
