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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import forge.GameCommand;
import forge.StaticData;
import forge.card.CardStateName;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.AttachEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.event.GameEventCardChangeZone;
import forge.game.event.GameEventCardDestroyed;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.event.GameEventCardTapped;
import forge.game.event.GameEventFlipCoin;
import forge.game.event.GameEventGameStarted;
import forge.game.event.GameEventScry;
import forge.game.keyword.KeywordInterface;
import forge.game.mulligan.MulliganService;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityPredicates;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityLayer;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Aggregates;
import forge.util.Expressions;
import forge.util.MyRandom;
import forge.util.ThreadUtil;
import forge.util.Visitor;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

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
        // Reset Activations per Turn
        for (final Card card : game.getCardsInGame()) {
            card.resetActivationsPerTurn();
            // need to reset this in exile
            card.resetForetoldThisTurn();
        }
    }

    public Card changeZone(final Zone zoneFrom, Zone zoneTo, final Card c, Integer position, SpellAbility cause) {
        return changeZone(zoneFrom, zoneTo, c, position, cause, null);
    }

    private Card changeZone(final Zone zoneFrom, Zone zoneTo, final Card c, Integer position, SpellAbility cause, Map<AbilityKey, Object> params) {
        // 111.11. A copy of a permanent spell becomes a token as it resolves.
        // The token has the characteristics of the spell that became that token.
        // The token is not “created” for the purposes of any replacement effects or triggered abilities that refer to creating a token.
        if (c.isCopiedSpell() && zoneTo.is(ZoneType.Battlefield) && c.isPermanent() && cause != null && cause.isSpell() && c.equals(cause.getHostCard())) {
            c.setCopiedSpell(false);
            c.setToken(true);
        }

        if (c.isCopiedSpell() || (c.isImmutable() && zoneTo.is(ZoneType.Exile))) {
            // Remove Effect from command immediately, this is essential when some replacement
            // effects happen during the resolving of a spellability ("the next time ..." effect)
            if (zoneFrom != null) {
                zoneFrom.remove(c);
            }
            return c;
        }
        if (zoneFrom == null && !c.isToken()) {
            zoneTo.add(c, position, CardUtil.getLKICopy(c));
            checkStaticAbilities();
            game.getTriggerHandler().registerActiveTrigger(c, true);
            game.fireEvent(new GameEventCardChangeZone(c, zoneFrom, zoneTo));
            return c;
        }

        boolean toBattlefield = zoneTo.is(ZoneType.Battlefield) || zoneTo.is(ZoneType.Merged);
        boolean fromBattlefield = zoneFrom != null && zoneFrom.is(ZoneType.Battlefield);
        boolean wasFacedown = c.isFaceDown();

        //Rule 110.5g: A token that has left the battlefield can't move to another zone
        if (c.isToken() && zoneFrom != null && !fromBattlefield && !zoneFrom.is(ZoneType.Stack)) {
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
        Card commanderEffect = null; // The effect card of commander replacement effect

        // get the LKI from above like ChangeZoneEffect
        if (params != null && params.containsKey(AbilityKey.CardLKI)) {
            lastKnownInfo = (Card) params.get(AbilityKey.CardLKI);
        } else if (toBattlefield && cause != null && cause.isReplacementAbility()) {
            // if to Battlefield and it is caused by an replacement effect,
            // try to get previous LKI if able
            ReplacementEffect re = cause.getReplacementEffect();
            if (ReplacementType.Moved.equals(re.getMode())) {
                lastKnownInfo = (Card) cause.getReplacingObject(AbilityKey.CardLKI);
            }
        }

        if (c.isSplitCard()) {
            boolean resetToOriginal = false;

            if (c.isManifested()) {
                if (fromBattlefield) {
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

        // Clean up the temporary Dash SVar when the Dashed card leaves the battlefield
        // Clean up the temporary AtEOT SVar
        String endofTurn = c.getSVar("EndOfTurnLeavePlay");
        if (fromBattlefield && (endofTurn.equals("Dash") || endofTurn.equals("AtEOT"))) {
            c.removeSVar("EndOfTurnLeavePlay");
        }

        if (fromBattlefield && !toBattlefield) {
            c.getController().setRevolt(true);
        }

        // Don't copy Tokens, copy only cards leaving the battlefield
        // and returning to hand (to recreate their spell ability information)
        if (suppress || toBattlefield) {
            copied = c;

            if (lastKnownInfo == null) {
                lastKnownInfo = CardUtil.getLKICopy(c);
            }

            if (!lastKnownInfo.hasKeyword("Counters remain on CARDNAME as it moves to any zone other than a player's hand or library.")) {
                copied.clearCounters();
            }
        } else {
            // if from Battlefield to Graveyard and Card does exist in LastStateBattlefield
            // use that instead
            if (fromBattlefield) {
                CardCollectionView lastBattlefield = game.getLastStateBattlefield();
                int idx = lastBattlefield.indexOf(c);
                if (idx != -1) {
                    lastKnownInfo = lastBattlefield.get(idx);
                }
            }

            if (lastKnownInfo == null) {
                lastKnownInfo = CardUtil.getLKICopy(c);
            }

            if (!c.isRealToken()) {
                copied = CardFactory.copyCard(c, false);

                if (zoneTo.is(ZoneType.Stack)) {
                    // when moving to stack, copy changed card infomation
                    copied.setChangedCardColors(c.getChangedCardColors());
                    copied.setChangedCardKeywords(c.getChangedCardKeywords());
                    copied.setChangedCardTypes(c.getChangedCardTypesMap());
                    copied.setChangedCardNames(c.getChangedCardNames());
                    copied.setChangedCardTraits(c.getChangedCardTraits());

                    copied.copyChangedTextFrom(c);

                    // copy exiled properties when adding to stack
                    // will be cleanup later in MagicStack
                    copied.setExiledWith(c.getExiledWith());
                    copied.setExiledBy(c.getExiledBy());

                    // copy bestow timestamp
                    copied.setBestowTimestamp(c.getBestowTimestamp());
                } else {
                    // when a card leaves the battlefield, ensure it's in its original state
                    // (we need to do this on the object before copying it, or it won't work correctly e.g.
                    // on Transformed objects)
                    copied.setState(CardStateName.Original, false);
                    copied.setBackSide(false);
                }

                copied.setUnearthed(c.isUnearthed());
                copied.setTapped(false);

                // need to copy counters when card enters another zone than hand or library
                if (lastKnownInfo.hasKeyword("Counters remain on CARDNAME as it moves to any zone other than a player's hand or library.") &&
                        !(zoneTo.is(ZoneType.Hand) || zoneTo.is(ZoneType.Library))) {
                    copied.setCounters(Maps.newHashMap(lastKnownInfo.getCounters()));
                }
            } else { //Token
                copied = c;
            }
        }

        // ensure that any leftover keyword/type changes are cleared in the state view
        copied.updateStateForView();

        if (!suppress) {
            // Temporary disable commander replacement effect
            // 903.9a
            if (fromBattlefield && !toBattlefield && c.isCommander() && c.hasMergedCard()) {
                // Find the commander replacement effect "card"
                CardCollectionView comCards = c.getOwner().getCardsIn(ZoneType.Command);
                for (final Card effCard : comCards) {
                    for (final ReplacementEffect re : effCard.getReplacementEffects()) {
                        if (re.hasSVar("CommanderMoveReplacement") && effCard.getEffectSource().getName().equals(c.getRealCommander().getName())) {
                            commanderEffect = effCard;
                            break;
                        }
                    }
                    if (commanderEffect != null) break;
                }
                // Disable the commander replacement effect
                for (final ReplacementEffect re : commanderEffect.getReplacementEffects()) {
                    re.setSuppressed(true);
                }
            }

            if (zoneFrom == null) {
                copied.getOwner().addInboundToken(copied);
            }

            Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(copied);
            repParams.put(AbilityKey.CardLKI, lastKnownInfo);
            repParams.put(AbilityKey.Cause, cause);
            repParams.put(AbilityKey.Origin, zoneFrom != null ? zoneFrom.getZoneType() : null);
            repParams.put(AbilityKey.Destination, zoneTo.getZoneType());

            if (params != null) {
                repParams.putAll(params);
            }

            ReplacementResult repres = game.getReplacementHandler().run(ReplacementType.Moved, repParams);
            if (repres != ReplacementResult.NotReplaced) {
                // reset failed manifested Cards back to original
                if (c.isManifested() && !c.isInZone(ZoneType.Battlefield)) {
                    c.forceTurnFaceUp();
                }

                copied.getOwner().removeInboundToken(copied);

                if (repres == ReplacementResult.Prevented) {
                    if (game.getStack().isResolving(c) && !zoneTo.is(ZoneType.Graveyard)) {
                        return moveToGraveyard(c, cause, params);
                    }

                    copied.clearDevoured();
                    copied.clearDelved();
                    copied.clearConvoked();
                    copied.clearExploited();
                }

                // was replaced with another Zone Change
                if (toBattlefield && !c.isInZone(ZoneType.Battlefield)) {
                    if (c.removeChangedState()) {
                        c.updateStateForView();
                    }
                }
                return c;
            }
        }

        copied.getOwner().removeInboundToken(copied);

        // Handle merged permanent here so all replacement effects are already applied.
        CardCollection mergedCards = null;
        if (fromBattlefield && !toBattlefield && c.hasMergedCard()) {
            CardCollection cards = new CardCollection(c.getMergedCards());
            // replace top card with copied card for correct name for human to choose.
            cards.set(cards.indexOf(c), copied);
            // 721.3b
            if (cause != null && zoneTo.getZoneType() == ZoneType.Exile) {
                cards = (CardCollection) cause.getHostCard().getController().getController().orderMoveToZoneList(cards, zoneTo.getZoneType(), cause);
            } else {
                cards = (CardCollection) c.getOwner().getController().orderMoveToZoneList(cards, zoneTo.getZoneType(), cause);
            }
            cards.set(cards.indexOf(copied), c);
            if (zoneTo.is(ZoneType.Library)) {
                java.util.Collections.reverse(cards);
            }
            mergedCards = cards;
            if (cause != null) {
                // Replace sa targeting cards
                final SpellAbility saTargeting = cause.getSATargetingCard();
                if (saTargeting != null) {
                    saTargeting.getTargets().replaceTargetCard(c, cards);
                }
                // Replace host rememberd cards
                // But not replace RememberLKI, since it wants to refer to the last known info.
                Card hostCard = cause.getHostCard();
                if (!cause.hasParam("RememberLKI") && hostCard.isRemembered(c)) {
                    hostCard.removeRemembered(c);
                    hostCard.addRemembered(cards);
                }
            }
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
            if (mergedCards != null) {
                for (final Card card : mergedCards) {
                    c.getOwner().getZone(ZoneType.Merged).remove(card);
                }
            }
            zoneFrom.remove(c);

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

            if (!zoneTo.is(ZoneType.Exile) && !zoneTo.is(ZoneType.Stack)) {
                c.cleanupExiledWith();
            }
        }

        // if an adventureCard is put from Stack somewhere else, need to reset to Original State
        if (copied.isAdventureCard() && ((zoneFrom != null && zoneFrom.is(ZoneType.Stack)) || !zoneTo.is(ZoneType.Stack))) {
            copied.setState(CardStateName.Original, false);
        }

        GameEntityCounterTable table = new GameEntityCounterTable();

        // need to suspend cards own replacement effects
        if (!suppress) {
            if (toBattlefield && !copied.getEtbCounters().isEmpty()) {
                for (final ReplacementEffect re : copied.getReplacementEffects()) {
                    re.setSuppressed(true);
                }
            }
        }

        if (mergedCards != null) {
            // Move components of merged permanet here
            // Also handle 721.3e and 903.9a
            boolean wasToken = c.isToken();
            if (commanderEffect != null) {
                for (final ReplacementEffect re : commanderEffect.getReplacementEffects()) {
                    re.setSuppressed(false);
                }
            }
            // Change zone of original card so components isToken() and isCommander() return correct value
            // when running replacement effects here
            c.setZone(zoneTo);
            for (final Card card : mergedCards) {
                if (card.isRealCommander()) {
                    card.setMoveToCommandZone(true);
                }
                // 721.3e & 903.9a
                if (wasToken && !card.isRealToken() || card.isRealCommander()) {
                    Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(card);
                    repParams.put(AbilityKey.CardLKI, card);
                    repParams.put(AbilityKey.Cause, cause);
                    repParams.put(AbilityKey.Origin, zoneFrom != null ? zoneFrom.getZoneType() : null);
                    repParams.put(AbilityKey.Destination, zoneTo.getZoneType());

                    if (params != null) {
                        repParams.putAll(params);
                    }

                    ReplacementResult repres = game.getReplacementHandler().run(ReplacementType.Moved, repParams);
                    if (repres != ReplacementResult.NotReplaced) continue;
                }
                if (card == c) {
                    zoneTo.add(copied, position, lastKnownInfo); // the modified state of the card is also reported here (e.g. for Morbid + Awaken)
                } else {
                    zoneTo.add(card, position, CardUtil.getLKICopy(card));
                }
                card.setZone(zoneTo);
            }
        } else {
            // "enter the battlefield as a copy" - apply code here
            // but how to query for input here and continue later while the callers assume synchronous result?
            zoneTo.add(copied, position, lastKnownInfo); // the modified state of the card is also reported here (e.g. for Morbid + Awaken)
            c.setZone(zoneTo);
        }

        // do ETB counters after zone add
        if (!suppress) {
            if (toBattlefield) {
                copied.putEtbCounters(table);
                // enable replacement effects again
                for (final ReplacementEffect re : copied.getReplacementEffects()) {
                    re.setSuppressed(false);
                }
            }
            copied.clearEtbCounters();
        }

        // update state for view
        copied.updateStateForView();

        if (fromBattlefield) {
            copied.setDamage(0); //clear damage after a card leaves the battlefield
            copied.setHasBeenDealtDeathtouchDamage(false);
            if (copied.isTapped()) {
                copied.setTapped(false); //untap card after it leaves the battlefield if needed
                game.fireEvent(new GameEventCardTapped(c, false));
            }
            copied.setMustAttackEntity(null);
        }

        // Need to apply any static effects to produce correct triggers
        checkStaticAbilities();
        game.getTriggerHandler().clearActiveTriggers(copied, null);
        game.getTriggerHandler().registerActiveLTBTrigger(lastKnownInfo);
        game.getTriggerHandler().registerActiveTrigger(copied, false);

        table.triggerCountersPutAll(game);

        // play the change zone sound
        game.fireEvent(new GameEventCardChangeZone(c, zoneFrom, zoneTo));

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(copied);
        runParams.put(AbilityKey.CardLKI, lastKnownInfo);
        runParams.put(AbilityKey.Cause, cause);
        runParams.put(AbilityKey.Origin, zoneFrom != null ? zoneFrom.getZoneType().name() : null);
        runParams.put(AbilityKey.Destination, zoneTo.getZoneType().name());
        runParams.put(AbilityKey.SpellAbilityStackInstance, game.stack.peek());
        runParams.put(AbilityKey.IndividualCostPaymentInstance, game.costPaymentStack.peek());
        runParams.put(AbilityKey.MergedCards, mergedCards);

        if (params != null) {
            runParams.putAll(params);
        }

        game.getTriggerHandler().runTrigger(TriggerType.ChangesZone, runParams, true);
        if (fromBattlefield && !zoneFrom.getPlayer().equals(zoneTo.getPlayer())) {
            final Map<AbilityKey, Object> runParams2 = AbilityKey.mapFromCard(lastKnownInfo);
            runParams2.put(AbilityKey.OriginalController, zoneFrom.getPlayer());
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

        if (!c.isRealToken() && !toBattlefield) {
            copied.clearDevoured();
            copied.clearDelved();
            copied.clearConvoked();
            copied.clearExploited();
        }

        // rule 504.6: reveal a face-down card leaving the stack
        if (zoneFrom != null && zoneTo != null && zoneFrom.is(ZoneType.Stack) && !zoneTo.is(ZoneType.Battlefield) && wasFacedown) {
            Card revealLKI = CardUtil.getLKICopy(c);
            revealLKI.forceTurnFaceUp();
            reveal(new CardCollection(revealLKI), revealLKI.getOwner(), true, "Face-down card moves from the stack: ");
        }

        if (fromBattlefield) {
            if (!c.isRealToken()) {
                copied.setState(CardStateName.Original, true);
            }
            // Soulbond unpairing
            if (c.isPaired()) {
                c.getPairedWith().setPairedWith(null);
                if (!c.isRealToken()) {
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
            if (wasFacedown) {
                Card revealLKI = CardUtil.getLKICopy(c);
                revealLKI.forceTurnFaceUp();

                reveal(new CardCollection(revealLKI), revealLKI.getOwner(), true, "Face-down card leaves the battlefield: ");

                copied.setState(CardStateName.Original, true);
            }
            unattachCardLeavingBattlefield(copied);
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

        // Cards not on the battlefield / stack should not have controller
        if (!zoneTo.is(ZoneType.Battlefield) && !zoneTo.is(ZoneType.Stack)) {
            copied.clearControllers();
        }

        return copied;
    }

    private static void unattachCardLeavingBattlefield(final Card copied) {
        // remove attachments from creatures
        copied.unAttachAllCards();

        // unenchant creature if moving aura
        if (copied.isAttachedToEntity()) {
            copied.unattachFromEntity(copied.getEntityAttachedTo());
        }
    }

    public final Card moveTo(final Zone zoneTo, Card c, SpellAbility cause) {
        return moveTo(zoneTo, c, cause, null);
    }

    public final Card moveTo(final Zone zoneTo, Card c, Integer position, SpellAbility cause) {
        return moveTo(zoneTo, c, position, cause, null);
    }

    public final Card moveTo(final Zone zoneTo, Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        // FThreads.assertExecutedByEdt(false); // This code must never be executed from EDT,
        // use FThreads.invokeInNewThread to run code in a pooled thread
        return moveTo(zoneTo, c, null, cause, params);
    }

    private Card moveTo(final Zone zoneTo, Card c, Integer position, SpellAbility cause, Map<AbilityKey, Object> params) {
        // Ideally move to should never be called without a prevZone
        // Remove card from Current Zone, if it has one
        final Zone zoneFrom = game.getZoneOf(c);
        // String prevName = prev != null ? prev.getZoneName() : "";

        // Card lastKnownInfo = c;

        // Handle the case that one component of a merged permanent got take to the subgame
        if (zoneTo.is(ZoneType.Subgame) && (c.hasMergedCard() || c.isMerged())) {
            c.moveMergedToSubgame(cause);
        }

        c = changeZone(zoneFrom, zoneTo, c, position, cause, params);

        // Move card in maingame if take card from subgame
        // 720.4a
        if (zoneFrom != null && zoneFrom.is(ZoneType.Sideboard) && game.getMaingame() != null) {
            Card maingameCard = c.getOwner().getMappingMaingameCard(c);
            if (maingameCard != null) {
                if (maingameCard.getZone().is(ZoneType.Stack)) {
                    game.getMaingame().getStack().remove(maingameCard);
                }
                game.getMaingame().getAction().moveTo(ZoneType.Subgame, maingameCard, null);
            }
        }

        if (zoneFrom == null) {
            c.setCastFrom(null);
            c.setCastSA(null);
        } else if (zoneTo.is(ZoneType.Stack)) {
            c.setCastFrom(zoneFrom.getZoneType());
            if (cause != null && cause.isSpell() && c.equals(cause.getHostCard()) && !c.isCopiedSpell()) {
                c.setCastSA(cause);
            } else {
                c.setCastSA(null);
            }
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

        if (c.isRealCommander()) {
            c.setMoveToCommandZone(true);
        }

        return c;
    }

    public final void controllerChangeZoneCorrection(final Card c) {
        System.out.println("Correcting zone for " + c.toString());
        final Zone oldBattlefield = game.getZoneOf(c);

        if (oldBattlefield == null || oldBattlefield.is(ZoneType.Stack)) {
            return;
        }

        final Player original = oldBattlefield.getPlayer();
        final Player controller = c.getController();
        if (original == null || controller == null || original.equals(controller)) {
            return;
        }
        final PlayerZone newBattlefield = controller.getZone(oldBattlefield.getZoneType());

        if (newBattlefield == null || oldBattlefield.equals(newBattlefield)) {
            return;
        }

        // 702.94e A paired creature becomes unpaired if any of the following occur:
        // another player gains control of it or the creature it’s paired with
        if (c.isPaired()) {
            Card partner = c.getPairedWith();
            c.setPairedWith(null);
            partner.setPairedWith(null);
            partner.updateStateForView();
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

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
        runParams.put(AbilityKey.OriginalController, original);
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

    public final Card moveToStack(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        Card result = moveTo(game.getStackZone(), c, cause, params);
        if (cause != null && cause.isSpell() && result.equals(cause.getHostCard())) {
            result.setSplitStateToPlayAbility(cause);
        }
        return result;
    }

    public final Card moveToGraveyard(final Card c, SpellAbility cause) {
        return moveToGraveyard(c, cause, null);
    }
    public final Card moveToGraveyard(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        final PlayerZone grave = c.getOwner().getZone(ZoneType.Graveyard);
        // must put card in OWNER's graveyard not controller's
        return moveTo(grave, c, cause, params);
    }

    public final Card moveToHand(final Card c, SpellAbility cause) {
        return moveToHand(c, cause, null);
    }

    public final Card moveToHand(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        final PlayerZone hand = c.getOwner().getZone(ZoneType.Hand);
        return moveTo(hand, c, cause, params);
    }

    public final Card moveToPlay(final Card c, SpellAbility cause) {
        final PlayerZone play = c.getController().getZone(ZoneType.Battlefield);
        return moveTo(play, c, cause, null);
    }

    public final Card moveToPlay(final Card c, final Player p, SpellAbility cause) {
        return moveToPlay(c, p, cause, null);
    }

    public final Card moveToPlay(final Card c, final Player p, SpellAbility cause, Map<AbilityKey, Object> params) {
        // move to a specific player's Battlefield
        final PlayerZone play = p.getZone(ZoneType.Battlefield);
        return moveTo(play, c, cause, params);
    }

    public final Card moveToBottomOfLibrary(final Card c, SpellAbility cause) {
        return moveToBottomOfLibrary(c, cause, null);
    }

    public final Card moveToBottomOfLibrary(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        return moveToLibrary(c, -1, cause, params);
    }

    public final Card moveToLibrary(final Card c, SpellAbility cause) {
        return moveToLibrary(c, cause, null);
    }

    public final Card moveToLibrary(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        return moveToLibrary(c, 0, cause, params);
    }

    public final Card moveToLibrary(Card c, int libPosition, SpellAbility cause) {
        return moveToLibrary(c, libPosition, cause, null);
    }

    public final Card moveToLibrary(Card c, int libPosition, SpellAbility cause, Map<AbilityKey, Object> params) {
        final PlayerZone library = c.getOwner().getZone(ZoneType.Library);
        if (libPosition == -1 || libPosition > library.size()) {
            libPosition = library.size();
        }
        return changeZone(game.getZoneOf(c), library, c, libPosition, cause, params);
    }

    public final Card moveToVariantDeck(Card c, ZoneType zone, int deckPosition, SpellAbility cause) {
        return moveToVariantDeck(c, zone, deckPosition, cause, null);
    }

    public final Card moveToVariantDeck(Card c, ZoneType zone, int deckPosition, SpellAbility cause, Map<AbilityKey, Object> params) {
        final PlayerZone deck = c.getOwner().getZone(zone);
        if (deckPosition == -1 || deckPosition > deck.size()) {
            deckPosition = deck.size();
        }
        return changeZone(game.getZoneOf(c), deck, c, deckPosition, cause, params);
    }

    public final Card exile(final Card c, SpellAbility cause) {
        if (c == null) {
            return null;
        }
        return exile(new CardCollection(c), cause).get(0);
    }
    public final CardCollection exile(final CardCollection cards, SpellAbility cause) {
        CardZoneTable table = new CardZoneTable();
        CardCollection result = new CardCollection();
        for (Card card : cards) {
            if (cause != null) {
                table.put(card.getZone().getZoneType(), ZoneType.Exile, card);
            }
            result.add(exile(card, cause, null));
        }
        if (cause != null) {
            table.triggerChangesZoneAll(game, cause);
        }
        return result;
    }
    public final Card exile(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        if (game.isCardExiled(c)) {
            return c;
        }

        final Zone origin = c.getZone();
        final PlayerZone removed = c.getOwner().getZone(ZoneType.Exile);
        final Card copied = moveTo(removed, c, cause, params);

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
        runParams.put(AbilityKey.Cause, cause);
        if (origin != null) { // is generally null when adding via dev mode
            runParams.put(AbilityKey.Origin, origin.getZoneType().name());
        }
        if (params != null) {
            runParams.putAll(params);
        }

        game.getTriggerHandler().runTrigger(TriggerType.Exiled, runParams, false);

        return copied;
    }

    public final Card moveTo(final ZoneType name, final Card c, SpellAbility cause) {
        return moveTo(name, c, 0, cause);
    }

    public final Card moveTo(final ZoneType name, final Card c, final int libPosition, SpellAbility cause) {
        return moveTo(name, c, libPosition, cause, null);
    }

    public final Card moveTo(final ZoneType name, final Card c, final int libPosition, SpellAbility cause, Map<AbilityKey, Object> params) {
        // Call specific functions to set PlayerZone, then move onto moveTo
        switch(name) {
            case Hand:          return moveToHand(c, cause, params);
            case Library:       return moveToLibrary(c, libPosition, cause, params);
            case Battlefield:   return moveToPlay(c, c.getController(), cause, params);
            case Graveyard:     return moveToGraveyard(c, cause, params);
            case Exile:         return exile(c, cause, params);
            case Stack:         return moveToStack(c, cause, params);
            case PlanarDeck:    return moveToVariantDeck(c, ZoneType.PlanarDeck, libPosition, cause, params);
            case SchemeDeck:    return moveToVariantDeck(c, ZoneType.SchemeDeck, libPosition, cause, params);
            default: // sideboard will also get there
                return moveTo(c.getOwner().getZone(name), c, cause);
        }
    }

    public void ceaseToExist(Card c, boolean skipTrig) {
        c.getZone().remove(c);

        // CR 603.6c other players LTB triggers should work
        if (!skipTrig) {
            game.addChangeZoneLKIInfo(c);
            CardCollectionView lastBattlefield = game.getLastStateBattlefield();
            int idx = lastBattlefield.indexOf(c);
            Card lki = null;
            if (idx != -1) {
                lki = lastBattlefield.get(idx);
            }
            if (lki == null) {
                lki = CardUtil.getLKICopy(c);
            }
            if (game.getCombat() != null) {
                game.getCombat().removeFromCombat(c);
                game.getCombat().saveLKI(lki);
            }
            // again, make sure no triggers run from cards leaving controlled by loser
            if (!lki.getController().equals(lki.getOwner())) {
                game.getTriggerHandler().registerActiveLTBTrigger(lki);
            }
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
            runParams.put(AbilityKey.CardLKI, lki);
            runParams.put(AbilityKey.Origin, c.getZone().getZoneType().name());
            game.getTriggerHandler().runTrigger(TriggerType.ChangesZone, runParams, false);
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
        checkStaticAbilities(runEvents, Sets.newHashSet(), CardCollection.EMPTY);
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

        for (final Player p : game.getPlayers()) {
            if (!game.getStack().isFrozen()) {
                p.getManaPool().restoreColorReplacements();
            }
            p.clearStaticAbilities();
        }

        // search for cards with static abilities
        final FCollection<StaticAbility> staticAbilities = new FCollection<>();
        final CardCollection staticList = new CardCollection();

        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public boolean visit(final Card c) {
                // need to get Card from preList if able
                final Card co = preList.get(c);
                for (StaticAbility stAb : co.getStaticAbilities()) {
                    if (stAb.getParam("Mode").equals("Continuous")) {
                        staticAbilities.add(stAb);
                    }
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
            List<StaticAbility> toAdd = Lists.newArrayList();
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
                if (affectedHere != null) {
                    for (final Card c : affectedHere) {
                        for (final StaticAbility st2 : c.getStaticAbilities()) {
                            if (!staticAbilities.contains(st2)) {
                                toAdd.add(st2);
                                st2.applyContinuousAbilityBefore(layer, preList);
                            }
                        }
                    }
                }
            }
            staticAbilities.addAll(toAdd);
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
            c.getStaticCommandList().removeAll(toRemove);
        }
        // Exclude cards in hidden zones from update
        /*
         * Refactoring this code to affectedCards.removeIf((Card c) -> c.isInZone(ZoneType.Library));
         * causes Android build not to compile
         * */
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
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            game.getTriggerHandler().runTrigger(TriggerType.Always, runParams, false);

            game.getTriggerHandler().runTrigger(TriggerType.Immediate, runParams, false);
        }

        // Update P/T and type in the view only once after all the cards have been processed, to avoid flickering
        for (Card c : affectedCards) {
            c.updateNameforView();
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
        checkStateEffects(runEvents, Sets.newHashSet());
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
        CardCollection cardsToUpdateLKI = new CardCollection();
        for (int q = 0; q < 9; q++) {
            checkStaticAbilities(false, affectedCards, CardCollection.EMPTY);
            boolean checkAgain = false;

            CardZoneTable table = new CardZoneTable();

            for (final Player p : game.getPlayers()) {
                for (final ZoneType zt : ZoneType.values()) {
                    if (zt == ZoneType.Command)
                        p.checkKeywordCard();

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
                            if (c.getLethal() <= dmg.intValue() || c.hasBeenDealtDeathtouchDamage()) {
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
                    else if (c.getDamage() > 0 && (c.getLethal() <= c.getDamage() || c.hasBeenDealtDeathtouchDamage())) {
                        if (desCreats == null) {
                            desCreats = new CardCollection();
                        }
                        desCreats.add(c);
                        c.setHasBeenDealtDeathtouchDamage(false);
                        checkAgain = true;
                    }
                }

                checkAgain |= stateBasedAction_Saga(c, table);
                checkAgain |= stateBasedAction704_attach(c, table); // Attachment

                if (c.isCreature() && c.isAttachedToEntity()) { // Rule 704.5q - Creature attached to an object or player, becomes unattached
                    c.unattachFromEntity(c.getEntityAttachedTo());
                    checkAgain = true;
                }

                checkAgain |= stateBasedAction704_5r(c); // annihilate +1/+1 counters with -1/-1 ones

                final CounterType dreamType = CounterType.get(CounterEnumType.DREAM);

                if (c.getCounters(dreamType) > 7 && c.hasKeyword("CARDNAME can't have more than seven dream counters on it.")) {
                    c.subtractCounter(dreamType,  c.getCounters(dreamType) - 7);
                    checkAgain = true;
                }

                if (c.hasKeyword("The number of loyalty counters on CARDNAME is equal to the number of Beebles you control.")) {
                    int beeble = CardLists.getValidCardCount(game.getCardsIn(ZoneType.Battlefield), "Beeble.YouCtrl", c.getController(), c, null);
                    int loyal = c.getCounters(CounterEnumType.LOYALTY);
                    if (loyal < beeble) {
                        GameEntityCounterTable counterTable = new GameEntityCounterTable();
                        c.addCounter(CounterEnumType.LOYALTY, beeble - loyal, c.getController(), false, counterTable);
                        counterTable.triggerCountersPutAll(game);
                    } else if (loyal > beeble) {
                        c.subtractCounter(CounterEnumType.LOYALTY, loyal - beeble);
                    }
                    // Only check again if counters actually changed
                    if (c.getCounters(CounterEnumType.LOYALTY) != loyal) {
                        checkAgain = true;
                    }
                }

                if (checkAgain) {
                    cardsToUpdateLKI.add(c);
                }
            }

            // only check static abilities once after destroying all the creatures
            // (e.g. helpful for Erebos's Titan and another creature dealing lethal damage to each other simultaneously)
            setHoldCheckingStaticAbilities(true);

            if (noRegCreats != null) {
                if (noRegCreats.size() > 1 && !orderedNoRegCreats) {
                    noRegCreats = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, noRegCreats, ZoneType.Graveyard, null);
                    orderedNoRegCreats = true;
                }
                for (Card c : noRegCreats) {
                    sacrificeDestroy(c, null, table);
                }
            }
            if (desCreats != null) {
                if (desCreats.size() > 1 && !orderedDesCreats) {
                    desCreats = CardLists.filter(desCreats, CardPredicates.Presets.CAN_BE_DESTROYED);
                    if (!desCreats.isEmpty()) {
                        desCreats = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, desCreats, ZoneType.Graveyard, null);
                    }
                    orderedDesCreats = true;
                }
                for (Card c : desCreats) {
                    destroy(c, null, true, table);
                }
            }
            setHoldCheckingStaticAbilities(false);

            if (game.getTriggerHandler().runWaitingTriggers()) {
                checkAgain = true;
            }

            for (Player p : game.getPlayers()) {
                if (handleLegendRule(p, table)) {
                    checkAgain = true;
                }

                if ((game.getRules().hasAppliedVariant(GameType.Commander)
                        || game.getRules().hasAppliedVariant(GameType.Brawl)
                        || game.getRules().hasAppliedVariant(GameType.Planeswalker)) && !checkAgain) {
                    Iterable<Card> cards = p.getCardsIn(ZoneType.Graveyard).threadSafeIterable();
                    for (final Card c : cards) {
                        checkAgain |= stateBasedAction903_9a(c);
                    }
                    cards = p.getCardsIn(ZoneType.Exile).threadSafeIterable();
                    for (final Card c : cards) {
                        checkAgain |= stateBasedAction903_9a(c);
                    }
                }

                if (handlePlaneswalkerRule(p, table)) {
                    checkAgain = true;
                }
            }
            // 704.5m World rule
            checkAgain |= handleWorldRule(table);

            if (game.getCombat() != null) {
                game.getCombat().removeAbsentCombatants();
            }
            table.triggerChangesZoneAll(game, null);
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

        for (final Card c : cardsToUpdateLKI) {
            game.updateLastStateForCard(c);
        }

        if (!refreeze) {
            game.getStack().unfreezeStack();
        }
    }

    private boolean stateBasedAction_Saga(Card c, CardZoneTable table) {
        boolean checkAgain = false;
        if (!c.getType().hasSubtype("Saga")) {
            return false;
        }
        if (!c.canBeSacrificed()) {
            return false;
        }
        if (c.getCounters(CounterEnumType.LORE) < c.getFinalChapterNr()) {
            return false;
        }
        if (!game.getStack().hasSimultaneousStackEntries() &&
                !game.getStack().hasSourceOnStack(c, SpellAbilityPredicates.isChapter())) {
            sacrifice(c, null, table);
            checkAgain = true;
        }
        return checkAgain;
    }

    private boolean stateBasedAction704_attach(Card c, CardZoneTable table) {
        boolean checkAgain = false;

        if (c.isAttachedToEntity()) {
            final GameEntity ge = c.getEntityAttachedTo();
            if (!ge.canBeAttached(c, true)) {
                c.unattachFromEntity(ge);
                checkAgain = true;
            }
        }

        if (c.hasCardAttachments()) {
            for (final Card attach : Lists.newArrayList(c.getAttachedCards())) {
                if (!attach.isInPlay()) {
                    attach.unattachFromEntity(c);
                    checkAgain = true;
                }
            }
        }

        // cleanup aura
        if (c.isAura() && c.isInPlay() && !c.isEnchanting()) {
            sacrificeDestroy(c, null, table);
            checkAgain = true;
        }
        return checkAgain;
    }

    private boolean stateBasedAction903_9a(Card c) {
        if (c.isRealCommander() && c.canMoveToCommandZone()) {
            c.setMoveToCommandZone(false);
            if (c.getOwner().getController().confirmAction(c.getSpellPermanent(), PlayerActionConfirmMode.ChangeZoneToAltDestination, c.getName() + ": If a commander is in a graveyard or in exile and that card was put into that zone since the last time state-based actions were checked, its owner may put it into the command zone.", null)) {
                moveTo(c.getOwner().getZone(ZoneType.Command), c, null);
                return true;
            }
        }
        return false;
    }

    private boolean stateBasedAction704_5r(Card c) {
        boolean checkAgain = false;
        final CounterType p1p1 = CounterType.get(CounterEnumType.P1P1);
        final CounterType m1m1 = CounterType.get(CounterEnumType.M1M1);
        int plusOneCounters = c.getCounters(p1p1);
        int minusOneCounters = c.getCounters(m1m1);
        if (plusOneCounters > 0 && minusOneCounters > 0) {
            int remove = Math.min(plusOneCounters, minusOneCounters);
            // If a permanent has both a +1/+1 counter and a -1/-1 counter on it,
            // N +1/+1 and N -1/-1 counters are removed from it, where N is the
            // smaller of the number of +1/+1 and -1/-1 counters on it.
            // This should fire remove counters trigger
            c.subtractCounter(p1p1, remove);
            c.subtractCounter(m1m1, remove);
            checkAgain = true;
        }
        return checkAgain;
    }

    // If a token is in a zone other than the battlefield, it ceases to exist.
    private boolean stateBasedAction704_5d(Card c) {
        boolean checkAgain = false;
        if (c.isRealToken()) {
            final Zone zoneFrom = game.getZoneOf(c);
            if (!zoneFrom.is(ZoneType.Battlefield)) {
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

    private boolean handlePlaneswalkerRule(Player p, CardZoneTable table) {
        // get all Planeswalkers
        final List<Card> list = CardLists.filter(p.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.PLANESWALKERS);
        boolean recheck = false;

        //final Multimap<String, Card> uniqueWalkers = ArrayListMultimap.create(); // Not used as of Ixalan

        for (Card c : list) {
            if (c.getCounters(CounterEnumType.LOYALTY) <= 0) {
                sacrificeDestroy(c, null, table);
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

    private boolean handleLegendRule(Player p, CardZoneTable table) {
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

            Card toKeep = p.getController().chooseSingleEntityForEffect(new CardCollection(cc), new SpellAbility.EmptySa(ApiType.InternalLegendaryRule, null, p),
                    "You have multiple legendary permanents named \""+name+"\" in play.\n\nChoose the one to stay on battlefield (the rest will be moved to graveyard)", null);
            for (Card c: cc) {
                if (c != toKeep) {
                    sacrificeDestroy(c, null, table);
                }
            }
            game.fireEvent(new GameEventCardDestroyed());
        }

        return recheck;
    }

    private boolean handleWorldRule(CardZoneTable table) {
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
            sacrificeDestroy(c, null, table);
            game.fireEvent(new GameEventCardDestroyed());
        }

        return true;
    }

    @Deprecated
    public final Card sacrifice(final Card c, final SpellAbility source) {
        return sacrifice(c, source, null);
    }
    public final Card sacrifice(final Card c, final SpellAbility source, CardZoneTable table) {
        if (!c.canBeSacrificedBy(source)) {
            return null;
        }

        c.getController().addSacrificedThisTurn(c, source);

        return sacrificeDestroy(c, source, table);
    }

    public final boolean destroy(final Card c, final SpellAbility sa, final boolean regenerate, CardZoneTable table) {
        Player activator = null;
        if (!c.canBeDestroyed()) {
            return false;
        }

        // Replacement effects
        final Map<AbilityKey, Object> repRunParams = AbilityKey.mapFromCard(c);
        repRunParams.put(AbilityKey.Source, sa);
        repRunParams.put(AbilityKey.Affected, c);
        repRunParams.put(AbilityKey.Regeneration, regenerate);

        if (game.getReplacementHandler().run(ReplacementType.Destroy, repRunParams) != ReplacementResult.NotReplaced) {
            return false;
        }


        if (sa != null) {
            activator = sa.getActivatingPlayer();
        }

        // Play the Destroy sound
        game.fireEvent(new GameEventCardDestroyed());

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
        runParams.put(AbilityKey.Causer, activator);
        game.getTriggerHandler().runTrigger(TriggerType.Destroyed, runParams, false);
        // in case the destroyed card has such a trigger
        game.getTriggerHandler().registerActiveLTBTrigger(c);
        
        final Card sacrificed = sacrificeDestroy(c, sa, table);
        return sacrificed != null;
    }

    /**
     * @return the sacrificed Card in its new location, or {@code null} if the
     * sacrifice wasn't successful.
     */
    protected final Card sacrificeDestroy(final Card c, SpellAbility cause, CardZoneTable table) {
        if (!c.isInPlay()) {
            return null;
        }

        final Card newCard = moveToGraveyard(c, cause, null);
        if (table != null) {
            table.put(ZoneType.Battlefield, newCard.getZone().getZoneType(), newCard);
        }

        return newCard;
    }

    public void revealTo(final Card card, final Player to) {
        revealTo(card, Collections.singleton(to));
    }
    public void revealTo(final CardCollectionView cards, final Player to) {
        revealTo(cards, to, null);
    }
    public void revealTo(final CardCollectionView cards, final Player to, String messagePrefix) {
        revealTo(cards, Collections.singleton(to), messagePrefix);
    }
    public void revealTo(final Card card, final Iterable<Player> to) {
        revealTo(new CardCollection(card), to);
    }
    public void revealTo(final CardCollectionView cards, final Iterable<Player> to) {
        revealTo(cards, to, null);
    }
    public void revealTo(final CardCollectionView cards, final Iterable<Player> to, String messagePrefix) {
        if (cards.isEmpty()) {
            return;
        }

        final ZoneType zone = cards.getFirst().getZone().getZoneType();
        final Player owner = cards.getFirst().getOwner();
        for (final Player p : to) {
            p.getController().reveal(cards, zone, owner, messagePrefix);
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
    public void notifyOfValue(SpellAbility saSource, GameObject relatedTarget, String value, Player playerExcept) {
        for (Player p : game.getPlayers()) {
            if (playerExcept == p) continue;
            p.getController().notifyOfValue(saSource, relatedTarget, value);
        }
    }

    private void drawStartingHand(Player p1) {
        //check initial hand
        List<Card> lib1 = Lists.newArrayList(p1.getZone(ZoneType.Library).getCards().threadSafeIterable());
        List<Card> hand1 = lib1.subList(0,p1.getMaxHandSize());

        //shuffle
        List<Card> shuffledCards = Lists.newArrayList(p1.getZone(ZoneType.Library).getCards().threadSafeIterable());
        Collections.shuffle(shuffledCards);

        //check a second hand
        List<Card> hand2 = shuffledCards.subList(0,p1.getMaxHandSize());

        //choose better hand according to land count
        float averageLandRatio = getLandRatio(lib1);
        if (getHandScore(hand1, averageLandRatio) > getHandScore(hand2, averageLandRatio)) {
            p1.getZone(ZoneType.Library).setCards(shuffledCards);
        }
        p1.drawCards(p1.getMaxHandSize());
    }

    private float getLandRatio(List<Card> deck) {
        int landCount = 0;
        for (Card c:deck) {
            if (c.isLand()){
                landCount++;
            }
        }
        if(landCount == 0) {
            return 0;
        }
        return Float.valueOf(landCount)/Float.valueOf(deck.size());
    }

    private float getHandScore(List<Card> hand, float landRatio) {
        int landCount = 0;
        for (Card c:hand) {
            if (c.isLand()) {
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

             runPreOpeningHandActions(first);

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
                new MulliganService(first).perform();
            }
            if (game.isGameOver()) { break; } // conceded during "mulligan" prompt

            game.setAge(GameStage.Play);

            //<THIS CODE WILL WORK WITH PHASE = NULL>
            if (game.getRules().hasAppliedVariant(GameType.Planechase)) {
                first.initPlane();
            }

            first =  runOpeningHandActions(first);
            checkStateEffects(true); // why?

            // Run Trigger beginning of the game
            game.getTriggerHandler().runTrigger(TriggerType.NewGame, AbilityKey.newMap(), true);
            //</THIS CODE WILL WORK WITH PHASE = NULL>


            game.getPhaseHandler().startFirstTurn(first, startGameHook);
            //after game ends, ensure Auto-Pass canceled for all players so it doesn't apply to next game
            for (Player p : game.getRegisteredPlayers()) {
                p.setNumCardsInHandStartedThisTurnWith(p.getCardsIn(ZoneType.Hand).size());
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

    // Returns the new player to go first
    private Player runOpeningHandActions(final Player first) {
        Player takesAction = first;
        Player newFirst = first;
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
                if (sa.hasParam("BecomeStartingPlayer")) {
                    newFirst = takesAction;
                }
            }
            takesAction = game.getNextPlayerAfter(takesAction);
        } while (takesAction != first);
        // state effects are checked only when someone gets priority
        return newFirst;
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

    public void becomeMonarch(final Player p, final String set) {
        final Player previous = game.getMonarch();
        if (p == null || p.equals(previous))
            return;

        if (previous != null)
            previous.removeMonarchEffect();

        p.createMonarchEffect(set);
        game.setMonarch(p);

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Player, p);
        game.getTriggerHandler().runTrigger(TriggerType.BecomeMonarch, runParams, false);
    }

    // Make scry an action function so that it can be used for mulligans (with a null cause)
    // Assumes that the list of players is in APNAP order, which should be the case
    // Optional here as well to handle the way that mulligans do the choice
    // 701.17. Scry
    // 701.17a To "scry N" means to look at the top N cards of your library, then put any number of them
    // on the bottom of your library in any order and the rest on top of your library in any order.
    // 701.17b If a player is instructed to scry 0, no scry event occurs. Abilities that trigger whenever a
    // player scries won’t trigger.
    // 701.17c If multiple players scry at once, each of those players looks at the top cards of their library
    // at the same time. Those players decide in APNAP order (see rule 101.4) where to put those
    // cards, then those cards move at the same time.
    public void scry(final List<Player> players, int numScry, SpellAbility cause) {
        if (numScry <= 0) {
            return;
        }

        // in case something alters the scry amount
        Map<Player, Integer> actualPlayers = Maps.newLinkedHashMap();

        for (final Player p : players) {
            int playerScry = numScry;
            final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(p);
            repParams.put(AbilityKey.Source, cause);
            repParams.put(AbilityKey.Num, playerScry);

            switch (game.getReplacementHandler().run(ReplacementType.Scry, repParams)) {
                case NotReplaced:
                    break;
                case Updated: {
                    playerScry = (int) repParams.get(AbilityKey.Num);
                    break;
                }
                default:
                    continue;
            }
            if (playerScry > 0) {
                actualPlayers.put(p, playerScry);

                // reveal the top N library cards to the player (only)
                // no real need to separate out the look if
                // there is only one player scrying
                if (players.size() > 1) {
                    final CardCollection topN = new CardCollection(p.getCardsIn(ZoneType.Library, playerScry));
                    revealTo(topN, p);
                }
            }
        }

        // make the decisions
        Map<Player, ImmutablePair<CardCollection, CardCollection>> decisions = Maps.newLinkedHashMap();
        for (final Map.Entry<Player, Integer> e : actualPlayers.entrySet()) {
            final Player p = e.getKey();
            final CardCollection topN = new CardCollection(p.getCardsIn(ZoneType.Library, e.getValue()));
            ImmutablePair<CardCollection, CardCollection> decision = p.getController().arrangeForScry(topN);
            decisions.put(p, decision);
            int numToTop = decision.getLeft() == null ? 0 : decision.getLeft().size();
            int numToBottom = decision.getRight() == null ? 0 : decision.getRight().size();

            // publicize the decision
            game.fireEvent(new GameEventScry(p, numToTop, numToBottom));
        }
        // do the moves after all the decisions (maybe not necesssary, but let's
        // do it the official way)
        for (Map.Entry<Player, ImmutablePair<CardCollection, CardCollection>> e : decisions.entrySet()) {
            // no good iterate simultaneously in Java
            final Player p = e.getKey();
            final CardCollection toTop = e.getValue().getLeft();
            final CardCollection toBottom = e.getValue().getRight();
            if (toTop != null) {
                Collections.reverse(toTop); // reverse to get the correct order
                for (Card c : toTop) {
                    moveToLibrary(c, cause, null);
                }
            }
            if (toBottom != null) {
                for (Card c : toBottom) {
                    moveToBottomOfLibrary(c, cause, null);
                }
            }

            if (cause != null) {
                // set up triggers (but not actually do them until later)
                final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.Player, p);
                game.getTriggerHandler().runTrigger(TriggerType.Scry, runParams, false);
            }
        }
    }
}
