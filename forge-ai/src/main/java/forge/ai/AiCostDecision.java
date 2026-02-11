package forge.ai;

import com.google.common.collect.Lists;

import forge.ai.AiCardMemory.MemorySet;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.cost.*;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;

import java.util.*;

import static forge.ai.ComputerUtilCard.getBestCreatureAI;
import static forge.ai.ComputerUtilCard.getWorstCreatureAI;

public class AiCostDecision extends CostDecisionMakerBase {
    private final CardCollection discarded;
    private final CardCollection tapped;

    public AiCostDecision(Player ai0, SpellAbility sa, final boolean effect) {
        this(ai0, sa, effect, false);
    }
    public AiCostDecision(Player ai0, SpellAbility sa, final boolean effect, final boolean payMana) {
        super(ai0, effect, sa, sa.getHostCard());

        discarded = new CardCollection();
        tapped = new CardCollection();
        Set<Card> tappedForMana = AiCardMemory.getMemorySet(ai0, MemorySet.PAYS_TAP_COST);
        if (!payMana && tappedForMana != null) {
            tapped.addAll(tappedForMana);
        }
    }

    @Override
    public PaymentDecision visit(CostAddMana cost) {
        int c = cost.getAbilityAmount(ability);

        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(CostBehold cost) {
        final String type = cost.getType();
        CardCollectionView hand = player.getCardsIn(cost.getRevealFrom());
        hand = CardLists.getValidCards(hand, type.split(";"), player, source, ability);
        return hand.isEmpty() ? null : PaymentDecision.card(getBestCreatureAI(hand));
    }

    @Override
    public PaymentDecision visit(CostBeholdExile cost) {
        final String type = cost.getType();
        CardCollectionView hand = player.getCardsIn(cost.getRevealFrom());
        hand = CardLists.getValidCards(hand, type.split(";"), player, source, ability);
        return hand.isEmpty() ? null : PaymentDecision.card(getWorstCreatureAI(hand));
    }

    @Override
    public PaymentDecision visit(CostChooseColor cost) {
        int c = cost.getAbilityAmount(ability);
        return PaymentDecision.colors(player.getController().chooseColors("Color", ability, c, c, ColorSet.WUBRG));
    }

    @Override
    public PaymentDecision visit(CostChooseCreatureType cost) {
        String choice = player.getController().chooseSomeType("Creature", ability, CardType.getAllCreatureTypes());
        return PaymentDecision.type(choice);
    }

    @Override
    public PaymentDecision visit(CostCollectEvidence cost) {
        int c = cost.getAbilityAmount(ability);
        CardCollectionView chosen = ComputerUtil.chooseCollectEvidence(player, cost, source, c, ability, isEffect());

        return null == chosen ? null : PaymentDecision.card(chosen);
    }

    @Override
    public PaymentDecision visit(CostDiscard cost) {
        final String type = cost.getType();
        CardCollectionView hand = player.getCardsIn(ZoneType.Hand);

        if (type.equals("LastDrawn")) {
            if (!hand.contains(player.getLastDrawnCard())) {
                return null;
            }
            return PaymentDecision.card(player.getLastDrawnCard());
        } else if (cost.payCostFromSource()) {
            if (!hand.contains(source)) {
                return null;
            }

            return PaymentDecision.card(source);
        } else if (type.equals("Hand")) {
            if (hand.size() > 1 && ability.getActivatingPlayer() != null) {
                hand = ability.getActivatingPlayer().getController().orderMoveToZoneList(hand, ZoneType.Graveyard, ability);
            }
            return PaymentDecision.card(hand);
        }

        if (type.contains("WithSameName")) {
            return null;
        }
        int c = cost.getAbilityAmount(ability);

        if (type.equals("Random")) {
            CardCollectionView randomSubset = CardLists.getRandomSubList(new CardCollection(hand), c);
            if (randomSubset.size() > 1 && ability.getActivatingPlayer() != null) {
                randomSubset = ability.getActivatingPlayer().getController().orderMoveToZoneList(randomSubset, ZoneType.Graveyard, ability);
            }
            return PaymentDecision.card(randomSubset);
        } else if (type.contains("+WithDifferentNames")) {
            CardCollection differentNames = new CardCollection();
            CardCollection discardMe = CardLists.filter(hand, CardPredicates.hasSVar("DiscardMe"));
            while (c > 0) {
                Card chosen;
                if (!discardMe.isEmpty()) {
                    chosen = Aggregates.random(discardMe);
                    discardMe = CardLists.filter(discardMe, CardPredicates.sharesNameWith(chosen).negate());
                } else {
                    final Card worst = ComputerUtilCard.getWorstAI(hand);
                    chosen = worst != null ? worst : Aggregates.random(hand);
                }
                differentNames.add(chosen);
                hand = CardLists.filter(hand, CardPredicates.sharesNameWith(chosen).negate());
                c--;
            }
            return PaymentDecision.card(differentNames);
        } else {
            final AiController aic = ((PlayerControllerAi)player.getController()).getAi();

            CardCollection result = aic.getCardsToDiscard(c, type.split(";"), ability, discarded);
            if (result != null) {
                discarded.addAll(result);
            }
            return PaymentDecision.card(result);
        }
    }

    @Override
    public PaymentDecision visit(CostDamage cost) {
        int c = cost.getAbilityAmount(ability);

        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(CostDraw cost) {
        if (!cost.canPay(ability, player, isEffect())) {
            return null;
        }
        int c = cost.getAbilityAmount(ability);

        List<Player> res = cost.getPotentialPlayers(player, ability);

        PaymentDecision decision = PaymentDecision.players(res);
        decision.c = c;
        return decision;
    }

    @Override
    public PaymentDecision visit(CostPromiseGift cost) {
        if (!cost.canPay(ability, player, isEffect())) {
            return null;
        }
        List<Player> res = cost.getPotentialPlayers(player, ability);
        // I should only choose one of these right?
        // TODO Choose the "worst" player.
        Collections.shuffle(res);

        return PaymentDecision.players(res.subList(0, 1));
    }

    @Override
    public PaymentDecision visit(CostExile cost) {
        String type = cost.getType();
        if (cost.payCostFromSource()) {
            return PaymentDecision.card(source);
        }

        if (type.equals("All")) {
            return PaymentDecision.card(player.getCardsIn(cost.getFrom()));
        } else if (type.contains("FromTopGrave")) {
            return null;
        } else if (type.contains("+withTotalCMCGE")) {
            String strAmount = type.split("withTotalCMCGE")[1];
            int amount = AbilityUtils.calculateAmount(source, strAmount, ability);
            String typeCleaned = TextUtil.fastReplace(type, TextUtil.concatNoSpace("+withTotalCMCGE", strAmount), "");
            CardCollection valid = CardLists.getValidCards(player.getGame().getCardsIn(cost.getFrom().get(0)), typeCleaned, player, source, ability);
            CardCollection chosen = new CardCollection();

            CardLists.sortByCmcDesc(valid);
            Collections.reverse(valid);

            int totalCMC = 0;
            for (Card card : valid) {
                totalCMC += card.getCMC();
                chosen.add(card);
                if (totalCMC >= amount) {
                    return PaymentDecision.card(chosen);
                }
            }

            return null;
        }

        int c = cost.getAbilityAmount(ability);

        if (cost.from.size() == 1 && cost.getFrom().get(0).equals(ZoneType.Library)) {
            return PaymentDecision.card(player.getCardsIn(ZoneType.Library, c));
        }
        else if (cost.zoneRestriction == 0) {
            // TODO Determine exile from same zone for AI
            return null;
        } else {
            CardCollectionView chosen = ComputerUtil.chooseExileFrom(player, cost, source, c, ability, isEffect());
            return null == chosen ? null : PaymentDecision.card(chosen);
        }
    }

    @Override
    public PaymentDecision visit(CostExileFromStack cost) {
        List<SpellAbility> chosen = Lists.newArrayList();
        for (SpellAbilityStackInstance si :source.getGame().getStack()) {
            SpellAbility sp = si.getSpellAbility().getRootAbility();
            if (si.getSourceCard().isValid(cost.getType().split(";"), source.getController(), source, sp)) {
                chosen.add(sp);
            }
        }
        return chosen.isEmpty() ? null : PaymentDecision.spellabilities(chosen);
    }

    @Override
    public PaymentDecision visit(CostExiledMoveToGrave cost) {
        CardCollection chosen = new CardCollection();

        int c = cost.getAbilityAmount(ability);

        CardCollection typeList = CardLists.getValidCards(player.getGame().getCardsIn(ZoneType.Exile), cost.getType().split(";"), player, source, ability);

        if (typeList.size() < c) {
            return null;
        }

        CardLists.sortByPowerDesc(typeList);

        for (int i = 0; i < c; i++) {
            chosen.add(typeList.get(i));
        }

        return chosen.isEmpty() ? null : PaymentDecision.card(chosen);
    }

    @Override
    public PaymentDecision visit(CostExert cost) {
        if (cost.payCostFromSource()) {
            return PaymentDecision.card(source);
        }

        int c = cost.getAbilityAmount(ability);

        final CardCollection typeList = CardLists.getValidCards(player.getGame().getCardsIn(ZoneType.Battlefield), cost.getType().split(";"), player, source, ability);

        if (typeList.size() < c) {
            return null;
        }

        CardLists.sortByPowerAsc(typeList);
        final CardCollection res = new CardCollection();

        for (int i = 0; i < c; i++) {
            res.add(typeList.get(i));
        }
        return res.isEmpty() ? null : PaymentDecision.card(res);
    }

    @Override
    public PaymentDecision visit(final CostEnlist cost) {
        CardCollection choices = CostEnlist.getCardsForEnlisting(player);
        CardLists.sortByPowerDesc(choices);
        return choices.isEmpty() ? null : PaymentDecision.card(choices.getFirst());
    }

    @Override
    public PaymentDecision visit(CostFlipCoin cost) {
        int c = cost.getAbilityAmount(ability);
        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(final CostForage cost) {
        CardCollection food = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), CardPredicates.isType("Food"), CardPredicates.canBeSacrificedBy(ability, isEffect()));
        CardCollection exile = CardLists.filter(player.getCardsIn(ZoneType.Graveyard), CardPredicates.canExiledBy(ability, isEffect()));
        if (!food.isEmpty()) {
            final AiController aic = ((PlayerControllerAi)player.getController()).getAi();
            CardCollectionView list = aic.chooseSacrificeType("Food", ability, isEffect(), 1, null);
            return list == null ? null : PaymentDecision.card(list);
        } else {
            CardCollectionView chosen = ComputerUtil.chooseExileFromList(player, exile, source, 3, ability, isEffect());
            return null == chosen ? null : PaymentDecision.card(chosen);
        }
    }

    @Override
    public PaymentDecision visit(CostRollDice cost) {
        int c = cost.getAbilityAmount(ability);
        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(CostGainControl cost) {
        if (cost.payCostFromSource()) {
            return PaymentDecision.card(source);
        }

        int c = cost.getAbilityAmount(ability);

        CardCollection typeList = CardLists.getValidCards(player.getGame().getCardsIn(ZoneType.Battlefield), cost.getType().split(";"), player, source, ability);
        typeList = CardLists.filter(typeList, crd -> crd.canBeControlledBy(player));

        if (typeList.size() < c) {
            return null;
        }

        CardLists.sortByPowerAsc(typeList);
        final CardCollection res = new CardCollection();

        for (int i = 0; i < c; i++) {
            res.add(typeList.get(i));
        }
        return res.isEmpty() ? null : PaymentDecision.card(res);
    }

    @Override
    public PaymentDecision visit(CostGainLife cost) {
        final List<Player> oppsThatCanGainLife = Lists.newArrayList();

        for (final Player opp : cost.getPotentialTargets(player, ability)) {
            if (opp.canGainLife()) {
                oppsThatCanGainLife.add(opp);
            }
        }

        if (oppsThatCanGainLife.isEmpty()) {
            return null;
        }

        return PaymentDecision.players(oppsThatCanGainLife);
    }

    @Override
    public PaymentDecision visit(CostMill cost) {
        int c = cost.getAbilityAmount(ability);

        CardCollectionView topLib = player.getCardsIn(ZoneType.Library, c);
        return topLib.size() < c ? null : PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(CostPartMana cost) {
        return PaymentDecision.number(0);
    }

    @Override
    public PaymentDecision visit(CostPayLife cost) {
        int c = cost.getAbilityAmount(ability);
        if (!player.canPayLife(c, isEffect(), ability)) {
            return null;
        }
        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(CostPayEnergy cost) {
        int c = cost.getAbilityAmount(ability);
        if (!player.canPayEnergy(c)) {
            return null;
        }
        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(CostPutCardToLib cost) {
        if (cost.payCostFromSource()) {
            return PaymentDecision.card(source);
        }
        final Game game = player.getGame();
        CardCollection chosen = new CardCollection();
        CardCollectionView list;

        if (cost.isSameZone()) {
            list = game.getCardsIn(cost.getFrom());
        } else {
            list = player.getCardsIn(cost.getFrom());
        }

        int c = cost.getAbilityAmount(ability);

        list = CardLists.getValidCards(list, cost.getType().split(";"), player, source, ability);

        if (cost.isSameZone()) {
            // Jötun Grunt
            // TODO: improve AI
            final FCollectionView<Player> players = game.getPlayers();
            for (Player p : players) {
                CardCollectionView enoughType = CardLists.filter(list, CardPredicates.isOwner(p));
                if (enoughType.size() >= c) {
                    chosen.addAll(enoughType);
                    break;
                }
            }
            chosen = chosen.subList(0, c);
        } else {
            chosen = ComputerUtil.choosePutToLibraryFrom(player, cost.getFrom(), cost.getType(), source, ability.getTargetCard(), c, ability);
        }
        return chosen.isEmpty() ? null : PaymentDecision.card(chosen);
    }

    @Override
    public PaymentDecision visit(CostPutCounter cost) {
        if (cost.payCostFromSource()) {
            return PaymentDecision.card(source);
        }

        CardCollection typeList = CardLists.getValidCards(player.getGame().getCardsIn(ZoneType.Battlefield),
                cost.getType().split(";"), player, source, ability);
        typeList = CardLists.filter(typeList, CardPredicates.canReceiveCounters(cost.getCounter()));

        Card card;
        if (cost.getType().equals("Creature.YouCtrl")) {
            card = ComputerUtilCard.getWorstCreatureAI(typeList);
        } else {
            card = ComputerUtilCard.getWorstPermanentAI(typeList, false, false, false, false);
        }
        return PaymentDecision.card(card);
    }

    @Override
    public PaymentDecision visit(CostTap cost) {
        return PaymentDecision.number(0);
    }

    @Override
    public PaymentDecision visit(CostTapType cost) {
        String type = cost.getType();
        boolean isVehicle = type.contains("+withTotalPowerGE");

        CardCollection exclude = new CardCollection();
        exclude.addAll(tapped);

        if (type.contains("sharesCreatureTypeWith")) {
            return null;
        }

        String totalP = "";
        CardCollectionView totap;
        if (isVehicle) {
            totalP = type.split("withTotalPowerGE")[1];
            type = TextUtil.fastReplace(type, "+withTotalPowerGE", "");
            totap = ComputerUtil.chooseTapTypeAccumulatePower(player, type, ability, !cost.canTapSource, Integer.parseInt(totalP), exclude);
        } else {
            int c = cost.getAbilityAmount(ability);
            totap = ComputerUtil.chooseTapType(player, type, source, !cost.canTapSource, c, exclude, ability);
        }

        if (totap == null) {
            //System.out.println("Couldn't find a valid card(s) to tap for: " + source.getName());
            return null;
        }
        tapped.addAll(totap);
        return PaymentDecision.card(totap);
    }

    @Override
    public PaymentDecision visit(CostSacrifice cost) {
        if (cost.payCostFromSource()) {
            return PaymentDecision.card(source);
        }
        if (cost.getType().equals("OriginalHost")) {
            return PaymentDecision.card(ability.getOriginalHost());
        }
        if (cost.getAmount().equals("All")) {
            // Does the AI want to use Sacrifice All?
            return null;
        }

        int c = cost.getAbilityAmount(ability);

        final AiController aic = ((PlayerControllerAi)player.getController()).getAi();
        CardCollectionView list = aic.chooseSacrificeType(cost.getType(), ability, isEffect(), c, null);
        return list == null ? null : PaymentDecision.card(list);
    }

    @Override
    public PaymentDecision visit(CostReturn cost) {
        if (cost.payCostFromSource())
            return PaymentDecision.card(source);

        int c = cost.getAbilityAmount(ability);

        CardCollectionView res = ComputerUtil.chooseReturnType(player, cost.getType(), source, ability.getTargetCard(), c, ability);
        return res.isEmpty() ? null : PaymentDecision.card(res);
    }

    @Override
    public PaymentDecision visit(CostReveal cost) {
        final String type = cost.getType();
        CardCollectionView hand = player.getCardsIn(cost.getRevealFrom());

        if (cost.payCostFromSource()) {
            if (!hand.contains(source)) {
                return null;
            }
            return PaymentDecision.card(source);
        }

        if (cost.getType().equals("Hand")) {
            return PaymentDecision.card(hand);
        }

        if (cost.getRevealFrom().size() == 2 && cost.getRevealFrom().containsAll(Arrays.asList(ZoneType.Hand, ZoneType.Battlefield))) { // RevealOrChoose
            String aiLogic = ability.getParamOrDefault("AILogic", "");
            hand = CardLists.getValidCards(hand, type.split(";"), player, source, ability);

            if (aiLogic.startsWith("PowerAtLeast.")) {
                int minPower = Integer.parseInt(aiLogic.substring(aiLogic.indexOf(".") + 1));
                hand = CardLists.filterPower(hand, minPower);
            }

            return hand.isEmpty() ? null : PaymentDecision.card(getBestCreatureAI(hand));
        }

        if (cost.getType().equals("SameColor")) {
            return null;
        }

        if (cost.getRevealFrom().get(0).equals(ZoneType.Exile)) {
            hand = CardLists.getValidCards(hand, type.split(";"), player, source, ability);
            return PaymentDecision.card(getBestCreatureAI(hand));
        }

        int c = cost.getAbilityAmount(ability);

        final AiController aic = ((PlayerControllerAi)player.getController()).getAi();
        return PaymentDecision.card(aic.getCardsToDiscard(c, type.split(";"), ability));
    }

    @Override
    public PaymentDecision visit(CostRevealChosen cost) {
        return PaymentDecision.number(1);
    }

    protected int removeCounter(GameEntityCounterTable table, List<Card> prefs, CounterEnumType cType, int stillToRemove) {
        int removed = 0;
        if (!prefs.isEmpty() && stillToRemove > 0) {
            prefs.sort(CardPredicates.compareByCounterType(cType));

            for (Card prefCard : prefs) {
                // already enough removed
                if (stillToRemove <= removed) {
                    break;
                }
                int thisRemove = Math.min(prefCard.getCounters(cType), stillToRemove);
                if (thisRemove > 0) {
                    removed += thisRemove;
                    table.put(null, prefCard, cType, thisRemove);
                }
            }
        }
        return removed;
    }

    @Override
    public PaymentDecision visit(CostRemoveAnyCounter cost) {
        final int c = cost.getAbilityAmount(ability);
        final Card originalHost = Objects.requireNonNullElse(ability.getOriginalHost(), source);

        if (c <= 0) {
            return null;
        }

        CardCollectionView typeList = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), cost.getType().split(";"), player, source, ability);
        // only cards with counters are of interest
        typeList = CardLists.filter(typeList, CardPredicates.hasCounters());

        // no target
        if (typeList.isEmpty()) {
            return null;
        }

        // TODO fill up a GameEntityCounterTable
        // cost now has counter type or null
        // the amount might be different from 1, could be X
        // currently if amount is bigger than one,
        // it tries to remove all counters from one source and type at once

        int toRemove = 0;
        final GameEntityCounterTable table = new GameEntityCounterTable();

        // currently the only one using remove any counter using a type uses p1p1

        // the first things are benefit from removing counters

        // try to remove -1/-1 counter from persist creature
        if (c > toRemove && (cost.counter == null || cost.counter.is(CounterEnumType.M1M1))) {
            List<Card> prefs = CardLists.filter(typeList, CardPredicates.hasCounter(CounterEnumType.M1M1), CardPredicates.hasKeyword(Keyword.PERSIST));

            toRemove += removeCounter(table, prefs, CounterEnumType.M1M1, c - toRemove);
        }

        // try to remove +1/+1 counter from undying creature
        if (c > toRemove && (cost.counter == null || cost.counter.is(CounterEnumType.P1P1))) {
            List<Card> prefs = CardLists.filter(typeList, CardPredicates.hasCounter(CounterEnumType.P1P1), CardPredicates.hasKeyword(Keyword.UNDYING));

            toRemove += removeCounter(table, prefs, CounterEnumType.P1P1, c - toRemove);
        }

        if (c > toRemove && cost.counter == null && originalHost.hasSVar("AIRemoveCounterCostPriority") && !"ANY".equalsIgnoreCase(originalHost.getSVar("AIRemoveCounterCostPriority"))) {
            String[] counters = TextUtil.split(originalHost.getSVar("AIRemoveCounterCostPriority"), ',');

            for (final String ctr : counters) {
                CounterType ctype = CounterType.getType(ctr);
                // ctype == null means any type
                // any type is just used to return null for this

                for (Card card : CardLists.filter(typeList, CardPredicates.hasCounter(ctype))) {
                    int thisRemove = Math.min(card.getCounters(ctype), c - toRemove);
                    if (thisRemove > 0) {
                        toRemove += thisRemove;
                        table.put(null, card, ctype, thisRemove);
                    }
                }
            }
        }

        // filter for negative counters
        if (c > toRemove && cost.counter == null) {
            List<Card> negatives = CardLists.filter(typeList, crd -> {
                for (CounterType cType : table.filterToRemove(crd).keySet()) {
                    if (ComputerUtil.isNegativeCounter(cType, crd)) {
                        return true;
                    }
                }
                return false;
            });

            if (!negatives.isEmpty()) {
                // TODO sort negatives to remove from best Cards first?
                for (final Card crd : negatives) {
                    for (Map.Entry<CounterType, Integer> e : table.filterToRemove(crd).entrySet()) {
                        if (ComputerUtil.isNegativeCounter(e.getKey(), crd) && crd.canRemoveCounters(e.getKey())) {
                            int over = Math.min(e.getValue(), c - toRemove);
                            if (over > 0) {
                                toRemove += over;
                                table.put(null, crd, e.getKey(), over);
                            }
                        }
                    }
                }
            }
        }

        // filter for useless counters
        // they have no effect on the card, if they are there or removed
        if (c > toRemove && cost.counter == null) {
            List<Card> useless = CardLists.filter(typeList, crd -> {
                for (CounterType ctype : table.filterToRemove(crd).keySet()) {
                    if (ComputerUtil.isUselessCounter(ctype, crd)) {
                        return true;
                    }
                }
                return false;
            });

            if (!useless.isEmpty()) {
                for (final Card crd : useless) {
                    for (Map.Entry<CounterType, Integer> e : table.filterToRemove(crd).entrySet()) {
                        if (ComputerUtil.isUselessCounter(e.getKey(), crd)) {
                            int over = Math.min(e.getValue(), c - toRemove);
                            if (over > 0) {
                                toRemove += over;
                                table.put(null, crd, e.getKey(), over);
                            }
                        }
                    }
                }
            }
        }

        // try to remove Time counter from Chronozoa, it will generate more token
        if (c > toRemove && (cost.counter == null || cost.counter.is(CounterEnumType.TIME))) {
            List<Card> prefs = CardLists.filter(typeList, CardPredicates.hasCounter(CounterEnumType.TIME), CardPredicates.nameEquals("Chronozoa"));

            toRemove += removeCounter(table, prefs, CounterEnumType.TIME, c - toRemove);
        }

        // try to remove Quest counter on something with enough counters for the
        // effect to continue
        if (c > toRemove && (cost.counter == null || cost.counter.is(CounterEnumType.QUEST))) {
            List<Card> prefs = CardLists.filter(typeList, crd -> {
                // a Card without MaxQuestEffect doesn't need any Quest
                // counters
                int e = 0;
                if (crd.hasSVar("MaxQuestEffect")) {
                    e = Integer.parseInt(crd.getSVar("MaxQuestEffect"));
                }
                return crd.getCounters(CounterEnumType.QUEST) > e;
            });
            prefs.sort(Collections.reverseOrder(CardPredicates.compareByCounterType(CounterEnumType.QUEST)));

            for (final Card crd : prefs) {
                int e = 0;
                if (crd.hasSVar("MaxQuestEffect")) {
                    e = Integer.parseInt(crd.getSVar("MaxQuestEffect"));
                }
                int over = Math.min(crd.getCounters(CounterEnumType.QUEST) - e, c - toRemove);
                if (over > 0) {
                    toRemove += over;
                    table.put(null, crd, CounterEnumType.QUEST, over);
                }
            }
        }

        // remove Lore counters from Sagas to keep them longer
        if (c > toRemove && (cost.counter == null || cost.counter.is(CounterEnumType.LORE))) {
            List<Card> prefs = CardLists.filter(typeList, CardPredicates.hasCounter(CounterEnumType.LORE), CardPredicates.isType("Saga"));
            // TODO add Svars and other stuff to keep the Sagas on specific levels
            // also add a way for the AI to respond to the last Chapter ability to keep the Saga on the field if wanted
            toRemove += removeCounter(table, prefs, CounterEnumType.LORE, c - toRemove);
        }

        // TODO add logic to remove positive counters?
        if (c > toRemove && cost.counter != null) {
            // TODO add logic for Ooze Flux, should probably try to make a token as big as possible
            // without killing own non undying creatures in the process
            // the amount of X should probably be tweaked for this
            List<Card> withCtr = CardLists.filter(typeList, CardPredicates.hasCounter(cost.counter));
            for (Card card : withCtr) {
                int thisRemove = Math.min(card.getCounters(cost.counter), c - toRemove);
                if (thisRemove > 0) {
                    toRemove += thisRemove;
                    table.put(null, card, cost.counter, thisRemove);
                }
            }
        }

        // Used to not return null
        // Special part for CostPriority Any
        if (c > toRemove && cost.counter == null && originalHost.hasSVar("AIRemoveCounterCostPriority") && "ANY".equalsIgnoreCase(originalHost.getSVar("AIRemoveCounterCostPriority"))) {
            for (Card card : typeList) {
                // TODO try not to remove to much positive counters from the same card
                for (Map.Entry<CounterType, Integer> e : table.filterToRemove(card).entrySet()) {
                    int thisRemove = Math.min(e.getValue(), c - toRemove);
                    if (thisRemove > 0) {
                        toRemove += thisRemove;
                        table.put(null, card, e.getKey(), thisRemove);
                    }
                }
            }
        }

        // if table is empty, then no counter was removed
        return table.isEmpty() ? null : PaymentDecision.counters(table);
    }

    @Override
    public PaymentDecision visit(CostRemoveCounter cost) {
        final String amount = cost.getAmount();
        final String type = cost.getType();
        final GameEntityCounterTable counterTable = new GameEntityCounterTable();

        // TODO Help AI filter card with most useless counters and put those counters in countertable for things like
        //  Moxite Refinery, similar to CostRemoveAnyCounter
        //  Probably a lot of that decision making can be re-used or pulled out for both PaymentDecisions to use
        if (cost.counter == null) return null;

        int c;

        final String sVar = ability.getSVar(amount);
        if (amount.equals("All")) {
            c = source.getCounters(cost.counter);
        } else if (sVar.equals("Targeted$CardManaCost")) {
            c = 0;
            if (ability.getTargets().size() > 0) {
                for (Card tgt : ability.getTargets().getTargetCards()) {
                    if (tgt.getManaCost() != null) {
                        c += tgt.getManaCost().getCMC();
                    }
                }
            }
        } else {
            c = cost.getAbilityAmount(ability);
        }

        if (!cost.payCostFromSource()) {
            CardCollectionView typeList;
            if (type.equals("OriginalHost")) {
                typeList = new CardCollection(ability.getOriginalHost());
            } else {
                typeList = CardLists.getValidCards(player.getCardsIn(cost.zone), type.split(";"), player, source, ability);
            }
            for (Card card : typeList) {
                if (card.getCounters(cost.counter) >= c) {
                    counterTable.put(null, card, cost.counter, c);
                    return PaymentDecision.counters(counterTable);
                }
            }
            return null;
        }

        if (c > source.getCounters(cost.counter)) {
            System.out.println("Not enough " + cost.counter + " on " + source.getName());
            return null;
        }

        counterTable.put(null, source, cost.counter, c);
        return PaymentDecision.counters(counterTable);
    }

    @Override
    public PaymentDecision visit(CostUntapType cost) {
        int c = cost.getAbilityAmount(ability);

        CardCollectionView list = ComputerUtil.chooseUntapType(player, cost.getType(), source, cost.canUntapSource, c, ability);

        if (list == null) {
            System.out.println("Couldn't find a valid card to untap for: " + source.getName());
            return null;
        }

        return PaymentDecision.card(list);
    }

    @Override
    public PaymentDecision visit(CostUntap cost) {
        return PaymentDecision.number(0);
    }

    @Override
    public PaymentDecision visit(CostPayShards cost) {
        return PaymentDecision.number(0);
    }

    @Override
    public PaymentDecision visit(CostUnattach cost) {
        final CardCollection cardToUnattach = cost.findCardToUnattach(source, player, ability);
        if (cardToUnattach.isEmpty()) {
            // We really shouldn't be able to get here if there's nothing to unattach
            return null;
        }
        return PaymentDecision.card(cardToUnattach.getFirst());
    }

    @Override
    public PaymentDecision visit(CostBlight cost) {
        // This tells the AI: "Treat this like placing counters"
        return this.visit((CostPutCounter) cost);
    }

    @Override
    public boolean paysRightAfterDecision() {
        return false;
    }
}
