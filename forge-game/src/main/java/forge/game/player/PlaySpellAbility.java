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
package forge.game.player;

import com.google.common.collect.Iterables;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.*;
import forge.game.cost.*;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.mana.ManaPool;
import forge.game.mana.ManaRefundService;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityManaConvert;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * SpellAbility_Requirements class.
 * </p>
 *
 * @author Forge
 * @version $Id: HumanPlaySpellAbility.java 24317 2014-01-17 08:32:39Z Max mtg $
 */
public class PlaySpellAbility {
    private final PlayerController controller;
    private SpellAbility ability;
    private boolean needX = true;

    public PlaySpellAbility(final PlayerController controller, final SpellAbility ability) {
        this.controller = controller;
        this.ability = ability;
    }

    /**
     * <p>
     * playSpellAbility.
     * </p>
     *
     * @param sa
     *            a {@link SpellAbility} object.
     */
    public static boolean playSpellAbility(final PlayerController controller, final Player p, SpellAbility sa) {
        //FThreads.assertExecutedByEdt(false); //TODO: Find a new home for this.

        // Should I be storing state here? It should be the same as last stored state though?

        Card source = sa.getHostCard();
        sa.setActivatingPlayer(p);

        if (sa.isLandAbility()) {
            if (sa.canPlay()) {
                sa.resolve();
            }
            return true;
        }

        boolean castFaceDown = sa.isCastFaceDown();
        boolean flippedToCast = sa.isSpell() && source.isFaceDown();

        sa = chooseOptionalAdditionalCosts(p, sa);
        if (sa == null) {
            return false;
        }

        final CardStateName oldState = source.getCurrentStateName();
        source.setSplitStateToPlayAbility(sa);

        // extra play check
        if (sa.isSpell() && !sa.canPlay()) {
            // in case human won't pay optional cost
            if (source.getCurrentStateName() != oldState) {
                source.setState(oldState, true);
            }
            return false;
        }

        if (flippedToCast && !castFaceDown) {
            source.forceTurnFaceUp();
        }

        final PlaySpellAbility req = new PlaySpellAbility(controller, sa);
        if (!req.playAbility(true, false, false)) {
            if (!controller.getGame().EXPERIMENTAL_RESTORE_SNAPSHOT) {
                Card rollback = p.getGame().getCardState(source);
                if (castFaceDown) {
                    rollback.setFaceDown(false);
                    rollback.updateStateForView();
                } else if (flippedToCast) {
                    // need to get the changed card if able
                    rollback.turnFaceDown(true);
                    if (rollback.isInZone(ZoneType.Exile)) {
                        rollback.addMayLookFaceDownExile(p);
                    }
                }
            }

            return false;
        }
        return true;
    }

    static SpellAbility chooseOptionalAdditionalCosts(Player p, final SpellAbility original) {
        PlayerController c = p.getController();

        // choose alternative additional cost
        final List<SpellAbility> abilities = GameActionUtil.getAdditionalCostSpell(original);

        final SpellAbility choosen = c.getAbilityToPlay(original.getHostCard(), abilities);

        List<OptionalCostValue> list = GameActionUtil.getOptionalCostValues(choosen);
        if (!list.isEmpty()) {
            list = c.chooseOptionalCosts(choosen, list);
        }

        return GameActionUtil.addOptionalCosts(choosen, list);
    }

    public static boolean payCostDuringAbilityResolve(final PlayerController controller, final Player p, final Cost cost, SpellAbility sourceAbility, String prompt) {
        final Card source = sourceAbility.getHostCard();
        // Only human player pays this way
        Card current = null; // Used in spells with RepeatEach effect to distinguish cards, Cut the Tethers
        if (sourceAbility.hasParam("ShowCurrentCard")) {
            Iterable<? extends Card> iterable = AbilityUtils.getDefinedCards(source, sourceAbility.getParam("ShowCurrentCard"), sourceAbility);
            current = Iterables.getFirst(iterable, null);
        }

        final List<CostPart> parts = cost.getCostParts();
        final List<CostPart> remainingParts = new ArrayList<>(parts);
        CostPart costPart = null;
        if (!parts.isEmpty()) {
            costPart = parts.get(0);
        }
        String orString = getOrStringFromCost(sourceAbility, prompt);

        if (costPart == null || (costPart.getAmount().equals("0") && parts.size() < 2)) {
            return p.getController().confirmPayment(costPart, Localizer.getInstance().getMessage("lblDoYouWantPay") + " {0}?" + orString, sourceAbility);
        }
        // 0 mana costs were slipping through because CostPart.getAmount returns 1
        else if (costPart instanceof CostPartMana && parts.size() < 2) {
            if (((CostPartMana) costPart).getMana().isZero()) {
                return p.getController().confirmPayment(costPart, Localizer.getInstance().getMessage("lblDoYouWantPay") + " {0}?" + orString, sourceAbility);
            }
        }

        final CostDecisionMakerBase hcd = controller.getCostDecisionMaker(p, sourceAbility, true, prompt);
        boolean mandatory = cost.isMandatory();

        //the following costs do not need inputs
        for (CostPart part : parts) {
            // early bail to check if the part can be paid
            if (!part.canPay(sourceAbility, p, hcd.isEffect())) {
                return false;
            }

            boolean mayRemovePart = true;

            // simplified costs that can use the HCD
            if (part instanceof CostPayLife
                    || part instanceof CostDraw
                    || part instanceof CostGainLife
                    || part instanceof CostFlipCoin
                    || part instanceof CostRollDice
                    || part instanceof CostDamage
                    || part instanceof CostEnlist
                    || part instanceof CostExileFromStack
                    || part instanceof CostPutCounter
                    || part instanceof CostRemoveCounter
                    || part instanceof CostRemoveAnyCounter
                    || part instanceof CostMill
                    || part instanceof CostSacrifice
                    || part instanceof CostCollectEvidence) {
                PaymentDecision pd = part.accept(hcd);

                if (pd == null) {
                    return false;
                }
                part.payAsDecided(p, pd, sourceAbility, hcd.isEffect());
            }
            else if (part instanceof CostAddMana) {
                String desc = part.toString();
                desc = desc.substring(0, 1).toLowerCase() + desc.substring(1);

                if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoyouWantTo") + " " + desc + "?" + orString, sourceAbility)) {
                    return false;
                }
                PaymentDecision pd = part.accept(hcd);

                if (pd == null) {
                    return false;
                }
                part.payAsDecided(p, pd, sourceAbility, hcd.isEffect());
            }
            else if (part instanceof CostExile costExile) {
                if ("All".equals(part.getType())) {
                    ZoneType zone = costExile.getFrom().get(0);
                    prompt = ZoneType.Graveyard.equals(zone) ? "lblDoYouWantExileAllCardYouGraveyard" :
                        "lblDoYouWantExileAllCardHand";
                    if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage(prompt),
                        sourceAbility)) return false;
                    costExile.payAsDecided(p, PaymentDecision.card(p.getCardsIn(zone)), sourceAbility, hcd.isEffect());
                } else {
                    CardCollection list = new CardCollection();
                    List<ZoneType> fromZones = costExile.getFrom();
                    boolean multiFromZones = fromZones.size() > 1;
                    for (ZoneType from : fromZones) {
                        list.addAll(costExile.zoneRestriction != 1 ? p.getGame().getCardsIn(from) : p.getCardsIn(from));
                    }
                    list = CardLists.getValidCards(list, part.getType().split(";"), p, source, sourceAbility);
                    final int nNeeded = part.getAbilityAmount(sourceAbility);
                    if (list.size() < nNeeded) {
                        return false;
                    }
                    if (!multiFromZones && fromZones.get(0).equals(ZoneType.Library)) {
                        if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantExileNCardsFromYourLibrary", String.valueOf(nNeeded)), sourceAbility)) {
                            return false;
                        }
                        list = list.subList(0, nNeeded);
                        costExile.payAsDecided(p, PaymentDecision.card(list), sourceAbility, hcd.isEffect());
                    } else {
                        List<String> zoneNames = fromZones.stream().map(ZoneType::getTranslatedName).collect(Collectors.toList());
                        String exilePrompt = Localizer.getInstance().getMessage("lblExileFromZone", Lang.joinHomogenous(zoneNames));
                        CardCollectionView chosen = controller.chooseCardsForCost(list, sourceAbility, costExile, nNeeded, !mandatory, exilePrompt);
                        if(chosen == null || chosen.size() < nNeeded)
                            return false;
                        costExile.payAsDecided(p, PaymentDecision.card(chosen), sourceAbility, hcd.isEffect());
                    }
                }
            } else if (part instanceof CostPutCardToLib) {
                int amount = Integer.parseInt(part.getAmount());
                final ZoneType from = ((CostPutCardToLib) part).getFrom();
                final boolean sameZone = ((CostPutCardToLib) part).isSameZone();
                CardCollectionView listView;
                if (sameZone) {
                    listView = p.getGame().getCardsIn(from);
                } else {
                    listView = p.getCardsIn(from);
                }
                CardCollection list = CardLists.getValidCards(listView, part.getType().split(";"), p, source, sourceAbility);

                if (sameZone) { // Jötun Grunt
                    PlayerCollection payableZone = new PlayerCollection();
                    for (Player player : p.getGame().getPlayers()) {
                        CardCollectionView enoughType = CardLists.filter(list, CardPredicates.isOwner(player));
                        if (enoughType.size() < amount) {
                            list.removeAll(enoughType);
                        } else {
                            payableZone.add(player);
                        }
                    }

                    String playerChoicePrompt = Localizer.getInstance().getMessage("lblPutCardFromWhoseZone", from.getTranslatedName());
                    Player chosen = controller.chooseSingleEntityForEffect(payableZone, sourceAbility, playerChoicePrompt, true, null);

                    if (chosen == null)
                        return false;

                    CardCollection typeList = CardLists.filter(list, CardPredicates.isOwner(chosen));

                    String cardPrompt = Localizer.getInstance().getMessage("lblPutCardToLibrary");
                    CardCollectionView cards = controller.chooseCardsForCost(typeList, sourceAbility, (CostPutCardToLib) part, amount, true, cardPrompt);

                    //401.4 - Owner chooses order of cards.
                    cards = GameActionUtil.orderCardsByTheirOwners(p.getGame(), cards, ZoneType.Library, sourceAbility);

                    int libPosition = Integer.parseInt(((CostPutCardToLib) part).getLibPos());
                    for(Card c : cards)
                        p.getGame().getAction().moveToLibrary(c, libPosition, sourceAbility);
                }
                else { // Tainted Specter, Gurzigost, etc.
                    boolean hasPaid = payCostPart(controller, p, sourceAbility, hcd.isEffect(), (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lblPutIntoLibrary") + orString);
                    if (!hasPaid) {
                        return false;
                    }
                }
            }
            else if (part instanceof CostGainControl) {
                int amount = Integer.parseInt(part.getAmount());
                CardCollectionView list = CardLists.getValidCards(p.getGame().getCardsIn(ZoneType.Battlefield), part.getType(), p, source, sourceAbility);
                boolean hasPaid = payCostPart(controller, p, sourceAbility, hcd.isEffect(), (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lblGainControl") + orString);
                if (!hasPaid) { return false; }
            }
            else if (part instanceof CostReturn) {
                CardCollectionView list = CardLists.getValidCards(p.getCardsIn(ZoneType.Battlefield), part.getType(), p, source, sourceAbility);
                int amount = part.getAbilityAmount(sourceAbility);
                boolean hasPaid = payCostPart(controller, p, sourceAbility, hcd.isEffect(), (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lblReturnToHand") + orString);
                if (!hasPaid) { return false; }
            }
            else if (part instanceof CostDiscard) {
                int amount = part.getAbilityAmount(sourceAbility);
                if ("Hand".equals(part.getType())) {
                    if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantDiscardYourHand"), sourceAbility)) {
                        return false;
                    }

                    part.payAsDecided(p, PaymentDecision.card(p.getCardsIn(ZoneType.Hand)), sourceAbility, true);
                } else if ("Random".equals(part.getType())) {
                    if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblWouldYouLikeRandomDiscardTargetCard", amount), sourceAbility)) {
                        return false;
                    }

                    part.payAsDecided(p, PaymentDecision.card(Aggregates.random(p.getCardsIn(ZoneType.Hand), amount)), sourceAbility, true);
                } else {
                    CardCollectionView list = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), part.getType().split(";"), p, source, sourceAbility);
                    boolean hasPaid = payCostPart(controller, p, sourceAbility, hcd.isEffect(), (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lbldiscard") + orString);
                    if (!hasPaid) { return false; }
                }
            }
            else if (part instanceof CostReveal costReveal) {
                CardCollectionView list = CardLists.getValidCards(p.getCardsIn(costReveal.getRevealFrom()), part.getType().split(";"), p, source, sourceAbility);
                int amount = part.getAbilityAmount(sourceAbility);
                boolean hasPaid = payCostPart(controller, p, sourceAbility, hcd.isEffect(), (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lblReveal") + orString);
                if (!hasPaid) { return false; }
            }
            else if (part instanceof CostTapType) {
                CardCollectionView list = CardLists.getValidCards(p.getCardsIn(ZoneType.Battlefield), part.getType().split(";"), p, source, sourceAbility);
                list = CardLists.filter(list, CardPredicates.CAN_TAP);
                int amount = part.getAbilityAmount(sourceAbility);
                boolean hasPaid = payCostPart(controller, p, sourceAbility, hcd.isEffect(), (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lblTap") + orString);
                if (!hasPaid) { return false; }
            }
            else if (part instanceof CostPartMana) {
                if (!((CostPartMana) part).getMana().isZero()) { // non-zero costs require input
                    mayRemovePart = false;
                }
            }
            else if (part instanceof CostPayEnergy) {
                CounterType counterType = CounterEnumType.ENERGY;
                int amount = part.getAbilityAmount(sourceAbility);

                if (!mandatory && !p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantSpendNTargetTypeCounter", String.valueOf(amount), counterType.getName()), sourceAbility)) {
                    return false;
                }

                p.payEnergy(amount, source);
            }
            else if (part instanceof CostExert) {
                part.payAsDecided(p, PaymentDecision.card(source), sourceAbility, hcd.isEffect());
            }

            else if (part instanceof CostPayShards) {
                int amount = part.getAbilityAmount(sourceAbility);

                if (!mandatory && !p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantPay") + " " + amount + " {M}?", sourceAbility)) {
                    return false;
                }

                p.payShards(amount, source);
            }

            else {
                throw new RuntimeException("GameActionUtil.payCostDuringAbilityResolve - An unhandled type of cost was met: " + part.getClass());
            }

            if (mayRemovePart) {
                remainingParts.remove(part);
            }
        }

        if (remainingParts.isEmpty()) {
            return true;
        }
        if (remainingParts.size() > 1) {
            throw new RuntimeException("GameActionUtil.payCostDuringAbilityResolve - Too many payment types - " + source);
        }
        costPart = remainingParts.get(0);
        // check this is a mana cost
        if (!(costPart instanceof CostPartMana)) {
            throw new RuntimeException("GameActionUtil.payCostDuringAbilityResolve - The remaining payment type is not Mana.");
        }

        if (prompt == null) {
            String promptCurrent = current == null ? "" : Localizer.getInstance().getMessage("lblCurrentCard") + ": " + current;
            prompt = source + "\n" + promptCurrent;
        }

        sourceAbility.clearManaPaid();
        boolean paid = p.getController().payManaCost(cost.getCostMana(), sourceAbility, prompt, null, hcd.isEffect());
        if (!paid) {
            new ManaRefundService(sourceAbility).refundManaPaid();
        }
        return paid;
    }

    public static String getOrStringFromCost(SpellAbility sourceAbility, String prompt) {
        String orString;
        if (sourceAbility.hasParam("OrString")) {
            orString = sourceAbility.getParam("OrString");
        } else {
            orString = prompt == null ? sourceAbility.getStackDescription().trim() : "";
        }
        if (!orString.isEmpty()) {
            if (sourceAbility.hasParam("UnlessSwitched")) {
                return TextUtil.concatWithSpace(" (" + Localizer.getInstance().getMessage("lblIfYouDo") + ":", orString + ")");
            } else {
                return TextUtil.concatWithSpace(" (" + Localizer.getInstance().getMessage("lblOr") + ":", orString, ")");
            }
        }
        return orString;
    }

    private static boolean payCostPart(final PlayerController controller, Player p, SpellAbility sourceAbility, boolean effect, CostPartWithList cpl, int amount, CardCollectionView list, String actionName) {
        if (list.size() < amount)
            return false; // unable to pay (not enough cards)

        String cardDesc = cpl.getDescriptiveType().equalsIgnoreCase("Card") ? "" : cpl.getDescriptiveType();
        CardCollectionView chosen = controller.chooseCardsForCost(list, sourceAbility, cpl, amount, true, Localizer.getInstance().getMessage("lblSelectNSpecifyTypeCardsToAction", cardDesc, actionName));
        if(chosen == null)
            return false;

        cpl.payAsDecided(p, PaymentDecision.card(chosen), sourceAbility, effect);
        return true;
    }

    public static boolean payManaCost(final PlayerController controller, final ManaCost realCost, final CostPartMana mc, final SpellAbility ability, final Player activator, String prompt, ManaConversionMatrix matrix, boolean effect) {
        final Card source = ability.getHostCard();
        ManaCostBeingPaid toPay = new ManaCostBeingPaid(realCost);

        String xInCard = ability.getParamOrDefault("XAlternative", ability.getSVar("X"));
        String xColor = ability.getXColor();
        if (source.hasKeyword("Spend only colored mana on X. No more than one mana of each color may be spent this way.")) {
            xColor = "WUBRGX";
        }
        if (mc.getAmountOfX() > 0 && !"Count$xPaid".equals(xInCard)) { // announce X will overwrite whatever was in card script
            int xPaid = AbilityUtils.calculateAmount(source, xInCard, ability);
            toPay.setXManaCostPaid(xPaid, xColor);
            ability.setXManaCostPaid(xPaid);
        }
        else if (ability.getXManaCostPaid() != null) { //ensure pre-announced X value retained
            toPay.setXManaCostPaid(ability.getXManaCostPaid(), xColor);
        }

        CardCollection cardsToDelve = new CardCollection();
        CostAdjustment.adjust(toPay, ability, activator, cardsToDelve, false, effect);

        Card offering = null;
        Card emerge = null;

        if (ability.isOffering()) {
            if (ability.getSacrificedAsOffering() == null) {
                System.out.println("Sacrifice input for Offering cancelled");
                return false;
            }
            offering = ability.getSacrificedAsOffering();
        }
        if (ability.isEmerge()) {
            if (ability.getSacrificedAsEmerge() == null) {
                System.out.println("Sacrifice input for Emerge cancelled");
                return false;
            }
            emerge = ability.getSacrificedAsEmerge();
        }
        if (!toPay.isPaid()) {
            // if matrix still null it's effect payment
            if (matrix == null) {
                matrix = new ManaConversionMatrix();
                matrix.restoreColorReplacements();
                // pass sa = null so it doesn't consider unless cost on spell
                StaticAbilityManaConvert.manaConvert(matrix, activator, ability.getHostCard(), null);
            }

            // Input is somehow clearing out the offering card?

            if (!controller.applyManaToCost(toPay, ability, prompt, matrix, effect)) {
                return handleOfferingConvokeAndDelve(ability, cardsToDelve, true);
            }

            source.setXManaCostPaidByColor(toPay.getXManaCostPaidByColor());
        }

        // Handle convoke and offerings
        if (ability.isOffering()) {
            if (ability.getSacrificedAsOffering() == null && offering != null) {
                ability.setSacrificedAsOffering(offering);
            }
        }
        if (ability.isEmerge()) {
            if (ability.getSacrificedAsEmerge() == null && emerge != null) {
                ability.setSacrificedAsEmerge(emerge);
            }
        }
        return handleOfferingConvokeAndDelve(ability, cardsToDelve, false);
    }

    private static boolean handleOfferingConvokeAndDelve(final SpellAbility ability, CardCollection cardsToDelve, boolean manaInputCancelled) {
        final Card hostCard = ability.getHostCard();
        final Game game = hostCard.getGame();
        final CardZoneTable table = new CardZoneTable(game.getLastStateBattlefield(), game.getLastStateGraveyard());
        Map<AbilityKey, Object> params = AbilityKey.newMap();
        AbilityKey.addCardZoneTableParams(params, table);

        if (!manaInputCancelled && !cardsToDelve.isEmpty()) {
            for (final Card c : cardsToDelve) {
                hostCard.addDelved(c);
                final Card d = game.getAction().exile(c, null, params);
                hostCard.addExiledCard(d);
                d.setExiledWith(hostCard);
                d.setExiledBy(hostCard.getController());
                d.setExiledSA(ability);
            }
        }
        if (ability.isOffering() && ability.getSacrificedAsOffering() != null) {
            final Card offering = ability.getSacrificedAsOffering();
            offering.setUsedToPay(false);
            if (!manaInputCancelled) {
                game.getAction().sacrifice(new CardCollection(offering), ability, false, params);
            }
            ability.resetSacrificedAsOffering();
        }
        if (ability.isEmerge() && ability.getSacrificedAsEmerge() != null) {
            final Card emerge = ability.getSacrificedAsEmerge();
            emerge.setUsedToPay(false);
            if (!manaInputCancelled) {
                game.getAction().sacrifice(new CardCollection(emerge), ability, false, params);
                ability.setSacrificedAsEmerge(game.getChangeZoneLKIInfo(emerge));
            } else {
                ability.resetSacrificedAsEmerge();
            }
        }
        if (!table.isEmpty() && !manaInputCancelled) {
            table.triggerChangesZoneAll(game, ability);
        }
        return !manaInputCancelled;
    }

    public static void playSaWithoutPayingManaCost(final PlayerController controller, SpellAbility sa, boolean mayChooseNewTargets) {
        //FThreads.assertExecutedByEdt(false);
        final Card source = sa.getHostCard();

        source.setSplitStateToPlayAbility(sa);

        final PlaySpellAbility req = new PlaySpellAbility(controller, sa);
        req.playAbility(mayChooseNewTargets, true, false);
    }

    public static boolean playSpellAbilityNoStack(final PlayerController controller, final Player player, final SpellAbility sa) {
        return playSpellAbilityNoStack(controller, player, sa, false);
    }

    public static boolean playSpellAbilityNoStack(final PlayerController controller, final Player player, final SpellAbility sa, boolean useOldTargets) {
        sa.setActivatingPlayer(player);

        final PlaySpellAbility req = new PlaySpellAbility(controller, sa);
        return req.playAbility(!useOldTargets, false, true);
    }

    public final boolean playAbility(final boolean mayChooseTargets, final boolean isFree, final boolean skipStack) {
        final Player player = ability.getActivatingPlayer();
        final Game game = player.getGame();
        boolean refreeze = game.getStack().isFrozen();

        if (!skipStack) {
            if (!refreeze) {
                // CR 401.5: freeze top library cards until cast/activated so player can't cheat and see the next
                game.setTopLibsCast();
            }

            if (ability.getApi() == ApiType.Charm) {
                if (ability.isAnnouncing("X")) {
                    needX = ability.costHasX();
                    // CR 601.4
                    if (!announceValuesLikeX()) {
                        game.clearTopLibsCast(ability);
                        return false;
                    }
                }
                if (!CharmEffect.makeChoices(ability)) {
                    game.clearTopLibsCast(ability);
                    // CR 603.3c If no mode is chosen, the ability is removed from the stack.
                    return false;
                }
            }

            ability = AbilityUtils.addSpliceEffects(ability);
        }

        // used to rollback
        Zone fromZone = null;
        int zonePosition = 0;
        final ManaPool manapool = player.getManaPool();

        final Card c = ability.getHostCard();
        final CardPlayOption option = c.mayPlay(ability.getMayPlay());

        if (ability.isSpell() && !c.isCopiedSpell()) {
            fromZone = game.getZoneOf(c);
            if (fromZone != null) {
                zonePosition = fromZone.getCards().indexOf(c);
            }
            ability.setHostCard(game.getAction().moveToStack(c, ability));
            ability.changeText();
        }

        if (!ability.isCopied()) {
            ability.resetPaidHash();
            ability.setPaidLife(0);
        }

        if (ability.isSpell() && !c.isCopiedSpell()) {
            ability = GameActionUtil.addExtraKeywordCost(ability);
        }

        Cost abCost = ability.getPayCosts();
        CostPayment payment = new CostPayment(abCost, ability);

        boolean manaColorConversion = false;

        if (!ability.isCopied()) {
            if (ability.isSpell()) { // Apply by Option
                if (option != null && option.applyManaConvert(payment)) {
                    manaColorConversion = true;
                }

                if (option != null && option.isIgnoreSnowSourceManaCostColor()) {
                    payment.setSnowForColor(true);
                }
            }

            if (ability.isActivatedAbility() && ability.getGrantorStatic() != null && ability.getGrantorStatic().hasParam("ManaConversion")) {
                AbilityUtils.applyManaColorConversion(payment, ability.getGrantorStatic().getParam("ManaConversion"));
                manaColorConversion = true;
            }

            if (StaticAbilityManaConvert.manaConvert(payment, player, ability.getHostCard(), ability)) {
                manaColorConversion = true;
            }

            if (ability.hasParam("ManaConversion")) {
                AbilityUtils.applyManaColorConversion(payment, ability.getParam("ManaConversion"));
                manaColorConversion = true;
            }
        }

        // reset is also done early here, because if an ability is canceled from targeting it might otherwise lead to refunding mana from earlier cast
        ability.clearManaPaid();
        ability.getPayingManaAbilities().clear();

        // This line makes use of short-circuit evaluation of boolean values, that is each subsequent argument
        // is only executed or evaluated if the first argument does not suffice to determine the value of the expression
        // because of Selective Snare do announceType first

        boolean preCostRequisites = announceType() && announceValuesLikeX() &&
            ability.checkRestrictions(player) &&
            (!mayChooseTargets || ability.setupTargets()) &&
            ability.canCastTiming(player) &&
            ability.isLegalAfterStack();

        // Freeze the stack just before we start paying costs but after the ability is fully set up
        game.getStack().freezeStack(ability);
        final boolean prerequisitesMet = preCostRequisites && (isFree || payment.payCost(controller.getCostDecisionMaker(player, ability, ability.isTrigger())));

        game.clearTopLibsCast(ability);

        if (!prerequisitesMet) {
            // Would love to restore game state when undoing a trigger rather than just declining all costs.
            // Is there a way to tell the difference?

            if (ability.isTrigger()) {
                // Only roll back triggers if they were not paid for
                if (game.EXPERIMENTAL_RESTORE_SNAPSHOT && preCostRequisites) {
                    GameActionUtil.rollbackAbility(ability, fromZone, zonePosition, payment, c);
                } else {
                    // If precost requsities failed, then there probably isn't anything to refund during experimental
                    payment.refundPayment();
                }
            } else {
                GameActionUtil.rollbackAbility(ability, fromZone, zonePosition, payment, c);
            }

            if (!refreeze) {
                game.getStack().unfreezeStack();
            }

            // These restores may not need to happen if we're restoring from snapshot
            if (manaColorConversion) {
                manapool.restoreColorReplacements();
            }

            return false;
        }

        if (isFree || payment.isFullyPaid()) {
            //track when planeswalker ultimates are activated
            player.getAchievementTracker().onSpellAbilityPlayed(ability);

            if (skipStack) {
                AbilityUtils.resolve(ability);
                // Should unfreeze stack (but if it was a RE with a cause better to let it be handled by that)
                if (!ability.isReplacementAbility()) {
                    game.getStack().unfreezeStack();
                }
            } else {
                ensureAbilityHasDescription(ability);
                game.getStack().addAndUnfreeze(ability);
            }

            if (manaColorConversion) {
                manapool.restoreColorReplacements();
            }
        }
        return true;
    }

    private boolean announceValuesLikeX() {
        if (ability.isCopied() || ability.isWrapper()) { return true; } //don't re-announce for spell copies

        final Cost cost = ability.getPayCosts();
        final Card card = ability.getHostCard();

        // Announcing Requirements like Choosing X or Multikicker
        // SA Params as comma delimited list
        final String announce = ability.getParam("Announce");
        if (announce != null && needX) {
            for (final String aVar : announce.split(",")) {
                final String varName = aVar.trim();
                Range<Integer> range = AbilityUtils.getAnnouncementBounds(ability, varName);

                final Integer value = controller.announceRequirements(ability, range.getMinimum(), range.getMaximum(), varName);
                if (value == null) {
                    return false;
                }

                if ("X".equalsIgnoreCase(varName)) {
                    needX = false;
                    ability.setXManaCostPaid(value);
                } else {
                    ability.setSVar(varName, value.toString());
                    card.setSVar(varName, value.toString());
                }
            }
        }

        if (needX) {
            if (cost.hasXInAnyCostPart()) {
                final String sVar = ability.getParamOrDefault("XAlternative", ability.getSVar("X")); //only prompt for new X value if card doesn't determine it another way
                // check if X != 0 is even allowed or the X shard got removed
                boolean replacedXshard = ability.isSpell() && ability.getHostCard().getManaCost().countX() > 0 && !cost.hasXInAnyCostPart();
                if (("Count$xPaid".equals(sVar) && !replacedXshard) || sVar.isEmpty()) {
                    Range<Integer> range = AbilityUtils.getAnnouncementBounds(ability, "X");
                    final Integer value = controller.announceRequirements(ability, range.getMinimum(), range.getMaximum(), "X");
                    if (value == null) {
                        return false;
                    }
                    ability.setXManaCostPaid(value);
                }
            } else {
                ability.setXManaCostPaid(null);
            }
        }
        return true;
    }

    // Announcing Requirements like choosing creature type or number
    private boolean announceType() {
        if (ability.isCopied()) { return true; } //don't re-announce for spell copies

        final String announce = ability.getParam("AnnounceType");
        final PlayerController pc = ability.getActivatingPlayer().getController();
        if (announce != null) {
            for (final String aVar : announce.split(",")) {
                final String varName = aVar.trim();
                if ("CreatureType".equals(varName)) {
                    final String choice = pc.chooseSomeType("Creature", ability, CardType.getAllCreatureTypes());
                    if(choice == null) //No options to choose from?
                        return false;
                    ability.getHostCard().setChosenType(choice);
                }
                if ("ChooseNumber".equals(varName)) {
                    final int min = Integer.parseInt(ability.getParam("Min"));
                    final int max = Integer.parseInt(ability.getParam("Max"));
                    final int i = ability.getActivatingPlayer().getController().chooseNumber(ability,
                            Localizer.getInstance().getMessage("lblChooseNumber") , min, max);
                    ability.getHostCard().setChosenNumber(i);
                }
                if ("Opponent".equals(varName)) {
                    Player opp = ability.getActivatingPlayer().getController().chooseSingleEntityForEffect(ability.getActivatingPlayer().getOpponents(), ability, Localizer.getInstance().getMessage("lblChooseAnOpponent"), null);
                    if(opp == null)
                        return false;
                    ability.getHostCard().setChosenPlayer(opp);
                }
            }
        }
        return true;
    }

    private static void ensureAbilityHasDescription(final SpellAbility ability) {
        if (!StringUtils.isBlank(ability.getStackDescription())) {
            return;
        }

        // For older abilities that don't setStackDescription set it here
        final StringBuilder sb = new StringBuilder();
        sb.append(ability.getHostCard().getDisplayName());
        if (ability.usesTargeting()) {
            final Iterable<GameObject> targets = ability.getTargets();
            if (!Iterables.isEmpty(targets)) {
                sb.append(" - Targeting ");
                for (final GameObject o : targets) {
                    sb.append(o.toString()).append(" ");
                }
            }
        }

        ability.setStackDescription(sb.toString());
    }
}
