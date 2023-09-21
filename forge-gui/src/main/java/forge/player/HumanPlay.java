package forge.player;

import java.util.ArrayList;
import java.util.List;

import forge.ImageKeys;
import forge.game.cost.*;

import com.google.common.collect.Iterables;

import forge.card.CardStateName;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntityView;
import forge.game.GameEntityViewMap;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CardView;
import forge.game.card.CardZoneTable;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.mana.ManaConversionMatrix;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.PlayerView;
import forge.game.spellability.LandAbility;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityManaConvert;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.input.InputPayMana;
import forge.gamemodes.match.input.InputPayManaOfCostPayment;
import forge.gamemodes.match.input.InputSelectCardsFromList;
import forge.gui.FThreads;
import forge.gui.util.SGuiChoose;
import forge.util.Aggregates;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;


public class HumanPlay {

    private HumanPlay() {
    }

    /**
     * <p>
     * playSpellAbility.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public final static boolean playSpellAbility(final PlayerControllerHuman controller, final Player p, SpellAbility sa) {
        FThreads.assertExecutedByEdt(false);

        Card source = sa.getHostCard();
        sa.setActivatingPlayer(p);

        if (sa instanceof LandAbility) {
            if (sa.canPlay()) {
                sa.resolve();
                p.getGame().updateLastStateForCard(source);
            }
            return false;
        }

        boolean isforetold = source.isForetold();
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

        if (sa.getApi() == ApiType.Charm && !CharmEffect.makeChoices(sa)) {
            // 603.3c If no mode is chosen, the ability is removed from the stack.
            return false;
        }

        sa = AbilityUtils.addSpliceEffects(sa);

        final HumanPlaySpellAbility req = new HumanPlaySpellAbility(controller, sa);
        if (!req.playAbility(true, false, false)) {
            Card rollback = p.getGame().getCardState(source);
            if (castFaceDown) {
                rollback.setFaceDown(false);
            } else if (flippedToCast) {
                // need to get the changed card if able
                rollback.turnFaceDown(true);
                //need to set correct imagekey when forcing facedown
                rollback.setImageKey(ImageKeys.getTokenKey(isforetold ? ImageKeys.FORETELL_IMAGE : ImageKeys.HIDDEN_CARD));
                if (rollback.isInZone(ZoneType.Exile)) {
                    rollback.addMayLookTemp(p);
                }
            }
            return false;
        }
        return true;
    }

    /**
     * choose optional additional costs. For HUMAN only
     * @param p
     *
     * @param original
     *            the original sa
     * @return an ArrayList<SpellAbility>.
     */
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

    /**
     * <p>
     * playSpellAbilityForFree.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     */
    public static final void playSaWithoutPayingManaCost(final PlayerControllerHuman controller, final Game game, SpellAbility sa, boolean mayChooseNewTargets) {
        FThreads.assertExecutedByEdt(false);
        final Card source = sa.getHostCard();

        source.setSplitStateToPlayAbility(sa);

        if (sa.getApi() == ApiType.Charm && !CharmEffect.makeChoices(sa)) {
            // 603.3c If no mode is chosen, the ability is removed from the stack.
            return;
        }

        if (!sa.isCopied()) {
            sa = AbilityUtils.addSpliceEffects(sa);
        }

        final HumanPlaySpellAbility req = new HumanPlaySpellAbility(controller, sa);
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

        final HumanPlaySpellAbility req = new HumanPlaySpellAbility(controller, sa);
        return req.playAbility(!useOldTargets, false, true);
    }

    /**
     * <p>
     * payCostDuringAbilityResolve.
     * </p>
     *
     * @param cost
     *            a {@link forge.game.cost.Cost} object.
     * @param sourceAbility TODO
     */
    public static boolean payCostDuringAbilityResolve(final PlayerControllerHuman controller, final Player p, final Card source, final Cost cost, SpellAbility sourceAbility, String prompt) {
        // Only human player pays this way
        Card current = null; // Used in spells with RepeatEach effect to distinguish cards, Cut the Tethers
        if (sourceAbility.hasParam("ShowCurrentCard")) {
            current = Iterables.getFirst(AbilityUtils.getDefinedCards(source, sourceAbility.getParam("ShowCurrentCard"), sourceAbility), null);
        }

        final List<CostPart> parts = cost.getCostParts();
        final List<CostPart> remainingParts = new ArrayList<>(parts);
        CostPart costPart = null;
        if (!parts.isEmpty()) {
            costPart = parts.get(0);
        }
        String orString;
        if (sourceAbility.hasParam("OrString")) {
            orString = sourceAbility.getParam("OrString");
        } else {
            orString = prompt == null ? sourceAbility.getStackDescription().trim() : "";
        }
        if (!orString.isEmpty()) {
            if (sourceAbility.hasParam("UnlessSwitched")) {
                orString = TextUtil.concatWithSpace(" (" + Localizer.getInstance().getMessage("lblIfYouDo") + ":", orString + ")");
            } else {
                orString = TextUtil.concatWithSpace(" (" + Localizer.getInstance().getMessage("lblOr") + ":", orString, ")");
            }
        }

        if (parts.isEmpty() || (costPart.getAmount().equals("0") && parts.size() < 2)) {
            return p.getController().confirmPayment(costPart, Localizer.getInstance().getMessage("lblDoYouWantPay") + " {0}?" + orString, sourceAbility);
        }
        // 0 mana costs were slipping through because CostPart.getAmount returns 1
        else if (costPart instanceof CostPartMana && parts.size() < 2) {
            if (((CostPartMana) costPart).getMana().isZero()) {
                return p.getController().confirmPayment(costPart, Localizer.getInstance().getMessage("lblDoYouWantPay") + " {0}?" + orString, sourceAbility);
            }
        }

        final HumanCostDecision hcd = new HumanCostDecision(controller, p, sourceAbility, true, source, orString);
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
                    || part instanceof CostSacrifice) {
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
            else if (part instanceof CostExile) {
                CostExile costExile = (CostExile) part;

                ZoneType from = ZoneType.Graveyard;
                if ("All".equals(part.getType())) {
                    if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantExileAllCardYouGraveyard"), sourceAbility)) {
                        return false;
                    }

                    costExile.payAsDecided(p, PaymentDecision.card(p.getCardsIn(ZoneType.Graveyard)), sourceAbility, hcd.isEffect());
                } else {
                    from = costExile.getFrom();
                    CardCollection list;
                    if (costExile.zoneRestriction != 1) {
                        list = new CardCollection(p.getGame().getCardsIn(from));
                    } else {
                        list = new CardCollection(p.getCardsIn(from));
                    }
                    list = CardLists.getValidCards(list, part.getType().split(";"), p, source, sourceAbility);
                    final int nNeeded = part.getAbilityAmount(sourceAbility);
                    if (list.size() < nNeeded) {
                        return false;
                    }
                    if (from == ZoneType.Library) {
                        if (!p.getController().confirmPayment(part, Localizer.getInstance().getMessage("lblDoYouWantExileNCardsFromYourLibrary", String.valueOf(nNeeded)), sourceAbility)) {
                            return false;
                        }
                        list = list.subList(0, nNeeded);
                        costExile.payAsDecided(p, PaymentDecision.card(list), sourceAbility, hcd.isEffect());
                    } else {
                        // replace this with input
                        CardCollection newList = new CardCollection();
                        GameEntityViewMap<Card, CardView> gameCacheList = GameEntityView.getMap(list);
                        for (int i = 0; i < nNeeded; i++) {
                            final CardView cv;
                            if (mandatory) {
                                cv = SGuiChoose.one(Localizer.getInstance().getMessage("lblExileFromZone", from.getTranslatedName()), gameCacheList.getTrackableKeys());
                            } else {
                                cv = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblExileFromZone", from.getTranslatedName()), gameCacheList.getTrackableKeys());
                            }
                            if (cv == null || !gameCacheList.containsKey(cv)) {
                                return false;
                            }
                            newList.add(gameCacheList.remove(cv));
                        }
                        costExile.payAsDecided(p, PaymentDecision.card(newList), sourceAbility, hcd.isEffect());
                    }
                }
            }
            else if (part instanceof CostPutCardToLib) {
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

                if (sameZone) { // Jotun Grunt
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
                    GameEntityViewMap<Player, PlayerView> gameCachePlayer = GameEntityView.getMap(payableZone);
                    PlayerView pv = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblPutCardFromWhoseZone", from.getTranslatedName()), gameCachePlayer.getTrackableKeys());
                    if (pv == null || !gameCachePlayer.containsKey(pv)) {
                        return false;
                    }
                    Player chosen = gameCachePlayer.get(pv);

                    List<Card> typeList = CardLists.filter(list, CardPredicates.isOwner(chosen));

                    GameEntityViewMap<Card, CardView> gameCacheTypeList = GameEntityView.getMap(typeList);
                    for (int i = 0; i < amount; i++) {
                        if (gameCacheTypeList.isEmpty()) {
                            return false;
                        }
                        final CardView cv = SGuiChoose.oneOrNone(Localizer.getInstance().getMessage("lblPutCardToLibrary"), gameCacheTypeList.getTrackableKeys());
                        if (cv == null || !gameCacheTypeList.containsKey(cv)) {
                            return false;
                        }
                        final Card c = gameCacheTypeList.get(cv);

                        gameCacheTypeList.remove(c);
                        p.getGame().getAction().moveToLibrary(c, Integer.parseInt(((CostPutCardToLib) part).getLibPos()), null);
                    }
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
                CardCollectionView list = CardLists.getValidCards(p.getCardsIn(costReveal.getRevealFrom()), part.getType(), p, source, sourceAbility);
                int amount = part.getAbilityAmount(sourceAbility);
                boolean hasPaid = payCostPart(controller, p, sourceAbility, hcd.isEffect(), (CostPartWithList)part, amount, list, Localizer.getInstance().getMessage("lblReveal") + orString);
                if (!hasPaid) { return false; }
            }
            else if (part instanceof CostTapType) {
                CardCollectionView list = CardLists.getValidCards(p.getCardsIn(ZoneType.Battlefield), part.getType(), p, source, sourceAbility);
                list = CardLists.filter(list, Presets.UNTAPPED);
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
                CounterType counterType = CounterType.get(CounterEnumType.ENERGY);
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
            p.getManaPool().refundManaPaid(sourceAbility);
        }
        return paid;
    }

    private static boolean payCostPart(final PlayerControllerHuman controller, Player p, SpellAbility sourceAbility, boolean effect, CostPartWithList cpl, int amount, CardCollectionView list, String actionName) {
        if (list.size() < amount) { return false; } // unable to pay (not enough cards)

        InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, amount, amount, list, sourceAbility);
        String cardDesc = cpl.getDescriptiveType().equalsIgnoreCase("Card") ? "" : cpl.getDescriptiveType();
        inp.setMessage(Localizer.getInstance().getMessage("lblSelectNSpecifyTypeCardsToAction", cardDesc, actionName));
        inp.setCancelAllowed(true);

        inp.showAndWait();
        if (inp.hasCancelled() || inp.getSelected().size() != amount) {
            return false;
        }

        cpl.payAsDecided(p, PaymentDecision.card(inp.getSelected()), sourceAbility, effect);

        return true;
    }

    private static boolean handleOfferingConvokeAndDelve(final SpellAbility ability, CardCollection cardsToDelve, boolean manaInputCancelled) {
        Card hostCard = ability.getHostCard();
        final Game game = hostCard.getGame();

        final CardZoneTable table = new CardZoneTable();
        if (!manaInputCancelled && !cardsToDelve.isEmpty()) {
            for (final Card c : cardsToDelve) {
                hostCard.addDelved(c);
                final ZoneType o = c.getZone().getZoneType();
                final Card d = game.getAction().exile(c, null, null);
                hostCard.addExiledCard(d);
                d.setExiledWith(hostCard);
                d.setExiledBy(hostCard.getController());
                table.put(o, d.getZone().getZoneType(), d);
            }
        }
        if (ability.isOffering() && ability.getSacrificedAsOffering() != null) {
            final Card offering = ability.getSacrificedAsOffering();
            offering.setUsedToPay(false);
            if (!manaInputCancelled) {
                game.getAction().sacrifice(offering, ability, false, table, null);
            }
            ability.resetSacrificedAsOffering();
        }
        if (ability.isEmerge() && ability.getSacrificedAsEmerge() != null) {
            final Card emerge = ability.getSacrificedAsEmerge();
            emerge.setUsedToPay(false);
            if (!manaInputCancelled) {
                game.getAction().sacrifice(emerge, ability, false, table, null);
            }
            ability.resetSacrificedAsEmerge();
        }
        if (ability.getTappedForConvoke() != null) {
            for (final Card c : ability.getTappedForConvoke()) {
                c.setTapped(false);
                if (!manaInputCancelled) {
                    c.tap(true, ability, ability.getActivatingPlayer());
                }
            }
            ability.clearTappedForConvoke();
        }
        if (!table.isEmpty() && !manaInputCancelled) {
            table.triggerChangesZoneAll(game, ability);
        }
        return !manaInputCancelled;
    }

    public static boolean payManaCost(final PlayerControllerHuman controller, final ManaCost realCost, final CostPartMana mc, final SpellAbility ability, final Player activator, String prompt, ManaConversionMatrix matrix, boolean effect) {
        final Card source = ability.getHostCard();
        ManaCostBeingPaid toPay = new ManaCostBeingPaid(realCost);

        String xInCard = source.getSVar("X");
        String xColor = ability.getXColor();
        if (source.hasKeyword("Spend only colored mana on X. No more than one mana of each color may be spent this way.")) {
            xColor = "WUBRGX";
        }
        if (mc.getAmountOfX() > 0 && !"Count$xPaid".equals(xInCard)) { // announce X will overwrite whatever was in card script
            int xPaid = AbilityUtils.calculateAmount(source, "X", ability);
            toPay.setXManaCostPaid(xPaid, xColor);
            ability.setXManaCostPaid(xPaid);
        }
        else if (ability.getXManaCostPaid() != null) { //ensure pre-announced X value retained
            toPay.setXManaCostPaid(ability.getXManaCostPaid(), xColor);
        }

        int timesMultikicked = source.getKickerMagnitude();
        if (timesMultikicked > 0 && ability.isAnnouncing("Multikicker")) {
            ManaCost mkCost = ability.getMultiKickerManaCost();
            for (int i = 0; i < timesMultikicked; i++) {
                toPay.addManaCost(mkCost);
            }
        }

        int timesPseudokicked = source.getPseudoKickerMagnitude();
        if (timesPseudokicked > 0 && ability.isAnnouncing("Pseudo-multikicker")) {
            ManaCost mkCost = ability.getMultiKickerManaCost();
            for (int i = 0; i < timesPseudokicked; i++) {
                toPay.addManaCost(mkCost);
            }
        }

        CardCollection cardsToDelve = new CardCollection();
        if (!effect) {
            CostAdjustment.adjust(toPay, ability, cardsToDelve, false);
        }

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
            if (ability.getSacrificedAsOffering() != null) {
                System.out.println("Finishing up Offering");
                offering.setUsedToPay(false);
                activator.getGame().getAction().sacrifice(offering, ability, false, null, null);
                ability.resetSacrificedAsOffering();
            }
        }
        if (ability.isEmerge()) {
            if (ability.getSacrificedAsEmerge() == null && emerge != null) {
                ability.setSacrificedAsEmerge(emerge);
            }
            if (ability.getSacrificedAsEmerge() != null) {
                System.out.println("Finishing up Emerge");
                emerge.setUsedToPay(false);
                activator.getGame().getAction().sacrifice(emerge, ability, false, null, null);
                ability.resetSacrificedAsEmerge();
            }
        }
        if (ability.getTappedForConvoke() != null) {
            activator.getGame().getTriggerHandler().suppressMode(TriggerType.Taps);
            for (final Card c : ability.getTappedForConvoke()) {
                c.setTapped(false);
                c.tap(true, ability, activator);
            }
            activator.getGame().getTriggerHandler().clearSuppression(TriggerType.Taps);
            ability.clearTappedForConvoke();
        }
        return handleOfferingConvokeAndDelve(ability, cardsToDelve, false);
    }
}
