package forge.ai;

import com.google.common.collect.*;
import forge.ai.ability.AnimateAi;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCostShard;
import forge.game.CardTraitPredicates;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.*;
import forge.game.keyword.Keyword;
import forge.game.mana.Mana;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.mana.ManaPool;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerPredicates;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementLayer;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityManaConvert;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static forge.ai.ComputerUtilMana.*;

public class ManaPaymentService {
    private final static boolean DEBUG_MANA_PAYMENT = false;

    ManaCostBeingPaid cost;
    SpellAbility sa;
    Player ai;
    boolean test;
    boolean checkPlayable;
    boolean effect;
    CardCollection sortedManaSources = null;
    ListMultimap<ManaCostShard, SpellAbility> sourcesForShards = null;
    ListMultimap<Integer, SpellAbility> sourcesByColor = null;
    Map<Card, Integer> sourceByFlexibility = null;

    public ManaPaymentService(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai, final boolean test, final boolean checkPlayable, final boolean effect) {
        this.cost = cost;
        this.sa = sa;
        this.ai = ai;
        this.test = test;
        this.checkPlayable = checkPlayable;
        this.effect = effect;
        initialize();
    }

    public ManaPaymentService(final SpellAbility sa, final Player ai, final int extraMana, final boolean effect) {
        this(sa.getPayCosts(), sa, ai, extraMana, effect);
    }
    public ManaPaymentService(final Cost cost, final SpellAbility sa, final Player ai, final int extraMana, final boolean effect) {
        this(cost, sa, ai, true, extraMana, true, effect);
    }

    public ManaPaymentService(ManaCostBeingPaid cost, final SpellAbility sa, final Player ai, final boolean effect) {
        this(cost, sa, ai, false, true, effect);
    }
    public ManaPaymentService(final Cost cost, final Player ai, final SpellAbility sa, final boolean effect) {
        this(cost, sa, ai, false, 0, true, effect);
    }
    public ManaPaymentService(final Cost cost, final SpellAbility sa, final Player ai, final boolean test, final int extraMana, boolean checkPlayable, final boolean effect) {
        this(calculateManaCost(cost, sa, ai, test, extraMana, effect), sa, ai, test, checkPlayable, effect);
    }

    public static boolean canPayMana(final Cost cost, final SpellAbility sa, final Player ai, final int extraMana, final boolean effect) {
        ManaCostBeingPaid copiedCost = new ManaCostBeingPaid(cost.getTotalMana());
        ManaPaymentService service = new ManaPaymentService(copiedCost, sa, ai, true, true, effect);

        return service.payManaCost();
    }

    public static boolean canPayMana(ManaCostBeingPaid cost, final SpellAbility sa, final Player ai, final boolean effect) {
        // Logic to check if mana can be paid
        ManaCostBeingPaid copiedCost = new ManaCostBeingPaid(cost);
        ManaPaymentService service = new ManaPaymentService(copiedCost, sa, ai, true, true, effect);

        return service.payManaCost();
    }

    private void initialize() {
        sortedManaSources = getAvailableManaSources();
        sourcesByColor = groupSourcesByManaColor();
        sourcesForShards = getSourcesForShards();
        sourceByFlexibility = null;
        if (true) {
            System.out.println("DEBUG_MANA_PAYMENT: sortedManaSources = " + sortedManaSources);
            System.out.println("DEBUG_MANA_PAYMENT: sourcesForShards = " + sourcesForShards);
            System.out.println("DEBUG_MANA_PAYMENT: sourcesByColor = " + sourcesByColor);
        }
    }

    private Integer scoreManaProducingCard(final Card card) {
        int score = 0;

        for (SpellAbility ability : card.getSpellAbilities()) {
            ability.setActivatingPlayer(card.getController());
            if (ability.isManaAbility()) {
                score += ability.calculateScoreForManaAbility();
                // TODO check TriggersWhenSpent
            }
            else if (!ability.isTrigger() && ability.isPossible()) {
                score += 13; //add 13 for any non-mana activated abilities
            }
        }

        if (card.isCreature()) {
            //treat attacking and blocking as though they're non-mana abilities
            if (CombatUtil.canAttack(card)) {
                score += 13;
            }
            if (CombatUtil.canBlock(card)) {
                score += 13;
            }
        }

        return score;
    }

    private boolean isMultiManaAbility(SpellAbility ability) {
        AbilityManaPart manaPart = ability.getManaPart();
        if (manaPart == null) return false;
        String manaProduced = manaPart.mana(ability);
        if (manaProduced == null) return false;
        // Count number of mana symbols produced
        int count = 0;
        for (char c : manaProduced.toCharArray()) {
            if (c == 'W' || c == 'U' || c == 'B' || c == 'R' || c == 'G' || c == 'C' || c == 'X' || (c >= '1' && c <= '9')) count++;
        }
        return count > 1;
    }

    private boolean wouldOverpayWithAbility(SpellAbility ability, ManaCostBeingPaid cost) {
        AbilityManaPart manaPart = ability.getManaPart();
        if (manaPart == null) return false;
        String manaProduced = manaPart.mana(ability);
        if (manaProduced == null) return false;
        // Estimate if using this ability would produce more mana than needed
        int manaNeeded = cost.getUnpaidShards().size() + cost.getGenericManaAmount();
        int manaProducedCount = 0;
        for (char c : manaProduced.toCharArray()) {
            if (c == 'W' || c == 'U' || c == 'B' || c == 'R' || c == 'G' || c == 'C' || c == 'X' || (c >= '1' && c <= '9')) manaProducedCount++;
        }
        return manaProducedCount > manaNeeded;
    }

    private void sortManaAbilities(final Multimap<ManaCostShard, SpellAbility> manaAbilityMap, final SpellAbility sa, final CardCollection sortedManaSources) {
        final Map<Card, Integer> manaCardMap = Maps.newHashMap();
        final List<Card> orderedCards = Lists.newArrayList();

        for (final ManaCostShard shard : manaAbilityMap.keySet()) {
            for (SpellAbility ability : manaAbilityMap.get(shard)) {
                final Card hostCard = ability.getHostCard();
                if (!manaCardMap.containsKey(hostCard)) {
                    manaCardMap.put(hostCard, scoreManaProducingCard(hostCard));
                    orderedCards.add(hostCard);
                }
            }
        }
        orderedCards.sort(Comparator.comparingInt(manaCardMap::get));

        for (final ManaCostShard shard : manaAbilityMap.keySet()) {
            final Collection<SpellAbility> abilities = manaAbilityMap.get(shard);
            final List<SpellAbility> newAbilities = new ArrayList<>(abilities);

            // Strictly sort: multi-mana sources that can pay for multiple shards always first
            newAbilities.sort((ability1, ability2) -> {
                boolean multi1 = isMultiManaAbility(ability1);
                boolean multi2 = isMultiManaAbility(ability2);

                if (isReusableCost(ability1) && multi1 && !multi2) return -1;
                if (isReusableCost(ability2) && multi2 && !multi1) return 1;

                // If both are multi or both are single, preserve sortedManaSources order
                int idx1 = sortedManaSources.indexOf(ability1.getHostCard());
                int idx2 = sortedManaSources.indexOf(ability2.getHostCard());
                return Integer.compare(idx1, idx2);
            });
            manaAbilityMap.replaceValues(shard, newAbilities);
            // Debug output for order verification
            if (DEBUG_MANA_PAYMENT) {
                System.out.println("DEBUG_MANA_PAYMENT: sourcesForShards[" + shard + "] sorted = " + newAbilities);
            }
            // Sort the first N abilities so that the preferred shard is selected, e.g. Adamant
            String manaPref = sa.getParamOrDefault("AIManaPref", "");
            if (manaPref.isEmpty() && sa.getHostCard() != null && sa.getHostCard().hasSVar("AIManaPref")) {
                manaPref = sa.getHostCard().getSVar("AIManaPref");
            }
            if (!manaPref.isEmpty()) {
                final String[] prefShardInfo = manaPref.split(":");
                final String preferredShard = prefShardInfo[0];
                final int preferredShardAmount = prefShardInfo.length > 1 ? Integer.parseInt(prefShardInfo[1]) : 3;
                if (!preferredShard.isEmpty()) {
                    final List<SpellAbility> prefSortedAbilities = new ArrayList<>(newAbilities);
                    final List<SpellAbility> otherSortedAbilities = new ArrayList<>(newAbilities);
                    prefSortedAbilities.sort((ability1, ability2) -> {
                        if (ability1.getManaPart().mana(ability1).contains(preferredShard))
                            return -1;
                        else if (ability2.getManaPart().mana(ability2).contains(preferredShard))
                            return 1;
                        return 0;
                    });
                    otherSortedAbilities.sort((ability1, ability2) -> {
                        if (ability1.getManaPart().mana(ability1).contains(preferredShard))
                            return 1;
                        else if (ability2.getManaPart().mana(ability2).contains(preferredShard))
                            return -1;
                        return 0;
                    });
                    final List<SpellAbility> finalAbilities = new ArrayList<>();
                    for (int i = 0; i < preferredShardAmount && i < prefSortedAbilities.size(); i++) {
                        finalAbilities.add(prefSortedAbilities.get(i));
                    }
                    for (SpellAbility ab : otherSortedAbilities) {
                        if (!finalAbilities.contains(ab))
                            finalAbilities.add(ab);
                    }

                    manaAbilityMap.replaceValues(shard, finalAbilities);
                }
            }
        }
    }

    public boolean isReusableCost(SpellAbility sa) {
        for(CostPart cost : sa.getPayCosts().getCostParts()) {
            if (!cost.isReusable()) {
                return false;
            }
        }

        return true;
    }

    public SpellAbility chooseManaAbility(ManaCostShard toPay, Collection<SpellAbility> saList, boolean checkCosts) {
        Card saHost = sa.getHostCard();

        // CastTotalManaSpent (AIPreference:ManaFrom$Type or AIManaPref$ Type)
        String manaSourceType = "";
        if (saHost.hasSVar("AIPreference")) {
            String condition = saHost.getSVar("AIPreference");
            if (condition.startsWith("ManaFrom")) {
                manaSourceType = TextUtil.split(condition, '$')[1];
            }
        } else if (sa.hasParam("AIManaPref")) {
            manaSourceType = sa.getParam("AIManaPref");
        }
        if (manaSourceType != "") {
            List<SpellAbility> filteredList = Lists.newArrayList(saList);
            switch (manaSourceType) {
                case "Snow":
                    filteredList.sort((ab1, ab2) -> ab1.getHostCard() != null && ab1.getHostCard().isSnow()
                            && ab2.getHostCard() != null && !ab2.getHostCard().isSnow() ? -1 : 1);
                    saList = filteredList;
                    break;
                case "Treasure":
                    // Try to spend only one Treasure if possible
                    filteredList.sort((ab1, ab2) -> ab1.getHostCard() != null && ab1.getHostCard().getType().hasSubtype("Treasure")
                            && ab2.getHostCard() != null && !ab2.getHostCard().getType().hasSubtype("Treasure") ? -1 : 1);
                    SpellAbility first = filteredList.get(0);
                    if (first.getHostCard() != null && first.getHostCard().getType().hasSubtype("Treasure")) {
                        saList.remove(first);
                        List<SpellAbility> updatedList = Lists.newArrayList();
                        updatedList.add(first);
                        updatedList.addAll(saList);
                        saList = updatedList;
                    }
                    break;
                case "TreasureMax":
                    // Ok to spend as many Treasures as possible
                    filteredList.sort((ab1, ab2) -> ab1.getHostCard() != null && ab1.getHostCard().getType().hasSubtype("Treasure")
                            && ab2.getHostCard() != null && !ab2.getHostCard().getType().hasSubtype("Treasure") ? -1 : 1);
                    saList = filteredList;
                    break;
                case "NotSameCard":
                    String hostName = sa.getHostCard().getName();
                    saList = filteredList.stream()
                            .filter(saPay -> !saPay.getHostCard().getName().equals(hostName))
                            .collect(Collectors.toList());
                    break;
                default:
                    break;
            }
        }

        List<SpellAbility> filteredList = new ArrayList<>(saList);
        // Prefer multi-mana sources if they do not overpay, otherwise prefer single-mana sources
        filteredList.sort((a, b) -> {
            boolean multiA = isMultiManaAbility(a);
            boolean multiB = isMultiManaAbility(b);
            boolean overpayA = wouldOverpayWithAbility(a, cost);
            boolean overpayB = wouldOverpayWithAbility(b, cost);
            if (multiA && !overpayA && (!multiB || overpayB)) return -1;
            if (multiB && !overpayB && (!multiA || overpayA)) return 1;
            if (multiA && multiB) {
                if (overpayA && !overpayB) return 1;
                if (overpayB && !overpayA) return -1;
            }
            return 0;
        });

        // NEW: Prefer a multi-mana source that pays the remaining cost exactly (no overpay)
        for (SpellAbility ab : saList) {
            if (isMultiManaAbility(ab) && !wouldOverpayWithAbility(ab, cost)) {
                // Check if this ability pays the remaining cost exactly
                AbilityManaPart manaPart = ab.getManaPart();
                String manaProduced = manaPart.mana(ab);
                int manaProducedCount = 0;
                for (char c : manaProduced.toCharArray()) {
                    if (c == 'W' || c == 'U' || c == 'B' || c == 'R' || c == 'G' || c == 'C' || c == 'X' || (c >= '1' && c <= '9')) manaProducedCount++;
                }
                int manaNeeded = cost.getUnpaidShards().size() + cost.getGenericManaAmount();
                if (manaProducedCount == manaNeeded) {
                    // Also check that the colors match the cost (for colored mana)
                    // If so, use this ability immediately
                    return ab;
                }
            }
        }

        for (final SpellAbility ma : filteredList) {
            // this rarely seems like a good idea
            if (ma.getHostCard() == saHost) {
                continue;
            }

            if (ma.getPayCosts().hasTapCost() && AiCardMemory.isRememberedCard(ai, ma.getHostCard(), AiCardMemory.MemorySet.PAYS_TAP_COST)) {
                continue;
            }

            if (!ComputerUtilCost.checkTapTypeCost(ai, ma.getPayCosts(), ma.getHostCard(), sa, AiCardMemory.getMemorySet(ai, AiCardMemory.MemorySet.PAYS_TAP_COST))) {
                continue;
            }

            if (sa.getApi() == ApiType.Animate) {
                // For abilities like Genju of the Cedars, make sure that we're not activating the aura ability by tapping the enchanted card for mana
                if (saHost.isAura() && "Enchanted".equals(sa.getParam("Defined"))
                        && ma.getHostCard() == saHost.getEnchantingCard()
                        && ma.getPayCosts().hasTapCost()) {
                    continue;
                }

                // If a manland was previously animated this turn, do not tap it to animate another manland
                if (saHost.isLand() && ma.getHostCard().isLand()
                        && ai.getController().isAI()
                        && AnimateAi.isAnimatedThisTurn(ai, ma.getHostCard())) {
                    continue;
                }
            } else if (sa.getApi() == ApiType.Pump) {
                if ((saHost.isInstant() || saHost.isSorcery())
                        && ma.getHostCard().isCreature()
                        && ai.getController().isAI()
                        && ma.getPayCosts().hasTapCost()
                        && sa.getTargets().getTargetCards().contains(ma.getHostCard())) {
                    // do not activate pump instants/sorceries targeting creatures by tapping targeted
                    // creatures for mana (for example, Servant of the Conduit)
                    continue;
                }
            } else if (sa.getApi() == ApiType.Attach
                    && "AvoidPayingWithAttachTarget".equals(saHost.getSVar("AIPaymentPreference"))) {
                // For cards like Genju of the Cedars, make sure we're not attaching to the same land that will
                // be tapped to pay its own cost if there's another untapped land like that available
                if (ma.getHostCard().equals(sa.getTargetCard())) {
                    if (CardLists.count(ai.getCardsIn(ZoneType.Battlefield), CardPredicates.nameEquals(ma.getHostCard().getName()).and(CardPredicates.UNTAPPED)) > 1) {
                        continue;
                    }
                }
            }

            SpellAbility paymentChoice = ma;

            // Exception: when paying generic mana with Cavern of Souls, prefer the colored mana producing ability
            // to attempt to make the spell uncounterable when possible.
            if (ComputerUtilAbility.getAbilitySourceName(ma).equals("Cavern of Souls")
                    && saHost.getType().hasCreatureType(ma.getHostCard().getChosenType())) {
                if (toPay == ManaCostShard.COLORLESS && cost.getUnpaidShards().contains(ManaCostShard.GENERIC)) {
                    // Deprioritize Cavern of Souls, try to pay generic mana with it instead to use the NoCounter ability
                    continue;
                } else if (toPay == ManaCostShard.GENERIC || toPay == ManaCostShard.X) {
                    for (SpellAbility ab : saList) {
                        if (ab.isManaAbility() && ab.getManaPart().isAnyMana() && ab.hasParam("AddsNoCounter")) {
                            if (!ab.getHostCard().isTapped()) {
                                paymentChoice = ab;
                                break;
                            }
                        }
                    }
                }
            }

            if (!canPayShardWithSpellAbility(toPay, paymentChoice, checkCosts, cost.getXManaCostPaidByColor())) {
                continue;
            }

            if (!ComputerUtilCost.checkForManaSacrificeCost(ai, ma.getPayCosts(), ma, ma.isTrigger())) {
                continue;
            }

            return paymentChoice;
        }
        return null;
    }

//    public CardCollection getManaSourcesToPayCost(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai) {
//        CardCollection manaSources = new CardCollection();
//
//        adjustManaCostToAvoidNegEffects(sa.getHostCard());
//        List<Mana> manaSpentToPay = new ArrayList<>();
//
//        List<ManaCostShard> unpaidShards = cost.getUnpaidShards();
//        Collections.sort(unpaidShards); // most difficult shards must come first
//        for (ManaCostShard part : unpaidShards) {
//            if (part != ManaCostShard.X) {
//                if (cost.isPaid()) {
//                    continue;
//                }
//
//                // get a mana of this type from floating, bail if none available
//                final Mana mana = CostPayment.getMana(ai, part, sa, (byte) -1, cost.getXManaCostPaidByColor());
//                if (mana != null) {
//                    if (ai.getManaPool().tryPayCostWithMana(sa, cost, mana, false)) {
//                        manaSpentToPay.add(mana);
//                    }
//                }
//            }
//        }
//
//        if (cost.isPaid()) {
//            // refund any mana taken from mana pool when test
//            ai.getManaPool().refundMana(manaSpentToPay);
//            CostPayment.handleOfferings(sa, true, cost.isPaid());
//            return manaSources;
//        }
//
//        if (sourcesByColor.isEmpty()) {
//            ai.getManaPool().refundMana(manaSpentToPay);
//            CostPayment.handleOfferings(sa, true, cost.isPaid());
//            return manaSources;
//        }
//
//        // select which abilities may be used for each shard
//        Multimap<ManaCostShard, SpellAbility> sourcesForShards = groupAndOrderToPayShards();
//
//        sortManaAbilities(sourcesForShards, sa, sortedManaSources);
//
//        ManaCostShard toPay;
//        // Loop over mana needed
//        while (!cost.isPaid()) {
//            toPay = getNextShardToPay(cost, sourcesForShards);
//
//            Collection<SpellAbility> saList = sourcesForShards.get(toPay);
//            if (saList == null) {
//                break;
//            }
//
//            SpellAbility saPayment = chooseManaAbility(cost, sa, ai, toPay, saList, true);
//            if (saPayment == null) {
//                boolean lifeInsteadOfBlack = toPay.isBlack() && ai.hasKeyword("PayLifeInsteadOf:B");
//                if ((!toPay.isPhyrexian() && !lifeInsteadOfBlack) || !ai.canPayLife(2, false, sa)) {
//                    break; // cannot pay
//                }
//
//                if (toPay.isPhyrexian()) {
//                    cost.payPhyrexian();
//                } else if (lifeInsteadOfBlack) {
//                    cost.decreaseShard(ManaCostShard.BLACK, 1);
//                }
//
//                continue;
//            }
//
//            manaSources.add(saPayment.getHostCard());
//            setExpressColorChoice(sa, ai, cost, toPay, saPayment);
//
//            String manaProduced = predictManafromSpellAbility(saPayment, ai, toPay);
//
//            payMultipleMana(cost, manaProduced, ai);
//
//            // remove from available lists
//            sourcesForShards.values().removeIf(CardTraitPredicates.isHostCard(saPayment.getHostCard()));
//        }
//
//        CostPayment.handleOfferings(sa, true, cost.isPaid());
//        ai.getManaPool().refundMana(manaSpentToPay);
//
//        return manaSources;
//    }

    public boolean payManaCost() {
        if ((sa.isOffering() && sa.getSacrificedAsOffering() == null) || (sa.isEmerge() && sa.getSacrificedAsEmerge() == null)) {
            return false;
        }

        AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.PAYS_TAP_COST);
        AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.PAYS_SAC_COST);
        adjustManaCostToAvoidNegEffects(sa.getHostCard());

        List<Mana> manaSpentToPay = test ? new ArrayList<>() : sa.getPayingMana();
        List<SpellAbility> paymentList = Lists.newArrayList();
        final ManaPool manapool = ai.getManaPool();

        // Apply color/type conversion matrix if necessary (already done via autopay)
        if (ai.getControllingPlayer() == null) {
            manapool.restoreColorReplacements();
            CardPlayOption mayPlay = sa.getMayPlayOption();
            if (!effect) {
                if (sa.isSpell() && mayPlay != null) {
                    mayPlay.applyManaConvert(manapool);
                } else if (sa.isActivatedAbility() && sa.getGrantorStatic() != null && sa.getGrantorStatic().hasParam("ManaConversion")) {
                    AbilityUtils.applyManaColorConversion(manapool, sa.getGrantorStatic().getParam("ManaConversion"));
                }
            }
            if (sa.hasParam("ManaConversion")) {
                AbilityUtils.applyManaColorConversion(manapool, sa.getParam("ManaConversion"));
            }
            StaticAbilityManaConvert.manaConvert(manapool, ai, sa.getHostCard(), effect && !sa.isCastFromPlayEffect() ? null : sa);
        }

        // not worth checking if it makes sense to not spend floating first
        if (manapool.payManaCostFromPool(cost, sa, test, manaSpentToPay)) {
            CostPayment.handleOfferings(sa, test, cost.isPaid());
            // paid all from floating mana
            return true;
        }

        boolean purePhyrexian = cost.containsOnlyPhyrexianMana();
        boolean hasConverge = sa.getHostCard().hasConverge();

        int testEnergyPool = ai.getCounters(CounterEnumType.ENERGY);
        ManaCostShard toPay = null;
        List<SpellAbility> saExcludeList = new ArrayList<>();

        // --- Multi-mana source check: must be at the very start, before any payment is made ---
        if (!cost.isPaid()) {
            int manaNeeded = cost.getUnpaidShards().size() + cost.getGenericManaAmount();
            for (Card manaSource : sortedManaSources) {
                for (SpellAbility ability : getAIPlayableMana(manaSource)) {
                    if (isMultiManaAbility(ability) && !wouldOverpayWithAbility(ability, cost)) {
                        AbilityManaPart manaPart = ability.getManaPart();
                        String manaProduced = manaPart.mana(ability);
                        int manaProducedCount = 0;
                        for (char c : manaProduced.toCharArray()) {
                            if (c == 'W' || c == 'U' || c == 'B' || c == 'R' || c == 'G' || c == 'C' || c == 'X' || (c >= '1' && c <= '9')) manaProducedCount++;
                        }
                        if (manaProducedCount == manaNeeded) {
                            setExpressColorChoice(getNextShardToPay(), ability);
                            if (test) {
                                String manaProducedTest = predictManafromSpellAbility(ability, ai, getNextShardToPay());
                                payMultipleMana(manaProducedTest);
                            } else {
                                final CostPayment pay = new CostPayment(ability.getPayCosts(), ability);
                                if (!pay.payComputerCosts(new AiCostDecision(ai, ability, effect))) {
                                    continue;
                                }
                                ai.getGame().getStack().addAndUnfreeze(ability);
                                manapool.payManaFromAbility(sa, cost, ability);
                            }
                            paymentList.add(ability);
                            // Remove this multi-mana source from all other shard lists for this payment
                            if (sourcesForShards != null) {
                                for (ManaCostShard shard : sourcesForShards.keySet()) {
                                    sourcesForShards.get(shard).removeIf(sa2 -> sa2.getHostCard() == manaSource);
                                }
                            }
                            if (cost.isPaid()) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        boolean lifeInsteadOfBlack = false;
        Collection<SpellAbility> saList = null;
        // Loop over mana needed
        while (!cost.isPaid()) {
            while (!cost.isPaid() && !manapool.isEmpty()) {
                boolean found = false;
                for (byte color : ManaAtom.MANATYPES) {
                    if (manapool.tryPayCostWithColor(color, sa, cost, manaSpentToPay)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    break;
                }
            }
            if (cost.isPaid()) {
                break;
            }

            if (sourcesForShards == null && !purePhyrexian) {
                break;    // no mana abilities to use for paying
            }

            toPay = getNextShardToPay();

            if (hasConverge &&
                    (toPay == ManaCostShard.GENERIC || toPay == ManaCostShard.X)) {
                final int unpaidColors = cost.getUnpaidColors() + cost.getColorsPaid() ^ ManaCostShard.COLORS_SUPERPOSITION;
                for (final MagicColor.Color b : ColorSet.fromMask(unpaidColors)) {
                    // try and pay other colors for converge
                    final ManaCostShard shard = ManaCostShard.valueOf(String.valueOf(b));
                    saList = sourcesForShards.get(shard);
                    if (saList != null && !saList.isEmpty()) {
                        toPay = shard;
                        break;
                    }
                }
                if (saList == null || saList.isEmpty()) {
                    // failed to converge, revert to paying generic
                    saList = sourcesForShards.get(toPay);
                    hasConverge = false;
                }
            } else {
                if (!(sourcesForShards == null && purePhyrexian)) {
                    saList = sourcesForShards.get(toPay);
                } else {
                    saList = Lists.newArrayList(); // Phyrexian mana only: no valid mana sources, but can still pay life
                }
            }
            if (saList == null) {
                break;
            }

            saList.removeAll(saExcludeList);

            SpellAbility saPayment = saList.isEmpty() ? null : chooseManaAbility(toPay, saList, checkPlayable || !test);

            if (saPayment != null && ComputerUtilCost.isSacrificeSelfCost(saPayment.getPayCosts())) {
                if (sa.getTargets() != null && sa.getTargets().contains(saPayment.getHostCard())) {
                    saExcludeList.add(saPayment); // not a good idea to sac a card that you're targeting with the SA you're paying for
                    continue;
                }
            }

            if (saPayment != null && saPayment.hasParam("AILogic")) {
                boolean consider = false;
                if (saPayment.getParam("AILogic").equals("BlackLotus")) {
                    consider = SpecialCardAi.BlackLotus.consider(ai, sa, cost);
                    if (!consider) {
                        saExcludeList.add(saPayment); // since we checked this already, do not loop indefinitely checking again
                        continue;
                    }
                }
            }

            if (saPayment == null && toPay != null) {
                if ((!toPay.isPhyrexian() && !lifeInsteadOfBlack) || !ai.canPayLife(2, false, sa)
                        || (ai.getLife() <= 2 && !ai.cantLoseForZeroOrLessLife())) {
                    break; // cannot pay
                }

                if (sa.hasParam("AIPhyrexianPayment")) {
                    if ("Never".equals(sa.getParam("AIPhyrexianPayment"))) {
                        break; // unwise to pay
                    } else if (sa.getParam("AIPhyrexianPayment").startsWith("OnFatalDamage.")) {
                        int dmg = Integer.parseInt(sa.getParam("AIPhyrexianPayment").substring(14));
                        if (ai.getOpponents().stream().noneMatch(PlayerPredicates.lifeLessOrEqualTo(dmg))) {
                            break; // no one to finish with the gut shot
                        }
                    }
                }

                if (toPay.isPhyrexian()) {
                    cost.payPhyrexian();
                    if (!test) {
                        sa.setSpendPhyrexianMana(true);
                    }
                } else if (lifeInsteadOfBlack) {
                    cost.decreaseShard(ManaCostShard.BLACK, 1);
                }

                if (!test) {
                    ai.payLife(2, sa, false);
                }
                continue;
            }
            paymentList.add(saPayment);

            setExpressColorChoice(toPay, saPayment);

            if (saPayment.getPayCosts().hasTapCost()) {
                AiCardMemory.rememberCard(ai, saPayment.getHostCard(), AiCardMemory.MemorySet.PAYS_TAP_COST);
            }

            if (test) {
                // Check energy when testing
                CostPayEnergy energyCost = saPayment.getPayCosts().getCostEnergy();
                if (energyCost != null) {
                    testEnergyPool -= Integer.parseInt(energyCost.getAmount());
                    if (testEnergyPool < 0) {
                        // Can't pay energy cost
                        break;
                    }
                }

                String manaProduced = predictManafromSpellAbility(saPayment, ai, toPay);
                payMultipleMana(manaProduced);

                // remove from available lists
                sourcesForShards.values().removeIf(CardTraitPredicates.isHostCard(saPayment.getHostCard()));
            } else {
                final CostPayment pay = new CostPayment(saPayment.getPayCosts(), saPayment);
                if (!pay.payComputerCosts(new AiCostDecision(ai, saPayment, effect))) {
                    saList.remove(saPayment);
                    continue;
                }

                ai.getGame().getStack().addAndUnfreeze(saPayment);
                // subtract mana from mana pool
                manapool.payManaFromAbility(sa, cost, saPayment);

                // no need to remove abilities from resource map,
                // once their costs are paid and consume resources, they can not be used again

                if (hasConverge) {
                    // hack to prevent converge re-using sources
                    sourcesForShards.values().removeIf(CardTraitPredicates.isHostCard(saPayment.getHostCard()));
                }
            }
        }

        CostPayment.handleOfferings(sa, test, cost.isPaid());

//        if (DEBUG_MANA_PAYMENT) {
//            System.err.printf("%s > [%s] payment has %s (%s +%d) for (%s) %s:%n\t%s%n%n",
//                    FThreads.debugGetCurrThreadId(), test ? "test" : "PROD", cost.isPaid() ? "*PAID*" : "failed", originalCost,
//                    extraMana, sa.getHostCard(), sa.toUnsuppressedString(), StringUtils.join(paymentPlan, "\n\t"));
//        }

        // The cost is still unpaid, so refund the mana and report
        if (!cost.isPaid()) {
            manapool.refundMana(manaSpentToPay);
            if (test) {
                resetPayment(paymentList);
            } else {
                System.out.println("ComputerUtilMana: payManaCost() cost was not paid for " + sa + " (" +  sa.getHostCard().getName() + "). Didn't find what to pay for " + toPay);
                sa.setSkip(true);
            }
            return false;
        }

        if (test) {
            manapool.refundMana(manaSpentToPay);
            resetPayment(paymentList);
        }

        return true;
    }

    private void setExpressColorChoice(ManaCostShard toPay, SpellAbility saPayment) {
        if (saPayment == null) {
            return;
        }

        AbilityManaPart m = saPayment.getManaPart();
        if (m.isComboMana()) {
            // usually we'll want to produce color that matches the shard
            ColorSet shared = ColorSet.fromMask(toPay.getColorMask()).getSharedColors(ColorSet.fromNames(m.getComboColors(saPayment).split(" ")));
            // but other effects might still lead to a more permissive payment
            if (!shared.isColorless()) {
                m.setExpressChoice(shared);
            }
            getComboManaChoice(saPayment);
        }
        else if (saPayment.getApi() == ApiType.ManaReflected) {
            Set<String> reflected = CardUtil.getReflectableManaColors(saPayment);

            for (byte c : MagicColor.WUBRGC) {
                if (ai.getManaPool().canPayForShardWithColor(toPay, c) && reflected.contains(MagicColor.toLongString(c))) {
                    m.setExpressChoice(MagicColor.toShortString(c));
                    return;
                }
            }
        }
        else if (m.isAnyMana()) {
            byte colorChoice = 0;
            if (toPay.isOr2Generic())
                colorChoice = toPay.getColorMask();
            else {
                for (byte c : MagicColor.WUBRG) {
                    if (ai.getManaPool().canPayForShardWithColor(toPay, c)) {
                        colorChoice = c;
                        break;
                    }
                }
            }
            m.setExpressChoice(MagicColor.toShortString(colorChoice));
        }
    }

    private void resetPayment(List<SpellAbility> payments) {
        for (SpellAbility sa : payments) {
            sa.getManaPart().clearExpressChoice();
        }
    }

    private ListMultimap<ManaCostShard, SpellAbility> getSourcesForShards() {
        boolean hasConverge = sa.getHostCard().hasConverge();
        // arrange all mana abilities by color produced.
        if (sourcesByColor.isEmpty()) {
            // no mana abilities, bailing out
            return null;
        }
        if (DEBUG_MANA_PAYMENT) {
            System.out.println("DEBUG_MANA_PAYMENT: manaAbilityMap = " + sourcesByColor);
        }

        // select which abilities may be used for each shard
        ListMultimap<ManaCostShard, SpellAbility> sourcesForShards = groupAndOrderToPayShards();
        if (hasConverge) {
            // add extra colors for paying converge
            final int unpaidColors = cost.getUnpaidColors() + cost.getColorsPaid() ^ ManaCostShard.COLORS_SUPERPOSITION;
            for (final MagicColor.Color b : ColorSet.fromMask(unpaidColors)) {
                final ManaCostShard shard = ManaCostShard.valueOf(String.valueOf(b));
                if (!sourcesForShards.containsKey(shard)) {
                    if (ai.getManaPool().canPayForShardWithColor(shard, b.getColorMask())) {
                        for (SpellAbility saMana : sourcesByColor.get((int) b.getColorMask())) {
                            sourcesForShards.get(shard).add(saMana);
                        }
                    }
                }
            }
        }

        sortManaAbilities(sourcesForShards, sa, sortedManaSources);
        if (DEBUG_MANA_PAYMENT) {
            System.out.println("DEBUG_MANA_PAYMENT: sourcesForShards = " + sourcesForShards);
        }
        return sourcesForShards;
    }

    private ListMultimap<Integer, SpellAbility> groupSourcesByManaColor() {
        final ListMultimap<Integer, SpellAbility> manaMap = ArrayListMultimap.create();
        final Game game = ai.getGame();

        // Loop over all current available mana sources
        for (final Card sourceCard : sortedManaSources) {
            if (DEBUG_MANA_PAYMENT) {
                System.out.println("DEBUG_MANA_PAYMENT: groupSourcesByManaColor sourceCard = " + sourceCard);
            }
            for (final SpellAbility m : getAIPlayableMana(sourceCard)) {
                if (DEBUG_MANA_PAYMENT) {
                    System.out.println("DEBUG_MANA_PAYMENT: groupSourcesByManaColor m = " + m);
                }
                m.setActivatingPlayer(ai);
                if (checkPlayable && !m.canPlay()) {
                    continue;
                }

                // don't kill yourself
                final Cost abCost = m.getPayCosts();
                if (!ComputerUtilCost.checkLifeCost(ai, abCost, sourceCard, 1, m)) {
                    continue;
                }

                // don't use abilities with dangerous drawbacks
                AbilitySub sub = m.getSubAbility();
                if (sub != null) {
                    if (!SpellApiToAi.Converter.get(sub).chkDrawbackWithSubs(ai, sub).willingToPlay()) {
                        continue;
                    }
                }

                manaMap.get(ManaAtom.GENERIC).add(m); // add to generic source list

                SpellAbility tail = m;
                while (tail != null) {
                    AbilityManaPart mp = m.getManaPart();
                    if (mp != null && tail.metConditions()) {
                        // TODO Replacement Check currently doesn't work for reflected colors

                        // setup produce mana replacement effects
                        String origin = mp.getOrigProduced();
                        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(sourceCard);
                        repParams.put(AbilityKey.Mana, origin);
                        repParams.put(AbilityKey.Activator, ai);
                        repParams.put(AbilityKey.AbilityMana, m); // RootAbility

                        List<ReplacementEffect> reList = game.getReplacementHandler().getReplacementList(ReplacementType.ProduceMana, repParams, ReplacementLayer.Other);

                        if (reList.isEmpty()) {
                            Set<String> reflectedColors = CardUtil.getReflectableManaColors(m);
                            // find possible colors
                            for (byte color : MagicColor.WUBRG) {
                                if (tail.canThisProduce(MagicColor.toShortString(color)) || reflectedColors.contains(MagicColor.toLongString(color))) {
                                    manaMap.put((int)color, m);
                                }
                            }
                            if (m.canThisProduce("C") || reflectedColors.contains(MagicColor.Constant.COLORLESS)) {
                                manaMap.put(ManaAtom.COLORLESS, m);
                            }
                        } else {
                            // try to guess the color the mana gets replaced to
                            for (ReplacementEffect re : reList) {
                                SpellAbility o = re.getOverridingAbility();
                                String replaced = origin;
                                if (o == null || o.getApi() != ApiType.ReplaceMana) {
                                    continue;
                                }
                                if (o.hasParam("ReplaceMana")) {
                                    replaced = o.getParam("ReplaceMana");
                                } else if (o.hasParam("ReplaceType")) {
                                    String color = o.getParam("ReplaceType");
                                    for (byte c : MagicColor.WUBRGC) {
                                        String s = MagicColor.toShortString(c);
                                        replaced = replaced.replace(s, color);
                                    }
                                } else if (o.hasParam("ReplaceColor")) {
                                    String color = o.getParam("ReplaceColor");
                                    if (o.hasParam("ReplaceOnly")) {
                                        replaced = replaced.replace(o.getParam("ReplaceOnly"), color);
                                    } else {
                                        for (byte c : MagicColor.WUBRG) {
                                            String s = MagicColor.toShortString(c);
                                            replaced = replaced.replace(s, color);
                                        }
                                    }
                                }

                                for (byte color : MagicColor.WUBRG) {
                                    if ("Any".equals(replaced) || replaced.contains(MagicColor.toShortString(color))) {
                                        manaMap.put((int)color, m);
                                    }
                                }

                                if (replaced.contains("C")) {
                                    manaMap.put(ManaAtom.COLORLESS, m);
                                }

                            }
                        }
                    }
                    tail = tail.getSubAbility();
                }

                if (m.getHostCard().isSnow()) {
                    manaMap.put(ManaAtom.IS_SNOW, m);
                }
                if (DEBUG_MANA_PAYMENT) {
                    System.out.println("DEBUG_MANA_PAYMENT: groupSourcesByManaColor manaMap  = " + manaMap);
                }
            } // end of mana abilities loop
        } // end of mana sources loop

        return manaMap;
    }

    private ListMultimap<ManaCostShard, SpellAbility> groupAndOrderToPayShards() {
        ListMultimap<ManaCostShard, SpellAbility> res = ArrayListMultimap.create();

        if (cost.getGenericManaAmount() > 0 && sourcesByColor.containsKey(ManaAtom.GENERIC)) {
            res.putAll(ManaCostShard.GENERIC, sourcesByColor.get(ManaAtom.GENERIC));
        }

        // loop over cost parts
        for (ManaCostShard shard : cost.getDistinctShards()) {
            if (DEBUG_MANA_PAYMENT) {
                System.out.println("DEBUG_MANA_PAYMENT: shard = " + shard);
            }
            if (shard == ManaCostShard.S) {
                res.putAll(shard, sourcesByColor.get(ManaAtom.IS_SNOW));
                continue;
            }

            if (shard.isOr2Generic()) {
                Integer colorKey = (int) shard.getColorMask();
                if (sourcesByColor.containsKey(colorKey))
                    res.putAll(shard, sourcesByColor.get(colorKey));
                if (sourcesByColor.containsKey(ManaAtom.GENERIC))
                    res.putAll(shard, sourcesByColor.get(ManaAtom.GENERIC));
                continue;
            }

            if (shard == ManaCostShard.GENERIC) {
                continue;
            }

            for (Integer colorint : sourcesByColor.keySet()) {
                // apply mana color change matrix here
                if (ai.getManaPool().canPayForShardWithColor(shard, colorint.byteValue())) {
                    for (SpellAbility sa : sourcesByColor.get(colorint)) {
                        if (!res.get(shard).contains(sa)) {
                            res.get(shard).add(sa);
                        }
                    }
                }
            }
        }

        return res;
    }

    private Map<Card, Integer> getSourceByFlexibility() {
        Map<Card, Integer> sourceByFlexibility = new HashMap<>();
        for (Card card : sortedManaSources) {
            int flexibility = 0;
            for (SpellAbility sa : getAIPlayableMana(card)) {
                if (sa.getPayCosts().hasTapCost()) {
                    flexibility += 1; // tap ability
                }
//                if (sa.getPayCosts().hasSacrificeCost()) {
//                    flexibility += 2; // sacrifice ability
//                }
//                if (sa.getPayCosts().hasDiscardCost()) {
//                    flexibility += 3; // discard ability
//                }
            }
            if (flexibility > 0) {
                sourceByFlexibility.put(card, flexibility);
            }
        }
        return sourceByFlexibility;
    }


    private CardCollection getAvailableManaSources() {
        final CardCollectionView list = CardCollection.combine(ai.getCardsIn(ZoneType.Battlefield), ai.getCardsIn(ZoneType.Hand));
        final List<Card> manaSources = CardLists.filter(list, c -> {
            for (final SpellAbility am : getAIPlayableMana(c)) {
                am.setActivatingPlayer(ai);
                if (!checkPlayable || (am.canPlay() && am.checkRestrictions(ai))) {
                    return true;
                }
            }
            return false;
        }); // CardListFilter

        final CardCollection sortedManaSources = new CardCollection();
        final CardCollection multiManaSources = new CardCollection();
        final CardCollection otherManaSources = new CardCollection();
        final CardCollection useLastManaSources = new CardCollection();
        final CardCollection colorlessManaSources = new CardCollection();
        final CardCollection oneManaSources = new CardCollection();
        final CardCollection twoManaSources = new CardCollection();
        final CardCollection threeManaSources = new CardCollection();
        final CardCollection fourManaSources = new CardCollection();
        final CardCollection fiveManaSources = new CardCollection();
        final CardCollection anyColorManaSources = new CardCollection();

        // Sort mana sources
        // 0. Multi-mana sources (produce more than 1 mana per activation, including repeated symbols)
        // 1. Use lands that can only produce colorless mana without
        // drawback/cost first
        // 2. Search for mana sources that have a certain number of abilities
        // 3. Use lands that produce any color mana
        // 4. all other sources (creature, costs, drawback, etc.)
        for (Card card : manaSources) {
            boolean isMultiMana = false;
            for (SpellAbility m : getAIPlayableMana(card)) {
                if (!isReusableCost(m)) {
                    continue;
                }

                AbilityManaPart manaPart = m.getManaPart();
                if (manaPart != null) {
                    //String manaProduced = manaPart.mana(m);
                    String manaProduced = predictManafromSpellAbility(m, ai);
                    int produced = 0;
                    // Count all mana symbols, including repeats and numbers
                    for (char c : manaProduced.toCharArray()) {
                        if (c == 'W' || c == 'U' || c == 'B' || c == 'R' || c == 'G' || c == 'C') produced++;
                        else if (Character.isDigit(c)) produced += Character.getNumericValue(c);
                    }
                    int amount = m.amountOfManaGenerated(false);
                    if (produced > 1 || amount > 1) {
                        isMultiMana = true;
                        break;
                    }
                }
            }
            if (isMultiMana) {
                multiManaSources.add(card);
                continue;
            }
            // exclude creature sources that will tap as a part of an attack declaration
            if (card.isCreature()) {
                if (card.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS, ai)) {
                    Combat combat = card.getGame().getCombat();
                    if (combat.getAttackers().indexOf(card) != -1 && !card.hasKeyword(Keyword.VIGILANCE)) {
                        continue;
                    }
                }
            }
            // exclude cards that will deal lethal damage when tapped
            if (ai.canLoseLife() && !ai.cantLoseForZeroOrLessLife()) {
                boolean dealsLethalOnTap = false;
                for (Trigger t : card.getTriggers()) {
                    if (t.getMode() == TriggerType.Taps || t.getMode() == TriggerType.TapsForMana) {
                        SpellAbility trigSa = t.getOverridingAbility();
                        if (trigSa.getApi() == ApiType.DealDamage && trigSa.getParamOrDefault("Defined", "").equals("You")) {
                            int numDamage = AbilityUtils.calculateAmount(card, trigSa.getParam("NumDmg"), null);
                            numDamage = ai.staticReplaceDamage(numDamage, card, false);
                            if (ai.getLife() <= numDamage) {
                                dealsLethalOnTap = true;
                                break;
                            }
                        }
                    }
                }
                if (dealsLethalOnTap) {
                    continue;
                }
            }

            if (card.isCreature() || card.isEnchanted()) {
                otherManaSources.add(card);
                continue; // don't use creatures before other permanents
            }
            int usableManaAbilities = 0;
            boolean needsLimitedResources = false;
            boolean unpreferredCost = false;
            boolean producesAnyColor = false;
            final List<SpellAbility> manaAbilities = getAIPlayableMana(card);
            for (final SpellAbility m : manaAbilities) {
                if (m.getManaPart().isAnyMana()) {
                    producesAnyColor = true;
                }
                final Cost cost = m.getPayCosts();
                if (cost != null) {
                    m.setActivatingPlayer(ai);
                    if (!CostPayment.canPayAdditionalCosts(m.getPayCosts(), m, false)) {
                        continue;
                    }
                    if (!cost.isReusuableResource()) {
                        for(CostPart part : cost.getCostParts()) {
                            if (part instanceof CostSacrifice && !part.payCostFromSource()) {
                                unpreferredCost = true;
                            }
                        }
                        needsLimitedResources = !unpreferredCost;
                    }
                }
                AbilitySub sub = m.getSubAbility();
                if (sub != null && !card.getName().equals("Pristine Talisman") && !card.getName().equals("Zhur-Taa Druid")) {
                    if (!SpellApiToAi.Converter.get(sub).chkDrawbackWithSubs(ai, sub).willingToPlay()) {
                        continue;
                    }
                    needsLimitedResources = true;
                }
                usableManaAbilities++;
            }
            if (unpreferredCost) {
                useLastManaSources.add(card);
            } else if (needsLimitedResources) {
                otherManaSources.add(card);
            } else if (producesAnyColor) {
                anyColorManaSources.add(card);
            } else if (usableManaAbilities == 1) {
                if (manaAbilities.get(0).getManaPart().mana(manaAbilities.get(0)).equals("C")) {
                    colorlessManaSources.add(card);
                } else {
                    oneManaSources.add(card);
                }
            } else if (usableManaAbilities == 2) {
                twoManaSources.add(card);
            } else if (usableManaAbilities == 3) {
                threeManaSources.add(card);
            } else if (usableManaAbilities == 4) {
                fourManaSources.add(card);
            } else {
                fiveManaSources.add(card);
            }
        }
        sortedManaSources.clear();
        sortedManaSources.addAll(multiManaSources);
        sortedManaSources.addAll(colorlessManaSources);
        sortedManaSources.addAll(oneManaSources);
        sortedManaSources.addAll(twoManaSources);
        sortedManaSources.addAll(threeManaSources);
        sortedManaSources.addAll(fourManaSources);
        sortedManaSources.addAll(fiveManaSources);
        sortedManaSources.addAll(anyColorManaSources);
        //use better creatures later
        ComputerUtilCard.sortByEvaluateCreature(otherManaSources);
        Collections.reverse(otherManaSources);
        sortedManaSources.addAll(otherManaSources);
        // This should be things like sacrifice other stuff.
        ComputerUtilCard.sortByEvaluateCreature(useLastManaSources);
        Collections.reverse(useLastManaSources);
        sortedManaSources.addAll(useLastManaSources);

        if (DEBUG_MANA_PAYMENT) {
            System.out.println("DEBUG_MANA_PAYMENT: sortedManaSources = " + sortedManaSources);
        }
        return sortedManaSources;
    }

    public List<SpellAbility> getAIPlayableMana(Card c) {
        final List<SpellAbility> res = new ArrayList<>();
        for (final SpellAbility a : c.getManaAbilities()) {
            // if a mana ability has a mana cost the AI will miscalculate
            // if there is a parent ability the AI can't use it
            final Cost cost = a.getPayCosts();
            if (cost.hasManaCost() || (a.getApi() != ApiType.Mana && a.getApi() != ApiType.ManaReflected)) {
                continue;
            }

            if (a.getRestrictions() != null && a.getRestrictions().isInstantSpeed()) {
                continue;
            }

            if (!res.contains(a)) {
                if (cost.isReusuableResource()) {
                    res.add(0, a);
                } else {
                    res.add(res.size(), a);
                }
            }
        }
        return res;
    }

    private String payMultipleMana(String mana) {
        List<String> unused = new ArrayList<>(4);
        for (String manaPart : TextUtil.split(mana, ' ')) {
            if (StringUtils.isNumeric(manaPart)) {
                for (int i = Integer.parseInt(manaPart); i > 0; i--) {
                    boolean wasNeeded = cost.ai_payMana("1", ai.getManaPool());
                    if (!wasNeeded) {
                        unused.add(Integer.toString(i));
                        break;
                    }
                }
            } else {
                String color = MagicColor.toShortString(manaPart);
                boolean wasNeeded = cost.ai_payMana(color, ai.getManaPool());
                if (!wasNeeded) {
                    unused.add(color);
                }
            }
        }
        return unused.isEmpty() ? null : StringUtils.join(unused, ' ');
    }

    private boolean canPayShardWithSpellAbility(ManaCostShard toPay, SpellAbility ma, boolean checkCosts, Map<String, Integer> xManaCostPaidByColor) {
        final Card sourceCard = ma.getHostCard();

        if (isManaSourceReserved()) {
            return false;
        }

        if (toPay.isSnow() && !sourceCard.isSnow()) {
            return false;
        }

        AbilityManaPart m = ma.getManaPart();
        if (!m.meetsManaRestrictions(sa)) {
            return false;
        }

        if (checkCosts) {
            // Check if AI can still play this mana ability
            ma.setActivatingPlayer(ai);
            // if the AI can't pay the additional costs skip the mana ability
            if (!CostPayment.canPayAdditionalCosts(ma.getPayCosts(), ma, false)) {
                return false;
            } else if (ma.getRestrictions() != null && ma.getRestrictions().isInstantSpeed()) {
                return false;
            }
        }

        if (m.isComboMana()) {
            for (String s : m.getComboColors(ma).split(" ")) {
                if (toPay == ManaCostShard.COLORED_X && !ManaCostBeingPaid.canColoredXShardBePaidByColor(s, xManaCostPaidByColor)) {
                    continue;
                }

                if (!sa.allowsPayingWithShard(sourceCard, ManaAtom.fromName(s))) {
                    continue;
                }

                if ("Any".equals(s) || ai.getManaPool().canPayForShardWithColor(toPay, ManaAtom.fromName(s)))
                    return true;
            }
            return false;
        }

        if (ma.getApi() == ApiType.ManaReflected) {
            Set<String> reflected = CardUtil.getReflectableManaColors(ma);

            for (byte c : MagicColor.WUBRGC) {
                if (toPay == ManaCostShard.COLORED_X && !ManaCostBeingPaid.canColoredXShardBePaidByColor(MagicColor.toShortString(c), xManaCostPaidByColor)) {
                    continue;
                }

                if (!sa.allowsPayingWithShard(sourceCard, c)) {
                    continue;
                }

                if (ai.getManaPool().canPayForShardWithColor(toPay, c) && reflected.contains(MagicColor.toLongString(c))) {
                    m.setExpressChoice(MagicColor.toShortString(c));
                    return true;
                }
            }
            return false;
        }

        if (!sa.allowsPayingWithShard(sourceCard, MagicColor.fromName(m.getOrigProduced()))) {
            return false;
        }

        if (toPay == ManaCostShard.COLORED_X) {
            for (String s : m.mana(ma).split(" ")) {
                if (ManaCostBeingPaid.canColoredXShardBePaidByColor(s, xManaCostPaidByColor)) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    private void getComboManaChoice(final SpellAbility manaAb) {
        final StringBuilder choiceString = new StringBuilder();
        final Card source = manaAb.getHostCard();
        final AbilityManaPart abMana = manaAb.getManaPart();

        if (abMana.isComboMana()) {
            int amount = manaAb.hasParam("Amount") ? AbilityUtils.calculateAmount(source, manaAb.getParam("Amount"), manaAb) : 1;
            final ManaCostBeingPaid testCost = new ManaCostBeingPaid(cost);
            final String[] comboColors = abMana.getComboColors(manaAb).split(" ");
            for (int nMana = 1; nMana <= amount; nMana++) {
                String choice = "";
                // Use expressChoice first
                if (!abMana.getExpressChoice().isEmpty()) {
                    choice = abMana.getExpressChoice();
                    abMana.clearExpressChoice();
                    byte colorMask = ManaAtom.fromName(choice);
                    if (manaAb.canProduce(choice) && satisfiesColorChoice(abMana, choiceString, choice) && testCost.isAnyPartPayableWith(colorMask, ai.getManaPool())) {
                        choiceString.append(choice);
                        payMultipleMana(choice);
                        continue;
                    }
                }
                // check colors needed for cost
                if (!testCost.isPaid()) {
                    // Loop over combo colors
                    for (String color : comboColors) {
                        if (satisfiesColorChoice(abMana, choiceString, choice) && testCost.needsColor(ManaAtom.fromName(color), ai.getManaPool())) {
                            payMultipleMana(color);
                            if (nMana != 1) {
                                choiceString.append(" ");
                            }
                            choiceString.append(color);
                            choice = color;
                            break;
                        }
                    }
                    if (!choice.isEmpty()) {
                        continue;
                    }
                }
                // check if combo mana can produce most common color in hand
                String commonColor = ComputerUtilCard.getMostProminentColor(ai.getCardsIn(ZoneType.Hand));
                if (!commonColor.isEmpty() && satisfiesColorChoice(abMana, choiceString, MagicColor.toShortString(commonColor)) && abMana.getComboColors(manaAb).contains(MagicColor.toShortString(commonColor))) {
                    choice = MagicColor.toShortString(commonColor);
                } else {
                    // default to first available color
                    for (String c : comboColors) {
                        if (satisfiesColorChoice(abMana, choiceString, c)) {
                            choice = c;
                            break;
                        }
                    }
                }
                if (nMana != 1) {
                    choiceString.append(" ");
                }
                choiceString.append(choice);
            }
        }
        if (choiceString.toString().isEmpty()) {
            choiceString.append("0");
        }

        abMana.setExpressChoice(choiceString.toString());
    }

    private static boolean satisfiesColorChoice(AbilityManaPart abMana, StringBuilder choices, String choice) {
        return !abMana.getOrigProduced().contains("Different") || !choices.toString().contains(choice);
    }

    private ManaCostShard getNextShardToPay() {
        List<ManaCostShard> shardsToPay = Lists.newArrayList(cost.getDistinctShards());
        // optimize order so that the shards with less available sources are considered first
        shardsToPay.sort(Comparator.comparingInt(shard -> sourcesForShards.get(shard).size()));
        // mind the priorities
        // * Pay mono-colored first
        // * Pay 2/C with matching colors
        // * pay hybrids
        // * pay phyrexian, keep mana for colorless
        // * pay generic
        return cost.getShardToPayByPriority(shardsToPay, ColorSet.WUBRG.getColor());
    }

    private void adjustManaCostToAvoidNegEffects(final Card card) {
        // Make mana needed to avoid negative effect a mandatory cost for the AI
        for (String manaPart : card.getSVar("ManaNeededToAvoidNegativeEffect").split(",")) {
            // convert long color strings to short color strings
            if (manaPart.isEmpty()) {
                continue;
            }

            byte mask = ManaAtom.fromName(manaPart);

            // make mana mandatory for AI
            if (!cost.needsColor(mask, ai.getManaPool()) && cost.getGenericManaAmount() > 0) {
                ManaCostShard shard = ManaCostShard.valueOf(mask);
                cost.increaseShard(shard, 1);
                cost.decreaseGenericMana(1);
            }
        }
    }

    /**
     * Duplicate of ComputerUtilMana.predictManafromSpellAbility, but without ManaCostShard.
     * Predicts the mana that would be produced by a SpellAbility, including triggers.
     */

    public static String predictManafromSpellAbility(SpellAbility saPayment, Player ai, ManaCostShard shard) {
        // TOOD Copy this over to here
        return ComputerUtilMana.predictManafromSpellAbility(saPayment, ai, shard);
    }

    public static String predictManafromSpellAbility(SpellAbility saPayment, Player ai) {
        Card hostCard = saPayment.getHostCard();
        // Use the base mana produced by the ability
        StringBuilder manaProduced = new StringBuilder(GameActionUtil.generatedTotalMana(saPayment));
        String originalProduced = manaProduced.toString();

        if (originalProduced.isEmpty()) {
            return originalProduced;
        }

        // Run triggers like Nissa, Wild Growth, etc.
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(hostCard);
        runParams.put(AbilityKey.Activator, ai); // assuming AI would only ever give itself mana
        runParams.put(AbilityKey.AbilityMana, saPayment);
        runParams.put(AbilityKey.Produced, originalProduced);
        for (Trigger tr : ai.getGame().getTriggerHandler().getActiveTrigger(forge.game.trigger.TriggerType.TapsForMana, runParams)) {
            SpellAbility trSA = tr.ensureAbility();
            if (trSA == null) {
                continue;
            }
            if (ApiType.Mana.equals(trSA.getApi())) {
                int pAmount = AbilityUtils.calculateAmount(trSA.getHostCard(), trSA.getParamOrDefault("Amount", "1"), trSA);
                String produced = trSA.getParam("Produced");
                if (produced.equals("Chosen")) {
                    produced = MagicColor.toShortString(trSA.getHostCard().getChosenColor());
                }
                manaProduced.append(" ").append(StringUtils.repeat(produced, " ", pAmount));
            } else if (ApiType.ManaReflected.equals(trSA.getApi())) {
                final String colorOrType = trSA.getParamOrDefault("ColorOrType", "Color");
                final String reflectProperty = trSA.getParam("ReflectProperty");
                if (reflectProperty.equals("Produced") && !originalProduced.isEmpty()) {
                    if (originalProduced.length() == 1) {
                        if (colorOrType.equals("Type") || !originalProduced.equals("C")) {
                            manaProduced.append(" ").append(originalProduced);
                        }
                    } else {
                        boolean found = false;
                        for (String s : originalProduced.split(" ")) {
                            if (colorOrType.equals("Type") || !s.equals("C")) {
                                found = true;
                                manaProduced.append(" ").append(s);
                                break;
                            }
                        }
                        if (!found) {
                            for (String s : originalProduced.split(" ")) {
                                if (colorOrType.equals("Type") || !s.equals("C")) {
                                    manaProduced.append(" ").append(s);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return manaProduced.toString();
    }

    // isManaSourceReserved returns true if sourceCard is reserved as a mana source for payment
    // for the future spell to be cast in another phase. However, if "sa" (the spell ability that is
    // being considered for casting) is high priority, then mana source reservation will be ignored.
    private boolean isManaSourceReserved() {
        if (sa == null) {
            return false;
        }
        if (!(ai.getController() instanceof PlayerControllerAi)) {
            return false;
        }

        // Mana reserved for spell synchronization
        Card sourceCard = sa.getHostCard();
        if (AiCardMemory.isRememberedCard(ai, sourceCard, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_NEXT_SPELL)) {
            return true;
        }

        PhaseType curPhase = ai.getGame().getPhaseHandler().getPhase();
        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        int chanceToReserve = aic.getIntProperty(AiProps.RESERVE_MANA_FOR_MAIN2_CHANCE);

        // For combat tricks, always obey mana reservation
        if (curPhase == PhaseType.COMBAT_DECLARE_BLOCKERS || curPhase == PhaseType.CLEANUP) {
            if (!(ai.getGame().getPhaseHandler().isPlayerTurn(ai))) {
                AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_ENEMY_DECLBLK);
                AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.CHOSEN_FOG_EFFECT);
            } else
                AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_DECLBLK);
        } else {
            if ((AiCardMemory.isRememberedCard(ai, sourceCard, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_DECLBLK)) ||
                    (AiCardMemory.isRememberedCard(ai, sourceCard, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_ENEMY_DECLBLK))) {
                // This mana source is held elsewhere for a combat trick.
                return true;
            }
        }

        // If it's a low priority spell (it's explicitly marked so elsewhere in the AI with a SVar), always
        // obey mana reservations for Main 2; otherwise, obey mana reservations depending on the "chance to reserve"
        // AI profile variable.
        if (sa.getSVar("LowPriorityAI").isEmpty()) {
            if (chanceToReserve == 0 || MyRandom.getRandom().nextInt(100) >= chanceToReserve) {
                return false;
            }
        }

        if (curPhase == PhaseType.MAIN2 || curPhase == PhaseType.CLEANUP) {
            AiCardMemory.clearMemorySet(ai, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2);
        } else {
            if (AiCardMemory.isRememberedCard(ai, sourceCard, AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2)) {
                // This mana source is held elsewhere for a Main Phase 2 spell.
                return true;
            }
        }

        return false;
    }
}
