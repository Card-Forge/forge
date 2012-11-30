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
package forge.card.staticability;

import java.util.HashMap;

import forge.Card;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The Class StaticAbility_CantBeCast.
 */
public class StaticAbilityCostChange {

    /**
     * Applies applyRaiseCostAbility ability.
     * 
     * @param staticAbility
     *            a StaticAbility
     * @param sa
     *            the SpellAbility
     * @param originalCost
     *            a ManaCost
     */
    public static ManaCost applyRaiseCostAbility(final StaticAbility staticAbility, final SpellAbility sa
            , final ManaCost originalCost) {
        final HashMap<String, String> params = staticAbility.getMapParams();
        final Card hostCard = staticAbility.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Card card = sa.getSourceCard();
        final String amount = params.get("Amount");
        final ManaCost manaCost = new ManaCost(originalCost.toString());

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
            return originalCost;
        }

        if (params.containsKey("Activator") && ((activator == null)
                || !activator.isValid(params.get("Activator"), hostCard.getController(), hostCard))) {
            return originalCost;
        }

        if (params.containsKey("Type")) {
            if (params.get("Type").equals("Spell")) {
                if (!sa.isSpell()) {
                    return originalCost;
                }
            } else if (params.get("Type").equals("Ability")) {
                if (!(sa instanceof AbilityActivated)) {
                    return originalCost;
                }
            } else if (params.get("Type").equals("NonManaAbility")) {
                    if (!(sa instanceof AbilityActivated) || null != sa.getManaPart()) {
                        return originalCost;
                    }
            } else if (params.get("Type").equals("Flashback")) {
                    if (!sa.isFlashBackAbility()) {
                        return originalCost;
                    }
            }
        }
        if (params.containsKey("AffectedZone") && !card.isInZone(ZoneType.smartValueOf(params.get("AffectedZone")))) {
            return originalCost;
        }
        if (params.containsKey("ValidTarget")) {
            Target tgt = sa.getTarget();
            if (tgt == null) {
                return originalCost;
            }
            boolean targetValid = false;
            for (Object target : tgt.getTargets()) {
                if (target instanceof Card) {
                    Card targetCard = (Card) target;
                    if (targetCard.isValid(params.get("ValidTarget").split(","), hostCard.getController(), hostCard)) {
                        targetValid = true;
                    }
                }
            }
            if (!targetValid) {
                return originalCost;
            }
        }
        if (params.containsKey("ValidSpellTarget")) {
            Target tgt = sa.getTarget();
            if (tgt == null) {
                return originalCost;
            }
            boolean targetValid = false;
            for (Object target : tgt.getTargets()) {
                if (target instanceof SpellAbility) {
                    Card targetCard = ((SpellAbility) target).getSourceCard();
                    if (targetCard.isValid(params.get("ValidSpellTarget").split(","), hostCard.getController(), hostCard)) {
                        targetValid = true;
                    }
                }
            }
            if (!targetValid) {
                return originalCost;
            }
        }
        int value = 0;
        if ("X".equals(amount)) {
            value = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X"));
        } else if ("Y".equals(amount)) {
            value = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("Y"));
        } else if ("Min3".equals(amount)) {
            int cmc = manaCost.getConvertedManaCost();
            if (cmc < 3) {
                value = 3 - cmc;
            }
        } else {
            value = AbilityFactory.calculateAmount(card, amount, sa);
            //value = Integer.valueOf(amount);
        }

        if (!params.containsKey("Color")) {
            manaCost.increaseColorlessMana(value);
            if (manaCost.toString().equals("0") && params.containsKey("MinMana")) {
                manaCost.increaseColorlessMana(Integer.valueOf(params.get("MinMana")));
            }
        } else {
            if (params.get("Color").equals("W")) {
                manaCost.increaseShard(ManaCostShard.WHITE, value);
            } else if (params.get("Color").equals("B")) {
                manaCost.increaseShard(ManaCostShard.BLACK, value);
            } else if (params.get("Color").equals("U")) {
                manaCost.increaseShard(ManaCostShard.BLUE, value);
            } else if (params.get("Color").equals("R")) {
                manaCost.increaseShard(ManaCostShard.RED, value);
            } else if (params.get("Color").equals("G")) {
                manaCost.increaseShard(ManaCostShard.GREEN, value);
            }
        }

        return manaCost;
    }

    /**
     * Applies applyReduceCostAbility ability.
     * 
     * @param staticAbility
     *            a StaticAbility
     * @param sa
     *            the SpellAbility
     * @param originalCost
     *            a ManaCost
     */
    public static ManaCost applyReduceCostAbility(final StaticAbility staticAbility, final SpellAbility sa
            , final ManaCost originalCost) {
        //Can't reduce zero cost
        if (originalCost.toString().equals("0")) {
            return originalCost;
        }
        final HashMap<String, String> params = staticAbility.getMapParams();
        final Card hostCard = staticAbility.getHostCard();
        final Player activator = sa.getActivatingPlayer();
        final Card card = sa.getSourceCard();
        final String amount = params.get("Amount");
        final ManaCost manaCost = new ManaCost(originalCost.toString());

        if (params.containsKey("ValidCard")
                && !card.isValid(params.get("ValidCard").split(","), hostCard.getController(), hostCard)) {
            return originalCost;
        }
        if (params.containsKey("Activator") && ((activator == null)
                || !activator.isValid(params.get("Activator"), hostCard.getController(), hostCard))) {
            return originalCost;
        }
        if (params.containsKey("Type")) {
            if (params.get("Type").equals("Spell")) {
                if (!sa.isSpell()) {
                    return originalCost;
                }
            } else if (params.get("Type").equals("Ability")) {
                if (!(sa instanceof AbilityActivated)) {
                    return originalCost;
                }
            } else if (params.get("Type").equals("Cycling")) {
                if (!sa.isCycling()) {
                    return originalCost;
                }
            } else if (params.get("Type").equals("Equip")) {
                if (!(sa instanceof AbilityActivated) || !sa.hasParam("Equip")) {
                    return originalCost;
                }
            } else if (params.get("Type").equals("Flashback")) {
                if (!sa.isFlashBackAbility()) {
                    return originalCost;
                }
            }
        }
        if (params.containsKey("ValidTarget")) {
            Target tgt = sa.getTarget();
            if (tgt == null) {
                return originalCost;
            }
            boolean targetValid = false;
            for (Object target : tgt.getTargets()) {
                if (target instanceof Card) {
                    Card targetCard = (Card) target;
                    if (targetCard.isValid(params.get("ValidTarget").split(","), hostCard.getController(), hostCard)) {
                        targetValid = true;
                    }
                }
            }
            if (!targetValid) {
                return originalCost;
            }
        }
        if (params.containsKey("AffectedZone") && !card.isInZone(ZoneType.smartValueOf(params.get("AffectedZone")))) {
            return originalCost;
        }
        int value = 0;
        if ("X".equals(amount)) {
            value = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X"));
        } else {
            value = Integer.valueOf(amount);
        }

        if (!params.containsKey("Color")) {
            manaCost.decreaseColorlessMana(value);
            if (manaCost.toString().equals("0") && params.containsKey("MinMana")) {
                manaCost.increaseColorlessMana(Integer.valueOf(params.get("MinMana")));
            }
        } else {
            if (params.get("Color").equals("W")) {
                manaCost.decreaseShard(ManaCostShard.WHITE, value);
            } else if (params.get("Color").equals("B")) {
                manaCost.decreaseShard(ManaCostShard.BLACK, value);
            } else if (params.get("Color").equals("G")) {
                manaCost.decreaseShard(ManaCostShard.GREEN, value);
            }
        }


        return manaCost;
    }
}
