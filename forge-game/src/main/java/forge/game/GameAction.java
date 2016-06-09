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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import forge.GameCommand;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.AttachEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.card.CounterType;
import forge.game.event.*;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityLayer;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.CollectionSuppliers;
import forge.util.Expressions;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.ThreadUtil;
import forge.util.Visitor;
import forge.util.maps.HashMapOfLists;
import forge.util.maps.MapOfLists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Methods for common actions performed during a game.
 * 
 * @author Forge
 * @version $Id$
 */
public class GameAction {
    private final Game game;

    private boolean holdCheckingStaticAbilities = false;

    public GameAction(Game game0) {
        game = game0;
    }

    public final void resetActivationsPerTurn() {
        final CardCollectionView all = game.getCardsInGame();

        // Reset Activations per Turn
        for (final Card card : all) {
            for (final SpellAbility sa : card.getAllSpellAbilities()) {
                sa.getRestrictions().resetTurnActivations();
            }
        }
    }

    public Card changeZone(final Zone zoneFrom, Zone zoneTo, final Card c, Integer position) {
        if (c.isCopiedSpell() || (c.isImmutable() && zoneTo.is(ZoneType.Exile))) {
            // Remove Effect from command immediately, this is essential when some replacement
            // effects happen during the resolving of a spellability ("the next time ..." effect)
            if (zoneFrom != null) {
                zoneFrom.remove(c);
            }
            return c;
        }
        if (zoneFrom == null && !c.isToken()) {
            zoneTo.add(c, position);
            checkStaticAbilities();
            game.getTriggerHandler().registerActiveTrigger(c, true);
            game.fireEvent(new GameEventCardChangeZone(c, zoneFrom, zoneTo));
            return c;
        }

        boolean toBattlefield = zoneTo.is(ZoneType.Battlefield);
        boolean fromBattlefield = zoneFrom != null && zoneFrom.is(ZoneType.Battlefield);
        boolean toHand = zoneTo.is(ZoneType.Hand);

        // TODO: part of a workaround for suspend-cast creaturs bounced to hand
        boolean zoneChangedEarly = false;
        Zone originalZone = c.getZone();

        //Rule 110.5g: A token that has left the battlefield can't move to another zone
        if (c.isToken() && zoneFrom != null && !fromBattlefield && !zoneFrom.is(ZoneType.Command)) {
            return c;
        }

        // Rules 304.4, 307.4: non-permanents (instants, sorceries) can't enter the battlefield and remain
        // in their previous zone
        if (toBattlefield && !c.isPermanent()) {
            return c;
        }

        // LKI is only needed when something is moved from the battlefield.
        // also it does messup with Blink Effects like Eldrazi Displacer
        if (fromBattlefield && zoneTo != null && !zoneTo.is(ZoneType.Stack) && !zoneTo.is(ZoneType.Flashback)) {
            game.addChangeZoneLKIInfo(c);
        }

        boolean suppress = !c.isToken() && zoneFrom.equals(zoneTo);

        Card copied = null;
        Card lastKnownInfo = null;

        if (c.isSplitCard() && !zoneTo.is(ZoneType.Stack)) {
            c.setState(CardStateName.Original, true);
        }

        if (fromBattlefield && toHand && c.wasSuspendCast()) {
            // TODO: This has to be set early for suspend-cast creatures bounced to hand, otherwise they
            // end up in a state when they are considered on the battlefield. There should be a better solution.
            c.setZone(zoneTo);
            zoneChangedEarly = true;
        }

        // Don't copy Tokens, copy only cards leaving the battlefield
        // and returning to hand (to recreate their spell ability information)
        if (suppress || (!fromBattlefield && !toHand)) {
            lastKnownInfo = c;
            copied = c;
        } else {
            lastKnownInfo = CardUtil.getLKICopy(c);

            if (!c.isToken()) {
                if (c.isCloned()) {
                    c.switchStates(CardStateName.Cloner, CardStateName.Original, false);
                    c.setState(CardStateName.Original, false);
                    c.clearStates(CardStateName.Cloner, false);
                    if (c.isFlipCard()) {
                        c.clearStates(CardStateName.Flipped, false);
                    }
                    c.updateStateForView();
                }

                copied = CardFactory.copyCard(c, false);

                copied.setUnearthed(c.isUnearthed());
                copied.setTapped(false);
                if (fromBattlefield) {
                    // when a card leaves the battlefield, ensure it's in its original state
                    copied.setState(CardStateName.Original, false);
                }
                for (final Trigger trigger : copied.getTriggers()) {
                    trigger.setHostCard(copied);
                }
                for (final ReplacementEffect repl : copied.getReplacementEffects()) {
                    repl.setHostCard(copied);
                }
                if (c.getName().equals("Skullbriar, the Walking Grave")) {
                    copied.setCounters(c.getCounters());
                }

                // ensure that any leftover keyword/type changes are cleared in the state view
                copied.updateStateForView();
            } else { //Token
                copied = c;
            }
        }

        if (!suppress) {
            if (zoneFrom == null) {
                copied.getOwner().addInboundToken(copied);
            }

            HashMap<String, Object> repParams = new HashMap<String, Object>();
            repParams.put("Event", "Moved");
            repParams.put("Affected", copied);
            repParams.put("CardLKI", lastKnownInfo);
            repParams.put("Origin", zoneFrom != null ? zoneFrom.getZoneType() : null);
            repParams.put("Destination", zoneTo.getZoneType());

            ReplacementResult repres = game.getReplacementHandler().run(repParams);
            if (repres != ReplacementResult.NotReplaced) {
                if (zoneChangedEarly) {
                    c.setZone(originalZone); // TODO: part of a workaround for bounced suspend-cast cards 
                }
                if (game.getStack().isResolving(c) && !zoneTo.is(ZoneType.Graveyard) && repres == ReplacementResult.Prevented) {
                	copied.getOwner().removeInboundToken(copied);
                	return moveToGraveyard(c);
                }
                copied.getOwner().removeInboundToken(copied);
                return c;
            }

            if (c.isUnearthed() && (zoneTo.is(ZoneType.Graveyard) || zoneTo.is(ZoneType.Hand) || zoneTo.is(ZoneType.Library))) {
                zoneTo = c.getOwner().getZone(ZoneType.Exile);
                lastKnownInfo = CardUtil.getLKICopy(c);
                c.setUnearthed(false);
            }
        }

        copied.getOwner().removeInboundToken(copied);

        if (c.wasSuspendCast()) {
            copied = GameAction.addSuspendTriggers(c);
        }

        if (suppress) {
            game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        }

        if (zoneFrom != null) {
            if (fromBattlefield && c.isCreature() && game.getCombat() != null) {
                if (!toBattlefield) {
                    game.getCombat().saveLKI(lastKnownInfo);
                }
                game.getCombat().removeFromCombat(c);
            }
            if ((zoneFrom.is(ZoneType.Library) || zoneFrom.is(ZoneType.PlanarDeck) || zoneFrom.is(ZoneType.SchemeDeck))
                    && zoneFrom == zoneTo && position.equals(zoneFrom.size()) && position != 0) {
                position--;
            }
            zoneFrom.remove(c);
        }

        // "enter the battlefield as a copy" - apply code here
        // but how to query for input here and continue later while the callers assume synchronous result?
        zoneTo.add(copied, position, c); // the modified state of the card is also reported here (e.g. for Morbid + Awaken)

        if (fromBattlefield) {
            c.setZone(zoneTo);
            c.setDamage(0); //clear damage after a card leaves the battlefield
            c.setHasBeenDealtDeathtouchDamage(false);
            if (c.isTapped()) {
                c.setTapped(false); //untap card after it leaves the battlefield if needed
                game.fireEvent(new GameEventCardTapped(c, false));
            }
            c.setMustAttackEntity(null);
        }

        // Need to apply any static effects to produce correct triggers
        checkStaticAbilities();
        game.getTriggerHandler().clearInstrinsicActiveTriggers(c, zoneFrom);
        game.getTriggerHandler().registerActiveTrigger(c, false);

        // play the change zone sound
        game.fireEvent(new GameEventCardChangeZone(c, zoneFrom, zoneTo));

        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", lastKnownInfo);
        runParams.put("Origin", zoneFrom != null ? zoneFrom.getZoneType().name() : null);
        runParams.put("Destination", zoneTo.getZoneType().name());
        runParams.put("SpellAbilityStackInstance", game.stack.peek());
        runParams.put("IndividualCostPaymentInstance", game.costPaymentStack.peek());
        game.getTriggerHandler().runTrigger(TriggerType.ChangesZone, runParams, false);
        if (zoneFrom != null && zoneFrom.is(ZoneType.Battlefield)) {
            final HashMap<String, Object> runParams2 = new HashMap<String, Object>();
            runParams2.put("Card", lastKnownInfo);
            runParams2.put("OriginalController", zoneFrom.getPlayer());
            game.getTriggerHandler().runTrigger(TriggerType.ChangesController, runParams2, false);
        }
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
                (!toBattlefield && !c.getName().equals("Skullbriar, the Walking Grave")))) {
            copied.clearCounters();
        }

        if (!c.isToken() && !toBattlefield) {
            copied.clearDevoured();
            copied.clearDelved();
        }

        // rule 504.6: reveal a face-down card leaving the stack 
        if (zoneFrom != null && zoneTo != null && zoneFrom.is(ZoneType.Stack) && !zoneTo.is(ZoneType.Battlefield) && c.isFaceDown()) {
            c.setState(CardStateName.Original, true);
            reveal(new CardCollection(c), c.getOwner(), true, "Face-down card moves from the stack: ");
            c.setState(CardStateName.FaceDown, true);
        }

        if (fromBattlefield) {
            if (!c.isToken()) {
                copied.setSuspendCast(false);
                copied.setState(CardStateName.Original, true);
            }
            // Soulbond unpairing
            if (c.isPaired()) {
                c.getPairedWith().setPairedWith(null);
                if (!c.isToken()) {
                    c.setPairedWith(null);
                }
            }
            // Reveal if face-down
            if (c.isFaceDown()) {
            	c.setState(CardStateName.Original, true);
            	reveal(new CardCollection(c), c.getOwner(), true, "Face-down card leaves the battlefield: ");
            	c.setState(CardStateName.FaceDown, true);
            	copied.setState(CardStateName.Original, true);
            }
            unattachCardLeavingBattlefield(copied);
        } else if (toBattlefield) {
            // reset timestamp in changezone effects so they have same timestamp if ETB simutaneously 
            copied.setTimestamp(game.getNextTimestamp());
            for (String s : copied.getKeywords()) {
                if (s.startsWith("May be played") || s.startsWith("You may look at this card.")
                        || s.startsWith("Your opponent may look at this card.")) {
                    copied.removeAllExtrinsicKeyword(s);
                    copied.removeHiddenExtrinsicKeyword(s);
                }
            }
            for (Player p : game.getPlayers()) {
                copied.getDamageHistory().setNotAttackedSinceLastUpkeepOf(p);
                copied.getDamageHistory().setNotBlockedSinceLastUpkeepOf(p);
                copied.getDamageHistory().setNotBeenBlockedSinceLastUpkeepOf(p);
            }
            if (zoneFrom.is(ZoneType.Graveyard)) {
                // fizzle all "damage done" triggers for cards returning to battlefield from graveyard
                game.getStack().fizzleTriggersOnStackTargeting(copied, TriggerType.DamageDone);
            }
        } else if (zoneTo.is(ZoneType.Graveyard)
        		|| zoneTo.is(ZoneType.Hand)
        		|| zoneTo.is(ZoneType.Library)
        		|| zoneTo.is(ZoneType.Exile)) {
            copied.setTimestamp(game.getNextTimestamp());
            for (String s : copied.getKeywords()) {
                if (s.startsWith("May be played") || s.startsWith("You may look at this card.")
                        || s.startsWith("Your opponent may look at this card.")) {
                    copied.removeAllExtrinsicKeyword(s);
                    copied.removeHiddenExtrinsicKeyword(s);
                }
            }
            copied.clearOptionalCostsPaid();
            if (copied.isFaceDown()) {
                copied.setState(CardStateName.Original, true);
            }
        }

        return copied;
    }

    private static void unattachCardLeavingBattlefield(final Card copied) {
        // Handle unequipping creatures
        if (copied.isEquipped()) {
            for (final Card equipment : copied.getEquippedBy(true)) {
                if (equipment.isInPlay()) {
                    equipment.unEquipCard(copied);
                }
            }
        }
        // Handle unfortifying lands
        if (copied.isFortified()) {
            for (final Card f : copied.getFortifiedBy(true)) {
                if (f.isInPlay()) {
                    f.unFortifyCard(copied);
                }
            }
        }
        // equipment moving off battlefield
        if (copied.isEquipping()) {
            final Card equippedCreature = copied.getEquipping();
            if (equippedCreature.isInPlay()) {
                copied.unEquipCard(equippedCreature);
            }
        }
        // fortifications moving off battlefield
        if (copied.isFortifying()) {
            final Card fortifiedLand = copied.getFortifying();
            if (fortifiedLand.isInPlay()) {
                copied.unFortifyCard(fortifiedLand);
            }
        }
        // remove enchantments from creatures
        if (copied.isEnchanted()) {
            for (final Card aura : copied.getEnchantedBy(true)) {
                aura.unEnchantEntity(copied);
            }
        }
        // unenchant creature if moving aura
        if (copied.isEnchanting()) {
            copied.unEnchantEntity(copied.getEnchanting());
        }
    }

    public final Card moveTo(final Zone zoneTo, Card c) {
       // FThreads.assertExecutedByEdt(false); // This code must never be executed from EDT,
                                             // use FThreads.invokeInNewThread to run code in a pooled thread
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
            return moveTo(removed, c);
        }

        // Card lastKnownInfo = c;

        c = changeZone(zoneFrom, zoneTo, c, position);

        if (zoneFrom == null) {
            c.setCastFrom(null);
        } else if (zoneTo.is(ZoneType.Stack)) {
            c.setCastFrom(zoneFrom.getZoneType());
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

    public final void controllerChangeZoneCorrection(final Card c) {
        System.out.println("Correcting zone for " + c.toString());
        final Zone oldBattlefield = game.getZoneOf(c);
        if (oldBattlefield == null || oldBattlefield.getZoneType() == ZoneType.Stack) {
            return;
        }
        final Player original = oldBattlefield.getPlayer();
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
        if (game.getPhaseHandler().inCombat()) {
            game.getCombat().removeFromCombat(c);
        }

        c.setTurnInZone(tiz);
        c.setCameUnderControlSinceLastUpkeep(true);

        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Card", c);
        runParams.put("OriginalController", original);
        game.getTriggerHandler().runTrigger(TriggerType.ChangesController, runParams, false);

        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        for (Player p : game.getPlayers()) {
            ((PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield)).setTriggers(true);
        }
        c.runChangeControllerCommands();
    }

    public final Card moveToStack(final Card c) {
        final Zone stack = game.getStackZone();
        return moveTo(stack, c);
    }

    public final Card moveToGraveyard(Card c) {
        final Player owner = c.getOwner();
        final PlayerZone grave = owner.getZone(ZoneType.Graveyard);
        final PlayerZone exile = owner.getZone(ZoneType.Exile);

        if (c.hasKeyword("If CARDNAME would be put into a graveyard, exile it instead.")) {
            c.removeAllExtrinsicKeyword("If CARDNAME would be put into a graveyard, exile it instead.");
            return moveTo(exile, c);
        }

        // must put card in OWNER's graveyard not controller's
        return moveTo(grave, c);
    }

    public final Card moveToHand(final Card c) {
        final PlayerZone hand = c.getOwner().getZone(ZoneType.Hand);
        return moveTo(hand, c);
    }

    public final Card moveToPlay(final Card c) {
        final PlayerZone play = c.getController().getZone(ZoneType.Battlefield);
        return moveTo(play, c);
    }

    public final Card moveToPlay(final Card c, final Player p) {
        // move to a specific player's Battlefield
        final PlayerZone play = p.getZone(ZoneType.Battlefield);
        return moveTo(play, c);
    }

    public final Card moveToBottomOfLibrary(final Card c) {
        return moveToLibrary(c, -1);
    }

    public final Card moveToLibrary(final Card c) {
        return moveToLibrary(c, 0);
    }

    public final Card moveToLibrary(Card c, int libPosition) {
        final PlayerZone library = c.getOwner().getZone(ZoneType.Library);
        if (libPosition == -1 || libPosition > library.size()) {
            libPosition = library.size();
        }
        return changeZone(game.getZoneOf(c), library, c, libPosition);
    }

    public final Card moveToVariantDeck(Card c, ZoneType zone, int deckPosition) {
        final PlayerZone deck = c.getOwner().getZone(zone);
        if (deckPosition == -1 || deckPosition > deck.size()) {
            deckPosition = deck.size();
        }
        return changeZone(game.getZoneOf(c), deck, c, deckPosition);
    }

    public final Card exile(final Card c) {
        if (game.isCardExiled(c)) {
            return c;
        }
        final PlayerZone removed = c.getOwner().getZone(ZoneType.Exile);
        return moveTo(removed, c);
    }

    public final Card moveTo(final ZoneType name, final Card c) {
        return moveTo(name, c, 0);
    }

    public final Card moveTo(final ZoneType name, final Card c, final int libPosition) {
        // Call specific functions to set PlayerZone, then move onto moveTo
        switch(name) {
            case Hand:          return moveToHand(c);
            case Library:       return moveToLibrary(c, libPosition);
            case Battlefield:   return moveToPlay(c);
            case Graveyard:     return moveToGraveyard(c);
            case Exile:         return exile(c);
            case Stack:         return moveToStack(c);
            case PlanarDeck:    return moveToVariantDeck(c, ZoneType.PlanarDeck, libPosition);
            case SchemeDeck:    return moveToVariantDeck(c, ZoneType.SchemeDeck, libPosition);
            default: // sideboard will also get there
                return moveTo(c.getOwner().getZone(name), c);
        }
    }

    // Temporarily disable (if mode = true) actively checking static abilities.
    private void setHoldCheckingStaticAbilities(boolean mode) {
        holdCheckingStaticAbilities = mode;
    }

    private boolean isCheckingStaticAbilitiesOnHold() {
        return holdCheckingStaticAbilities;
    }

    public final void checkStaticAbilities() {
        checkStaticAbilities(true, new CardCollection());
    }
    public final void checkStaticAbilities(final boolean runEvents, final Set<Card> affectedCards) {
        if (isCheckingStaticAbilitiesOnHold()) {
            return;
        }
        if (game.isGameOver()) {
            return;
        }

        // remove old effects
        game.getStaticEffects().clearStaticEffects(affectedCards);
        game.getTriggerHandler().cleanUpTemporaryTriggers();
        game.getReplacementHandler().cleanUpTemporaryReplacements();

        for (final Player p : game.getPlayers()) {
            p.getManaPool().restoreColorReplacements();
        }

        // search for cards with static abilities
        final FCollection<StaticAbility> staticAbilities = new FCollection<StaticAbility>();
        final CardCollection staticList = new CardCollection();

        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public void visit(final Card c) {
                for (int i = 0; i < c.getStaticAbilities().size(); i++) {
                    final StaticAbility stAb = c.getStaticAbilities().get(i);
                    if (stAb.getMapParams().get("Mode").equals("Continuous")) {
                        staticAbilities.add(stAb);
                    }
                    if (stAb.isTemporary()) {
                        c.removeStaticAbility(stAb);
                        i--;
                    }
                 }
                 if (!c.getStaticCommandList().isEmpty()) {
                     staticList.add(c);
                 }
            }
        });

        final Comparator<StaticAbility> comp = new Comparator<StaticAbility>() {
            @Override
            public int compare(final StaticAbility a, final StaticAbility b) {
                return Long.compare(a.getHostCard().getTimestamp(), b.getHostCard().getTimestamp());
            }
        };
        Collections.sort(staticAbilities, comp);

        final Map<StaticAbility, CardCollectionView> affectedPerAbility = Maps.newHashMap();
        for (final StaticAbilityLayer layer : StaticAbilityLayer.CONTINUOUS_LAYERS) {
            for (final StaticAbility stAb : staticAbilities) {
                final CardCollectionView previouslyAffected = affectedPerAbility.get(stAb);
                final CardCollectionView affectedHere;
                if (previouslyAffected == null) {
                    affectedHere = stAb.applyContinuousAbility(layer);
                    if (affectedHere != null) {
                        affectedPerAbility.put(stAb, affectedHere);
                    }
                } else {
                    affectedHere = previouslyAffected;
                    stAb.applyContinuousAbility(layer, previouslyAffected);
                } 
            }
        }

        for (final CardCollectionView affected : affectedPerAbility.values()) {
            if (affected != null) {
                Iterables.addAll(affectedCards, affected);
            }
        }

        final CardCollection lands = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.LANDS);
        GameActionUtil.grantBasicLandsManaAbilities(lands);

        for (final Card c : staticList) {
            for (int i = 0; i < c.getStaticCommandList().size(); i++) {
                final Object[] staticCheck = c.getStaticCommandList().get(i);
                final String leftVar = (String) staticCheck[0];
                final String rightVar = (String) staticCheck[1];
                final Card affected = (Card) staticCheck[2];
                // calculate the affected card
                final int sVar = AbilityUtils.calculateAmount(affected, leftVar, null);
                final String svarOperator = rightVar.substring(0, 2);
                final String svarOperand = rightVar.substring(2);
                final int operandValue = AbilityUtils.calculateAmount(c, svarOperand, null);
                if (Expressions.compare(sVar, svarOperator, operandValue)) {
                    ((GameCommand) staticCheck[3]).run();
                    c.getStaticCommandList().remove(i);
                    i--;
                    affectedCards.add(c);
                }
            }
        }
        // Exclude cards in hidden zones from update
        Iterator<Card> it = affectedCards.iterator();
        while (it.hasNext()) {
            Card c = it.next();
            if (c.isInZone(ZoneType.Library)) {
                it.remove();
            }
        }

        for (Player p : game.getPlayers()) {
            for (Card c : p.getCardsIn(ZoneType.Battlefield).threadSafeIterable()) {
                if (!c.getController().equals(p)) {
                    controllerChangeZoneCorrection(c);
                    affectedCards.add(c);
                }
                if (c.isCreature() && c.isPaired()) {
                    Card partner = c.getPairedWith();
                    if (!partner.isCreature() || c.getController() != partner.getController() || !c.isInZone(ZoneType.Battlefield)) {
                        c.setPairedWith(null);
                        partner.setPairedWith(null);
                        affectedCards.add(c);
                    }
                }
            }
        }

        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        game.getTriggerHandler().runTrigger(TriggerType.Always, runParams, false);

        if (runEvents && !affectedCards.isEmpty()) {
            game.fireEvent(new GameEventCardStatsChanged(affectedCards));
        }
    }

    public final void checkStateEffects(final boolean runEvents) {
        checkStateEffects(runEvents, new HashSet<Card>());
    }
    public final void checkStateEffects(final boolean runEvents, final Set<Card> affectedCards) {
        // sol(10/29) added for Phase updates, state effects shouldn't be
        // checked during Spell Resolution (except when persist-returning
        if (game.getStack().isResolving()) {
            return;
        }

        if (game.isGameOver()) {
            return;
        }

        // Max: I don't know where to put this! - but since it's a state based action, it must be in check state effects
        if (game.getRules().hasAppliedVariant(GameType.Archenemy)
                || game.getRules().hasAppliedVariant(GameType.ArchenemyRumble)) {
            game.archenemy904_10();
        }

        final boolean refreeze = game.getStack().isFrozen();
        game.getStack().setFrozen(true);
        game.getTracker().freeze(); //prevent views flickering during while updating for state-based effects

        // check the game over condition early for win conditions such as Platinum Angel + Hurricane lethal for both players
        checkGameOverCondition();

        // do this multiple times, sometimes creatures/permanents will survive when they shouldn't
        for (int q = 0; q < 9; q++) {
            checkStaticAbilities(false, affectedCards);
            boolean checkAgain = false;

            for (final Player p : game.getPlayers()) {
                for (final ZoneType zt : ZoneType.values()) {
                    if (zt == ZoneType.Battlefield) {
                        continue;
                    }
                    final Iterable<Card> cards = p.getCardsIn(zt).threadSafeIterable();
                    for (final Card c : cards) {
                        // If a token is in a zone other than the battlefield, it ceases to exist.
                        checkAgain |= stateBasedAction704_5d(c);
                    }
                }
            }
            List<Card> noRegCreats = null;
            List<Card> desCreats = null;
            for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
                if (c.isCreature()) {
                    // Rule 704.5f - Put into grave (no regeneration) for toughness <= 0
                    if (c.getNetToughness() <= 0) {
                        if (noRegCreats == null) {
                            noRegCreats = new LinkedList<Card>();
                        }
                        noRegCreats.add(c);
                        checkAgain = true;
                    } else if (c.hasKeyword("CARDNAME can't be destroyed by lethal damage unless lethal damage dealt by a single source is marked on it.")) {
                        for (final Integer dmg : c.getReceivedDamageFromThisTurn().values()) {
                            if (c.getNetToughness() <= dmg.intValue()) {
                                if (desCreats == null) {
                                    desCreats = new LinkedList<Card>();
                                }
                                desCreats.add(c);
                                checkAgain = true;
                                break;
                            }
                        }
                    }
                    // Rule 704.5g - Destroy due to lethal damage
                    // Rule 704.5h - Destroy due to deathtouch
                    else if (c.getNetToughness() <= c.getDamage() || c.hasBeenDealtDeathtouchDamage()) {
                        if (desCreats == null) {
                            desCreats = new LinkedList<Card>();
                        }
                        desCreats.add(c);
                        c.setHasBeenDealtDeathtouchDamage(false);
                        checkAgain = true;
                    }
                }

                checkAgain |= stateBasedAction704_5n(c); // Auras attached to illegal or not attached go to graveyard
                checkAgain |= stateBasedAction704_5p(c); // Equipment and Fortifications

                if (c.isCreature() && c.isEnchanting()) { // Rule 704.5q - Creature attached to an object or player, becomes unattached
                    c.unEnchantEntity(c.getEnchanting());
                    checkAgain = true;
                }

                checkAgain |= stateBasedAction704_5r(c); // annihilate +1/+1 counters with -1/-1 ones

                if (c.getCounters(CounterType.DREAM) > 7 && c.hasKeyword("CARDNAME can't have more than seven dream counters on it.")) {
                    c.subtractCounter(CounterType.DREAM,  c.getCounters(CounterType.DREAM) - 7);
                    checkAgain = true;
                }
            }

            // only check static abilities once after destroying all the creatures
            // (e.g. helpful for Erebos's Titan and another creature dealing lethal damage to each other simultaneously)
            setHoldCheckingStaticAbilities(true);
            if (noRegCreats != null) {
                for (Card c : noRegCreats) {
                    sacrificeDestroy(c);
                }
            }
            if (desCreats != null) {
                for (Card c : desCreats) {
                    destroy(c, null);
                }
            }
            setHoldCheckingStaticAbilities(false);
            checkStaticAbilities();

            if (game.getTriggerHandler().runWaitingTriggers()) {
                checkAgain = true;
            }

            for (Player p : game.getPlayers()) {
                if (handleLegendRule(p)) {
                    checkAgain = true;
                }

                if (handlePlaneswalkerRule(p)) {
                    checkAgain = true;
                }
            }
            // 704.5m World rule
            checkAgain |= handleWorldRule();

            if (game.getCombat() != null) {
                game.getCombat().removeAbsentCombatants();
            }
            if (!checkAgain) {
                break; // do not continue the loop
            }
        } // for q=0;q<9

        game.getTracker().unfreeze();

        if (runEvents && !affectedCards.isEmpty()) {
            game.fireEvent(new GameEventCardStatsChanged(affectedCards));
        }

        // recheck the game over condition at this point to make sure no other win conditions apply now.
        // TODO: is this necessary at this point if it's checked early above anyway?
        if (!game.isGameOver()) {
            checkGameOverCondition();
        }
        
        if (game.getAge() != GameStage.Play) {
            return;
        }
        game.getTriggerHandler().resetActiveTriggers();
        // Resetting triggers may result in needing to check static abilities again. For example,
        // if the legendary rule was invoked on a Thespian's Stage that just copied Dark Depths, the
        // trigger reset above will activate the copy's Always trigger, which needs to be triggered at
        // this point.
        checkStaticAbilities(false, affectedCards);

        if (!refreeze) {
            game.getStack().unfreezeStack();
        }
    }

    private boolean stateBasedAction704_5n(Card c) {
        boolean checkAgain = false;
        if (!c.isAura()) {
            return false;
        }

        // Check if Card Aura is attached to is a legal target
        final GameEntity entity = c.getEnchanting();
        SpellAbility sa = c.getFirstAttachSpell();

        TargetRestrictions tgt = null;
        if (sa != null) {
            tgt = sa.getTargetRestrictions();
        }

        if (entity instanceof Card) {
            final Card perm = (Card) entity;
            final ZoneType tgtZone = tgt.getZone().get(0);

            if (!perm.isInZone(tgtZone) || !perm.canBeEnchantedBy(c, true) || (perm.isPhasedOut() && !c.isPhasedOut())) {
                c.unEnchantEntity(perm);
                moveToGraveyard(c);
                checkAgain = true;
            }
        } else if (entity instanceof Player) {
            final Player pl = (Player) entity;
            boolean invalid = false;

            if (tgt.canOnlyTgtOpponent() && !c.getController().isOpponentOf(pl)) {
                invalid = true;
            }
            else if (pl.hasProtectionFrom(c)) {
                invalid = true;
            }
            if (invalid) {
                c.unEnchantEntity(pl);
                moveToGraveyard(c);
                checkAgain = true;
            }
        }

        if (c.isInPlay() && !c.isEnchanting()) {
            moveToGraveyard(c);
            checkAgain = true;
        }
        return checkAgain;
    }

    private boolean stateBasedAction704_5p(Card c) {
        boolean checkAgain = false;
        if (c.isEquipped()) {
            for (final Card equipment : c.getEquippedBy(true)) {
                if (!equipment.isInPlay()) {
                    equipment.unEquipCard(c);
                    checkAgain = true;
                }
            }
        }

        if (c.isFortified()) {
            for (final Card f : c.getFortifiedBy(true)) {
                if (!f.isInPlay()) {
                    f.unFortifyCard(c);
                    checkAgain = true;
                }
            }
        }

        if (c.isEquipping()) {
            final Card equippedCreature = c.getEquipping();
            if (!equippedCreature.isCreature() || !equippedCreature.isInPlay()
                    || !equippedCreature.canBeEquippedBy(c)
                    || (equippedCreature.isPhasedOut() && !c.isPhasedOut())
                    || !c.isEquipment()) {
                c.unEquipCard(equippedCreature);
                checkAgain = true;
            }
            // make sure any equipment that has become a creature stops equipping
            if (c.isCreature()) {
                c.unEquipCard(equippedCreature);
                checkAgain = true;
            }
        }

        if (c.isFortifying()) {
            final Card fortifiedLand = c.getFortifying();
            if (!fortifiedLand.isLand() || !fortifiedLand.isInPlay()
                    || (fortifiedLand.isPhasedOut() && !c.isPhasedOut())) {
                c.unFortifyCard(fortifiedLand);
                checkAgain = true;
            }
            // make sure any fortification that has become a creature stops fortifying
            if (c.isCreature()) {
                c.unFortifyCard(fortifiedLand);
                checkAgain = true;
            }
        }
        return checkAgain;
    }

    private boolean stateBasedAction704_5r(Card c) {
        boolean checkAgain = false;
        int plusOneCounters = c.getCounters(CounterType.P1P1);
        int minusOneCounters = c.getCounters(CounterType.M1M1);
        if (plusOneCounters > 0 && minusOneCounters > 0) {
            int remove = Math.min(plusOneCounters, minusOneCounters);
            // If a permanent has both a +1/+1 counter and a -1/-1 counter on it,
            // N +1/+1 and N -1/-1 counters are removed from it, where N is the
            // smaller of the number of +1/+1 and -1/-1 counters on it.
            // This should fire remove counters trigger
            c.subtractCounter(CounterType.P1P1, remove);
            c.subtractCounter(CounterType.M1M1, remove);
            checkAgain = true;
        }
        return checkAgain;
    }

    // If a token is in a zone other than the battlefield, it ceases to exist.
    private boolean stateBasedAction704_5d(Card c) {
        boolean checkAgain = false;
        if (c.isToken()) {
            final Zone zoneFrom = game.getZoneOf(c);
            if (!zoneFrom.is(ZoneType.Battlefield) && !zoneFrom.is(ZoneType.Command)) {
                zoneFrom.remove(c);
                checkAgain = true;
            }
        }
        return checkAgain;
    }

    public void checkGameOverCondition() {
        // award loses as SBE
        List<Player> losers = null;
        FCollectionView<Player> allPlayers = game.getPlayers();
        for (Player p : allPlayers) {
            if (p.checkLoseCondition()) { // this will set appropriate outcomes
                // Run triggers
                if (losers == null) {
                    losers = new ArrayList<Player>(3);
                }
                losers.add(p);
            }
        }

        GameEndReason reason = null;
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

        if (reason == null) {
        	List<Player> notLost = new ArrayList<Player>();
        	Set<Integer> teams = new HashSet<Integer>();
        	for (Player p : allPlayers) {
                if (p.getOutcome() == null || p.getOutcome().hasWon()) {
                	notLost.add(p);
                	teams.add(p.getTeam());
                }
        	}
            int cntNotLost = notLost.size();
            if (cntNotLost == 1) {
                reason = GameEndReason.AllOpponentsLost;
            }
            else if (cntNotLost == 0) {
                reason = GameEndReason.Draw;
            }
            else if (teams.size() == 1) {
                reason = GameEndReason.AllOpposingTeamsLost;
            }
            else {
                return;
            }
        }

        // Clear Simultaneous triggers at the end of the game
        game.setGameOver(reason);
        game.getStack().clearSimultaneousStack();
    }

    private boolean handlePlaneswalkerRule(Player p) {
        // get all Planeswalkers
        final List<Card> list = CardLists.filter(p.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.PLANEWALKERS);

        boolean recheck = false;
        final Multimap<String, Card> uniqueWalkers = ArrayListMultimap.create();
        for (Card c : list) {
            if (c.getCounters(CounterType.LOYALTY) <= 0) {
                moveToGraveyard(c);
                // Play the Destroy sound
                game.fireEvent(new GameEventCardDestroyed());
                recheck = true;
            }


            for (final String type : c.getType()) {
                if (CardType.isAPlaneswalkerType(type)) {
                    uniqueWalkers.put(type, c);
                }
            }
        }

        for (String key : uniqueWalkers.keySet()) {
            Collection<Card> duplicates = uniqueWalkers.get(key);
            if (duplicates.size() < 2) {
                continue;
            }

            recheck = true;

            Card toKeep = p.getController().chooseSingleEntityForEffect(new CardCollection(duplicates), new AbilitySub(ApiType.InternalLegendaryRule, null, null, null), "You have multiple planeswalkers of type \""+key+"\"in play.\n\nChoose one to stay on battlefield (the rest will be moved to graveyard)");
            for (Card c: duplicates) {
                if (c != toKeep) {
                    moveToGraveyard(c);
                }
            }
        }
        return recheck;
    }

    private boolean handleLegendRule(Player p) {
        final List<Card> a = CardLists.getType(p.getCardsIn(ZoneType.Battlefield), "Legendary");
        if (a.isEmpty() || game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLegendRule)) {
            return false;
        }
        boolean recheck = false;
        List<Card> yamazaki = CardLists.getKeyword(a, "Legend rule doesn't apply to CARDNAME.");
        a.removeAll(yamazaki);


        Multimap<String, Card> uniqueLegends = ArrayListMultimap.create();
        for (Card c : a) {
            if (!c.isFaceDown()) {
                uniqueLegends.put(c.getName(), c);
            }
        }

        for (String name : uniqueLegends.keySet()) {
            Collection<Card> cc = uniqueLegends.get(name);
            if (cc.size() < 2) {
                continue;
            }

            recheck = true;

            Card toKeep = p.getController().chooseSingleEntityForEffect(new CardCollection(cc), new AbilitySub(ApiType.InternalLegendaryRule, null, null, null), "You have multiple legendary permanents named \""+name+"\" in play.\n\nChoose the one to stay on battlefield (the rest will be moved to graveyard)");
            for (Card c: cc) {
                if (c != toKeep) {
                    sacrificeDestroy(c);
                }
            }
            game.fireEvent(new GameEventCardDestroyed());
        }

        return recheck;
    }

    private boolean handleWorldRule() {
        final List<Card> worlds = CardLists.getType(game.getCardsIn(ZoneType.Battlefield), "World");
        if (worlds.size() <= 1) {
            return false;
        }

        List<Card> toKeep = new ArrayList<Card>();
        long ts = 0;

        for (final Card crd : worlds) {
            long crdTs = crd.getTimestamp();
            if (crdTs > ts) {
                ts = crdTs;
                toKeep.clear();
            }
            if (crdTs == ts) {
                toKeep.add(crd);
            }
        }

        if (toKeep.size() == 1) {
            worlds.removeAll(toKeep);
        }

        for (Card c : worlds) {
            sacrificeDestroy(c);
            game.fireEvent(new GameEventCardDestroyed());
        }

        return true;
    }

    public final Card sacrifice(final Card c, final SpellAbility source) {
        if (!c.canBeSacrificedBy(source)) {
            return null;
        }

        // Play the Sacrifice sound
        game.fireEvent(new GameEventCardSacrificed());

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        // use a copy that preserves last known information about the card (e.g. for Savra, Queen of the Golgari + Painter's Servant)
        runParams.put("Card", CardFactory.copyCardWithChangedStats(c, false)); 
        runParams.put("Cause", source);
        game.getTriggerHandler().runTrigger(TriggerType.Sacrificed, runParams, false);

        return sacrificeDestroy(c);
    }

    public final boolean destroy(final Card c, final SpellAbility sa) {
        if (!c.canBeDestroyed()) {
            return false;
        }

        if (c.canBeShielded() && (!c.isCreature() || c.getNetToughness() > 0)
                && (c.getShieldCount() > 0 || c.hasKeyword("If CARDNAME would be destroyed, regenerate it."))) {
            c.subtractShield(c.getController().getController().chooseRegenerationShield(c));
            c.setDamage(0);
            c.setHasBeenDealtDeathtouchDamage(false);
            c.tap();
            c.addRegeneratedThisTurn();
            if (game.getCombat() != null) {
                game.getCombat().removeFromCombat(c);
            }

            // Play the Regen sound
            game.fireEvent(new GameEventCardRegenerated());

            return false;
        }
        return destroyNoRegeneration(c, sa);
    }

    public final boolean destroyNoRegeneration(final Card c, final SpellAbility sa) {
        Player activator = null;
        if (!c.canBeDestroyed()) {
            return false;
        }

        if (c.isEnchanted()) {
            for (Card e : c.getEnchantedBy(false)) {
                CardFactoryUtil.refreshTotemArmor(e);
            }
        }

        // Replacement effects
        final HashMap<String, Object> repRunParams = new HashMap<String, Object>();
        repRunParams.put("Event", "Destroy");
        repRunParams.put("Source", sa);
        repRunParams.put("Card", c);
        repRunParams.put("Affected", c);

        if (game.getReplacementHandler().run(repRunParams) != ReplacementResult.NotReplaced) {
            return false;
        }


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

        final Card sacrificed = sacrificeDestroy(c);
        return sacrificed != null;
    }

    private static Card addSuspendTriggers(final Card c) {
        if (c.getSVar("HasteFromSuspend").equals("True")) {
            return c;
        }
        c.setSVar("HasteFromSuspend", "True");

        final GameCommand intoPlay = new GameCommand() {
            private static final long serialVersionUID = -4514610171270596654L;

            @Override
            public void run() {
                if (c.isInPlay() && c.isCreature()) {
                    c.addExtrinsicKeyword("Haste");
                }
            } // execute()
        };

        c.addComesIntoPlayCommand(intoPlay);

        final GameCommand loseControl = new GameCommand() {
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
     * @return the sacrificed Card in its new location, or {@code null} if the
     * sacrifice wasn't successful.
     */
    public final Card sacrificeDestroy(final Card c) {
        if (!c.isInPlay()) {
            return null;
        }

        boolean persist = (c.hasKeyword("Persist") && c.getCounters(CounterType.M1M1) == 0) && !c.isToken();
        boolean undying = (c.hasKeyword("Undying") && c.getCounters(CounterType.P1P1) == 0) && !c.isToken();

        final Card newCard = moveToGraveyard(c);

        // don't trigger persist/undying if the dying has been replaced
        if (newCard == null || !newCard.isInZone(ZoneType.Graveyard)) {
            persist = false;
            undying = false;
        }

        // System.out.println("Card " + c.getName() +
        // " is getting sent to GY, and this turn it got damaged by: ");

        if (persist) {
            final Card persistCard = newCard;
            String effect = String.format("AB$ ChangeZone | Cost$ 0 | Defined$ CardUID_%d" +
            		" | Origin$ Graveyard | Destination$ Battlefield | WithCounters$ M1M1_1",
                    persistCard.getId());
            SpellAbility persistAb = AbilityFactory.getAbility(effect, c);
            persistAb.setTrigger(true);
            persistAb.setStackDescription(newCard.getName() + " - Returning from Persist");
            persistAb.setDescription(newCard.getName() + " - Returning from Persist");
            persistAb.setActivatingPlayer(c.getController());

            game.getStack().addSimultaneousStackEntry(persistAb);
        }

        if (undying) {
            final Card undyingCard = newCard;
            String effect = String.format("AB$ ChangeZone | Cost$ 0 | Defined$ CardUID_%d |" +
            		" Origin$ Graveyard | Destination$ Battlefield | WithCounters$ P1P1_1",
            		undyingCard.getId());
            SpellAbility undyingAb = AbilityFactory.getAbility(effect, c);
            undyingAb.setTrigger(true);
            undyingAb.setStackDescription(newCard.getName() + " - Returning from Undying");
            undyingAb.setDescription(newCard.getName() + " - Returning from Undying");
            undyingAb.setActivatingPlayer(c.getController());

            game.getStack().addSimultaneousStackEntry(undyingAb);
        }
        return newCard;
    }

    public void revealTo(final Card card, final Player to) {
        revealTo(card, Collections.singleton(to));
    }
    public void revealTo(final CardCollectionView cards, final Player to) {
        revealTo(cards, Collections.singleton(to));
    }
    public void revealTo(final Card card, final Iterable<Player> to) {
        revealTo(new CardCollection(card), to);
    }
    public void revealTo(final CardCollectionView cards, final Iterable<Player> to) {
        if (cards.isEmpty()) {
            return;
        }

        final ZoneType zone = cards.getFirst().getZone().getZoneType();
        final Player owner = cards.getFirst().getOwner();
        for (final Player p : to) {
            p.getController().reveal(cards, zone, owner);
        }
    }

    public void reveal(CardCollectionView cards, Player cardOwner) {
        reveal(cards, cardOwner, true);
    }

    public void reveal(CardCollectionView cards, Player cardOwner, boolean dontRevealToOwner) {
        reveal(cards, cardOwner, dontRevealToOwner, null);
    }

    public void reveal(CardCollectionView cards, Player cardOwner, boolean dontRevealToOwner, String messagePrefix) {
        Card firstCard = Iterables.getFirst(cards, null);
        if (firstCard == null) {
            return;
        }
        reveal(cards, game.getZoneOf(firstCard).getZoneType(), cardOwner, dontRevealToOwner, messagePrefix);
    }

    public void reveal(CardCollectionView cards, ZoneType zt, Player cardOwner, boolean dontRevealToOwner, String messagePrefix) {
        for (Player p : game.getPlayers()) {
            if (dontRevealToOwner && cardOwner == p) {
                continue;
            }
            p.getController().reveal(cards, zt, cardOwner, messagePrefix);
        }
    }

    public void revealAnte(String title, Multimap<Player, PaperCard> removedAnteCards) {
        for (Player p : game.getPlayers()) {
            p.getController().revealAnte(title, removedAnteCards);
        }
    }

    /** Delivers a message to all players. (use reveal to show Cards) */
    public void nofityOfValue(SpellAbility saSource, GameObject relatedTarget, String value, Player playerExcept) {
        for (Player p : game.getPlayers()) {
            if (playerExcept == p) continue;
            p.getController().notifyOfValue(saSource, relatedTarget, value);
        }
    }

    public void startGame(GameOutcome lastGameOutcome) {
        Player first = determineFirstTurnPlayer(lastGameOutcome);

        GameType gameType = game.getRules().getGameType();
        do {
            if (game.isGameOver()) { break; } // conceded during "play or draw"

            // FControl should determine now if there are any human players.
            // Where there are none, it should bring up speed controls
            game.fireEvent(new GameEventGameStarted(gameType, first, game.getPlayers()));

            game.setAge(GameStage.Mulligan);
            for (final Player p1 : game.getPlayers()) {
                p1.drawCards(p1.getMaxHandSize());
            }

            performMulligans(first, game.getRules().hasAppliedVariant(GameType.Commander));
            if (game.isGameOver()) { break; } // conceded during "mulligan" prompt

            game.setAge(GameStage.Play);

            //<THIS CODE WILL WORK WITH PHASE = NULL>
            if (game.getRules().hasAppliedVariant(GameType.Planechase)) {
                first.initPlane();
            }

            runOpeningHandActions(first);
            checkStateEffects(true); // why?

            // Run Trigger beginning of the game
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            game.getTriggerHandler().runTrigger(TriggerType.NewGame, runParams, true);
            //</THIS CODE WILL WORK WITH PHASE = NULL>

            game.getPhaseHandler().startFirstTurn(first);

            //after game ends, ensure Auto-Pass canceled for all players so it doesn't apply to next game
            for (Player p : game.getRegisteredPlayers()) {
                p.getController().autoPassCancel();
            }

            first = game.getPhaseHandler().getPlayerTurn();  // needed only for restart
        } while (game.getAge() == GameStage.RestartedByKarn);
    }

    private Player determineFirstTurnPlayer(final GameOutcome lastGameOutcome) {
        // Only cut/coin toss if it's the first game of the match
        Player goesFirst = null;

        // 904.6: in Archenemy games the Archenemy goes first
        if (game != null && game.getRules().hasAppliedVariant(GameType.Archenemy)) {
            for (Player p : game.getPlayers()) {
                if (p.isArchenemy()) {
                    return p;
                }
            }
        }

        // Power Play - Each player with a Power Play in the CommandZone becomes the Starting Player
        Set<Player> powerPlayers = new HashSet<>();
        for (Card c : game.getCardsIn(ZoneType.Command)) {
            if (c.getName().equals("Power Play")) {
                powerPlayers.add(c.getOwner());
            }
        }

        if (!powerPlayers.isEmpty()) {
            List<Player> players = Lists.newArrayList(powerPlayers);
            Collections.shuffle(players);
            return players.get(0);
        }

        boolean isFirstGame = lastGameOutcome == null;
        if (isFirstGame) {
            game.fireEvent(new GameEventFlipCoin()); // Play the Flip Coin sound
            goesFirst = Aggregates.random(game.getPlayers());
        } else {
            for (Player p : game.getPlayers()) {
                if (!lastGameOutcome.isWinner(p.getLobbyPlayer())) {
                    goesFirst = p;
                    break;
                }
            }
        }

        if (goesFirst == null) {
            // This happens in hotseat matches when 2 equal lobbyplayers play.
            // Noone of them has lost, so cannot decide who goes first .
            goesFirst = game.getPlayers().get(0); // does not really matter who plays first - it's controlled from the same computer.
        }

        for (Player p : game.getPlayers()) {
            if (p != goesFirst) {
                p.getController().awaitNextInput(); //show "Waiting for opponent..." while first player chooses whether to go first or keep their hand
            }
        }
        goesFirst = goesFirst.getController().chooseStartingPlayer(isFirstGame);
        return goesFirst;
    }

    private void performMulligans(final Player firstPlayer, final boolean isCommander) {
        List<Player> whoCanMulligan = Lists.newArrayList(game.getPlayers());
        int offset = whoCanMulligan.indexOf(firstPlayer);

        // Have to cycle-shift the list to get the first player on index 0
        for (int i = 0; i < offset; i++) {
            whoCanMulligan.add(whoCanMulligan.remove(0));
        }

        boolean[] hasKept = new boolean[whoCanMulligan.size()];
        int[] handSize = new int[whoCanMulligan.size()];
        for (int i = 0; i < whoCanMulligan.size(); i++) {
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
                if (hasKept[i]) continue;

                Player p = whoCanMulligan.get(i);
                CardCollectionView toMulligan = p.canMulligan() ? p.getController().getCardsToMulligan(firstPlayer) : null;

                if (game.isGameOver()) { // conceded on mulligan prompt
                    return;
                }

                if (toMulligan != null && !toMulligan.isEmpty()) {
                    if (!isCommander) {
                        toMulligan = new CardCollection(p.getCardsIn(ZoneType.Hand));
                        for (final Card c : toMulligan) {
                            moveToLibrary(c);
                        }
                        try {
                            Thread.sleep(100); //delay for a tiny bit to give UI a chance catch up
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        p.shuffle(null);
                        p.drawCards(handSize[i] - mulliganDelta);
                    } else {
                        List<Card> toExile = Lists.newArrayList(toMulligan);
                        for (Card c : toExile) {
                            exile(c);
                        }
                        exiledDuringMulligans.addAll(p, toExile);
                        try {
                            Thread.sleep(100); //delay for a tiny bit to give UI a chance catch up
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        p.drawCards(toExile.size() - 1);
                    }

                    p.onMulliganned();
                    allKept = false;
                } else {
                    game.getGameLog().add(GameLogEntryType.MULLIGAN, p.getName() + " has kept a hand of " + p.getZone(ZoneType.Hand).size() + " cards");
                    hasKept[i] = true;
                }
            }
            mulliganDelta++;
        } while (!allKept);

        if (isCommander) {
            for (Entry<Player, Collection<Card>> kv : exiledDuringMulligans.entrySet()) {
                Player p = kv.getKey();
                Collection<Card> cc = kv.getValue();
                for (Card c : cc) {
                    moveToLibrary(c);
                }
                p.shuffle(null);
            }
        }

        //Vancouver Mulligan
        for(Player p : whoCanMulligan) {
            if (p.getStartingHandSize() > p.getZone(ZoneType.Hand).size()) {
                p.scry(1);
            }
        }
    }


    private void runOpeningHandActions(final Player first) {
        Player takesAction = first;
        do {
            List<SpellAbility> usableFromOpeningHand = new ArrayList<SpellAbility>();

            // Select what can be activated from a given hand
            for (final Card c : takesAction.getCardsIn(ZoneType.Hand)) {
                for (String kw : c.getKeywords()) {
                    if (kw.startsWith("MayEffectFromOpeningHand")) {
                        String[] split = kw.split(":");
                        final String effName = split[1];
                        if (split.length > 2 && split[2].equalsIgnoreCase("!PlayFirst") && first == takesAction) {
                            continue;
                        }

                        final SpellAbility effect = AbilityFactory.getAbility(c.getSVar(effName), c);
                        effect.setActivatingPlayer(takesAction);

                        usableFromOpeningHand.add(effect);
                    }
                }
            }

            // Players are supposed to return the effects in an order they want those to be resolved (Rule 103.5)
            if (!usableFromOpeningHand.isEmpty()) {
                usableFromOpeningHand = takesAction.getController().chooseSaToActivateFromOpeningHand(usableFromOpeningHand);
            }

            for (final SpellAbility sa : usableFromOpeningHand) {
                if (!takesAction.getZone(ZoneType.Hand).contains(sa.getHostCard())) {
                    continue;
                }

                takesAction.getController().playSpellAbilityNoStack(sa, true);
            }
            takesAction = game.getNextPlayerAfter(takesAction);
        } while (takesAction != first);
        // state effects are checked only when someone gets priority
    }

    // Invokes given runnable in Game thread pool - used to start game and perform actions from UI (when game-0 waits for input)
    public void invoke(final Runnable proc) {
        if (ThreadUtil.isGameThread()) {
            proc.run();
        }
        else {
            ThreadUtil.invokeInGameThread(proc);
        }
    }

}
