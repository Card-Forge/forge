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
package forge.game.trigger;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import forge.card.ColorSet;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.cost.Cost;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;
import forge.util.Expressions;
import forge.util.Localizer;

/**
 * <p>
 * Trigger_SpellAbilityCast class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class TriggerSpellAbilityCastOrCopy extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_SpellAbilityCast.
     * </p>
     *
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerSpellAbilityCastOrCopy(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        final SpellAbility spellAbility = (SpellAbility) runParams.get(AbilityKey.CastSA);
        if (spellAbility == null) {
            System.out.println("TriggerSpellAbilityCast performTest encountered spellAbility == null. runParams2 = " + runParams);
            return false;
        }
        final Card cast = spellAbility.getHostCard();
        final Game game = cast.getGame();
        final SpellAbilityStackInstance si = game.getStack().getInstanceFromSpellAbility(spellAbility);

        if (hasParam("ActivatedOnly")) {
            if (spellAbility.isTrigger()) {
                return false;
            }
        }

        if (!matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Player))) {
            return false;
        }

        if (!matchesValidParam("ValidControllingPlayer", cast.getController())) {
            return false;
        }

        if (hasParam("ValidControllingPlayer")) {
            if (!matchesValid(cast.getController(), getParam("ValidControllingPlayer").split(","))) {
                return false;
            }
        }

        if (hasParam("ValidActivatingPlayer")) {
            if (si == null || !matchesValid(si.getSpellAbility(true).getActivatingPlayer(), getParam("ValidActivatingPlayer").split(","))) {
                return false;
            }
            if (hasParam("ActivatorThisTurnCast")) {
                final String compare = getParam("ActivatorThisTurnCast");
                final String valid = hasParam("ValidCard") ? getParam("ValidCard") : "Card";
                List<Card> thisTurnCast = CardUtil.getThisTurnCast(valid, getHostCard(), this);
                thisTurnCast = CardLists.filterControlledBy(thisTurnCast, si.getSpellAbility(true).getActivatingPlayer());
                int left = thisTurnCast.size();
                int right = Integer.parseInt(compare.substring(2));
                if (!Expressions.compare(left, compare, right)) {
                    return false;
                }
            }
        }
        if (!matchesValidParam("ValidCard", cast)) {
            return false;
        }
        if (!matchesValidParam("ValidSA", spellAbility)) {
            return false;
        }

        if (hasParam("TargetsValid")) {
            SpellAbility sa = spellAbility;
            if (si != null) {
                sa = si.getSpellAbility(true);
            }

            boolean validTgtFound = false;
            while (sa != null && !validTgtFound) {
                for (final Card tgt : sa.getTargets().getTargetCards()) {

                    if (matchesValid(tgt, getParam("TargetsValid").split(","))) {
                        validTgtFound = true;
                        if (this.hasParam("RememberValidCards")) {
                            this.getHostCard().addRemembered(tgt);
                        } else break;
                    }
                }

                for (final Player p : sa.getTargets().getTargetPlayers()) {
                    if (matchesValid(p, getParam("TargetsValid").split(","))) {
                        validTgtFound = true;
                        break;
                    }
                }
                sa = sa.getSubAbility();
            }
            if (!validTgtFound) {
                 return false;
            }
        }

        if (hasParam("CanTargetOtherCondition")) {
            final CardCollection candidates = new CardCollection();
            SpellAbility targetedSA = spellAbility;
            while (targetedSA != null) {
                if (targetedSA.usesTargeting() && targetedSA.getTargets().size() != 0) {
                    break;
                }
                targetedSA = targetedSA.getSubAbility();
            }
            if (targetedSA == null) {
                return false;
            }
            final List<GameEntity> candidateTargets = targetedSA.getTargetRestrictions().getAllCandidates(targetedSA, true);
            for (GameEntity card : candidateTargets) {
                if (card instanceof Card) {
                    candidates.add((Card) card);
                }
            }
            candidates.removeAll(targetedSA.getTargets().getTargetCards());
            String valid = getParam("CanTargetOtherCondition");
            if (CardLists.getValidCards(candidates, valid, spellAbility.getActivatingPlayer(), spellAbility.getHostCard(), spellAbility).isEmpty()) {
                return false;
            }
        }

        if (hasParam("NonTapCost")) {
            final Cost cost = (Cost) (runParams.get(AbilityKey.Cost));
            if (cost.hasTapCost()) {
                return false;
            }
        }

        if (hasParam("HasTapCost")) {
            final Cost cost = (Cost) (runParams.get(AbilityKey.Cost));
            if (!cost.hasTapCost()) {
                return false;
            }
        }

        if (hasParam("HasXManaCost")) {
            final Cost cost = (Cost) (runParams.get(AbilityKey.Cost));
            if (cost.hasNoManaCost()) {
                return false;
            }
            if (cost.getCostMana().getAmountOfX() <= 0) {
                return false;
            }
        }

        if (hasParam("HasNoManaCost")) {
            final Cost cost = (Cost) (runParams.get(AbilityKey.Cost));
            if (!cost.getTotalMana().isZero()) {
                return false;
            }
        }

        if (hasParam("Outlast")) {
            if (!spellAbility.isOutlast()) {
                return false;
            }
        }

        if (hasParam("EternalizeOrEmbalm")) {
            if (!spellAbility.hasParam("Eternalize") && !spellAbility.hasParam("Embalm")) {
                return false;
            }
        }

        if (hasParam("IsSingleTarget")) {
            Set<GameObject> targets = Sets.newHashSet();
            for (TargetChoices tc : spellAbility.getAllTargetChoices()) {
                targets.addAll(tc);
                if (targets.size() > 1) {
                    return false;
                }
            }
            if (targets.size() != 1) {
                return false;
            }
        }

        if (hasParam("SharesNameWithActivatorsZone")) {
            String zones = getParam("SharesNameWithActivatorsZone");
            if (si == null) {
                return false;
            }
            boolean sameNameFound = false;
            for (Card c: si.getSpellAbility(true).getActivatingPlayer().getCardsIn(ZoneType.listValueOf(zones))) {
                if (cast.getName().equals(c.getName())) {
                    sameNameFound = true;
                    break;
                }
            }
            if (!sameNameFound) {
                return false;
            }
        }

        if (hasParam("NoColoredMana")) {
            for (Mana m : spellAbility.getPayingMana()) {
                if (!m.isColorless()) {
                    return false;
                }
            }
        }

        if (hasParam("SnowSpentForCardsColor")) {
            boolean found = false;
            for (Mana m : spellAbility.getPayingMana()) {
                if (!m.isSnow()) {
                    continue;
                }
                if (cast.determineColor().sharesColorWith(ColorSet.fromMask(m.getColor()))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        final SpellAbility castSA = (SpellAbility) runParams.get(AbilityKey.CastSA);
        final SpellAbilityStackInstance si = sa.getHostCard().getGame().getStack().getInstanceFromSpellAbility(castSA);
        sa.setTriggeringObject(AbilityKey.Card, castSA.getHostCard());
        sa.setTriggeringObject(AbilityKey.SpellAbility, castSA.copy(castSA.getHostCard(), true));
        sa.setTriggeringObject(AbilityKey.StackInstance, si);
        sa.setTriggeringObject(AbilityKey.SpellAbilityTargetingCards, (si != null ? si.getSpellAbility(true) : castSA).getTargets().getTargetCards());
        sa.setTriggeringObjectsFrom(
                runParams,
            AbilityKey.Player,
            AbilityKey.Activator,
            AbilityKey.CurrentStormCount,
            AbilityKey.CurrentCastSpells,
            AbilityKey.CastSACMC
        );
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblCard")).append(": ").append(sa.getTriggeringObject(AbilityKey.Card)).append(", ");
        sb.append(Localizer.getInstance().getMessage("lblActivator")).append(": ").append(sa.getTriggeringObject(AbilityKey.Activator)).append(", ");
        sb.append(Localizer.getInstance().getMessage("lblSpellAbility")).append(": ").append(sa.getTriggeringObject(AbilityKey.SpellAbility));
        return sb.toString();
    }
}
