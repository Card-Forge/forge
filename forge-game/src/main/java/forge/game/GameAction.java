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

import com.google.common.base.Predicate;
import com.google.common.collect.*;
import forge.GameCommand;
import forge.StaticData;
import forge.card.CardStateName;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.AttachEffect;
import forge.game.card.*;
import forge.game.event.*;
import forge.game.keyword.KeywordInterface;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityPredicates;
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
import forge.util.*;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.maps.HashMapOfLists;
import forge.util.maps.MapOfLists;

import java.util.*;
import java.util.Map.Entry;

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

    public Card changeZone(final Zone zoneFrom, Zone zoneTo, final Card c, Integer position, SpellAbility cause) {
        return changeZone(zoneFrom, zoneTo, c, position, cause, null);
    }
    
    public Card changeZone(final Zone zoneFrom, Zone zoneTo, final Card c, Integer position, SpellAbility cause, Map<String, Object> params) {
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

        if (c.isSplitCard()) {
            boolean resetToOriginal = false;

            if (c.isManifested()) {
                if (zoneFrom.is(ZoneType.Battlefield)) {
                    // Make sure the card returns from the battlefield as the original card with two halves
                    resetToOriginal = true;
                }
            } else {
                if (!zoneTo.is(ZoneType.Stack)) {
                    // For regular splits, recreate the original state unless the card is going to stack as one half
                    resetToOriginal = true;
                }
            }

            if (resetToOriginal) {
                c.setState(CardStateName.Original, true);
            }
        }

        // Cards returned from exile face-down must be reset to their original state, otherwise
        // all sort of funky shenanigans may happen later (e.g. their ETB replacement effects are set
        // up on the wrong card state etc.).
        if (c.isFaceDown() && (fromBattlefield || (toHand && zoneFrom.is(ZoneType.Exile)))) {
            c.setState(CardStateName.Original, true);
            c.runFaceupCommands();
        }

        // Clean up the temporary Dash SVar when the Dashed card leaves the battlefield
        if (fromBattlefield && c.getSVar("EndOfTurnLeavePlay").equals("Dash")) {
            c.removeSVar("EndOfTurnLeavePlay");
        }

        // Clean up temporary variables such as Sunburst value or announced PayX value
        if (!(zoneTo.is(ZoneType.Stack) || zoneTo.is(ZoneType.Battlefield))) {
            c.clearTemporaryVars();
        }

        if (fromBattlefield && !toBattlefield) {
            c.getController().setRevolt(true);
        }

        // Don't copy Tokens, copy only cards leaving the battlefield
        // and returning to hand (to recreate their spell ability information)
        if (suppress || (!fromBattlefield && !toHand)) {
            lastKnownInfo = c;
            copied = c;
        } else {
            // if from Battlefield to Graveyard and Card does exist in LastStateBattlefield
            // use that instead
            if (fromBattlefield && zoneTo.is(ZoneType.Graveyard)) {
                CardCollectionView lastBattlefield = game.getLastStateBattlefield();
                int idx = lastBattlefield.indexOf(c);
                if (idx != -1) {
                    lastKnownInfo = lastBattlefield.get(idx);
                }
            }

            if (lastKnownInfo == null) {
                lastKnownInfo = CardUtil.getLKICopy(c);
            }

            if (!c.isToken()) {
                if (c.isCloned()) {
                    c.switchStates(CardStateName.Original, CardStateName.Cloner, false);
                    c.setState(CardStateName.Original, false);
                    c.clearStates(CardStateName.Cloner, false);
                    if (c.isFlipCard()) {
                        c.clearStates(CardStateName.Flipped, false);
                    }
                    if (c.getStates().contains(CardStateName.OriginalText)) {
                        c.clearStates(CardStateName.OriginalText, false);
                        c.removeSVar("GainingTextFrom");
                        c.removeSVar("GainingTextFromTimestamp");
                    }
                    c.updateStateForView();
                } else if (c.getStates().contains(CardStateName.OriginalText)) {
                    // Volrath's Shapeshifter
                    CardFactory.copyState(c, CardStateName.OriginalText, c, CardStateName.Original, false);
                    c.setState(CardStateName.Original, false);
                    c.clearStates(CardStateName.OriginalText, false);
                    c.removeSVar("GainingTextFrom");
                    c.removeSVar("GainingTextFromTimestamp");
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
                for (final StaticAbility sa : copied.getStaticAbilities()) {
                    sa.setHostCard(copied);
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

        // special rule for Worms of the Earth
        if (toBattlefield && game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noLandBattlefield)) {
            // something that is already a Land cant enter the battlefield
            if (c.isLand()) {
                return c;
            }
            // check if something would be a land
            Card noLandLKI = CardUtil.getLKICopy(c);
            // this check needs to check if this card would be on the battlefield
            noLandLKI.setLastKnownZone(zoneTo);

            CardCollection preList = new CardCollection(noLandLKI);
            checkStaticAbilities(false, Sets.newHashSet(noLandLKI), preList);

            // fake etb counters thing, then if something changed,
            // need to apply checkStaticAbilities again
            if(!noLandLKI.isLand()) {
                if (noLandLKI.putEtbCounters()) {
                    // counters are added need to check again
                    checkStaticAbilities(false, Sets.newHashSet(noLandLKI), preList);
                }
            }

            if(noLandLKI.isLand()) {
                // if it isn't on the Stack, it stays in that Zone
                if (!c.getZone().is(ZoneType.Stack)) {
                    return c;
                }
                // if something would only be a land when entering the battlefield and not before
                // put it into the graveyard instead
                zoneTo = c.getOwner().getZone(ZoneType.Graveyard);
                // reset facedown
                copied.setState(CardStateName.Original, false);
                copied.setManifested(false);
                copied.updateStateForView();

                // not to battlefield anymore!
                toBattlefield = false;
            }
        }

        if (!suppress) {
            if (zoneFrom == null) {
                copied.getOwner().addInboundToken(copied);
            }

            Map<String, Object> repParams = Maps.newHashMap();
            repParams.put("Event", "Moved");
            repParams.put("Affected", copied);
            repParams.put("CardLKI", lastKnownInfo);
            repParams.put("Cause", cause);
            repParams.put("Origin", zoneFrom != null ? zoneFrom.getZoneType() : null);
            repParams.put("Destination", zoneTo.getZoneType());

            if (params != null) {
                repParams.putAll(params);
            }

            ReplacementResult repres = game.getReplacementHandler().run(repParams);
            if (repres != ReplacementResult.NotReplaced) {
                // reset failed manifested Cards back to original
                if (c.isManifested()) {
                    c.turnFaceUp(false, false);
                }

                if (game.getStack().isResolving(c) && !zoneTo.is(ZoneType.Graveyard) && repres == ReplacementResult.Prevented) {
                	copied.getOwner().removeInboundToken(copied);
                	return moveToGraveyard(c, cause, params);
                }
                copied.getOwner().removeInboundToken(copied);
                return c;
            }
        }

        copied.getOwner().removeInboundToken(copied);

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
            if (!zoneTo.is(ZoneType.Exile) && !zoneTo.is(ZoneType.Stack)) {
                c.setExiledWith(null);
            }

            // cleanup Encoding
            if (c.hasEncodedCard()) {
                for (final Card e : c.getEncodedCards()) {
                    e.setEncodingCard(null);
                }
            }
            if (zoneFrom.is(ZoneType.Exile)) {
                Card e = c.getEncodingCard();
                if (e != null) {
                    e.removeEncodedCard(c);
                }
            }
        }

        // "enter the battlefield as a copy" - apply code here
        // but how to query for input here and continue later while the callers assume synchronous result?
        zoneTo.add(copied, position, c); // the modified state of the card is also reported here (e.g. for Morbid + Awaken)
        c.setZone(zoneTo);

        if (fromBattlefield) {
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

        // do ETB counters after StaticAbilities check
        if (!suppress) {
            if (toBattlefield) {
                if (copied.putEtbCounters()) {
                    // if counter where put of card, call checkStaticAbilities again
                    checkStaticAbilities();
                }
            }
            copied.clearEtbCounters();
        }

        // play the change zone sound
        game.fireEvent(new GameEventCardChangeZone(c, zoneFrom, zoneTo));

        final Map<String, Object> runParams = Maps.newHashMap();
        runParams.put("Card", lastKnownInfo);
        runParams.put("Cause", cause);
        runParams.put("Origin", zoneFrom != null ? zoneFrom.getZoneType().name() : null);
        runParams.put("Destination", zoneTo.getZoneType().name());
        runParams.put("SpellAbilityStackInstance", game.stack.peek());
        runParams.put("IndividualCostPaymentInstance", game.costPaymentStack.peek());

        if (params != null) {
            runParams.putAll(params);
        }

        game.getTriggerHandler().runTrigger(TriggerType.ChangesZone, runParams, true);
        if (zoneFrom != null && zoneFrom.is(ZoneType.Battlefield) && !zoneFrom.getPlayer().equals(zoneTo.getPlayer())) {
            final Map<String, Object> runParams2 = Maps.newHashMap();
            runParams2.put("Card", lastKnownInfo);
            runParams2.put("OriginalController", zoneFrom.getPlayer());
            if(params != null) {
                runParams2.putAll(params);
            }
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
            copied.clearConvoked();
        }

        // rule 504.6: reveal a face-down card leaving the stack 
        if (zoneFrom != null && zoneTo != null && zoneFrom.is(ZoneType.Stack) && !zoneTo.is(ZoneType.Battlefield) && c.isFaceDown()) {
            c.setState(CardStateName.Original, true);
            reveal(new CardCollection(c), c.getOwner(), true, "Face-down card moves from the stack: ");
            c.setState(CardStateName.FaceDown, true);
        }

        if (fromBattlefield) {
            if (!c.isToken()) {
                copied.setState(CardStateName.Original, true);
            }
            // Soulbond unpairing
            if (c.isPaired()) {
                c.getPairedWith().setPairedWith(null);
                if (!c.isToken()) {
                    c.setPairedWith(null);
                }
            }
            // Spin off Melded card
            if (c.getMeldedWith() != null) {
                // Other melded card needs to go "above" or "below" if Library or Graveyard
                Card unmeld = c.getMeldedWith();
                //c.setMeldedWith(null);
                ((PlayerZoneBattlefield)zoneFrom).removeFromMelded(unmeld);
                Integer unmeldPosition = position;
                if (unmeldPosition != null && (zoneTo.is(ZoneType.Library) || zoneTo.is(ZoneType.Graveyard))) {
                    // Ask controller if it wants to be on top or bottom of other meld.
                    unmeldPosition++;
                }
                changeZone(null, zoneTo, unmeld, position, cause, params);
            }
            // Reveal if face-down
            if (c.isFaceDown()) {
            	c.setState(CardStateName.Original, true);
            	reveal(new CardCollection(c), c.getOwner(), true, "Face-down card leaves the battlefield: ");
            	c.setState(CardStateName.FaceDown, true);
            	copied.setState(CardStateName.Original, true);
            }
            unattachCardLeavingBattlefield(copied);
            // Remove all changed keywords
            copied.removeAllChangedText(game.getNextTimestamp());
            // reset activations
            for (SpellAbility ab : copied.getSpellAbilities()) {
                ab.getRestrictions().resetTurnActivations();
            }
        } else if (toBattlefield) {
            // reset timestamp in changezone effects so they have same timestamp if ETB simutaneously 
            copied.setTimestamp(game.getNextTimestamp());
            for (Player p : game.getPlayers()) {
                copied.getDamageHistory().setNotAttackedSinceLastUpkeepOf(p);
                copied.getDamageHistory().setNotBlockedSinceLastUpkeepOf(p);
                copied.getDamageHistory().setNotBeenBlockedSinceLastUpkeepOf(p);
            }
            if (zoneFrom.is(ZoneType.Graveyard)) {
                // fizzle all "damage done" triggers for cards returning to battlefield from graveyard
                game.getStack().fizzleTriggersOnStackTargeting(copied, TriggerType.DamageDone);
                game.getStack().fizzleTriggersOnStackTargeting(copied, TriggerType.DamageDoneOnce);
            }
        } else if (zoneTo.is(ZoneType.Graveyard)
        		|| zoneTo.is(ZoneType.Hand)
        		|| zoneTo.is(ZoneType.Library)
        		|| zoneTo.is(ZoneType.Exile)) {
            copied.setTimestamp(game.getNextTimestamp());
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

    public final Card moveTo(final Zone zoneTo, Card c, SpellAbility cause) {
        return moveTo(zoneTo, c, cause, null);
    }
    
    public final Card moveTo(final Zone zoneTo, Card c, SpellAbility cause, Map<String, Object> params) {
       // FThreads.assertExecutedByEdt(false); // This code must never be executed from EDT,
                                             // use FThreads.invokeInNewThread to run code in a pooled thread
        return moveTo(zoneTo, c, null, cause, params);
    }

    public final Card moveTo(final Zone zoneTo, Card c, Integer position, SpellAbility cause) {
        return moveTo(zoneTo, c, position, cause, null);
    }
    
    public final Card moveTo(final Zone zoneTo, Card c, Integer position, SpellAbility cause, Map<String, Object> params) {
        // Ideally move to should never be called without a prevZone
        // Remove card from Current Zone, if it has one
        final Zone zoneFrom = game.getZoneOf(c);
        // String prevName = prev != null ? prev.getZoneName() : "";

        // Card lastKnownInfo = c;

        c = changeZone(zoneFrom, zoneTo, c, position, cause, params);

        if (zoneFrom == null) {
            c.setCastFrom(null);
            c.setCastSA(null);
        } else if (zoneTo.is(ZoneType.Stack)) {
            c.setCastFrom(zoneFrom.getZoneType());
        } else if (!(zoneTo.is(ZoneType.Battlefield) && zoneFrom.is(ZoneType.Stack))) {
            c.setCastFrom(null);
            c.setCastSA(null);
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

        final Map<String, Object> runParams = Maps.newHashMap();
        runParams.put("Card", c);
        runParams.put("OriginalController", original);
        game.getTriggerHandler().runTrigger(TriggerType.ChangesController, runParams, false);

        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        for (Player p : game.getPlayers()) {
            ((PlayerZoneBattlefield) p.getZone(ZoneType.Battlefield)).setTriggers(true);
        }
        c.runChangeControllerCommands();
    }

    public final Card moveToStack(final Card c, SpellAbility cause) {
        return moveToStack(c, cause, null);
    }
    public final Card moveToStack(final Card c, SpellAbility cause, Map<String, Object> params) {
        final Zone stack = game.getStackZone();
        return moveTo(stack, c, cause, params);
    }

    public final Card moveToGraveyard(final Card c, SpellAbility cause) {
        return moveToGraveyard(c, cause, null);
    }
    public final Card moveToGraveyard(final Card c, SpellAbility cause, Map<String, Object> params) {
        final PlayerZone grave = c.getOwner().getZone(ZoneType.Graveyard);
        // must put card in OWNER's graveyard not controller's
        return moveTo(grave, c, cause, params);
    }

    public final Card moveToHand(final Card c, SpellAbility cause) {
        return moveToHand(c, cause, null);
    }
    
    public final Card moveToHand(final Card c, SpellAbility cause, Map<String, Object> params) {
        final PlayerZone hand = c.getOwner().getZone(ZoneType.Hand);
        return moveTo(hand, c, cause, params);
    }

    public final Card moveToPlay(final Card c, SpellAbility cause) {
        return moveToPlay(c, cause, null);
    }
    
    public final Card moveToPlay(final Card c, SpellAbility cause, Map<String, Object> params) {
        final PlayerZone play = c.getController().getZone(ZoneType.Battlefield);
        return moveTo(play, c, cause, params);
    }

    public final Card moveToPlay(final Card c, final Player p, SpellAbility cause) {
        return moveToPlay(c, p, cause, null);
    }
    
    public final Card moveToPlay(final Card c, final Player p, SpellAbility cause, Map<String, Object> params) {
        // move to a specific player's Battlefield
        final PlayerZone play = p.getZone(ZoneType.Battlefield);
        return moveTo(play, c, cause, params);
    }

    public final Card moveToBottomOfLibrary(final Card c, SpellAbility cause) {
        return moveToBottomOfLibrary(c, cause, null);
    }
    
    public final Card moveToBottomOfLibrary(final Card c, SpellAbility cause, Map<String, Object> params) {
        return moveToLibrary(c, -1, cause, params);
    }

    public final Card moveToLibrary(final Card c, SpellAbility cause) {
        return moveToLibrary(c, cause, null);
    }
    
    public final Card moveToLibrary(final Card c, SpellAbility cause, Map<String, Object> params) {
        return moveToLibrary(c, 0, cause, params);
    }

    public final Card moveToLibrary(Card c, int libPosition, SpellAbility cause) {
        return moveToLibrary(c, libPosition, cause, null);
    }
    
    public final Card moveToLibrary(Card c, int libPosition, SpellAbility cause, Map<String, Object> params) {
        final PlayerZone library = c.getOwner().getZone(ZoneType.Library);
        if (libPosition == -1 || libPosition > library.size()) {
            libPosition = library.size();
        }
        return changeZone(game.getZoneOf(c), library, c, libPosition, cause, params);
    }

    public final Card moveToVariantDeck(Card c, ZoneType zone, int deckPosition, SpellAbility cause) {
        return moveToVariantDeck(c, zone, deckPosition, cause, null);
    }
    
    public final Card moveToVariantDeck(Card c, ZoneType zone, int deckPosition, SpellAbility cause, Map<String, Object> params) {
        final PlayerZone deck = c.getOwner().getZone(zone);
        if (deckPosition == -1 || deckPosition > deck.size()) {
            deckPosition = deck.size();
        }
        return changeZone(game.getZoneOf(c), deck, c, deckPosition, cause, params);
    }

    public final Card exile(final Card c, SpellAbility cause) {
        return exile(c, cause, null);
    }
    public final Card exile(final Card c, SpellAbility cause, Map<String, Object> params) {
        if (game.isCardExiled(c)) {
            return c;
        }
        final PlayerZone removed = c.getOwner().getZone(ZoneType.Exile);
        return moveTo(removed, c, cause, params);
    }

    public final Card moveTo(final ZoneType name, final Card c, SpellAbility cause) {
        return moveTo(name, c, cause, null);
    }
    
    public final Card moveTo(final ZoneType name, final Card c, SpellAbility cause, Map<String, Object> params) {
        return moveTo(name, c, 0, cause, params);
    }
    
    public final Card moveTo(final ZoneType name, final Card c, final int libPosition, SpellAbility cause) {
        return moveTo(name, c, libPosition, cause, null);
    }

    public final Card moveTo(final ZoneType name, final Card c, final int libPosition, SpellAbility cause, Map<String, Object> params) {
        // Call specific functions to set PlayerZone, then move onto moveTo
        switch(name) {
            case Hand:          return moveToHand(c, cause, params);
            case Library:       return moveToLibrary(c, libPosition, cause, params);
            case Battlefield:   return moveToPlay(c, cause, params);
            case Graveyard:     return moveToGraveyard(c, cause, params);
            case Exile:         return exile(c, cause, params);
            case Stack:         return moveToStack(c, cause, params);
            case PlanarDeck:    return moveToVariantDeck(c, ZoneType.PlanarDeck, libPosition, cause, params);
            case SchemeDeck:    return moveToVariantDeck(c, ZoneType.SchemeDeck, libPosition, cause, params);
            default: // sideboard will also get there
                return moveTo(c.getOwner().getZone(name), c, cause, params);
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
        checkStaticAbilities(true);
    }
    public final void checkStaticAbilities(final boolean runEvents) {
        checkStaticAbilities(runEvents, Sets.<Card>newHashSet(), CardCollection.EMPTY);
    }
    public final void checkStaticAbilities(final boolean runEvents, final Set<Card> affectedCards, final CardCollectionView preList) {
        if (isCheckingStaticAbilitiesOnHold()) {
            return;
        }
        if (game.isGameOver()) {
            return;
        }
        game.getTracker().freeze(); //prevent views flickering during while updating for state-based effects

        // remove old effects
        game.getStaticEffects().clearStaticEffects(affectedCards);
        game.getTriggerHandler().cleanUpTemporaryTriggers();
        game.getReplacementHandler().cleanUpTemporaryReplacements();

        for (final Player p : game.getPlayers()) {
            if (!game.getStack().isFrozen()) {
                p.getManaPool().restoreColorReplacements();
            }
            p.clearStaticAbilities();
        }

        // search for cards with static abilities
        final FCollection<StaticAbility> staticAbilities = new FCollection<StaticAbility>();
        final CardCollection staticList = new CardCollection();

        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public boolean visit(final Card c) {
                // need to get Card from preList if able
                final Card co = preList.get(c);
                List<StaticAbility> toRemove = Lists.newArrayList();
                for (StaticAbility stAb : co.getStaticAbilities()) {
                    if (stAb.getMapParams().get("Mode").equals("Continuous")) {
                        staticAbilities.add(stAb);
                    }
                    if (stAb.isTemporary()) {
                        toRemove.add(stAb);
                    }
                 }
                 for (StaticAbility stAb : toRemove) {
                     co.removeStaticAbility(stAb);
                 }
                 if (!co.getStaticCommandList().isEmpty()) {
                     staticList.add(co);
                 }
                 return true;
            }
        });

        
        final Comparator<StaticAbility> comp = new Comparator<StaticAbility>() {
            @Override
            public int compare(final StaticAbility a, final StaticAbility b) {
                return ComparisonChain.start()
                        .compareTrueFirst(a.hasParam("CharacteristicDefining"), b.hasParam("CharacteristicDefining"))
                        .compare(a.getHostCard().getTimestamp(), b.getHostCard().getTimestamp())
                        .result();
            }
        };
        Collections.sort(staticAbilities, comp);

        final Map<StaticAbility, CardCollectionView> affectedPerAbility = Maps.newHashMap();
        for (final StaticAbilityLayer layer : StaticAbilityLayer.CONTINUOUS_LAYERS) {
            for (final StaticAbility stAb : staticAbilities) {
                final CardCollectionView previouslyAffected = affectedPerAbility.get(stAb);
                final CardCollectionView affectedHere;
                if (previouslyAffected == null) {
                    affectedHere = stAb.applyContinuousAbilityBefore(layer, preList);
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

        for (final Card c : staticList) {
            List<Object[]> toRemove = Lists.newArrayList();
            for (Object[] staticCheck : c.getStaticCommandList()) {
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
                    toRemove.add(staticCheck);
                    affectedCards.add(c);
                }
            }
            for (Object[] staticCheck : c.getStaticCommandList()) {
                c.getStaticCommandList().remove(staticCheck);
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

        // preList means that this is run by a pre Check with LKI objects
        // in that case Always trigger should not Run
        if (preList.isEmpty()) {
            final Map<String, Object> runParams = Maps.newHashMap();
            game.getTriggerHandler().runTrigger(TriggerType.Always, runParams, false);

            game.getTriggerHandler().runTrigger(TriggerType.Immediate, runParams, false);
        }

        // Update P/T and type in the view only once after all the cards have been processed, to avoid flickering
        for (Card c : affectedCards) {
            c.updatePowerToughnessForView();
            c.updateTypesForView();
            c.updateAbilityTextForView(); // only update keywords and text for view to avoid flickering
        }

        if (runEvents && !affectedCards.isEmpty()) {
            game.fireEvent(new GameEventCardStatsChanged(affectedCards));
        }
        game.getTracker().unfreeze();
    }

    public final void checkStateEffects(final boolean runEvents) {
        checkStateEffects(runEvents, Sets.<Card>newHashSet());
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
        boolean orderedDesCreats = false;
        boolean orderedNoRegCreats = false;
        for (int q = 0; q < 9; q++) {
            checkStaticAbilities(false, affectedCards, CardCollection.EMPTY);
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
            CardCollection noRegCreats = null;
            CardCollection desCreats = null;
            for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
                if (c.isCreature()) {
                    // Rule 704.5f - Put into grave (no regeneration) for toughness <= 0
                    if (c.getNetToughness() <= 0) {
                        if (noRegCreats == null) {
                            noRegCreats = new CardCollection();
                        }
                        noRegCreats.add(c);
                        checkAgain = true;
                    } else if (c.hasKeyword("CARDNAME can't be destroyed by lethal damage unless lethal damage dealt by a single source is marked on it.")) {
                        for (final Integer dmg : c.getReceivedDamageFromThisTurn().values()) {
                            if (c.getNetToughness() <= dmg.intValue()) {
                                if (desCreats == null) {
                                    desCreats = new CardCollection();
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
                            desCreats = new CardCollection();
                        }
                        desCreats.add(c);
                        c.setHasBeenDealtDeathtouchDamage(false);
                        checkAgain = true;
                    }
                }

                checkAgain |= stateBasedAction_Saga(c);
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
                if (noRegCreats.size() > 1 && !orderedNoRegCreats) {
                    noRegCreats = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, noRegCreats, ZoneType.Graveyard);
                    orderedNoRegCreats = true;
                }
                for (Card c : noRegCreats) {
                    sacrificeDestroy(c, null);
                }
            }
            if (desCreats != null) {
                if (desCreats.size() > 1 && !orderedDesCreats) {
                    desCreats = CardLists.filter(desCreats, new Predicate<Card>() {
                        @Override
                        public boolean apply(Card card) {
                            return card.canBeDestroyed();
                        }
                    });
                    if (!desCreats.isEmpty()) {
                        desCreats = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, desCreats, ZoneType.Graveyard);
                    }
                    orderedDesCreats = true;
                }
                for (Card c : desCreats) {
                    destroy(c, null);
                }
            }
            setHoldCheckingStaticAbilities(false);

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
        checkStaticAbilities(false, affectedCards, CardCollection.EMPTY);

        if (!refreeze) {
            game.getStack().unfreezeStack();
        }
    }

    private boolean stateBasedAction_Saga(Card c) {
        boolean checkAgain = false;
        if (!c.getType().hasSubtype("Saga")) {
            return false;
        }
        if (!c.canBeSacrificed()) {
            return false;
        }
        if (c.getCounters(CounterType.LORE) < c.getFinalChapterNr()) {
            return false;
        }
        if (!game.getStack().hasSimultaneousStackEntries() &&
                !game.getStack().hasSourceOnStack(c, SpellAbilityPredicates.isChapter())) {
            sacrifice(c, null);
            checkAgain = true;
        }
        return checkAgain;
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
                moveToGraveyard(c, null, null);
                checkAgain = true;
            }
        } else if (entity instanceof Player) {
            final Player pl = (Player) entity;
            boolean invalid = false;

            if (!game.getPlayers().contains(pl)) {
                // lost player can't have Aura on it
                invalid = true;
            } else if (tgt.canOnlyTgtOpponent() && !c.getController().isOpponentOf(pl)) {
                invalid = true;
            }
            else if (pl.hasProtectionFrom(c)) {
                invalid = true;
            }
            if (invalid) {
                c.unEnchantEntity(pl);
                moveToGraveyard(c, null, null);
                checkAgain = true;
            }
        }

        if (c.isInPlay() && !c.isEnchanting()) {
            moveToGraveyard(c, null, null);
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
                    || (fortifiedLand.isPhasedOut() && !c.isPhasedOut())
                    || !c.isFortification()) {
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
                    losers = Lists.newArrayListWithCapacity(3);
                }
                losers.add(p);
            }
        }

        GameEndReason reason = null;
        // Has anyone won by spelleffect?
        for (Player p : allPlayers) {
            if (!p.hasWon()) {
                continue;
            }

            // then the rest have lost!
            reason = GameEndReason.WinsGameSpellEffect;
            for (Player pl : allPlayers) {
                if (pl.equals(p)) {
                    continue;
                }

                if (!pl.loseConditionMet(GameLossReason.OpponentWon, p.getOutcome().altWinSourceName)) {
                    reason = null; // they cannot lose!
                } else {
                    if (losers == null) {
                        losers = Lists.newArrayListWithCapacity(3);
                    }
                    losers.add(pl);
                }
            }
            break;
        }

        // loop through all the non-losing players that can't win
        // see if all of their opponents are in that "about to lose" collection
        if (losers != null) {
            for (Player p : allPlayers) {
                if (losers.contains(p)) {
                    continue;
                }
                if (p.cantWin()) {
                    if (losers.containsAll(p.getOpponents())) {
                        // what to do here?!?!?!
                        System.err.println(p.toString() + " is about to win, but can't!");
                    }
                }

            }
        }

        // need a separate loop here, otherwise ConcurrentModificationException is raised
        if (losers != null) {
            for (Player p : losers) {
                game.onPlayerLost(p);
            }
        }

        if (reason == null) {
        	List<Player> notLost = Lists.newArrayList();
        	Set<Integer> teams = Sets.newHashSet();
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
        final List<Card> list = CardLists.filter(p.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.PLANESWALKERS);
        boolean recheck = false;

        //final Multimap<String, Card> uniqueWalkers = ArrayListMultimap.create(); // Not used as of Ixalan

        for (Card c : list) {
            if (c.getCounters(CounterType.LOYALTY) <= 0) {
                moveToGraveyard(c, null, null);
                // Play the Destroy sound
                game.fireEvent(new GameEventCardDestroyed());
                recheck = true;
            }

            /* -- Not used as of Ixalan --
            for (final String type : c.getType()) {
                if (CardType.isAPlaneswalkerType(type)) {
                    uniqueWalkers.put(type, c);
                }
            }*/
        }

        /* -- Not used as of Ixalan --
        for (String key : uniqueWalkers.keySet()) {
            Collection<Card> duplicates = uniqueWalkers.get(key);
            if (duplicates.size() < 2) {
                continue;
            }

            recheck = true;

            Card toKeep = p.getController().chooseSingleEntityForEffect(new CardCollection(duplicates), new AbilitySub(ApiType.InternalLegendaryRule, null, null, null), "You have multiple planeswalkers of type \""+key+"\"in play.\n\nChoose one to stay on battlefield (the rest will be moved to graveyard)");
            for (Card c: duplicates) {
                if (c != toKeep) {
                    moveToGraveyard(c, null);
                }
            }
        }
        */
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
                    sacrificeDestroy(c, null);
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

        List<Card> toKeep = Lists.newArrayList();
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
            sacrificeDestroy(c, null);
            game.fireEvent(new GameEventCardDestroyed());
        }

        return true;
    }

    public final Card sacrifice(final Card c, final SpellAbility source) {
        if (!c.canBeSacrificedBy(source)) {
            return null;
        }

        c.getController().addSacrificedThisTurn(c, source);

        return sacrificeDestroy(c, source);
    }

    public final boolean destroy(final Card c, final SpellAbility sa) {
        if (!c.canBeDestroyed()) {
            return false;
        }

        return destroy(c, sa, true);
    }
    public final boolean destroyNoRegeneration(final Card c, final SpellAbility sa) {
        return destroy(c, sa, false);
    }

    public final boolean destroy(final Card c, final SpellAbility sa, final boolean regenerate) {
        Player activator = null;
        if (!c.canBeDestroyed()) {
            return false;
        }

        // Replacement effects
        final Map<String, Object> repRunParams = Maps.newHashMap();
        repRunParams.put("Event", "Destroy");
        repRunParams.put("Source", sa);
        repRunParams.put("Card", c);
        repRunParams.put("Affected", c);
        repRunParams.put("Regeneration", regenerate);

        if (game.getReplacementHandler().run(repRunParams) != ReplacementResult.NotReplaced) {
            return false;
        }


        if (sa != null) {
            activator = sa.getActivatingPlayer();
        }

        // Play the Destroy sound
        game.fireEvent(new GameEventCardDestroyed());

        // Run triggers
        final Map<String, Object> runParams = Maps.newHashMap();
        runParams.put("Card", c);
        runParams.put("Causer", activator);
        game.getTriggerHandler().runTrigger(TriggerType.Destroyed, runParams, false);

        final Card sacrificed = sacrificeDestroy(c, sa);
        return sacrificed != null;
    }

    /**
     * @return the sacrificed Card in its new location, or {@code null} if the
     * sacrifice wasn't successful.
     */
    public final Card sacrificeDestroy(final Card c, SpellAbility cause) {
        if (!c.isInPlay()) {
            return null;
        }

        final Card newCard = moveToGraveyard(c, cause, null);

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

    private void drawStartingHand(Player p1){

        //check initial hand
        List<Card> lib1 = Lists.newArrayList(p1.getZone(ZoneType.Library).getCards().threadSafeIterable());
        List<Card> hand1 = lib1.subList(0,p1.getMaxHandSize());
        System.out.println(hand1.toString());

        //shuffle
        List<Card> shuffledCards = Lists.newArrayList(p1.getZone(ZoneType.Library).getCards().threadSafeIterable());
        Collections.shuffle(shuffledCards);

        //check a second hand
        List<Card> hand2 = shuffledCards.subList(0,p1.getMaxHandSize());
        System.out.println(hand2.toString());

        //choose better hand according to land count
        float averageLandRatio = getLandRatio(lib1);
        if(getHandScore(hand1, averageLandRatio)>getHandScore(hand2, averageLandRatio)){
            p1.getZone(ZoneType.Library).setCards(shuffledCards);
        }
        p1.drawCards(p1.getMaxHandSize());
    }

    private float getLandRatio(List<Card> deck){
        int landCount = 0;
        for(Card c:deck){
            if(c.isLand()){
                landCount++;
            }
        }
        if (landCount == 0 ){
            return 0;
        }
        return Float.valueOf(landCount)/Float.valueOf(deck.size());
    }

    private float getHandScore(List<Card> hand, float landRatio){
        int landCount = 0;
        for(Card c:hand){
            if(c.isLand()){
                landCount++;
            }
        }
        float averageCount = landRatio * hand.size();
        return Math.abs(averageCount-landCount);
    }

    public void startGame(GameOutcome lastGameOutcome) {
        startGame(lastGameOutcome, null);
    }

    public void startGame(GameOutcome lastGameOutcome, Runnable startGameHook) {
        Player first = determineFirstTurnPlayer(lastGameOutcome);

        GameType gameType = game.getRules().getGameType();
        do {
            if (game.isGameOver()) { break; } // conceded during "play or draw"

            // FControl should determine now if there are any human players.
            // Where there are none, it should bring up speed controls
            game.fireEvent(new GameEventGameStarted(gameType, first, game.getPlayers()));

            // Emissary's Plot
            // runPreOpeningHandActions(first);

            game.setAge(GameStage.Mulligan);
            for (final Player p1 : game.getPlayers()) {
                if (StaticData.instance().getFilteredHandsEnabled() ) {
                    drawStartingHand(p1);
                } else {
                    p1.drawCards(p1.getStartingHandSize());
                }

                // If pl has Backup Plan as a Conspiracy draw that many extra hands

            }

            // Choose starting hand for each player with multiple hands
            if (game.getRules().getGameType() != GameType.Puzzle) {
                performMulligans(first, game.getRules().hasAppliedVariant(GameType.Commander));
            }
            if (game.isGameOver()) { break; } // conceded during "mulligan" prompt

            game.setAge(GameStage.Play);

            //<THIS CODE WILL WORK WITH PHASE = NULL>
            if (game.getRules().hasAppliedVariant(GameType.Planechase)) {
                first.initPlane();
            }

            runOpeningHandActions(first);
            checkStateEffects(true); // why?

            // Run Trigger beginning of the game
            final Map<String, Object> runParams = Maps.newHashMap();
            game.getTriggerHandler().runTrigger(TriggerType.NewGame, runParams, true);
            //</THIS CODE WILL WORK WITH PHASE = NULL>


            game.getPhaseHandler().startFirstTurn(first, startGameHook);
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

        if (game != null) {
            if (game.getRules().getGameType().equals(GameType.Puzzle)) {
                return game.getPlayers().get(0);
            }

            // 904.6: in Archenemy games the Archenemy goes first
            if (game.getRules().hasAppliedVariant(GameType.Archenemy)) {
                for (Player p : game.getPlayers()) {
                    if (p.isArchenemy()) {
                        return p;
                    }
                }
            }
        }
        // Power Play - Each player with a Power Play in the CommandZone becomes the Starting Player
        Set<Player> powerPlayers = Sets.newHashSet();
        for (Card c : game.getCardsIn(ZoneType.Command)) {
            if (c.getName().equals("Power Play")) {
                powerPlayers.add(c.getOwner());
            }
        }

        if (!powerPlayers.isEmpty()) {
            List<Player> players = Lists.newArrayList(powerPlayers);
            Collections.shuffle(players, MyRandom.getRandom());
            return players.get(0);
        }

        boolean isFirstGame = lastGameOutcome == null;
        if (isFirstGame) {
            game.fireEvent(new GameEventFlipCoin()); // Play the Flip Coin sound
            goesFirst = Aggregates.random(game.getPlayers());
        } else {
            for (Player p : game.getPlayers()) {
                if (!lastGameOutcome.isWinner(p.getRegisteredPlayer())) {
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

        // https://magic.wizards.com/en/articles/archive/feature/checking-brawl-2018-07-09
        if (game.getRules().hasAppliedVariant(GameType.Brawl) && !isMultiPlayer){
            mulliganDelta = 0;
        }

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
                            moveToLibrary(c, null, null);
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
                            exile(c, null, null);
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
                    moveToLibrary(c, null, null);
                }
                p.shuffle(null);
            }
        }

        //Vancouver Mulligan
        for(Player p : whoCanMulligan) {
            if (p.getStartingHandSize() > p.getZone(ZoneType.Hand).size()) {
                p.scry(1, null);
            }
        }
    }

    private void runPreOpeningHandActions(final Player first) {
        Player takesAction = first;
        do {
            //
            List<Card> ploys = CardLists.filter(takesAction.getCardsIn(ZoneType.Command), new Predicate<Card>() {
                @Override
                public boolean apply(Card input) {
                    return input.getName().equals("Emissary's Ploy");
                }
            });

            int chosen = 1;
            List<Integer> cmc = Lists.newArrayList(1, 2, 3);

            for (Card c : ploys) {
                if (!cmc.isEmpty()) {
                    chosen = takesAction.getController().chooseNumber(c.getSpellPermanent(), "Emissary's Ploy", cmc, c.getOwner());
                    cmc.remove((Object)chosen);
                }

                c.setChosenNumber(chosen);
            }
            takesAction = game.getNextPlayerAfter(takesAction);
        } while (takesAction != first);
    }

    private void runOpeningHandActions(final Player first) {
        Player takesAction = first;
        do {
            List<SpellAbility> usableFromOpeningHand = Lists.newArrayList();

            // Select what can be activated from a given hand
            for (final Card c : takesAction.getCardsIn(ZoneType.Hand)) {
                for (KeywordInterface inst : c.getKeywords()) {
                    String kw = inst.getOriginal();
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

    public void becomeMonarch(final Player p) {
        final Player previous = game.getMonarch();
        if (p == null || p.equals(previous))
            return;

        if (previous != null)
            previous.removeMonarchEffect();

        p.createMonarchEffect();
        game.setMonarch(p);

        // Run triggers
        final Map<String, Object> runParams = Maps.newHashMap();
        runParams.put("Player", p);
        game.getTriggerHandler().runTrigger(TriggerType.BecomeMonarch, runParams, false);
    }
}
