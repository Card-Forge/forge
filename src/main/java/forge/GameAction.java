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
package forge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JFrame;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.AbilityFactoryAttach;
import forge.card.abilityfactory.AbilityFactoryCharm;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostPayment;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbilityRequirements;
import forge.card.spellability.Target;
import forge.card.spellability.TargetSelection;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerType;
import forge.control.input.InputPayManaCost;
import forge.control.input.InputPayManaCostAbility;
import forge.control.input.InputPayManaCostUtil;
import forge.game.GameEndReason;
import forge.game.GameSummary;
import forge.game.phase.PhaseHandler;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneComesIntoPlay;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;
import forge.gui.match.ViewWinLose;

/**
 * Methods for common actions performed during a game.
 * 
 * @author Forge
 * @version $Id$
 */
public class GameAction {
    /**
     * <p>
     * resetActivationsPerTurn.
     * </p>
     */
    public final void resetActivationsPerTurn() {
        final CardList all = AllZoneUtil.getCardsInGame();

        // Reset Activations per Turn
        for (final Card card : all) {
            for (final SpellAbility sa : card.getAllSpellAbilities()) {
                sa.getRestrictions().resetTurnActivations();
            }
        }
    }

    /**
     * <p>
     * changeZone.
     * </p>
     * 
     * @param zoneFrom
     *            a {@link forge.game.zone.PlayerZone} object.
     * @param zoneTo
     *            a {@link forge.game.zone.PlayerZone} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param position TODO
     * @return a {@link forge.Card} object.
     */
    public static Card changeZone(final PlayerZone zoneFrom, final PlayerZone zoneTo, final Card c, Integer position) {
        if ((zoneFrom == null) && !c.isToken()) {
            if (position == null) {
                zoneTo.add(c);
            }
            else {
                zoneTo.add(c, position);
            }
            Player p = zoneTo.getPlayer();
            if (p != null) {
                p.updateLabelObservers();
            }
            return c;
        }

//        System.err.println(String.format("%s moves from %s to %s", c.toString(), zoneFrom.getZoneType().name(), zoneTo.getZoneType().name()));

        boolean suppress;
        if ((zoneFrom == null) && !c.isToken()) {
            suppress = true;
        } else if (c.isToken()) {
            suppress = false;
        } else {
            suppress = zoneFrom.equals(zoneTo);
        }

        if (!suppress) {
            HashMap<String, Object> repParams = new HashMap<String, Object>();
            repParams.put("Event", "Moved");
            repParams.put("Affected", c);
            repParams.put("Origin", zoneFrom != null ? zoneFrom.getZoneType() : null);
            repParams.put("Destination", zoneTo.getZoneType());

            if (AllZone.getReplacementHandler().run(repParams)) {
                if (AllZone.getStack().isResolving(c) && !zoneTo.is(ZoneType.Graveyard)) {
                    return Singletons.getModel().getGameAction().moveToGraveyard(c);
                }
                return c;
            }
        }


        Card copied = null;
        Card lastKnownInfo = null;

        // Don't copy Tokens, Cards staying in same zone, or cards entering
        // Battlefield
        if (c.isToken() || suppress || zoneTo.is(ZoneType.Battlefield) || zoneTo.is(ZoneType.Stack)
                || (zoneFrom.is(ZoneType.Stack) && zoneTo.is(ZoneType.Battlefield))) {
            lastKnownInfo = c;
            copied = c;
        } else {
            lastKnownInfo = CardUtil.getLKICopy(c);

            AllZone.getTriggerHandler().suppressMode(TriggerType.Transformed);
            if (c.isCloned()) {
                c.switchStates(CardCharactersticName.Cloner, CardCharactersticName.Original);
                c.setState(CardCharactersticName.Original);
                c.clearStates(CardCharactersticName.Cloner);
                if (c.isFlipCard()) {
                    c.clearStates(CardCharactersticName.Flipped);
                }
            }
            // reset flip status when card leaves battlefield
            if (zoneFrom.is(ZoneType.Battlefield)) {
                c.setFlipStaus(false);
            }
            AllZone.getTriggerHandler().clearSuppression(TriggerType.Transformed);
            copied = AllZone.getCardFactory().copyCard(c);
            copied.setUnearthed(c.isUnearthed());
            copied.setTapped(false);
        }

        if (c.wasSuspendCast()) {
            copied = GameAction.addSuspendTriggers(c);
        }

        for (final Trigger trigger : c.getTriggers()) {
            trigger.setHostCard(copied);
        }

        if (suppress) {
            AllZone.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        }

        // "enter the battlefield as a copy" - apply code here
        // but how to query for input here and continue later while the callers assume synchronous result?
        if (position == null) {
            zoneTo.add(copied);
        }
        else {
            zoneTo.add(copied, position);
        }

        // Tokens outside the battlefield disappear immideately.
        if (copied.isToken() && !zoneTo.is(ZoneType.Battlefield)) {
            zoneTo.remove(copied);
        }

        if (zoneFrom != null) {
            if (zoneFrom.is(ZoneType.Battlefield) && c.isCreature()) {
                AllZone.getCombat().removeFromCombat(c);
            }
            zoneFrom.remove(c);
        }

        Player p = zoneTo.getPlayer();
        if (p != null) {
            p.updateLabelObservers();
        }

        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", lastKnownInfo);
        if (zoneFrom != null) {
            runParams.put("Origin", zoneFrom.getZoneType().name());
        } else {
            runParams.put("Origin", null);
        }
        runParams.put("Destination", zoneTo.getZoneType().name());
        AllZone.getTriggerHandler().runTrigger(TriggerType.ChangesZone, runParams);
        // AllZone.getStack().chooseOrderOfSimultaneousStackEntryAll();

        if (suppress) {
            AllZone.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        }

        // remove all counters from the card if destination is not the
        // battlefield
        // UNLESS we're dealing with Skullbriar, the Walking Grave
        if (!zoneTo.is(ZoneType.Battlefield)) {
            copied.setSuspendCast(false);
            // remove all counters from the card if destination is not the battlefield
            // UNLESS we're dealing with Skullbriar, the Walking Grave
            if (!(c.getName().equals("Skullbriar, the Walking Grave") && !zoneTo.is(ZoneType.Hand) && !zoneTo
                    .is(ZoneType.Library))) {
                copied.clearCounters();
            }
            AllZone.getTriggerHandler().suppressMode(TriggerType.Transformed);
            copied.setState(CardCharactersticName.Original);
            AllZone.getTriggerHandler().clearSuppression(TriggerType.Transformed);
            // Soulbond unpairing
            if (c.isPaired()) {
                c.getPairedWith().setPairedWith(null);
                c.setPairedWith(null);
            }
            // Handle unequipping creatures
            if (copied.isEquipped()) {
                final CardList equipments = new CardList(copied.getEquippedBy());
                for (final Card equipment : equipments) {
                    if (AllZoneUtil.isCardInPlay(equipment)) {
                        equipment.unEquipCard(copied);
                    }
                }
            }
            // Handle unequipping creatures
            if (copied.isEquipped()) {
                final CardList equipments = new CardList(copied.getEquippedBy());
                for (final Card equipment : equipments) {
                    if (AllZoneUtil.isCardInPlay(equipment)) {
                        equipment.unEquipCard(copied);
                    }
                }
            }
            // equipment moving off battlefield
            if (copied.isEquipping()) {
                final Card equippedCreature = copied.getEquipping().get(0);
                if (AllZoneUtil.isCardInPlay(equippedCreature)) {
                    copied.unEquipCard(equippedCreature);
                }
            }
            // remove enchantments from creatures
            if (copied.isEnchanted()) {
                final CardList auras = new CardList(copied.getEnchantedBy());
                for (final Card aura : auras) {
                    aura.unEnchantEntity(copied);
                }
            }
            // unenchant creature if moving aura
            if (copied.isEnchanting()) {
                copied.unEnchantEntity(copied.getEnchanting());
            }
        }

        copied.setTimestamp(AllZone.getNextTimestamp());
        for (String s : copied.getKeyword()) {
            if (s.startsWith("May be played") || s.startsWith("You may look at this card.")
                    || s.startsWith("May be played by your opponent")
                    || s.startsWith("Your opponent may look at this card.")) {
                copied.removeAllExtrinsicKeyword(s);
                copied.removeHiddenExtrinsicKeyword(s);
            }
        }

        return copied;
    }

    /**
     * <p>
     * moveTo.
     * </p>
     * 
     * @param zoneTo
     *            a {@link forge.game.zone.PlayerZone} object.
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveTo(final PlayerZone zoneTo, Card c) {
        return moveTo(zoneTo, c, null);
    }

    public final Card moveTo(final PlayerZone zoneTo, Card c, Integer position) {
        // Ideally move to should never be called without a prevZone
        // Remove card from Current Zone, if it has one
        final PlayerZone zoneFrom = AllZone.getZoneOf(c);
        // String prevName = prev != null ? prev.getZoneName() : "";

        if (c.hasKeyword("If CARDNAME would leave the battlefield, exile it instead of putting it anywhere else.")
                && !zoneTo.is(ZoneType.Exile)) {
            final PlayerZone removed = c.getOwner().getZone(ZoneType.Exile);
            c.removeAllExtrinsicKeyword("If CARDNAME would leave the battlefield, "
                    + "exile it instead of putting it anywhere else.");
            return this.moveTo(removed, c);
        }

        // Card lastKnownInfo = c;

        c = GameAction.changeZone(zoneFrom, zoneTo, c, position);

        if (zoneTo.is(ZoneType.Stack)) {
            c.setCastFrom(zoneFrom.getZoneType());
        } else if (zoneFrom == null) {
            c.setCastFrom(null);
        } else if (!(zoneTo.is(ZoneType.Battlefield) && zoneFrom.is(ZoneType.Stack))) {
            c.setCastFrom(null);
        }

        if (c.isAura() && zoneTo.is(ZoneType.Battlefield) && ((zoneFrom == null) || !zoneFrom.is(ZoneType.Stack))
                && !c.isEnchanting()) {
            // TODO Need a way to override this for Abilities that put Auras
            // into play attached to things
            AbilityFactoryAttach.attachAuraOnIndirectEnterBattlefield(c);
        }

        return c;
    }

    /**
     * <p>
     * moveToPlayFromHand.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToPlayFromHand(Card c) {
        // handles the case for Clone, etc where prev was null

        final PlayerZone hand = c.getOwner().getZone(ZoneType.Hand);
        final PlayerZone play = c.getController().getZone(ZoneType.Battlefield);

        c = GameAction.changeZone(hand, play, c, null);

        return c;
    }

    /**
     * Controller change zone correction.
     * 
     * @param c
     *            a Card object
     */
    public final void controllerChangeZoneCorrection(final Card c) {
        System.out.println("Correcting zone for " + c.toString());
        final PlayerZone oldBattlefield = AllZone.getZoneOf(c);
        final PlayerZone newBattlefield = c.getController().getZone(oldBattlefield.getZoneType());

        if ((oldBattlefield == null) || (newBattlefield == null) || oldBattlefield.equals(newBattlefield)) {
            return;
        }

        AllZone.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        ((PlayerZoneComesIntoPlay) AllZone.getHumanPlayer().getZone(ZoneType.Battlefield)).setTriggers(false);
        ((PlayerZoneComesIntoPlay) AllZone.getComputerPlayer().getZone(ZoneType.Battlefield)).setTriggers(false);

        final int tiz = c.getTurnInZone();

        oldBattlefield.remove(c);
        newBattlefield.add(c);
        c.setSickness(true);
        if (c.hasStartOfKeyword("Echo")) {
            c.addExtrinsicKeyword("(Echo unpaid)");
        }
        AllZone.getCombat().removeFromCombat(c);

        c.setTurnInZone(tiz);

        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", c);
        AllZone.getTriggerHandler().runTrigger(TriggerType.ChangesController, runParams);

        AllZone.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        ((PlayerZoneComesIntoPlay) AllZone.getHumanPlayer().getZone(ZoneType.Battlefield)).setTriggers(true);
        ((PlayerZoneComesIntoPlay) AllZone.getComputerPlayer().getZone(ZoneType.Battlefield)).setTriggers(true);
    }

    /**
     * <p>
     * moveToStack.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToStack(final Card c) {
        final PlayerZone stack = AllZone.getStackZone();
        return this.moveTo(stack, c);
    }

    /**
     * <p>
     * moveToGraveyard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToGraveyard(Card c) {
        final PlayerZone origZone = AllZone.getZoneOf(c);
        final Player owner = c.getOwner();
        final PlayerZone grave = owner.getZone(ZoneType.Graveyard);
        final PlayerZone exile = owner.getZone(ZoneType.Exile);
        final CardList ownerBoard = owner.getCardsIn(ZoneType.Battlefield);
        final CardList opponentsBoard = owner.getOpponent().getCardsIn(ZoneType.Battlefield);

        if (c.getName().equals("Nissa's Chosen") && origZone.is(ZoneType.Battlefield)) {
            return this.moveToLibrary(c, -1);
        }

        for (final Card card : opponentsBoard) {
            if (card.hasKeyword("If a card would be put into an opponent's "
                    + "graveyard from anywhere, exile it instead.")) {
                return this.moveTo(exile, c);
            }
        }

        for (final Card card : ownerBoard) {
            if (card.hasKeyword("If a card would be put into your graveyard from anywhere, exile it instead.")) {
                return this.moveTo(exile, c);
            }
        }

        if (c.hasKeyword("If CARDNAME would be put into a graveyard, exile it instead.")) {
            return this.moveTo(exile, c);
        }

        if (c.hasKeyword("If CARDNAME is put into a graveyard this turn, its controller gets a poison counter.")) {
            c.getController().addPoisonCounters(1, c);
        }

        // must put card in OWNER's graveyard not controller's
        c = this.moveTo(grave, c);

        // Recover keyword
        if (c.isCreature() && origZone.is(ZoneType.Battlefield)) {
            for (final Card recoverable : c.getOwner().getCardsIn(ZoneType.Graveyard)) {
                if (recoverable.hasStartOfKeyword("Recover") && !recoverable.equals(c)) {

                    final String recoverCost = recoverable.getKeyword().get(recoverable.getKeywordPosition("Recover"))
                            .split(":")[1];
                    final Cost cost = new Cost(recoverable, recoverCost, true);

                    final Command paidCommand = new Command() {
                        private static final long serialVersionUID = -6357156873861051845L;

                        @Override
                        public void execute() {
                            Singletons.getModel().getGameAction().moveToHand(recoverable);
                        }
                    };

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = -7354791599039157375L;

                        @Override
                        public void execute() {
                            Singletons.getModel().getGameAction().exile(recoverable);
                        }
                    };

                    final SpellAbility abRecover = new AbilityActivated(recoverable, cost, null) {
                        private static final long serialVersionUID = 8858061639236920054L;

                        @Override
                        public void resolve() {
                            Singletons.getModel().getGameAction().moveToHand(recoverable);
                        }

                        @Override
                        public String getStackDescription() {
                            final StringBuilder sd = new StringBuilder(recoverable.getName());
                            sd.append(" - Recover.");

                            return sd.toString();
                        }

                        @Override
                        public AbilityActivated getCopy() {
                            return null;
                        }
                    };

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Recover ").append(recoverable).append("\n");

                    final Ability recoverAbility = new Ability(recoverable, "0") {
                        @Override
                        public void resolve() {
                            if (recoverable.getController().isHuman()) {
                                GameActionUtil.payCostDuringAbilityResolve(sb.toString(), recoverable, recoverCost,
                                        paidCommand, unpaidCommand);
                            } else { // computer
                                if (ComputerUtil.canPayCost(abRecover)) {
                                    ComputerUtil.playNoStack(abRecover);
                                } else {
                                    Singletons.getModel().getGameAction().exile(recoverable);
                                }
                            }
                        }
                    };
                    recoverAbility.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(recoverAbility);
                }
            }
        }
        return c;
    }

    /**
     * <p>
     * moveToHand.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToHand(final Card c) {
        final PlayerZone hand = c.getOwner().getZone(ZoneType.Hand);
        return this.moveTo(hand, c);
    }

    /**
     * <p>
     * moveToPlay.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToPlay(final Card c) {
        final PlayerZone play = c.getOwner().getZone(ZoneType.Battlefield);
        return this.moveTo(play, c);
    }

    /**
     * <p>
     * moveToPlay.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param p
     *            a {@link forge.game.player.Player} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToPlay(final Card c, final Player p) {
        // move to a specific player's Battlefield
        final PlayerZone play = p.getZone(ZoneType.Battlefield);
        return this.moveTo(play, c);
    }

    /**
     * <p>
     * moveToBottomOfLibrary.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToBottomOfLibrary(final Card c) {
        return this.moveToLibrary(c, -1);
    }

    /**
     * <p>
     * moveToLibrary.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToLibrary(final Card c) {
        return this.moveToLibrary(c, 0);
    }

    /**
     * <p>
     * moveToLibrary.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param libPosition
     *            a int.
     * @return a {@link forge.Card} object.
     */
    public final Card moveToLibrary(Card c, int libPosition) {
        final PlayerZone p = AllZone.getZoneOf(c);
        final PlayerZone library = c.getOwner().getZone(ZoneType.Library);

        if (c.hasKeyword("If CARDNAME would leave the battlefield, exile it instead of putting it anywhere else.")) {
            final PlayerZone removed = c.getOwner().getZone(ZoneType.Exile);
            c.removeAllExtrinsicKeyword("If CARDNAME would leave the battlefield, "
                    + "exile it instead of putting it anywhere else.");
            return this.moveTo(removed, c);
        }

        if (p != null) {
            p.remove(c);
        }

        if (c.isToken()) {
            return c;
        }

        if (c.isInAlternateState()) {
            AllZone.getTriggerHandler().suppressMode(TriggerType.Transformed);
            c.setState(CardCharactersticName.Original);
            AllZone.getTriggerHandler().clearSuppression(TriggerType.Transformed);
        }

        if ((libPosition == -1) || (libPosition > library.size())) {
            libPosition = library.size();
        }

        Card lastKnownInfo = c;
        if (p != null && p.is(ZoneType.Battlefield)) {
            lastKnownInfo = CardUtil.getLKICopy(c);
            c.clearCounters(); // remove all counters
            library.add(AllZone.getCardFactory().copyCard(c), libPosition);
        } else {
            c.clearCounters(); // remove all counters
            library.add(c, libPosition);
        }

        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", lastKnownInfo);
        if (p != null) {
            runParams.put("Origin", p.getZoneType().name());
        } else {
            runParams.put("Origin", null);
        }
        runParams.put("Destination", ZoneType.Library.name());
        AllZone.getTriggerHandler().runTrigger(TriggerType.ChangesZone, runParams);

        Player owner = p.getPlayer();
        if (owner != null) {
            owner.updateLabelObservers();
        }

        // Soulbond unpairing
        if (c.isPaired()) {
            c.getPairedWith().setPairedWith(null);
            c.setPairedWith(null);
        }

        return c;
    }

    /**
     * <p>
     * exile.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card exile(final Card c) {
        if (AllZoneUtil.isCardExiled(c)) {
            return c;
        }

        final PlayerZone removed = c.getOwner().getZone(ZoneType.Exile);

        return Singletons.getModel().getGameAction().moveTo(removed, c);
    }

    /**
     * Move to.
     * 
     * @param name
     *            the name
     * @param c
     *            the c
     * @return the card
     */
    public final Card moveTo(final ZoneType name, final Card c) {
        return this.moveTo(name, c, 0);
    }

    /**
     * <p>
     * moveTo.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param libPosition
     *            a int.
     * @return a {@link forge.Card} object.
     */
    public final Card moveTo(final ZoneType name, final Card c, final int libPosition) {
        // Call specific functions to set PlayerZone, then move onto moveTo
        if (name.equals(ZoneType.Hand)) {
            return this.moveToHand(c);
        } else if (name.equals(ZoneType.Library)) {
            return this.moveToLibrary(c, libPosition);
        } else if (name.equals(ZoneType.Battlefield)) {
            return this.moveToPlay(c);
        } else if (name.equals(ZoneType.Graveyard)) {
            return this.moveToGraveyard(c);
        } else if (name.equals(ZoneType.Exile)) {
            return this.exile(c);
        } else if (name.equals(ZoneType.Ante)) {
            final PlayerZone ante = c.getOwner().getZone(ZoneType.Ante);
            return this.moveTo(ante, c);
        } else {
            return this.moveToStack(c);
        }
    }

    /**
     * <p>
     * drawMiracle.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     */
    public final void drawMiracle(final Card card) {
        // Whenever a card with miracle is the first card drawn in a turn,
        // you may cast it for it's miracle cost
        if (card.getMiracleCost() == null) {
            return;
        }

        final SpellAbility miracle = card.getFirstSpellAbility().copy();
        miracle.setPayCosts(new Cost(card, card.getMiracleCost(), false));

        final StringBuilder sb = new StringBuilder();
        sb.append(card.getName()).append(" - Cast via Miracle");
        miracle.setStackDescription(sb.toString());

        // TODO Convert this to a Trigger
        final Ability activate = new Ability(card, "0") {
            @Override
            public void resolve() {
                // pay miracle cost here.
                if (card.getOwner().isHuman()) {
                    if (GameActionUtil.showYesNoDialog(card, card + " - Drawn. Pay Miracle Cost?")) {
                        Singletons.getModel().getGameAction().playSpellAbility(miracle);
                    }
                } else {
                    Spell spell = (Spell) miracle;
                    if (spell.canPlayFromEffectAI(false, false)) {
                        ComputerUtil.playStack(miracle);
                    }
                }
            }
        };

        final StringBuilder sbAct = new StringBuilder();
        sbAct.append(card.getName()).append(" - Miracle.");
        activate.setStackDescription(sbAct.toString());
        activate.setActivatingPlayer(card.getOwner());
        activate.setTrigger(true);

        AllZone.getStack().add(activate);
    }

    /**
     * <p>
     * discardPutIntoPlayInstead.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void discardPutIntoPlayInstead(final Card c) {
        this.moveToPlay(c);

        if (c.getName().equals("Dodecapod")) {
            c.setCounter(Counters.P1P1, 2, false);
        }
    }

    /**
     * <p>
     * discardMadness.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     */
    public final void discardMadness(final Card card) {
        // Whenever a card with madness is discarded, you may cast it for it's
        // madness cost
        if (card.getMadnessCost() == null) {
            return;
        }

        final SpellAbility madness = card.getFirstSpellAbility().copy();
        madness.setPayCosts(new Cost(card, card.getMadnessCost(), false));

        final StringBuilder sb = new StringBuilder();
        sb.append(card.getName()).append(" - Cast via Madness");
        madness.setStackDescription(sb.toString());

        // TODO Convert this to a Trigger
        final Ability activate = new Ability(card, "0") {
            @Override
            public void resolve() {
                // pay madness cost here.
                if (card.getOwner().isHuman()) {
                    if (GameActionUtil.showYesNoDialog(card, card + " - Discarded. Pay Madness Cost?")) {
                        Singletons.getModel().getGameAction().playSpellAbility(madness);
                    }
                } else {
                    Spell spell = (Spell) madness;
                    if (spell.canPlayFromEffectAI(false, false)) {
                        ComputerUtil.playStack(madness);
                    }
                }
            }
        };

        final StringBuilder sbAct = new StringBuilder();
        sbAct.append(card.getName()).append(" - Madness.");
        activate.setStackDescription(sbAct.toString());
        activate.setActivatingPlayer(card.getOwner());

        AllZone.getStack().add(activate);
    }

    /**
     * <p>
     * checkEndGameSate.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean checkEndGameState() {
        // if game is already over return true
        if (Singletons.getModel().getGameState().isGameOver()) {
            return true;
        }
        // Win / Lose
        final GameSummary game = Singletons.getModel().getGameSummary();
        boolean humanWins = false;
        boolean computerWins = false;
        final Player computer = AllZone.getComputerPlayer();
        final Player human = AllZone.getHumanPlayer();

        // Winning Conditions can be worth more than losing conditions
        if (human.hasWon() || computer.hasLost()) {
            humanWins = true;

            if (human.getAltWin()) {
                game.end(GameEndReason.WinsGameSpellEffect, human.getName(), human.getWinConditionSource());
            } else {
                game.end(GameEndReason.AllOpponentsLost, human.getName(), null);
            }
        }

        if (computer.hasWon() || human.hasLost()) {
            if (humanWins) {
                // both players won/lost at the same time.
                game.end(GameEndReason.Draw, null, null);
            } else {
                computerWins = true;

                if (computer.getAltWin()) {
                    game.end(GameEndReason.WinsGameSpellEffect, computer.getName(), computer.getWinConditionSource());
                } else {
                    game.end(GameEndReason.AllOpponentsLost, computer.getName(), null);
                }

            }
        }

        final boolean isGameDone = humanWins || computerWins;
        if (isGameDone) {
            Singletons.getModel().getGameState().setGameOver(true);
            game.getPlayerRating(computer.getName()).setLossReason(computer.getLossState(),
                    computer.getLossConditionSource());
            game.getPlayerRating(human.getName()).setLossReason(human.getLossState(), human.getLossConditionSource());
            Singletons.getModel().getMatchState().addGamePlayed(game);
        }

        return isGameDone;
    }

    /** */
    public final void checkStaticAbilities() {
        // remove old effects
        AllZone.getStaticEffects().clearStaticEffects();

        // search for cards with static abilities
        final CardList allCards = AllZoneUtil.getCardsInGame();
        final ArrayList<StaticAbility> staticAbilities = new ArrayList<StaticAbility>();
        for (final Card card : allCards) {
            for (StaticAbility sa : card.getStaticAbilities()) {
                if (sa.getMapParams().get("Mode").equals("Continuous")) {
                    staticAbilities.add(sa);
                }
            }
        }

        final Comparator<StaticAbility> comp = new Comparator<StaticAbility>() {
            @Override
            public int compare(final StaticAbility a, final StaticAbility b) {
                if (a.getLayer() > b.getLayer()) {
                    return 1;
                }
                if (a.getLayer() < b.getLayer()) {
                    return -1;
                }
                if (a.getHostCard().getTimestamp() > b.getHostCard().getTimestamp()) {
                    return 1;
                }
                if (a.getHostCard().getTimestamp() < b.getHostCard().getTimestamp()) {
                    return -1;
                }
                return 0;
            }
        };
        Collections.sort(staticAbilities, comp);
        for (final StaticAbility stAb : staticAbilities) {
            stAb.applyAbility("Continuous");
        }

        // card state effects like Glorious Anthem
        for (final String effect : AllZone.getStaticEffects().getStateBasedMap().keySet()) {
            final Command com = GameActionUtil.getCommands().get(effect);
            com.execute();
        }

        GameActionUtil.getStLandManaAbilities().execute();
    }

    /**
     * <p>
     * checkStateEffects.
     * </p>
     */
    public final void checkStateEffects() {

        // sol(10/29) added for Phase updates, state effects shouldn't be
        // checked during Spell Resolution (except when persist-returning
        if (AllZone.getStack().getResolving()) {
            return;
        }

        final boolean refreeze = AllZone.getStack().isFrozen();
        AllZone.getStack().setFrozen(true);

        final JFrame frame = Singletons.getView().getFrame();
        if (!frame.isDisplayable()) {
            return;
        }

        if (this.checkEndGameState()) {
            // Clear Simultaneous triggers at the end of the game
            new ViewWinLose();
            Singletons.getModel().getGameState().getStack().clearSimultaneousStack();
            if (!refreeze) {
                AllZone.getStack().unfreezeStack();
            }
            return;
        }

        // do this twice, sometimes creatures/permanents will survive when they
        // shouldn't
        for (int q = 0; q < 9; q++) {

            boolean checkAgain = false;

            AllZone.getHumanPlayer().setMaxHandSize(7);
            AllZone.getComputerPlayer().setMaxHandSize(7);

            this.checkStaticAbilities();

            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            AllZone.getTriggerHandler().runTrigger(TriggerType.Always, runParams);

            final CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
            Card c;

            final Iterator<Card> it = list.iterator();

            while (it.hasNext()) {
                c = it.next();

                if (c.isEquipped()) {
                    final CardList equipments = new CardList(c.getEquippedBy());
                    for (final Card equipment : equipments) {
                        if (!AllZoneUtil.isCardInPlay(equipment)) {
                            equipment.unEquipCard(c);
                            checkAgain = true;
                        }
                    }
                } // if isEquipped()

                if (c.isEquipping()) {
                    final Card equippedCreature = c.getEquipping().get(0);
                    if (!equippedCreature.isCreature() || !AllZoneUtil.isCardInPlay(equippedCreature)) {
                        c.unEquipCard(equippedCreature);
                        checkAgain = true;
                    }

                    // make sure any equipment that has become a creature stops
                    // equipping
                    if (c.isCreature()) {
                        c.unEquipCard(equippedCreature);
                        checkAgain = true;
                    }
                } // if isEquipping()

                if (c.isAura()) {
                    // Check if Card Aura is attached to is a legal target
                    final GameEntity entity = c.getEnchanting();
                    final SpellAbility sa = c.getSpellPermanent();
                    Target tgt = null;
                    if (sa != null) {
                        tgt = sa.getTarget();
                    }

                    if (entity instanceof Card) {
                        final Card perm = (Card) entity;
                        if (!AllZoneUtil.isCardInPlay(perm) || !perm.canBeEnchantedBy(c)) {
                            c.unEnchantEntity(perm);
                            this.moveToGraveyard(c);
                            checkAgain = true;
                        }
                    } else if (entity instanceof Player) {
                        final Player pl = (Player) entity;
                        boolean invalid = false;

                        if (tgt.canOnlyTgtOpponent() && !c.getController().getOpponent().isPlayer(pl)) {
                            invalid = true;
                        } else {
                            if (pl.hasProtectionFrom(c)) {
                                invalid = true;
                            }
                        }
                        if (invalid) {
                            c.unEnchantEntity(pl);
                            this.moveToGraveyard(c);
                            checkAgain = true;
                        }
                    }

                    if (AllZoneUtil.isCardInPlay(c) && !c.isEnchanting()) {
                        this.moveToGraveyard(c);
                        checkAgain = true;
                    }

                } // if isAura

                if (c.isCreature()) {
                    if (c.isEnchanting()) {
                        c.unEnchantEntity(c.getEnchanting());
                        checkAgain = true;
                    }
                    if ((c.getNetDefense() <= c.getDamage()) && !c.hasKeyword("Indestructible")) {
                        this.destroy(c);
                        // this is untested with instants and abilities but
                        // required for First Strike combat phase
                        AllZone.getCombat().removeFromCombat(c);
                        checkAgain = true;
                    } else if (c.getNetDefense() <= 0) {
                        // TODO This shouldn't be a destroy, and should happen
                        // before the damage check probably
                        this.destroy(c);
                        AllZone.getCombat().removeFromCombat(c);
                        checkAgain = true;
                    }
                    // Soulbond unpairing
                    if (c.isPaired()) {
                        Card partner = c.getPairedWith();
                        if (!partner.isCreature() || c.getController() != partner.getController()) {
                            c.setPairedWith(null);
                            partner.setPairedWith(null);
                        }
                    }
                }

                // +1/+1 counters should erase -1/-1 counters
                if (c.getCounters(Counters.P1P1) > 0 && c.getCounters(Counters.M1M1) > 0) {

                    final Counters p1Counter = Counters.P1P1;
                    final Counters m1Counter = Counters.M1M1;
                    int plusOneCounters = c.getCounters(Counters.P1P1);
                    int minusOneCounters = c.getCounters(Counters.M1M1);

                    if (plusOneCounters == minusOneCounters) {
                        c.getCounters().remove(m1Counter);
                        c.getCounters().remove(p1Counter);
                    }
                    if (plusOneCounters > minusOneCounters) {
                        c.getCounters().remove(m1Counter);
                        c.getCounters().put(p1Counter, (plusOneCounters - minusOneCounters));
                    } else {
                        c.getCounters().put(m1Counter, (minusOneCounters - plusOneCounters));
                        c.getCounters().remove(p1Counter);
                    }
                    checkAgain = true;
                }

            } // while it.hasNext()

            if (!checkAgain) {
                break; // do not continue the loop
            }

        } // for q=0;q<2
        /*
        //Experiment Kraj experiment
        CardList krajs = AllZoneUtil.getCardsIn(ZoneType.Battlefield).filter(new CardListFilter() {
            @Override
            public boolean addCard(Card c) {
                return c.getName().equals("Experiment Kraj");
            }
        });
        CardList P1P1s = AllZoneUtil.getCardsIn(ZoneType.Battlefield).filter(new CardListFilter() {
            @Override
            public boolean addCard(Card c) {
                return c.getCounters(Counters.P1P1) > 0;
            }
        });
        for(final Card kraj : krajs)
        {
            kraj.clearAllButFirstSpellAbility();
            CardFactoryUtil.addAbilityFactoryAbilities(kraj);
            for(final Card P1P1 : P1P1s)
            {
                if(!P1P1.equals(kraj)) {
                    for(SpellAbility sa : P1P1.getSpellAbilities())
                    {
                        if(sa instanceof AbilityActivated)
                        {
                            AbilityActivated newSA = ((AbilityActivated)sa).getCopy();
                            newSA.setSourceCard(kraj);
                            kraj.addSpellAbility(newSA);
                        }
                    }
                }
            }
        }
        */

        this.destroyLegendaryCreatures();
        this.destroyPlaneswalkers();

        if (!refreeze) {
            AllZone.getStack().unfreezeStack();
        }
    } // checkStateEffects()

    /**
     * <p>
     * destroyPlaneswalkers.
     * </p>
     */
    private void destroyPlaneswalkers() {
        // get all Planeswalkers
        final CardList list = AllZoneUtil.getCardsIn(ZoneType.Battlefield).getType("Planeswalker");

        Card c;
        for (int i = 0; i < list.size(); i++) {
            c = list.get(i);

            if (c.getCounters(Counters.LOYALTY) <= 0) {
                Singletons.getModel().getGameAction().moveToGraveyard(c);
            }

            final ArrayList<String> types = c.getType();
            for (final String type : types) {
                if (!CardUtil.isAPlaneswalkerType(type)) {
                    continue;
                }

                final CardList cl = list.getType(type);

                if (cl.size() > 1) {
                    for (final Card crd : cl) {
                        Singletons.getModel().getGameAction().moveToGraveyard(crd);
                    }
                }
            }
        }
    }

    /**
     * <p>
     * destroyLegendaryCreatures.
     * </p>
     */
    private void destroyLegendaryCreatures() {
        final CardList a = AllZoneUtil.getCardsIn(ZoneType.Battlefield).getType("Legendary");
        if (a.isEmpty() || !AllZoneUtil.isCardInPlay("Mirror Gallery")) {
            return;
        }

        while (!a.isEmpty()) {
            CardList b = AllZoneUtil.getCardsIn(ZoneType.Battlefield, a.get(0).getName());
            b = b.getType("Legendary");
            b = b.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card c) {
                    return !c.isFaceDown();
                }
            });
            a.remove(0);
            if (1 < b.size()) {
                for (int i = 0; i < b.size(); i++) {
                    Singletons.getModel().getGameAction().sacrificeDestroy(b.get(i));
                }
            }
        }
    } // destroyLegendaryCreatures()

    /**
     * <p>
     * sacrifice.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param source
     *            a SpellAbility object.
     * @return a boolean.
     */
    public final boolean sacrifice(final Card c, final SpellAbility source) {
        if (c.isImmutable()) {
            System.out.println("Trying to sacrifice immutables: " + c);
            return false;
        }
        if (source != null && !c.getController().equals(source.getActivatingPlayer())
                && c.getController().hasKeyword("Spells and abilities your opponents control can't cause"
                        + " you to sacrifice permanents.")) {
            return false;
        }
        this.sacrificeDestroy(c);

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", c);
        AllZone.getTriggerHandler().runTrigger(TriggerType.Sacrificed, runParams);

        return true;
    }

    /**
     * <p>
     * destroyNoRegeneration.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean destroyNoRegeneration(final Card c) {
        if (!AllZoneUtil.isCardInPlay(c) || c.hasKeyword("Indestructible")) {
            return false;
        }

        if (c.isEnchanted()) {
            CardList list = new CardList(c.getEnchantedBy());
            list = list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card crd) {
                    return crd.hasKeyword("Totem armor");
                }
            });
            CardListUtil.sortCMC(list);

            if (list.size() != 0) {
                final Card crd;
                if (list.size() == 1) {
                    crd = list.get(0);
                } else {
                    if (c.getController().isHuman()) {
                        crd = GuiUtils.chooseOneOrNone("Select totem armor to destroy", list.toArray());
                    } else {
                        crd = list.get(0);
                    }
                }

                final Card card = c;
                final AbilityStatic ability = new AbilityStatic(crd, "0") {
                    @Override
                    public void resolve() {
                        GameAction.this.destroy(crd);
                        card.setDamage(0);

                    }
                };

                final StringBuilder sb = new StringBuilder();
                sb.append(crd).append(" - Totem armor: destroy this aura.");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().add(ability);
                return false;
            }
        } // totem armor

        return this.sacrificeDestroy(c);
    }

    /**
     * <p>
     * addSuspendTriggers.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public static Card addSuspendTriggers(final Card c) {
        c.setSVar("HasteFromSuspend", "True");

        final Command intoPlay = new Command() {
            private static final long serialVersionUID = -4514610171270596654L;

            @Override
            public void execute() {
                if (AllZoneUtil.isCardInPlay(c) && c.isCreature()) {
                    c.addExtrinsicKeyword("Haste");
                }
            } // execute()
        };

        c.addComesIntoPlayCommand(intoPlay);

        final Command loseControl = new Command() {
            private static final long serialVersionUID = -4514610171270596654L;

            @Override
            public void execute() {
                if (c.getSVar("HasteFromSuspend").equals("True")) {
                    c.setSVar("HasteFromSuspend", "False");
                    c.removeExtrinsicKeyword("Haste");
                }
            } // execute()
        };

        c.addChangeControllerCommand(loseControl);
        c.addLeavesPlayCommand(loseControl);
        return c;
    }

    /**
     * <p>
     * sacrificeDestroy.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean sacrificeDestroy(final Card c) {
        if (!AllZoneUtil.isCardInPlay(c)) {
            return false;
        }

        final Player owner = c.getOwner();
        if (!(owner.isComputer() || owner.isHuman())) {
            throw new RuntimeException("GameAction : destroy() invalid card.getOwner() - " + c + " " + owner);
        }

        final boolean persist = (c.hasKeyword("Persist") && (c.getCounters(Counters.M1M1) == 0)) && !c.isToken();

        final boolean undying = (c.hasKeyword("Undying") && (c.getCounters(Counters.P1P1) == 0)) && !c.isToken();

        final Card newCard = this.moveToGraveyard(c);

        // Destroy needs to be called with Last Known Information
        c.destroy();

        // System.out.println("Card " + c.getName() +
        // " is getting sent to GY, and this turn it got damaged by: ");
        for (final Card crd : c.getReceivedDamageFromThisTurn().keySet()) {
            if (c.getReceivedDamageFromThisTurn().get(crd) > 0) {
                // System.out.println(crd.getName() );
                GameActionUtil.executeVampiricEffects(crd);
            }
        }

        if (persist) {
            final Card persistCard = newCard;
            final Ability persistAb = new Ability(persistCard, "0") {

                @Override
                public void resolve() {
                    if (AllZone.getZoneOf(persistCard).is(ZoneType.Graveyard)) {
                        final PlayerZone ownerPlay = persistCard.getOwner().getZone(ZoneType.Battlefield);
                        final Card card = GameAction.this.moveTo(ownerPlay, persistCard);
                        card.addCounter(Counters.M1M1, 1);
                    }
                }
            };
            persistAb.setStackDescription(newCard.getName() + " - Returning from Persist");
            AllZone.getStack().add(persistAb);
        }

        if (undying) {
            final Card undyingCard = newCard;
            final Ability undyingAb = new Ability(undyingCard, "0") {

                @Override
                public void resolve() {
                    if (AllZone.getZoneOf(undyingCard).is(ZoneType.Graveyard)) {
                        final PlayerZone ownerPlay = undyingCard.getOwner().getZone(ZoneType.Battlefield);
                        final Card card = GameAction.this.moveTo(ownerPlay, undyingCard);
                        card.addCounter(Counters.P1P1, 1);
                    }
                }
            };
            undyingAb.setStackDescription(newCard.getName() + " - Returning from Undying");
            AllZone.getStack().add(undyingAb);
        }
        return true;
    } // sacrificeDestroy()

    /**
     * <p>
     * destroy.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean destroy(final Card c) {
        if (!AllZoneUtil.isCardInPlay(c)
                || (c.hasKeyword("Indestructible") && (!c.isCreature() || (c.getNetDefense() > 0)))) {
            return false;
        }

        if (c.canBeShielded() && (c.getShield() > 0)) {
            c.subtractShield();
            c.setDamage(0);
            c.tap();
            c.addRegeneratedThisTurn();
            AllZone.getCombat().removeFromCombat(c);
            return false;
        }

        if (c.isEnchanted()) {
            CardList list = new CardList(c.getEnchantedBy());
            list = list.filter(new CardListFilter() {
                @Override
                public boolean addCard(final Card crd) {
                    return crd.hasKeyword("Totem armor");
                }
            });
            CardListUtil.sortCMC(list);

            if (list.size() != 0) {
                final Card crd;
                if (list.size() == 1) {
                    crd = list.get(0);
                } else {
                    if (c.getController().isHuman()) {
                        crd = GuiUtils.chooseOneOrNone("Select totem armor to destroy", list.toArray());
                    } else {
                        crd = list.get(0);
                    }
                }

                c.setDamage(0);
                this.destroy(crd);
                System.out.println("Totem armor destroyed instead of original card");
                return false;
            }
        } // totem armor

        return this.sacrificeDestroy(c);
    }

    private boolean startCut = false;



    /**
     * <p>
     * playCard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean playCard(final Card c) {
        // this can only be called by the Human
        final HashMap<String, SpellAbility> map = new HashMap<String, SpellAbility>();
        final ArrayList<SpellAbility> basicAbilities = c.getSpellAbilities();
        final ArrayList<SpellAbility> abilities = c.getSpellAbilities();
        final ArrayList<String> choices = new ArrayList<String>();
        final Player human = AllZone.getHumanPlayer();
        final PlayerZone zone = AllZone.getZoneOf(c);

        if (c.isLand() && human.canPlayLand()) {
            if (zone.is(ZoneType.Hand) || ((!zone.is(ZoneType.Battlefield)) && c.hasStartOfKeyword("May be played"))) {
                choices.add("Play land");
            }
        }
        for (SpellAbility sa : basicAbilities) {
            //add alternative costs as additional spell abilities
            abilities.addAll(GameActionUtil.getAlternativeCosts(sa));
        }
        for (final SpellAbility sa : abilities) {
            sa.setActivatingPlayer(human);
            if (sa.canPlay()) {
                choices.add(sa.toString());
                map.put(sa.toString(), sa);
            }
        }

        String choice;
        if (choices.size() == 0) {
            return false;
        } else if (choices.size() == 1) {
            choice = choices.get(0);
        } else {
            choice = (String) GuiUtils.chooseOneOrNone("Choose", choices.toArray());
        }

        if (choice == null) {
            return false;
        }

        if (choice.equals("Play land")) {
            AllZone.getHumanPlayer().playLand(c);
            return true;
        }

        final SpellAbility ability = map.get(choice);
        if (ability != null) {
            this.playSpellAbility(ability);
            return true;
        }
        return false;
    }

    /**
     * <p>
     * playCardNoCost.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void playCardNoCost(final Card c) {
        final ArrayList<SpellAbility> choices = c.getBasicSpells();
        SpellAbility sa;

        // TODO add Buyback, Kicker, ... , spells here
        if (choices.size() == 0) {
            return;
        } else if (choices.size() == 1) {
            sa = choices.get(0);
        } else {
            sa = (SpellAbility) GuiUtils.chooseOneOrNone("Choose", choices.toArray());
        }

        if (sa == null) {
            return;
        }

        // Ripple causes a crash because it doesn't set the activatingPlayer in
        // this entrance
        if (sa.getActivatingPlayer() == null) {
            sa.setActivatingPlayer(AllZone.getHumanPlayer());
        }
        this.playSpellAbilityForFree(sa);
    }

    /**
     * <p>
     * playSpellAbilityForFree.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void playSpellAbilityForFree(final SpellAbility sa) {
        if (sa.getPayCosts() != null) {
            final TargetSelection ts = new TargetSelection(sa.getTarget(), sa);
            final CostPayment payment = new CostPayment(sa.getPayCosts(), sa);

            final SpellAbilityRequirements req = new SpellAbilityRequirements(sa, ts, payment);
            req.setFree(true);
            req.fillRequirements();
        } else if (sa.getBeforePayMana() == null) {
            if (sa.isSpell()) {
                final Card c = sa.getSourceCard();
                if (!c.isCopiedSpell()) {
                    sa.setSourceCard(Singletons.getModel().getGameAction().moveToStack(c));
                }
            }
            boolean x = sa.getSourceCard().getManaCost().getShardCount(ManaCostShard.X) > 0;

            if (sa.isKickerAbility()) {
                final Command paid1 = new Command() {
                    private static final long serialVersionUID = -6531785460264284794L;

                    @Override
                    public void execute() {
                        AllZone.getStack().add(sa);
                    }
                };
                AllZone.getInputControl().setInput(new InputPayManaCostAbility(sa.getAdditionalManaCost(), paid1));
            } else {
                AllZone.getStack().add(sa, x);
            }
        } else {
            sa.setManaCost("0"); // Beached As
            if (sa.isKickerAbility()) {
                sa.getBeforePayMana().setFree(false);
                sa.setManaCost(sa.getAdditionalManaCost());
            } else {
                sa.getBeforePayMana().setFree(true);
            }
            AllZone.getInputControl().setInput(sa.getBeforePayMana());
        }
    }

    /** The Cost cutting_ get multi kicker mana cost paid. */
    private int costCuttingGetMultiKickerManaCostPaid = 0;

    /** The Cost cutting_ get multi kicker mana cost paid_ colored. */
    private String costCuttingGetMultiKickerManaCostPaidColored = "";

    /**
     * <p>
     * getSpellCostChange.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param originalCost
     *            a {@link forge.card.mana.ManaCost} object.
     * @return a {@link forge.card.mana.ManaCost} object.
     */
    public final ManaCost getSpellCostChange(final SpellAbility sa, final ManaCost originalCost) {
        // Beached
        final Card originalCard = sa.getSourceCard();
        final SpellAbility spell = sa;
        String mana = originalCost.toString();
        ManaCost manaCost = new ManaCost(mana);
        if (sa.isXCost() && !originalCard.isCopiedSpell()) {
            originalCard.setXManaCostPaid(0);
        }

        if (PhaseHandler.getGameBegins() != 1 || sa.isTrigger()) {
            return manaCost;
        }

        if (spell.isSpell()) {
            if (spell.getIsDelve()) {
                final int cardsInGrave = originalCard.getController().getCardsIn(ZoneType.Graveyard).size();
                final ArrayList<Integer> choiceList = new ArrayList<Integer>();
                for (int i = 0; i <= cardsInGrave; i++) {
                    choiceList.add(i);
                }

                if (originalCard.getController().isHuman()) {

                    final int chosenAmount = (Integer) GuiUtils
                            .chooseOne("Exile how many cards?", choiceList.toArray());
                    System.out.println("Delve for " + chosenAmount);
                    final CardList choices = AllZone.getHumanPlayer().getCardsIn(ZoneType.Graveyard);
                    final CardList chosen = new CardList();
                    for (int i = 0; i < chosenAmount; i++) {
                        final Card nowChosen = GuiUtils.chooseOneOrNone("Exile which card?", choices.toArray());

                        if (nowChosen == null) {
                            // User canceled,abort delving.
                            chosen.clear();
                            break;
                        }

                        choices.remove(nowChosen);
                        chosen.add(nowChosen);
                    }

                    for (final Card c : chosen) {
                        this.exile(c);
                    }

                    manaCost = new ManaCost(originalCost.toString());
                    manaCost.decreaseColorlessMana(chosenAmount);
                } else {
                    // AI
                    int numToExile = 0;
                    final int colorlessCost = originalCost.getColorlessManaAmount();

                    if (cardsInGrave <= colorlessCost) {
                        numToExile = cardsInGrave;
                    } else {
                        numToExile = colorlessCost;
                    }

                    for (int i = 0; i < numToExile; i++) {
                        final CardList grave = new CardList(AllZone.getComputerPlayer().getZone(ZoneType.Graveyard)
                                .getCards());
                        Card chosen = null;
                        for (final Card c : grave) { // Exile noncreatures first
                                                     // in
                            // case we can revive. Might
                            // wanna do some additional
                            // checking here for Flashback
                            // and the like.
                            if (!c.isCreature()) {
                                chosen = c;
                                break;
                            }
                        }
                        if (chosen == null) {
                            chosen = CardFactoryUtil.getWorstCreatureAI(grave);
                        }

                        if (chosen == null) {
                            // Should never get here but... You know how it is.
                            chosen = grave.get(0);
                        }

                        this.exile(chosen);
                    }
                    manaCost = new ManaCost(originalCost.toString());
                    manaCost.decreaseColorlessMana(numToExile);
                }
            } else if (spell.getSourceCard().hasKeyword("Convoke")) {
                CardList untappedCreats = spell.getActivatingPlayer().getCardsIn(ZoneType.Battlefield).getType("Creature");
                untappedCreats = untappedCreats.filter(new CardListFilter() {
                    @Override
                    public boolean addCard(final Card c) {
                        return !c.isTapped();
                    }
                });

                if (untappedCreats.size() != 0) {
                    final ArrayList<Object> choices = new ArrayList<Object>();
                    for (final Card c : untappedCreats) {
                        choices.add(c);
                    }
                    choices.add("DONE");
                    ArrayList<String> usableColors = new ArrayList<String>();
                    ManaCost newCost = new ManaCost(originalCost.toString());
                    Object tapForConvoke = null;
                    if (sa.getActivatingPlayer().isHuman()) {
                        tapForConvoke = GuiUtils.chooseOneOrNone("Tap for Convoke? " + newCost.toString(),
                                choices.toArray());
                    } else {
                        // TODO: AI to choose a creature to tap would go here
                        // Probably along with deciding how many creatures to
                        // tap
                    }
                    while ((tapForConvoke != null) && (tapForConvoke instanceof Card) && (untappedCreats.size() != 0)) {
                        final Card workingCard = (Card) tapForConvoke;
                        usableColors = CardUtil.getConvokableColors(workingCard, newCost);

                        if (usableColors.size() != 0) {
                            String chosenColor = usableColors.get(0);
                            if (usableColors.size() > 1) {
                                if (sa.getActivatingPlayer().isHuman()) {
                                    chosenColor = (String) GuiUtils.chooseOne("Convoke for which color?",
                                            usableColors.toArray());
                                } else {
                                    // TODO: AI for choosing which color to
                                    // convoke goes here.
                                }
                            }

                            if (chosenColor.equals("colorless")) {
                                newCost.decreaseColorlessMana(1);
                            } else {
                                String newCostStr = newCost.toString();
                                newCostStr = newCostStr.replaceFirst(
                                        InputPayManaCostUtil.getShortColorString(chosenColor), "");
                                newCost = new ManaCost(newCostStr.trim());
                            }

                            sa.addTappedForConvoke(workingCard);
                            choices.remove(workingCard);
                            untappedCreats.remove(workingCard);
                            if ((choices.size() < 2) || (newCost.getConvertedManaCost() == 0)) {
                                break;
                            }
                        } else {
                            untappedCreats.remove(workingCard);
                        }

                        if (sa.getActivatingPlayer().isHuman()) {
                            tapForConvoke = GuiUtils.chooseOneOrNone("Tap for Convoke? " + newCost.toString(),
                                    choices.toArray());
                        } else {
                            // TODO: AI to choose a creature to tap would go
                            // here
                        }
                    }

                    // will only be null if user cancelled.
                    if (tapForConvoke != null) {
                        // Convoked creats are tapped here with triggers
                        // suppressed,
                        // Then again when payment is done(In
                        // InputPayManaCost.done()) with suppression cleared.
                        // This is to make sure that triggers go off at the
                        // right time
                        // AND that you can't use mana tapabilities of convoked
                        // creatures
                        // to pay the convoked cost.
                        AllZone.getTriggerHandler().suppressMode(TriggerType.Taps);
                        for (final Card c : sa.getTappedForConvoke()) {
                            c.tap();
                        }
                        AllZone.getTriggerHandler().clearSuppression(TriggerType.Taps);

                        manaCost = newCost;
                    }
                }

            }
        } // isSpell

        CardList cardsOnBattlefield = AllZoneUtil.getCardsIn(ZoneType.Battlefield);
        cardsOnBattlefield.add(originalCard);
        final ArrayList<StaticAbility> raiseAbilities = new ArrayList<StaticAbility>();
        final ArrayList<StaticAbility> reduceAbilities = new ArrayList<StaticAbility>();
        final ArrayList<StaticAbility> setAbilities = new ArrayList<StaticAbility>();

        // Sort abilities to apply them in proper order
        for (Card c : cardsOnBattlefield) {
            final ArrayList<StaticAbility> staticAbilities = c.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.getMapParams().get("Mode").equals("RaiseCost")) {
                    raiseAbilities.add(stAb);
                } else if (stAb.getMapParams().get("Mode").equals("ReduceCost")) {
                    reduceAbilities.add(stAb);
                } else if (stAb.getMapParams().get("Mode").equals("SetCost")) {
                    setAbilities.add(stAb);
                }
            }
        }
        // Raise cost
        for (final StaticAbility stAb : raiseAbilities) {
            manaCost = stAb.applyAbility("RaiseCost", spell, manaCost);
        }

        // Reduce cost
        for (final StaticAbility stAb : reduceAbilities) {
            manaCost = stAb.applyAbility("ReduceCost", spell, manaCost);
        }

        // Set cost (only used by Trinisphere) is applied last
        for (final StaticAbility stAb : setAbilities) {
            manaCost = stAb.applyAbility("SetCost", spell, manaCost);
        }

        return manaCost;
    } // GetSpellCostChange

    /**
     * <p>
     * playSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void playSpellAbility(SpellAbility sa) {
        sa.setActivatingPlayer(AllZone.getHumanPlayer());

        sa = AbilityFactoryCharm.setupCharmSAs(sa);

        // Need to check PayCosts, and Ability + All SubAbilities for Target
        boolean newAbility = sa.getPayCosts() != null;
        SpellAbility ability = sa;
        while ((ability != null) && !newAbility) {
            final Target tgt = ability.getTarget();

            newAbility |= tgt != null;
            ability = ability.getSubAbility();
        }

        if (newAbility) {
            final TargetSelection ts = new TargetSelection(sa.getTarget(), sa);
            CostPayment payment = null;
            if (sa.getPayCosts() == null) {
                payment = new CostPayment(new Cost(sa.getSourceCard(), "0", sa.isAbility()), sa);
            } else {
                payment = new CostPayment(sa.getPayCosts(), sa);
            }

            final SpellAbilityRequirements req = new SpellAbilityRequirements(sa, ts, payment);
            req.fillRequirements();
        } else {
            ManaCost manaCost = new ManaCost(sa.getManaCost());
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                manaCost = new ManaCost("0");
            } else {

                manaCost = this.getSpellCostChange(sa, new ManaCost(sa.getManaCost()));
            }
            if (manaCost.isPaid() && (sa.getBeforePayMana() == null)) {
                if (sa.getAfterPayMana() == null) {
                    final Card source = sa.getSourceCard();
                    if (sa.isSpell() && !source.isCopiedSpell()) {
                        sa.setSourceCard(Singletons.getModel().getGameAction().moveToStack(source));
                    }

                    AllZone.getStack().add(sa);
                    if (sa.isTapAbility() && !sa.wasCancelled()) {
                        sa.getSourceCard().tap();
                    }
                    if (sa.isUntapAbility()) {
                        sa.getSourceCard().untap();
                    }
                    return;
                } else {
                    AllZone.getInputControl().setInput(sa.getAfterPayMana());
                }
            } else if (sa.getBeforePayMana() == null) {
                AllZone.getInputControl().setInput(new InputPayManaCost(sa, manaCost));
            } else {
                AllZone.getInputControl().setInput(sa.getBeforePayMana());
            }
        }
    }

    /**
     * <p>
     * playSpellAbility_NoStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param skipTargeting
     *            a boolean.
     */
    public final void playSpellAbilityNoStack(final SpellAbility sa, final boolean skipTargeting) {
        sa.setActivatingPlayer(AllZone.getHumanPlayer());

        if (sa.getPayCosts() != null) {
            final TargetSelection ts = new TargetSelection(sa.getTarget(), sa);
            final CostPayment payment = new CostPayment(sa.getPayCosts(), sa);

            if (!sa.isTrigger()) {
                payment.changeCost();
            }

            final SpellAbilityRequirements req = new SpellAbilityRequirements(sa, ts, payment);
            req.setSkipStack(true);
            req.fillRequirements(skipTargeting);
        } else {
            ManaCost manaCost = new ManaCost(sa.getManaCost());
            if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
                manaCost = new ManaCost("0");
            } else {

                manaCost = this.getSpellCostChange(sa, new ManaCost(sa.getManaCost()));
            }
            if (manaCost.isPaid() && (sa.getBeforePayMana() == null)) {
                if (sa.getAfterPayMana() == null) {
                    AbilityFactory.resolve(sa, false);
                    if (sa.isTapAbility() && !sa.wasCancelled()) {
                        sa.getSourceCard().tap();
                    }
                    if (sa.isUntapAbility()) {
                        sa.getSourceCard().untap();
                    }
                    return;
                } else {
                    AllZone.getInputControl().setInput(sa.getAfterPayMana());
                }
            } else if (sa.getBeforePayMana() == null) {
                AllZone.getInputControl().setInput(new InputPayManaCost(sa, true));
            } else {
                AllZone.getInputControl().setInput(sa.getBeforePayMana());
            }
        }
    }

    /**
     * <p>
     * setComputerCut.
     * </p>
     * 
     * @param computerCut
     *            a {@link forge.Card} object.
     */
    public final void setComputerCut(final Card computerCut) {
        this.computerCut = computerCut;
    }

    /**
     * <p>
     * getComputerCut.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getComputerCut() {
        return this.computerCut;
    }

    /**
     * <p>
     * setStartCut.
     * </p>
     * 
     * @param startCutIn
     *            a boolean.
     */
    public final void setStartCut(final boolean startCutIn) {
        this.startCut = startCutIn;
    }

    /**
     * <p>
     * isStartCut.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isStartCut() {
        return this.startCut;
    }

    private Card humanCut = null;
    private Card computerCut = null;

    /**
     * <p>
     * setHumanCut.
     * </p>
     * 
     * @param humanCut
     *            a {@link forge.Card} object.
     */
    public final void setHumanCut(final Card humanCut) {
        this.humanCut = humanCut;
    }

    /**
     * <p>
     * getHumanCut.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getHumanCut() {
        return this.humanCut;
    }

    /**
     * Gets the cost cutting get multi kicker mana cost paid.
     * 
     * @return the costCuttingGetMultiKickerManaCostPaid
     */
    public int getCostCuttingGetMultiKickerManaCostPaid() {
        return this.costCuttingGetMultiKickerManaCostPaid;
    }

    /**
     * Sets the cost cutting get multi kicker mana cost paid.
     * 
     * @param costCuttingGetMultiKickerManaCostPaid0
     *            the costCuttingGetMultiKickerManaCostPaid to set
     */
    public void setCostCuttingGetMultiKickerManaCostPaid(final int costCuttingGetMultiKickerManaCostPaid0) {
        this.costCuttingGetMultiKickerManaCostPaid = costCuttingGetMultiKickerManaCostPaid0;
    }

    /**
     * Gets the cost cutting get multi kicker mana cost paid colored.
     * 
     * @return the costCuttingGetMultiKickerManaCostPaidColored
     */
    public String getCostCuttingGetMultiKickerManaCostPaidColored() {
        return this.costCuttingGetMultiKickerManaCostPaidColored;
    }

    /**
     * Sets the cost cutting get multi kicker mana cost paid colored.
     * 
     * @param costCuttingGetMultiKickerManaCostPaidColored0
     *            the costCuttingGetMultiKickerManaCostPaidColored to set
     */
    public void setCostCuttingGetMultiKickerManaCostPaidColored(
            final String costCuttingGetMultiKickerManaCostPaidColored0) {
        this.costCuttingGetMultiKickerManaCostPaidColored = costCuttingGetMultiKickerManaCostPaidColored0;
    }
}
