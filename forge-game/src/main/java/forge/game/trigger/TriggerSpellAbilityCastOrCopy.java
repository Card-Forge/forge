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
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.util.Expressions;
import forge.util.Localizer;
import forge.util.collect.FCollection;

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
     * @param runParams
     **/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        final SpellAbility spellAbility = (SpellAbility) runParams.get(AbilityKey.SpellAbility);
        if (spellAbility == null) {
            System.out.println("TriggerSpellAbilityCast performTest encountered spellAbility == null. runParams2 = " + runParams);
            return false;
        }
        final Card cast = spellAbility.getHostCard();

        if (hasParam("ValidActivatingPlayer")) {
            Player activator = (Player) runParams.get(AbilityKey.Activator);

            if (!matchesValidParam("ValidActivatingPlayer", activator)) {
                return false;
            }
            if (hasParam("ActivatorThisTurnCast")) {
                final String compare = getParam("ActivatorThisTurnCast");
                final String valid = getParamOrDefault("ValidCard", "Card");
                List<Card> thisTurnCast = CardUtil.getThisTurnCast(valid, getHostCard(), this, getHostCard().getController());
                thisTurnCast = CardLists.filterControlledByAsList(thisTurnCast, activator);
                int left = thisTurnCast.size();
                int right = Integer.parseInt(compare.substring(2));
                if (!Expressions.compare(left, compare, right)) {
                    return false;
                }
            }
            if (hasParam("ActivatorThisTurnCastEach")) {
                final String compare = getParam("ActivatorThisTurnCastEach");
                final String valid = getParamOrDefault("ValidCard", "Card");
                boolean found = false;
                int right = Integer.parseInt(compare.substring(2));
                for (String v : valid.split(",")) {
                    if (!cast.isValid(v, getHostCard().getController(), getHostCard(), this)) {
                        continue;
                    }
                    List<Card> thisTurnCast = CardUtil.getThisTurnCast(v, getHostCard(), this, getHostCard().getController());
                    thisTurnCast = CardLists.filterControlledByAsList(thisTurnCast, activator);
                    int left = thisTurnCast.size();
                    if (Expressions.compare(left, compare, right)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
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
        if (!matchesValidParam("ValidSAonCard", spellAbility, cast)) {
            return false;
        }

        if (hasParam("TargetsValid")) {
            SpellAbility sa = spellAbility;

            boolean validTgtFound = false;
            while (sa != null && !validTgtFound) {
                for (final GameEntity ge : sa.getTargets().getTargetEntities()) {
                    if (matchesValidParam("TargetsValid", ge)) {
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

        if (hasParam("HasXManaCost")) {
            final int numX;
            if (spellAbility.isActivatedAbility()) {
                numX = spellAbility.getPayCosts().hasManaCost() ? spellAbility.getPayCosts().getCostMana().getAmountOfX() : 0;
            } else {
                numX = cast.getManaCost().countX();
            }
            if (numX == 0) {
                return false;
            }
        }

        // use numTargets instead?
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
                if (cast.getColor().sharesColorWith(ColorSet.fromMask(m.getColor()))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        if (getSpawningAbility() != null && getSpawningAbility().hasParam("TriggersWhenSpent")) {
            if (!getTriggerRemembered().contains(spellAbility)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        final SpellAbility cause = (SpellAbility) runParams.get(AbilityKey.SpellAbility);
        sa.setTriggeringObject(AbilityKey.Card, cause.getHostCard());
        sa.setTriggeringObject(AbilityKey.SpellAbility, cause);
        final List<TargetChoices> allTgts = cause.getAllTargetChoices();
        if (!allTgts.isEmpty()) {
            final FCollection<GameEntity> saTargets = new FCollection<>();
            for (TargetChoices tc : allTgts) {
                saTargets.addAll(tc.getTargetEntities());
            }
            sa.setTriggeringObject(AbilityKey.SpellAbilityTargets, saTargets);
        }
        sa.setTriggeringObject(AbilityKey.LifeAmount, cause.getAmountLifePaid());
        sa.setTriggeringObjectsFrom(
                runParams,
                AbilityKey.CardLKI,
                AbilityKey.Activator,
                AbilityKey.CurrentStormCount,
                AbilityKey.CurrentCastSpells
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
