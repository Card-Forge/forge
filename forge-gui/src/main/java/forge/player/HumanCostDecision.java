package forge.player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.CardType;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CardView;
import forge.game.card.CounterType;
import forge.game.cost.CostAddMana;
import forge.game.cost.CostChooseCreatureType;
import forge.game.cost.CostDamage;
import forge.game.cost.CostDecisionMakerBase;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostDraw;
import forge.game.cost.CostExile;
import forge.game.cost.CostExileFromStack;
import forge.game.cost.CostExiledMoveToGrave;
import forge.game.cost.CostFlipCoin;
import forge.game.cost.CostGainControl;
import forge.game.cost.CostGainLife;
import forge.game.cost.CostMill;
import forge.game.cost.CostPartMana;
import forge.game.cost.CostPayLife;
import forge.game.cost.CostPutCardToLib;
import forge.game.cost.CostPutCounter;
import forge.game.cost.CostRemoveAnyCounter;
import forge.game.cost.CostRemoveCounter;
import forge.game.cost.CostReturn;
import forge.game.cost.CostReveal;
import forge.game.cost.CostSacrifice;
import forge.game.cost.CostTap;
import forge.game.cost.CostTapType;
import forge.game.cost.CostUnattach;
import forge.game.cost.CostUntap;
import forge.game.cost.CostUntapType;
import forge.game.cost.PaymentDecision;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import forge.match.input.InputSelectCardsFromList;
import forge.match.input.InputSelectManyBase;
import forge.util.Aggregates;
import forge.util.collect.FCollectionView;
import forge.util.ITriggerEvent;
import forge.util.Lang;

public class HumanCostDecision extends CostDecisionMakerBase {
    private final PlayerControllerHuman controller;
    private final SpellAbility ability;
    private final Card source;

    public HumanCostDecision(final PlayerControllerHuman controller, final Player p, final SpellAbility sa, final Card source) {
        super(p);
        this.controller = controller;
        ability = sa;
        this.source = source;
    }

    protected int chooseXValue(final int maxValue) {
        /*final String chosen = sa.getSVar("ChosenX");
        if (chosen.length() > 0) {
            return AbilityFactory.calculateAmount(card, "ChosenX", null);
        }*/

        final int chosenX = player.getController().chooseNumber(ability, source.toString() + " - Choose a Value for X", 0, maxValue);
        ability.setSVar("ChosenX", Integer.toString(chosenX));
        source.setSVar("ChosenX", Integer.toString(chosenX));
        return chosenX;
    }

    @Override
    public PaymentDecision visit(final CostAddMana cost) {
        Integer c = cost.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }
        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(final CostChooseCreatureType cost) {
        final String choice = controller.chooseSomeType("Creature", ability, new ArrayList<String>(CardType.Constant.CREATURE_TYPES), new ArrayList<String>(), true);
        if (null == choice) {
            return null;
        }
        return PaymentDecision.type(choice);
    }

    @Override
    public PaymentDecision visit(final CostDiscard cost) {
        CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
        final String discardType = cost.getType();
        final String amount = cost.getAmount();

        if (cost.payCostFromSource()) {
            return hand.contains(source) ? PaymentDecision.card(source) : null;
        }

        if (discardType.equals("Hand")) {
            return PaymentDecision.card(hand);
        }

        if (discardType.equals("LastDrawn")) {
            final Card lastDrawn = player.getLastDrawnCard();
            return hand.contains(lastDrawn) ? PaymentDecision.card(lastDrawn) : null;
        }

        Integer c = cost.convertAmount();

        if (discardType.equals("Random")) {
            if (c == null) {
                final String sVar = ability.getSVar(amount);
                // Generalize this
                if (sVar.equals("XChoice")) {
                    c = chooseXValue(hand.size());
                }
                else {
                    c = AbilityUtils.calculateAmount(source, amount, ability);
                }
            }

            return PaymentDecision.card(Aggregates.random(hand, c, new CardCollection()));
        }
        if (discardType.contains("+WithSameName")) {
            final String type = discardType.replace("+WithSameName", "");
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
                final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, hand);
                inp.setMessage("Select one of the cards with the same name to discard. Already chosen: " + discarded);
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

        final String type = new String(discardType);
        final String[] validType = type.split(";");
        hand = CardLists.getValidCards(hand, validType, player, source, ability);

        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(hand.size());
            }
            else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, hand);
        inp.setMessage("Select %d more " + cost.getDescriptiveType() + " to discard.");
        inp.setCancelAllowed(true);
        inp.showAndWait();
        if (inp.hasCancelled() || inp.getSelected().size() != c) {
            return null;
        }
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(final CostDamage cost) {
        final String amount = cost.getAmount();
        final int life = player.getLife();

        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(life);
            }
            else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

        if (player.canPayLife(c) && player.getController().confirmPayment(cost, "Pay " + c + " Life?")) {
            return PaymentDecision.number(c);
        }
        return null;
    }

    @Override
    public PaymentDecision visit(final CostDraw cost) {
        final String amount = cost.getAmount();

        Integer c = cost.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, amount, ability);
        }

        if (!player.getController().confirmPayment(cost, "Draw " + c + " Card" + (c == 1 ? "" : "s"))) {
            return null;
        }

        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(final CostExile cost) {
        final String amount = cost.getAmount();
        final Game game = player.getGame();

        Integer c = cost.convertAmount();
        String type = cost.getType();
        boolean fromTopGrave = false;
        if (type.contains("FromTopGrave")) {
            type = type.replace("FromTopGrave", "");
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
            return source.getZone() == player.getZone(cost.from) && player.getController().confirmPayment(cost, "Exile " + source.getName() + "?") ? PaymentDecision.card(source) : null;
        }

        if (type.equals("All")) {
            return PaymentDecision.card(list);
        }
        list = CardLists.getValidCards(list, type.split(";"), player, source, ability);
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(list.size());
            }
            else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

        if (cost.from == ZoneType.Battlefield || cost.from == ZoneType.Hand) {
            final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, list);
            inp.setMessage("Exile %d card(s) from your" + cost.from);
            inp.setCancelAllowed(true);
            inp.showAndWait();
            return inp.hasCancelled() ? null : PaymentDecision.card(inp.getSelected());
        }

        if (cost.from == ZoneType.Library) { return exileFromTop(cost, ability, player, c); }
        if (fromTopGrave) { return exileFromTopGraveType(ability, c, list); }
        if (!cost.sameZone) { return exileFromMiscZone(cost, ability, c, list); }

        final FCollectionView<Player> players = game.getPlayers();
        final List<Player> payableZone = new ArrayList<Player>();
        for (final Player p : players) {
            final CardCollection enoughType = CardLists.filter(list, CardPredicates.isOwner(p));
            if (enoughType.size() < c) {
                list.removeAll((CardCollectionView)enoughType);
            }
            else {
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
        final Game game = controller.getGame();
        final Player p = game.getPlayer(controller.getGui().oneOrNone(String.format("Exile from whose %s?", cost.getFrom()), PlayerView.getCollection(payableZone)));
        if (p == null) {
            return null;
        }

        final CardCollection typeList = CardLists.filter(list, CardPredicates.isOwner(p));
        final int count = typeList.size();
        if (count < nNeeded) {
            return null;
        }

        final CardCollection toExile = game.getCardList(controller.getGui().many("Exile from " + cost.getFrom(), "To be exiled", nNeeded, CardView.getCollection(typeList), null));
        return PaymentDecision.card(toExile);
    }

    @Override
    public PaymentDecision visit(final CostExileFromStack cost) {
        final String amount = cost.getAmount();
        final Game game = player.getGame();

        Integer c = cost.convertAmount();
        final String type = cost.getType();
        final List<SpellAbility> saList = new ArrayList<SpellAbility>();
        final List<String> descList = new ArrayList<String>();

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
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(saList.size());
            }
            else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

        if (saList.size() < c) {
            return null;
        }

        final List<SpellAbility> exiled = new ArrayList<SpellAbility>();
        for (int i = 0; i < c; i++) {
            //Have to use the stack descriptions here because some copied spells have no description otherwise
            final String o = controller.getGui().oneOrNone("Exile from Stack", descList);

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
        final StringBuilder sb = new StringBuilder();
        sb.append("Exile ").append(nNeeded).append(" cards from the top of your library?");
        final CardCollectionView list = player.getCardsIn(ZoneType.Library, nNeeded);

        if (list.size() > nNeeded || !player.getController().confirmPayment(cost, "Exile " + Lang.nounWithAmount(nNeeded, "card") + " from the top of your library?")) {
            return null;
        }
        return PaymentDecision.card(list);
    }

    private Card getCard(final CardView cardView) {
        return controller.getGame().getCard(cardView);
    }

    private PaymentDecision exileFromMiscZone(final CostExile cost, final SpellAbility sa, final int nNeeded, final CardCollection typeList) {
        if (typeList.size() < nNeeded) { return null; }

        final CardCollection exiled = new CardCollection();
        for (int i = 0; i < nNeeded; i++) {
            final Card c = getCard(controller.getGui().oneOrNone("Exile from " + cost.getFrom(), CardView.getCollection(typeList)));
            if (c == null) { return null; }

            typeList.remove(c);
            exiled.add(c);
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
        Integer c = cost.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }

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
        final CardCollection choice = controller.getGame().getCardList(controller.getGui().many("Choose an exiled card to put into graveyard", "To graveyard", min, c, CardView.getCollection(list), CardView.get(source)));
        
        if (choice == null || choice.size() < c) {
            return null;
        }
        return PaymentDecision.card(choice);
    }

    @Override
    public PaymentDecision visit(final CostFlipCoin cost) {
        final String amount = cost.getAmount();
        Integer c = cost.convertAmount();

        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(cost.getLKIList().size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        return PaymentDecision.number(c);
    }

    @Override
    public PaymentDecision visit(final CostGainControl cost) {
        final String amount = cost.getAmount();

        Integer c = cost.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, amount, ability);
        }
        final CardCollectionView list = player.getCardsIn(ZoneType.Battlefield);
        final CardCollectionView validCards = CardLists.getValidCards(list, cost.getType().split(";"), player, source, ability);

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, validCards);
        final String desc = cost.getTypeDescription() == null ? cost.getType() : cost.getTypeDescription();
        inp.setMessage("Gain control of %d " + desc);
        inp.showAndWait();
        if (inp.hasCancelled()) {
            return null;
        }
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(final CostGainLife cost) {
        final String amount = cost.getAmount();

        final int life = player.getLife();

        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(life);
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

        final List<Player> oppsThatCanGainLife = new ArrayList<Player>();
        for (final Player opp : cost.getPotentialTargets(player, source)) {
            if (opp.canGainLife()) {
                oppsThatCanGainLife.add(opp);
            }
        }

        if (cost.getCntPlayers() == Integer.MAX_VALUE) {
            return PaymentDecision.players(oppsThatCanGainLife);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(source.getName()).append(" - Choose an opponent to gain ").append(c).append(" life:");

        final Player chosenToGain = controller.getGame().getPlayer(controller.getGui().oneOrNone(sb.toString(), PlayerView.getCollection(oppsThatCanGainLife)));
        if (chosenToGain == null) {
            return null;
        }
        return PaymentDecision.players(Lists.newArrayList(chosenToGain));
    }

    @Override
    public PaymentDecision visit(final CostMill cost) {
        final String amount = cost.getAmount();
        Integer c = cost.convertAmount();

        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(cost.getLKIList().size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

        if (!player.getController().confirmPayment(cost, String.format("Mill %d card%s from your library?", c, c == 1 ? "" : "s"))) {
            return null;
        }
        return PaymentDecision.card(player.getCardsIn(ZoneType.Library, c));
    }

    @Override
    public PaymentDecision visit(final CostPayLife cost) {
        final String amount = cost.getAmount();
        final int life = player.getLife();

        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.startsWith("XChoice")) {
                int limit = life;
                if (sVar.contains("LimitMax")) {
                    limit = AbilityUtils.calculateAmount(source, sVar.split("LimitMax.")[1], ability);
                }
                final int maxLifePayment = limit < life ? limit : life;
                c = chooseXValue(maxLifePayment);
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

        if (player.canPayLife(c) && player.getController().confirmPayment(cost, "Pay " + c + " Life?")) {
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
        final String amount = cost.getAmount();
        Integer c = cost.convertAmount();

        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(cost.getLKIList().size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

        final CardCollection list = CardLists.getValidCards(cost.sameZone ? player.getGame().getCardsIn(cost.getFrom()) :
                player.getCardsIn(cost.getFrom()), cost.getType().split(";"), player, source, ability);

        if (cost.from == ZoneType.Hand) {
            final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, list);
            inp.setMessage("Put %d card(s) from your " + cost.from);
            inp.setCancelAllowed(true);
            inp.showAndWait();
            return inp.hasCancelled() ? null : PaymentDecision.card(inp.getSelected());
        }

        if (cost.sameZone){
            final FCollectionView<Player> players = player.getGame().getPlayers();
            final List<Player> payableZone = new ArrayList<Player>();
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
        for (int i = 0; i < nNeeded; i++) {
            final Card c = getCard(controller.getGui().oneOrNone("Put from " + fromZone + " to library", CardView.getCollection(typeList)));
            if (c == null) {
                return null;
            }
            typeList.remove(c);
            chosen.add(c);
        }
        return PaymentDecision.card(chosen);
    }

    private PaymentDecision putFromSame(final CardCollectionView list, final int nNeeded, final List<Player> payableZone, final ZoneType fromZone) {
        if (nNeeded == 0) {
            return PaymentDecision.number(0);
        }

        final Player p = controller.getGame().getPlayer(controller.getGui().oneOrNone(String.format("Put cards from whose %s?", fromZone), PlayerView.getCollection(payableZone)));
        if (p == null) {
            return null;
        }

        final CardCollection typeList = CardLists.filter(list, CardPredicates.isOwner(p));
        if (typeList.size() < nNeeded) {
            return null;
        }

        final CardCollection chosen = new CardCollection();
        for (int i = 0; i < nNeeded; i++) {
            final Card c = getCard(controller.getGui().oneOrNone("Put cards from " + fromZone + " to Library", CardView.getCollection(typeList)));
            if (c == null) {
                return null;
            }
            typeList.remove(c);
            chosen.add(c);
        }
        return PaymentDecision.card(chosen);
    }

    @Override
    public PaymentDecision visit(final CostPutCounter cost) {
        final Integer c = cost.getNumberOfCounters(ability);

        if (cost.payCostFromSource()) {
            cost.setLastPaidAmount(c);
            return PaymentDecision.number(c);
        }

        // Cards to use this branch: Scarscale Ritual, Wandering Mage - each adds only one counter
        final CardCollectionView typeList = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield),
                cost.getType().split(";"), player, ability.getHostCard(), ability);

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, typeList);
        inp.setMessage("Put " + Lang.nounWithAmount(c, cost.getCounter().getName() + " counter") + " on " + cost.getDescriptiveType());
        inp.setCancelAllowed(true);
        inp.showAndWait();

        if (inp.hasCancelled()) {
            return null;
        }
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(final CostReturn cost) {
        final String amount = cost.getAmount();
        Integer c = cost.convertAmount();

        final CardCollectionView list = player.getCardsIn(ZoneType.Battlefield);
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(list.size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        if (cost.payCostFromSource()) {
            final Card card = ability.getHostCard();
            if (card.getController() == player && card.isInPlay()) {
                final CardView view = CardView.get(card);
                return player.getController().confirmPayment(cost, "Return " + view + " to hand?") ? PaymentDecision.card(card) : null;
            }
        }
        else {
            final CardCollectionView validCards = CardLists.getValidCards(ability.getActivatingPlayer().getCardsIn(ZoneType.Battlefield),
                    cost.getType().split(";"), player, source, ability);

            final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, validCards);
            inp.setCancelAllowed(true);
            inp.setMessage("Return %d " + cost.getDescriptiveType() + " card(s) to hand");
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
        final String amount = cost.getAmount();

        if (cost.payCostFromSource()) {
            return PaymentDecision.card(source);
        }
        if (cost.getType().equals("Hand")) {
            return PaymentDecision.card(player.getCardsIn(ZoneType.Hand));
        }
        InputSelectCardsFromList inp = null;
        if (cost.getType().equals("SameColor")) {
            final Integer num = cost.convertAmount();
            CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
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
            inp = new InputSelectCardsFromList(controller, num, hand) {
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
            inp.setMessage("Select " + Lang.nounWithAmount(num, "card") + " of same color to reveal.");
        }
        else {
            Integer num = cost.convertAmount();

            CardCollectionView hand = player.getCardsIn(ZoneType.Hand);
            hand = CardLists.getValidCards(hand, cost.getType().split(";"), player, source, ability);

            if (num == null) {
                final String sVar = ability.getSVar(amount);
                if (sVar.equals("XChoice")) {
                    num = chooseXValue(hand.size());
                } else {
                    num = AbilityUtils.calculateAmount(source, amount, ability);
                }
            }
            if (num == 0) {
                return PaymentDecision.number(0);
            };

            inp = new InputSelectCardsFromList(controller, num, num, hand);
            inp.setMessage("Select %d more " + cost.getDescriptiveType() + " card(s) to reveal.");
        }
        inp.setCancelAllowed(true);
        inp.showAndWait();
        if (inp.hasCancelled()) {
            return null;
        }
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(final CostRemoveAnyCounter cost) {
        Integer c = cost.convertAmount();
        final String type = cost.getType();

        if (c == null) {
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }

        CardCollectionView list = new CardCollection(player.getCardsIn(ZoneType.Battlefield));
        list = CardLists.getValidCards(list, type.split(";"), player, source, ability);


        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card card) {
                return card.hasCounters();
            }
        });
        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, list);
        inp.setMessage("Select " + cost.getDescriptiveType() + " to remove a counter");
        inp.setCancelAllowed(false);
        inp.showAndWait();
        final Card selected = inp.getFirstSelected();
        final Map<CounterType, Integer> tgtCounters = selected.getCounters();
        final List<CounterType> typeChoices = new ArrayList<CounterType>();
        for (final CounterType key : tgtCounters.keySet()) {
            if (tgtCounters.get(key) > 0) {
                typeChoices.add(key);
            }
        }

        final String prompt = "Select type counters to remove";
        cost.setCounterType(controller.getGui().one(prompt, typeChoices));

        return PaymentDecision.card(selected, cost.getCounter());
    }

    public static final class InputSelectCardToRemoveCounter extends InputSelectManyBase<Card> {
        private static final long serialVersionUID = 2685832214519141903L;

        private final Map<Card,Integer> cardsChosen;
        private final CounterType counterType;
        private final CardCollectionView validChoices;

        public InputSelectCardToRemoveCounter(final PlayerControllerHuman controller, final int cntCounters, final CounterType cType, final CardCollectionView validCards) {
            super(controller, cntCounters, cntCounters);
            this.validChoices = validCards;
            counterType = cType;
            cardsChosen = cntCounters > 0 ? new HashMap<Card, Integer>() : null;
        }

        @Override
        protected boolean onCardSelected(final Card c, final List<Card> otherCardsToSelect, final ITriggerEvent triggerEvent) {
            if (!isValidChoice(c) || c.getCounters(counterType) <= getTimesSelected(c)) {
                return false;
            }

            final int tc = getTimesSelected(c);
            cardsChosen.put(c, tc + 1);

            onSelectStateChanged(c, true);
            refresh();
            return true;
        }

        @Override
        public String getActivateAction(final Card c) {
            if (!isValidChoice(c) || c.getCounters(counterType) <= getTimesSelected(c)) {
                return null;
            }
            return "remove counter from card";
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
            int sum = 0;
            for (final Entry<Card, Integer> kv : cardsChosen.entrySet()) {
                sum += kv.getValue().intValue();
            }
            return sum;
        }

        protected final boolean isValidChoice(final GameEntity choice) {
            return validChoices.contains(choice);
        }

        public int getTimesSelected(final Card c) {
            return cardsChosen.containsKey(c) ? cardsChosen.get(c).intValue() : 0;
        }

        @Override
        public Collection<Card> getSelected() {
            return cardsChosen.keySet();
        }
    }

    @Override
    public PaymentDecision visit(final CostRemoveCounter cost) {
        final String amount = cost.getAmount();
        final Integer c = cost.convertAmount();
        final String type = cost.getType();

        final String sVarAmount = ability.getSVar(amount);
        int cntRemoved = 1;
        if (c != null) {
            cntRemoved = c.intValue();
        } else if (!"XChoice".equals(sVarAmount)) {
            cntRemoved = AbilityUtils.calculateAmount(source, amount, ability);
        }

        if (cost.payCostFromSource()) {
            final int maxCounters = source.getCounters(cost.counter);
            if (amount.equals("All")) {
                final CardView view = CardView.get(ability.getHostCard());
                if (!controller.getGui().confirm(view, "Remove all counters?")) {
                    return null;
                }
                cntRemoved = maxCounters;
            }
            else if (c == null && "XChoice".equals(sVarAmount)) {
                cntRemoved = chooseXValue(maxCounters);
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

        final CardCollectionView validCards = CardLists.getValidCards(player.getCardsIn(cost.zone), type.split(";"), player, source, ability);
        if (cost.zone.equals(ZoneType.Battlefield)) {
            if (cntRemoved == 0) {
                return PaymentDecision.card(source, 0);
            }

            final InputSelectCardToRemoveCounter inp = new InputSelectCardToRemoveCounter(controller, cntRemoved, cost.counter, validCards);
            inp.setMessage("Remove %d " + cost.counter.getName() + " counters from " + cost.getDescriptiveType());
            inp.setCancelAllowed(true);
            inp.showAndWait();
            if (inp.hasCancelled()) {
                return null;
            }

            // Have to hack here: remove all counters minus one, without firing any triggers,
            // triggers will fire when last is removed by executePayment.
            // They don't care how many were removed anyway
            // int sum = 0;
            for (final Card crd : inp.getSelected()) {
                final int removed = inp.getTimesSelected(crd);
                // sum += removed;
                if (removed < 2) {
                    continue;
                }
                final int oldVal = crd.getCounters().get(cost.counter).intValue();
                crd.getCounters().put(cost.counter, Integer.valueOf(oldVal - removed + 1));
            }
            return PaymentDecision.card(inp.getSelected(), 1);
        }

        // Rift Elemental only - always removes 1 counter, so there will be no code for N counters.
        final List<CardView> suspended = Lists.newArrayList();
        for (final Card crd : validCards) {
            if (crd.getCounters(cost.counter) > 0) {
                suspended.add(CardView.get(crd));
            }
        }

        final Card card = getCard(controller.getGui().oneOrNone("Remove counter(s) from a card in " + cost.zone, suspended));
        return null == card ? null : PaymentDecision.card(card, c);
    }

    @Override
    public PaymentDecision visit(final CostSacrifice cost) {
        final String amount = cost.getAmount();
        final String type = cost.getType();

        CardCollectionView list = CardLists.filter(player.getCardsIn(ZoneType.Battlefield), CardPredicates.canBeSacrificedBy(ability));
        list = CardLists.getValidCards(list, type.split(";"), player, source, ability);

        if (cost.payCostFromSource()) {
            if (source.getController() == ability.getActivatingPlayer() && source.isInPlay()) {
                return player.getController().confirmPayment(cost, "Sacrifice " + source.getName() + "?") ? PaymentDecision.card(source) : null;
            }
            else {
                return null;
            }
        }

        if (amount.equals("All")) {
            return PaymentDecision.card(list);
        }

        Integer c = cost.convertAmount();
        if (c == null) {
            // Generalize this
            if (ability.getSVar(amount).equals("XChoice")) {
                c = chooseXValue(list.size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        if (0 == c.intValue()) {
            return PaymentDecision.number(0);
        }
        if (list.size() < c) {
            return null;
        }
        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, list);
        inp.setMessage("Select a " + cost.getDescriptiveType() + " to sacrifice (%d left)");
        inp.setCancelAllowed(true);
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
        Integer c = cost.convertAmount();

        boolean sameType = false;
        if (type.contains(".sharesCreatureTypeWith")) {
            sameType = true;
            type = type.replace(".sharesCreatureTypeWith", "");
        }

        boolean totalPower = false;
        String totalP = "";
        if (type.contains("+withTotalPowerGE")) {
            totalPower = true;
            totalP = type.split("withTotalPowerGE")[1];
            type = type.replace("+withTotalPowerGE" + totalP, "");
        }

        CardCollection typeList = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), type.split(";"), player,
                source, ability);
        typeList = CardLists.filter(typeList, Presets.UNTAPPED);
        if (c == null && !amount.equals("Any")) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(typeList.size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
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
            if (c == 0) {
                return PaymentDecision.number(0);
            }
            final CardCollection tapped = new CardCollection();
            while (c > 0) {
                final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, typeList);
                inp.setMessage("Select one of the cards to tap. Already chosen: " + tapped);
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
            final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 0, typeList.size(), typeList);
            inp.setMessage("Select a card to tap.");
            inp.setCancelAllowed(true);
            inp.showAndWait();

            if (inp.hasCancelled() || CardLists.getTotalPower(inp.getSelected()) < i) {
                return null;
            }
            return PaymentDecision.card(inp.getSelected());
        }

        if (c > typeList.size()) {
            controller.getGui().message("Not enough valid cards left to tap to pay the cost.", "Cost payment invalid");
            return null; // not enough targets anymore (e.g. Crackleburr + Smokebraider tapped to get mana)
        }

        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, typeList);
        inp.setCancelAllowed(true);
        inp.setMessage("Select a " + cost.getDescriptiveType() + " to tap (%d left)");
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
        final String amount = cost.getAmount();
        Integer c = cost.convertAmount();
        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(typeList.size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        final InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, typeList);
        inp.setCancelAllowed(true);
        inp.setMessage("Select a " + cost.getDescriptiveType() + " to untap (%d left)");
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
        final Card source = ability.getHostCard();

        final Card cardToUnattach = cost.findCardToUnattach(source, player, ability);
        if (cardToUnattach != null && player.getController().confirmPayment(cost, "Unattach " + cardToUnattach.getName() + "?")) {
            return PaymentDecision.card(cardToUnattach);
        }
        return null;
    }

    @Override
    public boolean paysRightAfterDecision() {
        return true;
    }
}
