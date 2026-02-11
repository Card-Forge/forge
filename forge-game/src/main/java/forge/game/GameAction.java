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

import com.google.common.collect.*;
import forge.GameCommand;
import forge.StaticData;
import forge.card.CardStateName;
import forge.card.CardType.Supertype;
import forge.card.ColorSet;
import forge.card.GamePieceType;
import forge.deck.DeckSection;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.*;
import forge.game.event.*;
import forge.game.extrahands.BackupPlanService;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.mulligan.MulliganService;
import forge.game.player.*;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellPermanent;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityCountersRemain;
import forge.game.staticability.StaticAbilityContinuous;
import forge.game.staticability.StaticAbilityLayer;
import forge.game.staticability.StaticAbilityMode;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.*;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jgrapht.alg.cycle.SzwarcfiterLauerSimpleCycles;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.*;
import java.util.function.Predicate;

/**
 * Methods for common actions performed during a game.
 *
 * @author Forge
 * @version $Id$
 */
public class GameAction {
    private final Game game;

    private boolean holdCheckingStaticAbilities = false;

    private final static Comparator<StaticAbility> effectOrder = Comparator.comparing(StaticAbility::isCharacteristicDefining).reversed()
            .thenComparing(StaticAbility::getTimestamp);

    public GameAction(Game game0) {
        game = game0;
    }

    public Card changeZone(final Zone zoneFrom, Zone zoneTo, final Card c, Integer position, SpellAbility cause) {
        return changeZone(zoneFrom, zoneTo, c, position, cause, null);
    }
    private Card changeZone(final Zone zoneFrom, Zone zoneTo, final Card c, Integer position, SpellAbility cause, Map<AbilityKey, Object> params) {
        // 111.11. A copy of a permanent spell becomes a token as it resolves.
        // The token has the characteristics of the spell that became that token.
        // The token is not “created” for the purposes of any replacement effects or triggered abilities that refer to creating a token.
        if (c.isCopiedSpell() && zoneTo.is(ZoneType.Battlefield) && c.isPermanent() && cause != null && cause.isSpell() && c.equals(cause.getHostCard())) {
            c.setGamePieceType(GamePieceType.TOKEN);
        }

        if (c.isCopiedSpell() || (c.isImmutable() && zoneTo.is(ZoneType.Exile))) {
            // Remove Effect from command immediately, this is essential when some replacement
            // effects happen during the resolving of a spellability ("the next time ..." effect)
            if (zoneFrom != null) {
                zoneFrom.remove(c);
            }
            return c;
        }

        // dev mode
        if (zoneFrom == null && !c.isToken()) {
            zoneTo.add(c, position, CardCopyService.getLKICopy(c));
            checkStaticAbilities();
            game.getTriggerHandler().registerActiveTrigger(c, true);
            game.fireEvent(new GameEventCardChangeZone(c, zoneFrom, zoneTo));
            return c;
        }

        boolean toBattlefield = zoneTo.is(ZoneType.Battlefield) || zoneTo.is(ZoneType.Merged);
        boolean fromBattlefield = zoneFrom != null && zoneFrom.is(ZoneType.Battlefield);
        boolean fromGraveyard = zoneFrom != null && zoneFrom.is(ZoneType.Graveyard);
        boolean wasFacedown = c.isFaceDown();

        // Rule 111.8: A token that has left the battlefield can't move to another zone
        if (!c.isSpell() && c.isToken() && !fromBattlefield && zoneFrom != null && !zoneFrom.is(ZoneType.Stack)
                && (cause == null || !(cause instanceof SpellPermanent || cause.isCastFaceDown()) || !cause.isCastFromPlayEffect())) {
            return c;
        }

        // Rules 304.4, 307.4: instants, sorceries can't enter the battlefield and remain
        // in their previous zone
        if (toBattlefield && (c.isInstant() || c.isSorcery())) {
            return c;
        }

        CardCollectionView lastBattlefield = getLastState(AbilityKey.LastStateBattlefield, cause, params, false);
        CardCollectionView lastGraveyard = getLastState(AbilityKey.LastStateGraveyard, cause, params, false);

        //717.6. If a card with an Astrotorium card back would be put into a zone other than the battlefield, exile,
        //or the command zone from anywhere, instead its owner puts it into the junkyard.
        if ((c.getGamePieceType() == GamePieceType.ATTRACTION || c.getGamePieceType() == GamePieceType.CONTRAPTION)
                && !toBattlefield && !zoneTo.getZoneType().isPartOfCommandZone() && !zoneTo.is(ZoneType.Exile)) {
            //This should technically be a replacement effect, but with the "can apply more than once to the same event"
            //clause, this seems sufficient for now.
            //TODO: Figure out what on earth happens if you animate an attraction, mutate a creature/commander/token onto it, and it dies...
            return moveToJunkyard(c, cause, params);
        }

        if (c.isSplitCard() && toBattlefield && c.getCastSA() == null) {
            // need to set as empty room
            c.updateRooms();
        }

        boolean suppress = !c.isToken() && zoneFrom.equals(zoneTo);
        Card copied = null;
        Card lastKnownInfo = null;

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

        // Don't copy Tokens, copy only cards leaving the battlefield
        // and returning to hand (to recreate their spell ability information)
        if (toBattlefield || (suppress && zoneTo.getZoneType().isHidden())) {
            copied = c;

            if (lastKnownInfo == null) {
                lastKnownInfo = CardCopyService.getLKICopy(c);
            }

            if (!StaticAbilityCountersRemain.countersRemain(lastKnownInfo, zoneTo)) {
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
            if (fromGraveyard) {
                int idx = lastGraveyard.indexOf(c);
                if (idx != -1) {
                    lastKnownInfo = lastGraveyard.get(idx);
                }
            }

            if (lastKnownInfo == null) {
                lastKnownInfo = CardCopyService.getLKICopy(c);
            }

            // LKI is only needed when something is moved from the battlefield.
            // also it does messup with Blink Effects like Eldrazi Displacer
            if (fromBattlefield && !zoneTo.is(ZoneType.Stack) && !zoneTo.is(ZoneType.Flashback)) {
                game.addChangeZoneLKIInfo(lastKnownInfo);
            }

            copied = new CardCopyService(c).copyCard(false);

            copied.setGameTimestamp(c.getGameTimestamp());

            if (zoneTo.is(ZoneType.Stack)) {
                // try not to copy changed stats when moving to stack

                // copy exiled properties when adding to stack
                // will be cleanup later in MagicStack
                copied.setExiledWith(c.getExiledWith());
                copied.setExiledBy(c.getExiledBy());
                copied.setDrawnThisTurn(c.getDrawnThisTurn());

                // CR 707.12 casting of a card copy
                if (c.isRealToken()) {
                    copied.setCopiedPermanent(c.getCopiedPermanent());
                    //TODO: Feels like this should fit here and seems to work but it'll take a fair bit more testing to be sure.
                    //copied.setGamePieceType(GamePieceType.COPIED_SPELL);
                }

                if (cause != null && cause.isSpell() && c.equals(cause.getHostCard())) {
                    copied.setCastFrom(zoneFrom);
                    copied.setCastSA(cause);
                    copied.setSplitStateToPlayAbility(cause);

                    // CR 112.2 A spell’s controller is, by default, the player who put it on the stack.
                    copied.setController(cause.getActivatingPlayer(), 0);

                    KeywordInterface kw = cause.getKeyword();
                    if (kw != null) {
                        copied.addKeywordForStaticAbility(kw);

                        // CR 400.7g If an effect grants a nonland card an ability that allows it to be cast,
                        // that ability will continue to apply to the new object that card became after it moved to the stack as a result of being cast this way.
                        if (!cause.isIntrinsic()) {
                            kw.setHostCard(copied);
                            copied.addChangedCardKeywordsInternal(ImmutableList.of(kw), null, false, copied.getGameTimestamp(), kw.getStatic(), false);
                        }
                    }
                }
            } else {
                // when a card leaves the battlefield, ensure it's in its original state
                copied.setState(CardStateName.Original, false);
                copied.setBackSide(false);
            }

            // need to copy counters when card enters another zone than hand or library
            if (StaticAbilityCountersRemain.countersRemain(lastKnownInfo, zoneTo)) {
                copied.setCounters(Maps.newHashMap(lastKnownInfo.getCounters()));
            }

            // perpetual stuff
            if (c.hasIntensity()) {
                copied.setIntensity(c.getIntensity(false));
            }
            if (c.isSpecialized()) {
                copied.setState(c.getCurrentStateName(), false);
            }
            if (c.hasPerpetual()) {
                copied.setPerpetual(c);
            }
        }

        // ensure that any leftover keyword/type changes are cleared in the state view
        copied.updateStateForView();

        final Card staticEff = setupStaticEffect(copied, cause);

        // Aura entering indirectly
        // need to check before it enters
        if (copied.isAura() && !copied.isAttachedToEntity() && toBattlefield && (zoneFrom == null || !zoneFrom.is(ZoneType.Stack))) {
            boolean found = false;
            if (game.getPlayers().stream().anyMatch(PlayerPredicates.canBeAttached(copied, null))) {
                found = true;
            }

            if (!found) {
                if (lastBattlefield.anyMatch(CardPredicates.canBeAttached(copied, null))) {
                    found = true;
                }
            }

            if (!found) {
                if (lastGraveyard.anyMatch(CardPredicates.canBeAttached(copied, null))) {
                    found = true;
                }
            }
            if (!found) {
                c.clearControllers();
                cleanStaticEffect(staticEff, copied);
                return c;
            }
        }

        GameEntityCounterTable table;
        if (params != null && params.containsKey(AbilityKey.CounterTable)) {
            table = (GameEntityCounterTable) params.get(AbilityKey.CounterTable);
        } else {
            table = new GameEntityCounterTable();
        }

        if (!suppress) {
            // Temporary disable commander replacement effect
            // 903.9a
            if (fromBattlefield && !toBattlefield && c.isCommander() && c.hasMergedCard()) {
                c.getOwner().setCommanderReplacementSuppressed(true);
            }

            Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(copied);
            repParams.put(AbilityKey.CardLKI, lastKnownInfo);
            repParams.put(AbilityKey.Cause, cause);
            repParams.put(AbilityKey.Origin, zoneFrom != null ? zoneFrom.getZoneType() : null);
            repParams.put(AbilityKey.Destination, zoneTo.getZoneType());
            if (toBattlefield) {
                repParams.put(AbilityKey.EffectOnly, true);
                repParams.put(AbilityKey.CounterTable, table);
                repParams.put(AbilityKey.CounterMap, table.column(copied));
            }
            if (params != null) {
                repParams.putAll(params);
            }

            // in addition to actual tokens, cards "made" by digital-only mechanics
            // are also added to inbound tokens so their etb replacements will work
            if (zoneFrom == null || zoneFrom.is(ZoneType.None)) {
                copied.getOwner().addInboundToken(copied);
            }
            ReplacementResult repres = game.getReplacementHandler().run(ReplacementType.Moved, repParams);
            copied.getOwner().removeInboundToken(copied);

            if (repres != ReplacementResult.NotReplaced && repres != ReplacementResult.Updated) {
                // reset failed manifested Cards back to original
                if ((c.isManifested() || c.isCloaked()) && !c.isInPlay()) {
                    c.forceTurnFaceUp();
                }

                if (repres == ReplacementResult.Prevented) {
                    c.clearControllers();
                    cleanStaticEffect(staticEff, copied);
                    if (cause != null) {
                        if (cause.hasParam("Transformed") || cause.hasParam("FaceDown")) {
                            c.setBackSide(false);
                            c.changeToState(CardStateName.Original);
                        }
                        unattachCardLeavingBattlefield(c, c);
                    }

                    if (c.isInZone(ZoneType.Stack) && !zoneTo.is(ZoneType.Graveyard)) {
                        return moveToGraveyard(c, cause, params);
                    }
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
            copied.setGameTimestamp(game.getNextTimestamp());
        }

        // Aura entering as Copy from stack
        // without targets it is sent to graveyard
        if (copied.isAura() && !copied.isAttachedToEntity() && toBattlefield) {
            if (zoneFrom != null && zoneFrom.is(ZoneType.Stack) && game.getStack().isResolving(c)) {
                boolean found = false;
                if (game.getPlayers().stream().anyMatch(PlayerPredicates.canBeAttached(copied, null))) {
                    found = true;
                }
                if (lastBattlefield.anyMatch(CardPredicates.canBeAttached(copied, null))) {
                    found = true;
                }
                if (lastGraveyard.anyMatch(CardPredicates.canBeAttached(copied, null))) {
                    found = true;
                }
                if (!found) {
                    return moveToGraveyard(copied, cause, params);
                }
            }
            attachAuraOnIndirectETB(copied, params);
        }

        // Handle merged permanent here so all replacement effects are already applied.
        CardCollection mergedCards = null;
        if (fromBattlefield && !toBattlefield && c.hasMergedCard()) {
            CardCollection cards = new CardCollection(c.getMergedCards());
            // replace top card with copied card for correct name for human to choose.
            cards.set(cards.indexOf(c), copied);
            // 725.3b
            if (cause != null && zoneTo.getZoneType() == ZoneType.Exile) {
                cards = (CardCollection) cause.getHostCard().getController().getController().orderMoveToZoneList(cards, zoneTo.getZoneType(), cause);
            } else {
                cards = (CardCollection) c.getOwner().getController().orderMoveToZoneList(cards, zoneTo.getZoneType(), cause);
            }
            cards.set(cards.indexOf(copied), c);
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

        if (zoneFrom != null) {
            if (fromBattlefield && game.getCombat() != null) {
                if (!toBattlefield) {
                    game.getCombat().saveLKI(lastKnownInfo);
                }
                game.getCombat().removeFromCombat(c);
            }
            if (zoneFrom.getZoneType().isDeck() && zoneFrom == zoneTo
                    && position.equals(zoneFrom.size()) && position != 0) {
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
                // CR 400.7b Effects from static abilities that grant an ability to a permanent spell that functions on the battlefield
                // continue to apply to the permanent that spell becomes
                Multimap<StaticAbility, KeywordInterface> addKw = MultimapBuilder.hashKeys().arrayListValues().build();
                for (KeywordInterface kw : c.getKeywords(Keyword.OFFSPRING)) {
                    if (!kw.isIntrinsic()) {
                        addKw.put(kw.getStatic(), kw);
                    }
                }
                if (!addKw.isEmpty()) {
                    for (Map.Entry<StaticAbility, Collection<KeywordInterface>> e : addKw.asMap().entrySet()) {
                        copied.addChangedCardKeywordsInternal(e.getValue(), null, false, copied.getGameTimestamp(), e.getKey(), true);
                    }
                }

                // CR 607.2q linked ability can find cards exiled as cost while it was a spell
                copied.addExiledCards(c.getExiledCards());
            }

            if (cause != null && cause.isCraft() && toBattlefield) { // retain cards crafted while ETB transformed
                copied.retainPaidList(cause, "ExiledCards");
            }
        }

        if (mergedCards != null) {
            // Move components of merged permanent here
            // Also handle 723.3e and 903.9a
            boolean wasToken = c.isToken();
            c.getOwner().setCommanderReplacementSuppressed(false);
            // Change zone of original card so components isToken() and isCommander() return correct value
            // when running replacement effects here
            c.setZone(zoneTo);
            for (final Card card : mergedCards) {
                if (card.isRealCommander()) {
                    card.setMoveToCommandZone(true);
                }
                // CR 727.3e & 903.9a
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
                    storeChangesZoneAll(copied, zoneFrom, zoneTo, params);
                    zoneTo.add(copied, position, toBattlefield ? null : lastKnownInfo); // the modified state of the card is also reported here (e.g. for Morbid + Awaken)
                } else {
                    storeChangesZoneAll(card, zoneFrom, zoneTo, params);
                    zoneTo.add(card, position, CardCopyService.getLKICopy(card));
                    card.setState(CardStateName.Original, false);
                    card.setBackSide(false);
                    card.updateStateForView();
                }
                card.setZone(zoneTo);
            }
            copied.clearMergedCards();
        } else {
            if (!suppress) {
                storeChangesZoneAll(copied, zoneFrom, zoneTo, params);
            }
            // "enter the battlefield as a copy" - apply code here
            // but how to query for input here and continue later while the callers assume synchronous result?
            zoneTo.add(copied, position, toBattlefield ? null : lastKnownInfo); // the modified state of the card is also reported here (e.g. for Morbid + Awaken)
            c.setZone(zoneTo);
        }

        if (fromBattlefield) {
            game.addLeftBattlefieldThisTurn(lastKnownInfo);
            // order here is important so it doesn't unattach cards that might have returned from UntilHostLeavesPlay
            unattachCardLeavingBattlefield(copied, c);
            c.runLeavesPlayCommands();

            if (copied.isTapped()) {
                copied.setTapped(false); //untap card after it leaves the battlefield if needed
                game.fireEvent(new GameEventCardTapped(c, false));
            }
        }
        if (fromGraveyard) {
            game.addLeftGraveyardThisTurn(lastKnownInfo);
        }

        if (c.hasMarkedColor()) {
            copied.setMarkedColors(c.getMarkedColors());
        }

        copied.updateStateForView();

        // we don't want always trigger before counters are placed
        game.getTriggerHandler().suppressMode(TriggerType.Always);
        // Need to apply any static effects to produce correct triggers
        checkStaticAbilities();

        // needed for counters + ascend
        if (!suppress && toBattlefield) {
            game.getTriggerHandler().registerActiveTrigger(copied, false);
        }

        // do ETB counters after zone add
        table.replaceCounterEffect(game, null, true, true, params);

        game.getTriggerHandler().clearSuppression(TriggerType.Always);

        // update static abilities after etb counters have been placed
        checkStaticAbilities();

        // CR 603.6b
        if (toBattlefield) {
            zoneTo.saveLKI(copied, lastKnownInfo);
            if (copied.isRoom() && copied.getCastSA() != null) {
                copied.unlockRoom(copied.getCastSA().getActivatingPlayer(), copied.getCastSA().getCardStateName());
            }
        }

        // only now that the LKI preserved it
        if (!zoneTo.is(ZoneType.Stack)) {
            c.cleanupExiledWith();
        }

        // play the change zone sound
        game.fireEvent(new GameEventCardChangeZone(c, zoneFrom, zoneTo));

        game.getTriggerHandler().clearActiveTriggers(copied, null);
        game.getTriggerHandler().registerActiveTrigger(copied, false);

        if (!suppress) {
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(copied);
            runParams.put(AbilityKey.CardLKI, lastKnownInfo);
            runParams.put(AbilityKey.Cause, cause);
            runParams.put(AbilityKey.Origin, zoneFrom != null ? zoneFrom.getZoneType().name() : null);
            runParams.put(AbilityKey.Destination, zoneTo.getZoneType().name());
            runParams.put(AbilityKey.IndividualCostPaymentInstance, game.costPaymentStack.peek());
            runParams.put(AbilityKey.MergedCards, mergedCards);
            if (params != null) {
                runParams.putAll(params);
            }
            game.getTriggerHandler().runTrigger(TriggerType.ChangesZone, runParams, true);
        }

        if (fromBattlefield && !zoneFrom.getPlayer().equals(zoneTo.getPlayer())) {
            final Map<AbilityKey, Object> runParams2 = AbilityKey.mapFromCard(lastKnownInfo);
            runParams2.put(AbilityKey.OriginalController, zoneFrom.getPlayer());
            if (params != null) {
                runParams2.putAll(params);
            }
            game.getTriggerHandler().runTrigger(TriggerType.ChangesController, runParams2, false);
        }

        if (zoneFrom == null) {
            return copied;
        }

        // CR 708.9 reveal face-down card leaving
        if (wasFacedown && (fromBattlefield || (zoneFrom.is(ZoneType.Stack) && !toBattlefield))) {
            Card revealLKI = CardCopyService.getLKICopy(c);
            revealLKI.forceTurnFaceUp();
            reveal(new CardCollection(revealLKI), revealLKI.getOwner(), true, "Face-down card leaves the " + zoneFrom.toString() + ": ");
        }

        if (fromBattlefield) {
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
                unmeld = changeZone(null, zoneTo, unmeld, position, cause, params);
                storeChangesZoneAll(unmeld, zoneFrom, zoneTo, params);
            }
        } else if (toBattlefield) {
            for (Player p : game.getPlayers()) {
                copied.getDamageHistory().setNotAttackedSinceLastUpkeepOf(p);
                copied.getDamageHistory().setNotBlockedSinceLastUpkeepOf(p);
                copied.getDamageHistory().setNotBeenBlockedSinceLastUpkeepOf(p);
            }
        }

        // Cards not on the battlefield / stack should not have controller
        if (!zoneTo.is(ZoneType.Battlefield) && !zoneTo.is(ZoneType.Stack)) {
            copied.clearControllers();
        }

        return copied;
    }

    private Card setupStaticEffect(Card copied, SpellAbility cause) {
        // CR 611.2e
        if (cause == null || !cause.hasParam("StaticEffect") || !copied.isPermanent()) {
            return null;
        }

        final Card source = cause.getHostCard();
        if (cause.hasParam("StaticEffectCheckSVar")) {
            String cmp = cause.getParamOrDefault("StaticEffectSVarCompare", "GE1");
            int lhs = AbilityUtils.calculateAmount(source, cause.getParam("StaticEffectCheckSVar"), cause);
            int rhs = AbilityUtils.calculateAmount(source, cmp.substring(2), cause);
            if (!Expressions.compare(lhs, cmp, rhs)) {
                return null;
            }
        }

        Long timestamp;
        // check if player ordered it manually
        if (cause.hasSVar("StaticEffectTimestamp"))  {
            // TODO the copied card won't have new timestamp yet
            timestamp = Long.parseLong(cause.getSVar("StaticEffectTimestamp"));
        } else {
            // else create default value (or realign)
            timestamp = game.getNextTimestamp();
            cause.setSVar("StaticEffectTimestamp", String.valueOf(timestamp));
        }
        String name = "Static Effect #" + cause.getId();
        // check if this isn't the first card being moved
        Optional<Card> opt = IterableUtil.tryFind(cause.getActivatingPlayer().getZone(ZoneType.Command).getCards(), CardPredicates.nameEquals(name));

        Card eff;
        if (opt.isPresent()) {
            eff = opt.get();
            // update in case player manually ordered
            eff.setLayerTimestamp(timestamp);
        } else {
            // otherwise create effect first
            eff = SpellAbilityEffect.createEffect(cause, source, cause.getActivatingPlayer(), name, source.getImageKey(), timestamp);
            eff.setRenderForUI(false);
            StaticAbility stAb = eff.addStaticAbility(AbilityUtils.getSVar(cause, cause.getParam("StaticEffect")));
            stAb.setActiveZone(EnumSet.of(ZoneType.Command));
            // needed for ETB lookahead like Bronzehide Lion
            stAb.putParam("AffectedZone", "All");
            SpellAbilityEffect.addForgetOnMovedTrigger(eff, "Battlefield");
            eff.getOwner().getZone(ZoneType.Command).add(eff);
        }

        eff.addRemembered(copied);
        // refresh needed for canEnchant checks
        checkStaticAbilities(false, Sets.newHashSet(copied), new CardCollection(copied));
        return eff;
    }
    private void cleanStaticEffect(Card eff, Card copied) {
        if (eff != null) {
            eff.removeRemembered(copied);
            if (!eff.hasRemembered()) {
                exileEffect(eff);
            }
        }
    }

    private void storeChangesZoneAll(Card c, Zone zoneFrom, Zone zoneTo, Map<AbilityKey, Object> params) {
        if (params != null && params.containsKey(AbilityKey.InternalTriggerTable)) {
            ((CardZoneTable) params.get(AbilityKey.InternalTriggerTable)).put(zoneFrom != null ? zoneFrom.getZoneType() : null, zoneTo.getZoneType(), c);
        }
    }

    private static void unattachCardLeavingBattlefield(final Card copied, final Card old) {
        // remove attachments from creatures
        copied.unAttachAllCards(old);

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

    public final Card moveTo(final ZoneType name, final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        return moveTo(name, c, 0, cause, params);
    }

    public final Card moveTo(final ZoneType name, final Card c, final int libPosition, SpellAbility cause, Map<AbilityKey, Object> params) {
        // Call specific functions to set PlayerZone, then move onto moveTo
        try {
            return switch (name) {
                case Hand -> moveToHand(c, cause, params);
                case Library -> moveToLibrary(c, libPosition, cause, params);
                case Battlefield -> moveToPlay(c, c.getController(), cause, params);
                case Graveyard -> moveToGraveyard(c, cause, params);
                case Exile -> !c.canExiledBy(cause, true) ? null : exile(c, cause, params);
                case Stack -> moveToStack(c, cause, params);
                case PlanarDeck, SchemeDeck, AttractionDeck, ContraptionDeck -> moveToVariantDeck(c, name, libPosition, cause, params);
                case Junkyard -> moveToJunkyard(c, cause, params);
                default -> moveTo(c.getOwner().getZone(name), c, cause); // sideboard will also get there
            };
        } catch (Exception e) {
            String msg = "GameAction:moveTo: Exception occurred";

            Breadcrumb bread = new Breadcrumb(msg);
            bread.setData("Card", c.getName());
            bread.setData("SA", cause.toString());
            bread.setData("ZoneType", name.name());
            bread.setData("Player", c.getOwner());
            Sentry.addBreadcrumb(bread);

            throw new RuntimeException("Error in GameAction moveTo " + c.getName() + " to Player Zone " + name.name(), e);
        }
    }

    private Card moveTo(final Zone zoneTo, Card c, Integer position, SpellAbility cause, Map<AbilityKey, Object> params) {
        // Ideally move to should never be called without a prevZone
        // Remove card from Current Zone, if it has one
        final Zone zoneFrom = game.getZoneOf(c);
        // String prevName = prev != null ? prev.getZoneName() : "";

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

            if (stAb.checkMode(StaticAbilityMode.CantBlockBy)) {
                if (!stAb.hasParam("ValidAttacker") || (stAb.hasParam("ValidBlocker") && stAb.getParam("ValidBlocker").equals("Creature.Self"))) {
                    continue;
                }
                for (Card creature : IterableUtil.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.CREATURES)) {
                    if (stAb.matchesValidParam("ValidAttacker", creature)) {
                        creature.updateAbilityTextForView();
                    }
                }
            }
            if (stAb.checkMode(StaticAbilityMode.MinMaxBlocker)) {
                for (Card creature : IterableUtil.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.CREATURES)) {
                    if (stAb.matchesValidParam("ValidCard", creature)) {
                        creature.updateAbilityTextForView();
                    }
                }
            }
        }

        // CR 720.4a Move card in maingame if take card from subgame
        if (zoneFrom != null && zoneFrom.is(ZoneType.Sideboard) && game.getMaingame() != null) {
            Card maingameCard = c.getOwner().getMappingMaingameCard(c);
            if (maingameCard != null) {
                if (maingameCard.getZone().is(ZoneType.Stack)) {
                    game.getMaingame().getStack().remove(maingameCard);
                }
                game.getMaingame().getAction().moveTo(ZoneType.Subgame, maingameCard, null, params);
            }
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

    public final Card moveToJunkyard(Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        final PlayerZone junkyard = c.getOwner().getZone(ZoneType.Junkyard);
        return moveTo(junkyard, c, cause, params);
    }

    public final CardCollection exile(final CardCollection cards, SpellAbility cause, Map<AbilityKey, Object> params) {
        CardCollection result = new CardCollection();
        for (Card card : cards) {
            result.add(exile(card, cause, params));
        }
        return result;
    }
    public final Card exile(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        final Zone origin = c.getZone();
        final PlayerZone removed = c.getOwner().getZone(ZoneType.Exile);
        final Card copied = moveTo(removed, c, cause, params);

        if (c.isImmutable()) {
            return copied;
        }

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
        runParams.put(AbilityKey.Cause, cause);
        if (origin != null) { // is generally null when adding via dev mode
            runParams.put(AbilityKey.Origin, origin.getZoneType().name());
        }
        if (params != null) {
            runParams.putAll(params);
        }
        runParams.put(AbilityKey.CostStack, game.costPaymentStack);
        runParams.put(AbilityKey.IndividualCostPaymentInstance, game.costPaymentStack.peek());

        game.getTriggerHandler().runTrigger(TriggerType.Exiled, runParams, false);

        return copied;
    }

    public final Card exileEffect(final Card effect) {
        return exile(effect, null, null);
    }

    public final void moveToCommand(final Card effect, final SpellAbility sa) {
        moveToCommand(effect, sa, AbilityKey.newMap());
    }
    public final void moveToCommand(final Card effect, final SpellAbility sa, Map<AbilityKey, Object> params) {
        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        moveTo(ZoneType.Command, effect, sa, params);
        effect.updateStateForView();
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
    }

    public void ceaseToExist(Card c, boolean skipTrig) {
        if (c.isInZone(ZoneType.Stack)) {
            game.getStack().remove(c);
        }

        final Zone z = c.getZone();
        // in some corner cases there's no zone yet (copied spell that failed targeting)
        if (z != null) {
            z.remove(c);
            c.setZone(c.getOwner().getZone(ZoneType.None));
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
                lki = CardCopyService.getLKICopy(c);
            }
            game.addChangeZoneLKIInfo(lki);
            // CR 702.26k
            if (lki.isInPlay() && !lki.isPhasedOut()) {
                if (game.getCombat() != null) {
                    game.getCombat().saveLKI(lki);
                    game.getCombat().removeFromCombat(c);
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
        c.handleChangedControllerSprocketReset();

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
                if (!stAb.checkConditions(StaticAbilityMode.Continuous)) {
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

        final Map<StaticAbilityLayer, Set<Card>> affectedPerLayer = Maps.newHashMap();

        // remove old effects
        game.getStaticEffects().clearStaticEffects(affectedCards, affectedPerLayer);

        // search for cards with static abilities
        final FCollection<StaticAbility> staticAbilities = new FCollection<>();
        final CardCollection staticList = new CardCollection();
        Table<StaticAbility, StaticAbility, Set<StaticAbilityLayer>> dependencies = null;
        if (preList.isEmpty()) {
            dependencies = HashBasedTable.create();
        }

        game.forEachCardInGame(new Visitor<>() {
            @Override
            public boolean visit(final Card c) {
                // need to get Card from preList if able
                final Card co = preList.get(c);
                for (StaticAbility stAb : co.getStaticAbilities()) {
                    if (stAb.checkMode(StaticAbilityMode.Continuous) && stAb.zonesCheck()) {
                        staticAbilities.add(stAb);
                    }
                }
                if (!co.getStaticCommandList().isEmpty()) {
                    staticList.add(co);
                }
                for (StaticAbility stAb : co.getHiddenStaticAbilities()) {
                    if (stAb.checkMode(StaticAbilityMode.Continuous) && stAb.zonesCheck()) {
                        staticAbilities.add(stAb);
                    }
                }
                return true;
            }
        }, true);

        staticAbilities.sort(effectOrder);

        final Map<StaticAbility, CardCollectionView> affectedPerAbility = Maps.newHashMap();
        for (final StaticAbilityLayer layer : StaticAbilityLayer.CONTINUOUS_LAYERS) {
            List<StaticAbility> toAdd = Lists.newArrayList();
            List<StaticAbility> staticsForLayer = Lists.newArrayList();
            for (StaticAbility stAb : staticAbilities) {
                if (stAb.getLayers().contains(layer)) {
                    staticsForLayer.add(stAb);
                }
            }

            while (!staticsForLayer.isEmpty()) {
                StaticAbility stAb = staticsForLayer.get(0);
                // dependency with CDA seems unlikely
                if (!stAb.isCharacteristicDefining()) {
                    stAb = findStaticAbilityToApply(layer, staticsForLayer, preList, affectedPerAbility, dependencies);
                }
                staticsForLayer.remove(stAb);
                final CardCollectionView previouslyAffected = affectedPerAbility.get(stAb);
                final CardCollectionView affectedHere;
                if (previouslyAffected == null) {
                    affectedHere = stAb.applyContinuousAbilityBefore(layer, preList);
                    if (affectedHere != null) {
                        affectedPerAbility.put(stAb, affectedHere);
                    }
                } else {
                    // CR 613.6 If an effect starts to apply in one layer and/or sublayer, it will continue to be applied
                    // to the same set of objects in each other applicable layer and/or sublayer,
                    // even if the ability generating the effect is removed during this process.
                    affectedHere = previouslyAffected;
                    stAb.applyContinuousAbility(layer, previouslyAffected);
                }
                if (affectedHere != null) {
                    affectedPerLayer.computeIfAbsent(layer, l -> Sets.newHashSet()).addAll(affectedHere);
                    for (final Card c : affectedHere) {
                        for (final StaticAbility st2 : c.getStaticAbilities()) {
                            if (!staticAbilities.contains(st2) && st2.checkMode(StaticAbilityMode.Continuous) && st2.zonesCheck()) {
                                toAdd.add(st2);
                                CardCollectionView newAffected = st2.applyContinuousAbilityBefore(layer, preList);
                                if (newAffected != null) {
                                    affectedPerLayer.computeIfAbsent(layer, l -> Sets.newHashSet()).addAll(newAffected);
                                }
                            }
                        }
                    }
                }
                // CR 613.8c After each effect is applied, the order of remaining effects is reevaluated
                // and may change if an effect that has not yet been applied becomes
                // dependent on or independent of one or more other effects that have not yet been applied.
            }
            staticAbilities.addAll(toAdd);
            for (Player p : game.getPlayers()) {
                p.afterStaticAbilityLayer(layer);
            }
        }

        for (final CardCollectionView affected : affectedPerAbility.values()) {
            if (affected != null) {
                affected.forEach(affectedCards::add);
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

            game.getView().setDependencies(dependencies);
        }

        CardCollection affectedKeywords = new CardCollection();
        CardCollection affectedPT = new CardCollection();
        if (affectedPerLayer.containsKey(StaticAbilityLayer.TEXT)) {
            affectedPerLayer.get(StaticAbilityLayer.TEXT).forEach(Card::updateNameforView);
            affectedKeywords.addAll(affectedPerLayer.get(StaticAbilityLayer.TEXT));
            affectedPT.addAll(affectedPerLayer.get(StaticAbilityLayer.TEXT));
        }
        if (affectedPerLayer.containsKey(StaticAbilityLayer.TYPE)) {
            affectedPerLayer.get(StaticAbilityLayer.TYPE).forEach(Card::updateTypesForView);
            // setting Basic Land Type case
            affectedKeywords.addAll(affectedPerLayer.get(StaticAbilityLayer.TYPE));
            affectedPT.addAll(affectedPerLayer.get(StaticAbilityLayer.TYPE));
        }
        if (affectedPerLayer.containsKey(StaticAbilityLayer.ABILITIES)) {
            affectedKeywords.addAll(affectedPerLayer.get(StaticAbilityLayer.ABILITIES));
        }
        // Update P/T and type in the view only once after all the cards have been processed, to avoid flickering
        if (affectedPerLayer.containsKey(StaticAbilityLayer.CHARACTERISTIC)) {
            affectedPT.addAll(affectedPerLayer.get(StaticAbilityLayer.CHARACTERISTIC));
        }
        if (affectedPerLayer.containsKey(StaticAbilityLayer.SETPT)) {
            affectedPT.addAll(affectedPerLayer.get(StaticAbilityLayer.SETPT));
        }
        if (affectedPerLayer.containsKey(StaticAbilityLayer.MODIFYPT)) {
            affectedPT.addAll(affectedPerLayer.get(StaticAbilityLayer.MODIFYPT));
        }
        /*
        if (affectedPerLayer.containsKey(StaticAbilityLayer.SWITCHPT)) {
            affectedPT.addAll(affectedPerLayer.get(StaticAbilityLayer.SWITCHPT));
        }
        //*/
        affectedPT.forEach(Card::updatePTforView);
        affectedKeywords.forEach(Card::updateKeywords);

        if (affectedPerLayer.containsKey(StaticAbilityLayer.RULES)) {
            affectedPerLayer.get(StaticAbilityLayer.RULES).forEach(Card::updateNonAbilityTextForView);
        }
        // TODO filter out old copies from zone change

        if (runEvents && !affectedCards.isEmpty()) {
            game.fireEvent(new GameEventCardStatsChanged(affectedCards));
        }
        game.getTracker().unfreeze();
    }

    private StaticAbility findStaticAbilityToApply(StaticAbilityLayer layer, List<StaticAbility> staticsForLayer, CardCollectionView preList, Map<StaticAbility, CardCollectionView> affectedPerAbility,
                                                   Table<StaticAbility, StaticAbility, Set<StaticAbilityLayer>> dependencies) {
        StaticAbility first = staticsForLayer.get(0);
        if (staticsForLayer.size() == 1) {
            return first;
        }
        if (!StaticAbilityLayer.CONTINUOUS_LAYERS_WITH_DEPENDENCY.contains(layer)) {
            return first;
        }
        // CR 611.2c continuous effects from resolved abilities always affect the same objects the same way
        Predicate<StaticAbility> isResolved = stAb -> stAb.getHostCard().isImmutable() && !stAb.getHostCard().isEmblem();
        if (isResolved.test(first)) {
            return first;
        }

        DefaultDirectedGraph<StaticAbility, DefaultEdge> dependencyGraph = new DefaultDirectedGraph<>(DefaultEdge.class);

        for (StaticAbility stAb : staticsForLayer) {
            dependencyGraph.addVertex(stAb);

            if (isResolved.test(stAb)) {
                continue;
            }

            boolean exists = stAb.getHostCard().getStaticAbilities().contains(stAb);
            boolean compareAffected = false;
            CardCollectionView affectedHere = affectedPerAbility.get(stAb);
            if (affectedHere == null) {
                affectedHere = StaticAbilityContinuous.getAffectedCards(stAb, preList);
                compareAffected = true;
            }
            Iterable<Object> effectResults = generateContinuousEffectChanges(layer, stAb);

            for (StaticAbility otherStAb : staticsForLayer) {
                if (stAb == otherStAb) {
                    continue;
                }

                boolean removeFull = true;
                CardCollectionView affectedOther = affectedPerAbility.get(otherStAb);
                if (affectedOther == null) {
                    affectedOther = otherStAb.applyContinuousAbilityBefore(layer, preList);
                    if (affectedOther == null) {
                        // ability was removed
                        continue;
                    }
                } else {
                    removeFull = false;
                    otherStAb.applyContinuousAbility(layer, affectedOther);
                }

                // CR 613.8a An effect is said to "depend on" another if
                // * (a) + (c) already handled *
                // (b) applying the other would change the text or the existence of the first effect...
                boolean dependency = exists != stAb.getHostCard().getStaticAbilities().contains(stAb);
                // ...what it applies to...
                if (!dependency && compareAffected) {
                    CardCollectionView affectedAfterOther = StaticAbilityContinuous.getAffectedCards(stAb, preList);
                    dependency = !Iterators.elementsEqual(affectedHere.iterator(), affectedAfterOther.iterator());
                }
                // ...or what it does to any of the things it applies to
                if (!dependency) {
                    Iterable<Object> effectResultsAfterOther = generateContinuousEffectChanges(layer, stAb);
                    dependency = !effectResults.equals(effectResultsAfterOther);
                }

                if (dependency) {
                    dependencyGraph.addVertex(otherStAb);
                    dependencyGraph.addEdge(stAb, otherStAb);
                    if (dependencies != null) {
                        if (dependencies.contains(stAb, otherStAb)) {
                            dependencies.get(stAb, otherStAb).add(layer);
                        } else {
                            dependencies.put(stAb, otherStAb, EnumSet.of(layer));
                        }
                    }
                }

                // undo changes and check next pair
                game.getStaticEffects().removeStaticEffect(otherStAb, layer, removeFull);
            }
            // when lucky the effect with the earliest timestamp has no dependency
            // then we can safely return it - otherwise we need to build the whole graph
            // because it might still be part of a loop
            if (dependencyGraph.edgeSet().isEmpty() && stAb == first) {
                return stAb;
            }
        }

        // CR 613.8b If several dependent effects form a dependency loop, then this rule is ignored
        List<List<StaticAbility>> cycles = new SzwarcfiterLauerSimpleCycles<>(dependencyGraph).findSimpleCycles();
        for (List<StaticAbility> cyc : cycles) {
            for (int i = 0 ; i < cyc.size() - 1 ; i++) {
                dependencyGraph.removeEdge(cyc.get(i), cyc.get(i + 1));
            }
            // remove final edge
            dependencyGraph.removeEdge(cyc.get(cyc.size() - 1), cyc.get(0));
        }

        // remove all effects that are still dependent on another
        Set<StaticAbility> toRemove = Sets.newHashSet();
        for (StaticAbility stAb : dependencyGraph.vertexSet()) {
            if (dependencyGraph.outDegreeOf(stAb) > 0) {
                toRemove.add(stAb);
            }
        }
        dependencyGraph.removeAllVertices(toRemove);

        // now the earliest one left is the correct choice
        List<StaticAbility> statics = Lists.newArrayList(dependencyGraph.vertexSet());
        statics.sort(Comparator.comparing(StaticAbility::getTimestamp));

        return statics.get(0);
    }

    private Iterable<Object> generateContinuousEffectChanges(StaticAbilityLayer layer, StaticAbility stAb) {
        List<Object> result = Collections.emptyList();
        if (layer == StaticAbilityLayer.CONTROL) {
            result = Lists.newArrayList();
            result.addAll(AbilityUtils.getDefinedPlayers(stAb.getHostCard(), stAb.getParam("GainControl"), stAb));
        }
        return result;
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

        for (int q = 0; q < 9; q++) {
            boolean checkAgain = false;
            CardCollection cardsToUpdateLKI = new CardCollection();

            checkStaticAbilities(false, affectedCards, CardCollection.EMPTY);

            CardZoneTable table = new CardZoneTable(game.getLastStateBattlefield(), game.getLastStateGraveyard());
            Map<AbilityKey, Object> mapParams = AbilityKey.newMap();
            AbilityKey.addCardZoneTableParams(mapParams, table);

            for (final Player p : game.getPlayers()) {
                p.checkKeywordCard();

                for (final ZoneType zt : ZoneType.values()) {
                    if (zt == ZoneType.Battlefield) {
                        continue;
                    }
                    for (final Card c : p.getCardsIn(zt).threadSafeIterable()) {
                        checkAgain |= stateBasedAction704_5d(c);
                        if (zt == ZoneType.Command) {
                            // Dungeon Card won't affect other cards, so don't need to set checkAgain
                            stateBasedAction_Dungeon(c);
                            stateBasedAction_Scheme(c);
                        }
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
                checkAgainCard |= stateBasedAction704_attach(c, unAttachList);
                checkAgainCard |= stateBasedAction_Contraption(c, noRegCreats);

                checkAgainCard |= stateBasedAction704_5q(c); // annihilate +1/+1 counters with -1/-1 ones

                checkAgainCard |= stateBasedAction704_5r(c);

                if (c.hasKeyword("The number of loyalty counters on CARDNAME is equal to the number of Beebles you control.")) {
                    int beeble = CardLists.getValidCardCount(game.getCardsIn(ZoneType.Battlefield), "Beeble.YouCtrl", c.getController(), c, null);
                    int loyal = c.getCounters(CounterEnumType.LOYALTY);
                    if (loyal < beeble) {
                        GameEntityCounterTable counterTable = new GameEntityCounterTable();
                        c.addCounter(CounterEnumType.LOYALTY, beeble - loyal, c.getController(), counterTable);
                        counterTable.replaceCounterEffect(game, null, false);
                    } else if (loyal > beeble) {
                        c.subtractCounter(CounterEnumType.LOYALTY, loyal - beeble, null);
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
                checkAgain |= handleLegendRule(p, noRegCreats);

                if ((game.getRules().hasAppliedVariant(GameType.Commander)
                        || game.getRules().hasAppliedVariant(GameType.Brawl)
                        || game.getRules().hasAppliedVariant(GameType.Planeswalker)) && !checkAgain) {
                    for (final Card c : p.getCardsIn(ZoneType.Graveyard).threadSafeIterable()) {
                        checkAgain |= stateBasedAction_Commander(c, mapParams);
                    }
                    for (final Card c : p.getCardsIn(ZoneType.Exile).threadSafeIterable()) {
                        checkAgain |= stateBasedAction_Commander(c, mapParams);
                    }
                }

                // 704.5z If a player controls a permanent with start your engines! and that player has no speed, that player’s speed becomes 1.
                if (p.getSpeed() == 0 && p.getCardsIn(ZoneType.Battlefield).anyMatch(c -> c.hasKeyword(Keyword.START_YOUR_ENGINES))) {
                    p.increaseSpeed();
                    checkAgain = true;
                }

                checkAgain |= handlePlaneswalkerRule(p, noRegCreats);
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
                sacrificeDestroy(c, null, mapParams);
            }

            if (desCreats != null) {
                if (desCreats.size() > 1 && !orderedDesCreats) {
                    desCreats = CardLists.filter(desCreats, Card::canBeDestroyed);
                    if (!desCreats.isEmpty()) {
                        desCreats = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, desCreats, ZoneType.Graveyard, null);
                    }
                    orderedDesCreats = true;
                }
                for (Card c : desCreats) {
                    destroy(c, null, true, mapParams);
                }
            }

            if (sacrificeList.size() > 1 && !orderedSacrificeList) {
                sacrificeList = (CardCollection) GameActionUtil.orderCardsByTheirOwners(game, sacrificeList, ZoneType.Graveyard, null);
                orderedSacrificeList = true;
            }
            sacrifice(sacrificeList, null, true, mapParams);

            setHoldCheckingStaticAbilities(false);

            table.triggerChangesZoneAll(game, null);

            // important to collect first otherwise if a static fires it will mess up registered ones from LKI
            game.getTriggerHandler().collectTriggerForWaiting();
            if (game.getTriggerHandler().runWaitingTriggers()) {
                checkAgain = true;
            }

            if (game.getCombat() != null) {
                game.getCombat().removeAbsentCombatants();
            }

            for (final Card c : cardsToUpdateLKI) {
                game.updateLastStateForCard(c);
            }

            if (checkAgain) {
                performedSBA = true;
            } else {
                break; // do not continue the loop
            }
        } // for q=0;q<9

        game.getTracker().unfreeze();

        if (runEvents && !affectedCards.isEmpty()) {
            game.fireEvent(new GameEventCardStatsChanged(affectedCards));
        }

        // recheck the game over condition at this point to make sure no other win conditions apply now.
        checkGameOverCondition();

        if (game.getAge() != GameStage.Play) {
            return false;
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

        // Run all commands that are queued to run after state based actions are checked
        game.runSBACheckedCommands();

        return performedSBA;
    }

    private boolean stateBasedAction_Saga(Card c, CardCollection sacrificeList) {
        boolean checkAgain = false;
        if (!c.isSaga() || !c.hasChapter()) {
            return false;
        }
        // needs to be effect, because otherwise it might be a cost?
        if (!c.canBeSacrificedBy(null, true)) {
            return false;
        }
        if (c.getCounters(CounterEnumType.LORE) < c.getFinalChapterNr()) {
            return false;
        }
        if (!game.getStack().hasSourceOnStack(c, SpellAbility::isChapter)) {
            sacrificeList.add(c);
            checkAgain = true;
        }
        return checkAgain;
    }

    private boolean stateBasedAction_Battle(Card c, CardCollection removeList) {
        boolean checkAgain = false;
        if (!c.isBattle()) {
            return checkAgain;
        }
        Player battleController = c.getController();
        Player battleProtector = c.getProtectingPlayer();
        /*
         704.5w If a battle has no player in the game designated as its protector and no attacking creatures are currently
         attacking that battle, that battle’s controller chooses an appropriate player to be its protector based on its
         battle type. If no player can be chosen this way, the battle is put into its owner’s graveyard.

         704.5x If a Siege’s controller is also its designated protector, that player chooses an opponent to become its
         protector. If no player can be chosen this way, the battle is put into its owner’s graveyard.
         */
        if (((battleProtector == null || !battleProtector.isInGame()) &&
                (game.getCombat() == null || game.getCombat().getAttackersOf(c).isEmpty())) ||
                (c.getType().hasStringType("Siege") && battleController.equals(battleProtector))) {
            Player newProtector;
            if (c.getType().getBattleTypes().contains("Siege"))
                newProtector = battleController.getController().chooseSingleEntityForEffect(battleController.getOpponents(), new SpellAbility.EmptySa(ApiType.ChoosePlayer, c), "Choose an opponent to protect this battle", null);
            else {
                // Fall back to the controller. Technically should fall back to null per the above rules, but no official
                // cards should use this branch. For now this better supports custom cards. May need to revise this later.
                newProtector = battleController;
            }
            // seems unlikely unless range of influence gets implemented
            if (newProtector == null) {
                removeList.add(c);
            } else {
                c.setProtectingPlayer(newProtector);
            }
            checkAgain = true;
        }
        if (c.getCounters(CounterEnumType.DEFENSE) > 0) {
            return checkAgain;
        }
        // 704.5v If a battle has defense 0 and it isn't the source of an ability that has triggered but not yet left the stack,
        // it’s put into its owner’s graveyard.
        if (!game.getStack().hasSourceOnStack(c, SpellAbility::isTrigger)) {
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
            rolesByPlayer.sort(CardPredicates.compareByGameTimestamp());
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

    private void stateBasedAction_Scheme(Card c) {
        if (!c.isScheme() || c.getType().hasSupertype(Supertype.Ongoing)) {
            return;
        }
        if (!game.getStack().hasSourceOnStack(c, null)) {
            moveTo(ZoneType.SchemeDeck, c, -1, null, AbilityKey.newMap());
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

    private boolean stateBasedAction_Contraption(Card c, CardCollection removeList) {
        if (!c.isContraption())
            return false;
        int currentSprocket = c.getSprocket();

        //A contraption that is in the battlefield without being assembled is put into the graveyard or junkyard.
        if (currentSprocket == 0) {
            removeList.add(c);
            return true;
        }

        //An assembled contraption that changes controller is reassembled onto a sprocket by its new controller.
        //A reassemble effect can handle that on its own. But if it changed controller due to some other effect,
        //we assign it here. A contraption uses sprocket -1 to signify it has been assembled previously but now needs
        //a new sprocket.
        if (currentSprocket > 0 && currentSprocket <= 3)
            return false;

        int sprocket = c.getController().getController().chooseSprocket(c);
        c.setSprocket(sprocket);
        return true;
    }

    private boolean stateBasedAction704_5u(Player p) {
        boolean checkAgain = false;

        CardCollection toAssign = new CardCollection();

        for (final Card c : p.getCreaturesInPlay().threadSafeIterable()) {
            if (!c.hasSector()) {
                toAssign.add(c);
                checkAgain = true;
            }
        }

        final StringBuilder sb = new StringBuilder();
        for (Card assignee : toAssign) { // probably would be nice for players to pick order of assigning?
            String sector = p.getController().chooseSector(assignee, "Assign");
            assignee.assignSector(sector);
            if (sb.length() == 0) {
                sb.append(p).append(" ").append(Localizer.getInstance().getMessage("lblAssigns")).append("\n");
            }
            String creature = assignee.getTranslatedName() + " (" + assignee.getId() + ")";
            sb.append(creature).append(" ").append(sector).append("\n");
        }
        if (sb.length() > 0) {
            notifyOfValue(null, p, sb.toString(), p);
        }

        return checkAgain;
    }

    private boolean stateBasedAction_Commander(Card c, Map<AbilityKey, Object> mapParams) {
        // CR 903.9a
        if (c.isRealCommander() && c.canMoveToCommandZone()) {
            // FIXME: need to flush the tracker to make sure the Commander is properly updated
            game.getTracker().flush();

            c.setMoveToCommandZone(false);
            if (c.getOwner().getController().confirmAction(c.getFirstSpellAbility(), PlayerActionConfirmMode.ChangeZoneToAltDestination, c.getDisplayName() + ": If a commander is in a graveyard or in exile and that card was put into that zone since the last time state-based actions were checked, its owner may put it into the command zone.", null)) {
                moveTo(c.getOwner().getZone(ZoneType.Command), c, null, mapParams);
                return true;
            }
        }
        return false;
    }

    private boolean stateBasedAction704_5q(Card c) {
        boolean checkAgain = false;
        final CounterType p1p1 = CounterEnumType.P1P1;
        final CounterType m1m1 = CounterEnumType.M1M1;
        int plusOneCounters = c.getCounters(p1p1);
        int minusOneCounters = c.getCounters(m1m1);
        if (plusOneCounters > 0 && minusOneCounters > 0) {
            if (!c.canRemoveCounters(p1p1) || !c.canRemoveCounters(m1m1)) {
                return checkAgain;
            }

            int remove = Math.min(plusOneCounters, minusOneCounters);
            // If a permanent has both a +1/+1 counter and a -1/-1 counter on it,
            // N +1/+1 and N -1/-1 counters are removed from it, where N is the
            // smaller of the number of +1/+1 and -1/-1 counters on it.
            // This should fire remove counters trigger
            c.subtractCounter(p1p1, remove, null);
            c.subtractCounter(m1m1, remove, null);
            checkAgain = true;
        }
        return checkAgain;
    }
    private boolean stateBasedAction704_5r(Card c) {
        final CounterType dreamType = CounterEnumType.DREAM;

        int old = c.getCounters(dreamType);
        if (old <= 0) {
            return false;
        }
        Integer max = c.getCounterMax(dreamType);
        if (max == null) {
            return false;
        }
        if (old > max) {
            if (!c.canRemoveCounters(dreamType)) {
                return false;
            }
            c.subtractCounter(dreamType,  old - max, null);
            return true;
        }
        return false;
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
        if (game.isGameOver()) {
            return;
        }

        // award loses as SBE
        GameEndReason reason = null;
        List<Player> losers = null;
        FCollectionView<Player> allPlayers = game.getPlayers();

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

        if (reason == null) {
            for (Player p : allPlayers) {
                if (p.checkLoseCondition()) { // this will set appropriate outcomes
                    if (losers == null) {
                        losers = Lists.newArrayListWithCapacity(3);
                    }
                    losers.add(p);
                }
            }
        }

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
        CardCollection nonLegendaryNames = CardLists.filter(a, Card::hasNonLegendaryCreatureNames);

        Multimap<String, Card> uniqueLegends = Multimaps.index(a, Card::getName);
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
        final List<Card> worlds = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), c -> c.getType().hasSupertype(Supertype.World));
        if (worlds.size() <= 1) {
            return false;
        }

        List<Card> toKeep = Lists.newArrayList();
        long ts = 0;

        for (final Card crd : worlds) {
            long crdTs = crd.getWorldTimestamp();
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

    public final CardCollection sacrifice(final Iterable<Card> list, final SpellAbility source, final boolean effect, Map<AbilityKey, Object> params) {
        Multimap<Player, Card> lki = MultimapBuilder.hashKeys().arrayListValues().build();
        final boolean showRevealDialog = source != null && source.hasParam("ShowSacrificedCards");

        CardCollection result = new CardCollection();
        for (Card c : list) {
            if (c == null) {
                continue;
            }

            if (!c.canBeSacrificedBy(source, effect)) {
                continue;
            }

            Card lkiCopy = ((CardCollection) params.get(AbilityKey.LastStateBattlefield)).get(c);
            c.getController().addSacrificedThisTurn(lkiCopy, source);
            lki.put(c.getController(), lkiCopy);

            c.updateWasDestroyed(true);

            Card changed = sacrificeDestroy(c, source, params);
            if (changed != null) {
                result.add(changed);
            }
            if (showRevealDialog) {
                final String message = Localizer.getInstance().getMessage("lblSacrifice");
                reveal(result, ZoneType.Graveyard, c.getOwner(), false, message, false);
            }
        }
        for (Map.Entry<Player, Collection<Card>> e : lki.asMap().entrySet()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(e.getKey());
            runParams.put(AbilityKey.Cards, new CardCollection(e.getValue()));
            runParams.put(AbilityKey.Cause, source);
            game.getTriggerHandler().runTrigger(TriggerType.SacrificedOnce, runParams, false);
        }
        return result;
    }

    public final boolean destroy(final Card c, final SpellAbility sa, final boolean regenerate, Map<AbilityKey, Object> params) {
        if (!c.canBeDestroyed()) {
            return false;
        }

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

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(c);
        runParams.put(AbilityKey.Causer, activator);
        if (params != null) {
            runParams.putAll(params);
        }
        game.getTriggerHandler().runTrigger(TriggerType.Destroyed, runParams, false);

        final Card sacrificed = sacrificeDestroy(c, sa, params);
        return sacrificed != null;
    }

    /**
     * @return the sacrificed Card in its new location, or {@code null} if the
     * sacrifice wasn't successful.
     */
    protected final Card sacrificeDestroy(final Card c, SpellAbility cause, Map<AbilityKey, Object> params) {
        if (!c.isInPlay()) {
            return null;
        }

        final Card newCard = moveToGraveyard(c, cause, params);

        return newCard;
    }

    public void revealTo(final Card card, final Player to) {
        revealTo(card, Collections.singleton(to));
    }
    public void revealTo(final CardCollectionView cards, final Player to) {
        revealTo(cards, to, null);
    }
    public void revealTo(final CardCollectionView cards, final Player to, String messagePrefix) {
        revealTo(cards, Collections.singleton(to), messagePrefix, true);
    }
    public void revealTo(final Card card, final Iterable<Player> to) {
        revealTo(new CardCollection(card), to);
    }
    public void revealTo(final CardCollectionView cards, final Iterable<Player> to) {
        revealTo(cards, to, null, true);
    }
    public void revealTo(final CardCollectionView cards, final Iterable<Player> to, String messagePrefix, boolean addSuffix) {
        if (cards.isEmpty()) {
            return;
        }

        final ZoneType zone = cards.getFirst().getZone().getZoneType();
        final Player owner = cards.getFirst().getOwner();
        for (final Player p : to) {
            p.getController().reveal(cards, zone, owner, messagePrefix, addSuffix);
        }
    }

    public void reveal(CardCollectionView cards, Player cardOwner) {
        reveal(cards, cardOwner, true);
    }
    public void reveal(CardCollectionView cards, Player cardOwner, boolean dontRevealToOwner) {
        reveal(cards, cardOwner, dontRevealToOwner, null);
    }
    public void reveal(CardCollectionView cards, Player cardOwner, boolean dontRevealToOwner, String messagePrefix) {
        reveal(cards, cardOwner, dontRevealToOwner, messagePrefix, true);
    }
    public void reveal(CardCollectionView cards, Player cardOwner, boolean dontRevealToOwner, String messagePrefix, boolean msgAddSuffix) {
        Card firstCard = Iterables.getFirst(cards, null);
        if (firstCard == null) {
            return;
        }
        reveal(cards, game.getZoneOf(firstCard).getZoneType(), cardOwner, dontRevealToOwner, messagePrefix, msgAddSuffix);
    }

    public void reveal(CardCollectionView cards, ZoneType zt, Player cardOwner, boolean dontRevealToOwner, String messagePrefix) {
        reveal(cards, zt, cardOwner, dontRevealToOwner, messagePrefix, true);
    }
    public void reveal(CardCollectionView cards, ZoneType zt, Player cardOwner, boolean dontRevealToOwner, String messagePrefix, boolean msgAddSuffix) {
        for (Player p : game.getPlayers()) {
            if (dontRevealToOwner && cardOwner == p) {
                continue;
            }
            p.getController().reveal(cards, zt, cardOwner, messagePrefix, msgAddSuffix);
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

    public void revealUnsupported(Map<Player, List<PaperCard>> unsupported) {
        // Notify players
        for (Player p : game.getPlayers()) {
            p.getController().revealUnsupported(unsupported);
        }
    }

    /** Delivers a message to all players. (use reveal to show Cards) */
    public void notifyOfValue(SpellAbility saSource, GameObject relatedTarget, String value, Player playerExcept) {
        if (saSource != null) {
            String name = saSource.getHostCard().getTranslatedName();
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
        return ((float) landCount) / ((float) deck.size());
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
                // Choose starting hand for each player with multiple hands
                if (StaticData.instance().getFilteredHandsEnabled() ) {
                    drawStartingHand(p1);
                } else {
                    p1.drawCards(p1.getStartingHandSize());
                }

                BackupPlanService backupPlans = new BackupPlanService(p1);
                if (backupPlans.initializeExtraHands()) {
                    backupPlans.chooseHand();
                }
            }

            if (game.getRules().getGameType() != GameType.Puzzle) {
                new MulliganService(first).perform();
            }
            if (game.isGameOver()) { break; } // conceded during "mulligan" prompt

            game.setAge(GameStage.Play);

            //<THIS CODE WILL WORK WITH PHASE = NULL>
            if (game.getRules().hasAppliedVariant(GameType.Planechase)) {
                first.initPlane();
                for (final Player p1 : game.getPlayers()) {
                    p1.createPlanechaseEffects(game);
                }
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
            // No one of them has lost, so cannot decide who goes first .
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
            List<Card> ploys = CardLists.filter(takesAction.getCardsIn(ZoneType.Command), input -> input.getName().equals("Emissary's Ploy"));
            CardCollectionView all = CardLists.filterControlledBy(game.getCardsInGame(), takesAction);
            List<Card> spires = CardLists.filter(all, input -> input.getName().equals("Cryptic Spires"));

            int chosen = 1;
            List<Integer> cmc = Lists.newArrayList(1, 2, 3);

            for (Card c : ploys) {
                if (!cmc.isEmpty()) {
                    SpellAbility sa = new SpellAbility.EmptySa(ApiType.ChooseNumber, c, takesAction);
                    chosen = takesAction.getController().chooseNumber(sa, "Emissary's Ploy", cmc, c.getOwner());
                    cmc.remove((Object)chosen);
                }

                c.setChosenNumber(chosen);
            }
            for (Card c : spires) {
                // TODO: only do this for the AI, for the player part, get the encoded color from the deck file and pass
                //  it to either player or the papercard object so it feels like rule based for the player side..
                if (!c.hasMarkedColor()) {
                    if (takesAction.isAI()) {
                        String prompt = c.getTranslatedName() + ": " +
                                Localizer.getInstance().getMessage("lblChooseNColors", Lang.getNumeral(2));
                        SpellAbility sa = new SpellAbility.EmptySa(ApiType.ChooseColor, c, takesAction);
                        sa.putParam("AILogic", "MostProminentInComputerDeck");
                        c.setMarkedColors(takesAction.getController().chooseColors(prompt, sa, 2, 2, ColorSet.WUBRG));
                    }
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
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(p);
        game.getTriggerHandler().runTrigger(TriggerType.TakesInitiative, runParams, false);
    }

    public void scry(final List<Player> players, int numScry, SpellAbility cause) {
        if (numScry <= 0) {
            // CR 701.22b If a player is instructed to scry 0, no scry event occurs.
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

                // no real need to separate out the look if there is only one player scrying
                if (players.size() > 1) {
                    revealTo(p.getCardsIn(ZoneType.Library, playerScry), p);
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
                final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(p);
                runParams.put(AbilityKey.ScryNum, numLookedAt);
                runParams.put(AbilityKey.ScryBottom, toBottom == null ? 0 : toBottom.size());
                game.getTriggerHandler().runTrigger(TriggerType.Scry, runParams, false);
            }
        }
    }

    public CardCollection mill(final PlayerCollection millers, final int numCards, final ZoneType destination, final SpellAbility sa, final Map<AbilityKey, Object> moveParams) {
        final boolean reveal = sa != null && !sa.hasParam("NoReveal");
        final boolean showRevealDialog = sa != null && sa.hasParam("ShowMilledCards");

        final CardCollection milled = new CardCollection();

        for (final Player p : millers) {
            if (!p.isInGame()) {
                continue;
            }

            final CardCollectionView milledPlayer = p.mill(numCards, destination, sa, moveParams);
            milled.addAll(milledPlayer);

            // Reveal the milled cards, so players don't have to manually inspect the
            // graveyard to figure out which ones were milled.
            if (reveal) { // do not reveal when exiling face down
                String toZoneStr = destination.equals(ZoneType.Graveyard) ? "" : " (" +
                        Localizer.getInstance().getMessage("lblMilledToZone", destination.getTranslatedName()) + ")";
                if (showRevealDialog) {
                    final String message = Localizer.getInstance().getMessage("lblMilledCards");
                    final boolean addSuffix = !toZoneStr.isEmpty();
                    reveal(milledPlayer, destination, p, false, message, addSuffix);
                }
                game.getGameLog().add(GameLogEntryType.ZONE_CHANGE, p + " milled " +
                        Lang.joinHomogenous(milledPlayer) + toZoneStr + ".");
            }
        }

        if (!milled.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Cards, milled);
            game.getTriggerHandler().runTrigger(TriggerType.MilledAll, runParams, false);
        }

        return milled;
    }

    public void dealDamage(final boolean isCombat, final CardDamageMap damageMap, final CardDamageMap preventMap,
                           final GameEntityCounterTable counterTable, final SpellAbility cause) {
        // Clear assigned damage if is combat
        if (isCombat) {
            for (Map.Entry<GameEntity, Map<Card, Integer>> et : damageMap.columnMap().entrySet()) {
                final GameEntity ge = et.getKey();
                if (ge instanceof Card c) {
                    c.clearAssignedDamage();
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

                if (e.getKey() instanceof Card c && !lethalDamage.containsKey(c)) {
                    lethalDamage.put(c, c.getExcessDamageValue(false));
                }

                e.setValue(e.getKey().addDamageAfterPrevention(e.getValue(), sourceLKI, cause, isCombat, counterTable));
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
    private boolean attachAuraOnIndirectETB(final Card source, Map<AbilityKey, Object> params) {
        // When an Aura ETB without being cast you can choose a valid card to attach it to
        if (!source.hasKeyword(Keyword.ENCHANT)) {
            return false;
        }

        SpellAbility aura = source.getCurrentState().getAuraSpell();
        if (aura == null) {
            return false;
        }
        aura.setActivatingPlayer(source.getController());

        Set<ZoneType> zones = EnumSet.noneOf(ZoneType.class);
        boolean canTargetPlayer = false;
        for (KeywordInterface ki : source.getKeywords(Keyword.ENCHANT)) {
            String o = ki.getOriginal();
            String m[] = o.split(":");
            String v = m[1];
            if (v.contains("inZone")) { // currently the only other zone is Graveyard
                zones.add(ZoneType.Graveyard);
            } else {
                zones.add(ZoneType.Battlefield);
            }
            if (v.startsWith("Player") || v.startsWith("Opponent")) {
                canTargetPlayer = true;
            }
        }
        Player p = source.getController();
        if (canTargetPlayer) {
            final FCollection<Player> players = game.getPlayers().filter(PlayerPredicates.canBeAttached(source, null));

            final Player pa = p.getController().chooseSingleEntityForEffect(players, aura,
                    Localizer.getInstance().getMessage("lblSelectAPlayerAttachSourceTo", source.getTranslatedName()), null);
            if (pa != null) {
                source.attachToEntity(pa, null, true);
                return true;
            }
        } else {
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

            list = CardLists.filter(list, CardPredicates.canBeAttached(source, null));
            if (list.isEmpty()) {
                return false;
            }

            final Card o = p.getController().chooseSingleEntityForEffect(list, aura,
                    Localizer.getInstance().getMessage("lblSelectACardAttachSourceTo", source.getTranslatedName()), null);
            if (o != null) {
                source.attachToEntity(game.getCardState(o), null, true);
                return true;
            }
        }
        return false;
    }

    public CardCollectionView getLastState(final AbilityKey key, final SpellAbility cause, final Map<AbilityKey, Object> params, final boolean refreshIfEmpty) {
        CardCollectionView lastState = null;
        if (params != null) {
            lastState = (CardCollectionView) params.get(key);
        }
        if (lastState == null && cause != null) {
            // inside RE
            if (key == AbilityKey.LastStateBattlefield) {
                lastState = cause.getLastStateBattlefield();
            }
            if (key == AbilityKey.LastStateGraveyard) {
                lastState = cause.getLastStateGraveyard();
            }
        }
        if (lastState == null) {
            // this fallback should be rare unless called when creating a new CardZoneTable
            if (key == AbilityKey.LastStateBattlefield) {
                if (refreshIfEmpty) {
                    lastState = game.copyLastStateBattlefield();
                } else {
                    lastState = game.getLastStateBattlefield();
                }
            }
            if (key == AbilityKey.LastStateGraveyard) {
                if (refreshIfEmpty) {
                    lastState = game.copyLastStateGraveyard();
                } else {
                    lastState = game.getLastStateGraveyard();
                }
            }
        }
        return lastState;
    }
}
