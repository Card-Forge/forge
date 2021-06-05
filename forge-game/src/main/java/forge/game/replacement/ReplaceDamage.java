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

import java.util.Map;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;

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
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        final Game game = getHostCard().getGame();

        if (((Integer) runParams.get(AbilityKey.DamageAmount)) == 0) {
            // If no actual damage is dealt, there is nothing to replace
            return false;
        }
        if (!matchesValidParam("ValidSource", runParams.get(AbilityKey.DamageSource))) {
            return false;
        }
        if (!matchesValidParam("ValidTarget", runParams.get(AbilityKey.Affected))) {
            return false;
        }
        if (!matchesValidParam("ValidCause", runParams.get(AbilityKey.Cause))) {
            return false;
        }
        if (hasParam("CauseIsSource")) {
            SpellAbility cause = (SpellAbility) runParams.get(AbilityKey.Cause);
            if (!cause.getHostCard().equals(runParams.get(AbilityKey.DamageSource))) {
                return false;
            }
        }
        if (hasParam("RelativeToSource")) {
            Card source = (Card) runParams.get(AbilityKey.DamageSource);
            String validRelative = getParam("RelativeToSource");
            if (!matchesValid(runParams.get(AbilityKey.DamageTarget), validRelative.split(","), source)) {
                return false;
            }
        }
        if (hasParam("DamageAmount")) {
            String full = getParam("DamageAmount");
            String operator = full.substring(0, 2);
            String operand = full.substring(2);
            int intoperand = AbilityUtils.calculateAmount(getHostCard(), operand, this);

            if (!Expressions.compare((Integer) runParams.get(AbilityKey.DamageAmount), operator, intoperand)) {
                return false;
            }
        }
        if (hasParam("IsCombat")) {
            if (getParam("IsCombat").equals("True") != ((Boolean) runParams.get(AbilityKey.IsCombat))) {
                return false;
            }
        }
        if (hasParam("IsEquipping") && !getHostCard().isEquipping()) {
            return false;
        }

        if (hasParam("DamageTarget")) {
            //Lava Burst and Whippoorwill check
            SpellAbility cause = (SpellAbility) runParams.get(AbilityKey.Cause);
            GameEntity affected = (GameEntity) runParams.get(AbilityKey.Affected);
            if (((cause != null) && (cause.hasParam("NoRedirection")) || (affected.hasKeyword("Damage that would be dealt to CARDNAME can't be redirected.")))) {
                return false;
            }
            // check for DamageRedirection, the Thing where the damage is redirected to must be a creature or planeswalker or a player
            String def = getParam("DamageTarget");
            if (def.startsWith("Replaced")) {
                // this can't work with the Defined below because the replaced objects aren't set to a possible SA yet
                if (def.equals("ReplacedSourceController")) {
                    Card source = (Card) runParams.get(AbilityKey.DamageSource);
                    if (!game.getPlayers().contains(source.getController())) {
                        return false;
                    }
                } else if (def.equals("ReplacedTargetController")) {
                    if (!(affected instanceof Card) || !game.getPlayers().contains(((Card) affected).getController())) {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                for (Player p : AbilityUtils.getDefinedPlayers(getHostCard(), def, null)) {
                    if (!game.getPlayers().contains(p)) {
                        return false;
                    }
                }
                for (Card c : AbilityUtils.getDefinedCards(getHostCard(), def, null)) {
                    if (!c.isCreature() && !c.isPlaneswalker()) {
                        return false;
                    }
                    if (!c.isInPlay()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }


    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#setReplacingObjects(java.util.HashMap, forge.card.spellability.SpellAbility)
     */
    @Override
    public void setReplacingObjects(Map<AbilityKey, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject(AbilityKey.DamageAmount, runParams.get(AbilityKey.DamageAmount));
        sa.setReplacingObject(AbilityKey.Target, runParams.get(AbilityKey.Affected));
        sa.setReplacingObject(AbilityKey.Source, runParams.get(AbilityKey.DamageSource));
    }

}
