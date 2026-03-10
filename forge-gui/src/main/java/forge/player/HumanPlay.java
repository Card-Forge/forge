package forge.player;

import com.google.common.collect.Iterables;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.cost.*;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.mana.ManaRefundService;
import forge.game.player.PlaySpellAbility;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityManaConvert;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.input.InputPayMana;
import forge.gamemodes.match.input.InputPayManaOfCostPayment;
import forge.gui.FThreads;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class HumanPlay {

    private HumanPlay() {
    }

    /**
     * <p>
     * playSpellAbilityForFree.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public static final void playSaWithoutPayingManaCost(final PlayerControllerHuman controller, SpellAbility sa, boolean mayChooseNewTargets) {
        FThreads.assertExecutedByEdt(false);
        final Card source = sa.getHostCard();

        source.setSplitStateToPlayAbility(sa);

        final PlaySpellAbility req = new PlaySpellAbility(controller, sa);
        req.playAbility(mayChooseNewTargets, true, false);
    }

    /**
     * <p>
     * playSpellAbility_NoStack.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public final static boolean playSpellAbilityNoStack(final PlayerControllerHuman controller, final Player player, final SpellAbility sa) {
        return playSpellAbilityNoStack(controller, player, sa, false);
    }
    public final static boolean playSpellAbilityNoStack(final PlayerControllerHuman controller, final Player player, final SpellAbility sa, boolean useOldTargets) {
        sa.setActivatingPlayer(player);

        final PlaySpellAbility req = new PlaySpellAbility(controller, sa);
        return req.playAbility(!useOldTargets, false, true);
    }

    /**
     * <p>
     * payCostDuringAbilityResolve.
     * </p>
     *
     * @param cost          a {@link Cost} object.
     * @param sourceAbility TODO
     */
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

        if (parts.isEmpty() || (costPart.getAmount().equals("0") && parts.size() < 2)) {
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
                        String exilePrompt = Localizer.getInstance().getMessage("lblExileFromZone",Lang.joinHomogenous(zoneNames));
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
                    FCollectionView<Player> players = p.getGame().getPlayers();
                    List<Player> payableZone = new ArrayList<>();
                    for (Player player : players) {
                        CardCollectionView enoughType = CardLists.filter(list, CardPredicates.isOwner(player));
                        if (enoughType.size() < amount) {
                            list.removeAll(enoughType);
                        } else {
                            payableZone.add(player);
                        }
                    }

                    String playerChoicePrompt = Localizer.getInstance().getMessage("lblPutCardFromWhoseZone", from.getTranslatedName());
                    Player chosen = controller.chooseSingleEntityForEffect(players, sourceAbility, playerChoicePrompt, true, null);

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

                    ((CostDiscard)part).payAsDecided(p, PaymentDecision.card(p.getCardsIn(ZoneType.Hand)), sourceAbility, true);
                } else if ("Random".equals(part.getType())) {
                    if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblWouldYouLikeRandomDiscardTargetCard", amount), sourceAbility)) {
                        return false;
                    }

                    ((CostDiscard)part).payAsDecided(p, PaymentDecision.card(Aggregates.random(p.getCardsIn(ZoneType.Hand), amount)), sourceAbility, true);
                } else {
                    CardCollectionView list = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), part.getType().split(";"), p, source, sourceAbility);
                    boolean hasPaid = payCostPart(controller, p, sourceAbility, hcd.isEffect(), (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lbldiscard") + orString);
                    if (!hasPaid) { return false; }
                }
            }
            else if (part instanceof CostReveal) {
                CostReveal costReveal = (CostReveal) part;
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

    public static boolean payManaCost(final PlayerControllerHuman controller, final ManaCost realCost, final CostPartMana mc, final SpellAbility ability, final Player activator, String prompt, ManaConversionMatrix matrix, boolean effect) {
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

        InputPayMana inpPayment;
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
            inpPayment = new InputPayManaOfCostPayment(controller, toPay, ability, activator, matrix, effect);
            inpPayment.setMessagePrefix(prompt);
            inpPayment.showAndWait();
            if (!inpPayment.isPaid()) {
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
}
