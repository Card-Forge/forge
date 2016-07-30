package forge.ai;

import com.google.common.base.Predicate;

import forge.card.CardType;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CounterType;
import forge.game.cost.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AiCostDecision extends CostDecisionMakerBase {
    private final SpellAbility ability;
    private final Card source;
    
    private final CardCollection discarded;
    private final CardCollection tapped;

    public AiCostDecision(Player ai0, SpellAbility sa) {
        super(ai0);
        ability = sa;
        source = ability.getHostCard();

        discarded = new CardCollection();
        tapped = new CardCollection();
    }

    @Override
    public PaymentDecision visit(CostAddMana cost) {
        Integer c = cost.convertAmount();
    
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }
    
        return PaymentDecision.number(c);
    }


    @Override
    public PaymentDecision visit(CostChooseCreatureType cost) {
        String choice = player.getController().chooseSomeType("Creature", ability, new ArrayList<String>(CardType.Constant.CREATURE_TYPES), new ArrayList<String>());
        return PaymentDecision.type(choice);
    }

    @Override
    public PaymentDecision visit(CostDiscard cost) {
        final String type = cost.getType();

        final CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
        if (type.equals("LastDrawn")) {
            if (!hand.contains(player.getLastDrawnCard())) {
                return null;
            }
            return PaymentDecision.card(player.getLastDrawnCard());
        }
        else if (cost.payCostFromSource()) {
            if (!hand.contains(source)) {
                return null;
            }

            return PaymentDecision.card(source);
        }
        else if (type.equals("Hand")) {
            return PaymentDecision.card(hand);
        }

        if (type.contains("WithSameName")) {
            return null;
        }
        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(cost.getAmount());
            if (sVar.equals("XChoice")) {
                return null;
            }
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }

        if (type.equals("Random")) {
            return PaymentDecision.card(CardLists.getRandomSubList(new CardCollection(hand), c));
        }
        else {
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
        Integer c = cost.convertAmount();

        if (c == null) {
            final String sVar = ability.getSVar(cost.getAmount());
            // Generalize cost
            if (sVar.equals("XChoice")) {
                return null; // cannot pay
            } else {
                c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
            }
        }

        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(CostDraw cost) {
        Integer c = cost.convertAmount();

        if (c == null) {
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }

        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(CostExile cost) {
        if (cost.payCostFromSource()) {
            return PaymentDecision.card(source);
        }

        if (cost.getType().equals("All")) {
            return PaymentDecision.card(player.getCardsIn(cost.getFrom()));
        }
        else if (cost.getType().contains("FromTopGrave")) {
            return null;
        }

        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(cost.getAmount());
            // Generalize cost
            if (sVar.equals("XChoice")) {
                return null;
            }
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }

        if (cost.getFrom().equals(ZoneType.Library)) {
            return PaymentDecision.card(player.getCardsIn(ZoneType.Library, c));
        }
        else if (cost.sameZone) {
            // TODO Determine exile from same zone for AI
            return null;
        }
        else {
            CardCollectionView chosen = ComputerUtil.chooseExileFrom(player, cost.getFrom(), cost.getType(), source, ability.getTargetCard(), c);
            return null == chosen ? null : PaymentDecision.card(chosen);
        }
    }

    @Override
    public PaymentDecision visit(CostExileFromStack cost) {

        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(cost.getAmount());
            // Generalize cost
            if (sVar.equals("XChoice")) {
                return null;
            }
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }
        List<SpellAbility> chosen = new ArrayList<SpellAbility>();
        for (SpellAbilityStackInstance si :source.getGame().getStack()) {
            SpellAbility sp = si.getSpellAbility(true).getRootAbility();
            if (si.getSourceCard().isValid(cost.getType().split(";"), source.getController(), source, sp)) {
                chosen.add(sp);
            }
        }
        return chosen.isEmpty() ? null : PaymentDecision.spellabilities(chosen);
    }

    @Override
    public PaymentDecision visit(CostExiledMoveToGrave cost) {
        Integer c = cost.convertAmount();
        CardCollection chosen = new CardCollection();

        if (c == null) {
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }

        CardCollection typeList = CardLists.getValidCards(player.getGame().getCardsIn(ZoneType.Exile), cost.getType().split(";"), player, source, ability);

        if (typeList.size() < c) {
            return null;
        }

        CardLists.sortByPowerAsc(typeList);
        Collections.reverse(typeList);

        for (int i = 0; i < c; i++) {
            chosen.add(typeList.get(i));
        }

        return chosen.isEmpty() ? null : PaymentDecision.card(chosen);
    }

    @Override
    public PaymentDecision visit(CostFlipCoin cost) {
        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(cost.getAmount());
            // Generalize cost
            if (sVar.equals("XChoice")) {
                return null;
            }
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }
        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(CostGainControl cost) {
        if (cost.payCostFromSource()) {
            return PaymentDecision.card(source);
        }

        Integer c = cost.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }

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
    public PaymentDecision visit(CostGainLife cost) {
        final List<Player> oppsThatCanGainLife = new ArrayList<Player>();
        
        for (final Player opp : cost.getPotentialTargets(player, source)) {
            if (opp.canGainLife()) {
                oppsThatCanGainLife.add(opp);
            }
        }
    
        if (oppsThatCanGainLife.size() == 0) {
            return null;
        }
    
        return PaymentDecision.players(oppsThatCanGainLife);
    }


    @Override
    public PaymentDecision visit(CostMill cost) {
        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(cost.getAmount());
            // Generalize cost
            if (sVar.equals("XChoice")) {
                return null;
            }

            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }

        CardCollectionView topLib = player.getCardsIn(ZoneType.Library, c);
        return topLib.size() < c ? null : PaymentDecision.card(topLib);
    }

    @Override
    public PaymentDecision visit(CostPartMana cost) {
        return PaymentDecision.number(0);
    }

    @Override
    public PaymentDecision visit(CostPayLife cost) {
        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(cost.getAmount());
            // Generalize cost
            if (sVar.equals("XChoice")) {
                return null;
            } else {
                c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
            }
        }
        if (!player.canPayLife(c)) {
            return null;
        }
        // activator.payLife(c, null);
        return PaymentDecision.number(c);
    }


    @Override
    public PaymentDecision visit(CostPutCardToLib cost) {
        Integer c = cost.convertAmount();
        final Game game = player.getGame();
        CardCollection chosen = new CardCollection();
        CardCollectionView list;

        if (cost.isSameZone()) {
            list = new CardCollection(game.getCardsIn(cost.getFrom()));
        }
        else {
            list = new CardCollection(player.getCardsIn(cost.getFrom()));
        }

        if (c == null) {
            final String sVar = ability.getSVar(cost.getAmount());
            // Generalize cost
            if (sVar.equals("XChoice")) {
                return null;
            }
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }

        list = CardLists.getValidCards(list, cost.getType().split(";"), player, source, ability);

        if (cost.isSameZone()) {
            // Jotun Grunt
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
        }
        else {
            chosen = ComputerUtil.choosePutToLibraryFrom(player, cost.getFrom(), cost.getType(), source, ability.getTargetCard(), c);
        }
        return chosen.isEmpty() ? null : PaymentDecision.card(chosen);
    }

    @Override
    public PaymentDecision visit(CostPutCounter cost) {
        if (cost.payCostFromSource()) {
            return PaymentDecision.card(source);
        }

        final CardCollection typeList = CardLists.getValidCards(player.getGame().getCardsIn(ZoneType.Battlefield),
                cost.getType().split(";"), player, source, ability);

        Card card;
        if (cost.getType().equals("Creature.YouCtrl")) {
            card = ComputerUtilCard.getWorstCreatureAI(typeList);
        }
        else {
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
        final String amount = cost.getAmount();
        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            if (sVar.equals("XChoice")) {
                CardCollectionView typeList =
                        CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), cost.getType().split(";"),
                                ability.getActivatingPlayer(), ability.getHostCard(), ability);
                typeList = CardLists.filter(typeList, Presets.UNTAPPED);
                c = typeList.size();
                source.setSVar("ChosenX", "Number$" + Integer.toString(c));
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        if (cost.getType().contains("sharesCreatureTypeWith") || cost.getType().contains("withTotalPowerGE")) {
            return null;
        }

        CardCollectionView totap = ComputerUtil.chooseTapType(player, cost.getType(), source, !cost.canTapSource, c, tapped);

        if (totap == null) {
            System.out.println("Couldn't find a valid card to tap for: " + source.getName());
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
        if (cost.getAmount().equals("All")) {
            /*CardCollectionView typeList = new CardCollection(activator.getCardsIn(ZoneType.Battlefield));
            typeList = CardLists.getValidCards(typeList, cost.getType().split(";"), activator, source);
            if (activator.hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
                typeList = CardLists.getNotType(typeList, "Creature");
            }*/
            // Does the AI want to use Sacrifice All?
            return null;
        }

        Integer c = cost.convertAmount();
        if (c == null) {
            if (ability.getSVar(cost.getAmount()).equals("XChoice")) {
                return null;
            }

            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }
        CardCollectionView list = ComputerUtil.chooseSacrificeType(player, cost.getType(), source, ability.getTargetCard(), c);
        return PaymentDecision.card(list);
    }

    @Override
    public PaymentDecision visit(CostReturn cost) {
        if (cost.payCostFromSource())
            return PaymentDecision.card(source);
        
        Integer c = cost.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }

        CardCollectionView res = ComputerUtil.chooseReturnType(player, cost.getType(), source, ability.getTargetCard(), c);
        return res.isEmpty() ? null : PaymentDecision.card(res);
    }

    @Override
    public PaymentDecision visit(CostReveal cost) {
        final String type = cost.getType();
        CardCollectionView hand = player.getCardsIn(ZoneType.Hand);

        if (cost.payCostFromSource()) {
            if (!hand.contains(source)) {
                return null;
            }
            return PaymentDecision.card(source);
        }

        if (cost.getType().equals("Hand")) {
            return PaymentDecision.card(hand);
        }

        if (cost.getType().equals("SameColor")) {
            return null;
        }
            
        hand = CardLists.getValidCards(hand, type.split(";"), player, source, ability);
        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(cost.getAmount());
            if (sVar.equals("XChoice")) {
                c = hand.size();
            } else {
                c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
            }
        }

        final AiController aic = ((PlayerControllerAi)player.getController()).getAi();
        return PaymentDecision.card(aic.getCardsToDiscard(c, type.split(";"), ability));
    }

    @Override
    public PaymentDecision visit(CostRemoveAnyCounter cost) {
        final String amount = cost.getAmount();
        final int c = AbilityUtils.calculateAmount(source, amount, ability);
        final String type = cost.getType();

        CardCollectionView typeList = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), type.split(";"), player, source, ability);
        CardCollectionView hperms = CardLists.filter(typeList, new Predicate<Card>() {
            @Override
            public boolean apply(final Card crd) {
                for (final CounterType c1 : CounterType.values()) {
                    if (crd.getCounters(c1) >= c  && ComputerUtil.isNegativeCounter(c1, crd)) {
                        return true;
                    }
                }
                return false;
            }
        });

        if(hperms.isEmpty())
            return null;
        
        PaymentDecision result = PaymentDecision.card(hperms);
        Card valid = hperms.get(0);
        for (CounterType c1 : valid.getCounters().keySet()) {
            if (valid.getCounters(c1) >= c && ComputerUtil.isNegativeCounter(c1, valid)) {
                result.ct = c1;
                break;
            }
        }
        // Only find cards with enough negative counters
        // TODO: add ai for Chisei, Heart of Oceans
        return result;
    }

    @Override
    public PaymentDecision visit(CostRemoveCounter cost) {
        final String amount = cost.getAmount();
        Integer c = cost.convertAmount();
        final String type = cost.getType();

        if (c == null) {
            final String sVar = ability.getSVar(amount);
            if (sVar.equals("XChoice")) {
                c = AbilityUtils.calculateAmount(source, "ChosenX", ability);
            } else if (amount.equals("All")) {
                c = source.getCounters(cost.counter);
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
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
                    return PaymentDecision.card(card, c);
                }
            }
            return null;
        }

        if (c > source.getCounters(cost.counter)) {
            System.out.println("Not enough " + cost.counter + " on " + source.getName());
            return null;
        }

        return PaymentDecision.card(source, c);
    }

    @Override
    public PaymentDecision visit(CostUntapType cost) {
        final String amount = cost.getAmount();
        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            if (sVar.equals("XChoice")) {
                CardCollection typeList = CardLists.getValidCards(player.getGame().getCardsIn(ZoneType.Battlefield),
                        cost.getType().split(";"), player, ability.getHostCard(), ability);
                if (!cost.canUntapSource) {
                    typeList.remove(source);
                }
                typeList = CardLists.filter(typeList, Presets.TAPPED);
                c = typeList.size();
                source.setSVar("ChosenX", "Number$" + Integer.toString(c));
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

        CardCollectionView list = ComputerUtil.chooseUntapType(player, cost.getType(), source, cost.canUntapSource, c);

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
    public PaymentDecision visit(CostUnattach cost) {
        final Card cardToUnattach = cost.findCardToUnattach(source, player, ability);
        if (cardToUnattach == null) {
            // We really shouldn't be able to get here if there's nothing to unattach
            return null;
        }
        return PaymentDecision.card(cardToUnattach);
    }

    @Override
    public boolean paysRightAfterDecision() {
        return false;
    }
}

