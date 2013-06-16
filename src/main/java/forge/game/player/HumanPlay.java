package forge.game.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates.Presets;
import forge.CardPredicates;
import forge.CounterType;
import forge.FThreads;
import forge.GameLogEntryType;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.ability.effects.CharmEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostDamage;
import forge.card.cost.CostDiscard;
import forge.card.cost.CostDraw;
import forge.card.cost.CostExile;
import forge.card.cost.CostMill;
import forge.card.cost.CostPart;
import forge.card.cost.CostPartMana;
import forge.card.cost.CostPartWithList;
import forge.card.cost.CostPayLife;
import forge.card.cost.CostPayment;
import forge.card.cost.CostPutCardToLib;
import forge.card.cost.CostPutCounter;
import forge.card.cost.CostRemoveAnyCounter;
import forge.card.cost.CostRemoveCounter;
import forge.card.cost.CostReturn;
import forge.card.cost.CostReveal;
import forge.card.cost.CostSacrifice;
import forge.card.cost.CostTapType;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostBeingPaid;
import forge.card.spellability.Ability;
import forge.card.spellability.HumanPlaySpellAbility;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.GameActionUtil;
import forge.game.Game;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.gui.input.InputPayMana;
import forge.gui.input.InputPayManaExecuteCommands;
import forge.gui.input.InputPayManaSimple;
import forge.gui.input.InputSelectCards;
import forge.gui.input.InputSelectCardsFromList;
import forge.util.Lang;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class HumanPlay {

    /**
     * TODO: Write javadoc for Constructor.
     */
    public HumanPlay() {
        // TODO Auto-generated constructor stub
    }

    /**
     * <p>
     * playSpellAbility.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final static void playSpellAbility(Player p, SpellAbility sa) {
        FThreads.assertExecutedByEdt(false);

        if (sa == Ability.PLAY_LAND_SURROGATE) {
            p.playLand(sa.getSourceCard(), false);
            p.getGame().getPhaseHandler().setPriority(p);
            return;
        }

        
        sa.setActivatingPlayer(p);
    
        final Card source = sa.getSourceCard();
        
        source.setSplitStateToPlayAbility(sa);
    
        if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
            CharmEffect.makeChoices(sa);
        }
    
        sa = chooseOptionalAdditionalCosts(p, sa);
    
        if (sa == null) {
            return;
        }
    
        // Need to check PayCosts, and Ability + All SubAbilities for Target
        boolean newAbility = sa.getPayCosts() != null;
        SpellAbility ability = sa;
        while ((ability != null) && !newAbility) {
            final Target tgt = ability.getTarget();
    
            newAbility |= tgt != null;
            ability = ability.getSubAbility();
        }
    
        // System.out.println("Playing:" + sa.getDescription() + " of " + sa.getSourceCard() +  " new = " + newAbility);
        if (newAbility) {
            CostPayment payment = null;
            if (sa.getPayCosts() == null) {
                payment = new CostPayment(new Cost("0", sa.isAbility()), sa);
            } else {
                payment = new CostPayment(sa.getPayCosts(), sa);
            }
    
            final HumanPlaySpellAbility req = new HumanPlaySpellAbility(sa, payment);
            req.fillRequirements(false, false, false);
        } else {
            if (payManaCostIfNeeded(p, sa)) {
                if (sa.isSpell() && !source.isCopiedSpell()) {
                    sa.setSourceCard(p.getGame().getAction().moveToStack(source));
                }
                p.getGame().getStack().add(sa);
            } 
        }
    }

    /**
     * choose optional additional costs. For HUMAN only
     * @param activator 
     * 
     * @param original
     *            the original sa
     * @return an ArrayList<SpellAbility>.
     */
    static SpellAbility chooseOptionalAdditionalCosts(Player p, final SpellAbility original) { 
        if (!original.isSpell()) {
            return original;
        }
        final List<SpellAbility> abilities = GameActionUtil.getOptionalCosts(original);
        return p.getController().getAbilityToPlay(abilities);
    }

    private static boolean payManaCostIfNeeded(final Player p, final SpellAbility sa) {
        final ManaCostBeingPaid manaCost; 
        if (sa.getSourceCard().isCopiedSpell() && sa.isSpell()) {
            manaCost = new ManaCostBeingPaid(ManaCost.ZERO);
        } else {
            manaCost = new ManaCostBeingPaid(sa.getPayCosts().getTotalMana());
            manaCost.applySpellCostChange(sa);
        }
    
        boolean isPaid = manaCost.isPaid();
    
        if( !isPaid ) {
            InputPayMana inputPay = new InputPayManaSimple(p.getGame(), sa, manaCost);
            Singletons.getControl().getInputQueue().setInputAndWait(inputPay);
            isPaid = inputPay.isPaid();
        }
        return isPaid;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param humanPlayer
     * @param c
     */
    public static final void playCardWithoutPayingManaCost(Player player, Card c) {
        final List<SpellAbility> choices = c.getBasicSpells();
        // TODO add Buyback, Kicker, ... , spells here
    
        SpellAbility sa = player.getController().getAbilityToPlay(choices);
    
        if (sa != null) {
            sa.setActivatingPlayer(player);
            playSaWithoutPayingManaCost(player.getGame(), sa);
        }
    }

    /**
     * <p>
     * playSpellAbilityForFree.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public static final void playSaWithoutPayingManaCost(final Game game, final SpellAbility sa) {
        FThreads.assertExecutedByEdt(false);
        final Card source = sa.getSourceCard();
        
        source.setSplitStateToPlayAbility(sa);
    
        if (sa.getPayCosts() != null) {
            if (sa.getApi() == ApiType.Charm && !sa.isWrapper()) {
                CharmEffect.makeChoices(sa);
            }
            final CostPayment payment = new CostPayment(sa.getPayCosts(), sa);
    
            final HumanPlaySpellAbility req = new HumanPlaySpellAbility(sa, payment);
            req.fillRequirements(false, true, false);
        } else {
            if (sa.isSpell()) {
                final Card c = sa.getSourceCard();
                if (!c.isCopiedSpell()) {
                    sa.setSourceCard(game.getAction().moveToStack(c));
                }
            }
            game.getStack().add(sa);
        }
    }

    /**
     * <p>
     * playSpellAbility_NoStack.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param skipTargeting
     *            a boolean.
     */
    public final static void playSpellAbilityNoStack(final Player player, final SpellAbility sa) {
        playSpellAbilityNoStack(player, sa, false);
    }

    public final static void playSpellAbilityNoStack(final Player player, final SpellAbility sa, boolean useOldTargets) {
        sa.setActivatingPlayer(player);
    
        if (sa.getPayCosts() != null) {
            final HumanPlaySpellAbility req = new HumanPlaySpellAbility(sa, new CostPayment(sa.getPayCosts(), sa));
            
            req.fillRequirements(useOldTargets, false, true);
        } else {
            if (payManaCostIfNeeded(player, sa)) {
                AbilityUtils.resolve(sa);
            }
    
        }
    }

    // ------------------------------------------------------------------------
    
    private static int getAmountFromPart(CostPart part, Card source, SpellAbility sourceAbility) {
        String amountString = part.getAmount();
        return StringUtils.isNumeric(amountString) ? Integer.parseInt(amountString) : AbilityUtils.calculateAmount(source, amountString, sourceAbility);
    }

    /**
     * TODO: Write javadoc for this method.
     * @param part
     * @param source
     * @param sourceAbility
     * @return
     */
    private static int getAmountFromPartX(CostPart part, Card source, SpellAbility sourceAbility) {
        String amountString = part.getAmount();
        return StringUtils.isNumeric(amountString) ? Integer.parseInt(amountString) : CardFactoryUtil.xCount(source, source.getSVar(amountString));
    }

    /**
     * <p>
     * payCostDuringAbilityResolve.
     * </p>
     * 
     * @param ability
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param cost
     *            a {@link forge.card.cost.Cost} object.
     * @param paid
     *            a {@link forge.Command} object.
     * @param unpaid
     *            a {@link forge.Command} object.
     * @param sourceAbility TODO
     */
    public static boolean payCostDuringAbilityResolve(final Player p, final Card source, final Cost cost, SpellAbility sourceAbility
            , String prompt) {
        
        // Only human player pays this way
        Card current = null; // Used in spells with RepeatEach effect to distinguish cards, Cut the Tethers
        if (!source.getRemembered().isEmpty()) {
            if (source.getRemembered().get(0) instanceof Card) { 
                current = (Card) source.getRemembered().get(0);
            }
        }
        if (!source.getImprinted().isEmpty()) {
            current = source.getImprinted().get(0);
        }
        
        final List<CostPart> parts =  cost.getCostParts();
        ArrayList<CostPart> remainingParts =  new ArrayList<CostPart>(cost.getCostParts());
        CostPart costPart = null;
        if (!parts.isEmpty()) {
            costPart = parts.get(0);
        }
        final String orString = sourceAbility == null ? "" : " (or: " + sourceAbility.getStackDescription() + ")";
        
        if (parts.isEmpty() || costPart.getAmount().equals("0")) {
            return GuiDialog.confirm(source, "Do you want to pay 0?" + orString);
        }

        //the following costs do not need inputs
        for (CostPart part : parts) {
            boolean mayRemovePart = true;
            
            if (part instanceof CostPayLife) {
                final int amount = getAmountFromPart(part, source, sourceAbility);
                if (!p.canPayLife(amount))
                    return false;

                if (false == GuiDialog.confirm(source, "Do you want to pay " + amount + " life?" + orString))
                    return false;

                p.payLife(amount, null);
            }

            else if (part instanceof CostDraw) {
                final int amount = getAmountFromPart(part, source, sourceAbility);
                List<Player> res = new ArrayList<Player>();
                String type = part.getType();
                for (Player player : p.getGame().getPlayers()) {
                    if (player.isValid(type, p, source) && player.canDraw()) {
                        res.add(player);
                    }
                }

                if (res.isEmpty()) {
                    return false;
                }

                StringBuilder sb = new StringBuilder();

                sb.append("Do you want to ");
                sb.append(res.contains(p) ? "" : "let that player ");
                sb.append(amount);
                sb.append(" card(s)?" + orString);

                if (!GuiDialog.confirm(source, sb.toString())) {
                    return false;
                }

                for (Player player : res) {
                    player.drawCards(amount);
                }
            }

            else if (part instanceof CostMill) {
                final int amount = getAmountFromPart(part, source, sourceAbility);
                final List<Card> list = p.getCardsIn(ZoneType.Library);
                if (list.size() < amount) return false;
                if (!GuiDialog.confirm(source, "Do you want to mill " + amount + " card(s)?" + orString))
                    return false;
                List<Card> listmill = p.getCardsIn(ZoneType.Library, amount);
                ((CostMill) part).executePayment(sourceAbility, listmill);
            }
    
            else if (part instanceof CostDamage) {
                int amount = getAmountFromPartX(part, source, sourceAbility);
                if (!p.canPayLife(amount))
                    return false;
    
                if (false == GuiDialog.confirm(source, "Do you want " + source + " to deal " + amount + " damage to you?"))
                    return false;
                
                p.addDamage(amount, source);
            }
    
            else if (part instanceof CostPutCounter) {
                CounterType counterType = ((CostPutCounter) part).getCounter();
                int amount = getAmountFromPartX(part, source, sourceAbility);
                if (part.payCostFromSource()) {
                    if (!source.canReceiveCounters(counterType)) {
                        String message = String.format("Won't be able to pay upkeep for %s but it can't have %s counters put on it.", source, counterType.getName());
                        p.getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, message);
                        return false;
                    }

                    if (!GuiDialog.confirm(source, "Do you want to put " + Lang.nounWithAmount(amount, counterType.getName() + " counter") + " on " + source + "?")) 
                        return false;

                    source.addCounter(counterType, amount, false);
                } else {
                    List<Card> list = p.getGame().getCardsIn(ZoneType.Battlefield);
                    list = CardLists.getValidCards(list, part.getType().split(";"), p, source);
                    boolean hasPaid = payCostPart(sourceAbility, (CostPartWithList)part, amount, list, "add a counter." + orString);
                    if(!hasPaid) return false;
                }
            }
    
            else if (part instanceof CostRemoveCounter) {
                CounterType counterType = ((CostRemoveCounter) part).getCounter();
                int amount = getAmountFromPartX(part, source, sourceAbility);
                
                if (!part.canPay(sourceAbility))
                    return false;
    
                if ( false == GuiDialog.confirm(source, "Do you want to remove " + Lang.nounWithAmount(amount, counterType.getName() + " counter") + " from " + source + "?"))
                    return false;
    
                source.subtractCounter(counterType, amount);
            }

            else if (part instanceof CostRemoveAnyCounter) {
                int amount = getAmountFromPartX(part, source, sourceAbility);
                List<Card> list = new ArrayList<Card>(p.getCardsIn(ZoneType.Battlefield));
                int allCounters = 0;
                for (Card c : list) {
                    final Map<CounterType, Integer> tgtCounters = c.getCounters();
                    for (Integer value : tgtCounters.values()) {
                        allCounters += value;
                    }
                }
                if (allCounters < amount) return false;
                if (!GuiDialog.confirm(source, "Do you want to remove counters from " + part.getDescriptiveType() + " ?")) {
                    return false;
                }

                list = CardLists.getValidCards(list, ((CostRemoveAnyCounter) part).getType().split(";"), p, source);
                while (amount > 0) {
                    final CounterType counterType;
                    list = CardLists.filter(list, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card card) {
                            return card.hasCounters();
                        }
                    });
                    if (list.isEmpty()) return false;
                    InputSelectCards inp = new InputSelectCardsFromList(1, 1, list);
                    inp.setMessage("Select a card to remove a counter");
                    inp.setCancelAllowed(true);
                    Singletons.getControl().getInputQueue().setInputAndWait(inp);
                    if (inp.hasCancelled())
                        continue;
                    Card selected = inp.getSelected().get(0);
                    final Map<CounterType, Integer> tgtCounters = selected.getCounters();
                    final ArrayList<CounterType> typeChoices = new ArrayList<CounterType>();
                    for (CounterType key : tgtCounters.keySet()) {
                        if (tgtCounters.get(key) > 0) {
                            typeChoices.add(key);
                        }
                    }
                    if (typeChoices.size() > 1) {
                        String cprompt = "Select type counters to remove";
                        counterType = GuiChoose.one(cprompt, typeChoices);
                    } else {
                        counterType = typeChoices.get(0);
                    }
                    selected.subtractCounter(counterType, 1);
                    amount--;
                }
            }

            else if (part instanceof CostExile) {
                if ("All".equals(part.getType())) {
                    if (false == GuiDialog.confirm(source, "Do you want to exile all cards in your graveyard?"))
                        return false;
                        
                    List<Card> cards = new ArrayList<Card>(p.getCardsIn(ZoneType.Graveyard));
                    for (final Card card : cards) {
                        p.getGame().getAction().exile(card);
                    }
                } else {
                    CostExile costExile = (CostExile) part;
                    ZoneType from = costExile.getFrom();
                    List<Card> list = CardLists.getValidCards(p.getCardsIn(from), part.getType().split(";"), p, source);
                    final int nNeeded = getAmountFromPart(costPart, source, sourceAbility);
                    if (list.size() < nNeeded)
                        return false;
                    if (from == ZoneType.Library) {
                        if (!GuiDialog.confirm(source, "Do you want to exile card(s) from you library?")) {
                            return false;
                        }
                        list = list.subList(0, nNeeded);
                        for (Card c : list) {
                            p.getGame().getAction().exile(c);
                        }
                        return true;
                    }
                    // replace this with input
                    for (int i = 0; i < nNeeded; i++) {
                        final Card c = GuiChoose.oneOrNone("Exile from " + from, list);
                        if (c == null)
                            return false;
                            
                        list.remove(c);
                        p.getGame().getAction().exile(c);
                    }
                }
            }

            else if (part instanceof CostPutCardToLib) {
                int amount = Integer.parseInt(((CostPutCardToLib) part).getAmount());
                final ZoneType from = ((CostPutCardToLib) part).getFrom();
                final boolean sameZone = ((CostPutCardToLib) part).isSameZone();
                List<Card> list;
                if (sameZone) {
                    list = p.getGame().getCardsIn(from);
                } else {
                    list = p.getCardsIn(from);
                }
                list = CardLists.getValidCards(list, part.getType().split(";"), p, source);

                if (sameZone) { // Jotun Grunt
                    List<Player> players = p.getGame().getPlayers();
                    List<Player> payableZone = new ArrayList<Player>();
                    for (Player player : players) {
                        List<Card> enoughType = CardLists.filter(list, CardPredicates.isOwner(player));
                        if (enoughType.size() < amount) {
                            list.removeAll(enoughType);
                        } else {
                            payableZone.add(player);
                        }
                    }
                    Player chosen = GuiChoose.oneOrNone(String.format("Put cards from whose %s?", from), payableZone);
                    if (chosen == null) {
                        return false;
                    }

                    List<Card> typeList = CardLists.filter(list, CardPredicates.isOwner(chosen));

                    for (int i = 0; i < amount; i++) {
                        if (typeList.isEmpty()) {
                            return false;
                        }

                        final Card c = GuiChoose.oneOrNone("Put cards to Library", typeList);

                        if (c != null) {
                            typeList.remove(c);
                            p.getGame().getAction().moveToLibrary(c, Integer.parseInt(((CostPutCardToLib) part).getLibPos()));
                        } else {
                            return false;
                        }
                    }
                } else if (from == ZoneType.Hand) { // Tainted Specter
                    boolean hasPaid = payCostPart(sourceAbility, (CostPartWithList)part, amount, list, "put into library." + orString);
                    if (!hasPaid) {
                        return false;
                    }
                }
                return true;
            }

            else if (part instanceof CostSacrifice) {
                int amount = Integer.parseInt(((CostSacrifice)part).getAmount());
                List<Card> list = CardLists.getValidCards(p.getCardsIn(ZoneType.Battlefield), part.getType(), p, source);
                boolean hasPaid = payCostPart(sourceAbility, (CostPartWithList)part, amount, list, "sacrifice." + orString);
                if(!hasPaid) return false;
            } else if (part instanceof CostReturn) {
                List<Card> list = CardLists.getValidCards(p.getCardsIn(ZoneType.Battlefield), part.getType(), p, source);
                int amount = getAmountFromPartX(part, source, sourceAbility);
                boolean hasPaid = payCostPart(sourceAbility, (CostPartWithList)part, amount, list, "return to hand." + orString);
                if(!hasPaid) return false;
            } else if (part instanceof CostDiscard) {
                List<Card> list = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), part.getType(), p, source);
                int amount = getAmountFromPartX(part, source, sourceAbility);
                boolean hasPaid = payCostPart(sourceAbility, (CostPartWithList)part, amount, list, "discard." + orString);
                if(!hasPaid) return false;
            } else if (part instanceof CostReveal) {
                List<Card> list = CardLists.getValidCards(p.getCardsIn(ZoneType.Hand), part.getType(), p, source);
                int amount = getAmountFromPartX(part, source, sourceAbility);
                boolean hasPaid = payCostPart(sourceAbility, (CostPartWithList)part, amount, list, "reveal." + orString);
                if(!hasPaid) return false;
            } else if (part instanceof CostTapType) {
                List<Card> list = CardLists.getValidCards(p.getCardsIn(ZoneType.Battlefield), part.getType(), p, source);
                list = CardLists.filter(list, Presets.UNTAPPED);
                int amount = getAmountFromPartX(part, source, sourceAbility);
                boolean hasPaid = payCostPart(sourceAbility, (CostPartWithList)part, amount, list, "tap." + orString);
                if(!hasPaid) return false;
            }
            
            else if (part instanceof CostPartMana ) {
                if (!((CostPartMana) part).getManaToPay().isZero()) // non-zero costs require input
                    mayRemovePart = false; 
            } else {
                throw new RuntimeException("GameActionUtil.payCostDuringAbilityResolve - An unhandled type of cost was met: " + part.getClass());
            }
    
            if( mayRemovePart )
                remainingParts.remove(part);
        }
    
    
        if (remainingParts.isEmpty()) {
            return true;
        }
        if (remainingParts.size() > 1) {
            throw new RuntimeException("GameActionUtil.payCostDuringAbilityResolve - Too many payment types - " + source);
        }
        costPart = remainingParts.get(0);
        // check this is a mana cost
        if (!(costPart instanceof CostPartMana ))
            throw new RuntimeException("GameActionUtil.payCostDuringAbilityResolve - The remaining payment type is not Mana.");
    
        if (prompt == null) {
            String promptCurrent = current == null ? "" : "Current Card: " + current + "\r\n";
            prompt = source + "\r\n" + promptCurrent;
        }
        InputPayMana toSet = new InputPayManaExecuteCommands(p, prompt, cost.getCostMana().getManaToPay());
        Singletons.getControl().getInputQueue().setInputAndWait(toSet);
        return toSet.isPaid();
    }

    private static boolean payCostPart(SpellAbility sourceAbility, CostPartWithList cpl, int amount, List<Card> list, String actionName) {
        if (list.size() < amount) return false;                     // unable to pay (not enough cards)
    
        InputSelectCards inp = new InputSelectCardsFromList(amount, amount, list);
        inp.setMessage("Select %d " + cpl.getDescriptiveType() + " card(s) to " + actionName);
        inp.setCancelAllowed(true);
        
        Singletons.getControl().getInputQueue().setInputAndWait(inp);
        if( inp.hasCancelled() || inp.getSelected().size() != amount)
            return false;
    
        for(Card c : inp.getSelected()) {
            cpl.executePayment(sourceAbility, c);
        }
        if (sourceAbility != null) {
            cpl.reportPaidCardsTo(sourceAbility);
        }
        return true;
    }

}
