package forge.player;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.card.CardType;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CounterType;
import forge.game.cost.*;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.zone.ZoneType;
import forge.interfaces.IGuiBase;
import forge.match.input.InputSelectCardsFromList;
import forge.match.input.InputSelectManyBase;
import forge.util.Aggregates;
import forge.util.ITriggerEvent;
import forge.util.Lang;
import forge.util.gui.SGuiChoose;
import forge.util.gui.SGuiDialog;
import forge.view.CardView;
import forge.view.PlayerView;

import java.util.*;
import java.util.Map.Entry;

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

    private IGuiBase getGui() {
        return this.controller.getGui();
    }

    protected int chooseXValue(final int maxValue) {
        /*final String chosen = sa.getSVar("ChosenX");
        if (chosen.length() > 0) {
            return AbilityFactory.calculateAmount(card, "ChosenX", null);
        }*/

        int chosenX = player.getController().chooseNumber(ability, source.toString() + " - Choose a Value for X", 0, maxValue);
        ability.setSVar("ChosenX", Integer.toString(chosenX));
        source.setSVar("ChosenX", Integer.toString(chosenX));
        return chosenX;
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
        String choice = controller.chooseSomeType("Creature", ability, new ArrayList<String>(CardType.getCreatureTypes()), new ArrayList<String>(), true);
        if( null == choice )
            return null;
        return PaymentDecision.type(choice);
    }

    @Override
    public PaymentDecision visit(CostDiscard cost) {
        List<Card> handList = new ArrayList<Card>(player.getCardsIn(ZoneType.Hand));
        String discardType = cost.getType();
        final String amount = cost.getAmount();

        if (cost.payCostFromSource()) {
            return handList.contains(source) ? PaymentDecision.card(source) : null;
        }

        if (discardType.equals("Hand")) {
            return PaymentDecision.card(handList);
        }

        if (discardType.equals("LastDrawn")) {
            final Card lastDrawn = player.getLastDrawnCard();
            return handList.contains(lastDrawn) ? PaymentDecision.card(lastDrawn) : null;
        }

        Integer c = cost.convertAmount();

        if (discardType.equals("Random")) {
            if (c == null) {
                final String sVar = ability.getSVar(amount);
                // Generalize this
                if (sVar.equals("XChoice")) {
                    c = chooseXValue(handList.size());
                }
                else {
                    c = AbilityUtils.calculateAmount(source, amount, ability);
                }
            }

            return PaymentDecision.card(Aggregates.random(handList, c));
        }
        if (discardType.contains("+WithSameName")) {
            String type = discardType.replace("+WithSameName", "");
            handList = CardLists.getValidCards(handList, type.split(";"), player, source);
            final List<Card> landList2 = handList;
            handList = CardLists.filter(handList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    for (Card card : landList2) {
                        if (!card.equals(c) && card.getName().equals(c.getName())) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            if (c == 0) { return PaymentDecision.card(Lists.<Card>newArrayList()); }
            List<Card> discarded = new ArrayList<Card>();
            while (c > 0) {
                InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, handList);
                inp.setMessage("Select one of the cards with the same name to discard. Already chosen: " + discarded);
                inp.setCancelAllowed(true);
                inp.showAndWait();
                if (inp.hasCancelled()) {
                    return null;
                }
                final Card first = inp.getFirstSelected();
                discarded.add(first);
                handList = CardLists.filter(handList, CardPredicates.nameEquals(first.getName()));
                handList.remove(first);
                c--;
            }
            return PaymentDecision.card(discarded);
        }
        
        String type = new String(discardType);
        final String[] validType = type.split(";");
        handList = CardLists.getValidCards(handList, validType, player, source);

        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(handList.size());
            }
            else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

        InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, handList);
        inp.setMessage("Select %d more " + cost.getDescriptiveType() + " to discard.");
        inp.setCancelAllowed(true);
        inp.showAndWait();
        if (inp.hasCancelled() || inp.getSelected().size() != c) {
            return null;
        }

        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(CostDamage cost) {
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
    public PaymentDecision visit(CostDraw cost) {
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
    public PaymentDecision visit(CostExile cost) {
        final String amount = cost.getAmount();
        final Game game = player.getGame(); 

        Integer c = cost.convertAmount();
        String type = cost.getType();
        boolean fromTopGrave = false;
        if (type.contains("FromTopGrave")) {
            type = type.replace("FromTopGrave", "");
            fromTopGrave = true;
        }

        List<Card> list;
        if (cost.getFrom().equals(ZoneType.Stack)) {
            list = new ArrayList<Card>();
            for (SpellAbilityStackInstance si : game.getStack()) {
                list.add(si.getSourceCard());
            }
        }
        else if (cost.sameZone) {
            list = new ArrayList<Card>(game.getCardsIn(cost.from));
        }
        else {
            list = new ArrayList<Card>(player.getCardsIn(cost.from));
        }

        if (cost.payCostFromSource()) {
            return source.getZone() == player.getZone(cost.from) && player.getController().confirmPayment(cost, "Exile " + source.getName() + "?") ? PaymentDecision.card(source) : null;

        }

        if (type.equals("All")) {
            return PaymentDecision.card(list);
        }
        list = CardLists.getValidCards(list, type.split(";"), player, source);
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
            InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, list);
            inp.setMessage("Exile %d card(s) from your" + cost.from);
            inp.setCancelAllowed(true);
            inp.showAndWait();
            return inp.hasCancelled() ? null : PaymentDecision.card(inp.getSelected());
        }

        if (cost.from == ZoneType.Library) { return exileFromTop(cost, ability, player, c); }
        if (fromTopGrave) { return exileFromTopGraveType(ability, c, list); }
        if (!cost.sameZone) { return exileFromMiscZone(cost, ability, c, list); }

        List<Player> players = game.getPlayers();
        List<Player> payableZone = new ArrayList<Player>();
        for (Player p : players) {
            List<Card> enoughType = CardLists.filter(list, CardPredicates.isOwner(p));
            if (enoughType.size() < c) {
                list.removeAll(enoughType);
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

    private PaymentDecision exileFromSame(CostExile cost, List<Card> list, int nNeeded, List<Player> payableZone) {
        if (nNeeded == 0) {
            return PaymentDecision.number(0);
        }
        final PlayerView view = SGuiChoose.oneOrNone(getGui(), String.format("Exile from whose %s?", cost.getFrom()),
                controller.getPlayerViews(payableZone));
        final Player p = controller.getPlayer(view);
        if (p == null) {
            return null;
        }

        List<Card> typeList = CardLists.filter(list, CardPredicates.isOwner(p));
        int count = typeList.size();
        if(count < nNeeded)
            return null;
        
        List<Card> toExile = SGuiChoose.many(getGui(), "Exile from " + cost.getFrom(), "To be exiled", count - nNeeded, typeList, null);
        return PaymentDecision.card(toExile);
    }
    
    @Override
    public PaymentDecision visit(CostExileFromStack cost) {
        final String amount = cost.getAmount();
        final Game game = player.getGame(); 

        Integer c = cost.convertAmount();
        String type = cost.getType();
        List<SpellAbility> saList = new ArrayList<SpellAbility>();
        ArrayList<String> descList = new ArrayList<String>();

        for (SpellAbilityStackInstance si : game.getStack()) {
            final Card stC = si.getSourceCard();
            final SpellAbility stSA = si.getSpellAbility().getRootAbility();
            if (stC.isValid(cost.getType().split(";"), ability.getActivatingPlayer(), source) && stSA.isSpell()) {
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
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }

        if (saList.size() < c) {
            return null;
        }
        
        List<SpellAbility> exiled = new ArrayList<SpellAbility>();
        for (int i = 0; i < c; i++) {
            //Have to use the stack descriptions here because some copied spells have no description otherwise
            final String o = SGuiChoose.oneOrNone(getGui(), "Exile from Stack", descList);

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
        final List<Card> list = player.getCardsIn(ZoneType.Library, nNeeded);

        if (list.size() > nNeeded || !player.getController().confirmPayment(cost, "Exile " + Lang.nounWithAmount(nNeeded, "card") + " from the top of your library?")) {
            return null;
        }

        return PaymentDecision.card(list);
    }

    private PaymentDecision exileFromMiscZone(CostExile cost, SpellAbility sa, int nNeeded, List<Card> typeList) {
        if (typeList.size() < nNeeded)
            return null;
        
        List<Card> exiled = new ArrayList<Card>();
        for (int i = 0; i < nNeeded; i++) {
            final CardView view = SGuiChoose.oneOrNone(getGui(), "Exile from " + cost.getFrom(), controller.getCardViews(typeList));
            final Card c = controller.getCard(view);

            if (c != null) {
                typeList.remove(c);
                exiled.add(c);
            } else {
                return null;
            }
        }
        return PaymentDecision.card(exiled);
    }

    private PaymentDecision exileFromTopGraveType(SpellAbility sa, int nNeeded, List<Card> typeList) {
        if (typeList.size() < nNeeded)
            return null;
        
        Collections.reverse(typeList);
        return PaymentDecision.card(Lists.newArrayList(Iterables.limit(typeList, nNeeded)));
    }    

    @Override
    public PaymentDecision visit(CostExiledMoveToGrave cost) {
        Integer c = cost.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }

        final Player activator = ability.getActivatingPlayer();
        List<Card> list = activator.getGame().getCardsIn(ZoneType.Exile);
        list = CardLists.getValidCards(list, cost.getType().split(";"), activator, source);

        if (list.size() < c)
            return null;

        final List<CardView> choice = SGuiChoose.many(getGui(), "Choose an exiled card to put into graveyard", "To graveyard", c, 
                controller.getCardViews(list), controller.getCardView(source));
        return PaymentDecision.card(controller.getCards(choice));
    }

    @Override
    public PaymentDecision visit(CostFlipCoin cost) {
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
    public PaymentDecision visit(CostGainControl cost) {
        final String amount = cost.getAmount();

        Integer c = cost.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, amount, ability);
        }
        final List<Card> list = player.getCardsIn(ZoneType.Battlefield);
        List<Card> validCards = CardLists.getValidCards(list, cost.getType().split(";"), player, source);

        InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, validCards);
        final String desc = cost.getTypeDescription() == null ? cost.getType() : cost.getTypeDescription();
        inp.setMessage("Gain control of %d " + desc);
        inp.showAndWait();
        if (inp.hasCancelled()) {
            return null;
        }
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(CostGainLife cost) {
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

        if (cost.getCntPlayers() == Integer.MAX_VALUE) // applied to all players who can gain
            return PaymentDecision.players(oppsThatCanGainLife);

        final StringBuilder sb = new StringBuilder();
        sb.append(source.getName()).append(" - Choose an opponent to gain ").append(c).append(" life:");

        final PlayerView chosenToGainView = SGuiChoose.oneOrNone(getGui(), sb.toString(), controller.getPlayerViews(oppsThatCanGainLife));
        final Player chosenToGain = controller.getPlayer(chosenToGainView);
        if (null == chosenToGain)
            return null;
        else
            return PaymentDecision.players(Lists.newArrayList(chosenToGain));
    }

    @Override
    public PaymentDecision visit(CostMill cost) {
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

        if (!player.getController().confirmPayment(cost, "Mill " + c + " card" + (c == 1 ? "" : "s") + " from your library?")) {
            return null;
        }
        return PaymentDecision.card(player.getCardsIn(ZoneType.Library, c));
    }

    @Override
    public PaymentDecision visit(CostPayLife cost) {
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
                int maxLifePayment = limit < life ? limit : life;
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
    public PaymentDecision visit(CostPartMana cost) {
        // only interactive payment possible for now =(
        return new PaymentDecision(0);
    }

    @Override
    public PaymentDecision visit(CostPutCardToLib cost) {
        final String amount = cost.getAmount();
        Integer c = cost.convertAmount();

        List<Card> list = cost.sameZone ? player.getGame().getCardsIn(cost.getFrom()) : player.getCardsIn(cost.getFrom());

        if (c == null) {
            final String sVar = ability.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = chooseXValue(cost.getLKIList().size());
            } else {
                c = AbilityUtils.calculateAmount(source, amount, ability);
            }
        }
        
        list = CardLists.getValidCards(list, cost.getType().split(";"), player, source);
        
        if (cost.from == ZoneType.Hand) {
            InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, list);
            inp.setMessage("Put %d card(s) from your " + cost.from );
            inp.setCancelAllowed(true);
            inp.showAndWait();
            return inp.hasCancelled() ? null : PaymentDecision.card(inp.getSelected());
        }
        
        if (cost.sameZone){
            List<Player> players = player.getGame().getPlayers();
            List<Player> payableZone = new ArrayList<Player>();
            for (Player p : players) {
                List<Card> enoughType = CardLists.filter(list, CardPredicates.isOwner(p));
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


    private PaymentDecision putFromMiscZone(SpellAbility sa, int nNeeded, List<Card> typeList, ZoneType fromZone) {
        if(typeList.size() < nNeeded)
            return null;

        final List<CardView> viewList = controller.getCardViews(typeList);
        List<Card> chosen = new ArrayList<>();
        for (int i = 0; i < nNeeded; i++) {
            final CardView view = SGuiChoose.oneOrNone(getGui(), "Put from " + fromZone + " to library", viewList);
            final Card c = controller.getCard(view);

            if (c == null)
                return null;

            viewList.remove(view);
            chosen.add(c);
        }
        return PaymentDecision.card(chosen);
    }

    private PaymentDecision putFromSame(List<Card> list, int nNeeded, List<Player> payableZone, ZoneType fromZone) {
        if (nNeeded == 0) {
            return PaymentDecision.number(0);
        }
    
        final List<PlayerView> players = controller.getPlayerViews(payableZone);
        final PlayerView pView = SGuiChoose.oneOrNone(getGui(), String.format("Put cards from whose %s?", fromZone), players);
        final Player p = controller.getPlayer(pView);
        if (p == null) {
            return null;
        }
    
        List<Card> typeList = CardLists.filter(list, CardPredicates.isOwner(p));
        if (typeList.size() < nNeeded)
            return null;

        final List<CardView> viewList = controller.getCardViews(typeList);
        List<Card> chosen = new ArrayList<>();
        for (int i = 0; i < nNeeded; i++) {
            final CardView view = SGuiChoose.oneOrNone(getGui(), "Put cards from " + fromZone + " to Library", viewList);
            final Card c = controller.getCard(view);

            if (c == null)
                return null;

            viewList.remove(view);
            chosen.add(c);
        }
        return PaymentDecision.card(chosen);
    }
    
    @Override
    public PaymentDecision visit(CostPutCounter cost) {
        Integer c = cost.getNumberOfCounters(ability);

        if (cost.payCostFromSource()) {
            cost.setLastPaidAmount(c);
            return PaymentDecision.number(c);
        } 

        // Cards to use this branch: Scarscale Ritual, Wandering Mage - each adds only one counter 
        List<Card> typeList = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), cost.getType().split(";"), player, ability.getHostCard());
        
        InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, typeList);
        inp.setMessage("Put " + Lang.nounWithAmount(c, cost.getCounter().getName() + " counter") + " on " + cost.getDescriptiveType());
        inp.setCancelAllowed(true);
        inp.showAndWait();

        if(inp.hasCancelled())
            return null;

        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(CostReturn cost) {
        final String amount = cost.getAmount();
        Integer c = cost.convertAmount();

        final List<Card> list = player.getCardsIn(ZoneType.Battlefield);
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
                final CardView view = controller.getCardView(card);
                return player.getController().confirmPayment(cost, "Return " + view + " to hand?") ? PaymentDecision.card(card) : null;
            }
        }
        else {
            List<Card> validCards = CardLists.getValidCards(ability.getActivatingPlayer().getCardsIn(ZoneType.Battlefield), cost.getType().split(";"), ability.getActivatingPlayer(), ability.getHostCard());

            InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, validCards);
            inp.setMessage("Return %d " + cost.getType() + " " + cost.getType() + " card(s) to hand");
            inp.showAndWait();
            if (inp.hasCancelled())
                return null;
            
            return PaymentDecision.card(inp.getSelected());
       }
       return null;

    }

    @Override
    public PaymentDecision visit(CostReveal cost) {
        final String amount = cost.getAmount();

        if (cost.payCostFromSource())
            return PaymentDecision.card(source);

        if (cost.getType().equals("Hand"))
            return PaymentDecision.card(player.getCardsIn(ZoneType.Hand));

        InputSelectCardsFromList inp = null;
        if (cost.getType().equals("SameColor")) {
            Integer num = cost.convertAmount();
            List<Card> handList = player.getCardsIn(ZoneType.Hand);
            final List<Card> handList2 = handList;
            handList = CardLists.filter(handList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    for (Card card : handList2) {
                        if (!card.equals(c) && card.sharesColorWith(c)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            if (num == 0) 
                return PaymentDecision.number(0);

            inp = new InputSelectCardsFromList(controller, num, handList) {
                private static final long serialVersionUID = 8338626212893374798L;

                @Override
                protected boolean onCardSelected(Card c, ITriggerEvent triggerEvent) {
                    Card firstCard = Iterables.getFirst(this.selected, null);
                    if (firstCard != null && !CardPredicates.sharesColorWith(firstCard).apply(c)) {
                        return false;
                    }
                    return super.onCardSelected(c, triggerEvent);
                }
            };
            inp.setMessage("Select " + Lang.nounWithAmount(num, "card" ) + " of same color to reveal.");

        } else {
            Integer num = cost.convertAmount();

            List<Card> handList = player.getCardsIn(ZoneType.Hand);
            handList = CardLists.getValidCards(handList, cost.getType().split(";"), player, ability.getHostCard());

            if (num == null) {
                final String sVar = ability.getSVar(amount);
                if (sVar.equals("XChoice")) {
                    num = chooseXValue(handList.size());
                } else {
                    num = AbilityUtils.calculateAmount(source, amount, ability);
                }
            }
            if ( num == 0 )
                return PaymentDecision.number(0);;
                
            inp = new InputSelectCardsFromList(controller, num, num, handList);
            inp.setMessage("Select %d more " + cost.getDescriptiveType() + " card(s) to reveal.");
        }
        inp.setCancelAllowed(true);
        inp.showAndWait();
        if (inp.hasCancelled())
            return null;
        
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(CostRemoveAnyCounter cost) {
        Integer c = cost.convertAmount();
        final String type = cost.getType();

        if (c == null) {
            c = AbilityUtils.calculateAmount(source, cost.getAmount(), ability);
        }

        List<Card> list = new ArrayList<Card>(player.getCardsIn(ZoneType.Battlefield));
        list = CardLists.getValidCards(list, type.split(";"), player, source);


        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card card) {
                return card.hasCounters();
            }
        });
        InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, list);
        inp.setMessage("Select " + cost.getDescriptiveType() + " to remove a counter");
        inp.setCancelAllowed(false);
        inp.showAndWait();
        Card selected = inp.getFirstSelected();
        final Map<CounterType, Integer> tgtCounters = selected.getCounters();
        final ArrayList<CounterType> typeChoices = new ArrayList<CounterType>();
        for (CounterType key : tgtCounters.keySet()) {
            if (tgtCounters.get(key) > 0) {
                typeChoices.add(key);
            }
        }

        String prompt = "Select type counters to remove";
        cost.setCounterType(SGuiChoose.one(getGui(), prompt, typeChoices));
        
        return PaymentDecision.card(selected, cost.getCounter());
    }

    public static final class InputSelectCardToRemoveCounter extends InputSelectManyBase<Card> {
        private static final long serialVersionUID = 2685832214519141903L;

        private final Map<Card,Integer> cardsChosen;
        private final CounterType counterType;
        private final List<Card> validChoices;

        public InputSelectCardToRemoveCounter(final PlayerControllerHuman controller, int cntCounters, CounterType cType, List<Card> validCards) {
            super(controller, cntCounters, cntCounters);
            this.validChoices = validCards;
            counterType = cType;
            cardsChosen = cntCounters > 0 ? new HashMap<Card, Integer>() : null; 
        }

        @Override
        protected boolean onCardSelected(Card c, ITriggerEvent triggerEvent) {
            if (!isValidChoice(c) || c.getCounters(counterType) <= getTimesSelected(c)) {
                return false;
            }

            int tc = getTimesSelected(c);
            cardsChosen.put(c, tc+1);

            onSelectStateChanged(c, true);
            refresh();
            return true;
        };

        @Override
        protected boolean hasEnoughTargets() {
            return hasAllTargets();
        }

        @Override
        protected boolean hasAllTargets() {
            int sum = getDistibutedCounters();
            return sum >= max;
        }

        protected String getMessage() {
            return max == Integer.MAX_VALUE
                ? String.format(message, getDistibutedCounters())
                : String.format(message, max - getDistibutedCounters());
        }

        private int getDistibutedCounters() {
            int sum = 0;
            for(Entry<Card, Integer> kv : cardsChosen.entrySet()) {
                sum += kv.getValue().intValue();
            }
            return sum;
        }
        
        protected final boolean isValidChoice(GameEntity choice) {
            return validChoices.contains(choice);
        }

        public int getTimesSelected(Card c) {
            return cardsChosen.containsKey(c) ? cardsChosen.get(c).intValue() : 0;
        }

        @Override
        public Collection<Card> getSelected() {
            return cardsChosen.keySet();
        }
    }
    
    @Override
    public PaymentDecision visit(CostRemoveCounter cost) {

        final String amount = cost.getAmount();
        Integer c = cost.convertAmount();
        final String type = cost.getType();

        String sVarAmount = ability.getSVar(amount);
        int cntRemoved = 1;
        if (c != null)  
            cntRemoved = c.intValue();
        else if (!"XChoice".equals(sVarAmount)) {
            cntRemoved = AbilityUtils.calculateAmount(source, amount, ability);
        }

        if (cost.payCostFromSource()) {
            int maxCounters = source.getCounters(cost.counter);
            if (amount.equals("All")) {
                final CardView view = controller.getCardView(ability.getHostCard());
                if (!SGuiDialog.confirm(getGui(), view, "Remove all counters?")) {
                    return null;
                }
                cntRemoved = maxCounters;
            }
            else if ( c == null && "XChoice".equals(sVarAmount)) { 
                cntRemoved = chooseXValue(maxCounters);
            }

            if (maxCounters < cntRemoved) 
                return null;
            return PaymentDecision.card(source, cntRemoved >= 0 ? cntRemoved : maxCounters);
            
        } else if (type.equals("OriginalHost")) {
            int maxCounters = ability.getOriginalHost().getCounters(cost.counter);
            if (amount.equals("All")) {
                cntRemoved = maxCounters;
            }
            if (maxCounters < cntRemoved) 
                return null;

            return PaymentDecision.card(ability.getOriginalHost(), cntRemoved >= 0 ? cntRemoved : maxCounters);
        }

        List<Card> validCards = CardLists.getValidCards(player.getCardsIn(cost.zone), type.split(";"), player, source);
        if (cost.zone.equals(ZoneType.Battlefield)) {
            final InputSelectCardToRemoveCounter inp = new InputSelectCardToRemoveCounter(controller, cntRemoved, cost.counter, validCards);
            inp.setMessage("Remove %d " + cost.counter.getName() + " counters from " + cost.getDescriptiveType());
            inp.setCancelAllowed(true);
            inp.showAndWait();
            if(inp.hasCancelled())
                return null;

            // Have to hack here: remove all counters minus one, without firing any triggers,
            // triggers will fire when last is removed by executePayment.
            // They don't care how many were removed anyway
            // int sum = 0;
            for(Card crd : inp.getSelected()) {
                int removed = inp.getTimesSelected(crd);
               // sum += removed;
                if(removed < 2) continue;
                int oldVal = crd.getCounters().get(cost.counter).intValue();
                crd.getCounters().put(cost.counter, Integer.valueOf(oldVal - removed + 1));
            }
            return PaymentDecision.card(inp.getSelected(), 1);
        } 

        // Rift Elemental only - always removes 1 counter, so there will be no code for N counters.
        List<CardView> suspended = Lists.newArrayList();
        for (final Card crd : validCards)
            if (crd.getCounters( cost.counter) > 0)
                suspended.add(controller.getCardView(crd));

        final CardView view = SGuiChoose.oneOrNone(getGui(), "Remove counter(s) from a card in " + cost.zone, suspended);
        final Card card = controller.getCard(view);
        return null == card ? null : PaymentDecision.card(card, c);
    }

    @Override
    public PaymentDecision visit(CostSacrifice cost) {
        final String amount = cost.getAmount();
        final String type = cost.getType();

        List<Card> list = new ArrayList<Card>(player.getCardsIn(ZoneType.Battlefield));
        list = CardLists.getValidCards(list, type.split(";"), player, source);
        if (player.hasKeyword("You can't sacrifice creatures to cast spells or activate abilities.")) {
            list = CardLists.getNotType(list, "Creature");
        }

        if (cost.payCostFromSource()) {
            if (source.getController() == ability.getActivatingPlayer() && source.isInPlay()) {
                return player.getController().confirmPayment(cost, "Sacrifice " + source.getName() + "?") ? PaymentDecision.card(source) : null;
            } else 
                return null;
        }
        
        if (amount.equals("All"))
            return PaymentDecision.card(list);
        
    
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
        InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, list);
        inp.setMessage("Select a " + cost.getDescriptiveType() + " to sacrifice (%d left)");
        inp.setCancelAllowed(true);
        inp.showAndWait();
        if ( inp.hasCancelled() )
            return null;

        return PaymentDecision.card(inp.getSelected());

    }

    @Override
    public PaymentDecision visit(CostTap cost) {
        // if (!canPay(ability, source, ability.getActivatingPlayer(),
        // payment.getCost()))
        // return false;
        return PaymentDecision.number(1);
    }

    @Override
    public PaymentDecision visit(CostTapType cost) {
        List<Card> typeList = new ArrayList<Card>(player.getCardsIn(ZoneType.Battlefield));
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

        typeList = CardLists.getValidCards(typeList, type.split(";"), player, ability.getHostCard());
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
            final List<Card> List2 = typeList;
            typeList = CardLists.filter(typeList, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    for (Card card : List2) {
                        if (!card.equals(c) && card.sharesCreatureTypeWith(c)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            if (c == 0) return PaymentDecision.number(0);
            List<Card> tapped = new ArrayList<Card>();
            while (c > 0) {
                InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 1, 1, typeList);
                inp.setMessage("Select one of the cards to tap. Already chosen: " + tapped);
                inp.setCancelAllowed(true);
                inp.showAndWait();
                if (inp.hasCancelled())
                    return null;
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
            int i = Integer.parseInt(totalP);
            InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, 0, typeList.size(), typeList);
            inp.setMessage("Select a card to tap.");
            inp.setUnselectAllowed(true);
            inp.setCancelAllowed(true);
            inp.showAndWait();

            if (inp.hasCancelled() || CardLists.getTotalPower(inp.getSelected()) < i) {
                return null;
            } else {
                return PaymentDecision.card(inp.getSelected());
            }
        }
        
        InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, typeList);
        inp.setMessage("Select a " + cost.getDescriptiveType() + " to tap (%d left)");
        inp.showAndWait();
        if ( inp.hasCancelled() )
            return null;

        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(CostUntapType cost) {
        List<Card> typeList = CardLists.getValidCards(player.getGame().getCardsIn(ZoneType.Battlefield), cost.getType().split(";"),
                player, ability.getHostCard());
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
        InputSelectCardsFromList inp = new InputSelectCardsFromList(controller, c, c, typeList);
        inp.setMessage("Select a " + cost.getDescriptiveType() + " to untap (%d left)");
        inp.showAndWait();
        if( inp.hasCancelled() || inp.getSelected().size() != c )
            return null;
        return PaymentDecision.card(inp.getSelected());
    }

    @Override
    public PaymentDecision visit(CostUntap cost) {
        return PaymentDecision.number(1);
    }

    @Override
    public PaymentDecision visit(CostUnattach cost) {
        final Card source = ability.getHostCard();
        
        Card cardToUnattach = cost.findCardToUnattach(source, player, ability);
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
