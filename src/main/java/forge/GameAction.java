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
import java.util.List;
import javax.swing.JFrame;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.ApiType;
import forge.card.abilityfactory.effects.AttachEffect;
import forge.card.abilityfactory.effects.CharmEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostPayment;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.card.replacement.ReplacementEffect;
import forge.card.replacement.ReplacementResult;
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
import forge.control.input.InputPayManaCostUtil;
import forge.game.GameEndReason;
import forge.game.GameLossReason;
import forge.game.GameState;
import forge.game.GlobalRuleChange;
import forge.game.MatchController;
import forge.game.event.CardDestroyedEvent;
import forge.game.event.CardRegeneratedEvent;
import forge.game.event.CardSacrificedEvent;
import forge.game.event.DuelOutcomeEvent;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.player.PlayerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
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

    private final GameState game;
    public GameAction(GameState game0) {
        game = game0;
    }

    public final void resetActivationsPerTurn() {
        final List<Card> all = game.getCardsInGame();

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
    public Card changeZone(final Zone zoneFrom, final Zone zoneTo, final Card c, Integer position) {
        if (c.isCopiedSpell()) {
            if ((zoneFrom != null)) {
                zoneFrom.remove(c);
            }
            return c;
        }
        if ((zoneFrom == null) && !c.isToken()) {
            if (position == null) {
                zoneTo.add(c);
            }
            else {
                zoneTo.add(c, position);
            }

            zoneTo.updateLabelObservers();
            return c;
        }

        boolean suppress;
        if ((zoneFrom == null) && !c.isToken()) {
            suppress = true;
        } else if (c.isToken()) {
            suppress = false;
        } else {
            suppress = zoneFrom.equals(zoneTo);
        }

        Card copied = null;
        Card lastKnownInfo = null;

        // Don't copy Tokens, copy only cards leaving the battlefield
        if (c.isToken() || suppress || zoneTo.is(ZoneType.Battlefield) || zoneFrom == null
                || !zoneFrom.is(ZoneType.Battlefield)) {
            lastKnownInfo = c;
            copied = c;
        } else {
            lastKnownInfo = CardUtil.getLKICopy(c);

            if (c.isCloned()) {
                c.switchStates(CardCharacteristicName.Cloner, CardCharacteristicName.Original);
                c.setState(CardCharacteristicName.Original);
                c.clearStates(CardCharacteristicName.Cloner);
                if (c.isFlipCard()) {
                    c.clearStates(CardCharacteristicName.Flipped);
                }
            }
            // reset flip status when card leaves battlefield
            if (zoneFrom.is(ZoneType.Battlefield)) {
                c.setFlipStaus(false);
            }
            copied = Singletons.getModel().getCardFactory().copyCard(c);
            copied.setUnearthed(c.isUnearthed());
            copied.setTapped(false);
            for (final Trigger trigger : copied.getTriggers()) {
                trigger.setHostCard(copied);
            }
            for (final ReplacementEffect repl : copied.getReplacementEffects()) {
                repl.setHostCard(copied);
            }
            if (c.getName().equals("Skullbriar, the Walking Grave")) {
                copied.setCounters(c.getCounters());
            }
        }

        if (!suppress) {
            HashMap<String, Object> repParams = new HashMap<String, Object>();
            repParams.put("Event", "Moved");
            repParams.put("Affected", copied);
            repParams.put("Origin", zoneFrom != null ? zoneFrom.getZoneType() : null);
            repParams.put("Destination", zoneTo.getZoneType());

            ReplacementResult repres = game.getReplacementHandler().run(repParams);
            if (repres != ReplacementResult.NotReplaced) {
                if (game.getStack().isResolving(c) && !zoneTo.is(ZoneType.Graveyard) && repres == ReplacementResult.Prevented) {
                    return Singletons.getModel().getGame().getAction().moveToGraveyard(c);
                }
                return c;
            }
        }

        if (c.wasSuspendCast()) {
            copied = GameAction.addSuspendTriggers(c);
        }

        if (suppress) {
            game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        }

        // "enter the battlefield as a copy" - apply code here
        // but how to query for input here and continue later while the callers assume synchronous result?
        if (position == null) {
            zoneTo.add(copied);
        }
        else {
            zoneTo.add(copied, position);
        }

        // Tokens outside the battlefield disappear immediately.
        if ((copied.isToken() && !zoneTo.is(ZoneType.Battlefield)
            && !((copied.isType("Emblem") || copied.isType("Effect")) && zoneTo.is(ZoneType.Command)))) {
            zoneTo.remove(copied);
        }

        if (zoneFrom != null) {
            if (zoneFrom.is(ZoneType.Battlefield) && c.isCreature()) {
                game.getCombat().removeFromCombat(c);
            }
            zoneFrom.remove(c);
        }

        zoneTo.updateLabelObservers();

        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", lastKnownInfo);
        if (zoneFrom != null) {
            runParams.put("Origin", zoneFrom.getZoneType().name());
        } else {
            runParams.put("Origin", null);
        }
        runParams.put("Destination", zoneTo.getZoneType().name());
        game.getTriggerHandler().runTrigger(TriggerType.ChangesZone, runParams);
        // AllZone.getStack().chooseOrderOfSimultaneousStackEntryAll();

        if (suppress) {
            game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        }

        if (zoneFrom == null) {
            return copied;
        }

        if (zoneFrom.is(ZoneType.Battlefield)) {
            copied.setSuspendCast(false);
            // remove all counters from the card if destination is not the battlefield
            // UNLESS we're dealing with Skullbriar, the Walking Grave
            if (zoneTo.is(ZoneType.Hand) || zoneTo.is(ZoneType.Library) || !c.getName().equals("Skullbriar, the Walking Grave")) {
                copied.clearCounters();
            }
            copied.setState(CardCharacteristicName.Original);
            // Soulbond unpairing
            if (c.isPaired()) {
                c.getPairedWith().setPairedWith(null);
                c.setPairedWith(null);
            }
            // Handle unequipping creatures
            if (copied.isEquipped()) {
                final List<Card> equipments = new ArrayList<Card>(copied.getEquippedBy());
                for (final Card equipment : equipments) {
                    if (equipment.isInPlay()) {
                        equipment.unEquipCard(copied);
                    }
                }
            }
            // Handle unequipping creatures
            if (copied.isEquipped()) {
                final List<Card> equipments = new ArrayList<Card>(copied.getEquippedBy());
                for (final Card equipment : equipments) {
                    if (equipment.isInPlay()) {
                        equipment.unEquipCard(copied);
                    }
                }
            }
            // equipment moving off battlefield
            if (copied.isEquipping()) {
                final Card equippedCreature = copied.getEquipping().get(0);
                if (equippedCreature.isInPlay()) {
                    copied.unEquipCard(equippedCreature);
                }
            }
            // remove enchantments from creatures
            if (copied.isEnchanted()) {
                final List<Card> auras = new ArrayList<Card>(copied.getEnchantedBy());
                for (final Card aura : auras) {
                    aura.unEnchantEntity(copied);
                }
            }
            // unenchant creature if moving aura
            if (copied.isEnchanting()) {
                copied.unEnchantEntity(copied.getEnchanting());
            }
        } else if (zoneFrom.is(ZoneType.Exile) && !zoneTo.is(ZoneType.Battlefield)) {
            // Pull from Eternity used on a suspended card
            if (!zoneTo.is(ZoneType.Graveyard) || !c.getName().equals("Skullbriar, the Walking Grave")) {
                copied.clearCounters();
            }
            copied.clearAdditionalCostsPaid();
            if (copied.isFaceDown()) {
                copied.turnFaceUp();
            }
        } else if (zoneTo.is(ZoneType.Battlefield)) {
            copied.setTimestamp(Singletons.getModel().getGame().getNextTimestamp());
            for (String s : copied.getKeyword()) {
                if (s.startsWith("May be played") || s.startsWith("You may look at this card.")
                        || s.startsWith("May be played by your opponent")
                        || s.startsWith("Your opponent may look at this card.")) {
                    copied.removeAllExtrinsicKeyword(s);
                    copied.removeHiddenExtrinsicKeyword(s);
                }
            }
        } else if (zoneTo.is(ZoneType.Graveyard)) {
            copied.setTimestamp(Singletons.getModel().getGame().getNextTimestamp());
            for (String s : copied.getKeyword()) {
                if (s.startsWith("May be played") || s.startsWith("You may look at this card.")
                        || s.startsWith("May be played by your opponent")
                        || s.startsWith("Your opponent may look at this card.")) {
                    copied.removeAllExtrinsicKeyword(s);
                    copied.removeHiddenExtrinsicKeyword(s);
                }
            }
            copied.clearAdditionalCostsPaid();
            if (copied.isFaceDown()) {
                copied.turnFaceUp();
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
    public final Card moveTo(final Zone zoneTo, Card c) {
        return moveTo(zoneTo, c, null);
    }

    public final Card moveTo(final Zone zoneTo, Card c, Integer position) {
        // Ideally move to should never be called without a prevZone
        // Remove card from Current Zone, if it has one
        final Zone zoneFrom = game.getZoneOf(c);
        // String prevName = prev != null ? prev.getZoneName() : "";

        if (c.hasKeyword("If CARDNAME would leave the battlefield, exile it instead of putting it anywhere else.")
                && !zoneTo.is(ZoneType.Exile)) {
            final PlayerZone removed = c.getOwner().getZone(ZoneType.Exile);
            c.removeAllExtrinsicKeyword("If CARDNAME would leave the battlefield, "
                    + "exile it instead of putting it anywhere else.");
            return this.moveTo(removed, c);
        }

        // Card lastKnownInfo = c;

        c = changeZone(zoneFrom, zoneTo, c, position);

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
            AttachEffect.attachAuraOnIndirectEnterBattlefield(c);
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

        c = Singletons.getModel().getGame().getAction().changeZone(hand, play, c, null);

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
        final Zone oldBattlefield = game.getZoneOf(c);
        if (oldBattlefield == null || oldBattlefield.getZoneType() == ZoneType.Stack) {
            return;
        }
        final PlayerZone newBattlefield = c.getController().getZone(oldBattlefield.getZoneType());

        if (newBattlefield == null || oldBattlefield.equals(newBattlefield)) {
            return;
        }

        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        for (Player p : game.getPlayers()) {
            ((PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield)).setTriggers(false);
        }

        final int tiz = c.getTurnInZone();

        oldBattlefield.remove(c);
        newBattlefield.add(c);
        c.setSickness(true);
        if (c.hasStartOfKeyword("Echo")) {
            c.addExtrinsicKeyword("(Echo unpaid)");
        }
        game.getCombat().removeFromCombat(c);

        c.setTurnInZone(tiz);

        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", c);
        game.getTriggerHandler().runTrigger(TriggerType.ChangesController, runParams);

        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        for (Player p : game.getPlayers()) {
            ((PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield)).setTriggers(true);
        }
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
        final Zone stack = game.getStackZone();
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
        final Zone origZone = game.getZoneOf(c);
        final Player owner = c.getOwner();
        final PlayerZone grave = owner.getZone(ZoneType.Graveyard);
        final PlayerZone exile = owner.getZone(ZoneType.Exile);
        final List<Card> ownerBoard = new ArrayList<Card>(owner.getCardsIn(ZoneType.Battlefield));

        if (c.getName().equals("Nissa's Chosen") && origZone.is(ZoneType.Battlefield)) {
            return this.moveToLibrary(c, -1);
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
            List<Card> cards = new ArrayList<Card>(c.getOwner().getCardsIn(ZoneType.Graveyard));
            for (final Card recoverable : cards) {
                if (recoverable.hasStartOfKeyword("Recover") && !recoverable.equals(c)) {

                    final String recoverCost = recoverable.getKeyword().get(recoverable.getKeywordPosition("Recover"))
                            .split(":")[1];
                    final Cost cost = new Cost(recoverable, recoverCost, true);

                    final Command paidCommand = new Command() {
                        private static final long serialVersionUID = -6357156873861051845L;

                        @Override
                        public void execute() {
                            Singletons.getModel().getGame().getAction().moveToHand(recoverable);
                        }
                    };

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = -7354791599039157375L;

                        @Override
                        public void execute() {
                            Singletons.getModel().getGame().getAction().exile(recoverable);
                        }
                    };

                    final SpellAbility abRecover = new AbilityActivated(recoverable, cost, null) {
                        private static final long serialVersionUID = 8858061639236920054L;

                        @Override
                        public void resolve() {
                            Singletons.getModel().getGame().getAction().moveToHand(recoverable);
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
                            Player p = recoverable.getController();
                            if (p.isHuman()) {
                                GameActionUtil.payCostDuringAbilityResolve(abRecover, abRecover.getPayCosts(),
                                        paidCommand, unpaidCommand, null);
                            } else { // computer
                                if (ComputerUtil.canPayCost(abRecover, p)) {
                                    ComputerUtil.playNoStack(p, abRecover);
                                } else {
                                    Singletons.getModel().getGame().getAction().exile(recoverable);
                                }
                            }
                        }
                    };
                    recoverAbility.setStackDescription(sb.toString());

                    game.getStack().addSimultaneousStackEntry(recoverAbility);
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
        final PlayerZone play = c.getController().getZone(ZoneType.Battlefield);
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
        final Zone p = game.getZoneOf(c);
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
            c.setState(CardCharacteristicName.Original);
        }

        if ((libPosition == -1) || (libPosition > library.size())) {
            libPosition = library.size();
        }

        Card lastKnownInfo = c;
        if (p != null && p.is(ZoneType.Battlefield)) {
            lastKnownInfo = CardUtil.getLKICopy(c);
            c.clearCounters(); // remove all counters
            library.add(Singletons.getModel().getCardFactory().copyCard(c), libPosition);
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
        game.getTriggerHandler().runTrigger(TriggerType.ChangesZone, runParams);

        if (p != null) {
            p.updateLabelObservers();
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
        if (game.isCardExiled(c)) {
            return c;
        }

        final PlayerZone removed = c.getOwner().getZone(ZoneType.Exile);

        return Singletons.getModel().getGame().getAction().moveTo(removed, c);
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
        } else if (name.equals(ZoneType.Command)) {
            final PlayerZone command = c.getOwner().getZone(ZoneType.Command);
            return this.moveTo(command, c);
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
                        Singletons.getModel().getGame().getAction().playSpellAbility(miracle);
                    }
                } else {
                    Spell spell = (Spell) miracle;
                    spell.setActivatingPlayer(card.getOwner());
                    if (spell.canPlayFromEffectAI(false, false)) {
                        ComputerUtil.playStack(miracle, card.getOwner());
                    }
                }
            }
        };

        final StringBuilder sbAct = new StringBuilder();
        sbAct.append(card.getName()).append(" - Miracle.");
        activate.setStackDescription(sbAct.toString());
        activate.setActivatingPlayer(card.getOwner());
        activate.setTrigger(true);

        game.getStack().add(activate);
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
                        Singletons.getModel().getGame().getAction().playSpellAbility(madness);
                    }
                } else {
                    Spell spell = (Spell) madness;
                    if (spell.canPlayFromEffectAI(false, false)) {
                        ComputerUtil.playStack(madness, card.getOwner());
                    }
                }
            }
        };

        final StringBuilder sbAct = new StringBuilder();
        sbAct.append(card.getName()).append(" - Madness.");
        activate.setStackDescription(sbAct.toString());
        activate.setActivatingPlayer(card.getOwner());

        game.getStack().add(activate);
    }

    /**
     * <p>
     * checkEndGameSate.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean checkEndGameState(final MatchController match, final GameState game) {
        // if game is already over return true
        if (game.isGameOver()) {
            return true;
        }

        GameEndReason reason = null;
        // award loses as SBE
        List<Player> losers = null;
        for (Player p : game.getPlayers()) {
            if (p.checkLoseCondition()) { // this will set appropriate outcomes
                // Run triggers
                if (losers == null) {
                    losers = new ArrayList<Player>(3);
                }
                losers.add(p);
            }
        }

        // Has anyone won by spelleffect?
        for (Player p : game.getPlayers()) {
            if (!p.hasWon()) {
                continue;
            }

            // then the rest have lost!
            reason = GameEndReason.WinsGameSpellEffect;
            for (Player pl : game.getPlayers()) {
                if (pl.equals(p)) {
                    continue;
                }

                if (!pl.loseConditionMet(GameLossReason.OpponentWon, p.getOutcome().altWinSourceName)) {
                    reason = null; // they cannot lose!
                } else {
                    if (losers == null) {
                        losers = new ArrayList<Player>(3);
                    }
                    losers.add(p);
                }
            }
            break;
        }

        // need a separate loop here, otherwise ConcurrentModificationException is raised
        if (losers != null) {
            for (Player p : losers) {
                game.onPlayerLost(p);
            }
        }

        // still unclear why this has not caught me conceding
        if (reason == null && Iterables.size(Iterables.filter(game.getPlayers(), Player.Predicates.NOT_LOST)) == 1)
        {
            reason = GameEndReason.AllOpponentsLost;
        }

        // ai's cannot finish their game without human yet - so terminate a game if human has left.
        if (reason == null && !Iterables.any(game.getPlayers(), Predicates.and(Player.Predicates.NOT_LOST, Player.Predicates.isType(PlayerType.HUMAN)))) {
            reason = GameEndReason.AllHumansLost;
        }

        if (reason != null) {
            game.setGameOver();
            match.addGamePlayed(reason, game);
        }

        return reason != null;
    }

    /** */
    public final void checkStaticAbilities() {
        // remove old effects
        game.getStaticEffects().clearStaticEffects();

        // search for cards with static abilities
        final List<Card> allCards = game.getCardsInGame();
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
        for (final String effect : game.getStaticEffects().getStateBasedMap().keySet()) {
            final Command com = GameActionUtil.getCommands().get(effect);
            com.execute();
        }

        GameActionUtil.grantBasicLandsManaAbilities();
    }

    /**
     * <p>
     * checkStateEffects.
     * </p>
     */
    public final void checkStateEffects() {

        // sol(10/29) added for Phase updates, state effects shouldn't be
        // checked during Spell Resolution (except when persist-returning
        if (game.getStack().isResolving()) {
            return;
        }

        final boolean refreeze = game.getStack().isFrozen();
        game.getStack().setFrozen(true);

        final JFrame frame = Singletons.getView().getFrame();
        if (!frame.isDisplayable()) {
            return;
        }

        MatchController match = Singletons.getModel().getMatch();
        if (this.checkEndGameState(match, match.getCurrentGame())) {
            // Clear Simultaneous triggers at the end of the game
            new ViewWinLose(match);
            match.getCurrentGame().getStack().clearSimultaneousStack();
            if (!refreeze) {
                game.getStack().unfreezeStack();
            }
            // Play the win/lose sound
            boolean humanWon = match.getLastGameOutcome().isWinner(Singletons.getControl().getPlayer().getLobbyPlayer());
            Singletons.getModel().getGame().getEvents().post(new DuelOutcomeEvent(humanWon));
            return;
        }

        // do this twice, sometimes creatures/permanents will survive when they
        // shouldn't
        for (int q = 0; q < 9; q++) {

            boolean checkAgain = false;

            this.checkStaticAbilities();

            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            game.getTriggerHandler().runTrigger(TriggerType.Always, runParams);

            for (Card c : game.getCardsIn(ZoneType.Battlefield)) {

                if (c.isEquipped()) {
                    final List<Card> equipments = new ArrayList<Card>(c.getEquippedBy());
                    for (final Card equipment : equipments) {
                        if (!equipment.isInPlay()) {
                            equipment.unEquipCard(c);
                            checkAgain = true;
                        }
                    }
                } // if isEquipped()

                if (c.isEquipping()) {
                    final Card equippedCreature = c.getEquipping().get(0);
                    if (!equippedCreature.isCreature() || !equippedCreature.isInPlay()) {
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
                    final SpellAbility sa = c.getSpells().get(0);

                    Target tgt = null;
                    if (sa != null) {
                        tgt = sa.getTarget();
                    }

                    if (entity instanceof Card) {
                        final Card perm = (Card) entity;
                        ZoneType tgtZone = tgt.getZone().get(0);

                        if (!perm.isInZone(tgtZone) || !perm.canBeEnchantedBy(c)) {
                            c.unEnchantEntity(perm);
                            this.moveToGraveyard(c);
                            checkAgain = true;
                        }
                    } else if (entity instanceof Player) {
                        final Player pl = (Player) entity;
                        boolean invalid = false;

                        if (tgt.canOnlyTgtOpponent() && !c.getController().getOpponent().equals(pl)) {
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

                    if (c.isInPlay() && !c.isEnchanting()) {
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
                        game.getCombat().removeFromCombat(c);
                        checkAgain = true;
                    } else if (c.getNetDefense() <= 0) {
                        // TODO This shouldn't be a destroy, and should happen
                        // before the damage check probably
                        this.destroy(c);
                        game.getCombat().removeFromCombat(c);
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
                if (c.getCounters(CounterType.P1P1) > 0 && c.getCounters(CounterType.M1M1) > 0) {

                    final CounterType p1Counter = CounterType.P1P1;
                    final CounterType m1Counter = CounterType.M1M1;
                    int plusOneCounters = c.getCounters(CounterType.P1P1);
                    int minusOneCounters = c.getCounters(CounterType.M1M1);

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
        List<Card> krajs = AllZoneUtil.getCardsIn(ZoneType.Battlefield).filter(new Predicate<Card>() {
            @Override
            public boolean addCard(Card c) {
                return c.getName().equals("Experiment Kraj");
            }
        });
        List<Card> P1P1s = AllZoneUtil.getCardsIn(ZoneType.Battlefield).filter(new Predicate<Card>() {
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

        this.handleLegendRule();
        this.handlePlaneswalkerRule();
        
        game.getTriggerHandler().runWaitingTriggers();

        if (!refreeze) {
            game.getStack().unfreezeStack();
        }
    } // checkStateEffects()

    /**
     * <p>
     * destroyPlaneswalkers.
     * </p>
     */
    private void handlePlaneswalkerRule() {
        // get all Planeswalkers
        final List<Card> list = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.PLANEWALKERS);

        Card c;
        for (int i = 0; i < list.size(); i++) {
            c = list.get(i);

            if (c.getCounters(CounterType.LOYALTY) <= 0) {
                Singletons.getModel().getGame().getAction().moveToGraveyard(c);
                // Play the Destroy sound
                Singletons.getModel().getGame().getEvents().post(new CardDestroyedEvent());
            }

            final ArrayList<String> types = c.getType();
            for (final String type : types) {
                if (!CardUtil.isAPlaneswalkerType(type)) {
                    continue;
                }

                final List<Card> cl = CardLists.getType(list, type);

                if (cl.size() > 1) {
                    for (final Card crd : cl) {
                        Singletons.getModel().getGame().getAction().moveToGraveyard(crd);
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
    private void handleLegendRule() {
        final List<Card> a = CardLists.getType(game.getCardsIn(ZoneType.Battlefield), "Legendary");
        if (a.isEmpty() || game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule)) {
            return;
        }

        while (!a.isEmpty()) {
            List<Card> b = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals(a.get(0).getName()));
            b = CardLists.getType(b, "Legendary");
            b = CardLists.filter(b, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return !c.isFaceDown();
                }
            });
            a.remove(0);
            if (1 < b.size()) {
                for (int i = 0; i < b.size(); i++) {
                    Singletons.getModel().getGame().getAction().sacrificeDestroy(b.get(i));
                }

                // Play the Destroy sound
                Singletons.getModel().getGame().getEvents().post(new CardDestroyedEvent());
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

        // Play the Sacrifice sound
        Singletons.getModel().getGame().getEvents().post(new CardSacrificedEvent());

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", c);
        game.getTriggerHandler().runTrigger(TriggerType.Sacrificed, runParams);

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
        if (!c.isInPlay() || c.hasKeyword("Indestructible")) {
            return false;
        }

        if (c.isEnchanted()) {
            List<Card> list = new ArrayList<Card>(c.getEnchantedBy());
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card crd) {
                    return crd.hasKeyword("Totem armor");
                }
            });
            CardLists.sortCMC(list);

            if (list.size() != 0) {
                final Card crd;
                if (list.size() == 1) {
                    crd = list.get(0);
                } else {
                    if (c.getController().isHuman()) {
                        crd = GuiChoose.oneOrNone("Select totem armor to destroy", list);
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

                        // Play the Destroy sound
                        Singletons.getModel().getGame().getEvents().post(new CardDestroyedEvent());
                    }
                };

                final StringBuilder sb = new StringBuilder();
                sb.append(crd).append(" - Totem armor: destroy this aura.");
                ability.setStackDescription(sb.toString());

                game.getStack().add(ability);
                return false;
            }
        } // totem armor

        // Play the Destroy sound
        Singletons.getModel().getGame().getEvents().post(new CardDestroyedEvent());

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
                if (c.isInPlay() && c.isCreature()) {
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
        if (!c.isInPlay()) {
            return false;
        }

        final Player owner = c.getOwner();
        if (!(owner.isComputer() || owner.isHuman())) {
            throw new RuntimeException("GameAction : destroy() invalid card.getOwner() - " + c + " " + owner);
        }

        final boolean persist = (c.hasKeyword("Persist") && (c.getCounters(CounterType.M1M1) == 0)) && !c.isToken();

        final boolean undying = (c.hasKeyword("Undying") && (c.getCounters(CounterType.P1P1) == 0)) && !c.isToken();

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
                    if (game.getZoneOf(persistCard).is(ZoneType.Graveyard)) {
                        final PlayerZone ownerPlay = persistCard.getOwner().getZone(ZoneType.Battlefield);
                        final Card card = GameAction.this.moveTo(ownerPlay, persistCard);
                        card.addCounter(CounterType.M1M1, 1, true);
                    }
                }
            };
            persistAb.setStackDescription(newCard.getName() + " - Returning from Persist");
            persistAb.setDescription(newCard.getName() + " - Returning from Persist");
            persistAb.setActivatingPlayer(c.getController());

            game.getStack().addSimultaneousStackEntry(persistAb);
        }

        if (undying) {
            final Card undyingCard = newCard;
            final Ability undyingAb = new Ability(undyingCard, "0") {

                @Override
                public void resolve() {
                    if (game.getZoneOf(undyingCard).is(ZoneType.Graveyard)) {
                        final PlayerZone ownerPlay = undyingCard.getOwner().getZone(ZoneType.Battlefield);
                        final Card card = GameAction.this.moveTo(ownerPlay, undyingCard);
                        card.addCounter(CounterType.P1P1, 1, true);
                    }
                }
            };
            undyingAb.setStackDescription(newCard.getName() + " - Returning from Undying");
            undyingAb.setDescription(newCard.getName() + " - Returning from Undying");
            undyingAb.setActivatingPlayer(c.getController());

            game.getStack().addSimultaneousStackEntry(undyingAb);
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
        if (!c.isInPlay()
                || (c.hasKeyword("Indestructible") && (!c.isCreature() || (c.getNetDefense() > 0)))) {
            return false;
        }

        if (c.canBeShielded() && (c.getShield() > 0)) {
            c.subtractShield();
            c.setDamage(0);
            c.tap();
            c.addRegeneratedThisTurn();
            game.getCombat().removeFromCombat(c);

            // Play the Regen sound
            Singletons.getModel().getGame().getEvents().post(new CardRegeneratedEvent());

            return false;
        }

        if (c.isEnchanted()) {
            List<Card> list = new ArrayList<Card>(c.getEnchantedBy());
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card crd) {
                    return crd.hasKeyword("Totem armor");
                }
            });
            CardLists.sortCMC(list);

            if (list.size() != 0) {
                final Card crd;
                if (list.size() == 1) {
                    crd = list.get(0);
                } else {
                    if (c.getController().isHuman()) {
                        crd = GuiChoose.oneOrNone("Select totem armor to destroy", list);
                    } else {
                        crd = list.get(0);
                    }
                }

                c.setDamage(0);
                this.destroy(crd);
                System.out.println("Totem armor destroyed instead of original card");

                // Play the Destroy sound
                Singletons.getModel().getGame().getEvents().post(new CardDestroyedEvent());

                return false;
            }
        } // totem armor

        // Play the Destroy sound
        Singletons.getModel().getGame().getEvents().post(new CardDestroyedEvent());

        return this.sacrificeDestroy(c);
    }

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

        final List<String> choices = new ArrayList<String>();
        final Player human = Singletons.getControl().getPlayer();
        final Zone zone = game.getZoneOf(c);

        if (c.isLand() && human.canPlayLand()) {
            if (zone.is(ZoneType.Hand) || ((!zone.is(ZoneType.Battlefield)) && c.hasStartOfKeyword("May be played"))) {
                choices.add("Play land");
            }
        }

        final List<SpellAbility> abilities = new ArrayList<SpellAbility>();
        for (SpellAbility sa : c.getSpellAbilities()) {
            sa.setActivatingPlayer(human);
            //add alternative costs as additional spell abilities
            abilities.add(sa);
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
            choice = GuiChoose.oneOrNone("Choose", choices);
        }

        if (choice == null) {
            return false;
        }

        if (choice.equals("Play land")) {
            human.playLand(c);
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
     * playCardWithoutManaCost.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void playCardWithoutManaCost(final Card c) {
        final List<SpellAbility> choices = c.getBasicSpells();
        SpellAbility sa;

        // TODO add Buyback, Kicker, ... , spells here
        if (choices.size() == 0) {
            return;
        } else if (choices.size() == 1) {
            sa = choices.get(0);
        } else {
            sa = GuiChoose.oneOrNone("Choose", choices);
        }

        if (sa == null) {
            return;
        }

        // Ripple causes a crash because it doesn't set the activatingPlayer in
        // this entrance
        if (sa.getActivatingPlayer() == null) {
            sa.setActivatingPlayer(Singletons.getControl().getPlayer());
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
                    sa.setSourceCard(Singletons.getModel().getGame().getAction().moveToStack(c));
                }
            }
            boolean x = sa.getSourceCard().getManaCost().getShardCount(ManaCostShard.X) > 0;

            game.getStack().add(sa, x);
        } else {
            sa.setManaCost("0"); // Beached As
            Singletons.getModel().getMatch().getInput().setInput(sa.getBeforePayMana());
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

        if (game == null || sa.isTrigger()) {
            return manaCost;
        }

        if (spell.isSpell()) {
            if (spell.isDelve()) {
                final int cardsInGrave = originalCard.getController().getCardsIn(ZoneType.Graveyard).size();

                final Player pc = originalCard.getController();
                if (pc.isHuman()) {
                    final Integer[] cntChoice = new Integer[cardsInGrave + 1];
                    for (int i = 0; i <= cardsInGrave; i++) {
                        cntChoice[i] = Integer.valueOf(i);
                    }

                    final Integer chosenAmount = GuiChoose.one("Exile how many cards?", cntChoice);
                    System.out.println("Delve for " + chosenAmount);
                    final List<Card> choices = pc.getCardsIn(ZoneType.Graveyard);
                    final List<Card> chosen = new ArrayList<Card>();
                    for (int i = 0; i < chosenAmount; i++) {
                        final Card nowChosen = GuiChoose.oneOrNone("Exile which card?", choices);

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
                        final List<Card> grave = pc.getZone(ZoneType.Graveyard).getCards();
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
                List<Card> untappedCreats = CardLists.filter(spell.getActivatingPlayer().getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES);
                untappedCreats = CardLists.filter(untappedCreats, CardPredicates.Presets.UNTAPPED);

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
                        tapForConvoke = GuiChoose.oneOrNone("Tap for Convoke? " + newCost.toString(),
                                choices);
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
                                    chosenColor = GuiChoose.one("Convoke for which color?",
                                            usableColors);
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
                                        InputPayManaCostUtil.getShortColorString(chosenColor), "").replaceFirst("  ", " ");
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
                            tapForConvoke = GuiChoose.oneOrNone("Tap for Convoke? " + newCost.toString(), choices);
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
                        for (final Card c : sa.getTappedForConvoke()) {
                            c.setTapped(true);
                        }

                        manaCost = newCost;
                    }
                }

            }
        } // isSpell

        List<Card> cardsOnBattlefield = Lists.newArrayList(game.getCardsIn(ZoneType.Battlefield));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Stack));
        cardsOnBattlefield.addAll(game.getCardsIn(ZoneType.Command));
        if (!cardsOnBattlefield.contains(originalCard)) {
            cardsOnBattlefield.add(originalCard);
        }
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
     * choose optional additional costs. For HUMAN only
     * 
     * @param original
     *            the original sa
     * @return an ArrayList<SpellAbility>.
     */
    public static SpellAbility chooseOptionalAdditionalCosts(final SpellAbility original) {
        final HashMap<String, SpellAbility> map = new HashMap<String, SpellAbility>();
        final ArrayList<SpellAbility> abilities = GameActionUtil.getOptionalAdditionalCosts(original);
        final ArrayList<String> choices = new ArrayList<String>();
        final Player human = Singletons.getControl().getPlayer();
        if (!original.isSpell()) {
            return original;
        }

        for (final SpellAbility sa : abilities) {
            sa.setActivatingPlayer(human);
            choices.add(sa.toString());
            map.put(sa.toString(), sa);
        }

        String choice;
        if (choices.isEmpty()) {
            return null;
        } else if (choices.size() == 1) {
            choice = choices.get(0);
        } else {
            choice = GuiChoose.oneOrNone("Choose", choices);
        }
        final SpellAbility ability = map.get(choice);

        return ability;
    }

    /**
     * <p>
     * playSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void playSpellAbility(SpellAbility sa) {
        sa.setActivatingPlayer(Singletons.getControl().getPlayer());

        if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
            CharmEffect.makeChoices(sa);
        }

        sa = GameAction.chooseOptionalAdditionalCosts(sa);

        if (sa == null) {
            return;
        }

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
                        sa.setSourceCard(Singletons.getModel().getGame().getAction().moveToStack(source));
                    }

                    game.getStack().add(sa);
                    return;
                } else {
                    Singletons.getModel().getMatch().getInput().setInput(sa.getAfterPayMana());
                }
            } else if (sa.getBeforePayMana() == null) {
                Singletons.getModel().getMatch().getInput().setInput(new InputPayManaCost(sa, manaCost));
            } else {
                Singletons.getModel().getMatch().getInput().setInput(sa.getBeforePayMana());
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
        sa.setActivatingPlayer(Singletons.getControl().getPlayer());

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
                    return;
                } else {
                    Singletons.getModel().getMatch().getInput().setInput(sa.getAfterPayMana());
                }
            } else if (sa.getBeforePayMana() == null) {
                Singletons.getModel().getMatch().getInput().setInput(new InputPayManaCost(sa, true));
            } else {
                Singletons.getModel().getMatch().getInput().setInput(sa.getBeforePayMana());
            }
        }
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
