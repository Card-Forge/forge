package forge.ai;

import java.util.List;
import java.util.Set;

import forge.game.ability.ApiType;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.ai.ability.AnimateAi;
import forge.card.ColorSet;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CardUtil;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostDamage;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostPart;
import forge.game.cost.CostPayLife;
import forge.game.cost.CostPayment;
import forge.game.cost.CostPutCounter;
import forge.game.cost.CostRemoveAnyCounter;
import forge.game.cost.CostRemoveCounter;
import forge.game.cost.CostSacrifice;
import forge.game.cost.CostTapType;
import forge.game.cost.PaymentDecision;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.TextUtil;
import forge.util.collect.FCollectionView;


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
        final AiCostDecision decision = new AiCostDecision(sa.getActivatingPlayer(), sa);
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostRemoveCounter) {
                final CostRemoveCounter remCounter = (CostRemoveCounter) part;

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
            } else if (part instanceof CostRemoveAnyCounter) {
                final CostRemoveAnyCounter remCounter = (CostRemoveAnyCounter) part;

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
            if (part instanceof CostDiscard) {
                final CostDiscard disc = (CostDiscard) part;

                final String type = disc.getType();
                if (type.equals("CARDNAME") && source.getAbilityText().contains("Bloodrush")) {
                    continue;
                }
                final CardCollection typeList = CardLists.getValidCards(hand, type.split(","), source.getController(), source, sa);
                if (typeList.size() > ai.getMaxHandSize()) {
                    continue;
                }
                int num = AbilityUtils.calculateAmount(source, disc.getAmount(), null);

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
    public static boolean checkLifeCost(final Player ai, final Cost cost, final Card source, int remainingLife, SpellAbility sourceAbility) {
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
            if (part instanceof CostSacrifice) {
                final CostSacrifice sac = (CostSacrifice) part;
                final int amount = AbilityUtils.calculateAmount(source, sac.getAmount(), sourceAbility);

                if (sac.payCostFromSource() && source.isCreature()) {
                    return false;
                }
                final String type = sac.getType();

                if (type.equals("CARDNAME")) {
                    continue;
                }

                final CardCollection sacList = new CardCollection();
                final CardCollection typeList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(";"), source.getController(), source, sourceAbility);

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
            if (part instanceof CostSacrifice) {
                final CostSacrifice sac = (CostSacrifice) part;
                final int amount = AbilityUtils.calculateAmount(source, sac.getAmount(), sourceAbility);

                final String type = sac.getType();

                if (type.equals("CARDNAME")) {
                    if (!important) {
                        return false;
                    }
                    if (!CardLists.filterControlledBy(source.getEnchantedBy(), source.getController()).isEmpty()) {
                        return false;
                    }
                    continue;
                }

                final CardCollection sacList = new CardCollection();
                final CardCollection typeList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split(";"), source.getController(), source, sourceAbility);

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
    public static boolean checkTapTypeCost(final Player ai, final Cost cost, final Card source, final SpellAbility sa) {
        if (cost == null) {
            return true;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostTapType) {
                /*
                 * Only crew with creatures weaker than vehicle
                 *
                 * Possible improvements:
                 * - block against evasive (flyers, intimidate, etc.)
                 * - break board stall by racing with evasive vehicle
                 */
                if (sa.hasParam("Crew")) {
                    Card vehicle = AnimateAi.becomeAnimated(source, sa);
                    final int vehicleValue = ComputerUtilCard.evaluateCreature(vehicle);
                    String type = part.getType();
                    String totalP = type.split("withTotalPowerGE")[1];
                    type = TextUtil.fastReplace(type, TextUtil.concatNoSpace("+withTotalPowerGE", totalP), "");
                    CardCollection exclude = CardLists.getValidCards(
                            new CardCollection(ai.getCardsIn(ZoneType.Battlefield)), type.split(";"),
                            source.getController(), source, sa);
                    exclude = CardLists.filter(exclude, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            return ComputerUtilCard.evaluateCreature(c) >= vehicleValue;
                        }
                    }); // exclude creatures >= vehicle
                    return ComputerUtil.chooseTapTypeAccumulatePower(ai, type, sa, true,
                            Integer.parseInt(totalP), exclude) != null;
                }
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
    public static boolean checkSacrificeCost(final Player ai, final Cost cost, final Card source, final SpellAbility sourceAbility) {
        return checkSacrificeCost(ai, cost, source, sourceAbility,true);
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
                final int lifeCost = part.convertAmount();
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
            final boolean cannotBeCountered = !CardFactoryUtil.isCounterable(sa.getHostCard());
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
            for (final CostPart part : sa.getPayCosts().getCostParts()) {
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

        // KLD vehicle
        if (sa.hasParam("Crew")) {  // put under checkTapTypeCost?
            for (final CostPart part : sa.getPayCosts().getCostParts()) {
                if (part instanceof CostTapType && part.getType().contains("+withTotalPowerGE")) {
                    return new AiCostDecision(player, sa).visit((CostTapType)part) != null;
                }
            }
        }

        // TODO: Alternate costs which involve both paying mana and tapping a card, e.g. Zahid, Djinn of the Lamp
        // Current AI decides on each part separately, thus making it possible for the AI to cheat by
        // tapping a mana source for mana and for the tap cost at the same time. Until this is improved, AI
        // will not consider mana sources valid for paying the tap cost to avoid this exact situation.
        if ("DontPayTapCostWithManaSources".equals(sa.getHostCard().getSVar("AIPaymentPreference"))) {
            for (final CostPart part : sa.getPayCosts().getCostParts()) {
                if (part instanceof CostTapType) {
                    CardCollectionView nonManaSources =
                            CardLists.getValidCards(player.getCardsIn(ZoneType.Battlefield), part.getType().split(";"),
                                    sa.getActivatingPlayer(), sa.getHostCard(), sa);
                    nonManaSources = CardLists.filter(nonManaSources, new Predicate<Card>() {
                        @Override
                        public boolean apply(Card card) {
                            boolean hasManaSa = false;
                            for (final SpellAbility sa : card.getSpellAbilities()) {
                                if (sa.isManaAbility() && sa.getPayCosts().hasTapCost()) {
                                    hasManaSa = true;
                                    break;
                                }
                            }
                            return !hasManaSa;
                        }
                    });
                    if (nonManaSources.size() < part.convertAmount()) {
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
        boolean payOwner = sa.hasParam("UnlessAI") && aiLogic.startsWith("Defined");
        boolean payNever = "Never".equals(aiLogic);
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
        } else if ("Paralyze".equals(aiLogic)) {
            final Card c = source.getEnchantingCard();
            if (c == null || c.isUntapped()) {
                return false;
            }
        } else if ("RiskFactor".equals(aiLogic)) {
            final Player activator = sa.getActivatingPlayer();
            if (!activator.canDraw() || activator.hasKeyword("You can't draw more than one card each turn.")) {
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
            else if (payer.getLife() <= AbilityUtils.calculateAmount(source, aiLogic.substring(6), sa)) {
                return true;
            }
        } else if ("WillAttack".equals(aiLogic)) {
            AiAttackController aiAtk = new AiAttackController(payer);
            Combat combat = new Combat(payer);
            aiAtk.declareAttackers(combat);
            if (combat.getAttackers().isEmpty()) {
                return false;
            }
        } else if ("nonToken".equals(aiLogic) && !AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa).isEmpty()
                && AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa).get(0).isToken()) {
            return false;
        } else if ("LowPriority".equals(aiLogic) && MyRandom.getRandom().nextInt(100) < 67) {
            return false;
        }

        // Check for shocklands and similar ETB replacement effects
        if (sa.hasParam("ETB") && sa.getApi().equals(ApiType.Tap)) {
            for (final CostPart part : cost.getCostParts()) {
                if (part instanceof CostPayLife) {
                    final CostPayLife lifeCost = (CostPayLife) part;
                    Integer amount = lifeCost.convertAmount();
                    if (payer.getLife() > (amount + 1) && payer.canPayLife(amount)) {
                        final int landsize = payer.getLandsInPlay().size() + 1;
                        for (Card c : payer.getCardsIn(ZoneType.Hand)) {
                            // Check if the AI has enough lands to play the card
                            if (landsize != c.getCMC()) {
                                continue;
                            }
                            // Check if the AI intends to play the card and if it can pay for it with the mana it has
                            boolean willPlay = ComputerUtil.hasReasonToPlayCardThisTurn(payer, c);
                            boolean canPay = c.getManaCost().canBePaidWithAvaliable(ColorSet.fromNames(getAvailableManaColors(payer, source)).getColor());
                            return canPay && willPlay;
                        }
                    }
                }
            }
        }

        // AI will only pay when it's not already payed and only opponents abilities
        if (alreadyPaid || (payers.size() > 1 && (isMine && !payForOwnOnly))) {
            return false;
        }

        // AI was crashing because the blank ability used to pay costs
        // Didn't have any of the data on the original SA to pay dependant costs

        return checkLifeCost(payer, cost, source, 4, sa)
                && checkDamageCost(payer, cost, source, 4)
                && (isMine || checkSacrificeCost(payer, cost, source, sa))
                && (isMine || checkDiscardCost(payer, cost, source, sa))
                && (!source.getName().equals("Tyrannize") || payer.getCardsIn(ZoneType.Hand).size() > 2)
                && (!source.getName().equals("Perplex") || payer.getCardsIn(ZoneType.Hand).size() < 2)
                && (!source.getName().equals("Breaking Point") || payer.getCreaturesInPlay().size() > 1)
                && (!source.getName().equals("Chain of Vapor") || (payer.getWeakestOpponent().getCreaturesInPlay().size() > 0 && payer.getLandsInPlay().size() > 3));
    }

    public static Set<String> getAvailableManaColors(Player ai, Card additionalLand) {
        return getAvailableManaColors(ai, Lists.newArrayList(additionalLand));
    }

    public static Set<String> getAvailableManaColors(Player ai, List<Card> additionalLands) {
        CardCollection cardsToConsider = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Presets.UNTAPPED);
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

    public static int getMaxXValue(SpellAbility sa, Player ai) {
        final Card source = sa.getHostCard();
        final SpellAbility root = sa.getRootAbility();
        final Cost abCost = root.getPayCosts();
        if (abCost == null || !abCost.hasXInAnyCostPart()) {
            return 0;
        }

        Integer val = null;

        if (root.costHasManaX()) {
            val = ComputerUtilMana.determineLeftoverMana(root, ai);
        }

        if (sa.usesTargeting()) {
            // if announce is used as min targets, check what the max possible number would be
            if ("X".equals(sa.getTargetRestrictions().getMinTargets())) {
                val = ObjectUtils.min(val, CardUtil.getValidCardsToTarget(sa.getTargetRestrictions(), sa).size());
            }

            if (sa.hasParam("AIMaxTgtsCount")) {
                // Cards that have confusing costs for the AI (e.g. Eliminate the Competition) can have forced max target constraints specified
                // TODO: is there a better way to predict things like "sac X" costs without needing a special AI variable?
                val = ObjectUtils.min(val, AbilityUtils.calculateAmount(sa.getHostCard(), "Count$" + sa.getParam("AIMaxTgtsCount"), sa));
            }
        }

        val = ObjectUtils.min(val, abCost.getMaxForNonManaX(root, ai));

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
}
