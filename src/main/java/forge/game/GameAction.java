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
package forge.game;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardUtil;
import forge.Command;
import forge.CounterType;
import forge.FThreads;
import forge.GameEntity;
import forge.GameEventType;
import forge.card.CardType;
import forge.card.TriggerReplacementBase;
import forge.card.ability.AbilityFactory;
import forge.card.ability.effects.AttachEffect;
import forge.card.cardfactory.CardFactory;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.replacement.ReplacementResult;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerType;
import forge.card.trigger.ZCTrigger;
import forge.game.ai.ComputerUtil;
import forge.game.ai.ComputerUtilCost;
import forge.game.event.GameEventCardDestroyed;
import forge.game.event.GameEventCardRegenerated;
import forge.game.event.GameEventCardSacrificed;
import forge.game.event.GameEventDuelFinished;
import forge.game.player.GameLossReason;
import forge.game.player.HumanPlay;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.util.maps.CollectionSuppliers;
import forge.util.maps.HashMapOfLists;
import forge.util.maps.MapOfLists;

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
        if (zoneFrom == null && !c.isToken()) {
            if (position == null) {
                zoneTo.add(c);
            } else {
                zoneTo.add(c, position);
            }

            zoneTo.updateLabelObservers();
            return c;
        }

        boolean suppress = !c.isToken() && zoneFrom.equals(zoneTo);

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
            copied = CardFactory.copyCard(c);
            copied.setUnearthed(c.isUnearthed());
            copied.setTapped(false);
            for (final Trigger trigger : copied.getTriggers()) {
                trigger.setHostCard(copied);
            }
            for (final TriggerReplacementBase repl : copied.getReplacementEffects()) {
                repl.setHostCard(copied);
            }
            if (c.getName().equals("Skullbriar, the Walking Grave")) {
                copied.setCounters(c.getCounters());
            }
        }

        if (!suppress) {
            if(zoneFrom == null)
                copied.getOwner().addInboundToken(copied);
            
            HashMap<String, Object> repParams = new HashMap<String, Object>();
            repParams.put("Event", "Moved");
            repParams.put("Affected", copied);
            repParams.put("Origin", zoneFrom != null ? zoneFrom.getZoneType() : null);
            repParams.put("Destination", zoneTo.getZoneType());

            ReplacementResult repres = game.getReplacementHandler().run(repParams);
            if (repres != ReplacementResult.NotReplaced) {
                if (game.getStack().isResolving(c) && !zoneTo.is(ZoneType.Graveyard) && repres == ReplacementResult.Prevented) {
                    return GameAction.this.moveToGraveyard(c);
                }
                return c;
            }
        }
        
        copied.getOwner().removeInboundToken(copied);

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
        } else {
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
        game.getTriggerHandler().runTrigger(TriggerType.ChangesZone, runParams, false);
        // AllZone.getStack().chooseOrderOfSimultaneousStackEntryAll();

        if (suppress) {
            game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        }

        if (zoneFrom == null) {
            return copied;
        }

        // remove all counters from the card if destination is not the battlefield
        // UNLESS we're dealing with Skullbriar, the Walking Grave
        if (!c.isToken() && (zoneTo.is(ZoneType.Hand) || zoneTo.is(ZoneType.Library) || 
                (!zoneTo.is(ZoneType.Battlefield) && !c.getName().equals("Skullbriar, the Walking Grave")))) {
            copied.clearCounters();
        }

        if (!c.isToken() && !zoneTo.is(ZoneType.Battlefield)) {
            copied.getCharacteristics().resetCardColor();
        }

        if (zoneFrom.is(ZoneType.Battlefield) && !c.isToken()) {
            copied.setSuspendCast(false);
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
            copied.clearOptionalCostsPaid();
            if (copied.isFaceDown()) {
                copied.turnFaceUp();
            }
        } else if (zoneTo.is(ZoneType.Battlefield)) {
            copied.setTimestamp(game.getNextTimestamp());
            for (String s : copied.getKeyword()) {
                if (s.startsWith("May be played") || s.startsWith("You may look at this card.")
                        || s.startsWith("May be played by your opponent")
                        || s.startsWith("Your opponent may look at this card.")) {
                    copied.removeAllExtrinsicKeyword(s);
                    copied.removeHiddenExtrinsicKeyword(s);
                }
            }
        } else if (zoneTo.is(ZoneType.Graveyard)) {
            copied.setTimestamp(game.getNextTimestamp());
            for (String s : copied.getKeyword()) {
                if (s.startsWith("May be played") || s.startsWith("You may look at this card.")
                        || s.startsWith("May be played by your opponent")
                        || s.startsWith("Your opponent may look at this card.")) {
                    copied.removeAllExtrinsicKeyword(s);
                    copied.removeHiddenExtrinsicKeyword(s);
                }
            }
            copied.clearOptionalCostsPaid();
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
       // FThreads.assertExecutedByEdt(false); // This code must never be executed from EDT, 
                                             // use FThreads.invokeInNewThread to run code in a pooled thread

        // if a split card is moved, convert it back to its full form before moving (unless moving to stack)
        if (c.isSplitCard() && !zoneTo.is(ZoneType.Stack)) {
            c.setState(CardCharacteristicName.Original);
        }

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
        game.getTriggerHandler().runTrigger(TriggerType.ChangesController, runParams, false);

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

        if (c.getName().equals("Nissa's Chosen") && origZone.is(ZoneType.Battlefield)) {
            return this.moveToLibrary(c, -1);
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
            for (final Card recoverable : grave) {
                if (recoverable.hasStartOfKeyword("Recover") && !recoverable.equals(c)) {
                    handleRecoverAbility(recoverable);
                }
            }
        }
        return c;
    }

    private void handleRecoverAbility(final Card recoverable) {
        final String recoverCost = recoverable.getKeyword().get(recoverable.getKeywordPosition("Recover")).split(":")[1];
        final Cost cost = new Cost(recoverCost, true);

        final SpellAbility abRecover = new AbilityActivated(recoverable, cost, null) {
            private static final long serialVersionUID = 8858061639236920054L;

            @Override
            public void resolve() {
                GameAction.this.moveToHand(recoverable);
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
        abRecover.setActivatingPlayer(recoverable.getController());
        final StringBuilder sb = new StringBuilder();
        sb.append("Recover ").append(recoverable).append("\n");

        final Ability recoverAbility = new Ability(recoverable, ManaCost.ZERO) {
            @Override
            public void resolve() {
                Player p = recoverable.getController();

                if (p.isHuman()) {
                    if ( HumanPlay.payCostDuringAbilityResolve(abRecover, abRecover.getPayCosts(), null, game) )
                        moveToHand(recoverable);
                    else
                        exile(recoverable);
                } else { // computer
                    if (ComputerUtilCost.canPayCost(abRecover, p)) {
                        ComputerUtil.playNoStack(p, abRecover, game);
                    } else {
                        GameAction.this.exile(recoverable);
                    }
                }
            }
        };
        recoverAbility.setStackDescription(sb.toString());

        game.getStack().addSimultaneousStackEntry(recoverAbility);
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
            library.add(CardFactory.copyCard(c), libPosition);
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
        game.getTriggerHandler().runTrigger(TriggerType.ChangesZone, runParams, false);

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

        return moveTo(removed, c);
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
        switch(name) {
            case Hand:          return this.moveToHand(c);
            case Library:       return this.moveToLibrary(c, libPosition);
            case Battlefield:   return this.moveToPlay(c);
            case Graveyard:     return this.moveToGraveyard(c);
            case Exile:         return this.exile(c);
            case Stack:         return this.moveToStack(c);
            default: // sideboard will also get there
                return this.moveTo(c.getOwner().getZone(name), c);
        }
    }

    /**
     * <p>
     * discardMadness.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param player 
     */
    public final void discardMadness(final Card card, final Player player) {
        // Whenever a card with madness is discarded, you may cast it for it's
        // madness cost
        if (card.getMadnessCost() == null) {
            return;
        }

        final SpellAbility madness = card.getFirstSpellAbility().copy();
        madness.setPayCosts(new Cost(card.getMadnessCost(), false));

        final StringBuilder sb = new StringBuilder();
        sb.append(card.getName()).append(" - Cast via Madness");
        madness.setStackDescription(sb.toString());

        // TODO Convert this to a Trigger
        final Ability activate = new Ability(card, ManaCost.ZERO) {
            @Override
            public void resolve() {
                // pay madness cost here.
                card.getOwner().getController().playMadness(madness);
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
    private final GameEndReason checkEndGameState(final GameState game) {

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
        /*
        if (reason == null && !Iterables.any(game.getPlayers(), Predicates.and(Player.Predicates.NOT_LOST, Player.Predicates.isType(PlayerType.HUMAN)))) {
            reason = GameEndReason.AllHumansLost;
        }
        */
        return reason;
    }

    /** */
    public final void checkStaticAbilities() {
        FThreads.assertExecutedByEdt(false);
        
        if (game.isGameOver())
            return;

        // remove old effects
        game.getStaticEffects().clearStaticEffects();
        game.getTriggerHandler().cleanUpTemporaryTriggers();

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
                int layerDelta = a.getLayer() - b.getLayer();
                if( layerDelta != 0) return layerDelta;

                long tsDelta = a.getHostCard().getTimestamp() - b.getHostCard().getTimestamp();
                return tsDelta == 0 ? 0 : tsDelta > 0 ? 1 : -1;
            }
        };
        Collections.sort(staticAbilities, comp);
        for (final StaticAbility stAb : staticAbilities) {
            stAb.applyAbility("Continuous");
        }

        // card state effects like Glorious Anthem
        for (final String effect : game.getStaticEffects().getStateBasedMap().keySet()) {
            final Function<GameState, ?> com = GameActionUtil.getCommands().get(effect);
            com.apply(game);
        }

        GameActionUtil.grantBasicLandsManaAbilities(game);
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

        // final JFrame frame = Singletons.getView().getFrame();
        // if (!frame.isDisplayable()) {
        // return;
        // }

        if (game.isGameOver())
            return;

        // Max: I don't know where to put this! - but since it's a state based action, it must be in check state effects
        if(game.getType() == GameType.Archenemy) 
            game.archenemy904_10();

        
        final boolean refreeze = game.getStack().isFrozen();
        game.getStack().setFrozen(true);

        // do this twice, sometimes creatures/permanents will survive when they
        // shouldn't
        for (int q = 0; q < 9; q++) {

            boolean checkAgain = false;

            this.checkStaticAbilities();

            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            game.getTriggerHandler().runTrigger(TriggerType.Always, runParams, false);

            for (Player p : game.getPlayers()) {
                for (Card c : p.getCardsIn(ZoneType.Battlefield)) {
                    if (!c.getController().equals(p)) {
                        controllerChangeZoneCorrection(c);
                        c.runChangeControllerCommands();
                        checkAgain = true;
                    }
                }
            }

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
                    if (!equippedCreature.isCreature() || !equippedCreature.isInPlay()
                            || !equippedCreature.canBeEquippedBy(c)) {
                        c.unEquipCard(equippedCreature);
                        checkAgain = true;
                    }
                    // make sure any equipment that has become a creature stops equipping
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
                    if (c.getNetDefense() <= 0 || c.getNetDefense() <= c.getDamage()) {
                        this.destroy(c, null);
                        checkAgain = true;
                    }
                    // Soulbond unpairing
                    if (c.isPaired()) {
                        Card partner = c.getPairedWith();
                        if (!partner.isCreature() || c.getController() != partner.getController() || !game.isCardInZone(c, ZoneType.Battlefield)) {
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
            }

            if (game.getTriggerHandler().runWaitingTriggers()) {
                checkAgain = true;
                // Place triggers on stack
                game.getStack().chooseOrderOfSimultaneousStackEntryAll();
            }

            if (this.handleLegendRule()) {
                checkAgain = true;
            }

            if (this.handlePlaneswalkerRule()) {
                checkAgain = true;
            }

            if (!checkAgain) {
                break; // do not continue the loop
            }
        } // for q=0;q<2

        GameEndReason endGame = this.checkEndGameState(game);
        if (endGame != null) {
            // Clear Simultaneous triggers at the end of the game
            game.setGameOver(endGame);
            game.getStack().clearSimultaneousStack();
        }

        if (!refreeze) {
            game.getStack().unfreezeStack();
        }
    } // checkStateEffects()

    /**
     * <p>
     * destroyPlaneswalkers.
     * </p>
     */
    private boolean handlePlaneswalkerRule() {
        // get all Planeswalkers
        final List<Card> list = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.PLANEWALKERS);

        boolean recheck = false;
        Card c;
        for (int i = 0; i < list.size(); i++) {
            c = list.get(i);

            if (c.getCounters(CounterType.LOYALTY) <= 0) {
                moveToGraveyard(c);
                // Play the Destroy sound
                game.fireEvent(new GameEventCardDestroyed());
                recheck = true;
            }

            final ArrayList<String> types = c.getType();
            for (final String type : types) {
                if (!CardType.isAPlaneswalkerType(type)) {
                    continue;
                }

                final List<Card> cl = CardLists.getType(list, type);

                if (cl.size() > 1) {
                    for (final Card crd : cl) {
                        moveToGraveyard(crd);
                    }
                    recheck = true;
                }
            }
        }
        return recheck;
    }

    /**
     * <p>
     * destroyLegendaryCreatures.
     * </p>
     */
    private boolean handleLegendRule() {
        final List<Card> a = CardLists.getType(game.getCardsIn(ZoneType.Battlefield), "Legendary");
        if (a.isEmpty() || game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule)) {
            return false;
        }
        boolean recheck = false;
        final List<Card> yamazaki = CardLists.filter(a, CardPredicates.nameEquals("Brothers Yamazaki"));
        if (yamazaki.size() == 2) {
            a.removeAll(yamazaki);
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
                    sacrificeDestroy(b.get(i));
                }
                recheck = true;
                // Play the Destroy sound
                game.fireEvent(new GameEventCardDestroyed());
            }
        }

        return recheck;
    } // destroyLegendaryCreatures()


    public final boolean sacrifice(final Card c, final SpellAbility source) {
        if (!c.canBeSacrificedBy(source))
            return false;

        this.sacrificeDestroy(c);

        // Play the Sacrifice sound
        game.fireEvent(new GameEventCardSacrificed());

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", c);
        game.getTriggerHandler().runTrigger(TriggerType.Sacrificed, runParams, false);
        return true;
    }

    /**
     * <p>
     * destroy.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean destroy(final Card c, final SpellAbility sa) {
        if (!c.canBeDestroyed()) {
            return false;
        }

        if (c.canBeShielded() && (!c.isCreature() || c.getNetDefense() > 0)
                && (c.getShield() > 0 || c.hasKeyword("If CARDNAME would be destroyed, regenerate it."))) {
            c.subtractShield();
            c.setDamage(0);
            c.tap();
            c.addRegeneratedThisTurn();
            game.getCombat().removeFromCombat(c);

            // Play the Regen sound
            game.fireEvent(new GameEventCardRegenerated());

            return false;
        }

        return this.destroyNoRegeneration(c, sa);
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
    public final boolean destroyNoRegeneration(final Card c, final SpellAbility sa) {
        Player activator = null;
        if (!c.canBeDestroyed())
            return false;

        if (c.isEnchanted()) {
            List<Card> list = new ArrayList<Card>(c.getEnchantedBy());
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card crd) {
                    return crd.hasKeyword("Totem armor");
                }
            });
            CardLists.sortByCmcDesc(list);

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
                final AbilityStatic ability = new AbilityStatic(crd, ManaCost.ZERO) {
                    @Override
                    public void resolve() {
                        GameAction.this.destroy(crd, sa);
                        card.setDamage(0);

                        // Play the Destroy sound
                        game.fireEvent(new GameEventCardDestroyed());
                    }
                };

                final StringBuilder sb = new StringBuilder();
                sb.append(crd).append(" - Totem armor: destroy this aura.");
                ability.setStackDescription(sb.toString());

                game.getStack().add(ability);
                return false;
            }
        } // totem armor
        if (sa != null) {
            activator = sa.getActivatingPlayer();
        }

        // Play the Destroy sound
        game.fireEvent(new GameEventCardDestroyed());
        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", c);
        runParams.put("Causer", activator);
        game.getTriggerHandler().runTrigger(TriggerType.Destroyed, runParams, false);

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
    private static Card addSuspendTriggers(final Card c) {
        if (c.getSVar("HasteFromSuspend").equals("True")) {
            return c;
        }
        c.setSVar("HasteFromSuspend", "True");

        final Command intoPlay = new Command() {
            private static final long serialVersionUID = -4514610171270596654L;

            @Override
            public void run() {
                if (c.isInPlay() && c.isCreature()) {
                    c.addExtrinsicKeyword("Haste");
                }
            } // execute()
        };

        c.addComesIntoPlayCommand(intoPlay);

        final Command loseControl = new Command() {
            private static final long serialVersionUID = -4514610171270596654L;

            @Override
            public void run() {
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

        final boolean persist = (c.hasKeyword("Persist") && (c.getCounters(CounterType.M1M1) == 0)) && !c.isToken();
        final boolean undying = (c.hasKeyword("Undying") && (c.getCounters(CounterType.P1P1) == 0)) && !c.isToken();

        game.getCombat().removeFromCombat(c);

        final Card newCard = this.moveToGraveyard(c);

        // Destroy needs to be called with Last Known Information
        c.executeTrigger(ZCTrigger.DESTROY);

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
            final Ability persistAb = new Ability(persistCard, ManaCost.ZERO) {

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
            final Ability undyingAb = new Ability(undyingCard, ManaCost.ZERO) {

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
     * TODO: Write javadoc for this method.
     * @param targetCard
     * @param activatingPlayer
     */
    public void reveal(List<Card> cards, Player cardOwner) {
        ZoneType zt = cards.isEmpty() ? ZoneType.Hand : game.getZoneOf(cards.get(0)).getZoneType(); 
        for(Player p : game.getPlayers()) {
            if (cardOwner == p /* && zt.isKnown() */) continue;
            p.getController().reveal(cardOwner + " reveals card from " + zt, cards, zt, cardOwner);
        }
    }

    private void handleLeylinesAndChancellors() {
        for (Player p : game.getPlayers()) {
            final List<Card> openingHand = new ArrayList<Card>(p.getCardsIn(ZoneType.Hand));
    
            for (final Card c : openingHand) {
                if (p.isHuman()) {
                    for (String kw : c.getKeyword()) {
                        if (kw.startsWith("MayEffectFromOpeningHand")) {
                            final String effName = kw.split(":")[1];
    
                            final SpellAbility effect = AbilityFactory.getAbility(c.getSVar(effName), c);
                            effect.setActivatingPlayer(p);
                            if (GuiDialog.confirm(c, "Use " + c +"'s  ability?")) {
                                // If we ever let the AI memorize cards in the players
                                // hand, this would be a place to do so.
                                HumanPlay.playSpellAbilityNoStack(p, effect);
                            }
                        }
                    }
                    if (c.getName().startsWith("Leyline of")) {
                        if (GuiDialog.confirm(c, "Use " + c + "'s ability?")) {
                            game.getAction().moveToPlay(c);
                        }
                    }
                } else { // Computer Leylines & Chancellors
                    if (!c.getName().startsWith("Leyline of")) {
                        for (String kw : c.getKeyword()) {
                            if (kw.startsWith("MayEffectFromOpeningHand")) {
                                final String effName = kw.split(":")[1];
    
                                final SpellAbility effect = AbilityFactory.getAbility(c.getSVar(effName), c);
                                effect.setActivatingPlayer(p);
                                // Is there a better way for the AI to decide this?
                                if (effect.doTrigger(false, p)) {
                                    GuiDialog.message("Computer reveals " + c.getName() + "(" + c.getUniqueNumber() + ").");
                                    ComputerUtil.playNoStack(p, effect, game);
                                }
                            }
                        }
                    }
                    if (c.getName().startsWith("Leyline of")
                            && !(c.getName().startsWith("Leyline of Singularity")
                            && (Iterables.any(game.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals("Leyline of Singularity"))))) {
                        game.getAction().moveToPlay(c);
                        //ga.checkStateEffects();
                    }
                }
            }
        }
    }

    public void startGame(final Player firstPlayer) {
        Player first = firstPlayer;
        do { 
            // Draw <handsize> cards
            for (final Player p1 : game.getPlayers()) {
                p1.drawCards(p1.getMaxHandSize());
            }

            game.setAge(GameAge.Mulligan);
            performMulligans(first, game.getType() == GameType.Commander);
            
            // should I restore everyting exiled by Karn here, or before Mulligans is fine?  
            
            game.setAge(GameAge.Play);
            
            // THIS CODE WILL WORK WITH PHASE = NULL {
                if(game.getType() == GameType.Planechase)
                    firstPlayer.initPlane();

                handleLeylinesAndChancellors();
                checkStateEffects();

                // Run Trigger beginning of the game
                final HashMap<String, Object> runParams = new HashMap<String, Object>();
                game.getTriggerHandler().runTrigger(TriggerType.NewGame, runParams, false);
            // }
    
            game.getPhaseHandler().startFirstTurn(first);
            
            first = game.getPhaseHandler().getPlayerTurn();  // needed only for restart
        } while( game.getAge() == GameAge.RestartedByKarn );

        // will pull UI 
        game.fireEvent(new GameEventDuelFinished());
    }
    
    private void performMulligans(final Player firstPlayer, final boolean isCommander) {
        List<Player> whoCanMulligan = Lists.newArrayList(game.getPlayers());
        int offset = whoCanMulligan.indexOf(firstPlayer);
    
        // Have to cycle-shift the list to get the first player on index 0 
        for( int i = 0; i < offset; i++ ) {
            whoCanMulligan.add(whoCanMulligan.remove(0));
        }
        
        boolean[] hasKept = new boolean[whoCanMulligan.size()];
        int[] handSize = new int[whoCanMulligan.size()];
        for( int i = 0; i < whoCanMulligan.size(); i++) {
            hasKept[i] = false;
            handSize[i] = whoCanMulligan.get(i).getZone(ZoneType.Hand).size();
        }
        
    
        MapOfLists<Player, Card> exiledDuringMulligans = new HashMapOfLists<Player, Card>(CollectionSuppliers.<Card>arrayLists());
        
        // rule 103.4b
        boolean isMultiPlayer = game.getPlayers().size() > 2;
        int mulliganDelta = isMultiPlayer ? 0 : 1; 
        
        boolean allKept;
        do {
            allKept = true;
            for (int i = 0; i < whoCanMulligan.size(); i++) {
                if( hasKept[i]) continue;
                
                Player p = whoCanMulligan.get(i);
                List<Card> toMulligan = p.canMulligan() ? p.getController().getCardsToMulligan(isCommander, firstPlayer) : null; 
                if ( toMulligan != null && !toMulligan.isEmpty()) {
                    if( !isCommander ) {
                        toMulligan = new ArrayList<Card>(p.getCardsIn(ZoneType.Hand));
                        for (final Card c : toMulligan) {
                            moveToLibrary(c);
                        }
                        p.shuffle();
                        p.drawCards(handSize[i] - mulliganDelta);
                    } else { 
                        List<Card> toExile = Lists.newArrayList(toMulligan);
                        for(Card c : toExile) {
                            exile(c);
                        }
                        exiledDuringMulligans.addAll(p, toExile);
                        p.drawCards(toExile.size() - 1);
                    } 
                    
                    p.onMulliganned();
                    allKept = false;
                } else {
                    game.getGameLog().add(GameEventType.MULLIGAN, p.getName() + " has kept a hand of " + p.getZone(ZoneType.Hand).size() + " cards");
                    hasKept[i] = true;
                }
            }
            mulliganDelta++;
        } while( !allKept );
        
        if( isCommander )
            for(Entry<Player, Collection<Card>> kv : exiledDuringMulligans.entrySet() ) {
                Player p = kv.getKey();
                Collection<Card> cc = kv.getValue();
                for(Card c : cc) {
                    moveToLibrary(c);
                }
                p.shuffle();
            }
    }
    
    public void invoke(final Runnable proc) {
        if( FThreads.isGameThread() ) {
            proc.run();
        } else
            FThreads.invokeInGameThread(proc);
    }

}
