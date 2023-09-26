package forge.ai;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import forge.ai.AiCardMemory.MemorySet;
import forge.ai.ability.AnimateAi;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
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
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class ComputerUtilMana {
    private final static boolean DEBUG_MANA_PAYMENT = false;

    public static boolean canPayManaCost(ManaCostBeingPaid cost, final SpellAbility sa, final Player ai, final boolean effect) {
        cost = new ManaCostBeingPaid(cost); //check copy of cost so it doesn't modify the exist cost being paid
        return payManaCost(cost, sa, ai, true, true, effect);
    }
    public static boolean canPayManaCost(final SpellAbility sa, final Player ai, final int extraMana, final boolean effect) {
        return payManaCost(sa, ai, true, extraMana, true, effect);
    }

    public static boolean payManaCost(ManaCostBeingPaid cost, final SpellAbility sa, final Player ai, final boolean effect) {
        return payManaCost(cost, sa, ai, false, true, effect);
    }
    public static boolean payManaCost(final Player ai, final SpellAbility sa, final boolean effect) {
        return payManaCost(sa, ai, false, 0, true, effect);
    }
    private static boolean payManaCost(final SpellAbility sa, final Player ai, final boolean test, final int extraMana, boolean checkPlayable, final boolean effect) {
        ManaCostBeingPaid cost = calculateManaCost(sa, test, extraMana);
        return payManaCost(cost, sa, ai, test, checkPlayable, effect);
    }

    /**
     * Return the number of colors used for payment for Converge
     */
    public static int getConvergeCount(final SpellAbility sa, final Player ai) {
        ManaCostBeingPaid cost = calculateManaCost(sa, true, 0);
        if (payManaCost(cost, sa, ai, true, true, false)) {
            return cost.getSunburst();
        }
        return 0;
    }

    // Does not check if mana sources can be used right now, just checks for potential chance.
    public static boolean hasEnoughManaSourcesToCast(final SpellAbility sa, final Player ai) {
        if (ai == null || sa == null)
            return false;
        sa.setActivatingPlayer(ai, true);
        return payManaCost(sa, ai, true, 0, false, false);
    }

    private static Integer scoreManaProducingCard(final Card card) {
        int score = 0;

        for (SpellAbility ability : card.getSpellAbilities()) {
            ability.setActivatingPlayer(card.getController(), true);
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

    private static void sortManaAbilities(final Multimap<ManaCostShard, SpellAbility> manaAbilityMap, final SpellAbility sa) {
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
        Collections.sort(orderedCards, new Comparator<Card>() {
            @Override
            public int compare(final Card card1, final Card card2) {
                return Integer.compare(manaCardMap.get(card1), manaCardMap.get(card2));
            }
        });

        if (DEBUG_MANA_PAYMENT) {
            System.out.print("Ordered Cards: " + orderedCards.size());
            for (Card card : orderedCards) {
                System.out.print(card.getName() + ", ");
            }
            System.out.println();
        }

        for (final ManaCostShard shard : manaAbilityMap.keySet()) {
            final Collection<SpellAbility> abilities = manaAbilityMap.get(shard);
            final List<SpellAbility> newAbilities = new ArrayList<>(abilities);

            if (DEBUG_MANA_PAYMENT) {
                System.out.println("Unsorted Abilities: " + newAbilities);
            }

            Collections.sort(newAbilities, new Comparator<SpellAbility>() {
                @Override
                public int compare(final SpellAbility ability1, final SpellAbility ability2) {
                    int preOrder = orderedCards.indexOf(ability1.getHostCard()) - orderedCards.indexOf(ability2.getHostCard());

                    if (preOrder != 0) {
                        return preOrder;
                    }

                    // Mana abilities on the same card
                    String shardMana = shard.toString().replaceAll("\\{", "").replaceAll("\\}", "");

                    boolean payWithAb1 = ability1.getManaPart().mana(ability1).contains(shardMana);
                    boolean payWithAb2 = ability2.getManaPart().mana(ability2).contains(shardMana);

                    if (payWithAb1 && !payWithAb2) {
                        return -1;
                    } else if (payWithAb2 && !payWithAb1) {
                        return 1;
                    }

                    return ability1.compareTo(ability2);
                }
            });

            if (DEBUG_MANA_PAYMENT) {
                System.out.println("Sorted Abilities: " + newAbilities);
            }

            manaAbilityMap.replaceValues(shard, newAbilities);

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

                    Collections.sort(prefSortedAbilities, new Comparator<SpellAbility>() {
                        @Override
                        public int compare(final SpellAbility ability1, final SpellAbility ability2) {
                            if (ability1.getManaPart().mana(ability1).contains(preferredShard))
                                return -1;
                            else if (ability2.getManaPart().mana(ability2).contains(preferredShard))
                                return 1;

                            return 0;
                        }
                    });
                    Collections.sort(otherSortedAbilities, new Comparator<SpellAbility>() {
                        @Override
                        public int compare(final SpellAbility ability1, final SpellAbility ability2) {
                            if (ability1.getManaPart().mana(ability1).contains(preferredShard))
                                return 1;
                            else if (ability2.getManaPart().mana(ability2).contains(preferredShard))
                                return -1;

                            return 0;
                        }
                    });

                    final List<SpellAbility> finalAbilities = new ArrayList<>();
                    for (int i = 0; i < preferredShardAmount && i < prefSortedAbilities.size(); i++) {
                        finalAbilities.add(prefSortedAbilities.get(i));
                    }
                    for (int i = 0; i < otherSortedAbilities.size(); i++) {
                        SpellAbility ab = otherSortedAbilities.get(i);
                        if (!finalAbilities.contains(ab))
                            finalAbilities.add(ab);
                    }

                    manaAbilityMap.replaceValues(shard, finalAbilities);
                }
            }
        }
    }

    public static SpellAbility chooseManaAbility(ManaCostBeingPaid cost, SpellAbility sa, Player ai, ManaCostShard toPay,
            Collection<SpellAbility> saList, boolean checkCosts) {
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
                    filteredList.sort(new Comparator<SpellAbility>() {
                        @Override
                        public int compare(SpellAbility ab1, SpellAbility ab2) {
                            return ab1.getHostCard() != null && ab1.getHostCard().isSnow()
                                    && ab2.getHostCard() != null && !ab2.getHostCard().isSnow() ? -1 : 1;
                        }
                    });
                    saList = filteredList;
                    break;
                case "Treasure":
                    // Try to spend only one Treasure if possible
                    filteredList.sort(new Comparator<SpellAbility>() {
                        @Override
                        public int compare(SpellAbility ab1, SpellAbility ab2) {
                            return ab1.getHostCard() != null && ab1.getHostCard().getType().hasSubtype("Treasure")
                                    && ab2.getHostCard() != null && !ab2.getHostCard().getType().hasSubtype("Treasure") ? -1 : 1;
                        }
                    });
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
                    filteredList.sort(new Comparator<SpellAbility>() {
                        @Override
                        public int compare(SpellAbility ab1, SpellAbility ab2) {
                            return ab1.getHostCard() != null && ab1.getHostCard().getType().hasSubtype("Treasure")
                                    && ab2.getHostCard() != null && !ab2.getHostCard().getType().hasSubtype("Treasure") ? -1 : 1;
                        }
                    });
                    saList = filteredList;
                    break;
                case "NotSameCard":
                    saList = Lists.newArrayList(Iterables.filter(filteredList, new Predicate<SpellAbility>() {
                        @Override
                        public boolean apply(final SpellAbility saPay) {
                            return !saPay.getHostCard().getName().equals(sa.getHostCard().getName());
                        }
                    }));
                    break;
                default:
                    break;
            }
        }

        for (final SpellAbility ma : saList) {
            if (ma.getHostCard() == saHost) {
                continue;
            }

            if (ma.getPayCosts().hasTapCost() && AiCardMemory.isRememberedCard(ai, ma.getHostCard(), MemorySet.PAYS_TAP_COST)) {
                continue;
            }

            if (!ComputerUtilCost.checkTapTypeCost(ai, ma.getPayCosts(), ma.getHostCard(), sa, new CardCollection())) {
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
                    if (CardLists.count(ai.getCardsIn(ZoneType.Battlefield), Predicates.and(CardPredicates.nameEquals(ma.getHostCard().getName()), CardPredicates.Presets.UNTAPPED)) > 1) {
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

            if (!canPayShardWithSpellAbility(toPay, ai, paymentChoice, sa, checkCosts, cost.getXManaCostPaidByColor())) {
                continue;
            }

            if (!ComputerUtilCost.checkForManaSacrificeCost(ai, ma.getPayCosts(), ma, ma.isTrigger())) {
                continue;
            }

            return paymentChoice;
        }
        return null;
    }

    public static String predictManaReplacement(SpellAbility saPayment, Player ai, ManaCostShard toPay) {
        Card hostCard = saPayment.getHostCard();
        Game game = hostCard.getGame();
        String manaProduced = toPay.isSnow() && hostCard.isSnow() ? "S" : GameActionUtil.generatedTotalMana(saPayment);

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(hostCard);
        repParams.put(AbilityKey.Mana, manaProduced);
        repParams.put(AbilityKey.Activator, ai);
        repParams.put(AbilityKey.AbilityMana, saPayment); // RootAbility

        // TODO Damping Sphere might replace later?

        // add flags to replacementEffects to filter better?
        List<ReplacementEffect> reList = game.getReplacementHandler().getReplacementList(ReplacementType.ProduceMana, repParams, ReplacementLayer.Other);

        List<SpellAbility> replaceMana = Lists.newArrayList();
        List<SpellAbility> replaceType = Lists.newArrayList();
        List<SpellAbility> replaceAmount = Lists.newArrayList(); // currently only multi

        // try to guess the color the mana gets replaced to
        for (ReplacementEffect re : reList) {
            SpellAbility o = re.getOverridingAbility();

            if (o == null || o.getApi() != ApiType.ReplaceMana) {
                continue;
            }

            // this one does replace the amount too
            if (o.hasParam("ReplaceMana")) {
                replaceMana.add(o);
            } else if (o.hasParam("ReplaceType") || o.hasParam("ReplaceColor")) {
                // this one replaces the color/type
                // check if this one can be replaced into wanted mana shard
                replaceType.add(o);
            } else if (o.hasParam("ReplaceAmount")) {
                replaceAmount.add(o);
            }
        }

        // it is better to apply these ones first
        if (!replaceMana.isEmpty()) {
            for (SpellAbility saMana : replaceMana) {
                // one of then has to Any
                // one of then has to C
                // one of then has to B
                String m = saMana.getParam("ReplaceMana");
                if ("Any".equals(m)) {
                    byte rs = MagicColor.GREEN;
                    for (byte c : MagicColor.WUBRGC) {
                        if (toPay.canBePaidWithManaOfColor(c)) {
                            rs = c;
                            break;
                        }
                    }
                    manaProduced = MagicColor.toShortString(rs);
                } else {
                    manaProduced = m;
                }
            }
        }

        // then apply this one
        if (!replaceType.isEmpty()) {
            for (SpellAbility saMana : replaceAmount) {
                Card card = saMana.getHostCard();
                if (saMana.hasParam("ReplaceType")) {
                    // replace color and colorless
                    String color = saMana.getParam("ReplaceType");
                    if ("Any".equals(color)) {
                        byte rs = MagicColor.GREEN;
                        for (byte c : MagicColor.WUBRGC) {
                            if (toPay.canBePaidWithManaOfColor(c)) {
                                rs = c;
                                break;
                            }
                        }
                        color = MagicColor.toShortString(rs);
                    }
                    for (byte c : MagicColor.WUBRGC) {
                        String s = MagicColor.toShortString(c);
                        manaProduced = manaProduced.replace(s, color);
                    }
                } else if (saMana.hasParam("ReplaceColor")) {
                    // replace color
                    String color = saMana.getParam("ReplaceColor");
                    if ("Chosen".equals(color)) {
                        if (card.hasChosenColor()) {
                            color = MagicColor.toShortString(card.getChosenColor());
                        }
                    }
                    if (saMana.hasParam("ReplaceOnly")) {
                        manaProduced = manaProduced.replace(saMana.getParam("ReplaceOnly"), color);
                    } else {
                        for (byte c : MagicColor.WUBRG) {
                            String s = MagicColor.toShortString(c);
                            manaProduced = manaProduced.replace(s, color);
                        }
                    }
                }
            }
        }

        // then multiply if able
        if (!replaceAmount.isEmpty()) {
            int totalAmount = 1;
            for (SpellAbility saMana : replaceAmount) {
                totalAmount *= Integer.valueOf(saMana.getParam("ReplaceAmount"));
            }
            manaProduced = StringUtils.repeat(manaProduced, " ", totalAmount);
        }

        return manaProduced;
    }

    public static String predictManafromSpellAbility(SpellAbility saPayment, Player ai, ManaCostShard toPay) {
        Card hostCard = saPayment.getHostCard();

        String manaProduced = predictManaReplacement(saPayment, ai, toPay);
        String originalProduced = manaProduced;

        if (originalProduced.isEmpty()) {
            return manaProduced;
        }

        // Run triggers like Nissa
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(hostCard);
        runParams.put(AbilityKey.Activator, ai); // assuming AI would only ever gives itself mana
        runParams.put(AbilityKey.AbilityMana, saPayment);
        runParams.put(AbilityKey.Produced, manaProduced);
        for (Trigger tr : ai.getGame().getTriggerHandler().getActiveTrigger(TriggerType.TapsForMana, runParams)) {
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
                manaProduced += " " + StringUtils.repeat(produced, " ", pAmount);
            } else if (ApiType.ManaReflected.equals(trSA.getApi())) {
                final String colorOrType = trSA.getParamOrDefault("ColorOrType", "Color");
                // currently Color or Type, Type is colors + colorless
                final String reflectProperty = trSA.getParam("ReflectProperty");

                if (reflectProperty.equals("Produced") && !originalProduced.isEmpty()) {
                    // check if a colorless shard can be paid from the trigger
                    if (toPay.equals(ManaCostShard.COLORLESS) && colorOrType.equals("Type") && originalProduced.contains("C")) {
                        manaProduced += " " + "C";
                    } else if (originalProduced.length() == 1) {
                        // if length is only one, and it either is equal C == Type
                        if (colorOrType.equals("Type") || !originalProduced.equals("C")) {
                            manaProduced += " " + originalProduced;
                        }
                    } else {
                        // should it look for other shards too?
                        boolean found = false;
                        for (String s : originalProduced.split(" ")) {
                            if (colorOrType.equals("Type") || !s.equals("C") && toPay.canBePaidWithManaOfColor(MagicColor.fromName(s))) {
                                found = true;
                                manaProduced += " " + s;
                                break;
                            }
                        }
                        // no good mana found? just add the first generated color
                        if (!found) {
                            for (String s : originalProduced.split(" ")) {
                                if (colorOrType.equals("Type") || !s.equals("C")) {
                                    manaProduced += " " + s;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return manaProduced;
    }

    public static CardCollection getManaSourcesToPayCost(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai) {
        CardCollection manaSources = new CardCollection();

        adjustManaCostToAvoidNegEffects(cost, sa.getHostCard(), ai);
        List<Mana> manaSpentToPay = new ArrayList<>();

        List<ManaCostShard> unpaidShards = cost.getUnpaidShards();
        Collections.sort(unpaidShards); // most difficult shards must come first
        for (ManaCostShard part : unpaidShards) {
            if (part != ManaCostShard.X) {
                if (cost.isPaid()) {
                    continue;
                }

                // get a mana of this type from floating, bail if none available
                final Mana mana = CostPayment.getMana(ai, part, sa, (byte) -1, cost.getXManaCostPaidByColor());
                if (mana != null) {
                    if (ai.getManaPool().tryPayCostWithMana(sa, cost, mana, false)) {
                        manaSpentToPay.add(mana);
                    }
                }
            }
        }

        if (cost.isPaid()) {
            // refund any mana taken from mana pool when test
            ManaPool.refundMana(manaSpentToPay, ai, sa);
            CostPayment.handleOfferings(sa, true, cost.isPaid());
            return manaSources;
        }

        // arrange all mana abilities by color produced.
        final ListMultimap<Integer, SpellAbility> manaAbilityMap = groupSourcesByManaColor(ai, true);
        if (manaAbilityMap.isEmpty()) {
            ManaPool.refundMana(manaSpentToPay, ai, sa);
            CostPayment.handleOfferings(sa, true, cost.isPaid());
            return manaSources;
        }

        // select which abilities may be used for each shard
        Multimap<ManaCostShard, SpellAbility> sourcesForShards = groupAndOrderToPayShards(ai, manaAbilityMap, cost);

        sortManaAbilities(sourcesForShards, sa);

        ManaCostShard toPay;
        // Loop over mana needed
        while (!cost.isPaid()) {
            toPay = getNextShardToPay(cost);

            Collection<SpellAbility> saList = sourcesForShards.get(toPay);
            if (saList == null) {
                break;
            }

            SpellAbility saPayment = chooseManaAbility(cost, sa, ai, toPay, saList, true);
            if (saPayment == null) {
                boolean lifeInsteadOfBlack = toPay.isBlack() && ai.hasKeyword("PayLifeInsteadOf:B");
                if ((!toPay.isPhyrexian() && !lifeInsteadOfBlack) || !ai.canPayLife(2, false, sa)) {
                    break; // cannot pay
                }

                if (toPay.isPhyrexian()) {
                    cost.payPhyrexian();
                } else if (lifeInsteadOfBlack) {
                    cost.decreaseShard(ManaCostShard.BLACK, 1);
                }

                continue;
            }

            manaSources.add(saPayment.getHostCard());
            setExpressColorChoice(sa, ai, cost, toPay, saPayment);

            String manaProduced = predictManafromSpellAbility(saPayment, ai, toPay);

            payMultipleMana(cost, manaProduced, ai);

            // remove from available lists
            Iterables.removeIf(sourcesForShards.values(), CardTraitPredicates.isHostCard(saPayment.getHostCard()));
        }

        CostPayment.handleOfferings(sa, true, cost.isPaid());
        ManaPool.refundMana(manaSpentToPay, ai, sa);

        return manaSources;
    }

    private static boolean payManaCost(final ManaCostBeingPaid cost, final SpellAbility sa, final Player ai, final boolean test, boolean checkPlayable, boolean effect) {
        AiCardMemory.clearMemorySet(ai, MemorySet.PAYS_TAP_COST);
        AiCardMemory.clearMemorySet(ai, MemorySet.PAYS_SAC_COST);
        adjustManaCostToAvoidNegEffects(cost, sa.getHostCard(), ai);

        List<Mana> manaSpentToPay = test ? new ArrayList<>() : sa.getPayingMana();
        List<SpellAbility> paymentList = Lists.newArrayList();
        final ManaPool manapool = ai.getManaPool();

        // Apply the color/type conversion matrix if necessary
        manapool.restoreColorReplacements();
        CardPlayOption mayPlay = sa.getMayPlayOption();
        if (!effect) {
            if (sa.isSpell() && mayPlay != null) {
                mayPlay.applyManaConvert(manapool);
            } else if (sa.isActivatedAbility() && sa.getGrantorStatic() != null && sa.getGrantorStatic().hasParam("ManaConversion")) {
                AbilityUtils.applyManaColorConversion(manapool, sa.getGrantorStatic().getParam("ManaConversion"));
            }
        }
        StaticAbilityManaConvert.manaConvert(manapool, ai, sa.getHostCard(), effect ? null : sa);

        if (ManaPool.payManaCostFromPool(cost, sa, ai, test, manaSpentToPay)) {
            return true;    // paid all from floating mana
        }

        boolean purePhyrexian = cost.containsOnlyPhyrexianMana();
        boolean hasConverge = sa.getHostCard().hasConverge();
        ListMultimap<ManaCostShard, SpellAbility> sourcesForShards = getSourcesForShards(cost, sa, ai, test,
                checkPlayable, hasConverge);

        int testEnergyPool = ai.getCounters(CounterEnumType.ENERGY);
        ManaCostShard toPay = null;
        List<SpellAbility> saExcludeList = new ArrayList<>();

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

            toPay = getNextShardToPay(cost);

            boolean lifeInsteadOfBlack = toPay.isBlack() && ai.hasKeyword("PayLifeInsteadOf:B");

            Collection<SpellAbility> saList = null;
            if (hasConverge &&
                    (toPay == ManaCostShard.GENERIC || toPay == ManaCostShard.X)) {
                final int unpaidColors = cost.getUnpaidColors() + cost.getColorsPaid() ^ ManaCostShard.COLORS_SUPERPOSITION;
                for (final byte b : ColorSet.fromMask(unpaidColors)) {
                    // try and pay other colors for converge
                    final ManaCostShard shard = ManaCostShard.valueOf(b);
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

            SpellAbility saPayment = saList.isEmpty() ? null : chooseManaAbility(cost, sa, ai, toPay, saList, checkPlayable || !test);

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

            if (saPayment == null) {
                if ((!toPay.isPhyrexian() && !lifeInsteadOfBlack) || !ai.canPayLife(2, false, sa)
                        || (ai.getLife() <= 2 && !ai.cantLoseForZeroOrLessLife())) {
                    break; // cannot pay
                }

                if (sa.hasParam("AIPhyrexianPayment")) {
                    if ("Never".equals(sa.getParam("AIPhyrexianPayment"))) {
                        break; // unwise to pay
                    } else if (sa.getParam("AIPhyrexianPayment").startsWith("OnFatalDamage.")) {
                        int dmg = Integer.parseInt(sa.getParam("AIPhyrexianPayment").substring(14));
                        if (!Iterables.any(ai.getOpponents(), PlayerPredicates.lifeLessOrEqualTo(dmg))) {
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

            setExpressColorChoice(sa, ai, cost, toPay, saPayment);

            if (saPayment.getPayCosts().hasTapCost()) {
                AiCardMemory.rememberCard(ai, saPayment.getHostCard(), MemorySet.PAYS_TAP_COST);
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
                payMultipleMana(cost, manaProduced, ai);

                // remove from available lists
                Iterables.removeIf(sourcesForShards.values(), CardTraitPredicates.isHostCard(saPayment.getHostCard()));
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
                    Iterables.removeIf(sourcesForShards.values(), CardTraitPredicates.isHostCard(saPayment.getHostCard()));
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
            ManaPool.refundMana(manaSpentToPay, ai, sa);
            if (test) {
                resetPayment(paymentList);
            } else {
                System.out.println("ComputerUtilMana: payManaCost() cost was not paid for " + sa.toString() + " (" +  sa.getHostCard().getName() + "). Didn't find what to pay for " + toPay);
            }
            return false;
        }

        if (test) {
            ManaPool.refundMana(manaSpentToPay, ai, sa);
            resetPayment(paymentList);
        }

        return true;
    }

    private static void resetPayment(List<SpellAbility> payments) {
        for (SpellAbility sa : payments) {
            sa.getManaPart().clearExpressChoice();
        }
    }

    /**
     * Creates a mapping between the required mana shards and the available spell abilities to pay for them
     */
    private static ListMultimap<ManaCostShard, SpellAbility> getSourcesForShards(final ManaCostBeingPaid cost,
            final SpellAbility sa, final Player ai, final boolean test, final boolean checkPlayable,
            final boolean hasConverge) {
        // arrange all mana abilities by color produced.
        final ListMultimap<Integer, SpellAbility> manaAbilityMap = groupSourcesByManaColor(ai, checkPlayable);
        if (manaAbilityMap.isEmpty()) {
            // no mana abilities, bailing out
            return null;
        }
        if (DEBUG_MANA_PAYMENT) {
            System.out.println("DEBUG_MANA_PAYMENT: manaAbilityMap = " + manaAbilityMap);
        }

        // select which abilities may be used for each shard
        ListMultimap<ManaCostShard, SpellAbility> sourcesForShards = groupAndOrderToPayShards(ai, manaAbilityMap, cost);
        if (hasConverge) {
            // add extra colors for paying converge
            final int unpaidColors = cost.getUnpaidColors() + cost.getColorsPaid() ^ ManaCostShard.COLORS_SUPERPOSITION;
            for (final byte b : ColorSet.fromMask(unpaidColors)) {
                final ManaCostShard shard = ManaCostShard.valueOf(b);
                if (!sourcesForShards.containsKey(shard)) {
                    if (ai.getManaPool().canPayForShardWithColor(shard, b)) {
                        for (SpellAbility saMana : manaAbilityMap.get((int)b)) {
                            sourcesForShards.get(shard).add(saMana);
                        }
                    }
                }
            }
        }

        sortManaAbilities(sourcesForShards, sa);
        if (DEBUG_MANA_PAYMENT) {
            System.out.println("DEBUG_MANA_PAYMENT: sourcesForShards = " + sourcesForShards);
        }
        return sourcesForShards;
    }

    private static void setExpressColorChoice(final SpellAbility sa, final Player ai, ManaCostBeingPaid cost,
            ManaCostShard toPay, SpellAbility saPayment) {
        AbilityManaPart m = saPayment.getManaPart();
        if (m.isComboMana()) {
            // usually we'll want to produce color that matches the shard
            ColorSet shared = ColorSet.fromMask(toPay.getColorMask()).getSharedColors(ColorSet.fromNames(m.getComboColors(saPayment).split(" ")));
            // but other effects might still lead to a more permissive payment
            if (!shared.isColorless()) {
                m.setExpressChoice(ColorSet.fromMask(shared.iterator().next()));
            }
            getComboManaChoice(ai, saPayment, sa, cost);
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

    private static boolean canPayShardWithSpellAbility(ManaCostShard toPay, Player ai, SpellAbility ma, SpellAbility sa, boolean checkCosts, Map<String, Integer> xManaCostPaidByColor) {
        final Card sourceCard = ma.getHostCard();

        if (isManaSourceReserved(ai, sourceCard, sa)) {
            return false;
        }

        if (toPay.isSnow() && !sourceCard.isSnow()) {
            return false;
        }

        AbilityManaPart m = ma.getManaPart();
        if (!m.meetsManaRestrictions(sa)) {
            return false;
        }

        if (ma.hasParam("ActivationLimit")) {
            if (ma.getActivationsThisTurn() >= AbilityUtils.calculateAmount(sourceCard, ma.getParam("ActivationLimit"), ma)) {
                return false;
            }
        }

        if (checkCosts) {
            // Check if AI can still play this mana ability
            ma.setActivatingPlayer(ai, true);
            // if the AI can't pay the additional costs skip the mana ability
            if (!CostPayment.canPayAdditionalCosts(ma.getPayCosts(), ma)) {
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

    // isManaSourceReserved returns true if sourceCard is reserved as a mana source for payment
    // for the future spell to be cast in another phase. However, if "sa" (the spell ability that is
    // being considered for casting) is high priority, then mana source reservation will be ignored.
    private static boolean isManaSourceReserved(Player ai, Card sourceCard, SpellAbility sa) {
        if (sa == null) {
            return false;
        }
        if (!(ai.getController() instanceof PlayerControllerAi)) {
            return false;
        }

        // Mana reserved for spell synchronization
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
        if (sa.getSVar("LowPriorityAI").equals("")) {
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

    private static ManaCostShard getNextShardToPay(ManaCostBeingPaid cost) {
        // mind the priorities
        // * Pay mono-colored first,curPhase == PhaseType.CLEANUP
        // * Pay 2/C with matching colors
        // * pay hybrids
        // * pay phyrexian, keep mana for colorless
        // * pay generic
        return cost.getShardToPayByPriority(cost.getDistinctShards(), ColorSet.ALL_COLORS.getColor());
    }

    private static void adjustManaCostToAvoidNegEffects(ManaCostBeingPaid cost, final Card card, Player ai) {
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
     * <p>
     * getComboManaChoice.
     * </p>
     *
     * @param manaAb
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param saRoot
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param cost
     *            a {@link forge.game.mana.ManaCostBeingPaid} object.
     */
    private static void getComboManaChoice(final Player ai, final SpellAbility manaAb, final SpellAbility saRoot, final ManaCostBeingPaid cost) {
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
                        payMultipleMana(testCost, choice, ai);
                        continue;
                    }
                }
                // check colors needed for cost
                if (!testCost.isPaid()) {
                    // Loop over combo colors
                    for (String color : comboColors) {
                        if (satisfiesColorChoice(abMana, choiceString, choice) && testCost.needsColor(ManaAtom.fromName(color), ai.getManaPool())) {
                            payMultipleMana(testCost, color, ai);
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

    /**
     * <p>
     * payMultipleMana.
     * </p>
     * @param mana
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    private static String payMultipleMana(ManaCostBeingPaid testCost, String mana, final Player p) {
        List<String> unused = new ArrayList<>(4);
        for (String manaPart : TextUtil.split(mana, ' ')) {
            if (StringUtils.isNumeric(manaPart)) {
                for (int i = Integer.parseInt(manaPart); i > 0; i--) {
                    boolean wasNeeded = testCost.ai_payMana("1", p.getManaPool());
                    if (!wasNeeded) {
                        unused.add(Integer.toString(i));
                        break;
                    }
                }
            } else {
                String color = MagicColor.toShortString(manaPart);
                boolean wasNeeded = testCost.ai_payMana(color, p.getManaPool());
                if (!wasNeeded) {
                    unused.add(color);
                }
            }
        }
        return unused.isEmpty() ? null : StringUtils.join(unused, ' ');
    }

    /**
     * Find all mana sources.
     * @param manaAbilityMap The map of SpellAbilities that produce mana.
     * @return Were all mana sources found?
     */
    private static ListMultimap<ManaCostShard, SpellAbility> groupAndOrderToPayShards(final Player ai, final ListMultimap<Integer, SpellAbility> manaAbilityMap,
            final ManaCostBeingPaid cost) {
        ListMultimap<ManaCostShard, SpellAbility> res = ArrayListMultimap.create();

        if (cost.getGenericManaAmount() > 0 && manaAbilityMap.containsKey(ManaAtom.GENERIC)) {
            res.putAll(ManaCostShard.GENERIC, manaAbilityMap.get(ManaAtom.GENERIC));
        }

        // loop over cost parts
        for (ManaCostShard shard : cost.getDistinctShards()) {
            if (DEBUG_MANA_PAYMENT) {
                System.out.println("DEBUG_MANA_PAYMENT: shard = " + shard);
            }
            if (shard == ManaCostShard.S) {
                res.putAll(shard, manaAbilityMap.get(ManaAtom.IS_SNOW));
                continue;
            }

            if (shard.isOr2Generic()) {
                Integer colorKey = (int) shard.getColorMask();
                if (manaAbilityMap.containsKey(colorKey))
                    res.putAll(shard, manaAbilityMap.get(colorKey));
                if (manaAbilityMap.containsKey(ManaAtom.GENERIC))
                    res.putAll(shard, manaAbilityMap.get(ManaAtom.GENERIC));
                continue;
            }

            if (shard == ManaCostShard.GENERIC) {
                continue;
            }

            for (Integer colorint : manaAbilityMap.keySet()) {
                // apply mana color change matrix here
                if (ai.getManaPool().canPayForShardWithColor(shard, colorint.byteValue())) {
                    for (SpellAbility sa : manaAbilityMap.get(colorint)) {
                        if (!res.get(shard).contains(sa)) {
                            res.get(shard).add(sa);
                        }
                    }
                }
            }
        }

        return res;
    }

    /**
     * Calculate the ManaCost for the given SpellAbility.
     * @param sa The SpellAbility to calculate for.
     * @param test test
     * @param extraMana extraMana
     * @return ManaCost
     */
    public static ManaCostBeingPaid calculateManaCost(final SpellAbility sa, final boolean test, final int extraMana) {
        Card card = sa.getHostCard();
        Zone castFromBackup = null;
        if (test && sa.isSpell() && !card.isInZone(ZoneType.Stack)) {
            castFromBackup = card.getCastFrom();
            card.setCastFrom(card.getZone() != null ? card.getZone() : null);
        }

        Cost payCosts = CostAdjustment.adjust(sa.getPayCosts(), sa);
        CostPartMana manapart = payCosts != null ? payCosts.getCostMana() : null;
        final ManaCost mana = payCosts != null ? ( manapart == null ? ManaCost.ZERO : manapart.getManaCostFor(sa) ) : ManaCost.NO_COST;

        ManaCostBeingPaid cost = new ManaCostBeingPaid(mana);

        // Tack xMana Payments into mana here if X is a set value
        if (cost.getXcounter() > 0 || extraMana > 0) {
            int manaToAdd = 0;
            int xCounter = cost.getXcounter();
            if (test && extraMana > 0) {
                final int multiplicator = Math.max(xCounter, 1);
                manaToAdd = extraMana * multiplicator;
            } else {
                manaToAdd = AbilityUtils.calculateAmount(card, "X", sa) * xCounter;
            }

            if (manaToAdd < 1 && !payCosts.getCostMana().canXbe0()) {
                // AI cannot really handle X costs properly but this keeps AI from violating rules
                manaToAdd = 1;
            }

            String xColor = sa.getXColor();
            if (xColor == null) {
                xColor = "1";
            }
            if (card.hasKeyword("Spend only colored mana on X. No more than one mana of each color may be spent this way.")) {
                xColor = "WUBRGX";
            }
            if (xCounter > 0) {
                cost.setXManaCostPaid(manaToAdd / xCounter, xColor);
            } else {
                cost.increaseShard(ManaCostShard.parseNonGeneric(xColor), manaToAdd);
            }

            if (!test) {
                sa.setXManaCostPaid(manaToAdd / xCounter);
            }
        }

        CostAdjustment.adjust(cost, sa, null, test);

        int timesMultikicked = card.getKickerMagnitude();
        if (timesMultikicked > 0 && sa.hasParam("Announce") && sa.getParam("Announce").startsWith("Multikicker")) {
            ManaCost mkCost = sa.getMultiKickerManaCost();
            for (int i = 0; i < timesMultikicked; i++) {
                cost.addManaCost(mkCost);
            }
            sa.setSVar("Multikicker", String.valueOf(timesMultikicked));
        }

        if ("NumTimes".equals(sa.getParam("Announce"))) { // e.g. the Adversary cycle
            ManaCost mkCost = sa.getPayCosts().getTotalMana();
            ManaCost mCost = ManaCost.ZERO;
            for (int i = 0; i < 10; i++) {
                mCost = ManaCost.combine(mCost, mkCost);
                ManaCostBeingPaid mcbp = new ManaCostBeingPaid(mCost);
                if (!canPayManaCost(mcbp, sa, sa.getActivatingPlayer(), true)) {
                    sa.getHostCard().setSVar("NumTimes", "Number$" + i);
                    break;
                }
            }
        }

        if (test && sa.isSpell()) {
            sa.getHostCard().setCastFrom(castFromBackup);
        }

        return cost;
    }

    // This method can be used to estimate the total amount of mana available to the player,
    // including the mana available in that player's mana pool
    public static int getAvailableManaEstimate(final Player p) {
        return getAvailableManaEstimate(p, true);
    }
    public static int getAvailableManaEstimate(final Player p, final boolean checkPlayable) {
        int availableMana = 0;

        final List<Card> srcs = CardLists.filter(p.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !c.getManaAbilities().isEmpty();
            }
        });

        int maxProduced = 0;
        int producedWithCost = 0;
        boolean hasSourcesWithNoManaCost = false;

        for (Card src : srcs) {
            maxProduced = 0;

            for (SpellAbility ma : src.getManaAbilities()) {
                ma.setActivatingPlayer(p, true);
                if (!checkPlayable || ma.canPlay()) {
                    int costsToActivate = ma.getPayCosts().getCostMana() != null ? ma.getPayCosts().getCostMana().convertAmount() : 0;
                    int producedMana = ma.getParamOrDefault("Produced", "").split(" ").length;
                    int producedAmount = AbilityUtils.calculateAmount(src, ma.getParamOrDefault("Amount", "1"), ma);

                    int producedTotal = producedMana * producedAmount - costsToActivate;

                    if (costsToActivate > 0) {
                        producedWithCost += producedTotal;
                    } else if (!hasSourcesWithNoManaCost) {
                        hasSourcesWithNoManaCost = true;
                    }

                    if (producedTotal > maxProduced) {
                        maxProduced = producedTotal;
                    }
                }
            }

            availableMana += maxProduced;
        }

        availableMana += p.getManaPool().totalMana();

        if (producedWithCost > 0 && !hasSourcesWithNoManaCost) {
            availableMana -= producedWithCost; // probably can't activate them, no other mana available
        }

        return availableMana;
    }

    //This method is currently used by AI to estimate available mana
    public static CardCollection getAvailableManaSources(final Player ai, final boolean checkPlayable) {
        final CardCollectionView list = CardCollection.combine(ai.getCardsIn(ZoneType.Battlefield), ai.getCardsIn(ZoneType.Hand));
        final List<Card> manaSources = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                for (final SpellAbility am : getAIPlayableMana(c)) {
                    am.setActivatingPlayer(ai, true);
                    if (!checkPlayable || (am.canPlay() && am.checkRestrictions(ai))) {
                        return true;
                    }
                }
                return false;
            }
        }); // CardListFilter

        final CardCollection sortedManaSources = new CardCollection();
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
        // 1. Use lands that can only produce colorless mana without
        // drawback/cost first
        // 2. Search for mana sources that have a certain number of abilities
        // 3. Use lands that produce any color many
        // 4. all other sources (creature, costs, drawback, etc.)
        for (Card card : manaSources) {
            // exclude creature sources that will tap as a part of an attack declaration
            if (card.isCreature()) {
                if (card.getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS, ai)) {
                    Combat combat = card.getGame().getCombat();
                    if (combat.getAttackers().indexOf(card) != -1 && !card.hasKeyword(Keyword.VIGILANCE)) {
                        continue;
                    }
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
                    // if the AI can't pay the additional costs skip the mana ability
                    m.setActivatingPlayer(ai, true);
                    if (!CostPayment.canPayAdditionalCosts(m.getPayCosts(), m)) {
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
                // We really shouldn't be hardcoding names here. ChkDrawback should just return true for them
                if (sub != null && !card.getName().equals("Pristine Talisman") && !card.getName().equals("Zhur-Taa Druid")) {
                    if (!SpellApiToAi.Converter.get(sub.getApi()).chkDrawbackWithSubs(ai, sub)) {
                        continue;
                    }
                    needsLimitedResources = true; // TODO: check for good drawbacks (gainLife)
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
        sortedManaSources.addAll(sortedManaSources.size(), colorlessManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), oneManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), twoManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), threeManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), fourManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), fiveManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), anyColorManaSources);
        //use better creatures later
        ComputerUtilCard.sortByEvaluateCreature(otherManaSources);
        Collections.reverse(otherManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), otherManaSources);
        // This should be things like sacrifice other stuff.
        ComputerUtilCard.sortByEvaluateCreature(useLastManaSources);
        Collections.reverse(useLastManaSources);
        sortedManaSources.addAll(sortedManaSources.size(), useLastManaSources);

        if (DEBUG_MANA_PAYMENT) {
            System.out.println("DEBUG_MANA_PAYMENT: sortedManaSources = " + sortedManaSources);
        }
        return sortedManaSources;
    }

    //This method is currently used by AI to estimate mana available
    private static ListMultimap<Integer, SpellAbility> groupSourcesByManaColor(final Player ai, boolean checkPlayable) {
        final ListMultimap<Integer, SpellAbility> manaMap = ArrayListMultimap.create();
        final Game game = ai.getGame();

        // Loop over all current available mana sources
        for (final Card sourceCard : getAvailableManaSources(ai, checkPlayable)) {
            if (DEBUG_MANA_PAYMENT) {
                System.out.println("DEBUG_MANA_PAYMENT: groupSourcesByManaColor sourceCard = " + sourceCard);
            }
            for (final SpellAbility m : getAIPlayableMana(sourceCard)) {
                if (DEBUG_MANA_PAYMENT) {
                    System.out.println("DEBUG_MANA_PAYMENT: groupSourcesByManaColor m = " + m);
                }
                m.setActivatingPlayer(ai, true);
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
                    if (!SpellApiToAi.Converter.get(sub.getApi()).chkDrawbackWithSubs(ai, sub)) {
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

    /**
     * <p>
     * determineLeftoverMana.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a int.
     * @since 1.0.15
     */
    public static int determineLeftoverMana(final SpellAbility sa, final Player player, final boolean effect) {
        int max = 99;
        if (sa.hasParam("XMaxLimit")) {
            max = Math.min(max, AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("XMaxLimit"), sa));
        }
        for (int i = 1; i <= max; i++) {
            if (!canPayManaCost(sa.getRootAbility(), player, i, effect)) {
                return i - 1;
            }
        }
        return max;
    }

    /**
     * <p>
     * determineLeftoverMana.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param shardColor
     *            a mana shard to specifically test for.
     * @return a int.
     * @since 1.5.59
     */
    public static int determineLeftoverMana(final SpellAbility sa, final Player player, final String shardColor, final boolean effect) {
        ManaCost origCost = sa.getRootAbility().getPayCosts().getTotalMana();

        String shardSurplus = shardColor;
        for (int i = 1; i < 100; i++) {
            ManaCost extra = new ManaCost(new ManaCostParser(shardSurplus));
            if (!canPayManaCost(new ManaCostBeingPaid(ManaCost.combine(origCost, extra)), sa, player, effect)) {
                return i - 1;
            }
            shardSurplus += " " + shardColor;
        }
        return 99;
    }

    // Returns basic mana abilities plus "reflected mana" abilities
    /**
     * <p>
     * getAIPlayableMana.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public static List<SpellAbility> getAIPlayableMana(Card c) {
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

    /**
     * Matches list of creatures to shards in mana cost for convoking.
     * @param cost cost of convoked ability
     * @param list creatures to be evaluated
     * @param improvise
     * @return map between creatures and shards to convoke
     */
    public static Map<Card, ManaCostShard> getConvokeOrImproviseFromList(final ManaCost cost, List<Card> list, boolean improvise) {
        final Map<Card, ManaCostShard> convoke = new HashMap<>();
        Card convoked = null;
        if (!improvise) {
            for (ManaCostShard toPay : cost) {
                for (Card c : list) {
                    final int mask = c.getColor().getColor() & toPay.getColorMask();
                    if (mask != 0) {
                        convoked = c;
                        convoke.put(c, toPay);
                        break;
                    }
                }
                if (convoked != null) {
                    list.remove(convoked);
                }
                convoked = null;
            }
        }
        for (int i = 0; i < list.size() && i < cost.getGenericCost(); i++) {
            convoke.put(list.get(i), ManaCostShard.GENERIC);
        }
        return convoke;
    }
}
