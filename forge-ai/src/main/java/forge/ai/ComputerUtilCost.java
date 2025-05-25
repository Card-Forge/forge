package forge.ai;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.ai.AiCardMemory.MemorySet;
import forge.ai.ability.AnimateAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.cost.*;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetChoices;
import forge.game.zone.ZoneType;
import forge.util.IterableUtil;
import forge.util.MyRandom;
import forge.util.TextUtil;


public class ComputerUtilCost {
    private static boolean suppressRecursiveSacCostCheck = false;
    public static void setSuppressRecursiveSacCostCheck(boolean shouldSuppress) {
        suppressRecursiveSacCostCheck = shouldSuppress;
    }

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
            if (part instanceof CostPutCounter addCounter) {
                final CounterType type = addCounter.getCounter();

                if (type.is(CounterEnumType.M1M1)) {
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
    public static boolean checkRemoveCounterCost(final Cost cost, final Card source, final SpellAbility sa) {
        if (cost == null) {
            return true;
        }
        final AiCostDecision decision = new AiCostDecision(sa.getActivatingPlayer(), sa, false);
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostRemoveCounter remCounter) {
                final CounterType type = remCounter.counter;
                if (!part.payCostFromSource()) {
                    if (type.is(CounterEnumType.P1P1)) {
                        return false;
                    }
                    continue;
                }

                // even if it can be paid, removing zero counters should not be done.
                if (part.payCostFromSource() && source.getCounters(type) <= 0) {
                    return false;
                }

                // ignore Loyality abilities with Zero as Cost
                if (!type.is(CounterEnumType.LOYALTY)) {
                    PaymentDecision pay = decision.visit(remCounter);
                    if (pay == null || pay.c <= 0) {
                        return false;
                    }
                }

                //don't kill the creature
                if (type.is(CounterEnumType.P1P1) && source.getLethalDamage() <= 1
                        && !source.hasKeyword(Keyword.UNDYING)) {
                    return false;
                }
            } else if (part instanceof CostRemoveAnyCounter remCounter) {
                PaymentDecision pay = decision.visit(remCounter);
                return pay != null;
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
    public static boolean checkDiscardCost(final Player ai, final Cost cost, final Card source, SpellAbility sa) {
        if (cost == null) {
            return true;
        }

        CardCollection hand = new CardCollection(ai.getCardsIn(ZoneType.Hand));

        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostDiscard disc) {
                final String type = disc.getType();
                final CardCollection typeList;
                int num;
                if (type.equals("Hand")) {
                    typeList = hand;
                    num = hand.size();
                } else {
                    if (type.equals("CARDNAME")) {
                        if (source.getAbilityText().contains("Bloodrush")) {
                            continue;
                        } else if (ai.getGame().getPhaseHandler().is(PhaseType.END_OF_TURN, ai)
                                && !ai.isUnlimitedHandSize() && ai.getCardsIn(ZoneType.Hand).size() > ai.getMaxHandSize()) {
                            // Better do something than just discard stuff
                            return true;
                        }
                    }
                    typeList = CardLists.getValidCards(hand, type, source.getController(), source, sa);
                    if (typeList.size() > ai.getMaxHandSize()) {
                        continue;
                    }
                    num = AbilityUtils.calculateAmount(source, disc.getAmount(), sa);
                }
                for (int i = 0; i < num; i++) {
                    Card pref = ComputerUtil.getCardPreference(ai, source, "DiscardCost", typeList);
                    if (pref == null) {
                        return false;
                    }
                    typeList.remove(pref);
                    hand.remove(pref);
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
    public static boolean checkDamageCost(final Player ai, final Cost cost, final Card source, final int remainingLife, final SpellAbility sa) {
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostDamage pay) {
                int realDamage = ComputerUtilCombat.predictDamageTo(ai, pay.getAbilityAmount(sa), source, false);
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
    public static boolean checkLifeCost(final Player ai, final Cost cost, final Card source, int remainingLife, SpellAbility sourceAbility) {
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostPayLife payLife) {
                int amount = payLife.getAbilityAmount(sourceAbility);

                // check if there's override for the remainingLife threshold
                if (sourceAbility != null && sourceAbility.hasParam("AILifeThreshold")) {
                    remainingLife = Integer.parseInt(sourceAbility.getParam("AILifeThreshold"));
                }

                if (ai.getLife() - amount < remainingLife && !ai.cantLoseForZeroOrLessLife()) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean checkForManaSacrificeCost(final Player ai, final Cost cost, final SpellAbility sourceAbility, final boolean effect) {
        // TODO cheating via autopay can still happen, need to get the real ai player from controlledBy
        if (cost == null || !ai.isAI()) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice) {
                CardCollection list = new CardCollection();
                final CardCollection exclude = new CardCollection();
                if (AiCardMemory.getMemorySet(ai, MemorySet.PAYS_SAC_COST) != null) {
                    exclude.addAll(AiCardMemory.getMemorySet(ai, MemorySet.PAYS_SAC_COST));
                }
                if (part.payCostFromSource()) {
                    list.add(sourceAbility.getHostCard());
                } else if (part.getType().equals("OriginalHost")) {
                    list.add(sourceAbility.getOriginalHost());
                } else if (part.getAmount().equals("All")) {
                    // Does the AI want to use Sacrifice All?
                    return false;
                } else {
                    Integer c = part.convertAmount();

                    if (c == null) {
                        c = part.getAbilityAmount(sourceAbility);
                    }
                    final AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
                    CardCollectionView choices = aic.chooseSacrificeType(part.getType(), sourceAbility, effect, c, exclude);
                    if (choices != null) {
                        list.addAll(choices);
                    }
                }
                list.removeAll(exclude);
                if (list.isEmpty()) {
                    return false;
                }
                for (Card choice : list) {
                    AiCardMemory.rememberCard(ai, choice, MemorySet.PAYS_SAC_COST);
                }
                return true;
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
    public static boolean checkCreatureSacrificeCost(final Player ai, final Cost cost, final Card source, final SpellAbility sourceAbility) {
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice sac) {
                final int amount = AbilityUtils.calculateAmount(source, sac.getAmount(), sourceAbility);

                if (sac.payCostFromSource() && source.isCreature()) {
                    return false;
                }
                final String type = sac.getType();

                if (type.equals("CARDNAME")) {
                    continue;
                }

                final CardCollection sacList = new CardCollection();
                CardCollection typeList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(";"), source.getController(), source, sourceAbility);

                // don't sacrifice the card we're pumping
                typeList = paymentChoicesWithoutTargets(typeList, sourceAbility, ai);

                int count = 0;
                while (count < amount) {
                    Card prefCard = ComputerUtil.getCardPreference(ai, source, "SacCost", typeList);
                    if (prefCard == null) {
                        return false;
                    }
                    sacList.add(prefCard);
                    typeList.remove(prefCard);
                    count++;
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
    public static boolean checkSacrificeCost(final Player ai, final Cost cost, final Card source, final SpellAbility sourceAbility, final boolean important) {
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice sac) {
                if (suppressRecursiveSacCostCheck) {
                    return false;
                }

                final int amount = AbilityUtils.calculateAmount(source, sac.getAmount(), sourceAbility);

                String type = sac.getType();

                if (type.equals("CARDNAME")) {
                    if (!important) {
                        return false;
                    }
                    if (!CardLists.filterControlledBy(source.getEnchantedBy(), source.getController()).isEmpty()) {
                        return false;
                    }
                    if (source.isCreature()) {
                        // e.g. Sakura-Tribe Elder
                        final Combat combat = ai.getGame().getCombat();
                        final boolean beforeNextTurn = ai.getGame().getPhaseHandler().is(PhaseType.END_OF_TURN) && ai.getGame().getPhaseHandler().getNextTurn().equals(ai) && ComputerUtilCard.evaluateCreature(source) <= 150;
                        final boolean creatureInDanger = ComputerUtil.predictCreatureWillDieThisTurn(ai, source, sourceAbility, false)
                                && !ComputerUtilCombat.willOpposingCreatureDieInCombat(ai, source, combat);
                        final int lifeThreshold = ai.getController().isAI() ? (((PlayerControllerAi) ai.getController()).getAi().getIntProperty(AiProps.AI_IN_DANGER_THRESHOLD)) : 4;
                        final boolean aiInDanger = ai.getLife() <= lifeThreshold && ai.canLoseLife() && !ai.cantLoseForZeroOrLessLife();
                        if (creatureInDanger && !ComputerUtilCombat.isDangerousToSacInCombat(ai, source, combat)) {
                            return true;
                        } else if (aiInDanger || !beforeNextTurn) {
                            return false;
                        }
                    }
                    continue;
                }

                boolean differentNames = false;
                if (type.contains("+WithDifferentNames")) {
                    type = type.replace("+WithDifferentNames", "");
                    differentNames = true;
                }

                CardCollection typeList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(";"), source.getController(), source, sourceAbility);
                if (differentNames) {
                    final Set<Card> uniqueNameCards = Sets.newHashSet();
                    for (final Card card : typeList) {
                        // CR 201.2b Those objects have different names only if each of them has at least one name and no two objects in that group have a name in common
                        if (!card.hasNoName()) {
                            uniqueNameCards.add(card);
                        }
                    }
                    typeList.clear();
                    typeList.addAll(uniqueNameCards);
                }

                // don't sacrifice the card we're pumping
                typeList = paymentChoicesWithoutTargets(typeList, sourceAbility, ai);

                int count = 0;
                while (count < amount) {
                    Card prefCard = ComputerUtil.getCardPreference(ai, source, "SacCost", typeList, sourceAbility);
                    if (prefCard == null) {
                        return false;
                    }
                    typeList.remove(prefCard);
                    count++;
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
    public static boolean checkSacrificeCost(final Player ai, final Cost cost, final Card source, final SpellAbility sourceAbility) {
        return checkSacrificeCost(ai, cost, source, sourceAbility, true);
    }

    public static boolean isSacrificeSelfCost(final Cost cost) {
        if (cost == null) {
            return false;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice && part.payCostFromSource()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check TapType cost.
     *
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean checkTapTypeCost(final Player ai, final Cost cost, final Card source, final SpellAbility sa, final Collection<Card> alreadyTapped) {
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostTapType) {
                String type = part.getType();

                /*
                 * Only crew with creatures weaker than vehicle
                 *
                 * Possible improvements:
                 * - block against evasive (flyers, intimidate, etc.)
                 * - break board stall by racing with evasive vehicle
                 */
                if (sa.isCrew()) {
                    Card vehicle = AnimateAi.becomeAnimated(source, sa);
                    final int vehicleValue = ComputerUtilCard.evaluateCreature(vehicle);
                    String totalP = type.split("withTotalPowerGE")[1];
                    type = TextUtil.fastReplace(type, TextUtil.concatNoSpace("+withTotalPowerGE", totalP), "");
                    CardCollection exclude = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(";"), source.getController(), source, sa);
                    exclude = CardLists.filter(exclude, c -> ComputerUtilCard.evaluateCreature(c) >= vehicleValue); // exclude creatures >= vehicle
                    exclude.addAll(alreadyTapped);
                    CardCollection tappedCrew = ComputerUtil.chooseTapTypeAccumulatePower(ai, type, sa, true, Integer.parseInt(totalP), exclude);
                    if (tappedCrew != null) {
                        alreadyTapped.addAll(tappedCrew);
                        return true;
                    }
                    return false;
                }

                // check if we have a valid card to tap (e.g. Jaspera Sentinel)
                Integer c = part.convertAmount();
                if (c == null) {
                    c = AbilityUtils.calculateAmount(source, part.getAmount(), sa);
                }
                CardCollection exclude = new CardCollection();
                if (alreadyTapped != null) {
                    exclude.addAll(alreadyTapped);
                }
                // trying to produce mana that includes tapping source that will already be tapped
                if (exclude.contains(source) && cost.hasTapCost()) {
                    return false;
                }
                // if we want to pay for an ability with tapping the source can't be chosen
                if (sa.getPayCosts().hasTapCost()) {
                    exclude.add(sa.getHostCard());
                }
                CardCollection tapChoices = ComputerUtil.chooseTapType(ai, type, source, cost.hasTapCost(), c, exclude, sa);
                if (tapChoices != null) {
                    if (alreadyTapped != null) {
                        alreadyTapped.addAll(tapChoices);
                        // if manasource gets tapped to produce it also can't help paying another
                        if (cost.hasTapCost()) {
                            alreadyTapped.add(source);
                        }
                    }
                    return true;
                }
                return false;
            }
        }
        return true;
    }

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
    public static boolean canPayCost(final SpellAbility sa, final Player player, final boolean effect) {
        return canPayCost(sa.getPayCosts(), sa, player, effect);
    }
    public static boolean canPayCost(final Cost cost, final SpellAbility sa, final Player player, final boolean effect) {
        if (sa.getActivatingPlayer() == null) {
            sa.setActivatingPlayer(player); // complaints on NPE had came before this line was added.
        }

        boolean cannotBeCountered = false;

        // Check for stuff like Nether Void
        int extraManaNeeded = 0;
        if (!effect) {
            if (sa instanceof Spell) {
                cannotBeCountered = !sa.isCounterableBy(null);
                for (Card c : player.getGame().getCardsIn(ZoneType.Battlefield)) {
                    final String snem = c.getSVar("AI_SpellsNeedExtraMana");
                    if (!StringUtils.isBlank(snem)) {
                        if (cannotBeCountered && c.getName().equals("Nether Void")) {
                            continue;
                        }
                        String[] parts = TextUtil.split(snem, ' ');
                        boolean meetsRestriction = parts.length == 1 || player.isValid(parts[1], c.getController(), c, sa);
                        if(!meetsRestriction)
                            continue;

                        if (StringUtils.isNumeric(parts[0])) {
                            extraManaNeeded += Integer.parseInt(parts[0]);
                        } else {
                            System.out.println("wrong SpellsNeedExtraMana SVar format on " + c);
                        }
                    }
                }
                for (Card c : player.getCardsIn(ZoneType.Command)) {
                    if (cannotBeCountered) {
                        continue;
                    }
                    final String snem = c.getSVar("SpellsNeedExtraManaEffect");
                    if (!StringUtils.isBlank(snem)) {
                        if (StringUtils.isNumeric(snem)) {
                            extraManaNeeded += Integer.parseInt(snem);
                        } else {
                            System.out.println("wrong SpellsNeedExtraManaEffect SVar format on " + c);
                        }
                    }
                }
            }

            // Try not to lose Planeswalker if not threatened
            if (sa.isPwAbility()) {
                for (final CostPart part : cost.getCostParts()) {
                    if (part instanceof CostRemoveCounter) {
                        if (part.convertAmount() != null && part.convertAmount() == sa.getHostCard().getCurrentLoyalty()) {
                            // refuse to pay if opponent has no creature threats or
                            // 50% chance otherwise
                            if (player.getOpponents().getCreaturesInPlay().isEmpty()
                                    || MyRandom.getRandom().nextFloat() < .5f) {
                                return false;
                            }
                        }
                    }
                }
            }

            // Ward - will be accounted for when rechecking a targeted ability
            if (!sa.isTrigger() && (!sa.isSpell() || !cannotBeCountered)) {
                for (TargetChoices tc : sa.getAllTargetChoices()) {
                    for (Card tgt : tc.getTargetCards()) {
                        if (tgt.hasKeyword(Keyword.WARD) && tgt.isInPlay() && tgt.getController().isOpponentOf(sa.getHostCard().getController())) {
                            Cost wardCost = ComputerUtilCard.getTotalWardCost(tgt);
                            if (wardCost.hasManaCost()) {
                                extraManaNeeded += wardCost.getTotalMana().getCMC();
                            }
                        }
                    }
                }
            }

            // Bail early on Casualty in case there are no cards that would make sense to pay with
            if (sa.getHostCard().hasKeyword(Keyword.CASUALTY)) {
                for (final CostPart part : cost.getCostParts()) {
                    if (part instanceof CostSacrifice) {
                        CardCollection valid = CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), part.getType().split(";"),
                                sa.getActivatingPlayer(), sa.getHostCard(), sa);
                        valid = CardLists.filter(valid, CardPredicates.hasSVar("AIDontSacToCasualty").negate());
                        if (valid.isEmpty()) {
                            return false;
                        }
                    }
                }
            }
        }

        return ComputerUtilMana.canPayManaCost(cost, sa, player, extraManaNeeded, effect)
                && CostPayment.canPayAdditionalCosts(cost, sa, effect, player);
    }

    public static Set<String> getAvailableManaColors(Player ai, Card additionalLand) {
        return getAvailableManaColors(ai, Lists.newArrayList(additionalLand));
    }
    public static Set<String> getAvailableManaColors(Player ai, List<Card> additionalLands) {
        CardCollection cardsToConsider = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.UNTAPPED);
        Set<String> colorsAvailable = Sets.newHashSet();

        if (additionalLands != null) {
            cardsToConsider.addAll(additionalLands);
        }

        for (Card c : cardsToConsider) {
            for (SpellAbility sa : c.getManaAbilities()) {
                if (sa.getManaPart() != null) {
                    colorsAvailable.add(sa.getManaPart().getOrigProduced());
                }
            }
        }

        return colorsAvailable;
    }

    public static boolean isFreeCastAllowedByPermanent(Player player, String altCost) {
        Game game = player.getGame();
        for (Card cardInPlay : game.getCardsIn(ZoneType.Battlefield)) {
            if (cardInPlay.hasSVar("AllowFreeCast")) {
                return altCost == null ? "Always".equals(cardInPlay.getSVar("AllowFreeCast"))
                        : altCost.equals(cardInPlay.getSVar("AllowFreeCast"));
            }
        }
        return false;
    }

    public static int getMaxXValue(SpellAbility sa, Player ai, final boolean effect) {
        final Card source = sa.getHostCard();
        SpellAbility root = sa.getRootAbility();
        final Cost abCost = root.getPayCosts();

        if (abCost == null || !abCost.hasXInAnyCostPart()) {
            return 0;
        }

        Integer val = null;

        if (root.costHasManaX()) {
            val = ComputerUtilMana.determineLeftoverMana(root, ai, effect);
        }

        if (sa.usesTargeting()) {
            // if announce is used as min targets, check what the max possible number would be
            if ("X".equals(sa.getTargetRestrictions().getMinTargets())) {
                val = ObjectUtils.min(val, CardUtil.getValidCardsToTarget(sa).size());
            }

            if (sa.hasParam("AIMaxTgtsCount")) {
                // Cards that have confusing costs for the AI (e.g. Eliminate the Competition) can have forced max target constraints specified
                // TODO: is there a better way to predict things like "sac X" costs without needing a special AI variable?
                val = ObjectUtils.min(val, AbilityUtils.calculateAmount(source, "Count$" + sa.getParam("AIMaxTgtsCount"), sa));
            }
        }

        val = ObjectUtils.min(val, abCost.getMaxForNonManaX(root, ai, effect));

        if (val != null && val > 0) {
            // filter cost parts for preferences, don't choose X > than possible preferences
            for (final CostPart part : abCost.getCostParts()) {
                if (part instanceof CostSacrifice) {
                    if (part.payCostFromSource()) {
                        continue;
                    }
                    if (!part.getAmount().equals("X")) {
                        continue;
                    }

                    final CardCollection typeList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), part.getType().split(";"), source.getController(), source, sa);

                    int count = 0;
                    while (count < val) {
                        Card prefCard = ComputerUtil.getCardPreference(ai, source, "SacCost", typeList);
                        if (prefCard == null) {
                            break;
                        }
                        typeList.remove(prefCard);
                        count++;
                    }
                    val = ObjectUtils.min(val, count);
                }
            }
        }
        return ObjectUtils.defaultIfNull(val, 0);
    }

    public static CardCollection paymentChoicesWithoutTargets(Iterable<Card> choices, SpellAbility source, Player ai) {
        if (source.usesTargeting()) {
            final CardCollectionView targets = source.getTargets().getTargetCards();
            choices = IterableUtil.filter(choices, Predicate.not(CardPredicates.isController(ai).and(targets::contains)));
        }
        return new CardCollection(choices);
    }
}
