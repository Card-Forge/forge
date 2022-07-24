package forge.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.CardType;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.GameEntityView;
import forge.game.GameEntityViewMap;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.cost.*;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.input.InputConfirm;
import forge.gamemodes.match.input.InputSelectCardsFromList;
import forge.gamemodes.match.input.InputSelectManyBase;
import forge.gui.util.SGuiChoose;
import forge.util.Aggregates;
import forge.util.CardTranslation;
import forge.util.ITriggerEvent;
import forge.util.ImageUtil;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;

public class HumanCostDecision extends CostDecisionMakerBase {
    private final PlayerControllerHuman controller;
    private final SpellAbility ability;
    private final Card source;
    private String orString = null;
    private boolean mandatory;

    public HumanCostDecision(final PlayerControllerHuman controller, final Player p, final SpellAbility sa, final boolean effect, final Card source) {
        this(controller, p, sa, effect, source, null);
    }
    public HumanCostDecision(final PlayerControllerHuman controller, final Player p, final SpellAbility sa, final boolean effect, final Card source, final String orString) {
        super(p, effect);
        this.controller = controller;
        ability = sa;
        mandatory = sa.getPayCosts().isMandatory();
        this.source = source;
        this.orString = orString;
    }

    @Override
    public PaymentDecision visit(final CostAddMana cost) {
        return PaymentDecision.number(cost.getAbilityAmount(ability));
    }

    @Override
    public PaymentDecision visit(final CostChooseCreatureType cost) {
        final String choice = controller.chooseSomeType(Localizer.getInstance().getMessage("lblCreature"), ability, new ArrayList<>(CardType.Constant.CREATURE_TYPES), new ArrayList<>(), true);
        if (null == choice) {
            return null;
        }
        return PaymentDecision.type(choice);
    }

    @Override
    public PaymentDecision visit(final CostDiscard cost) {
        CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
        final String discardType = cost.getType();

        if (cost.payCostFromSource()) {
            return hand.contains(source) ? PaymentDecision.card(source) : null;
        }

        if (discardType.equals("Hand")) {
            if (!mandatory && !confirmAction(Localizer.getInstance().getMessage("lblDoYouWantDiscardYourHand"))/*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblDoYouWantDiscardYourHand"), ability)*/) {
                return null;
            }
            if (hand.size() > 1 && ability.getActivatingPlayer() != null) {
                hand = ability.getActivatingPlayer().getController().orderMoveToZoneList(hand, ZoneType.Graveyard, ability);
            }
            return PaymentDecision.card(hand);
        }

        if (discardType.equals("LastDrawn")) {
            final Card lastDrawn = player.getLastDrawnCard();
            return hand.contains(lastDrawn) ? PaymentDecision.card(lastDrawn) : null;
        }

        int c = cost.getAbilityAmount(ability);

        if (discardType.equals("Random")) {
            CardCollectionView randomSubset = Aggregates.random(hand, c, new CardCollection());
            if (randomSubset.size() > 1 && ability.getActivatingPlayer() != null) {
                randomSubset = ability.getActivatingPlayer().getController().orderMoveToZoneList(randomSubset, ZoneType.Graveyard, ability);
            }
            return PaymentDecision.card(randomSubset);
        }
        if (discardType.equals("DifferentNames")) {
            final CardCollection discarded = new CardCollection();
            while (c > 0) {
                final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, hand, ability);
                inp.setMessage(Localizer.getInstance().getMessage("lblSelectOneDifferentNameCardToDiscardAlreadyChosen") + discarded);
                inp.setCancelAllowed(true);
                inp.showAndWait();
                if (inp.hasCancelled()) {
                    return null;
                }
                final Card first = inp.getFirstSelected();
                discarded.add(first);
                hand = CardLists.filter(hand, Predicates.not(CardPredicates.sharesNameWith(first)));
                c--;
            }
            return PaymentDecision.card(discarded);
        }
        if (discardType.contains("+WithSameName")) {
            final String type = TextUtil.fastReplace(discardType, "+WithSameName", "");
            hand = CardLists.getValidCards(hand, type.split(";"), player, source, ability);
            final CardCollectionView landList2 = hand;
            hand = CardLists.filter(hand, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    for (final Card card : landList2) {
                        if (!card.equals(c) && card.getName().equals(c.getName())) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            if (c == 0) {
                return PaymentDecision.card(new CardCollection());
            }
            final CardCollection discarded = new CardCollection();
            while (c > 0) {
                final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, hand, ability);
                inp.setMessage(Localizer.getInstance().getMessage("lblSelectOneSameNameCardToDiscardAlreadyChosen") + discarded);
                inp.setCancelAllowed(true);
                inp.showAndWait();
                if (inp.hasCancelled()) {
                    return null;
                }
                final Card first = inp.getFirstSelected();
                discarded.add(first);
                final CardCollection filteredHand = CardLists.filter(hand, CardPredicates.nameEquals(first.getName()));
                filteredHand.remove(first);
                hand = filteredHand;
                c--;
            }
            return PaymentDecision.card(discarded);
        }

        final String type = discardType;
        final String[] validType = type.split(";");
        hand = CardLists.getValidCards(hand, validType, player, source, ability);

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, hand, ability);
        inp.setMessage(Localizer.getInstance().getMessage("lblSelectNMoreTargetTypeCardToDiscard", "%d", cost.getDescriptiveType()));
        inp.setCancelAllowed(!mandatory);
        inp.showAndWait();
        if (inp.hasCancelled() || inp.getSelected().size() != c) {
            return null;
        }
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(final CostDamage cost) {
        int c = cost.getAbilityAmount(ability);

        if (confirmAction(Localizer.getInstance().getMessage("lblDoYouWantCardDealNDamageToYou", CardTranslation.getTranslatedName(source.getName()), String.valueOf(c)))/*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblDoYouWantCardDealNDamageToYou", CardTranslation.getTranslatedName(source.getName()), String.valueOf(c)), ability)*/) {
            return PaymentDecision.number(c);
        }
        return null;
    }

    @Override
    public PaymentDecision visit(final CostDraw cost) {
        if (!cost.canPay(ability, player, isEffect())) {
            return null;
        }

        int c = cost.getAbilityAmount(ability);

        List<Player> res = cost.getPotentialPlayers(player, ability);

        String message = null;
        if (orString != null && !orString.isEmpty()) {
            if (res.contains(player)) {
                message = Localizer.getInstance().getMessage("lblDoYouWantLetThatPlayerDrawNCardOrDoAction", String.valueOf(c), orString);
            } else {
                message = Localizer.getInstance().getMessage("lblDoYouWantDrawNCardOrDoAction", String.valueOf(c), orString);
            }
        } else {
            message = Localizer.getInstance().getMessage("lblDrawNCardsConfirm", String.valueOf(c));
        }

        if (!confirmAction(message)/*controller.confirmPayment(cost, message, ability)*/) {
            return null;
        }

        PaymentDecision decision = PaymentDecision.players(res);
        decision.c = c;
        return decision;
    }

    @Override
    public PaymentDecision visit(final CostExile cost) {
        final Game game = player.getGame();

        String type = cost.getType();
        boolean fromTopGrave = false;
        if (type.contains("FromTopGrave")) {
            type = TextUtil.fastReplace(type, "FromTopGrave", "");
            fromTopGrave = true;
        }

        CardCollection list;
        if (cost.getFrom().equals(ZoneType.Stack)) {
            list = new CardCollection();
            for (final SpellAbilityStackInstance si : game.getStack()) {
                list.add(si.getSourceCard());
            }
        }
        else if (cost.sameZone) {
            list = new CardCollection(game.getCardsIn(cost.from));
        }
        else {
            list = new CardCollection(player.getCardsIn(cost.from));
        }

        if (cost.payCostFromSource()) {
            return source.getZone() == player.getZone(cost.from) && confirmAction(Localizer.getInstance().getMessage("lblExileConfirm", CardTranslation.getTranslatedName(source.getName())))/*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblExileConfirm", CardTranslation.getTranslatedName(source.getName())), ability)*/ ? PaymentDecision.card(source) : null;
        }

        if (type.equals("All")) {
            return PaymentDecision.card(list);
        }
        list = CardLists.getValidCards(list, type.split(";"), player, source, ability);

        int c = cost.getAbilityAmount(ability);

        if (list.size() < c) {
            return null;
        }

        if (c == 0) { //in case choice was already made to pay 0 cards
            return PaymentDecision.number(c);
        }

        if (cost.from == ZoneType.Battlefield || cost.from == ZoneType.Hand) {
            final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, list, ability);
            inp.setMessage(Localizer.getInstance().getMessage("lblExileNCardsFromYourZone", "%d", cost.getFrom().getTranslatedName()));
            inp.setCancelAllowed(!mandatory);
            inp.showAndWait();
            return inp.hasCancelled() ? null : PaymentDecision.card(inp.getSelected());
        }

        if (cost.from == ZoneType.Library) { return exileFromTop(cost, ability, player, c); }
        if (fromTopGrave) { return exileFromTopGraveType(ability, c, list); }
        if (!cost.sameZone) { return exileFromMiscZone(cost, ability, c, list); }

        final FCollectionView<Player> players = game.getPlayers();
        final List<Player> payableZone = new ArrayList<>();
        for (final Player p : players) {
            final CardCollection enoughType = CardLists.filter(list, CardPredicates.isOwner(p));
            if (enoughType.size() < c) {
                list.removeAll((CardCollectionView)enoughType);
            } else {
                payableZone.add(p);
            }
        }
        return exileFromSame(cost, list, c, payableZone);
    }

    // Inputs

    // Exile<Num/Type{/TypeDescription}>
    // ExileFromHand<Num/Type{/TypeDescription}>
    // ExileFromGrave<Num/Type{/TypeDescription}>
    // ExileFromTop<Num/Type{/TypeDescription}> (of library)
    // ExileSameGrave<Num/Type{/TypeDescription}>

    private PaymentDecision exileFromSame(final CostExile cost, final CardCollectionView list, final int nNeeded, final List<Player> payableZone) {
        if (nNeeded == 0) {
            return PaymentDecision.number(0);
        }
        GameEntityViewMap<Player, PlayerView> gameCachePlayer = GameEntityView.getMap(payableZone);
        final PlayerView pv = controller.getGui().oneOrNone(Localizer.getInstance().getMessage("lblExileFromWhoseZone", cost.getFrom().getTranslatedName()), gameCachePlayer.getTrackableKeys());
        if (pv == null || !gameCachePlayer.containsKey(pv)) {
            return null;
        }
        final Player p = gameCachePlayer.get(pv);

        final CardCollection typeList = CardLists.filter(list, CardPredicates.isOwner(p));
        final int count = typeList.size();
        if (count < nNeeded) {
            return null;
        }

        GameEntityViewMap<Card, CardView> gameCacheExile = GameEntityView.getMap(typeList);
        List<CardView> views = controller.getGui().many(
                Localizer.getInstance().getMessage("lblExileFromZone", cost.getFrom().getTranslatedName()),
                Localizer.getInstance().getMessage("lblToBeExiled"), nNeeded, gameCacheExile.getTrackableKeys(), null);
        List<Card> result = Lists.newArrayList();
        gameCacheExile.addToList(views, result);
        return PaymentDecision.card(result);
    }

    @Override
    public PaymentDecision visit(final CostExileFromStack cost) {
        final Game game = player.getGame();

        final String type = cost.getType();
        final List<SpellAbility> saList = new ArrayList<>();
        final List<String> descList = new ArrayList<>();

        for (final SpellAbilityStackInstance si : game.getStack()) {
            final Card stC = si.getSourceCard();
            final SpellAbility stSA = si.getSpellAbility(true).getRootAbility();
            if (stC.isValid(cost.getType().split(";"), ability.getActivatingPlayer(), source, ability) && stSA.isSpell()) {
                saList.add(stSA);
                if (stC.isCopiedSpell()) {
                    descList.add(stSA.getStackDescription() + " (Copied Spell)");
                } else {
                    descList.add(stSA.getStackDescription());
                }
            }
        }

        if (type.equals("All")) {
            return PaymentDecision.spellabilities(saList);
        }
        int c = cost.getAbilityAmount(ability);

        if (saList.size() < c) {
            return null;
        }

        final List<SpellAbility> exiled = new ArrayList<>();
        for (int i = 0; i < c; i++) {
            //Have to use the stack descriptions here because some copied spells have no description otherwise
            final String o = controller.getGui().oneOrNone(Localizer.getInstance().getMessage("lblExileFromStack"), descList);

            if (o != null) {
                final SpellAbility toExile = saList.get(descList.indexOf(o));

                saList.remove(toExile);
                descList.remove(o);

                exiled.add(toExile);
            } else {
                return null;
            }
        }
        return PaymentDecision.spellabilities(exiled);
    }

    private PaymentDecision exileFromTop(final CostExile cost, final SpellAbility sa, final Player player, final int nNeeded) {
        final CardCollectionView list = player.getCardsIn(ZoneType.Library, nNeeded);

        if (list.size() > nNeeded || !confirmAction(Localizer.getInstance().getMessage("lblExileNCardFromYourTopLibraryConfirm"))/*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblExileNCardFromYourTopLibraryConfirm"), ability)*/) {
            return null;
        }
        return PaymentDecision.card(list);
    }

    private PaymentDecision exileFromMiscZone(final CostExile cost, final SpellAbility sa, final int nNeeded, final CardCollection typeList) {
        if (typeList.size() < nNeeded) { return null; }

        final List<ZoneType> origin = Lists.newArrayList();
        origin.add(cost.from);
        final CardCollection exiled = new CardCollection();

        final List<Card> chosen = controller.chooseCardsForZoneChange(ZoneType.Exile, origin, sa, typeList, mandatory ? nNeeded : 0,
                nNeeded, null, cost.toString(), null);

        exiled.addAll(chosen);
        if (exiled.size() < nNeeded) {
            return null;
        }
        return PaymentDecision.card(exiled);
    }

    private PaymentDecision exileFromTopGraveType(final SpellAbility sa, final int nNeeded, final CardCollection typeList) {
        if (typeList.size() < nNeeded) { return null; }

        Collections.reverse(typeList);
        return PaymentDecision.card(Iterables.limit(typeList, nNeeded));
    }

    @Override
    public PaymentDecision visit(final CostExiledMoveToGrave cost) {
        int c = cost.getAbilityAmount(ability);

        final Player activator = ability.getActivatingPlayer();
        final CardCollection list = CardLists.getValidCards(activator.getGame().getCardsIn(ZoneType.Exile),
                cost.getType().split(";"), activator, source, ability);

        if (list.size() < c) {
            return null;
        }
        Integer min = c;
        if (ability.isOptionalTrigger()) {
            min = 0;
        }
        GameEntityViewMap<Card, CardView> gameCacheExile = GameEntityView.getMap(list);
        List<CardView> views = controller.getGui().many(
                Localizer.getInstance().getMessage("lblChooseAnExiledCardPutIntoGraveyard"),
                Localizer.getInstance().getMessage("lblToGraveyard"), min, c, CardView.getCollection(list), CardView.get(source));

        if (views == null || views.size() < c) {
            return null;
        }
        List<Card> result = Lists.newArrayList();
        gameCacheExile.addToList(views, result);
        return PaymentDecision.card(result);
    }

    @Override
    public PaymentDecision visit(final CostExert cost) {
        final String type = cost.getType();

        CardCollectionView list = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), type.split(";"), player, source, ability);

        if (cost.payCostFromSource()) {
            if (source.getController() == ability.getActivatingPlayer() && source.isInPlay()) {
                return confirmAction(Localizer.getInstance().getMessage("lblExertCardConfirm", CardTranslation.getTranslatedName(source.getName())))/*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblExertCardConfirm", CardTranslation.getTranslatedName(source.getName())), ability)*/ ? PaymentDecision.card(source) : null;
            }
            return null;
        }

        int c = cost.getAbilityAmount(ability);
        if (0 == c) {
            return PaymentDecision.number(0);
        }
        if (list.size() < c) {
            return null;
        }
        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, list, ability);
        inp.setMessage(Localizer.getInstance().getMessage("lblSelectACostToExert", cost.getDescriptiveType(), "%d"));
        inp.setCancelAllowed(true);
        inp.showAndWait();
        if (inp.hasCancelled()) {
            return null;
        }

        return PaymentDecision.card(inp.getSelected());

    }

    @Override
    public PaymentDecision visit(final CostFlipCoin cost) {
        Integer c = cost.getAbilityAmount(ability);

        if (!confirmAction(Localizer.getInstance().getMessage("lblDoYouWantFlipNCoinAction", String.valueOf(c)))/*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblDoYouWantFlipNCoinAction", String.valueOf(c)), ability)*/) {
            return null;
        }

        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(final CostRollDice cost) {
        int c = cost.getAbilityAmount(ability);

        if (!confirmAction(Localizer.getInstance().getMessage("lblDoYouWantRollNDiceAction", String.valueOf(c), "d" + cost.getType()))/*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblDoYouWantRollNDiceAction", String.valueOf(c), "d" + cost.getType()), ability)*/) {
            return null;
        }

        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(final CostGainControl cost) {
        int c = cost.getAbilityAmount(ability);

        final CardCollectionView list = player.getCardsIn(ZoneType.Battlefield);
        final CardCollectionView validCards = CardLists.getValidCards(list, cost.getType().split(";"), player, source, ability);

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, validCards, ability);
        final String desc = cost.getTypeDescription() == null ? cost.getType() : cost.getTypeDescription();
        inp.setMessage(Localizer.getInstance().getMessage("lblGainNTargetControl", "%d", desc));
        inp.showAndWait();
        if (inp.hasCancelled()) {
            return null;
        }
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(final CostGainLife cost) {
        int c = cost.getAbilityAmount(ability);

        final List<Player> oppsThatCanGainLife = new ArrayList<>();
        for (final Player opp : cost.getPotentialTargets(player, ability)) {
            if (opp.canGainLife()) {
                oppsThatCanGainLife.add(opp);
            }
        }

        if (cost.getCntPlayers() == Integer.MAX_VALUE) {
            return PaymentDecision.players(oppsThatCanGainLife);
        }

        GameEntityViewMap<Player, PlayerView> gameCachePlayer = GameEntityView.getMap(oppsThatCanGainLife);
        final PlayerView pv = controller.getGui().oneOrNone(Localizer.getInstance().getMessage("lblCardChooseAnOpponentToGainNLife", CardTranslation.getTranslatedName(source.getName()), String.valueOf(c)), gameCachePlayer.getTrackableKeys());
        if (pv == null || !gameCachePlayer.containsKey(pv)) {
            return null;
        }
        return PaymentDecision.players(Lists.newArrayList(gameCachePlayer.get(pv)));
    }

    @Override
    public PaymentDecision visit(final CostMill cost) {
        Integer c = cost.getAbilityAmount(ability);

        String message = null;
        if (orString != null && !orString.isEmpty()) {
            message = Localizer.getInstance().getMessage("lblDoYouWantMillNCardsOrDoAction", String.valueOf(c), orString);
        } else {
            message = Localizer.getInstance().getMessage("lblMillNCardsFromYourLibraryConfirm", String.valueOf(c));
        }

        if (!confirmAction(message)/*controller.confirmPayment(cost, message, ability)*/) {
            return null;
        }
        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(final CostPayLife cost) {
        Integer c = cost.getAbilityAmount(ability);

        if (mandatory) {
            return PaymentDecision.number(c);
        }

        String message = null;
        if (orString != null && !orString.isEmpty()) {
            message = Localizer.getInstance().getMessage("lblDoYouWantPayNLife", String.valueOf(c), orString);
        } else {
            message = Localizer.getInstance().getMessage("lblPayNLifeConfirm", String.valueOf(c));
        }

        // for costs declared mandatory, this is only reachable with a valid amount
        if (player.canPayLife(c, isEffect(), ability) && confirmAction(message)/*controller.confirmPayment(cost, message, ability)*/) {
            //force mandatory if paylife is paid.. todo add check if all can be paid
            mandatory = true;
            return PaymentDecision.number(c);
        }
        return null;
    }

    @Override
    public PaymentDecision visit(final CostPayEnergy cost) {
        Integer c = cost.getAbilityAmount(ability);

        if (player.canPayEnergy(c) &&
                confirmAction(Localizer.getInstance().getMessage("lblPayEnergyConfirm", cost.toString(), String.valueOf(player.getCounters(CounterEnumType.ENERGY)), "{E}"))/*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblPayEnergyConfirm", cost.toString(), String.valueOf(player.getCounters(CounterEnumType.ENERGY)), "{E}"), ability)*/) {
            return PaymentDecision.number(c);
        }
        return null;
    }

    @Override
    public PaymentDecision visit(final CostPartMana cost) {
        // only interactive payment possible for now =(
        return new PaymentDecision(0);
    }

    @Override
    public PaymentDecision visit(final CostPutCardToLib cost) {
        Integer c = cost.getAbilityAmount(ability);

        final CardCollection list = CardLists.getValidCards(cost.sameZone ? player.getGame().getCardsIn(cost.getFrom()) :
                player.getCardsIn(cost.getFrom()), cost.getType().split(";"), player, source, ability);

        if (cost.payCostFromSource()) {
            return source.getZone() == player.getZone(cost.from) && confirmAction(Localizer.getInstance().getMessage("lblPutCardToLibraryConfirm", CardTranslation.getTranslatedName(source.getName())))/*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblPutCardToLibraryConfirm", CardTranslation.getTranslatedName(source.getName())), ability)*/ ? PaymentDecision.card(source) : null;
        }

        if (cost.from == ZoneType.Hand) {
            final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, list, ability);
            inp.setMessage(Localizer.getInstance().getMessage("lblPutNCardsFromYourZone", "%d", cost.from.getTranslatedName()));
            inp.setCancelAllowed(true);
            inp.showAndWait();
            return inp.hasCancelled() ? null : PaymentDecision.card(inp.getSelected());
        }

        if (cost.sameZone) {
            final FCollectionView<Player> players = player.getGame().getPlayers();
            final List<Player> payableZone = new ArrayList<>();
            for (final Player p : players) {
                final CardCollectionView enoughType = CardLists.filter(list, CardPredicates.isOwner(p));
                if (enoughType.size() < c) {
                    list.removeAll(enoughType);
                } else {
                    payableZone.add(p);
                }
            }
            return putFromSame(list, c.intValue(), payableZone, cost.from);
        } else {//Graveyard
            return putFromMiscZone(ability, c.intValue(), list, cost.from);
        }
    }

    private PaymentDecision putFromMiscZone(final SpellAbility sa, final int nNeeded, final CardCollection typeList, final ZoneType fromZone) {
        if (typeList.size() < nNeeded) {
            return null;
        }

        final CardCollection chosen = new CardCollection();
        GameEntityViewMap<Card, CardView> gameCacheCard = GameEntityView.getMap(typeList);
        for (int i = 0; i < nNeeded; i++) {
            final CardView cv = controller.getGui().oneOrNone(Localizer.getInstance().getMessage("lblFromZonePutToLibrary", fromZone.getTranslatedName()), gameCacheCard.getTrackableKeys());
            if (cv == null || !gameCacheCard.containsKey(cv)) {
                return null;
            }
            chosen.add(gameCacheCard.remove(cv));
        }
        return PaymentDecision.card(chosen);
    }

    private PaymentDecision putFromSame(final CardCollectionView list, final int nNeeded, final List<Player> payableZone, final ZoneType fromZone) {
        if (nNeeded == 0) {
            return PaymentDecision.number(0);
        }

        GameEntityViewMap<Player, PlayerView> gameCachePlayer = GameEntityView.getMap(payableZone);
        PlayerView pv = SGuiChoose.oneOrNone(TextUtil.concatNoSpace(Localizer.getInstance().getMessage("lblPutCardsFromWhoseZone"), fromZone.getTranslatedName()), gameCachePlayer.getTrackableKeys());
        if (pv == null || !gameCachePlayer.containsKey(pv)) {
            return null;
        }
        Player p = gameCachePlayer.get(pv);

        final CardCollection typeList = CardLists.filter(list, CardPredicates.isOwner(p));
        if (typeList.size() < nNeeded) {
            return null;
        }

        final CardCollection chosen = new CardCollection();
        GameEntityViewMap<Card, CardView> gameCacheCard = GameEntityView.getMap(typeList);
        for (int i = 0; i < nNeeded; i++) {
            final CardView cv = controller.getGui().oneOrNone(Localizer.getInstance().getMessage("lblPutZoneCardsToLibrary", fromZone.getTranslatedName()), gameCacheCard.getTrackableKeys());
            if (cv == null || !gameCacheCard.containsKey(cv)) {
                return null;
            }
            chosen.add(gameCacheCard.remove(cv));
        }
        return PaymentDecision.card(chosen);
    }

    @Override
    public PaymentDecision visit(final CostPutCounter cost) {
        final int c = cost.getAbilityAmount(ability);

        if (cost.payCostFromSource()) {
            // UnlessCost so player might not want to pay (Fabricate)
            if (ability.hasParam("UnlessCost") && !confirmAction(Localizer.getInstance().getMessage("lblPutNTypeCounterOnTarget", String.valueOf(c), cost.getCounter().getName(), ability.getHostCard().getName()))/*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblPutNTypeCounterOnTarget", String.valueOf(c), cost.getCounter().getName(), ability.getHostCard().getName()), ability)*/) {
                return null;
            }
            cost.setLastPaidAmount(c);
            return PaymentDecision.number(c);
        }

        // Cards to use this branch: Scarscale Ritual, Wandering Mage - each adds only one counter
        final CardCollectionView typeList = CardLists.getValidCards(source.getGame().getCardsIn(ZoneType.Battlefield),
                cost.getType().split(";"), player, ability.getHostCard(), ability);

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, typeList, ability);
        inp.setMessage(Localizer.getInstance().getMessage("lblPutNTypeCounterOnTarget", String.valueOf(c), cost.getCounter().getName(), cost.getDescriptiveType()));
        inp.setCancelAllowed(!mandatory);
        inp.showAndWait();

        if (inp.hasCancelled()) {
            return null;
        }
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(final CostReturn cost) {
        int c = cost.getAbilityAmount(ability);

        if (cost.payCostFromSource()) {
            final Card card = ability.getHostCard();
            if (card.getController() == player && card.isInPlay()) {
                final CardView view = CardView.get(card);
                return confirmAction(Localizer.getInstance().getMessage("lblReturnCardToHandConfirm", CardTranslation.getTranslatedName(view.getName())))/*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblReturnCardToHandConfirm", CardTranslation.getTranslatedName(view.getName())), ability)*/ ? PaymentDecision.card(card) : null;
            }
        } else {
            final CardCollectionView validCards = CardLists.getValidCards(ability.getActivatingPlayer().getCardsIn(ZoneType.Battlefield),
                    cost.getType().split(";"), player, source, ability);

            final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, validCards, ability);
            inp.setCancelAllowed(!mandatory);
            inp.setMessage(Localizer.getInstance().getMessage("lblNTypeCardsToHand", "%d", cost.getDescriptiveType()));
            inp.showAndWait();
            if (inp.hasCancelled()) {
                return null;
            }
            return PaymentDecision.card(inp.getSelected());
        }
        return null;
    }

    @Override
    public PaymentDecision visit(final CostReveal cost) {
        if (cost.payCostFromSource()) {
            return PaymentDecision.card(source);
        }
        if (cost.getType().equals("Hand")) {
            return PaymentDecision.card(player.getCardsIn(ZoneType.Hand));
        }
        InputSelectCardsFromList inp = null;
        if (cost.getType().equals("SameColor")) {
            final Integer num = cost.getAbilityAmount(ability);
            CardCollectionView hand = player.getCardsIn(cost.getRevealFrom());
            final CardCollectionView hand2 = hand;
            hand = CardLists.filter(hand, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    for (final Card card : hand2) {
                        if (!card.equals(c) && card.sharesColorWith(c)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            if (num == 0) {
                return PaymentDecision.number(0);
            }
            inp = new InputSelectCardsFromList(controller, num, hand, ability) {
                private static final long serialVersionUID = 8338626212893374798L;

                @Override
                protected boolean onCardSelected(final Card c, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
                    final Card firstCard = Iterables.getFirst(this.selected, null);
                    if (firstCard != null && !CardPredicates.sharesColorWith(firstCard).apply(c)) {
                        return false;
                    }
                    return super.onCardSelected(c, otherCardsToSelect, triggerEvent);
                }
            };
            inp.setMessage(Localizer.getInstance().getMessage("lblSelectNCardOfSameColorToReveal", String.valueOf(num)));
        } else {
            Integer num = cost.getAbilityAmount(ability);

            CardCollectionView hand = player.getCardsIn(cost.getRevealFrom());
            hand = CardLists.getValidCards(hand, cost.getType().split(";"), player, source, ability);

            if (hand.size() < num) {
                return null;
            }
            if (num == 0) {
                return PaymentDecision.number(0);
            }
            // player might not want to pay if from a trigger
            if (!ability.hasSVar("IsCastFromPlayEffect") && hand.size() == num) {
                return PaymentDecision.card(hand);
            }

            inp = new InputSelectCardsFromList(controller, num, num, hand, ability);
            inp.setMessage(Localizer.getInstance().getMessage("lblSelectNMoreTypeCardsTpReveal", "%d", cost.getDescriptiveType()));
        }
        inp.setCancelAllowed(!mandatory);
        inp.showAndWait();
        if (inp.hasCancelled()) {
            return null;
        }
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(final CostRevealChosenPlayer cost) {
        return PaymentDecision.number(1);
    }

    @Override
    public PaymentDecision visit(final CostRemoveAnyCounter cost) {
        int c = cost.getAbilityAmount(ability);
        final String type = cost.getType();


        CardCollectionView list = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), type.split(";"), player, source, ability);
        list = CardLists.filter(list, CardPredicates.hasCounters());

        final InputSelectCardToRemoveCounter inp = new InputSelectCardToRemoveCounter(controller, c, cost, cost.counter, list, ability);
        inp.setCancelAllowed(true);
        inp.showAndWait();
        if (inp.hasCancelled()) {
            return null;
        }

        return PaymentDecision.counters(inp.getCounterTable());
    }

    public static final class InputSelectCardToRemoveCounter extends InputSelectManyBase<GameEntity> {
        private static final long serialVersionUID = 2685832214519141903L;

        private final CounterType counterType;
        private final CardCollectionView validChoices;

        private final GameEntityCounterTable counterTable = new GameEntityCounterTable();

        public InputSelectCardToRemoveCounter(final PlayerControllerHuman controller, final int cntCounters, final CostPart costPart, final CounterType cType, final CardCollectionView validCards, final SpellAbility sa) {
            super(controller, cntCounters, cntCounters, sa);
            this.validChoices = validCards;
            counterType = cType;
            String fromWhat = costPart.getDescriptiveType();
            if (fromWhat.equals("CARDNAME")) {
                fromWhat = sa.getHostCard().getName();
            }

            setMessage(Localizer.getInstance().getMessage("lblRemoveNTargetCounterFromCardPayCostConfirm",
                    "%d", counterType == null ? "" : " " + counterType.getName().toLowerCase(), fromWhat));
        }

        @Override
        protected boolean onCardSelected(final Card c, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
            if (!isValidChoice(c)) {
                return false;
            }

            CounterType cType = this.counterType;
            if (cType == null) {
                Map<CounterType, Integer> cmap = counterTable.filterToRemove(c);

                String prompt = Localizer.getInstance().getMessage("lblSelectCountersTypeToRemove");

                cType = getController().chooseCounterType(Lists.newArrayList(cmap.keySet()), sa, prompt, null);
            }

            if (cType == null) {
                return false;
            }

            if (c.getCounters(cType) <= counterTable.get(null, c, cType)) {
                return false;
            }

            counterTable.put(null, c, cType, 1);

            onSelectStateChanged(c, true);
            refresh();
            return true;
        }

        @Override
        public String getActivateAction(final Card c) {
            if (!isValidChoice(c)) {
                return null;
            }
            if (counterType != null) {
                if (c.getCounters(counterType) <= counterTable.get(null, c, counterType)) {
                    return null;
                }
            } else {
                boolean found = false;
                for (Map.Entry<CounterType, Integer> e : c.getCounters().entrySet()) {
                    if (e.getValue() > counterTable.get(null, c, e.getKey())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return null;
                }
            }
            return Localizer.getInstance().getMessage("lblRemoveCounterFromCard");
        }

        @Override
        protected boolean hasEnoughTargets() {
            return hasAllTargets();
        }

        @Override
        protected boolean hasAllTargets() {
            final int sum = getDistibutedCounters();
            return sum >= max;
        }

        @Override
        protected String getMessage() {
            return max == Integer.MAX_VALUE
                    ? String.format(message, getDistibutedCounters())
                    : String.format(message, max - getDistibutedCounters());
        }

        private int getDistibutedCounters() {
            return counterTable.totalValues();
        }

        protected final boolean isValidChoice(final GameEntity choice) {
            return validChoices.contains(choice);
        }

        public GameEntityCounterTable getCounterTable() {
            return this.counterTable;
        }

        @Override
        public Collection<GameEntity> getSelected() {
            return counterTable.columnKeySet();
        }
    }

    @Override
    public PaymentDecision visit(final CostRemoveCounter cost) {
        final String amount = cost.getAmount();
        final String type = cost.getType();

        int cntRemoved = 1;
        if (!amount.equals("All")) {
            cntRemoved = cost.getAbilityAmount(ability);
        }

        if (cost.payCostFromSource()) {
            final int maxCounters = source.getCounters(cost.counter);
            if (amount.equals("All")) {
                if (!InputConfirm.confirm(controller, ability, Localizer.getInstance().getMessage("lblRemoveAllCountersConfirm"))) {
                    return null;
                }
                cntRemoved = maxCounters;
            } else if (ability != null && !ability.isPwAbility()) {
                // ignore Planeswalker abilities for this
                if (maxCounters < cntRemoved) {
                    return null;
                }
                if (!confirmAction(Localizer.getInstance().getMessage("lblRemoveNTargetCounterFromCardPayCostConfirm", amount, cost.counter.getName().toLowerCase(), CardTranslation.getTranslatedName(source.getName())))/*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblRemoveNTargetCounterFromCardPayCostConfirm", amount, cost.counter.getName().toLowerCase(), CardTranslation.getTranslatedName(source.getName())), ability)*/) {
                    return null;
                }
            }

            if (maxCounters < cntRemoved) {
                return null;
            }
            return PaymentDecision.card(source, cntRemoved >= 0 ? cntRemoved : maxCounters);

        } else if (type.equals("OriginalHost")) {
            final int maxCounters = ability.getOriginalHost().getCounters(cost.counter);
            if (amount.equals("All")) {
                cntRemoved = maxCounters;
            }
            if (maxCounters < cntRemoved) {
                return null;
            }

            return PaymentDecision.card(ability.getOriginalHost(), cntRemoved >= 0 ? cntRemoved : maxCounters);
        }

        CardCollectionView validCards = CardLists.getValidCards(player.getCardsIn(cost.zone), type.split(";"), player, source, ability);
        // you can only select 1 card to remove N counters from
        validCards = CardLists.filter(validCards, CardPredicates.hasCounter(cost.counter, cntRemoved));
        if (validCards.isEmpty()) {
            return null;
        }

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, validCards, ability);
        inp.setMessage(Localizer.getInstance().getMessage("lblRemoveCountersFromAInZoneCard", cost.zone.getTranslatedName()));
        inp.setCancelAllowed(true);
        inp.showAndWait();

        if (inp.hasCancelled()) {
            return null;
        }

        final Card selected = inp.getFirstSelected();
        if (selected == null) {
            return null;
        }

        return PaymentDecision.card(selected, cntRemoved);
    }

    @Override
    public PaymentDecision visit(final CostSacrifice cost) {
        final String amount = cost.getAmount();
        final String type = cost.getType();

        CardCollectionView list = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), CardPredicates.canBeSacrificedBy(ability, isEffect()));
        list = CardLists.getValidCards(list, type.split(";"), player, source, ability);

        if (cost.payCostFromSource()) {
            if (source.getController() == ability.getActivatingPlayer() && source.isInPlay()) {
                return mandatory || confirmAction(Localizer.getInstance().getMessage("lblSacrificeCardConfirm", CardTranslation.getTranslatedName(source.getName()))) /*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblSacrificeCardConfirm", CardTranslation.getTranslatedName(source.getName())), ability)*/ ? PaymentDecision.card(source) : null;
            }
            return null;
        }

        if (type.equals("OriginalHost")) {
            Card host = ability.getOriginalHost();
            if (host.getController() == ability.getActivatingPlayer() && host.isInPlay()) {
                return confirmAction(Localizer.getInstance().getMessage("lblSacrificeCardConfirm", CardTranslation.getTranslatedName(host.getName()))) /*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblSacrificeCardConfirm", CardTranslation.getTranslatedName(host.getName())), ability)*/ ? PaymentDecision.card(host) : null;
            }
            return null;
        }

        if (amount.equals("All")) {
            return PaymentDecision.card(list);
        }

        int c = cost.getAbilityAmount(ability);
        if (0 == c) {
            return PaymentDecision.number(0);
        }
        if (list.size() < c) {
            return null;
        }
        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, list, ability);
        inp.setMessage(Localizer.getInstance().getMessage("lblSelectATargetToSacrifice", cost.getDescriptiveType(), "%d"));
        inp.setCancelAllowed(!mandatory);
        inp.showAndWait();
        if (inp.hasCancelled()) {
            return null;
        }

        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(final CostTap cost) {
        // if (!canPay(ability, source, ability.getActivatingPlayer(),
        // payment.getCost()))
        // return false;
        return PaymentDecision.number(1);
    }

    @Override
    public PaymentDecision visit(final CostTapType cost) {
        String type = cost.getType();
        final String amount = cost.getAmount();

        boolean sameType = false;
        if (type.contains(".sharesCreatureTypeWith")) {
            sameType = true;
            type = TextUtil.fastReplace(type, ".sharesCreatureTypeWith", "");
        }

        boolean totalPower = false;
        String totalP = "";
        if (type.contains("+withTotalPowerGE")) {
            totalPower = true;
            totalP = type.split("withTotalPowerGE")[1];
            type = TextUtil.fastReplace(type, TextUtil.concatNoSpace("+withTotalPowerGE", totalP), "");
        }

        CardCollection typeList = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), type.split(";"), player,
                source, ability);
        typeList = CardLists.filter(typeList, Presets.UNTAPPED);

        if (ability.hasParam("Crew")) {
            typeList = CardLists.getNotKeyword(typeList, "CARDNAME can't crew Vehicles.");
        }

        Integer c = null;
        if (!amount.equals("Any")) {
            c = cost.getAbilityAmount(ability);
        }

        if (c != null && c == 0) {
            return PaymentDecision.number(0);
        }

        if (sameType) {
            final CardCollection list2 = typeList;
            typeList = CardLists.filter(typeList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    for (final Card card : list2) {
                        if (!card.equals(c) && card.sharesCreatureTypeWith(c)) {
                            return true;
                        }
                    }
                    return false;
                }
            });

            final CardCollection tapped = new CardCollection();
            while (c > 0) {
                final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, typeList, ability);
                inp.setMessage(Localizer.getInstance().getMessage("lblSelectOneOfCardsToTapAlreadyChosen", tapped));
                inp.setCancelAllowed(true);
                inp.showAndWait();
                if (inp.hasCancelled()) {
                    return null;
                }
                final Card first = inp.getFirstSelected();
                tapped.add(first);
                typeList = CardLists.filter(typeList, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return c.sharesCreatureTypeWith(first);
                    }
                });
                typeList.remove(first);
                c--;
            }
            return PaymentDecision.card(tapped);
        }

        if (totalPower) {
            final int i = Integer.parseInt(totalP);
            final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 0, typeList.size(), typeList, ability);
            inp.setMessage(Localizer.getInstance().getMessage("lblSelectACreatureToTap"));
            inp.setCancelAllowed(true);
            inp.showAndWait();

            if (inp.hasCancelled() || CardLists.getTotalPower(inp.getSelected(), true, ability.hasParam("Crew")) < i) {
                return null;
            }
            return PaymentDecision.card(inp.getSelected());
        }

        if (c > typeList.size()) {
            controller.getGui().message(Localizer.getInstance().getMessage("lblEnoughValidCardNotToPayTheCost"), Localizer.getInstance().getMessage("lblCostPaymentInvalid"));
            return null; // not enough targets anymore (e.g. Crackleburr + Smokebraider tapped to get mana)
        }

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, typeList, ability);
        inp.setCancelAllowed(!mandatory);
        inp.setMessage(Localizer.getInstance().getMessage("lblSelectATargetToTap", cost.getDescriptiveType(), "%d"));
        inp.showAndWait();
        if (inp.hasCancelled()) {
            return null;
        }
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(final CostUntapType cost) {
        CardCollection typeList = CardLists.getValidCards(player.getGame().getCardsIn(ZoneType.Battlefield), cost.getType().split(";"),
                player, source, ability);
        typeList = CardLists.filter(typeList, Presets.TAPPED);
        if (!cost.canUntapSource) {
            typeList.remove(source);
        }
        int c = cost.getAbilityAmount(ability);
        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, typeList, ability);
        inp.setCancelAllowed(true);
        inp.setMessage(Localizer.getInstance().getMessage("lblSelectATargetToUntap", cost.getDescriptiveType(), "%d"));
        inp.showAndWait();
        if (inp.hasCancelled() || inp.getSelected().size() != c) {
            return null;
        }
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(final CostUntap cost) {
        return PaymentDecision.number(1);
    }

    @Override
    public PaymentDecision visit(final CostUnattach cost) {
        final Card cardToUnattach = cost.findCardToUnattach(source, player, ability);
        if (cardToUnattach != null && confirmAction(Localizer.getInstance().getMessage("lblUnattachCardConfirm", CardTranslation.getTranslatedName(cardToUnattach.getName()))) /*controller.confirmPayment(cost, Localizer.getInstance().getMessage("lblUnattachCardConfirm", CardTranslation.getTranslatedName(cardToUnattach.getName())), ability)*/) {
            return PaymentDecision.card(cardToUnattach);
        }
        return null;
    }

    @Override
    public boolean paysRightAfterDecision() {
        return true;
    }
    private boolean confirmAction(String message) {
        CardView cardView = ability.getCardView();
        //for cards like Sword-Point Diplomacy and others that uses imprinted as container for their ability
        if (cardView != null && cardView.getImprintedCards() != null && cardView.getImprintedCards().size() == 1)
            cardView = CardView.getCardForUi(ImageUtil.getPaperCardFromImageKey(cardView.getImprintedCards().get(0).getCurrentState().getImageKey()));
        else if (ability.getTargets() != null && ability.getTargets().isTargetingAnyCard() && ability.getTargets().size() == 1)
            cardView = CardView.get(ability.getTargetCard());
        return controller.getGui().confirm(cardView, message);
    }
}
