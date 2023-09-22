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
import forge.card.MagicColor;
import forge.deck.DeckSection;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.event.*;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.mulligan.MulliganService;
import forge.game.player.GameLossReason;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityPredicates;
import forge.game.spellability.SpellPermanent;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityCantAttackBlock;
import forge.game.staticability.StaticAbilityLayer;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.*;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;

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

        // Rule 111.8: A token that has left the battlefield can't move to another zone
        if (!c.isSpell() && c.isToken() && !fromBattlefield && zoneFrom != null && !zoneFrom.is(ZoneType.Stack)
                && (cause == null || !(cause instanceof SpellPermanent) || !cause.hasSVar("IsCastFromPlayEffect"))) {
            return c;
        }

        // Rules 304.4, 307.4: non-permanents (instants, sorceries) can't enter the battlefield and remain
        // in their previous zone
        if (toBattlefield && !c.isPermanent()) {
            return c;
        }

        // Aura entering indirectly
        // need to check before it enters
        if (c.isAura() && !c.isAttachedToEntity() && toBattlefield && (zoneFrom == null || !zoneFrom.is(ZoneType.Stack))) {
            boolean found = false;
            try {
                if (Iterables.any(game.getPlayers(), PlayerPredicates.canBeAttached(c, null))) {
                    found = true;
                }
            } catch (Exception e1) {
                found = false;
            }

            if (!found) {
                try {
                    if (Iterables.any((CardCollectionView) params.get(AbilityKey.LastStateBattlefield), CardPredicates.canBeAttached(c, null))) {
                        found = true;
                    }
                } catch (Exception e2) {
                    found = false;
                }
            }

            if (!found) {
                try {
                    if (Iterables.any((CardCollectionView) params.get(AbilityKey.LastStateGraveyard), CardPredicates.canBeAttached(c, null))) {
                        found = true;
                    }
                } catch (Exception e3) {
                    found = false;
                }
            }
            if (!found) {
                c.clearControllers();
                if (cause != null) {
                    unanimateOnAbortedChange(cause, c);
                }
                return c;
            }
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
            if (ReplacementType.Moved.equals(re.getMode()) && cause.getReplacingObject(AbilityKey.CardLKI).equals(c)) {
                lastKnownInfo = (Card) cause.getReplacingObject(AbilityKey.CardLKI);
            }
        }
        CardCollectionView lastBattlefield = null;
        if (params != null) {
            lastBattlefield = (CardCollectionView) params.get(AbilityKey.LastStateBattlefield);
        }
        if (lastBattlefield == null && cause != null) {
            lastBattlefield = cause.getLastStateBattlefield();
        }
        if (lastBattlefield == null) {
            lastBattlefield = game.getLastStateBattlefield();
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

        if (fromBattlefield && !toBattlefield) {
            c.getController().setRevolt(true);
        }

        // Don't copy Tokens, copy only cards leaving the battlefield
        // and returning to hand (to recreate their spell ability information)
        if (toBattlefield || (suppress && zoneTo.getZoneType().isHidden())) {
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
                int idx = lastBattlefield.indexOf(c);
                if (idx != -1) {
                    lastKnownInfo = lastBattlefield.get(idx);
                }
            }

            if (lastKnownInfo == null) {
                lastKnownInfo = CardUtil.getLKICopy(c);
            }

            // LKI is only needed when something is moved from the battlefield.
            // also it does messup with Blink Effects like Eldrazi Displacer
            if (fromBattlefield && !zoneTo.is(ZoneType.Stack) && !zoneTo.is(ZoneType.Flashback)) {
                game.addChangeZoneLKIInfo(lastKnownInfo);
            }

            // CR 707.12 casting of a card copy, don't copy it again
            if (zoneTo.is(ZoneType.Stack) && c.isRealToken()) {
                copied = c;
            } else {
                copied = CardFactory.copyCard(c, false);
            }

            copied.setTimestamp(c.getTimestamp());

            if (zoneTo.is(ZoneType.Stack)) {
                // try not to copy changed stats when moving to stack

                // copy exiled properties when adding to stack
                // will be cleanup later in MagicStack
                copied.setExiledWith(c.getExiledWith());
                copied.setExiledBy(c.getExiledBy());
                copied.setDrawnThisTurn(c.getDrawnThisTurn());

                if (c.isTransformed()) {
                    copied.incrementTransformedTimestamp();
                }

                if (cause != null && cause.isSpell() && c.equals(cause.getHostCard())) {
                    copied.setCastSA(cause);
                    copied.setSplitStateToPlayAbility(cause);

                    // CR 112.2 A spell’s controller is, by default, the player who put it on the stack.
                    copied.setController(cause.getActivatingPlayer(), 0);
                    KeywordInterface kw = cause.getKeyword();
                    if (kw != null) {
                        copied.addKeywordForStaticAbility(kw);
                    }
                }
            } else {
                // when a card leaves the battlefield, ensure it's in its original state
                // (we need to do this on the object before copying it, or it won't work correctly e.g.
                // on Transformed objects)
                copied.setState(CardStateName.Original, false);
                copied.setBackSide(false);
            }

            copied.setUnearthed(c.isUnearthed());

            // need to copy counters when card enters another zone than hand or library
            if (lastKnownInfo.hasKeyword("Counters remain on CARDNAME as it moves to any zone other than a player's hand or library.") &&
                    !(zoneTo.is(ZoneType.Hand) || zoneTo.is(ZoneType.Library))) {
                copied.setCounters(Maps.newHashMap(lastKnownInfo.getCounters()));
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
                        if (re.hasParam("CommanderMoveReplacement") && c.getMergedCards().contains(effCard.getEffectSource())) {
                            commanderEffect = effCard;
                            break;
                        }
                    }
                    if (commanderEffect != null) break;
                }
                // Disable the commander replacement effect
                if (commanderEffect != null) {
                    for (final ReplacementEffect re : commanderEffect.getReplacementEffects()) {
                        re.setSuppressed(true);
                    }
                }
            }

            // in addition to actual tokens, cards "made" by digital-only mechanics
            // are also added to inbound tokens so their etb replacements will work
            if (zoneFrom == null || zoneFrom.is(ZoneType.None)) {
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
            if (repres != ReplacementResult.NotReplaced && repres != ReplacementResult.Updated) {
                // reset failed manifested Cards back to original
                if (c.isManifested() && !c.isInPlay()) {
                    c.forceTurnFaceUp();
                }

                copied.getOwner().removeInboundToken(copied);

                if (repres == ReplacementResult.Prevented) {
                    c.clearEtbCounters();
                    c.clearControllers();
                    if (cause != null) {
                        unanimateOnAbortedChange(cause, c);
                        if (cause.hasParam("Transformed") || cause.hasParam("FaceDown")) {
                            c.setBackSide(false);
                            c.changeToState(CardStateName.Original);
                        }
                        unattachCardLeavingBattlefield(c);
                    }

                    if (c.isInZone(ZoneType.Stack) && !zoneTo.is(ZoneType.Graveyard)) {
                        return moveToGraveyard(c, cause, params);
                    }

                    copied.clearDevoured();
                    copied.clearDelved();
                    copied.clearConvoked();
                    copied.clearExploited();
                } else if (toBattlefield && !c.isInPlay()) {
                    // was replaced with another Zone Change
                    if (c.removeChangedState()) {
                        c.updateStateForView();
                    }
                }

                return c;
            }
        }

        if (!zoneTo.is(ZoneType.Stack)) {
            // reset timestamp in changezone effects so they have same timestamp if ETB simultaneously
            copied.setTimestamp(game.getNextTimestamp());
        }

        copied.getOwner().removeInboundToken(copied);

        // Aura entering as Copy from stack
        // without targets it is sent to graveyard
        if (copied.isAura() && !copied.isAttachedToEntity() && toBattlefield) {
            if (zoneFrom != null && zoneFrom.is(ZoneType.Stack) && game.getStack().isResolving(c)) {
                boolean found = false;
                if (Iterables.any(game.getPlayers(), PlayerPredicates.canBeAttached(copied, null))) {
                    found = true;
                }
                if (Iterables.any((CardCollectionView) params.get(AbilityKey.LastStateBattlefield), CardPredicates.canBeAttached(copied, null))) {
                    found = true;
                }
                if (Iterables.any((CardCollectionView) params.get(AbilityKey.LastStateGraveyard), CardPredicates.canBeAttached(copied, null))) {
                    found = true;
                }
                if (!found) {
                    return moveToGraveyard(copied, cause, params);
                }
            }
            attachAuraOnIndirectEnterBattlefield(copied, params);
        }

        // Handle merged permanent here so all replacement effects are already applied.
        CardCollection mergedCards = null;
        if (fromBattlefield && !toBattlefield && c.hasMergedCard()) {
            CardCollection cards = new CardCollection(c.getMergedCards());
            // replace top card with copied card for correct name for human to choose.
            cards.set(cards.indexOf(c), copied);
            // 723.3b
            if (cause != null && zoneTo.getZoneType() == ZoneType.Exile) {
                cards = (CardCollection) cause.getHostCard().getController().getController().orderMoveToZoneList(cards, zoneTo.getZoneType(), cause);
            } else {
                cards = (CardCollection) c.getOwner().getController().orderMoveToZoneList(cards, zoneTo.getZoneType(), cause);
            }
            cards.set(cards.indexOf(copied), c);
            if (zoneTo.is(ZoneType.Library)) {
                Collections.reverse(cards);
            }
            mergedCards = cards;
            if (cause != null) {
                // Replace sa targeting cards
                final SpellAbility saTargeting = cause.getSATargetingCard();
                if (saTargeting != null) {
                    saTargeting.getTargets().replaceTargetCard(c, cards);
                }
                // Replace host remembered cards
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
            if (fromBattlefield && game.getCombat() != null) {
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

            if (zoneFrom.is(ZoneType.Stack) && toBattlefield) {
                // 400.7a Effects from static abilities that give a permanent spell on the stack an ability
                // that allows it to be cast for an alternative cost continue to apply to the permanent that spell becomes.
                if (c.getCastSA() != null && !c.getCastSA().isIntrinsic() && c.getCastSA().getKeyword() != null) {
                    KeywordInterface ki = c.getCastSA().getKeyword();
                    ki.setHostCard(copied);
                    copied.addChangedCardKeywordsInternal(ImmutableList.of(ki), null, false, copied.getTimestamp(), 0, true);
                }

                // 607.2q linked ability can find cards exiled as cost while it was a spell
                copied.addExiledCards(c.getExiledCards());
            }
        }

        // if an adventureCard is put from Stack somewhere else, need to reset to Original State
        if (copied.isAdventureCard() && ((zoneFrom != null && zoneFrom.is(ZoneType.Stack)) || !zoneTo.is(ZoneType.Stack))) {
            copied.setState(CardStateName.Original, false);
        }

        GameEntityCounterTable table = new GameEntityCounterTable();

        if (mergedCards != null) {
            // Move components of merged permanent here
            // Also handle 723.3e and 903.9a
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
                // 723.3e & 903.9a
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
                    zoneTo.add(copied, position, toBattlefield ? null : lastKnownInfo); // the modified state of the card is also reported here (e.g. for Morbid + Awaken)
                } else {
                    zoneTo.add(card, position, CardUtil.getLKICopy(card));
                }
                card.setZone(zoneTo);
            }
        } else {
            // "enter the battlefield as a copy" - apply code here
            // but how to query for input here and continue later while the callers assume synchronous result?
            zoneTo.add(copied, position, toBattlefield ? null : lastKnownInfo); // the modified state of the card is also reported here (e.g. for Morbid + Awaken)
            c.setZone(zoneTo);
        }

        if (fromBattlefield) {
            // order here is important so it doesn't unattach cards that might have returned from UntilHostLeavesPlay
            unattachCardLeavingBattlefield(copied);
            c.runLeavesPlayCommands();
        }

        // do ETB counters after zone add
        if (!suppress && toBattlefield && !copied.getEtbCounters().isEmpty()) {
            game.getTriggerHandler().registerActiveTrigger(copied, false);
            copied.putEtbCounters(table);
            copied.clearEtbCounters();
        }

        // intensity is perpetual
        if (c.hasIntensity()) {
            copied.setIntensity(c.getIntensity(false));
        }
        // specialize is perpetual
        if (c.isSpecialized()) {
            copied.setState(c.getCurrentStateName(), false);
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
        }

        table.replaceCounterEffect(game, null, true, true, params);

        // Need to apply any static effects to produce correct triggers
        checkStaticAbilities();

        // 400.7g try adding keyword back into card if it doesn't already have it
        if (zoneTo.is(ZoneType.Stack) && cause != null && cause.isSpell() && !cause.isIntrinsic() && c.equals(cause.getHostCard())) {
            if (cause.getKeyword() != null && !copied.getKeywords().contains(cause.getKeyword())) {
                copied.addChangedCardKeywordsInternal(ImmutableList.of(cause.getKeyword()), null, false, game.getNextTimestamp(), 0, true);
            }
        }

        // CR 603.6b
        if (toBattlefield) {
            zoneTo.saveLKI(copied, lastKnownInfo);
        }

        // only now that the LKI preserved it
        if (!zoneTo.is(ZoneType.Stack)) {
            c.cleanupExiledWith();
        }

        game.getTriggerHandler().clearActiveTriggers(copied, null);
        // register all LTB trigger from last state battlefield
        for (Card lki : lastBattlefield) {
            game.getTriggerHandler().registerActiveLTBTrigger(lki);
        }
        game.getTriggerHandler().registerActiveTrigger(copied, false);

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
            if (params != null) {
                runParams2.putAll(params);
            }
            game.getTriggerHandler().runTrigger(TriggerType.ChangesController, runParams2, false);
        }

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
            if (!c.isRealToken() && !c.isSpecialized()) {
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
        } else if (toBattlefield) {
            for (Player p : game.getPlayers()) {
                copied.getDamageHistory().setNotAttackedSinceLastUpkeepOf(p);
                copied.getDamageHistory().setNotBlockedSinceLastUpkeepOf(p);
                copied.getDamageHistory().setNotBeenBlockedSinceLastUpkeepOf(p);
            }
        } else if (zoneTo.is(ZoneType.Graveyard)
                || zoneTo.is(ZoneType.Hand)
                || zoneTo.is(ZoneType.Library)
                || zoneTo.is(ZoneType.Exile)) {
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
        return moveTo(zoneTo, c, cause, AbilityKey.newMap());
    }
    public final Card moveTo(final Zone zoneTo, Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        // FThreads.assertExecutedByEdt(false); // This code must never be executed from EDT,
        // use FThreads.invokeInNewThread to run code in a pooled thread
        return moveTo(zoneTo, c, null, cause, params);
    }
    public final Card moveTo(final Zone zoneTo, Card c, Integer position, SpellAbility cause) {
        return moveTo(zoneTo, c, position, cause, AbilityKey.newMap());
    }

    public final Card moveTo(final ZoneType name, final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        return moveTo(name, c, 0, cause, params);
    }
    public final Card moveTo(final ZoneType name, final Card c, final int libPosition, SpellAbility cause) {
        return moveTo(name, c, libPosition, cause, AbilityKey.newMap());
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

        // need to refresh ability text for affected cards
        for (final StaticAbility stAb : c.getStaticAbilities()) {
            if (!stAb.checkConditions()) {
                continue;
            }

            if (stAb.checkMode("CantBlockBy")) {
                if (!stAb.hasParam("ValidAttacker") || (stAb.hasParam("ValidBlocker") && stAb.getParam("ValidBlocker").equals("Creature.Self"))) {
                    continue;
                }
                for (Card creature : Iterables.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES)) {
                    if (stAb.matchesValidParam("ValidAttacker", creature)) {
                        creature.updateAbilityTextForView();
                    }
                }
            }
            if (stAb.checkMode(StaticAbilityCantAttackBlock.MinMaxBlockerMode)) {
                for (Card creature : Iterables.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.CREATURES)) {
                    if (stAb.matchesValidParam("ValidCard", creature)) {
                        creature.updateAbilityTextForView();
                    }
                }
            }
        }

        // Move card in maingame if take card from subgame
        // 720.4a
        if (zoneFrom != null && zoneFrom.is(ZoneType.Sideboard) && game.getMaingame() != null) {
            Card maingameCard = c.getOwner().getMappingMaingameCard(c);
            if (maingameCard != null) {
                if (maingameCard.getZone().is(ZoneType.Stack)) {
                    game.getMaingame().getStack().remove(maingameCard);
                }
                game.getMaingame().getAction().moveTo(ZoneType.Subgame, maingameCard, null, params);
            }
        }

        if (zoneTo.is(ZoneType.Stack)) {
            // zoneFrom maybe null if the spell is cast from "Ouside the game", ex. ability of Garth One-Eye
            c.setCastFrom(zoneFrom);
            if (cause != null && cause.isSpell() && c.equals(cause.getHostCard())) {
                c.setCastSA(cause);
            } else {
                c.setCastSA(null);
            }
        } else if (zoneFrom == null || !(zoneFrom.is(ZoneType.Stack) &&
                (zoneTo.is(ZoneType.Battlefield) || zoneTo.is(ZoneType.Merged)))) {
            c.setCastFrom(null);
            c.setCastSA(null);
        }

        if (c.isRealCommander()) {
            c.setMoveToCommandZone(true);
        }

        return c;
    }

    public final Card moveToStack(final Card c, SpellAbility cause) {
        Map<AbilityKey, Object> params = AbilityKey.newMap();
        params.put(AbilityKey.LastStateBattlefield, game.getLastStateBattlefield());
        params.put(AbilityKey.LastStateGraveyard, game.getLastStateGraveyard());
        return moveToStack(c, cause, params);
    }
    public final Card moveToStack(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        return moveTo(game.getStackZone(), c, cause, params);
    }

    public final Card moveToGraveyard(final Card c, SpellAbility cause) {
        return moveToGraveyard(c, cause, AbilityKey.newMap());
    }
    public final Card moveToGraveyard(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        final PlayerZone grave = c.getOwner().getZone(ZoneType.Graveyard);
        // must put card in OWNER's graveyard not controller's
        return moveTo(grave, c, cause, params);
    }

    public final Card moveToHand(final Card c, SpellAbility cause) {
        return moveToHand(c, cause, AbilityKey.newMap());
    }
    public final Card moveToHand(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        final PlayerZone hand = c.getOwner().getZone(ZoneType.Hand);
        return moveTo(hand, c, cause, params);
    }

    public final Card moveToPlay(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        return moveToPlay(c, c.getController(), cause, params);
    }
    public final Card moveToPlay(final Card c, final Player p, SpellAbility cause, Map<AbilityKey, Object> params) {
        // move to a specific player's Battlefield
        final PlayerZone play = p.getZone(ZoneType.Battlefield);
        return moveTo(play, c, cause, params);
    }

    public final Card moveToBottomOfLibrary(final Card c, SpellAbility cause) {
        return moveToBottomOfLibrary(c, cause, AbilityKey.newMap());
    }
    public final Card moveToBottomOfLibrary(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        return moveToLibrary(c, -1, cause, params);
    }

    public final Card moveToLibrary(final Card c, SpellAbility cause) {
        return moveToLibrary(c, cause, AbilityKey.newMap());
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

    public final Card moveToVariantDeck(Card c, ZoneType zone, int deckPosition, SpellAbility cause, Map<AbilityKey, Object> params) {
        final PlayerZone deck = c.getOwner().getZone(zone);
        if (deckPosition == -1 || deckPosition > deck.size()) {
            deckPosition = deck.size();
        }
        return changeZone(game.getZoneOf(c), deck, c, deckPosition, cause, params);
    }

    public final CardCollection exile(final CardCollection cards, SpellAbility cause, Map<AbilityKey, Object> params) {
        CardZoneTable table = new CardZoneTable();
        CardCollection result = new CardCollection();
        for (Card card : cards) {
            if (cause != null) {
                table.put(card.getZone().getZoneType(), ZoneType.Exile, card);
            }
            result.add(exile(card, cause, params));
        }
        if (cause != null) {
            table.triggerChangesZoneAll(game, cause);
        }
        return result;
    }
    public final Card exile(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
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

    public void ceaseToExist(Card c, boolean skipTrig) {
        if (c.isInZone(ZoneType.Stack)) {
            c.getGame().getStack().remove(c);
        }

        final Zone z = c.getZone();
        // in some corner cases there's no zone yet (copied spell that failed targeting)
        if (z != null) {
            z.remove(c);
            if (z.is(ZoneType.Battlefield)) {
                c.runLeavesPlayCommands();
            }

        }

        // CR 603.6c other players LTB triggers should work
        if (!skipTrig) {
            CardCollectionView lastBattlefield = game.getLastStateBattlefield();
            int idx = lastBattlefield.indexOf(c);
            Card lki = null;
            if (idx != -1) {
                lki = lastBattlefield.get(idx);
            }
            if (lki == null) {
                lki = CardUtil.getLKICopy(c);
            }
            game.addChangeZoneLKIInfo(lki);
            if (lki.isInPlay()) {
                if (game.getCombat() != null) {
                    game.getCombat().saveLKI(lki);
                    game.getCombat().removeFromCombat(c);
                }
                // again, make sure no triggers run from cards leaving controlled by loser
                if (!lki.getController().equals(lki.getOwner())) {
                    game.getTriggerHandler().registerActiveLTBTrigger(lki);
                }
            }
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
            runParams.put(AbilityKey.CardLKI, lki);
            runParams.put(AbilityKey.Origin, c.getZone().getZoneType().name());
            game.getTriggerHandler().runTrigger(TriggerType.ChangesZone, runParams, false);
        }
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

        // run Game Commands early
        c.runChangeControllerCommands();

        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);

        oldBattlefield.remove(c);
        newBattlefield.add(c);
        if (game.getPhaseHandler().inCombat()) {
            game.getCombat().removeFromCombat(c);
        }

        c.setCameUnderControlSinceLastUpkeep(true);

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
        runParams.put(AbilityKey.OriginalController, original);
        game.getTriggerHandler().runTrigger(TriggerType.ChangesController, runParams, false);

        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

    }

    // Temporarily disable (if mode = true) actively checking static abilities.
    private void setHoldCheckingStaticAbilities(boolean mode) {
        holdCheckingStaticAbilities = mode;
    }

    private boolean isCheckingStaticAbilitiesOnHold() {
        return holdCheckingStaticAbilities;
    }

    // This doesn't check layers or if the ability gets removed by other effects
    public boolean hasStaticAbilityAffectingZone(ZoneType zone, StaticAbilityLayer layer) {
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions("Continuous")) {
                    continue;
                }
                if (layer != null && !stAb.getLayers().contains(layer)) {
                    continue;
                }
                if (ZoneType.listValueOf(stAb.getParamOrDefault("AffectedZone", ZoneType.Battlefield.toString())).contains(zone)) {
                    return true;
                }
            }
        }
        return false;
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
                    if (stAb.checkMode("Continuous")) {
                        staticAbilities.add(stAb);
                    }
                 }
                 if (!co.getStaticCommandList().isEmpty()) {
                     staticList.add(co);
                 }
                 return true;
            }
        }, true);

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

        // preList means that this is run by a pre Check with LKI objects
        // in that case Always trigger should not Run
        if (preList.isEmpty()) {
            for (Player p : game.getPlayers()) {
                for (Card c : p.getCardsIn(ZoneType.Battlefield).threadSafeIterable()) {
                    if (!c.getController().equals(p)) {
                        controllerChangeZoneCorrection(c);
                        affectedCards.add(c);
                    }
                    if (c.isCreature() && c.isPaired()) {
                        Card partner = c.getPairedWith();
                        if (!partner.isCreature() || c.getController() != partner.getController() || !c.isInPlay()) {
                            c.setPairedWith(null);
                            partner.setPairedWith(null);
                            affectedCards.add(c);
                        }
                    }
                }
            }

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

        // TODO filter out old copies from zone change

        if (runEvents && !affectedCards.isEmpty()) {
            game.fireEvent(new GameEventCardStatsChanged(affectedCards));
        }
        game.getTracker().unfreeze();
    }

    public final boolean checkStateEffects(final boolean runEvents) {
        return checkStateEffects(runEvents, Sets.newHashSet());
    }
    public boolean checkStateEffects(final boolean runEvents, final Set<Card> affectedCards) {
        // sol(10/29) added for Phase updates, state effects shouldn't be
        // checked during Spell Resolution (except when persist-returning
        if (game.getStack().isResolving()) {
            return false;
        }

        if (game.isGameOver()) {
            return false;
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
        boolean performedSBA = false;
        boolean orderedDesCreats = false;
        boolean orderedNoRegCreats = false;
        boolean orderedSacrificeList = false;
        CardCollection cardsToUpdateLKI = new CardCollection();

        Map<AbilityKey, Object> mapParams = AbilityKey.newMap();
        mapParams.put(AbilityKey.LastStateBattlefield, game.getLastStateBattlefield());
        mapParams.put(AbilityKey.LastStateGraveyard, game.getLastStateGraveyard());
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
                    for (final Card c : p.getCardsIn(zt).threadSafeIterable()) {
                        checkAgain |= stateBasedAction704_5d(c);
                         // Dungeon Card won't affect other cards, so don't need to set checkAgain
                        stateBasedAction_Dungeon(c);
                    }
                }
            }
            CardCollection noRegCreats = new CardCollection();
            CardCollection desCreats = null;
            CardCollection unAttachList = new CardCollection();
            CardCollection sacrificeList = new CardCollection();
            PlayerCollection spaceSculptors = new PlayerCollection();
            for (final Card c : game.getCardsIn(ZoneType.Battlefield)) {
                boolean checkAgainCard = false;
                if (c.hasKeyword(Keyword.SPACE_SCULPTOR)) {
                    spaceSculptors.add(c.getController());
                }
                if (c.isCreature()) {
                    // Rule 704.5f - Put into grave (no regeneration) for toughness <= 0
                    if (c.getNetToughness() <= 0) {
                        noRegCreats.add(c);
                        checkAgainCard = true;
                    } else if (c.hasKeyword(Keyword.INDESTRUCTIBLE)) {
                        //702.12b. A permanent with indestructible can't be destroyed.
                        // Such permanents aren't destroyed by lethal damage, and they
                        // ignore the state-based action that checks for lethal damage
                    } else if (c.hasKeyword("CARDNAME can't be destroyed by lethal damage unless lethal damage dealt by a single source is marked on it.")) {
                        if (c.getLethal() <= c.getMaxDamageFromSource() || c.hasBeenDealtDeathtouchDamage()) {
                            if (desCreats == null) {
                                desCreats = new CardCollection();
                            }
                            desCreats.add(c);
                            c.setHasBeenDealtDeathtouchDamage(false);
                            checkAgainCard = true;
                        }
                    }
                    // Rule 704.5g - Destroy due to lethal damage
                    // Rule 704.5h - Destroy due to deathtouch
                    else if (c.hasBeenDealtDeathtouchDamage() || (c.getDamage() > 0 && c.getLethal() <= c.getDamage())) {
                        if (desCreats == null) {
                            desCreats = new CardCollection();
                        }
                        desCreats.add(c);
                        c.setHasBeenDealtDeathtouchDamage(false);
                        checkAgainCard = true;
                    }
                }

                checkAgainCard |= stateBasedAction_Saga(c, sacrificeList);
                checkAgainCard |= stateBasedAction_Battle(c, noRegCreats);
                checkAgainCard |= stateBasedAction_Role(c, unAttachList);
                checkAgainCard |= stateBasedAction704_attach(c, unAttachList); // Attachment

                checkAgainCard |= stateBasedAction704_5r(c); // annihilate +1/+1 counters with -1/-1 ones

                final CounterType dreamType = CounterType.get(CounterEnumType.DREAM);

                if (c.getCounters(dreamType) > 7 && c.hasKeyword("CARDNAME can't have more than seven dream counters on it.")) {
                    c.subtractCounter(dreamType,  c.getCounters(dreamType) - 7);
                    checkAgainCard = true;
                }

                if (c.hasKeyword("The number of loyalty counters on CARDNAME is equal to the number of Beebles you control.")) {
                    int beeble = CardLists.getValidCardCount(game.getCardsIn(ZoneType.Battlefield), "Beeble.YouCtrl", c.getController(), c, null);
                    int loyal = c.getCounters(CounterEnumType.LOYALTY);
                    if (loyal < beeble) {
                        GameEntityCounterTable counterTable = new GameEntityCounterTable();
                        c.addCounter(CounterEnumType.LOYALTY, beeble - loyal, c.getController(), counterTable);
                        counterTable.replaceCounterEffect(game, null, false);
                    } else if (loyal > beeble) {
                        c.subtractCounter(CounterEnumType.LOYALTY, loyal - beeble);
                    }
                    // Only check again if counters actually changed
                    if (c.getCounters(CounterEnumType.LOYALTY) != loyal) {
                        checkAgainCard = true;
                    }
                }

                // cleanup aura
                if (c.isAura() && c.isInPlay() && !c.isEnchanting()) {
                    noRegCreats.add(c);
                    checkAgainCard = true;
                }
                if (checkAgainCard) {
                    cardsToUpdateLKI.add(c);
                    checkAgain = true;
                }
            }
            for (Card u : unAttachList) {
                u.unattachFromEntity(u.getEntityAttachedTo());

                // cleanup aura
                if (u.isAura() && u.isInPlay() && !u.isEnchanting()) {
                    noRegCreats.add(u);
                    checkAgain = true;
                }
            }

            for (Player p : game.getPlayers()) {
                if (!spaceSculptors.isEmpty() && !spaceSculptors.contains(p)) {
                    checkAgain |= stateBasedAction704_5u(p);
                }
                if (handleLegendRule(p, noRegCreats)) {
                    checkAgain = true;
                }

                if ((game.getRules().hasAppliedVariant(GameType.Commander)
                        || game.getRules().hasAppliedVariant(GameType.Brawl)
                        || game.getRules().hasAppliedVariant(GameType.Planeswalker)) && !checkAgain) {
                    for (final Card c : p.getCardsIn(ZoneType.Graveyard).threadSafeIterable()) {
                        checkAgain |= stateBasedAction903_9a(c);
                    }
                    for (final Card c : p.getCardsIn(ZoneType.Exile).threadSafeIterable()) {
                        checkAgain |= stateBasedAction903_9a(c);
                    }
                }

                if (handlePlaneswalkerRule(p, noRegCreats)) {
                    checkAgain = true;
                }
            }
            for (Player p : spaceSculptors) {
                checkAgain |= stateBasedAction704_5u(p);
            }
            // 704.5m World rule
            checkAgain |= handleWorldRule(noRegCreats);
            // only check static abilities once after destroying all the creatures
            // (e.g. helpful for Erebos's Titan and another creature dealing lethal damage to each other simultaneously)
            setHoldCheckingStaticAbilities(true);

            if (noRegCreats.size() > 1 && !orderedNoRegCreats) {
                noRegCreats = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, noRegCreats, ZoneType.Graveyard, null);
                orderedNoRegCreats = true;
            }
            for (Card c : noRegCreats) {
                c.updateWasDestroyed(true);
                sacrificeDestroy(c, null, table, mapParams);
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
                    destroy(c, null, true, table, mapParams);
                }
            }

            if (sacrificeList.size() > 1 && !orderedSacrificeList) {
                sacrificeList = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, sacrificeList, ZoneType.Graveyard, null);
                orderedSacrificeList = true;
            }
            for (Card c : sacrificeList) {
                c.updateWasDestroyed(true);
                sacrifice(c, null, true, table, mapParams);
            }
            setHoldCheckingStaticAbilities(false);

            // important to collect first otherwise if a static fires it will mess up registered ones from LKI
            game.getTriggerHandler().collectTriggerForWaiting();
            if (game.getTriggerHandler().runWaitingTriggers()) {
                checkAgain = true;
            }

            if (game.getCombat() != null) {
                game.getCombat().removeAbsentCombatants();
            }

            table.triggerChangesZoneAll(game, null);

            if (!checkAgain) {
                break; // do not continue the loop
            } else {
                performedSBA = true;
            }
        } // for q=0;q<9

        game.getTracker().unfreeze();

        if (runEvents && !affectedCards.isEmpty()) {
            game.fireEvent(new GameEventCardStatsChanged(affectedCards));
        }

        // recheck the game over condition at this point to make sure no other win conditions apply now.
        if (!game.isGameOver()) {
            checkGameOverCondition();
        }

        if (game.getAge() != GameStage.Play) {
            return false;
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

        // Run all commands that are queued to run after state based actions are checked
        game.runSBACheckedCommands();

        return performedSBA;
    }

    private boolean stateBasedAction_Saga(Card c, CardCollection sacrificeList) {
        boolean checkAgain = false;
        if (!c.getType().hasSubtype("Saga")) {
            return false;
        }
        if (!c.canBeSacrificedBy(null, true)) {
            return false;
        }
        if (c.getCounters(CounterEnumType.LORE) < c.getFinalChapterNr()) {
            return false;
        }
        if (!game.getStack().hasSourceOnStack(c, SpellAbilityPredicates.isChapter())) {
            // needs to be effect, because otherwise it might be a cost?
            sacrificeList.add(c);
            checkAgain = true;
        }
        return checkAgain;
    }

    private boolean stateBasedAction_Battle(Card c, CardCollection removeList) {
        boolean checkAgain = false;
        if (!c.getType().isBattle()) {
            return false;
        }
        if (c.getCounters(CounterEnumType.DEFENSE) > 0) {
            return false;
        }
        // 704.5v If a battle has defense 0 and it isn’t the source of an ability that has triggered but not yet left the stack,
        // it’s put into its owner’s graveyard.
        if (!game.getStack().hasSourceOnStack(c, SpellAbilityPredicates.isTrigger())) {
            removeList.add(c);
            checkAgain = true;
        }
        return checkAgain;
    }
    private boolean stateBasedAction_Role(Card c, CardCollection removeList) {
        if (!c.hasCardAttachments()) {
            return false;
        }
        boolean checkAgain = false;
        CardCollection roles = CardLists.filter(c.getAttachedCards(), CardPredicates.isType("Role"));
        if (roles.isEmpty()) {
            return false;
        }

        for (Player p : game.getPlayers()) {
            CardCollection rolesByPlayer = CardLists.filterControlledBy(roles, p);
            if (rolesByPlayer.size() <= 1) {
                continue;
            }
            // sort by game timestamp
            rolesByPlayer.sort(CardPredicates.compareByTimestamp());
            removeList.addAll(rolesByPlayer.subList(0, rolesByPlayer.size() - 1));
            checkAgain = true;
        }
        return checkAgain;
    }

    private void stateBasedAction_Dungeon(Card c) {
        if (!c.getType().isDungeon() || !c.isInLastRoom()) {
            return;
        }
        if (!game.getStack().hasSourceOnStack(c, null)) {
            completeDungeon(c.getController(), c);
        }
    }

    private boolean stateBasedAction704_attach(Card c, CardCollection unAttachList) {
        boolean checkAgain = false;

        if (c.isAttachedToEntity()) {
            final GameEntity ge = c.getEntityAttachedTo();
            // Rule 704.5q - Creature attached to an object or player, becomes unattached
            if (c.isCreature() || c.isBattle() || !ge.canBeAttached(c, null, true)) {
                unAttachList.add(c);
                checkAgain = true;
            }
        }

        if (c.hasCardAttachments()) {
            for (final Card attach : c.getAttachedCards()) {
                if (!attach.isInPlay()) {
                    unAttachList.add(attach);
                    checkAgain = true;
                }
            }
        }

        return checkAgain;
    }

    private boolean stateBasedAction704_5u(Player p) {
        boolean checkAgain = false;

        CardCollection toAssign = new CardCollection();

        for (final Card c : p.getCreaturesInPlay().threadSafeIterable()) {
            if (!c.hasSector()) {
                toAssign.add(c);
                if (!checkAgain) {
                    checkAgain = true;
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        for (Card assignee : toAssign) { // probably would be nice for players to pick order of assigning?
            String sector = p.getController().chooseSector(assignee, "Assign");
            assignee.assignSector(sector);
            if (sb.length() == 0) {
                sb.append(p).append(" ").append(Localizer.getInstance().getMessage("lblAssigns")).append("\n");
            }
            String creature = CardTranslation.getTranslatedName(assignee.getName()) + " (" + assignee.getId() + ")";
            sb.append(creature).append(" ").append(sector).append("\n");
        }
        if (sb.length() > 0) {
            notifyOfValue(null, p, sb.toString(), p);
        }

        return checkAgain;
    }

    private boolean stateBasedAction903_9a(Card c) {
        if (c.isRealCommander() && c.canMoveToCommandZone()) {
            // FIXME: need to flush the tracker to make sure the Commander is properly updated
            c.getGame().getTracker().flush();

            c.setMoveToCommandZone(false);
            if (c.getOwner().getController().confirmAction(c.getFirstSpellAbility(), PlayerActionConfirmMode.ChangeZoneToAltDestination, c.getName() + ": If a commander is in a graveyard or in exile and that card was put into that zone since the last time state-based actions were checked, its owner may put it into the command zone.", null)) {
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

            // card copies are allowed on the stack
            if (zoneFrom.is(ZoneType.Stack) && c.getCopiedPermanent() != null) {
                return false;
            }

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

    private boolean handlePlaneswalkerRule(Player p, CardCollection noRegCreats) {
        // get all Planeswalkers
        final List<Card> list = p.getPlaneswalkersInPlay();
        boolean recheck = false;

        for (Card c : list) {
            if (c.getCounters(CounterEnumType.LOYALTY) <= 0) {
                noRegCreats.add(c);
                recheck = true;
            }
        }

        return recheck;
    }

    private boolean handleLegendRule(Player p, CardCollection noRegCreats) {
        final List<Card> a = Lists.newArrayList();

        // check for ignore legend rule
        for (Card c : CardLists.getType(p.getCardsIn(ZoneType.Battlefield), "Legendary")) {
            if (!c.ignoreLegendRule()) {
                a.add(c);
            }
        }

        if (a.isEmpty()) {
            return false;
        }
        boolean recheck = false;

        // Corner Case 1: Legendary with non legendary creature names
        CardCollection nonLegendaryNames = CardLists.filter(a, new Predicate<Card>() {
            @Override
            public boolean apply(Card input) {
                return input.hasNonLegendaryCreatureNames();
            }

        });

        Multimap<String, Card> uniqueLegends = Multimaps.index(a, CardPredicates.Accessors.fnGetNetName);
        CardCollection removed = new CardCollection();

        for (String name : uniqueLegends.keySet()) {
            // skip the ones with empty names
            if (name.isEmpty()) {
                continue;
            }
            CardCollection cc = new CardCollection(uniqueLegends.get(name));
            // check if it is a non legendary creature name
            // if yes, then add the other legendary with Spy Kit too
            if (!name.isEmpty() && StaticData.instance().getCommonCards().isNonLegendaryCreatureName(name)) {
                cc.addAll(nonLegendaryNames);
            }
            if (cc.size() < 2) {
                continue;
            }

            recheck = true;

            Card toKeep = p.getController().chooseSingleEntityForEffect(cc, new SpellAbility.EmptySa(ApiType.InternalLegendaryRule, new Card(-1, game), p),
                    "You have multiple legendary permanents named \""+name+"\" in play.\n\nChoose the one to stay on battlefield (the rest will be moved to graveyard)", null);
            cc.remove(toKeep);
            removed.addAll(cc);
        }

        // Corner Case 2: with all non legendary creature names
        CardCollection emptyNameAllNonLegendary = new CardCollection(nonLegendaryNames);
        // remove the ones that got already removed by other legend rule above
        emptyNameAllNonLegendary.removeAll(removed);
        if (emptyNameAllNonLegendary.size() > 1) {
            recheck = true;

            Card toKeep = p.getController().chooseSingleEntityForEffect(emptyNameAllNonLegendary, new SpellAbility.EmptySa(ApiType.InternalLegendaryRule, new Card(-1, game), p),
                    "You have multiple legendary permanents with non legendary creature names in play.\n\nChoose the one to stay on battlefield (the rest will be moved to graveyard)", null);
            emptyNameAllNonLegendary.remove(toKeep);
            removed.addAll(emptyNameAllNonLegendary);

        }
        noRegCreats.addAll(removed);

        return recheck;
    }

    private boolean handleWorldRule(CardCollection noRegCreats) {
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

        noRegCreats.addAll(worlds);

        return true;
    }

    public final Card sacrifice(final Card c, final SpellAbility source, final boolean effect, CardZoneTable table, Map<AbilityKey, Object> params) {
        if (!c.canBeSacrificedBy(source, effect)) {
            return null;
        }

        c.getController().addSacrificedThisTurn(c, source);

        c.updateWasDestroyed(true);
        return sacrificeDestroy(c, source, table, params);
    }

    public final boolean destroy(final Card c, final SpellAbility sa, final boolean regenerate, CardZoneTable table, Map<AbilityKey, Object> params) {
        if (!c.canBeDestroyed()) {
            return false;
        }

        // Replacement effects
        final Map<AbilityKey, Object> repRunParams = AbilityKey.mapFromAffected(c);
        repRunParams.put(AbilityKey.Cause, sa);
        repRunParams.put(AbilityKey.Regeneration, regenerate);
        if (params != null) {
            repRunParams.putAll(params);
        }

        if (game.getReplacementHandler().run(ReplacementType.Destroy, repRunParams) != ReplacementResult.NotReplaced) {
            return false;
        }

        Player activator = null;
        if (sa != null) {
            activator = sa.getActivatingPlayer();
        }

        //for animation
        c.updateWasDestroyed(true);
        // Play the Destroy sound
        game.fireEvent(new GameEventCardDestroyed());

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
        runParams.put(AbilityKey.Causer, activator);
        if (params != null) {
            runParams.putAll(params);
        }
        game.getTriggerHandler().runTrigger(TriggerType.Destroyed, runParams, false);
        // in case the destroyed card has such a trigger
        game.getTriggerHandler().registerActiveLTBTrigger(c);

        final Card sacrificed = sacrificeDestroy(c, sa, table, params);
        return sacrificed != null;
    }

    /**
     * @return the sacrificed Card in its new location, or {@code null} if the
     * sacrifice wasn't successful.
     */
    protected final Card sacrificeDestroy(final Card c, SpellAbility cause, CardZoneTable table, Map<AbilityKey, Object> params) {
        if (!c.isInPlay()) {
            return null;
        }

        final Card newCard = moveToGraveyard(c, cause, params);
        if (table != null && newCard != null && newCard.getZone() != null) {
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

    public void revealUnplayableByAI(String title, Map<Player, Map<DeckSection, List<? extends PaperCard>>> unplayableCards) {
        // Notify both players
        for (Player p : game.getPlayers()) {
            p.getController().revealAISkipCards(title, unplayableCards);
        }
    }

    public void revealAnte(String title, Multimap<Player, PaperCard> removedAnteCards) {
        // Notify both players
        for (Player p : game.getPlayers()) {
            p.getController().revealAnte(title, removedAnteCards);
        }
    }

    /** Delivers a message to all players. (use reveal to show Cards) */
    public void notifyOfValue(SpellAbility saSource, GameObject relatedTarget, String value, Player playerExcept) {
        if (saSource != null) {
            String name = CardTranslation.getTranslatedName(saSource.getHostCard().getName());
            value = TextUtil.fastReplace(value, "CARDNAME", name);
            value = TextUtil.fastReplace(value, "NICKNAME", Lang.getInstance().getNickName(name));
        }
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
            if (c.isLand()) {
                landCount++;
            }
        }
        if (landCount == 0) {
            return 0;
        }
        return Float.valueOf(landCount)/Float.valueOf(deck.size());
    }

    private float getHandScore(List<Card> hand, float landRatio) {
        int landCount = 0;
        for (Card c : hand) {
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

            first = runOpeningHandActions(first);
            checkStateEffects(true); // why?

            // Run Trigger beginning of the game
            game.getTriggerHandler().runTrigger(TriggerType.NewGame, AbilityKey.newMap(), true);
            //</THIS CODE WILL WORK WITH PHASE = NULL>

            game.setStartingPlayer(first);
            game.getPhaseHandler().startFirstTurn(first, startGameHook);
            //after game ends, ensure Auto-Pass canceled for all players so it doesn't apply to next game
            for (Player p : game.getRegisteredPlayers()) {
                p.setNumCardsInHandStartedThisTurnWith(p.getCardsIn(ZoneType.Hand).size());
                p.getController().autoPassCancel();
            }

            first = game.getPhaseHandler().getPlayerTurn(); // needed only for restart
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
            List<Card> ploys = CardLists.filter(takesAction.getCardsIn(ZoneType.Command), new Predicate<Card>() {
                @Override
                public boolean apply(Card input) {
                    return input.getName().equals("Emissary's Ploy");
                }
            });
            CardCollectionView all = CardLists.filterControlledBy(game.getCardsInGame(), takesAction);
            List<Card> spires = CardLists.filter(all, new Predicate<Card>() {
                    @Override
                    public boolean apply(Card input) {
                        return input.getName().equals("Cryptic Spires");
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
            for (Card c : spires) {
                if (!c.hasChosenColor()) {
                    List<String> colorChoices = new ArrayList<>(MagicColor.Constant.ONLY_COLORS);
                    String prompt = CardTranslation.getTranslatedName(c.getName()) + ": " +
                            Localizer.getInstance().getMessage("lblChooseNColors", Lang.getNumeral(2));
                    SpellAbility sa = new SpellAbility.EmptySa(ApiType.ChooseColor, c, takesAction);
                    sa.putParam("AILogic", "MostProminentInComputerDeck");
                    List<String> chosenColors = takesAction.getController().chooseColors(prompt, sa, 2, 2, colorChoices);
                    c.setChosenColors(chosenColors);
                }
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
        } else {
            ThreadUtil.invokeInGameThread(proc);
        }
    }

    public void becomeMonarch(final Player p, final String set) {
        final Player previous = game.getMonarch();
        if (p == null || p.equals(previous)) {
            return;
        }

        if (previous != null)
            previous.removeMonarchEffect();

        // by Monarch losing, its a way to make the game lose the monarch
        if (!p.canBecomeMonarch()) {
            return;
        }

        p.createMonarchEffect(set);
        game.setMonarch(p);

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(p);
        game.getTriggerHandler().runTrigger(TriggerType.BecomeMonarch, runParams, false);
    }

    public void takeInitiative(final Player p, final String set) {
        final Player previous = game.getHasInitiative();
        if (p == null) {
            return;
        }

        if (!p.equals(previous)) {
            if (previous != null) {
                previous.removeInitiativeEffect();
            }

            if (p.hasLost()) { // the person who should take initiative is gone, it goes to next player
                takeInitiative(game.getNextPlayerAfter(p), set);
            }

            game.setHasInitiative(p);
            p.createInitiativeEffect(set);
        }

        // You can take the initiative even if you already have it
        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(p);
        game.getTriggerHandler().runTrigger(TriggerType.TakesInitiative, runParams, false);
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
        // do the moves after all the decisions (maybe not necessary, but let's
        // do it the official way)
        for (Map.Entry<Player, ImmutablePair<CardCollection, CardCollection>> e : decisions.entrySet()) {
            // no good iterate simultaneously in Java
            final Player p = e.getKey();
            final CardCollection toTop = e.getValue().getLeft();
            final CardCollection toBottom = e.getValue().getRight();
            int numLookedAt = 0;
            if (toTop != null) {
                numLookedAt += toTop.size();
                Collections.reverse(toTop); // reverse to get the correct order
                for (Card c : toTop) {
                    moveToLibrary(c, cause, null);
                }
            }
            if (toBottom != null) {
                numLookedAt += toBottom.size();
                for (Card c : toBottom) {
                    moveToBottomOfLibrary(c, cause, null);
                }
            }

            if (cause != null) {
                // set up triggers (but not actually do them until later)
                final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(p);
                runParams.put(AbilityKey.ScryNum, numLookedAt);
                runParams.put(AbilityKey.ScryBottom, toBottom == null ? 0 : toBottom.size());
                game.getTriggerHandler().runTrigger(TriggerType.Scry, runParams, false);
            }
        }
    }

    public void dealDamage(final boolean isCombat, final CardDamageMap damageMap, final CardDamageMap preventMap,
            final GameEntityCounterTable counterTable, final SpellAbility cause) {
        // Clear assigned damage if is combat
        if (isCombat) {
            for (Map.Entry<GameEntity, Map<Card, Integer>> et : damageMap.columnMap().entrySet()) {
                final GameEntity ge = et.getKey();
                if (ge instanceof Card) {
                    ((Card) ge).clearAssignedDamage();
                }
            }
        }

        // Run replacement effect for each entity dealt damage
        game.getReplacementHandler().runReplaceDamage(isCombat, damageMap, preventMap, counterTable, cause);

        Map<Card, Integer> lethalDamage = Maps.newHashMap();
        Map<Integer, Card> lkiCache = Maps.newHashMap();

        // Actually deal damage according to replaced damage map
        for (Map.Entry<Card, Map<GameEntity, Integer>> et : damageMap.rowMap().entrySet()) {
            final Card sourceLKI = et.getKey();
            int sum = 0;
            for (Map.Entry<GameEntity, Integer> e : et.getValue().entrySet()) {
                if (e.getValue() <= 0) {
                    continue;
                }

                if (e.getKey() instanceof Card && !lethalDamage.containsKey(e.getKey())) {
                    Card c = (Card) e.getKey();
                    lethalDamage.put(c, c.getExcessDamageValue(false));
                }

                e.setValue(Integer.valueOf(e.getKey().addDamageAfterPrevention(e.getValue(), sourceLKI, cause, isCombat, counterTable)));
                sum += e.getValue();

                sourceLKI.getDamageHistory().registerDamage(e.getValue(), isCombat, sourceLKI, e.getKey(), lkiCache);
            }

            // CR 702.15e
            if (sum > 0 && sourceLKI.hasKeyword(Keyword.LIFELINK)) {
                sourceLKI.getController().gainLife(sum, sourceLKI, cause);
            }
        }

        // for Zangief do this before runWaitingTriggers DamageDone
        damageMap.triggerExcessDamage(isCombat, lethalDamage, game, cause, lkiCache);

        // lose life simultaneously
        Map<Player, Integer> lifeLostAllDamageMap = Maps.newHashMap();
        for (Player p : game.getPlayers()) {
            int lost = p.processDamage();
            if (lost > 0) {
                lifeLostAllDamageMap.put(p, lost);
            }
        }

        if (isCombat) {
            game.getTriggerHandler().runWaitingTriggers();
        }

        if (!lifeLostAllDamageMap.isEmpty()) { // Run triggers if any player actually lost life
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPIMap(lifeLostAllDamageMap);
            game.getTriggerHandler().runTrigger(TriggerType.LifeLostAll, runParams, false);
        }

        if (cause != null) {
            // Remember objects as needed
            final Card sourceLKI = game.getChangeZoneLKIInfo(cause.getHostCard());
            final boolean rememberCard = cause.hasParam("RememberDamaged") || cause.hasParam("RememberDamagedCreature");
            final boolean rememberPlayer = cause.hasParam("RememberDamaged") || cause.hasParam("RememberDamagedPlayer");
            if (rememberCard || rememberPlayer) {
                for (GameEntity e : damageMap.row(sourceLKI).keySet()) {
                    if (e instanceof Card && rememberCard) {
                        cause.getHostCard().addRemembered(e);
                    } else if (e instanceof Player && rememberPlayer) {
                        cause.getHostCard().addRemembered(e);
                    }
                }
            }
            if (cause.hasParam("RememberAmount")) {
                cause.getHostCard().addRemembered(damageMap.totalAmount());
            }
        }

        preventMap.triggerPreventDamage(isCombat);
        preventMap.clear();

        damageMap.triggerDamageDoneOnce(isCombat, game);
        damageMap.clear();

        counterTable.replaceCounterEffect(game, cause, !isCombat);
        counterTable.clear();
    }

    public void completeDungeon(Player player, Card dungeon) {
        player.addCompletedDungeon(dungeon);
        ceaseToExist(dungeon, true);

        // Run RoomEntered trigger
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(dungeon);
        runParams.put(AbilityKey.Player, player);
        game.getTriggerHandler().runTrigger(TriggerType.DungeonCompleted, runParams, false);
    }

    /**
     * Attach aura on indirect enter battlefield.
     *
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean attachAuraOnIndirectEnterBattlefield(final Card source, Map<AbilityKey, Object> params) {
        // When an Aura ETB without being cast you can choose a valid card to
        // attach it to
        final SpellAbility aura = source.getFirstAttachSpell();

        if (aura == null) {
            return false;
        }
        aura.setActivatingPlayer(source.getController());
        final Game game = source.getGame();
        final TargetRestrictions tgt = aura.getTargetRestrictions();

        Player p = source.getController();
        if (tgt.canTgtPlayer()) {
            final FCollection<Player> players = game.getPlayers().filter(PlayerPredicates.canBeAttached(source, aura));

            final Player pa = p.getController().chooseSingleEntityForEffect(players, aura,
                    Localizer.getInstance().getMessage("lblSelectAPlayerAttachSourceTo", CardTranslation.getTranslatedName(source.getName())), null);
            if (pa != null) {
                source.attachToEntity(pa, null, true);
                return true;
            }
        } else {
            List<ZoneType> zones = Lists.newArrayList(tgt.getZone());
            CardCollection list = new CardCollection();

            if (params != null) {
                if (zones.contains(ZoneType.Battlefield)) {
                    list.addAll((CardCollectionView) params.get(AbilityKey.LastStateBattlefield));
                    zones.remove(ZoneType.Battlefield);
                }
                if (zones.contains(ZoneType.Graveyard)) {
                    list.addAll((CardCollectionView) params.get(AbilityKey.LastStateGraveyard));
                    zones.remove(ZoneType.Graveyard);
                }
            }
            list.addAll(game.getCardsIn(zones));

            list = CardLists.filter(list, CardPredicates.canBeAttached(source, aura));
            if (list.isEmpty()) {
                return false;
            }

            final Card o = p.getController().chooseSingleEntityForEffect(list, aura,
                    Localizer.getInstance().getMessage("lblSelectACardAttachSourceTo", CardTranslation.getTranslatedName(source.getName())), null);
            if (o != null) {
                source.attachToEntity(game.getCardState(o), null, true);
                return true;
            }
        }
        return false;
    }

    private static void unanimateOnAbortedChange(final SpellAbility cause, final Card c) {
        if (cause.hasParam("AnimateSubAbility")) {
            long unanimateTimestamp = Long.valueOf(cause.getAdditionalAbility("AnimateSubAbility").getSVar("unanimateTimestamp"));
            c.removeChangedCardKeywords(unanimateTimestamp, 0);
            c.removeChangedCardTypes(unanimateTimestamp, 0);
            c.removeChangedName(unanimateTimestamp, 0);
            c.removeNewPT(unanimateTimestamp, 0);
            if (c.removeChangedCardTraits(unanimateTimestamp, 0)) {
                c.updateStateForView();
            }
        }
    }
}
