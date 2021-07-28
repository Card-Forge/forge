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
package forge.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityView;
import forge.game.GameEntityViewMap;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardUtil;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.StackItemView;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbilityMustTarget;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.input.InputSelectTargets;
import forge.util.Aggregates;

/**
 * <p>
 * Target_Selection class.
 * </p>
 *
 * @author Forge
 * @version $Id: TargetSelection.java 25148 2014-03-12 08:28:52Z swordshine $
 */
public class TargetSelection {
    private final PlayerControllerHuman controller;
    private final SpellAbility ability;

    public TargetSelection(final PlayerControllerHuman controller, final SpellAbility currentAbility) {
        this.controller = controller;
        this.ability = currentAbility;
    }

    private final TargetRestrictions getTgt() {
        return this.ability.getTargetRestrictions();
    }

    private boolean bTargetingDone = false;

    private boolean isMandatory() {
        // even if its an optionalTrigger, the targeting is still mandatory
        return ability.isTrigger() || getTgt().getMandatory();
    }

    public final boolean chooseTargets(Integer numTargets, Collection<Integer> divisionValues, Predicate<GameObject> filter, boolean optional, boolean canFilterMustTarget) {
        if (!ability.usesTargeting()) {
            throw new RuntimeException("TargetSelection.chooseTargets called for ability that does not target - " + ability);
        }
        final TargetRestrictions tgt = getTgt();

        // Number of targets is explicitly set only if spell is being redirected (ex. Swerve or Redirect)
        final int minTargets = numTargets != null ? numTargets.intValue() : ability.getMinTargets();
        final int maxTargets = numTargets != null ? numTargets.intValue() : ability.getMaxTargets();
        //final int maxTotalCMC = tgt.getMaxTotalCMC(ability.getHostCard(), ability);
        final int numTargeted = ability.getTargets().size();
        final boolean isSingleZone = ability.getTargetRestrictions().isSingleZone();

        final boolean hasEnoughTargets = minTargets == 0 || numTargeted >= minTargets;
        final boolean hasAllTargets = numTargeted == maxTargets && maxTargets > 0;
        if (maxTargets == 0) { return true; }

        // if not enough targets chosen, cancel Ability
        if (this.bTargetingDone && !hasEnoughTargets) {
            return false;
        }

        if (this.bTargetingDone && hasEnoughTargets || hasAllTargets || ability.isDividedAsYouChoose() && divisionValues == null && ability.getStillToDivide() == 0) {
            return true;
        }

        final boolean hasCandidates = tgt.hasCandidates(this.ability, true);
        if (!hasCandidates && !hasEnoughTargets) {
            // Cancel ability if there aren't any valid Candidates
            return false;
        }
        if (isMandatory() && !hasCandidates && hasEnoughTargets) {
            // Mandatory target selection, that has no candidates but enough targets (Min == 0, but no choices)
            return true;
        }

        final List<ZoneType> zones = tgt.getZone();
        final boolean mandatory = isMandatory() && hasCandidates && !optional;

        final boolean choiceResult;
        if (tgt.isRandomTarget() && numTargets == null) {
            final List<GameEntity> candidates = tgt.getAllCandidates(this.ability, true);
            final GameObject choice = Aggregates.random(candidates);
            return ability.getTargets().add(choice);
        }
        else if (zones.size() == 1 && zones.get(0) == ZoneType.Stack) {
            // If Zone is Stack, the choices are handled slightly differently.
            // Handle everything inside function due to interaction with StackInstance
            return this.chooseCardFromStack(mandatory);
        }
        else {
            List<Card> validTargets = CardUtil.getValidCardsToTarget(tgt, ability);
            boolean mustTargetFiltered = false;
            if (canFilterMustTarget) {
                mustTargetFiltered = StaticAbilityMustTarget.filterMustTargetCards(controller.getPlayer(), validTargets, ability);
            }
            if (filter != null) {
                validTargets = new CardCollection(Iterables.filter(validTargets, filter));
            }

            // single zone
            if (isSingleZone) {
                final List<Card> removeCandidates = new ArrayList<>();
                final Card firstTgt = ability.getTargets().getFirstTargetedCard();
                if (firstTgt != null) {
                    for (Card t : validTargets) {
                        if (!t.getController().equals(firstTgt.getController())) {
                            removeCandidates.add(t);
                        }
                    }
                    validTargets.removeAll(removeCandidates);
                }
            }
            if (validTargets.isEmpty()) {
                // If all targets are filtered after applying MustTarget static ability, the spell can't be cast or the ability can't be activated
                if (mustTargetFiltered) {
                    return false;
                }
                //if no valid cards to target and only one valid non-card, auto-target the non-card
                //this handles "target opponent" cards, along with any other cards that can only target a single non-card game entity
                //note that we don't handle auto-targeting cards this way since it's possible that the result will be undesirable
                List<GameEntity> nonCardTargets = tgt.getAllCandidates(this.ability, true, true);
                if (nonCardTargets.size() == 1 && minTargets != 0) {
                    return ability.getTargets().add(nonCardTargets.get(0));
                }
            }
            else if (validTargets.size() == 1 && minTargets != 0 && ability.isTrigger() && !tgt.canTgtPlayer()) {
                //if only one valid target card for triggered ability, auto-target that card
                //only do this for triggered abilities to prevent auto-targeting when user chooses
                //to play a spell or activat an ability
                if (ability.isDividedAsYouChoose()) {
                    ability.addDividedAllocation(validTargets.get(0), ability.getStillToDivide());
                }
                return ability.getTargets().add(validTargets.get(0));
            }
            final Map<PlayerView, Object> playersWithValidTargets = Maps.newHashMap();
            for (Card card : validTargets) {
                playersWithValidTargets.put(PlayerView.get(card.getController()), null);
            }

            PlayerView playerView = controller.getLocalPlayerView();
            PlayerZoneUpdates playerZoneUpdates = controller.getGui().openZones(playerView, zones, playersWithValidTargets);
            if (!zones.contains(ZoneType.Stack)) {
                InputSelectTargets inp = new InputSelectTargets(controller, validTargets, ability, mandatory, divisionValues, filter, mustTargetFiltered);
                inp.showAndWait();
                choiceResult = !inp.hasCancelled();
                bTargetingDone = inp.hasPressedOk();
                controller.getGui().restoreOldZones(playerView, playerZoneUpdates);
            } else {
                // for every other case an all-purpose GuiChoose
                choiceResult = this.chooseCardFromList(validTargets, true, mandatory);
            }
        }
        // some inputs choose cards one-by-one and need to be called again
        return choiceResult && chooseTargets(numTargets, divisionValues, filter, optional, canFilterMustTarget);
    }

    private final boolean chooseCardFromList(final List<Card> choices, final boolean targeted, final boolean mandatory) {
        // Send in a list of valid cards, and popup a choice box to target
        final Game game = ability.getActivatingPlayer().getGame();

        GameEntityViewMap<Card, CardView> gameCacheChooseCard = GameEntityView.getMap(choices);

        final List<CardView> crdsBattle  = Lists.newArrayList();
        final List<CardView> crdsExile   = Lists.newArrayList();
        final List<CardView> crdsGrave   = Lists.newArrayList();
        final List<CardView> crdsLibrary = Lists.newArrayList();
        final List<CardView> crdsStack   = Lists.newArrayList();
        final List<CardView> crdsAnte    = Lists.newArrayList();
        for (final Card inZone : choices) {
            Zone zz = game.getZoneOf(inZone);
            CardView cardView = CardView.get(inZone);
            if (this.ability.getTargetRestrictions() != null && this.ability.getTargetRestrictions().isWithSameCreatureType()) {
                Card firstTgt = this.ability.getTargetCard();
                if (firstTgt != null && !firstTgt.sharesCreatureTypeWith(inZone)) {
                    continue;
                }
            }
            if (zz.is(ZoneType.Battlefield))    crdsBattle.add(cardView);
            else if (zz.is(ZoneType.Exile))     crdsExile.add(cardView);
            else if (zz.is(ZoneType.Graveyard)) crdsGrave.add(cardView);
            else if (zz.is(ZoneType.Library))   crdsLibrary.add(cardView);
            else if (zz.is(ZoneType.Stack))     crdsStack.add(cardView);
            else if (zz.is(ZoneType.Ante))      crdsAnte.add(cardView);
        }
        List<Object> choicesFiltered = new ArrayList<>();
        if (!crdsBattle.isEmpty()) {
            choicesFiltered.add("--CARDS ON BATTLEFIELD:--");
            choicesFiltered.addAll(crdsBattle);
        }
        if (!crdsExile.isEmpty()) {
            choicesFiltered.add("--CARDS IN EXILE:--");
            choicesFiltered.addAll(crdsExile);
        }
        if (!crdsGrave.isEmpty()) {
            choicesFiltered.add("--CARDS IN GRAVEYARD:--");
            choicesFiltered.addAll(crdsGrave);
        }
        if (!crdsLibrary.isEmpty()) {
            choicesFiltered.add("--CARDS IN LIBRARY:--");
            choicesFiltered.addAll(crdsLibrary);
        }
        if (!crdsStack.isEmpty()) {
            choicesFiltered.add("--CARDS IN STACK:--");
            choicesFiltered.addAll(crdsStack);
        }
        if (!crdsAnte.isEmpty()) {
            choicesFiltered.add("--CARDS IN ANTE:--");
            choicesFiltered.addAll(crdsAnte);
        }

        final String msgDone = "[FINISH TARGETING]";
        if (ability.isMinTargetChosen()) {
            // is there a more elegant way of doing this?
            choicesFiltered.add(msgDone);
        }

        Object chosen = null;
        if (!choices.isEmpty() && mandatory) {
            chosen = controller.getGui().one(getTgt().getVTSelection(), choicesFiltered);
        } else {
            chosen = controller.getGui().oneOrNone(getTgt().getVTSelection(), choicesFiltered);
        }
        if (chosen == null) {
            return false;
        }
        if (msgDone.equals(chosen)) {
            bTargetingDone = true;
            return true;
        }

        if (chosen instanceof CardView) {
            if (!gameCacheChooseCard.containsKey(chosen)) {
                return false;
            }
            if (((CardView) chosen).getZone().equals(ZoneType.Stack)) {
                for (final SpellAbilityStackInstance si : game.getStack()) {
                    SpellAbility abilityOnStack = si.getSpellAbility(true);
                    if (si.compareToSpellAbility(ability)) {
                        // By peeking at stack item, target is set to its SI state. So set it back before adding targets
                        ability.resetTargets();
                    }
                    if (abilityOnStack.getHostCard().getView().equals(chosen)) {
                        ability.getTargets().add(abilityOnStack);
                        break;
                    }
                }
            } else {
                ability.getTargets().add(gameCacheChooseCard.get((CardView) chosen));
            }
        }
        return true;
    }

    private final boolean chooseCardFromStack(final boolean mandatory) {
        final TargetRestrictions tgt = this.getTgt();
        final String message = tgt.getVTSelection();
        // Find what's targetable, then allow human to choose
        final List<Object> selectOptions = new ArrayList<>();
        HashMap<StackItemView, SpellAbilityStackInstance> stackItemViewCache = new HashMap<>();

        final Game game = ability.getActivatingPlayer().getGame();
        for (final SpellAbilityStackInstance si : game.getStack()) {
            SpellAbility abilityOnStack = si.getSpellAbility(true);
            if (si.compareToSpellAbility(ability)) {
                // By peeking at stack item, target is set to its SI state. So set it back before adding targets
                ability.resetTargets();
            }
            if (ability.canTargetSpellAbility(abilityOnStack)) {
                stackItemViewCache.put(si.getView(), si);
                selectOptions.add(si.getView());
            }
        }

        while (!bTargetingDone) {
            if (ability.isMaxTargetChosen()) {
                bTargetingDone = true;
                return true;
            }

            if (!selectOptions.contains("[FINISH TARGETING]") && ability.isMinTargetChosen()) {
                selectOptions.add("[FINISH TARGETING]");
            }

            if (selectOptions.isEmpty()) {
                // Not enough targets, cancel targeting
                return false;
            }
            Object madeChoice = mandatory ? controller.getGui().one(message, selectOptions) : controller.getGui().oneOrNone(message, selectOptions);
            if (madeChoice == null) {
                return false;
            }
            if (madeChoice instanceof StackItemView) {
                ability.getTargets().add(stackItemViewCache.get(madeChoice).getSpellAbility(true));
            }
            else {// 'FINISH TARGETING' chosen
                bTargetingDone = true;
            }
        }
        return true;
    }
}
