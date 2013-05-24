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
package forge.card.spellability;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.card.ability.AbilityUtils;
import forge.control.input.InputSelectTargets;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.Aggregates;

/**
 * <p>
 * Target_Selection class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TargetSelection {
    private final SpellAbility ability;


    public TargetSelection(final SpellAbility sa) {
        this.ability = sa;
    }

    private final Target getTgt() {
        return this.ability.getTarget();
    }

    private boolean bTargetingDone = false;


    /**
     * <p>
     * resetTargets.
     * </p>
     */

    public final boolean chooseTargets() {
        Target tgt = getTgt();
        final boolean canTarget = tgt != null && tgt.doesTarget();
        if( !canTarget )
            throw new RuntimeException("TargetSelection.chooseTargets called for ability that does not target - " + ability);
        
        final int minTargets = tgt.getMinTargets(ability.getSourceCard(), ability);
        final int maxTargets = tgt.getMaxTargets(ability.getSourceCard(), ability);
        final int numTargeted = tgt.getNumTargeted();

        boolean hasEnoughTargets = minTargets == 0 || numTargeted >= minTargets;
        boolean hasAllTargets = numTargeted == maxTargets && maxTargets > 0;

        // if not enough targets chosen, cancel Ability
        if (this.bTargetingDone && !hasEnoughTargets)
            return false;
        

        if (this.bTargetingDone && hasEnoughTargets || hasAllTargets || tgt.isDividedAsYouChoose() && tgt.getStillToDivide() == 0) {
            return true;
        }

        if (!tgt.hasCandidates(this.ability, true) && !hasEnoughTargets) {
            // Cancel ability if there aren't any valid Candidates
            return false;
        }
        
        final List<ZoneType> zone = tgt.getZone();
        final boolean mandatory = tgt.getMandatory() && tgt.hasCandidates(this.ability, true);
        
        final boolean choiceResult;
        final boolean random = tgt.isRandomTarget();
        if (random) {
            List<Object> candidates = tgt.getAllCandidates(this.ability, true);
            Object choice = Aggregates.random(candidates);
            return tgt.addTarget(choice);
        } else if (zone.size() == 1 && zone.get(0) == ZoneType.Stack) {
            // If Zone is Stack, the choices are handled slightly differently.
            // Handle everything inside function due to interaction with StackInstance
            return this.chooseCardFromStack(mandatory);
        } else {
            List<Card> validTargets = this.getValidCardsToTarget();
            if (zone.size() == 1 && zone.get(0) == ZoneType.Battlefield) {
                InputSelectTargets inp = new InputSelectTargets(validTargets, ability, mandatory);
                ability.getActivatingPlayer().getGame().getInputQueue().setInputAndWait(inp);
                choiceResult = !inp.hasCancelled();
                bTargetingDone = inp.hasPressedOk();
            } else {
                // for every other case an all-purpose GuiChoose
                choiceResult = this.chooseCardFromList(validTargets, true, mandatory);
            }
        }
        // some inputs choose cards one-by-one and need to be called again 
        return choiceResult && chooseTargets();
    }

    

    // these have been copied over from CardFactoryUtil as they need two extra
    // parameters for target selection.
    // however, due to the changes necessary for SA_Requirements this is much
    // different than the original

    /**
     * <p>
     * chooseValidInput.
     * </p>
     * @return 
     */
    private final List<Card> getValidCardsToTarget() {
        final Target tgt = this.getTgt();
        final GameState game = ability.getActivatingPlayer().getGame();
        final List<ZoneType> zone = tgt.getZone();

        final boolean canTgtStack = zone.contains(ZoneType.Stack);
        List<Card> validCards = CardLists.getValidCards(game.getCardsIn(zone), tgt.getValidTgts(), this.ability.getActivatingPlayer(), this.ability.getSourceCard());
        List<Card> choices = CardLists.getTargetableCards(validCards, this.ability);
        if (canTgtStack) {
            // Since getTargetableCards doesn't have additional checks if one of the Zones is stack
            // Remove the activating card from targeting itself if its on the Stack
            Card activatingCard = tgt.getSourceCard();
            if (activatingCard.isInZone(ZoneType.Stack)) {
                choices.remove(tgt.getSourceCard());
            }
        }
        List<Object> targetedObjects = this.ability.getUniqueTargets();

        if (tgt.isUniqueTargets()) {
            for (final Object o : targetedObjects) {
                if ((o instanceof Card) && targetedObjects.contains(o)) {
                    choices.remove(o);
                }
            }
        }

        // Remove cards already targeted
        final List<Card> targeted = tgt.getTargetCards();
        for (final Card c : targeted) {
            if (choices.contains(c)) {
                choices.remove(c);
            }
        }

        // If all cards (including subability targets) must have the same controller
        if (tgt.isSameController() && !targetedObjects.isEmpty()) {
            final List<Card> list = new ArrayList<Card>();
            for (final Object o : targetedObjects) {
                if (o instanceof Card) {
                    list.add((Card) o);
                }
            }
            if (!list.isEmpty()) {
                final Card card = list.get(0);
                choices = CardLists.filter(choices, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return c.sharesControllerWith(card);
                    }
                });
            }
        }
        // If second target has properties related to the first
        if (tgt.getRelatedProperty() != null && !targetedObjects.isEmpty()) {
            final List<Card> list = new ArrayList<Card>();
            final String related = tgt.getRelatedProperty();
            for (final Object o : targetedObjects) {
                if (o instanceof Card) {
                    list.add((Card) o);
                }
            }
            if (!list.isEmpty()) {
                final Card card = list.get(0);
                if ("LEPower".equals(related)) {
                    choices = CardLists.filter(choices, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return c.getNetAttack() <= card.getNetAttack();
                        }
                    });
                } 
                if ("LECMC".equals(related)) {
                    choices = CardLists.filter(choices, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return c.getCMC() <= card.getCMC();
                        }
                    });
                }
            }
        }
        // If all cards must be from the same zone
        if (tgt.isSingleZone() && !targeted.isEmpty()) {
            choices = CardLists.filterControlledBy(choices, targeted.get(0).getController());
        }
        // If all cards must be from different zones
        if (tgt.isDifferentZone() && !targeted.isEmpty()) {
            choices = CardLists.filterControlledBy(choices, targeted.get(0).getController().getOpponent());
        }
        // If all cards must have different controllers
        if (tgt.isDifferentControllers() && !targeted.isEmpty()) {
            final List<Player> availableControllers = new ArrayList<Player>(game.getPlayers());
            for (int i = 0; i < targeted.size(); i++) {
                availableControllers.remove(targeted.get(i).getController());
            }
            choices = CardLists.filterControlledBy(choices, availableControllers);
        }
        // If the cards can't share a creature type
        if (tgt.isWithoutSameCreatureType() && !targeted.isEmpty()) {
            final Card card = targeted.get(0);
            choices = CardLists.filter(choices, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return !c.sharesCreatureTypeWith(card);
                }
            });
        }
        // If the cards must have a specific controller
        if (tgt.getDefinedController() != null) {
            List<Player> pl = AbilityUtils.getDefinedPlayers(ability.getSourceCard(), tgt.getDefinedController(), this.ability);
            if (pl != null && !pl.isEmpty()) {
                Player controller = pl.get(0);
                choices = CardLists.filterControlledBy(choices, controller);
            } else {
                choices.clear();
            }
        }
        return choices;
    }

    /**
     * <p>
     * chooseCardFromList.
     * </p>
     * 
     * @param choices
     *            a {@link forge.CardList} object.
     * @param targeted
     *            a boolean.
     * @param mandatory
     *            a boolean.
     */
    private final boolean chooseCardFromList(final List<Card> choices, final boolean targeted, final boolean mandatory) {
        // Send in a list of valid cards, and popup a choice box to target
        final GameState game = ability.getActivatingPlayer().getGame(); 

        final List<Card> crdsBattle = new ArrayList<Card>();
        final List<Card> crdsExile = new ArrayList<Card>();
        final List<Card> crdsGrave = new ArrayList<Card>();
        final List<Card> crdsLibrary = new ArrayList<Card>();
        final List<Card> crdsStack = new ArrayList<Card>();
        for (final Card inZone : choices) {
            Zone zz = game.getZoneOf(inZone);
            if (zz.is(ZoneType.Battlefield))    crdsBattle.add(inZone);
            else if (zz.is(ZoneType.Exile))     crdsExile.add(inZone);
            else if (zz.is(ZoneType.Graveyard)) crdsGrave.add(inZone);
            else if (zz.is(ZoneType.Library))   crdsLibrary.add(inZone);
            else if (zz.is(ZoneType.Stack))     crdsStack.add(inZone);
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

        final String msgDone = "[FINISH TARGETING]";
        if (this.getTgt().isMinTargetsChosen(this.ability.getSourceCard(), this.ability)) {
            // is there a more elegant way of doing this?
            choicesFiltered.add(msgDone);
        }
        
        final Object chosen = GuiChoose.oneOrNone(getTgt().getVTSelection(), choicesFiltered);
        if (chosen == null) {
            return false;
        }
        if (msgDone.equals(chosen)) {
            bTargetingDone = true;
            return true;
        }
        
        if (chosen instanceof Card )
            this.getTgt().addTarget(chosen);
        return true;
    }

    /**
     * <p>
     * chooseCardFromStack.
     * </p>
     * 
     * @param mandatory
     *            a boolean.
     */
    private final boolean chooseCardFromStack(final boolean mandatory) {
        final Target tgt = this.getTgt();
        final String message = tgt.getVTSelection();
        // Find what's targetable, then allow human to choose
        final List<Object> selectOptions = new ArrayList<Object>();

        final GameState game = ability.getActivatingPlayer().getGame();
        for (SpellAbilityStackInstance si : game.getStack()) {
            SpellAbility abilityOnStack = si.getSpellAbility();
            if (ability.equals(abilityOnStack)) {
                // By peeking at stack item, target is set to its SI state. So set it back before adding targets
                tgt.resetTargets();
            }
            else if (ability.canTargetSpellAbility(abilityOnStack)) {
                selectOptions.add(abilityOnStack);
            }
        }

        while(!bTargetingDone) {
            if (tgt.isMaxTargetsChosen(this.ability.getSourceCard(), this.ability)) {
                bTargetingDone = true;
                return true;
            }

            if (!selectOptions.contains("[FINISH TARGETING]") && tgt.isMinTargetsChosen(this.ability.getSourceCard(), this.ability)) {
                selectOptions.add("[FINISH TARGETING]");
            }

            if (selectOptions.isEmpty()) {
                // Not enough targets, cancel targeting
                return false;
            } else {
                final Object madeChoice = GuiChoose.oneOrNone(message, selectOptions);
                if (madeChoice == null) {
                    return false;
                }
                if (madeChoice instanceof SpellAbility) {
                    tgt.addTarget(madeChoice);
                } else // 'FINISH TARGETING' chosen 
                    bTargetingDone = true;
            }
        }
        return true;
    }
}
