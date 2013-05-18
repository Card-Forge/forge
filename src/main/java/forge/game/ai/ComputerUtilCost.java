package forge.game.ai;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.CardLists;
import forge.CounterType;
import forge.card.ability.AbilityUtils;
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
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.TextUtil;

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
                if (!part.payCostFromSource()) {
                    if (type.name().equals("P1P1")) {
                        return false;
                    }
                    continue;
                }

                //don't kill the creature
                if (type.name().equals("P1P1") && source.getLethalDamage() <= 1) {
                    return false;
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
                if (sac.payCostFromSource() && source.isCreature()) {
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
        final Cost cost = new Cost(costString, false);
    
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
        sa.setActivatingPlayer(player); // complaints on NPE had came before this line was added.

        // Check for stuff like Nether Void
        int extraManaNeeded = 0;
        if (sa instanceof Spell) {
            for (Card c : player.getGame().getCardsIn(ZoneType.Battlefield)) {
                final String snem = c.getSVar("AI_SpellsNeedExtraMana");
                if (!StringUtils.isBlank(snem)) {
                    String[] parts = TextUtil.split(snem, ' ');
                    boolean meetsRestriction = parts.length == 1 || player.isValid(parts[1], c.getController(), c);
                    if(!meetsRestriction)
                        continue;

                    try {
                        extraManaNeeded += Integer.parseInt(snem);
                    } catch (final NumberFormatException e) {
                        System.out.println("wrong SpellsNeedExtraMana SVar format on " + c);
                    }
                }
            }
        }

        return ComputerUtilMana.canPayManaCost(sa, player, extraManaNeeded) 
            && CostPayment.canPayAdditionalCosts(sa.getPayCosts(), sa);
    } // canPayCost()

    public static boolean willPayUnlessCost(SpellAbility sa, Player payer, SpellAbility ability, boolean alreadyPaid, List<Player> payers) {
        final Card source = sa.getSourceCard();
        boolean payForOwnOnly = "OnlyOwn".equals(sa.getParam("UnlessAI"));
        boolean payOwner = sa.hasParam("UnlessAI") ? sa.getParam("UnlessAI").startsWith("Defined") : false;
        boolean payNever = "Never".equals(sa.getParam("UnlessAI"));
        boolean shockland = "Shockland".equals(sa.getParam("UnlessAI"));
        boolean isMine = sa.getActivatingPlayer().equals(payer);
    
        if (payNever) { return false; }
        if (payForOwnOnly && !isMine) { return false; }
        if (payOwner) {
            final String defined = sa.getParam("UnlessAI").substring(7);
            final Player player = AbilityUtils.getDefinedPlayers(source, defined, sa).get(0);
            if (!payer.equals(player)) {
                return false;
            }
        } else if (shockland) {
            if (payer.getLife() > 3 && payer.canPayLife(2)) {
                final int landsize = payer.getLandsInPlay().size();
                for (Card c : payer.getCardsIn(ZoneType.Hand)) {
                    if (landsize == c.getCMC()) {
                        return true;
                    }
                }
            }
            return false;
        } else if ("Paralyze".equals(sa.getParam("UnlessAI"))) {
            final Card c = source.getEnchantingCard();
            if (c == null || c.isUntapped()) {
                return false;
            }
        }
    
        // AI will only pay when it's not already payed and only opponents abilities
        if (alreadyPaid || (payers.size() > 1 && (isMine && !payForOwnOnly))) {
            return false;
        }
        
        // AI was crashing because the blank ability used to pay costs
        // Didn't have any of the data on the original SA to pay dependant costs

        return checkLifeCost(payer, ability.getPayCosts(), source, 4, sa)
            && checkDamageCost(payer, ability.getPayCosts(), source, 4)
            && (isMine || checkDiscardCost(payer, ability.getPayCosts(), source))
            && (!source.getName().equals("Tyrannize") || payer.getCardsIn(ZoneType.Hand).size() > 2)
            && (!source.getName().equals("Perplex") || payer.getCardsIn(ZoneType.Hand).size() < 2)
            && (!source.getName().equals("Breaking Point") || payer.getCreaturesInPlay().size() > 1)
            && (!source.getName().equals("Chain of Vapor") || (payer.getOpponent().getCreaturesInPlay().size() > 0 && payer.getLandsInPlay().size() > 3));
    }

}
