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
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.match.input.InputSelectTargets;
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

    public final boolean chooseTargets(Integer numTargets) {
        final TargetRestrictions tgt = getTgt();
        final boolean canTarget = tgt != null && tgt.doesTarget();
        if (!canTarget) {
            throw new RuntimeException("TargetSelection.chooseTargets called for ability that does not target - " + ability);
        }
        
        // Number of targets is explicitly set only if spell is being redirected (ex. Swerve or Redirect) 
        final int minTargets = numTargets != null ? numTargets.intValue() : tgt.getMinTargets(ability.getHostCard(), ability);
        final int maxTargets = numTargets != null ? numTargets.intValue() : tgt.getMaxTargets(ability.getHostCard(), ability);
        //final int maxTotalCMC = tgt.getMaxTotalCMC(ability.getHostCard(), ability);
        final int numTargeted = ability.getTargets().getNumTargeted();

        final boolean hasEnoughTargets = minTargets == 0 || numTargeted >= minTargets;
        final boolean hasAllTargets = numTargeted == maxTargets && maxTargets > 0;
        if (maxTargets == 0) { return true; }

        // if not enough targets chosen, cancel Ability
        if (this.bTargetingDone && !hasEnoughTargets) {
            return false;
        }

        if (this.bTargetingDone && hasEnoughTargets || hasAllTargets || tgt.isDividedAsYouChoose() && tgt.getStillToDivide() == 0) {
            return true;
        }

        final boolean hasCandidates = tgt.hasCandidates(this.ability, true);
        if (!hasCandidates && !hasEnoughTargets) {
            // Cancel ability if there aren't any valid Candidates
            return false;
        }
        if (tgt.getMandatory() && !hasCandidates && hasEnoughTargets) {
            // Mandatory target selection, that has no candidates but enough targets (Min == 0, but no choices)
            return true;
        }
        
        final List<ZoneType> zone = tgt.getZone();
        final boolean mandatory = tgt.getMandatory() && hasCandidates;
        
        final boolean choiceResult;
        final boolean random = tgt.isRandomTarget();
        if (random) {
            final List<GameEntity> candidates = tgt.getAllCandidates(this.ability, true);
            final GameObject choice = Aggregates.random(candidates);
            return ability.getTargets().add(choice);
        }
        else if (zone.size() == 1 && zone.get(0) == ZoneType.Stack) {
            // If Zone is Stack, the choices are handled slightly differently.
            // Handle everything inside function due to interaction with StackInstance
            return this.chooseCardFromStack(mandatory);
        }
        else {
            final List<Card> validTargets = CardUtil.getValidCardsToTarget(tgt, ability);
            if (validTargets.isEmpty()) {
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
                if (tgt.isDividedAsYouChoose()) {
                    tgt.addDividedAllocation(validTargets.get(0), tgt.getStillToDivide());
                }
                return ability.getTargets().add(validTargets.get(0));
            }
            final Map<PlayerView, Object> playersWithValidTargets = Maps.newHashMap();
            for (Card card : validTargets) {
                playersWithValidTargets.put(PlayerView.get(card.getController()), null);
            }
            if (controller.getGui().openZones(zone, playersWithValidTargets)) {
                InputSelectTargets inp = new InputSelectTargets(controller, validTargets, ability, mandatory);
                inp.showAndWait();
                choiceResult = !inp.hasCancelled();
                bTargetingDone = inp.hasPressedOk();
                controller.getGui().restoreOldZones(playersWithValidTargets);
            }
            else {
                // for every other case an all-purpose GuiChoose
                choiceResult = this.chooseCardFromList(validTargets, true, mandatory);
            }
        }
        // some inputs choose cards one-by-one and need to be called again 
        return choiceResult && chooseTargets(numTargets);
    }

    private final boolean chooseCardFromList(final List<Card> choices, final boolean targeted, final boolean mandatory) {
        // Send in a list of valid cards, and popup a choice box to target
        final Game game = ability.getActivatingPlayer().getGame();

        final List<Card> crdsBattle  = Lists.newArrayList();
        final List<Card> crdsExile   = Lists.newArrayList();
        final List<Card> crdsGrave   = Lists.newArrayList();
        final List<Card> crdsLibrary = Lists.newArrayList();
        final List<Card> crdsStack   = Lists.newArrayList();
        final List<Card> crdsAnte    = Lists.newArrayList();
        for (final Card inZone : choices) {
            Zone zz = game.getZoneOf(inZone);
            if (zz.is(ZoneType.Battlefield))    crdsBattle.add(inZone);
            else if (zz.is(ZoneType.Exile))     crdsExile.add(inZone);
            else if (zz.is(ZoneType.Graveyard)) crdsGrave.add(inZone);
            else if (zz.is(ZoneType.Library))   crdsLibrary.add(inZone);
            else if (zz.is(ZoneType.Stack))     crdsStack.add(inZone);
            else if (zz.is(ZoneType.Ante))      crdsAnte.add(inZone);
        }
        List<Object> choicesFiltered = new ArrayList<Object>();
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
        if (this.getTgt().isMinTargetsChosen(this.ability.getHostCard(), this.ability)) {
            // is there a more elegant way of doing this?
            choicesFiltered.add(msgDone);
        }
        
        Object chosen = null;
        if (!choices.isEmpty() && mandatory) {
            chosen = controller.getGui().one(getTgt().getVTSelection(), choicesFiltered);
        }
        else {
            chosen = controller.getGui().oneOrNone(getTgt().getVTSelection(), choicesFiltered);
        }
        if (chosen == null) {
            return false;
        }
        if (msgDone.equals(chosen)) {
            bTargetingDone = true;
            return true;
        }

        if (chosen instanceof Card) {
            ability.getTargets().add((Card) chosen);
        }
        return true;
    }

    private final boolean chooseCardFromStack(final boolean mandatory) {
        final TargetRestrictions tgt = this.getTgt();
        final String message = tgt.getVTSelection();
        // Find what's targetable, then allow human to choose
        final List<Object> selectOptions = new ArrayList<Object>();

        final Game game = ability.getActivatingPlayer().getGame();
        for (final SpellAbilityStackInstance si : game.getStack()) {
            SpellAbility abilityOnStack = si.getSpellAbility(true);
            if (ability.equals(abilityOnStack)) {
                // By peeking at stack item, target is set to its SI state. So set it back before adding targets
                ability.resetTargets();
            }
            else if (ability.canTargetSpellAbility(abilityOnStack)) {
                selectOptions.add(si);
            }
        }

        while(!bTargetingDone) {
            if (tgt.isMaxTargetsChosen(this.ability.getHostCard(), this.ability)) {
                bTargetingDone = true;
                return true;
            }

            if (!selectOptions.contains("[FINISH TARGETING]") && tgt.isMinTargetsChosen(this.ability.getHostCard(), this.ability)) {
                selectOptions.add("[FINISH TARGETING]");
            }

            if (selectOptions.isEmpty()) {
                // Not enough targets, cancel targeting
                return false;
            }
            else {
                Object madeChoice = mandatory ? controller.getGui().one(message, selectOptions) : controller.getGui().oneOrNone(message, selectOptions);
                if (madeChoice == null) {
                    return false;
                }
                if (madeChoice instanceof SpellAbilityStackInstance) {
                    ability.getTargets().add(((SpellAbilityStackInstance)madeChoice).getSpellAbility(true));
                }
                else {// 'FINISH TARGETING' chosen 
                    bTargetingDone = true;
                }
            }
        }
        return true;
    }
}
