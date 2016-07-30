package forge.ai;

import com.google.common.collect.Lists;
import forge.card.ColorSet;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.cost.*;
import forge.game.player.Player;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;
import forge.util.MyRandom;
import forge.util.TextUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;


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
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostRemoveCounter) {
                final CostRemoveCounter remCounter = (CostRemoveCounter) part;
    
                final CounterType type = remCounter.counter;
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

        CardCollection hand = new CardCollection(ai.getCardsIn(ZoneType.Hand));

        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostDiscard) {
                final CostDiscard disc = (CostDiscard) part;
    
                final String type = disc.getType();
                if (type.equals("CARDNAME") && source.getAbilityText().contains("Bloodrush")) {
                    continue;
                }
                final CardCollection typeList = CardLists.getValidCards(hand, type.split(","), source.getController(), source, null);
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
                        hand.remove(pref);
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
    
                if (ai.getLife() - amount < remainingLife && !ai.cantLoseForZeroOrLessLife()) {
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
    
                final CardCollection typeList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(","), source.getController(), source, null);
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
                    if (!CardLists.filterControlledBy(source.getEnchantedBy(false), source.getController()).isEmpty()) {
                        return false;
                    }
                    continue;
                }
                final CardCollection typeList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(";"), source.getController(), source, null);
                if (ComputerUtil.getCardPreference(ai, source, "SacCost", typeList) == null) {
                    return false;
                }
            }
        }
        return true;
    }
    
    public static boolean isSacrificeSelfCost(final Cost cost) {
    	 if (cost == null) {
             return false;
         }
         for (final CostPart part : cost.getCostParts()) {
             if (part instanceof CostSacrifice) {
                 if ("CARDNAME".equals(part.getType())) {
                	 return true;
                 }
             }
         }
         return false;
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
    public static boolean checkTapTypeCost(final Player ai, final Cost cost, final Card source) {
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostTapType) {
            	return false;
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
     *            a {@link forge.game.card.Card} object.
     * @param cost
     * @return a boolean.
     */
    public static boolean shouldPayCost(final Player ai, final Card hostCard, final Cost cost) {
    
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostPayLife) {
                if (!ai.cantLoseForZeroOrLessLife()) {
                    continue;
                }
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
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    public static boolean canPayCost(final SpellAbility sa, final Player player) {
        if (sa.getActivatingPlayer() == null) {
            sa.setActivatingPlayer(player); // complaints on NPE had came before this line was added.
        }

        // Check for stuff like Nether Void
        int extraManaNeeded = 0;
        if (sa instanceof Spell) {
            for (Card c : player.getGame().getCardsIn(ZoneType.Battlefield)) {
                final String snem = c.getSVar("AI_SpellsNeedExtraMana");
                if (!StringUtils.isBlank(snem)) {
                    String[] parts = TextUtil.split(snem, ' ');
                    boolean meetsRestriction = parts.length == 1 || player.isValid(parts[1], c.getController(), c, sa);
                    if(!meetsRestriction)
                        continue;

                    try {
                        extraManaNeeded += Integer.parseInt(snem);
                    } catch (final NumberFormatException e) {
                        System.out.println("wrong SpellsNeedExtraMana SVar format on " + c);
                    }
                }
            }
            for (Card c : player.getCardsIn(ZoneType.Command)) {
                final String snem = c.getSVar("SpellsNeedExtraManaEffect");
                if (!StringUtils.isBlank(snem)) {
                    try {
                        extraManaNeeded += Integer.parseInt(snem);
                    } catch (final NumberFormatException e) {
                        System.out.println("wrong SpellsNeedExtraManaEffect SVar format on " + c);
                    }
                }
            }
        }
        
        // Try not to lose Planeswalker if not threatened
        if (sa.getRestrictions().isPwAbility()) {
            final CostPart cost = sa.getPayCosts().getCostParts().get(0);
            if (cost instanceof CostRemoveCounter) {
                if (cost.convertAmount() != null && cost.convertAmount() == sa.getHostCard().getCurrentLoyalty()) {
                    // refuse to pay if opponent has no creature threats or 50% chance otherwise
                    if (player.getOpponent().getCreaturesInPlay().isEmpty() || MyRandom.getRandom().nextFloat() < .5f) {
                        return false;
                    }
                }
            }
        }

        return ComputerUtilMana.canPayManaCost(sa, player, extraManaNeeded) 
            && CostPayment.canPayAdditionalCosts(sa.getPayCosts(), sa);
    } // canPayCost()

    public static boolean willPayUnlessCost(SpellAbility sa, Player payer, Cost cost, boolean alreadyPaid, FCollectionView<Player> payers) {
        final Card source = sa.getHostCard();
        final String aiLogic = sa.getParam("UnlessAI");
        boolean payForOwnOnly = "OnlyOwn".equals(aiLogic);
        boolean payOwner = sa.hasParam("UnlessAI") ? aiLogic.startsWith("Defined") : false;
        boolean payNever = "Never".equals(aiLogic);
        boolean shockland = "Shockland".equals(aiLogic);
        boolean isMine = sa.getActivatingPlayer().equals(payer);
    
        if (payNever) { return false; }
        if (payForOwnOnly && !isMine) { return false; }
        if (payOwner) {
            final String defined = aiLogic.substring(7);
            final Player player = AbilityUtils.getDefinedPlayers(source, defined, sa).get(0);
            if (!payer.equals(player)) {
                return false;
            }
        } else if ("OnlyDontControl".equals(aiLogic)) {
            if (sa.getHostCard() == null || payer.equals(sa.getHostCard().getController())) {
                return false;
            }
        } else if (shockland) {
            if (payer.getLife() > 3 && payer.canPayLife(2)) {
                final int landsize = payer.getLandsInPlay().size() + 1;
                for (Card c : payer.getCardsIn(ZoneType.Hand)) {
                    // if the new land size would equal the CMC of a card in AIs hand, consider playing it untapped, 
                    // otherwise don't bother running other checks
                    if (landsize != c.getCMC()) {
                        continue; 
                    }
                    // try to determine in the AI is actually planning to play a spell ability from the card
                    boolean willPlay = ComputerUtil.hasReasonToPlayCardThisTurn(payer, c);
                    // try to determine if the mana shards provided by the lands would be applicable to pay the mana cost
                    boolean canPay = c.getManaCost().canBePaidWithAvaliable(ColorSet.fromNames(getAvailableManaColors(payer, source)).getColor());

                    if (canPay && willPlay) {
                        return true;
                    }
                }
            }
            return false;
        } else if ("Paralyze".equals(aiLogic)) {
            final Card c = source.getEnchantingCard();
            if (c == null || c.isUntapped()) {
                return false;
            }
        } else if ("MorePowerful".equals(aiLogic)) {
            final int sourceCreatures = sa.getActivatingPlayer().getCreaturesInPlay().size();
            final int payerCreatures = payer.getCreaturesInPlay().size();
            if (payerCreatures > sourceCreatures + 1) {
                return false;
            }
        } else if (aiLogic != null && aiLogic.startsWith("LifeLE")) {
        	// if payer can't lose life its no need to pay unless
        	if (!payer.canLoseLife())
        		return false;
        	else if (payer.getLife() <= Integer.valueOf(aiLogic.substring(6))) {
                return true;
            }
        } else if ("WillAttack".equals(aiLogic)) {
            AiAttackController aiAtk = new AiAttackController(payer);
            Combat combat = new Combat(payer);
            aiAtk.declareAttackers(combat);
            if (combat.getAttackers().isEmpty()) {
                return false;
            }
        } else if ("nonToken".equals(aiLogic) && AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa).get(0).isToken()) {
            return false;
        } else if ("LowPriority".equals(aiLogic) && MyRandom.getRandom().nextInt(100) < 67) {
            return false;
        }

        // AI will only pay when it's not already payed and only opponents abilities
        if (alreadyPaid || (payers.size() > 1 && (isMine && !payForOwnOnly))) {
            return false;
        }

        // AI was crashing because the blank ability used to pay costs
        // Didn't have any of the data on the original SA to pay dependant costs

        return checkLifeCost(payer, cost, source, 4, sa)
            && checkDamageCost(payer, cost, source, 4)
            && (isMine || checkSacrificeCost(payer, cost, source))
            && (isMine || checkDiscardCost(payer, cost, source))
            && (!source.getName().equals("Tyrannize") || payer.getCardsIn(ZoneType.Hand).size() > 2)
            && (!source.getName().equals("Perplex") || payer.getCardsIn(ZoneType.Hand).size() < 2)
            && (!source.getName().equals("Breaking Point") || payer.getCreaturesInPlay().size() > 1)
            && (!source.getName().equals("Chain of Vapor") || (payer.getOpponent().getCreaturesInPlay().size() > 0 && payer.getLandsInPlay().size() > 3));
    }

    public static Set<String> getAvailableManaColors(Player ai, Card additionalLand) {
        return getAvailableManaColors(ai, Lists.newArrayList(additionalLand));
    }

    public static Set<String> getAvailableManaColors(Player ai, List<Card> additionalLands) {
        CardCollection cardsToConsider = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Presets.UNTAPPED);
        Set<String> colorsAvailable = new HashSet<>();

        if (additionalLands != null) {
            GameActionUtil.grantBasicLandsManaAbilities(additionalLands);
            cardsToConsider.addAll(additionalLands);
        }

        for (Card c : cardsToConsider) {
            for (SpellAbility sa : c.getManaAbilities()) {
                colorsAvailable.add(sa.getManaPart().getOrigProduced());
            }
        }

        return colorsAvailable;
    }
}
