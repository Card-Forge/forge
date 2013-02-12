package forge.game.ai;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.CardLists;
import forge.CounterType;
import forge.Singletons;
import forge.card.abilityfactory.AbilityUtils;
import forge.card.cost.Cost;
import forge.card.cost.CostDamage;
import forge.card.cost.CostDiscard;
import forge.card.cost.CostPart;
import forge.card.cost.CostPayLife;
import forge.card.cost.CostPayment;
import forge.card.cost.CostPutCounter;
import forge.card.cost.CostRemoveCounter;
import forge.card.cost.CostSacrifice;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ComputerUtilCost {

    /**
     * Check add m1 m1 counter cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean checkAddM1M1CounterCost(final Cost cost, final Card source) {
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostPutCounter) {
                final CostPutCounter addCounter = (CostPutCounter) part;
                final CounterType type = addCounter.getCounter();
    
                if (type.equals(CounterType.M1M1)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check remove counter cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean checkRemoveCounterCost(final Cost cost, final Card source) {
        if (cost == null) {
            return true;
        }
        double p1p1Percent = .25;
        if (source.isCreature()) {
            p1p1Percent = .1;
        }
        final double otherPercent = .9;
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostRemoveCounter) {
                final CostRemoveCounter remCounter = (CostRemoveCounter) part;
    
                // A card has a 25% chance per counter to be able to pass
                // through here
                // 4+ counters will always pass. 0 counters will never
                final CounterType type = remCounter.getCounter();
                final double percent = type.name().equals("P1P1") ? p1p1Percent : otherPercent;
                final int currentNum = source.getCounters(type);
                if (!part.isTargetingThis()) {
                    if (type.name().equals("P1P1")) {
                        return false;
                    }
                    continue;
                }
    
                Integer amount = part.convertAmount();
                if (amount == null) {
                    amount = currentNum;
                }
                final double chance = percent * (currentNum / amount);
                if (chance <= MyRandom.getRandom().nextFloat()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check discard cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean checkDiscardCost(final Player ai, final Cost cost, final Card source) {
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostDiscard) {
                final CostDiscard disc = (CostDiscard) part;
    
                final String type = disc.getType();
                final List<Card> typeList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Hand), type.split(","), source.getController(), source);
                if (typeList.size() > ai.getMaxHandSize()) {
                    continue;
                }
                int num = AbilityUtils.calculateAmount(source, disc.getAmount(), null);
                for (int i = 0; i < num; i++) {
                    Card pref = ComputerUtil.getCardPreference(ai, source, "DiscardCost", typeList);
                    if (pref == null) {
                        return false;
                    } else {
                        typeList.remove(pref);
                    }
                }
            }
        }
        return true;
    }

    /**
     * Check life cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @param remainingLife
     *            the remaining life
     * @return true, if successful
     */
    public static boolean checkDamageCost(final Player ai, final Cost cost, final Card source, final int remainingLife) {
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostDamage) {
                final CostDamage pay = (CostDamage) part;
                int realDamage = ComputerUtilCombat.predictDamageTo(ai, pay.convertAmount(), source, false);
                if (ai.getLife() - realDamage < remainingLife
                        && realDamage > 0 && !ai.cantLoseForZeroOrLessLife()
                        && ai.canLoseLife()) {
                    return false;
                }
                if (source.getName().equals("Skullscorch") && ai.getCardsIn(ZoneType.Hand).size() < 2) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check life cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @param remainingLife
     *            the remaining life
     * @param sourceAbility TODO
     * @return true, if successful
     */
    public static boolean checkLifeCost(final Player ai, final Cost cost, final Card source, final int remainingLife, SpellAbility sourceAbility) {
        // TODO - Pass in SA for everything else that calls this function
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostPayLife) {
                final CostPayLife payLife = (CostPayLife) part;
    
                Integer amount = payLife.convertAmount();
                if (amount == null) {
                    amount = AbilityUtils.calculateAmount(source, payLife.getAmount(), sourceAbility);
                }
    
                if ((ai.getLife() - amount) < remainingLife) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check creature sacrifice cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean checkCreatureSacrificeCost(final Player ai, final Cost cost, final Card source) {
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice) {
                final CostSacrifice sac = (CostSacrifice) part;
                if (sac.isTargetingThis() && source.isCreature()) {
                    return false;
                }
                final String type = sac.getType();
    
                if (type.equals("CARDNAME")) {
                    continue;
                }
    
                final List<Card> typeList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(","), source.getController(), source);
                if (ComputerUtil.getCardPreference(ai, source, "SacCost", typeList) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check sacrifice cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @param important
     *            is the gain important enough?
     * @return true, if successful
     */
    public static boolean checkSacrificeCost(final Player ai, final Cost cost, final Card source, final boolean important) {
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice) {
                final CostSacrifice sac = (CostSacrifice) part;
    
                final String type = sac.getType();
    
                if (type.equals("CARDNAME")) {
                    if (!important) {
                        return false;
                    }
                    List<Card> auras = new ArrayList<Card>(source.getEnchantedBy());
                    if (!CardLists.filterControlledBy(auras, source.getController()).isEmpty()) {
                        return false;
                    }
                    continue;
                }
    
                final List<Card> typeList =
                        CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(","), source.getController(), source);
                if (ComputerUtil.getCardPreference(ai, source, "SacCost", typeList) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check sacrifice cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean checkSacrificeCost(final Player ai, final Cost cost, final Card source) {
        return checkSacrificeCost(ai, cost, source, true);
    }

    /**
     * <p>
     * shouldPayCost.
     * </p>
     * 
     * @param hostCard
     *            a {@link forge.Card} object.
     * @param costString
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public static boolean shouldPayCost(final Player ai, final Card hostCard, final String costString) {
        final Cost cost = new Cost(hostCard, costString, false);
    
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostPayLife) {
                final int remainingLife = ai.getLife();
                final int lifeCost = ((CostPayLife) part).convertAmount();
                if ((remainingLife - lifeCost) < 10) {
                    return false; //Don't pay life if it would put AI under 10 life
                } else if ((remainingLife / lifeCost) < 4) {
                    return false; //Don't pay life if it is more than 25% of current life
                }
            }
        }
    
        return true;
    } // shouldPayCost()

    /**
     * <p>
     * canPayCost.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public static boolean canPayCost(final SpellAbility sa, final Player player) {
    
        final GameState game = Singletons.getModel().getGame();
        // Check for stuff like Nether Void
        int extraManaNeeded = 0;
        if (sa instanceof Spell) {
            for (Player opp : player.getOpponents()) {
                for (Card c : opp.getCardsIn(ZoneType.Battlefield)) {
                    final String snem = c.getSVar("SpellsNeedExtraMana");
                    if (!StringUtils.isBlank(snem)) {
                        try {
                            extraManaNeeded += Integer.parseInt(snem);
                        } catch (final NumberFormatException e) {
                            System.out.println("wrong SpellsNeedExtraMana SVar format on " + c);
                        }
                    }
                }
            }
        }
    
        if (!ComputerUtilMana.payManaCost(sa, player, true, extraManaNeeded, true)) {
            return false;
        }
    
        return ComputerUtilCost.canPayAdditionalCosts(sa, player, game);
    } // canPayCost()

    /**
     * <p>
     * canPayAdditionalCosts.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public static boolean canPayAdditionalCosts(final SpellAbility sa, final Player player, final GameState game) {
        if (sa.getActivatingPlayer() == null) {
            final StringBuilder sb = new StringBuilder();
            sb.append(sa.getSourceCard());
            sb.append(" in ComputerUtil.canPayAdditionalCosts() without an activating player");
            System.out.println(sb.toString());
            sa.setActivatingPlayer(player);
        }
        return CostPayment.canPayAdditionalCosts(game, sa.getPayCosts(), sa);
    }

}
