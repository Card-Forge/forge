package forge.ai;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import forge.StaticData;
import forge.ai.simulation.GameStateEvaluator;
import forge.card.mana.ManaCost;
import forge.game.card.*;
import forge.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.card.CardRules;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.MagicColor.Constant;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.cost.Cost;
import forge.game.cost.CostPayEnergy;
import forge.game.cost.CostRemoveCounter;
import forge.game.cost.CostUntap;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordCollection;
import forge.game.keyword.KeywordInterface;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementLayer;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;
import forge.game.trigger.Trigger;
import forge.game.zone.MagicStack;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;

public class ComputerUtilCard {
    public static Card getMostExpensivePermanentAI(final CardCollectionView list, final SpellAbility spell, final boolean targeted) {
        CardCollectionView all = list;
        if (targeted) {
            all = CardLists.filter(all, c -> c.canBeTargetedBy(spell));
        }
        return getMostExpensivePermanentAI(all);
    }

    /**
     * <p>
     * Sorts a List<Card> by "best" using the EvaluateCreature function.
     * the best creatures will be first in the list.
     * </p>
     *
     * @param list
     */
    public static void sortByEvaluateCreature(final CardCollection list) {
        list.sort(getCachedCreatureComparator().reversed());
    }

    /**
     * <p>
     * getBestArtifactAI.
     * </p>
     *
     * @param list
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestArtifactAI(final List<Card> list) {
        // get biggest Artifact
        return list.stream()
                .filter(CardPredicates.ARTIFACTS)
                .max(Comparator.comparing(Card::getCMC))
                .orElse(null);
    }

    /**
     * Returns the best Planeswalker from a given list
     *
     * @param list list of cards to evaluate
     * @return best Planeswalker
     */
    public static Card getBestPlaneswalkerAI(final List<Card> list) {
        // no AI logic, just return most expensive
        return list.stream()
                .filter(CardPredicates.PLANESWALKERS)
                .max(Comparator.comparing(Card::getCMC))
                .orElse(null);
    }

    /**
     * Returns the worst Planeswalker from a given list
     *
     * @param list list of cards to evaluate
     * @return best Planeswalker
     */
    public static Card getWorstPlaneswalkerAI(final List<Card> list) {
        // no AI logic, just return least expensive
        return list.stream()
                .filter(CardPredicates.PLANESWALKERS)
                .min(Comparator.comparing(Card::getCMC))
                .orElse(null);
    }

    public static Card getBestPlaneswalkerToDamage(final List<Card> pws) {
        Card bestTgt = null;

        // As of right now, ranks planeswalkers by their Current Loyalty * 10 + Big buff if close to "Ultimate"
        int bestScore = 0;
        for (Card pw : pws) {
            int curLoyalty = pw.getCounters(CounterEnumType.LOYALTY);
            int pwScore = curLoyalty * 10;

            for (SpellAbility sa : pw.getSpellAbilities()) {
                if (sa.hasParam("Ultimate")) {
                    Integer loyaltyCost = 0;
                    CostRemoveCounter remLoyalty = sa.getPayCosts().getCostPartByType(CostRemoveCounter.class);
                    if (remLoyalty != null) {
                        // if remLoyalty is null, generally there's an AddCounter<0/LOYALTY> cost, like for Gideon Jura.
                        loyaltyCost = remLoyalty.convertAmount();
                    }

                    if (loyaltyCost != null && loyaltyCost != 0 && loyaltyCost - curLoyalty <= 1) {
                        // Will ultimate soon
                        pwScore += 10000;
                    }

                    if (pwScore > bestScore) {
                        bestScore = pwScore;
                        bestTgt = pw;
                    }
                }
            }
        }

        return bestTgt;
    }

    public static Card getWorstPlaneswalkerToDamage(final List<Card> pws) {
        Card bestTgt = null;

        int bestScore = Integer.MAX_VALUE;
        for (Card pw : pws) {
            int curLoyalty = pw.getCounters(CounterEnumType.LOYALTY);

            if (curLoyalty < bestScore) {
                bestScore = curLoyalty;
                bestTgt = pw;
            }
        }

        return bestTgt;
    }

    // The AI doesn't really pick the best enchantment, just the most expensive.

    /**
     * <p>
     * getBestEnchantmentAI.
     * </p>
     *
     * @param list
     * @param spell    a {@link forge.game.card.Card} object.
     * @param targeted a boolean.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestEnchantmentAI(final List<Card> list, final SpellAbility spell, final boolean targeted) {
        Stream<Card> cardStream = list.stream().filter(CardPredicates.ENCHANTMENTS);
        if (targeted) {
            cardStream = cardStream.filter(c -> c.canBeTargetedBy(spell));
        }

        // get biggest Enchantment
        return cardStream.max(Comparator.comparing(Card::getCMC)).orElse(null);
    }

    /**
     * <p>
     * getBestLandAI.
     * </p>
     *
     * @param list
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestLandAI(final Iterable<Card> list) {
        final List<Card> land = CardLists.filter(list, CardPredicates.LANDS);
        if (land.isEmpty()) {
            return null;
        }

        // prefer to target non basic lands
        final List<Card> nbLand = CardLists.filter(land, CardPredicates.NONBASIC_LANDS);

        if (!nbLand.isEmpty()) {
            // TODO - Improve ranking various non-basic lands depending on context

            // Urza's Mine/Tower/Power Plant
            final CardCollectionView aiAvailable = nbLand.get(0).getController().getCardsIn(Arrays.asList(ZoneType.Battlefield, ZoneType.Hand));
            if (IterableUtil.any(list, CardPredicates.nameEquals("Urza's Mine"))) {
                if (CardLists.filter(aiAvailable, CardPredicates.nameEquals("Urza's Mine")).isEmpty()) {
                    return CardLists.filter(nbLand, CardPredicates.nameEquals("Urza's Mine")).getFirst();
                }
            }
            if (IterableUtil.any(list, CardPredicates.nameEquals("Urza's Tower"))) {
                if (CardLists.filter(aiAvailable, CardPredicates.nameEquals("Urza's Tower")).isEmpty()) {
                    return CardLists.filter(nbLand, CardPredicates.nameEquals("Urza's Tower")).getFirst();
                }
            }
            if (IterableUtil.any(list, CardPredicates.nameEquals("Urza's Power Plant"))) {
                if (CardLists.filter(aiAvailable, CardPredicates.nameEquals("Urza's Power Plant")).isEmpty()) {
                    return CardLists.filter(nbLand, CardPredicates.nameEquals("Urza's Power Plant")).getFirst();
                }
            }

            return Aggregates.random(nbLand);
        }

        // if no non-basic lands, target the least represented basic land type
        String sminBL = "";
        int iminBL = Integer.MAX_VALUE;
        int n = 0;
        for (String name : MagicColor.Constant.BASIC_LANDS) {
            n = CardLists.getType(land, name).size();
            if (n < iminBL && n > 0) {
                iminBL = n;
                sminBL = name;
            }
        }
        if (iminBL == Integer.MAX_VALUE) {
            // All basic lands have no basic land type. Just return something
            return land.stream().filter(CardPredicates.UNTAPPED).findFirst().orElse(land.get(0));
        }

        final List<Card> bLand = CardLists.getType(land, sminBL);

        return bLand.stream()
                .filter(CardPredicates.UNTAPPED)
                .findFirst()
                // TODO potentially risky if simulation mode currently able to reach this from triggers
                .orElseGet(() -> Aggregates.random(bLand)); // random tapped land of least represented type
    }

    /**
     * <p>
     * getWorstLand.
     * </p>
     *
     * @param lands
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getWorstLand(final List<Card> lands) {
        Card worstLand = null;
        int maxScore = Integer.MIN_VALUE;
        // first, check for tapped, basic lands
        for (Card tmp : lands) {
            int score = tmp.isTapped() ? 2 : 0;
            score += tmp.isBasicLand() ? 1 : 0;
            score -= tmp.isCreature() ? 4 : 0;
            for (Card aura : tmp.getEnchantedBy()) {
                if (aura.getController().isOpponentOf(tmp.getController())) {
                    score += 5;
                } else {
                    score -= 5;
                }
            }
            if (score == maxScore &&
                    CardLists.count(lands, CardPredicates.sharesNameWith(tmp)) > CardLists.count(lands, CardPredicates.sharesNameWith(worstLand))) {
                worstLand = tmp;
            }
            if (score > maxScore) {
                worstLand = tmp;
                maxScore = score;
            }
        }
        return worstLand;
    }

    public static Card getBestLandToAnimate(final Iterable<Card> lands) {
        Card land = null;
        int maxScore = Integer.MIN_VALUE;
        // first, check for tapped, basic lands
        for (Card tmp : lands) {
            int score = tmp.isTapped() ? 0 : 2;
            score += tmp.isBasicLand() ? 2 : 0;
            score -= tmp.isCreature() ? 4 : 0;
            score -= 5 * tmp.getEnchantedBy().size();

            if (score == maxScore &&
                    CardLists.count(lands, CardPredicates.sharesNameWith(tmp)) > CardLists.count(lands, CardPredicates.sharesNameWith(land))) {
                land = tmp;
            }
            if (score > maxScore) {
                land = tmp;
                maxScore = score;
            }
        }
        return land;
    }

    /**
     * <p>
     * getCheapestPermanentAI.
     * </p>
     *
     * @param all
     * @param spell    a {@link forge.game.card.Card} object.
     * @param targeted a boolean.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getCheapestPermanentAI(Iterable<Card> all, final SpellAbility spell, final boolean targeted) {
        if (targeted) {
            all = CardLists.filter(all, c -> c.canBeTargetedBy(spell));
        }
        if (Iterables.isEmpty(all)) {
            return null;
        }

        // get cheapest card:
        Card cheapest = null;

        for (Card c : all) {
            if (cheapest == null || c.getManaCost().getCMC() <= cheapest.getManaCost().getCMC()) {
                cheapest = c;
            }
        }

        return cheapest;
    }

    // returns null if list.size() == 0

    /**
     * <p>
     * getBestAI.
     * </p>
     *
     * @param list
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestAI(final Iterable<Card> list) {
        // Get Best will filter by appropriate getBest list if ALL of the list is of that type
        if (IterableUtil.all(list, CardPredicates.CREATURES)) {
            return getBestCreatureAI(list);
        }
        if (IterableUtil.all(list, CardPredicates.LANDS)) {
            return getBestLandAI(list);
        }
        // TODO - Once we get an EvaluatePermanent this should call getBestPermanent()
        return getMostExpensivePermanentAI(list);
    }

    /**
     * getBestCreatureAI.
     *
     * @param list the list
     * @return the card
     */
    public static Card getBestCreatureAI(final Iterable<Card> list) {
        if (Iterables.size(list) == 1) {
            return Iterables.get(list, 0);
        }
        return Aggregates.itemWithMax(IterableUtil.filter(list, CardPredicates.CREATURES), ComputerUtilCard.creatureEvaluator);
    }

    /**
     * getBestLandToPlayAI.
     *
     * @param list the list
     * @return the card
     */
    public static Card getBestLandToPlayAI(final Iterable<Card> list) {
        if (Iterables.size(list) == 1) {
            return Iterables.get(list, 0);
        }
        return Aggregates.itemWithMax(IterableUtil.filter(list, Card::hasPlayableLandFace), ComputerUtilCard.landEvaluator);
    }

    /**
     * <p>
     * getWorstCreatureAI.
     * </p>
     *
     * @param list
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getWorstCreatureAI(final Iterable<Card> list) {
        if (Iterables.size(list) == 1) {
            return Iterables.get(list, 0);
        }
        return Aggregates.itemWithMin(IterableUtil.filter(list, CardPredicates.CREATURES), ComputerUtilCard.creatureEvaluator);
    }

    // This selection rates tokens higher

    /**
     * <p>
     * getBestCreatureToBounceAI.
     * </p>
     *
     * @param list
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getBestCreatureToBounceAI(final Iterable<Card> list) {
        if (Iterables.size(list) == 1) {
            return Iterables.get(list, 0);
        }
        final int tokenBonus = 60;
        Card biggest = null;
        int biggestvalue = -1;

        for (Card card : CardLists.filter(list, CardPredicates.CREATURES)) {
            int newvalue = evaluateCreature(card);
            newvalue += card.isToken() ? tokenBonus : 0; // raise the value of tokens

            if (biggestvalue < newvalue) {
                biggest = card;
                biggestvalue = newvalue;
            }
        }
        return biggest;
    }

    // For ability of Oracle en-Vec, return the first card that are going to attack next turn
    public static Card getBestCreatureToAttackNextTurnAI(final Player aiPlayer, final Iterable<Card> list) {
        AiController aic = ((PlayerControllerAi) aiPlayer.getController()).getAi();
        for (final Card card : list) {
            if (aic.getPredictedCombatNextTurn().isAttacking(card)) {
                return card;
            }
        }
        return null;
    }

    /**
     * <p>
     * getWorstAI.
     * </p>
     *
     * @param list
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getWorstAI(final Iterable<Card> list) {
        return getWorstPermanentAI(list, false, false, false, false);
    }

    /**
     * <p>
     * getWorstPermanentAI.
     * </p>
     *
     * @param list
     * @param biasEnch     a boolean.
     * @param biasLand     a boolean.
     * @param biasArt      a boolean.
     * @param biasCreature a boolean.
     * @return a {@link forge.game.card.Card} object.
     */
    public static Card getWorstPermanentAI(final Iterable<Card> list, final boolean biasEnch, final boolean biasLand,
                                           final boolean biasArt, final boolean biasCreature) {
        if (Iterables.isEmpty(list)) {
            return null;
        }

        final boolean hasEnchantmants = IterableUtil.any(list, CardPredicates.ENCHANTMENTS);
        if (biasEnch && hasEnchantmants) {
            return getCheapestPermanentAI(CardLists.filter(list, CardPredicates.ENCHANTMENTS), null, false);
        }

        final boolean hasArtifacts = IterableUtil.any(list, CardPredicates.ARTIFACTS);
        if (biasArt && hasArtifacts) {
            return getCheapestPermanentAI(CardLists.filter(list, CardPredicates.ARTIFACTS), null, false);
        }

        if (biasLand && IterableUtil.any(list, CardPredicates.LANDS)) {
            return getWorstLand(CardLists.filter(list, CardPredicates.LANDS));
        }

        final boolean hasCreatures = IterableUtil.any(list, CardPredicates.CREATURES);
        if (biasCreature && hasCreatures) {
            return getWorstCreatureAI(CardLists.filter(list, CardPredicates.CREATURES));
        }

        List<Card> lands = CardLists.filter(list, CardPredicates.LANDS);
        if (lands.size() > 6 || lands.size() == Iterables.size(list)) {
            return getWorstLand(lands);
        }

        if (hasEnchantmants || hasArtifacts) {
            final List<Card> ae = CardLists.filter(list,
                    (CardPredicates.ARTIFACTS.or(CardPredicates.ENCHANTMENTS))
                    .and(card -> !card.hasSVar("DoNotDiscardIfAble"))
            );
            return getCheapestPermanentAI(ae, null, false);
        }

        if (hasCreatures) {
            return getWorstCreatureAI(CardLists.filter(list, CardPredicates.CREATURES));
        }

        // Planeswalkers fall through to here, lands will fall through if there aren't very many
        return getCheapestPermanentAI(list, null, false);
    }

    public static final Card getCheapestSpellAI(final Iterable<Card> list) {
        if (!Iterables.isEmpty(list)) {
            CardCollection cc = CardLists.filter(list, CardPredicates.INSTANTS_AND_SORCERIES);

            if (cc.isEmpty()) {
                return null;
            }

            cc.sort(CardLists.CmcComparatorInv);

            Card cheapest = cc.getLast();
            if (cheapest.hasSVar("DoNotDiscardIfAble")) {
                for (int i = cc.size() - 1; i >= 0; i--) {
                    if (!cc.get(i).hasSVar("DoNotDiscardIfAble")) {
                        cheapest = cc.get(i);
                        break;
                    }
                }
            }

            return cheapest;
        }

        return null;
    }

    public static Comparator<Card> getCachedCreatureComparator() {
        Map<Card, Integer> cache = new IdentityHashMap<>();
        return Comparator.comparing(c -> cache.computeIfAbsent(c, creatureEvaluator));
    }
    public static final Comparator<SpellAbility> EvaluateCreatureSpellComparator = (a, b) -> {
        // TODO ideally we could reuse the value from the previous pass with false
        return ComputerUtilAbility.saEvaluator.compareEvaluator(a, b, true);
    };

    private static final CreatureEvaluator creatureEvaluator = new CreatureEvaluator();
    private static final LandEvaluator landEvaluator = new LandEvaluator();

    /**
     * <p>
     * evaluateCreature.
     * </p>
     *
     * @param c a {@link forge.game.card.Card} object.
     * @return a int.
     */
    public static int evaluateCreature(final Card c) {
        return creatureEvaluator.evaluateCreature(c);
    }
    public static int evaluateCreature(final Card c, final boolean considerPT, final boolean considerCMC) {
        return creatureEvaluator.evaluateCreature(c, considerPT, considerCMC);
    }
    public static int evaluateCreature(final SpellAbility sa) {
        final Card host = sa.getHostCard();

        if (sa.getApi() != ApiType.PermanentCreature) {
            System.err.println("Warning: tried to evaluate a non-creature spell with evaluateCreature for card " + host + " via SA " + sa);
            return 0;
        }

        // switch to the needed card face
        CardStateName currentState = sa.getCardState() != null && host.getCurrentStateName() != sa.getCardStateName() && !host.isInPlay() ? host.getCurrentStateName() : null;
        if (currentState != null) {
            host.setState(sa.getCardStateName(), false);
        }

        int eval = evaluateCreature(host, true, false);

        if (currentState != null) {
            host.setState(currentState, false);
        }

        return eval;
    }

    public static int evaluatePermanentList(final CardCollectionView list) {
        int value = 0;
        for (int i = 0; i < list.size(); i++) {
            value += list.get(i).getCMC() + 1;
        }
        return value;
    }

    public static int evaluateCreatureList(final CardCollectionView list) {
        return Aggregates.sum(list, creatureEvaluator);
    }

    public static Map<String, Integer> evaluateCreatureListByName(final CardCollectionView list) {
        // Compute value for each possible target
        Map<String, Integer> values = Maps.newHashMap();
        for (Card c : list) {
            String name = c.getName();
            int val = evaluateCreature(c);
            if (values.containsKey(name)) {
                values.put(name, values.get(name) + val);
            } else {
                values.put(name, val);
            }
        }
        return values;
    }

    public static boolean doesCreatureAttackAI(final Player aiPlayer, final Card card) {
        AiController aic = ((PlayerControllerAi) aiPlayer.getController()).getAi();
        return aic.getPredictedCombat().isAttacking(card);
    }

    /**
     * Extension of doesCreatureAttackAI() for "virtual" creatures that do not actually exist on the battlefield yet
     * such as unanimated manlands.
     *
     * @param ai   controller of creature
     * @param card creature to be evaluated
     * @return creature will be attack
     */
    public static boolean doesSpecifiedCreatureAttackAI(final Player ai, final Card card) {
        AiAttackController aiAtk = new AiAttackController(ai, card);
        Combat combat = new Combat(ai);
        aiAtk.declareAttackers(combat);
        return combat.isAttacking(card);
    }

    /**
     * Create a mock combat where ai is being attacked and returns the list of likely blockers.
     *
     * @param ai       blocking player
     * @param blockers list of additional blockers to be considered
     * @return list of creatures assigned to block in the simulation
     */
    public static CardCollectionView getLikelyBlockers(final Player ai, final CardCollectionView blockers) {
        AiBlockController aiBlk = new AiBlockController(ai, false);
        final Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
        Combat combat = new Combat(opp);
        //Use actual attackers if available, else consider all possible attackers
        Combat currentCombat = ai.getGame().getCombat();
        if (currentCombat != null && currentCombat.getAttackingPlayer() != ai) {
            for (Card c : currentCombat.getAttackers()) {
                combat.addAttacker(c, ai);
            }
        } else {
            for (Card c : opp.getCreaturesInPlay()) {
                if (ComputerUtilCombat.canAttackNextTurn(c, ai)) {
                    combat.addAttacker(c, ai);
                }
            }
        }
        if (blockers == null || blockers.isEmpty()) {
            aiBlk.assignBlockersForCombat(combat);
        } else {
            aiBlk.assignAdditionalBlockers(combat, blockers);
        }
        return combat.getAllBlockers();
    }

    /**
     * Decide if a creature is going to be used as a blocker.
     *
     * @param ai      controller of creature
     * @param blocker creature to be evaluated
     * @return creature will be a blocker
     */
    public static boolean doesSpecifiedCreatureBlock(final Player ai, Card blocker) {
        return getLikelyBlockers(ai, new CardCollection(blocker)).contains(blocker);
    }

    /**
     * Check if an attacker can be blocked profitably (ie. kill attacker)
     *
     * @param ai       controller of attacking creature
     * @param attacker attacking creature to evaluate
     * @return attacker will die
     */
    public static boolean canBeBlockedProfitably(final Player ai, Card attacker, boolean checkingOther) {
        AiBlockController aiBlk = new AiBlockController(ai, checkingOther);
        Combat combat = new Combat(ai);
        // avoid removing original attacker
        attacker.setCombatLKI(null);
        combat.addAttacker(attacker, ai);
        final List<Card> attackers = Lists.newArrayList(attacker);
        aiBlk.assignBlockersGivenAttackers(combat, attackers);
        return ComputerUtilCombat.attackerWouldBeDestroyed(ai, attacker, combat);
    }

    public static boolean canBeKilledByRoyalAssassin(final Player ai, final Card card) {
        boolean wasTapped = card.isTapped();
        for (Player opp : ai.getOpponents()) {
            for (Card c : opp.getCardsIn(ZoneType.Battlefield)) {
                for (SpellAbility sa : c.getSpellAbilities()) {
                    if (sa.getApi() != ApiType.Destroy) {
                        continue;
                    }
                    if (!ComputerUtilCost.canPayCost(sa, opp, sa.isTrigger())) {
                        continue;
                    }
                    sa.setActivatingPlayer(opp);
                    if (sa.canTarget(card)) {
                        continue;
                    }
                    // check whether the ability can only target tapped creatures
                    card.setTapped(true);
                    if (!sa.canTarget(card)) {
                        card.setTapped(wasTapped);
                        continue;
                    }
                    card.setTapped(wasTapped);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * getMostExpensivePermanentAI.
     *
     * @param all the all
     * @return the card
     */
    public static Card getMostExpensivePermanentAI(final Iterable<Card> all) {
        Card biggest = null;

        int bigCMC = -1;
        for (final Card card : all) {
            // TODO when PlayAi can consider MDFC this should also look at the back face (if not on stack or battlefield)
            int curCMC = card.getCMC();

            // Add all cost of all auras with the same controller
            if (card.isEnchanted()) {
                final List<Card> auras = CardLists.filterControlledBy(card.getEnchantedBy(), card.getController());
                curCMC += Aggregates.sum(auras, Card::getCMC) + auras.size();
            }

            if (curCMC >= bigCMC) {
                bigCMC = curCMC;
                biggest = card;
            }
        }

        return biggest;
    }

    public static String getMostProminentCardName(final CardCollectionView list) {
        if (list.size() == 0) {
            return "";
        }

        final Map<String, Integer> map = Maps.newHashMap();

        for (final Card c : list) {
            final String name = c.getName();
            Integer currentCnt = map.get(name);
            map.put(name, currentCnt == null ? Integer.valueOf(1) : Integer.valueOf(1 + currentCnt));
        }

        int max = 0;
        String maxName = "";

        for (final Entry<String, Integer> entry : map.entrySet()) {
            final String type = entry.getKey();

            if (max < entry.getValue()) {
                max = entry.getValue();
                maxName = type;
            }
        }
        return maxName;
    }

    public static String getMostProminentType(final CardCollectionView list, final Collection<String> valid) {
        return getMostProminentType(list, valid, true);
    }
    public static String getMostProminentType(final CardCollectionView list, final Collection<String> valid, boolean includeTokens) {
        if (list.isEmpty()) {
            return "";
        }

        final Map<String, Integer> typesInDeck = Maps.newHashMap();

        for (final Card c : list) {
            if (!includeTokens && c.isToken()) {
                continue;
            }
            // Changeling are all creature types, they are not interesting for
            // counting creature types
            if (c.getType().hasAllCreatureTypes()) {
                continue;
            }
            // ignore cards that does enter the battlefield as clones
            boolean isClone = false;
            for (ReplacementEffect re : c.getReplacementEffects()) {
                if (re.getLayer() == ReplacementLayer.Copy) {
                    isClone = true;
                    break;
                }
            }
            if (isClone) {
                continue;
            }

            // Cards in hand and commanders are worth double, as they are more likely to be played.
            int weight = 1;
            if (c.isInZone(ZoneType.Hand) || c.isRealCommander()) {
                weight = 2;
            }

            Set<String> cardCreatureTypes = c.getType().getCreatureTypes();
            for (String type : cardCreatureTypes) {
                Integer count = typesInDeck.getOrDefault(type, 0);
                typesInDeck.put(type, count + weight);
            }

            //also take into account abilities that generate tokens
            if (includeTokens) {
                if (c.getRules() != null) {
                    for (String token : c.getRules().getTokens()) {
                        CardRules tokenCR = StaticData.instance().getAllTokens().getToken(token).getRules();
                        if (tokenCR == null)
                            continue;
                        for (String type : tokenCR.getType().getCreatureTypes()) {
                            Integer count = typesInDeck.getOrDefault(type, 0);
                            typesInDeck.put(type, count + 1);
                        }
                    }
                }

                // special rule for Fabricate and Servo
                if (c.hasKeyword(Keyword.FABRICATE)) {
                    Integer count = typesInDeck.getOrDefault("Servo", 0);
                    typesInDeck.put("Servo", count + weight);
                }
            }
        }

        int max = 0;
        String maxType = "";

        // Iterate through typesInDeck and consider only valid types
        for (final Entry<String, Integer> entry : typesInDeck.entrySet()) {
            final String type = entry.getKey();

            // consider the types that are in the valid list
            if ((valid.isEmpty() || valid.contains(type)) && max < entry.getValue()) {
                max = entry.getValue();
                maxType = type;
            }
        }

        return maxType;
    }

    public static String getMostProminentCardType(final CardCollectionView list, final Collection<String> valid) {
        if (list.isEmpty() || valid.isEmpty()) {
            return "";
        }

        final Map<String, Integer> typesInDeck = Maps.newHashMap();
        for (String type : valid) {
            typesInDeck.put(type, 0);
        }

        for (final Card c : list) {
            Iterable<CardType.CoreType> cardTypes = c.getType().getCoreTypes();
            for (CardType.CoreType type : cardTypes) {
                Integer count = typesInDeck.get(type.toString());
                if (count != null) {
                    typesInDeck.put(type.toString(), count + 1);
                }
            }
        }

        int max = 0;
        String maxType = "";

        for (final Entry<String, Integer> entry : typesInDeck.entrySet()) {
            final String type = entry.getKey();

            if (max < entry.getValue()) {
                max = entry.getValue();
                maxType = type;
            }
        }

        return maxType;
    }

    /**
     * <p>
     * getMostProminentColor.
     * </p>
     *
     * @param list
     * @return a {@link java.lang.String} object.
     */
    public static String getMostProminentColor(final Iterable<Card> list) {
        byte colors = CardFactoryUtil.getMostProminentColors(list);
        for (byte c : MagicColor.WUBRG) {
            if ((colors & c) != 0)
                return MagicColor.toLongString(c);
        }
        return MagicColor.Constant.WHITE; // no difference, there was no prominent color
    }

    public static String getMostProminentColor(final CardCollectionView list, final Iterable<String> restrictedToColors) {
        byte colors = CardFactoryUtil.getMostProminentColorsFromList(list, restrictedToColors);
        for (byte c : MagicColor.WUBRG) {
            if ((colors & c) != 0) {
                return MagicColor.toLongString(c);
            }
        }
        return Iterables.get(restrictedToColors, 0); // no difference, there was no prominent color
    }

    public static List<String> getColorByProminence(final List<Card> list) {
        int cntColors = MagicColor.WUBRG.length;
        final List<Pair<Byte, Integer>> map = new ArrayList<>();
        for (int i = 0; i < cntColors; i++) {
            map.add(MutablePair.of(MagicColor.WUBRG[i], 0));
        }

        for (final Card crd : list) {
            ColorSet color = crd.getColor();
            if (color.hasWhite()) map.get(0).setValue(map.get(0).getValue() + 1);
            if (color.hasBlue()) map.get(1).setValue(map.get(1).getValue() + 1);
            if (color.hasBlack()) map.get(2).setValue(map.get(2).getValue() + 1);
            if (color.hasRed()) map.get(3).setValue(map.get(3).getValue() + 1);
            if (color.hasGreen()) map.get(4).setValue(map.get(4).getValue() + 1);
        }

        map.sort(Comparator.<Pair<Byte, Integer>>comparingInt(Pair::getValue).reversed());

        // will this part be once dropped?
        List<String> result = new ArrayList<>(cntColors);
        for (Pair<Byte, Integer> idx : map) { // fetch color names in the same order
            result.add(MagicColor.toLongString(idx.getKey()));
        }
        // reverse to get indices for most prominent colors first.
        return result;
    }

    public static final Predicate<Deck> AI_KNOWS_HOW_TO_PLAY_ALL_CARDS = d -> {
        for (Entry<DeckSection, CardPool> cp : d) {
            for (Entry<PaperCard, Integer> e : cp.getValue()) {
                if (e.getKey().getRules().getAiHints().getRemAIDecks())
                    return false;
            }
        }
        return true;
    };

    public static List<String> chooseColor(SpellAbility sa, int min, int max, List<String> colorChoices) {
        List<String> chosen = new ArrayList<>();
        Player ai = sa.getActivatingPlayer();
        final Game game = ai.getGame();
        Player opp = ai.getStrongestOpponent();
        if (sa.hasParam("AILogic")) {
            final String logic = sa.getParam("AILogic");

            if (logic.equals("MostProminentInHumanDeck")) {
                chosen.add(getMostProminentColor(CardLists.filterControlledBy(game.getCardsInGame(), opp), colorChoices));
            } else if (logic.equals("MostProminentInComputerDeck")) {
                chosen.add(getMostProminentColor(CardLists.filterControlledBy(game.getCardsInGame(), ai), colorChoices));
            } else if (logic.equals("MostProminentDualInComputerDeck")) {
                List<String> prominence = getColorByProminence(CardLists.filterControlledBy(game.getCardsInGame(), ai));
                chosen.add(prominence.get(0));
                chosen.add(prominence.get(1));
            } else if (logic.equals("MostProminentInGame")) {
                chosen.add(getMostProminentColor(game.getCardsInGame(), colorChoices));
            } else if (logic.equals("MostProminentHumanCreatures")) {
                CardCollectionView list = opp.getCreaturesInPlay();
                if (list.isEmpty()) {
                    list = CardLists.filter(CardLists.filterControlledBy(game.getCardsInGame(), opp), CardPredicates.CREATURES);
                }
                chosen.add(getMostProminentColor(list, colorChoices));
            } else if (logic.equals("MostProminentComputerControls")) {
                chosen.add(getMostProminentColor(ai.getCardsIn(ZoneType.Battlefield), colorChoices));
            } else if (logic.equals("MostProminentHumanControls")) {
                chosen.add(getMostProminentColor(opp.getCardsIn(ZoneType.Battlefield), colorChoices));
            } else if (logic.equals("MostProminentPermanent")) {
                chosen.add(getMostProminentColor(game.getCardsIn(ZoneType.Battlefield), colorChoices));
            } else if (logic.equals("MostProminentAttackers") && game.getPhaseHandler().inCombat()) {
                chosen.add(getMostProminentColor(game.getCombat().getAttackers(), colorChoices));
            } else if (logic.equals("MostProminentInActivePlayerHand")) {
                chosen.add(getMostProminentColor(game.getPhaseHandler().getPlayerTurn().getCardsIn(ZoneType.Hand), colorChoices));
            } else if (logic.equals("MostProminentInComputerDeckButGreen")) {
                List<String> prominence = getColorByProminence(CardLists.filterControlledBy(game.getCardsInGame(), ai));
                if (prominence.get(0).equals(MagicColor.Constant.GREEN)) {
                    chosen.add(prominence.get(1));
                } else {
                    chosen.add(prominence.get(0));
                }
            } else if (logic.equals("MostExcessOpponentControls")) {
                int maxExcess = 0;
                String bestColor = Constant.GREEN;
                for (byte color : MagicColor.WUBRG) {
                    CardCollectionView ailist = ai.getColoredCardsInPlay(color);
                    CardCollectionView opplist = opp.getColoredCardsInPlay(color);

                    int excess = evaluatePermanentList(opplist) - evaluatePermanentList(ailist);
                    if (excess > maxExcess) {
                        maxExcess = excess;
                        bestColor = MagicColor.toLongString(color);
                    }
                }
                chosen.add(bestColor);
            } else if (logic.equals("MostProminentKeywordInComputerDeck")) {
                CardCollectionView list = ai.getAllCards();
                int m1 = 0;
                String chosenColor = MagicColor.Constant.WHITE;

                for (final String c : MagicColor.Constant.ONLY_COLORS) {
                    final int cmp = CardLists.filter(list, CardPredicates.containsKeyword(c)).size();
                    if (cmp > m1) {
                        m1 = cmp;
                        chosenColor = c;
                    }
                }
                chosen.add(chosenColor);
            } else if (logic.equals("HighestDevotionToColor")) {
                int curDevotion = 0;
                String chosenColor = MagicColor.Constant.WHITE;
                CardCollectionView hand = ai.getCardsIn(ZoneType.Hand);
                for (byte c : MagicColor.WUBRG) {
                    String devotionCode = "Count$Devotion." + MagicColor.toLongString(c);

                    int devotion = AbilityUtils.calculateAmount(sa.getHostCard(), devotionCode, sa);
                    if (devotion > curDevotion && hand.anyMatch(CardPredicates.isColor(c))) {
                        curDevotion = devotion;
                        chosenColor = MagicColor.toLongString(c);
                    }
                }
                chosen.add(chosenColor);
            }

        }
        if (chosen.isEmpty()) {
            //chosen.add(MagicColor.Constant.GREEN);
            chosen.add(getMostProminentColor(ai.getAllCards(), colorChoices));
        }
        return chosen;
    }

    public static boolean useRemovalNow(final SpellAbility sa, final Card c, final int dmg, ZoneType destination) {
        final Player ai = sa.getActivatingPlayer();
        final Game game = ai.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        final PhaseType phaseType = ph.getPhase();
        final Player opp = ph.getPlayerTurn().isOpponentOf(ai) ? ph.getPlayerTurn() : ai.getStrongestOpponent();

        final int costRemoval = sa.getHostCard().getCMC();
        final int costTarget = c.getCMC();

        if (!sa.isSpell()) {
            return true;
        }

        //Check for cards that profit from spells - for example Prowess or Threshold
        if (phaseType == PhaseType.MAIN1 && ComputerUtil.castSpellInMain1(ai, sa)) {
            return true;
        }

        //interrupt 1: Check whether a possible blocker will be killed for the AI to make a bigger attack
        if (ph.is(PhaseType.MAIN1) && ph.isPlayerTurn(ai) && c.isCreature()) {
            AiAttackController aiAtk = new AiAttackController(ai);
            final Combat combat = new Combat(ai);
            aiAtk.removeBlocker(c);
            aiAtk.declareAttackers(combat);
            if (!combat.getAttackers().isEmpty()) {
                AiAttackController aiAtk2 = new AiAttackController(ai);
                final Combat combat2 = new Combat(ai);
                aiAtk2.declareAttackers(combat2);
                if (combat.getAttackers().size() > combat2.getAttackers().size()) {
                    return true;
                }
            }
        }

        // interrupt 2: remove blocker to save my attacker
        if (ph.is(PhaseType.COMBAT_DECLARE_BLOCKERS) && !ph.isPlayerTurn(ai)) {
            Combat currCombat = game.getCombat();
            if (currCombat != null && !currCombat.getAllBlockers().isEmpty() && currCombat.getAllBlockers().contains(c)) {
                for (Card attacker : currCombat.getAttackersBlockedBy(c)) {
                    if (attacker.getShieldCount() == 0 && ComputerUtilCombat.attackerWouldBeDestroyed(ai, attacker, currCombat)) {
                        CardCollection blockers = currCombat.getBlockers(attacker);
                        sortByEvaluateCreature(blockers);
                        Combat combat = new Combat(ai);
                        combat.addAttacker(attacker, opp);
                        for (Card blocker : blockers) {
                            if (blocker == c) {
                                continue;
                            }
                            combat.addBlocker(attacker, blocker);
                        }
                        if (!ComputerUtilCombat.attackerWouldBeDestroyed(ai, attacker, combat)) {
                            return true;
                        }
                    }
                }
            }
        }

        // interrupt 3:  two for one = good
        if (c.isEnchanted()) {
            boolean myEnchants = false;
            for (Card enc : c.getEnchantedBy()) {
                if (enc.getOwner().equals(ai)) {
                    myEnchants = true;
                    break;
                }
            }
            if (!myEnchants) {
                return true;    //card advantage > tempo
            }
        }

        //interrupt 4: opponent pumping target (only works if the pump target is the chosen best target to begin with)
        final MagicStack stack = game.getStack();
        if (!stack.isEmpty()) {
            final SpellAbility topStack = stack.peekAbility();
            if (topStack.getActivatingPlayer().equals(opp) && c.equals(topStack.getTargetCard()) && topStack.isSpell()) {
                return true;
            }
        }

        //burn and curse spells
        float valueBurn = 0;
        if (dmg > 0) {
            if (sa.getDescription().contains("would die, exile it instead")) {
                destination = ZoneType.Exile;
            }
            valueBurn = 1.0f * c.getNetToughness() / dmg;
            valueBurn *= valueBurn;
            if (sa.getTargetRestrictions().canTgtPlayer()) {
                valueBurn /= 2;     //preserve option to burn to the face
            }
            if (valueBurn >= 0.8 && phaseType.isBefore(PhaseType.COMBAT_END)) {
                return true;
            }
        }

        //evaluate tempo gain
        float valueTempo = Math.max(0.1f * costTarget / costRemoval, valueBurn);
        if (c.isEquipped()) {
            valueTempo *= 2;
        }
        if (SpellAbilityAi.isSorcerySpeed(sa, ai)) {
            valueTempo *= 2;    //sorceries have less usage opportunities
        }
        if (!c.canBeDestroyed()) {
            valueTempo *= 2;    //deal with annoying things
        }
        if (!destination.equals(ZoneType.Graveyard) &&  //TODO:boat-load of "when blah dies" triggers
                c.hasKeyword(Keyword.PERSIST) || c.hasKeyword(Keyword.UNDYING) || c.hasKeyword(Keyword.MODULAR)) {
            valueTempo *= 2;
        }
        if (destination.equals(ZoneType.Hand) && !c.isToken()) {
            valueTempo /= 2;    //bouncing non-tokens for tempo is less valuable
        }
        if (c.isLand()) {
            valueTempo += 0.5f / opp.getLandsInPlay().size();   //set back opponent's mana
            if ("Land".equals(sa.getParam("ValidTgts")) && ph.getPhase().isAfter(PhaseType.COMBAT_END)) {
                valueTempo += 0.5; // especially when nothing else can be targeted
            }
        }
        if (!ph.isPlayerTurn(ai) && ph.getPhase().equals(PhaseType.END_OF_TURN)) {
            valueTempo *= 2;    //prefer to cast at opponent EOT
        }
        if (valueTempo >= 0.8 && ph.getPhase().isBefore(PhaseType.COMBAT_END)) {
            return true;
        }

        //evaluate threat of targeted card
        float threat = 0;
        if (c.isCreature()) {
            // the base value for evaluate creature is 100
            threat += (-1 + 1.0f * evaluateCreature(c) / 100) / costRemoval;
            if (ai.getLife() > 0 && ComputerUtilCombat.canAttackNextTurn(c)) {
                Combat combat = game.getCombat();
                threat += 1.0f * ComputerUtilCombat.damageIfUnblocked(c, ai, combat, true) / ai.getLife();
                //TODO:add threat from triggers and other abilities (ie. Master of Cruelties)
            }
            if (ph.isPlayerTurn(ai) && phaseType.isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                threat *= 0.1f;
            }
            if (!ph.isPlayerTurn(ai) &&
                    (phaseType.isBefore(PhaseType.COMBAT_BEGIN) || phaseType.isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS))) {
                threat *= 0.1f;
            }
        } else if (c.isPlaneswalker()) {
            threat = 1;
        } else if (AiProfileUtil.getBoolProperty(ai, AiProps.ACTIVELY_DESTROY_ARTS_AND_NONAURA_ENCHS) && ((c.isArtifact() && !c.isCreature()) || (c.isEnchantment() && !c.isAura()))) {
            // non-creature artifacts and global enchantments with suspicious intrinsic abilities
            boolean priority = false;
            if (c.getOwner().isOpponentOf(ai) && c.getController().isOpponentOf(ai)) {
                // if this thing is both owned and controlled by an opponent and it has a continuous ability,
                // assume it either benefits the player or disrupts the opponent
                for (final StaticAbility stAb : c.getStaticAbilities()) {
                    if (stAb.checkMode(StaticAbilityMode.Continuous) && stAb.isIntrinsic()) {
                        priority = true;
                        break;
                    }
                }
                if (!priority) {
                    for (final Trigger t : c.getTriggers()) {
                        if (t.isIntrinsic()) {
                            // has a triggered ability, could be benefitting the opponent or disrupting the AI
                            priority = true;
                            break;
                        }
                    }
                }
                // if this thing has AILogic set to "Curse", it's probably meant as some form of disruption
                if (!priority) {
                    for (final String value : c.getSVars().values()) {
                        if (value.contains("AILogic$ Curse")) {
                            // this is a curse ability, so prioritize its removal
                            priority = true;
                            break;
                        }
                    }
                }
                // if it's a priority object, set its threat level to high
                if (priority) {
                    threat = 1.0f;
                }
            }
        } else {
            for (final StaticAbility stAb : c.getStaticAbilities()) {
                //continuous buffs
                if (stAb.checkMode(StaticAbilityMode.Continuous) && "Creature.YouCtrl".equals(stAb.getParam("Affected"))) {
                    int bonusPT = 0;
                    if (stAb.hasParam("AddPower")) {
                        bonusPT += AbilityUtils.calculateAmount(c, stAb.getParam("AddPower"), stAb);
                    }
                    if (stAb.hasParam("AddToughness")) {
                        bonusPT += AbilityUtils.calculateAmount(c, stAb.getParam("AddPower"), stAb);
                    }
                    String kws = stAb.getParam("AddKeyword");
                    if (kws != null) {
                        bonusPT += 4 * (1 + StringUtils.countMatches(kws, "&")); //treat each added keyword as a +2/+2 for now
                    }
                    if (bonusPT > 0) {
                        threat = bonusPT * (1 + opp.getCreaturesInPlay().size()) / 10.0f;
                    }
                }
            }
            //TODO:add threat from triggers and other abilities (ie. Bident of Thassa)
        }
        if (!c.getManaAbilities().isEmpty()) {
            threat += 0.5f * costTarget / opp.getLandsInPlay().size();   //set back opponent's mana
        }

        final float valueNow = Math.max(valueTempo, threat);
        if (valueNow < 0.2) { //hard floor to reduce ridiculous odds for instants over time
            return false;
        }
        final float chance = MyRandom.getRandom().nextFloat();
        return chance < valueNow;
    }

    /**
     * Decides if the "pump" is worthwhile
     *
     * @param ai        casting player
     * @param sa        Pump* or CounterPut*
     * @param c         target of sa
     * @param toughness +T
     * @param power     +P
     * @param keywords  additional keywords from sa (only for Pump)
     * @return
     */
    public static boolean shouldPumpCard(final Player ai, final SpellAbility sa, final Card c, final int toughness,
                                         final int power, final List<String> keywords) {
        return shouldPumpCard(ai, sa, c, toughness, power, keywords, false);
    }
    public static boolean shouldPumpCard(final Player ai, final SpellAbility sa, final Card c, final int toughness,
                                         final int power, final List<String> keywords, boolean immediately) {
        final Game game = ai.getGame();
        final PhaseHandler phase = game.getPhaseHandler();
        final Combat combat = phase.getCombat();
        final boolean main1Preferred = "Main1IfAble".equals(sa.getParam("AILogic")) && phase.is(PhaseType.MAIN1, ai);
        final boolean isBerserk = "Berserk".equals(sa.getParam("AILogic"));
        final boolean loseCardAtEOT = "Sacrifice".equals(sa.getParam("AtEOT")) || "Exile".equals(sa.getParam("AtEOT"))
                || "Destroy".equals(sa.getParam("AtEOT")) || "ExileCombat".equals(sa.getParam("AtEOT"));

        boolean combatTrick = false;
        boolean holdCombatTricks = false;
        int chanceToHoldCombatTricks = -1;
        boolean simAI = false;

        if (ai.getController().isAI()) {
            AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
            simAI = aic.usesSimulation();
            if (!simAI) {
                holdCombatTricks = aic.getBoolProperty(AiProps.TRY_TO_HOLD_COMBAT_TRICKS_UNTIL_BLOCK);
                chanceToHoldCombatTricks = aic.getIntProperty(AiProps.CHANCE_TO_HOLD_COMBAT_TRICKS_UNTIL_BLOCK);
            }
        }

        if (!c.canBeTargetedBy(sa)) {
            return false;
        }

        if (c.getNetToughness() + toughness <= 0) {
            return false;
        }

        if (sa.getHostCard().equals(c) && ComputerUtilCost.isSacrificeSelfCost(sa.getPayCosts())) {
            return false;
        }

        /* -- currently disabled until better conditions are devised and the spell prediction is made smarter --
        // Determine if some mana sources need to be held for the future spell to cast in Main 2 before determining whether to pump.
        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        if (aic.getCardMemory().isMemorySetEmpty(AiCardMemory.MemorySet.HELD_MANA_SOURCES_FOR_MAIN2)) {
            // only hold mana sources once
            SpellAbility futureSpell = aic.predictSpellToCastInMain2(ApiType.Pump);
            if (futureSpell != null && futureSpell.getHostCard() != null) {
                aic.reserveManaSources(futureSpell);
            }
        }
        */

        // will the creature attack (only relevant for sorcery speed)?
        if (phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                && phase.isPlayerTurn(ai)
                && (SpellAbilityAi.isSorcerySpeed(sa, ai) || main1Preferred)
                && power > 0
                && doesCreatureAttackAI(ai, c)) {
            return true;
        }

        // buff attacker/blocker using triggered pump (unless it's lethal and we don't want to be reckless)
        if (immediately && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS) && !loseCardAtEOT) {
            if (phase.isPlayerTurn(ai)) {
                if (CombatUtil.canAttack(c) || (phase.inCombat() && c.isAttacking())) {
                    return true;
                }
            } else {
                if (CombatUtil.canBlock(c)) {
                    return true;
                }
            }
        }

        if (keywords.contains("Banding") && !c.hasKeyword(Keyword.BANDING)) {
            if (phase.is(PhaseType.COMBAT_BEGIN) && phase.isPlayerTurn(ai) && !ComputerUtilCard.doesCreatureAttackAI(ai, c)) {
                // will this card participate in an attacking band?
                Card bandingCard = getPumpedCreature(ai, sa, c, toughness, power, keywords);
                // TODO: It may be possible to use AiController.getPredictedCombat here, but that makes it difficult to
                // use reinforceWithBanding through the attack controller, especially with the extra card parameter in mind
                AiAttackController aiAtk = new AiAttackController(ai);
                Combat predicted = new Combat(ai);
                aiAtk.declareAttackers(predicted);
                aiAtk.reinforceWithBanding(predicted, bandingCard);
                if (predicted.isAttacking(bandingCard) && predicted.getBandOfAttacker(bandingCard).getAttackers().size() > 1) {
                    return true;
                }
            } else if (phase.is(PhaseType.COMBAT_DECLARE_BLOCKERS) && combat != null) {
                // does this card block a Trample card or participate in a multi block?
                for (Card atk : combat.getAttackers()) {
                    if (atk.getController().isOpponentOf(ai)) {
                        CardCollection blockers = combat.getBlockers(atk);
                        boolean hasBanding = false;
                        for (Card blocker : blockers) {
                            if (blocker.hasKeyword(Keyword.BANDING)) {
                                hasBanding = true;
                                break;
                            }
                        }
                        if (!hasBanding && ((blockers.contains(c) && blockers.size() > 1) || atk.hasKeyword(Keyword.TRAMPLE))) {
                            return true;
                        }
                    }
                }
            }
        }

        final Player opp = ai.getWeakestOpponent();
        Card pumped = getPumpedCreature(ai, sa, c, toughness, power, keywords);
        List<Card> oppCreatures = opp.getCreaturesInPlay();
        float chance = 0;

        //create and buff attackers
        if (phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS) && phase.isPlayerTurn(ai) && opp.getLife() > 0) {
            //1. become attacker for whatever reason
            if (!doesCreatureAttackAI(ai, c) && doesSpecifiedCreatureAttackAI(ai, pumped)) {
                float threat = 1.0f * ComputerUtilCombat.damageIfUnblocked(pumped, opp, combat, true) / opp.getLife();
                if (oppCreatures.stream().noneMatch(CardPredicates.possibleBlockers(pumped))) {
                    threat *= 2;
                }
                if (c.getNetPower() == 0 && c == sa.getHostCard() && power > 0) {
                    threat *= 4; //over-value self +attack for 0 power creatures which may be pumped further after attacking 
                }
                chance += threat;

                // -- Hold combat trick (the AI will try to delay the pump until Declare Blockers) --
                // Enable combat trick mode only in case it's a pure buff spell in hand with no keywords or with Trample,
                // First Strike, or Double Strike, otherwise the AI is unlikely to cast it or it's too late to
                // cast it during Declare Blockers, thus ruining its attacker
                if (holdCombatTricks && sa.getApi() == ApiType.Pump
                        && sa.hasParam("NumAtt") && sa.getHostCard() != null
                        && sa.getHostCard().isInZone(ZoneType.Hand)
                        && c.getNetPower() > 0 // too obvious if attacking with a 0-power creature
                        && sa.getHostCard().isInstant() // only do it for instant speed spells in hand
                        && ComputerUtilMana.hasEnoughManaSourcesToCast(sa, ai)) {
                    combatTrick = true;

                    for (String kw : keywords) {
                        if (!kw.equals("Trample") && !kw.equals("First Strike") && !kw.equals("Double Strike")) {
                            combatTrick = false;
                            break;
                        }
                    }
                }
            }

            //2. grant haste
            if (keywords.contains("Haste") && c.hasSickness() && !c.isTapped()) {
                double nonCombatChance = 0.0f;
                double combatChance = 0.0f;
                // non-combat Haste: has an activated ability with tap cost
                if (c.isAbilitySick()) {
                    for (SpellAbility ab : c.getSpellAbilities()) {
                        Cost abCost = ab.getPayCosts();
                        if (abCost != null && (abCost.hasTapCost() || abCost.hasSpecificCostType(CostUntap.class))
                                && (!abCost.hasManaCost() || ComputerUtilMana.canPayManaCost(ab, ai, sa.getPayCosts().getTotalMana().getCMC(), false))) {
                            nonCombatChance += 0.5f;
                            break;
                        }
                    }
                }
                // combat Haste: only grant it if the creature will attack
                if (doesSpecifiedCreatureAttackAI(ai, pumped)) {
                    combatChance += 0.5f + (0.5f * ComputerUtilCombat.damageIfUnblocked(pumped, opp, combat, true) / opp.getLife());
                }
                chance += nonCombatChance + combatChance;
            }

            //3. grant evasive
            if (oppCreatures.stream().anyMatch(CardPredicates.possibleBlockers(c))) {
                if (oppCreatures.stream().noneMatch(CardPredicates.possibleBlockers(pumped))
                        && doesSpecifiedCreatureAttackAI(ai, pumped)) {
                    chance += 0.5f * ComputerUtilCombat.damageIfUnblocked(pumped, opp, combat, true) / opp.getLife();
                }
            }
        }

        //combat trickery
        if (phase.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            //clunky code because ComputerUtilCombat.combatantWouldBeDestroyed() does not work for this sort of artificial combat
            Combat pumpedCombat = new Combat(phase.isPlayerTurn(ai) ? ai : opp);
            List<Card> opposing = null;
            boolean pumpedWillDie = false;
            final boolean isAttacking = combat.isAttacking(c);

            if ((isBerserk && isAttacking) || loseCardAtEOT) {
                pumpedWillDie = true;
            }

            if (isAttacking) {
                pumpedCombat.addAttacker(pumped, opp);
                opposing = combat.getBlockers(c);
                for (Card b : opposing) {
                    pumpedCombat.addBlocker(pumped, b);
                }
                if (ComputerUtilCombat.attackerWouldBeDestroyed(ai, pumped, pumpedCombat)) {
                    pumpedWillDie = true;
                }
            } else {
                opposing = combat.getAttackersBlockedBy(c);
                for (Card a : opposing) {
                    pumpedCombat.addAttacker(a, ai);
                    pumpedCombat.addBlocker(a, pumped);
                }
                if (ComputerUtilCombat.blockerWouldBeDestroyed(ai, pumped, pumpedCombat)) {
                    pumpedWillDie = true;
                }
            }

            //1. save combatant
            if (ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat) && !pumpedWillDie
                    && !c.hasKeyword(Keyword.INDESTRUCTIBLE)) {
                // hack because attackerWouldBeDestroyed()
                // does not check for Indestructible when computing lethal damage
                return true;
            }

            //2. kill combatant
            boolean survivor = false;
            for (Card o : opposing) {
                if (!ComputerUtilCombat.combatantWouldBeDestroyed(opp, o, combat)) {
                    survivor = true;
                    break;
                }
            }
            if (survivor) {
                for (Card o : opposing) {
                    if (!ComputerUtilCombat.combatantWouldBeDestroyed(opp, o, combat)
                            && !(o.hasSVar("SacMe") && Integer.parseInt(o.getSVar("SacMe")) > 2)) {
                        if (isAttacking) {
                            if (ComputerUtilCombat.blockerWouldBeDestroyed(opp, o, pumpedCombat)) {
                                return true;
                            }
                        } else {
                            if (ComputerUtilCombat.attackerWouldBeDestroyed(opp, o, pumpedCombat)) {
                                return true;
                            }
                        }
                    }
                }
            }

            //3. buff attacker
            if (combat.isAttacking(c) && opp.getLife() > 0) {
                int dmg = ComputerUtilCombat.damageIfUnblocked(c, opp, combat, true);
                int pumpedDmg = ComputerUtilCombat.damageIfUnblocked(pumped, opp, pumpedCombat, true);
                int poisonOrig = ComputerUtilCombat.poisonIfUnblocked(c, ai);
                int poisonPumped = ComputerUtilCombat.poisonIfUnblocked(pumped, ai);

                // predict Infect
                if (pumpedDmg == 0 && c.hasKeyword(Keyword.INFECT)) {
                    if (poisonPumped > poisonOrig) {
                        pumpedDmg = poisonPumped;
                    }
                }

                if (combat.isBlocked(c)) {
                    if (!c.hasKeyword(Keyword.TRAMPLE)) {
                        dmg = 0;
                    }
                    if (c.hasKeyword(Keyword.TRAMPLE) || keywords.contains("Trample")) {
                        for (Card b : combat.getBlockers(c)) {
                            pumpedDmg -= ComputerUtilCombat.getDamageToKill(b, false);
                        }
                    } else {
                        pumpedDmg = 0;
                    }
                }
                if (pumpedDmg > dmg) {
                    if ((!c.hasKeyword(Keyword.INFECT) && pumpedDmg >= opp.getLife())
                            || (c.hasKeyword(Keyword.INFECT) && opp.canReceiveCounters(CounterEnumType.POISON) && pumpedDmg >= opp.getPoisonCounters())
                            || ("PumpForTrample".equals(sa.getParam("AILogic")))) {
                        return true;
                    }

                    // try to determine if pumping a creature for more power will give lethal on board
                    // considering all unblocked creatures after the blockers are already declared
                    if (phase.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                        int totalPowerUnblocked = 0;
                        for (Card atk : combat.getAttackers()) {
                            if (combat.isBlocked(atk) && !atk.hasKeyword(Keyword.TRAMPLE)) {
                                continue;
                            }
                            if (atk == c) {
                                totalPowerUnblocked += pumpedDmg; // this accounts for Trample by now
                            } else {
                                totalPowerUnblocked += ComputerUtilCombat.damageIfUnblocked(atk, opp, combat, true);
                                if (combat.isBlocked(atk)) {
                                    // consider Trample damage properly for a blocked creature
                                    for (Card blk : combat.getBlockers(atk)) {
                                        totalPowerUnblocked -= ComputerUtilCombat.getDamageToKill(blk, false);
                                    }
                                }
                            }
                        }
                        if (totalPowerUnblocked >= opp.getLife()) {
                            return true;
                        } else if (totalPowerUnblocked > dmg && sa.getHostCard() != null && sa.getHostCard().isInPlay()) {
                            if (sa.getPayCosts().hasNoManaCost()) {
                                return true; // always activate abilities which cost no mana and which can increase unblocked damage
                            }
                        }
                    }
                }

                float value = 1.0f * (pumpedDmg - dmg);
                if (c == sa.getHostCard() && power > 0) {
                    int divisor = sa.getPayCosts().getTotalMana().getCMC();
                    if (divisor <= 0) {
                        divisor = 1;
                    }
                    value *= power / divisor;
                } else {
                    value /= opp.getLife();
                }
                chance += value;
            }

            //4. lifelink
            if (ai.canGainLife() && ai.getLife() > 0 && !c.hasKeyword(Keyword.LIFELINK) && keywords.contains("Lifelink")
                    && (combat.isAttacking(c) || combat.isBlocking(c))) {
                int dmg = pumped.getNetCombatDamage();
                //The actual dmg inflicted should be the sum of ComputerUtilCombat.predictDamageTo() for opposing creature
                //and trample damage (if any)
                chance += 1.0f * dmg / ai.getLife();
            }

            //5. if the life of the computer is in danger, try to pump blockers blocking Tramplers
            if (combat.isBlocking(c) && toughness > 0) {
                List<Card> blockedBy = combat.getAttackersBlockedBy(c);
                boolean attackerHasTrample = false;
                for (Card b : blockedBy) {
                    attackerHasTrample |= b.hasKeyword(Keyword.TRAMPLE);
                }
                if (attackerHasTrample && (sa.isAbility() || ComputerUtilCombat.lifeInDanger(ai, combat))) {
                    return true;
                }
            }
        }

        if ("UntapCombatTrick".equals(sa.getParam("AILogic")) && c.isTapped()) {
            if (phase.is(PhaseType.COMBAT_DECLARE_ATTACKERS) && phase.getPlayerTurn().isOpponentOf(ai)) {
                chance += 0.5f; // this creature will untap to become a potential blocker
            } else if (phase.is(PhaseType.COMBAT_DECLARE_BLOCKERS, ai)) {
                chance += 1.0f; // untap after tapping for attack
            }
        }

        if (isBerserk) {
            // if we got here, Berserk will result in the pumped creature dying at EOT and the opponent will not lose
            // (other similar cards with AILogic$ Berserk that do not die only when attacking are excluded from consideration)
            if (ai.getController() instanceof PlayerControllerAi) {
                boolean aggr = ((PlayerControllerAi) ai.getController()).getAi().getBoolProperty(AiProps.USE_BERSERK_AGGRESSIVELY)
                        || sa.hasParam("AtEOT");
                if (!aggr) {
                    return false;
                }
            }
        }

        boolean wantToHoldTrick = holdCombatTricks && !ai.getCardsIn(ZoneType.Hand).isEmpty();
        if (chanceToHoldCombatTricks >= 0) {
            // Obey the chance specified in the AI profile for holding combat tricks
            wantToHoldTrick &= MyRandom.percentTrue(chanceToHoldCombatTricks);
        } else {
            // Use standard considerations dependent solely on the buff chance determined above
            wantToHoldTrick &= MyRandom.getRandom().nextFloat() < chance;
        }

        boolean isHeldCombatTrick = combatTrick && wantToHoldTrick;

        if (isHeldCombatTrick) {
            if (AiCardMemory.isMemorySetEmpty(ai, AiCardMemory.MemorySet.TRICK_ATTACKERS)) {
                // Attempt to hold combat tricks until blockers are declared, and try to lure the opponent into blocking
                // (The AI will only do it for one attacker at the moment, otherwise it risks running his attackers into
                // an army of opposing blockers with only one combat trick in hand)
                // Reserve the mana until Declare Blockers such that the AI doesn't tap out before having a chance to use
                // the combat trick
                boolean reserved = false;
                if (ai.getController().isAI()) {
                    reserved = ((PlayerControllerAi) ai.getController()).getAi().reserveManaSources(sa, PhaseType.COMBAT_DECLARE_BLOCKERS, false);
                    // Only proceed with this if we could actually reserve mana
                    if (reserved) {
                        AiCardMemory.rememberCard(ai, c, AiCardMemory.MemorySet.MANDATORY_ATTACKERS);
                        AiCardMemory.rememberCard(ai, c, AiCardMemory.MemorySet.TRICK_ATTACKERS);
                        return false;
                    }
                }
            } else {
                // Don't try to mix "lure" and "precast" paradigms for combat tricks, since that creates issues with
                // the AI overextending the attack
                return false;
            }
        }

        return simAI || MyRandom.getRandom().nextFloat() < chance;
    }

    /**
     * Apply "pump" ability and return modified creature
     *
     * @param ai        casting player
     * @param sa        Pump* or CounterPut*
     * @param c         target of sa
     * @param toughness +T
     * @param power     +P
     * @param keywords  additional keywords from sa (only for Pump)
     * @return
     */
    public static Card getPumpedCreature(final Player ai, final SpellAbility sa,
                                         final Card c, int toughness, int power, final List<String> keywords) {
        Card pumped = new CardCopyService(c).copyCard(false);
        pumped.setSickness(c.hasSickness());
        final long timestamp = c.getGame().getNextTimestamp();
        final List<String> kws = Lists.newArrayList();
        final List<String> hiddenKws = Lists.newArrayList();
        for (String kw : keywords) {
            if (kw.startsWith("HIDDEN")) {
                hiddenKws.add(kw.substring(7));
            } else {
                kws.add(kw);
            }
        }

        // Berserk (and other similar cards)
        final boolean isBerserk = "Berserk".equals(sa.getParam("AILogic"));
        int berserkPower = 0;
        if (isBerserk && sa.hasSVar("X")) {
            if ("Targeted$CardPower".equals(sa.getSVar("X"))) {
                berserkPower = c.getCurrentPower();
            } else {
                berserkPower = AbilityUtils.calculateAmount(sa.getHostCard(), "X", sa);
            }
        }

        // Electrostatic Pummeler
        for (SpellAbility ab : c.getSpellAbilities()) {
            if ("Pummeler".equals(ab.getParam("AILogic"))) {
                Pair<Integer, Integer> newPT = SpecialCardAi.ElectrostaticPummeler.getPumpedPT(ai, power, toughness);
                power = newPT.getLeft();
                toughness = newPT.getRight();
            }
        }

        pumped.addNewPT(c.getCurrentPower(), c.getCurrentToughness(), timestamp, 0);
        pumped.setPTBoost(c.getPTBoostTable());
        pumped.addPTBoost(power + berserkPower, toughness, timestamp, 0);

        if (!kws.isEmpty()) {
            pumped.addChangedCardKeywords(kws, null, false, timestamp, null, false);
        }
        if (!hiddenKws.isEmpty()) {
            pumped.addHiddenExtrinsicKeywords(timestamp, 0, hiddenKws);
        }
        pumped.setCounters(c.getCounters());
        //Copies tap-state and extra keywords (auras, equipment, etc.) 
        if (c.isTapped()) {
            pumped.setTapped(true);
        }

        KeywordCollection copiedKeywords = new KeywordCollection();
        copiedKeywords.insertAll(pumped.getKeywords());
        List<KeywordInterface> toCopy = Lists.newArrayList();
        for (KeywordInterface k : c.getUnhiddenKeywords()) {
            KeywordInterface copiedKI = k.copy(c, true);
            if (!copiedKeywords.contains(copiedKI.getOriginal())) {
                toCopy.add(copiedKI);
            }
        }
        final long timestamp2 = c.getGame().getNextTimestamp(); //is this necessary or can the timestamp be re-used?
        pumped.addChangedCardKeywordsInternal(toCopy, null, false, timestamp2, null, false);
        pumped.updateKeywordsCache();
        applyStaticContPT(ai.getGame(), pumped, new CardCollection(c));
        return pumped;
    }

    /**
     * Applies static continuous Power/Toughness effects to a (virtual) creature.
     *
     * @param game    game instance to work with
     * @param vCard   creature to work with
     * @param exclude list of cards to exclude when considering ability sources, accepts null
     */
    public static void applyStaticContPT(final Game game, Card vCard, final CardCollectionView exclude) {
        if (!vCard.isCreature()) {
            return;
        }
        final CardCollection list = new CardCollection(game.getCardsIn(ZoneType.Battlefield));
        list.addAll(game.getCardsIn(ZoneType.Command));
        if (exclude != null) {
            list.removeAll(exclude);
        }
        list.add(vCard); // account for the static abilities that may be present on the card itself
        for (final Card c : list) {
            // remove old boost that might be copied
            for (final StaticAbility stAb : c.getStaticAbilities()) {
                vCard.removePTBoost(c.getLayerTimestamp(), stAb.getId());
                if (!stAb.checkMode(StaticAbilityMode.Continuous)) {
                    continue;
                }
                if (!stAb.hasParam("Affected")) {
                    continue;
                }
                if (!stAb.hasParam("AddPower") && !stAb.hasParam("AddToughness")) {
                    continue;
                }
                if (!stAb.matchesValidParam("Affected", vCard)) {
                    continue;
                }
                int att = 0;
                if (stAb.hasParam("AddPower")) {
                    String addP = stAb.getParam("AddPower");
                    att = AbilityUtils.calculateAmount(addP.contains("Affected") ? vCard : c, addP, stAb, true);
                }
                int def = 0;
                if (stAb.hasParam("AddToughness")) {
                    String addT = stAb.getParam("AddToughness");
                    def = AbilityUtils.calculateAmount(addT.contains("Affected") ? vCard : c, addT, stAb, true);
                }
                vCard.addPTBoost(att, def, c.getLayerTimestamp(), stAb.getId());
            }
        }
    }

    /**
     * Evaluate if the ability can save a target against removal
     *
     * @param ai casting player
     * @param sa Pump* or CounterPut*
     * @return
     */
    public static AiAbilityDecision canPumpAgainstRemoval(Player ai, SpellAbility sa) {
        final List<GameObject> objects = ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa, true);

        if (!sa.usesTargeting()) {
            final List<Card> cards = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa);
            for (final Card card : cards) {
                if (objects.contains(card)) {
                    return new AiAbilityDecision(100, AiPlayDecision.ResponseToStackResolve);
                }
            }
            // For pumps without targeting restrictions, just return immediately until this is fleshed out.
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }

        CardCollection threatenedTargets = CardLists.getTargetableCards(ai.getCardsIn(ZoneType.Battlefield), sa);
        threatenedTargets = ComputerUtil.getSafeTargets(ai, sa, threatenedTargets);
        threatenedTargets.retainAll(objects);

        if (!threatenedTargets.isEmpty()) {
            sortByEvaluateCreature(threatenedTargets);
            for (Card c : threatenedTargets) {
                if (sa.canAddMoreTarget()) {
                    sa.getTargets().add(c);
                    if (!sa.canAddMoreTarget()) {
                        break;
                    }
                }
            }
            if (!sa.isTargetNumberValid()) {
                sa.resetTargets();
                return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
            }
            return new AiAbilityDecision(100, AiPlayDecision.ResponseToStackResolve);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    public static boolean isUselessCreature(Player ai, Card c) {
        if (c == null) {
            return true;
        }
        if (!c.isCreature()) {
            return false;
        }
        if (c.isDetained()) {
            return true;
        }
        if (c.hasKeyword("CARDNAME can't attack or block.")) {
            return true;
        }
        if (c.getOwner() == ai && ai.getOpponents().contains(c.getController())) {
            return true;
        }
        if (c.isTapped() && !c.canUntap(ai, true)) {
            return true;
        }
        return false;
    }

    public static boolean hasActiveUndyingOrPersist(final Card c) {
        if (c.isToken()) {
            return false;
        }
        if (c.hasKeyword(Keyword.UNDYING) && c.getCounters(CounterEnumType.P1P1) == 0) {
            return true;
        }
        if (c.hasKeyword(Keyword.PERSIST) && c.getCounters(CounterEnumType.M1M1) == 0) {
            return true;
        }
        return false;
    }

    public static int getMaxSAEnergyCostOnBattlefield(final Player ai) {
        // returns the maximum energy cost of an ability that permanents on the battlefield under AI's control have
        int maxEnergyCost = 0;

        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            for (SpellAbility sa : c.getSpellAbilities()) {
                CostPayEnergy energyCost = sa.getPayCosts().getCostEnergy();
                if (energyCost != null) {
                    int amount = energyCost.convertAmount();
                    if (amount > maxEnergyCost) {
                        maxEnergyCost = amount;
                    }
                }
            }
        }

        return maxEnergyCost;
    }

    public static CardCollection prioritizeCreaturesWorthRemovingNow(final Player ai, CardCollection oppCards, final boolean temporary) {
        if (!CardLists.getNotType(oppCards, "Creature").isEmpty()) {
            // non-creatures were passed, nothing to do here
            return oppCards;
        }

        boolean enablePriorityRemoval = AiProfileUtil.getBoolProperty(ai, AiProps.ACTIVELY_DESTROY_IMMEDIATELY_UNBLOCKABLE);
        int priorityRemovalThreshold = AiProfileUtil.getIntProperty(ai, AiProps.DESTROY_IMMEDIATELY_UNBLOCKABLE_THRESHOLD);
        boolean priorityRemovalOnlyInDanger = AiProfileUtil.getBoolProperty(ai, AiProps.DESTROY_IMMEDIATELY_UNBLOCKABLE_ONLY_IN_DNGR);
        int lifeInDanger = AiProfileUtil.getIntProperty(ai, AiProps.DESTROY_IMMEDIATELY_UNBLOCKABLE_LIFE_IN_DNGR);

        if (!enablePriorityRemoval) {
            // Nothing to do here, the profile does not allow prioritizing
            return oppCards;
        }

        CardCollection aiCreats = ai.getCreaturesInPlay();
        if (temporary) {
            // Pump effects that add "CARDNAME can't attack" and similar things. Only do it if something is untapped.
            oppCards = CardLists.filter(oppCards, CardPredicates.UNTAPPED);
        }

        CardCollection priorityCards = new CardCollection();
        for (Card atk : oppCards) {
            boolean canBeBlocked = false;
            if (isUselessCreature(atk.getController(), atk)) {
                continue;
            }
            for (Card blk : aiCreats) {
                if (CombatUtil.canBlock(atk, blk, true)) {
                    canBeBlocked = true;
                    break;
                }
            }
            if (!canBeBlocked) {
                boolean threat = ComputerUtilCombat.getAttack(atk) >= ai.getLife() - lifeInDanger;
                if (!priorityRemovalOnlyInDanger || threat) {
                    priorityCards.add(atk);
                }
            }
        }

        if (!priorityCards.isEmpty() && priorityCards.size() <= priorityRemovalThreshold) {
            return priorityCards;
        }

        return oppCards;
    }

    public static AiPlayDecision checkNeedsToPlayReqs(final Card card, final SpellAbility sa) {
        Game game = card.getGame();
        boolean isRightSplit = sa != null && sa.getCardState().getStateName() == CardStateName.RightSplit;
        String needsToPlayName = isRightSplit ? "SplitNeedsToPlay" : "NeedsToPlay";
        String needsToPlayVarName = isRightSplit ? "SplitNeedsToPlayVar" : "NeedsToPlayVar";

        // TODO: if there are ever split cards with Evoke or Kicker, factor in the right split option above
        if (sa != null) {
            if (sa.isEvoke()) {
                // if the spell is evoked, will use NeedsToPlayEvoked if available (otherwise falls back to NeedsToPlay)
                if (card.hasSVar("NeedsToPlayEvoked")) {
                    needsToPlayName = "NeedsToPlayEvoked";
                }
                if (card.hasSVar("NeedsToPlayEvokedVar")) {
                    needsToPlayVarName = "NeedsToPlayEvokedVar";
                }
            } else if (sa.isKicked()) {
                // if the spell is kicked, uses NeedsToPlayKicked if able and locks out the regular NeedsToPlay check
                // for unkicked spells, uses NeedsToPlay
                if (card.hasSVar("NeedsToPlayKicked")) {
                    needsToPlayName = "NeedsToPlayKicked";
                } else {
                    needsToPlayName = "UNUSED";
                }
                if (card.hasSVar("NeedsToPlayKickedVar")) {
                    needsToPlayVarName = "NeedsToPlayKickedVar";
                } else {
                    needsToPlayVarName = "UNUSED";
                }
            }
        }

        if (card.hasSVar(needsToPlayName)) {
            final String needsToPlay = card.getSVar(needsToPlayName);

            // A special case which checks that this creature will attack if it's the AI's turn
            if (needsToPlay.equalsIgnoreCase("WillAttack")) {
                if (sa != null && game.getPhaseHandler().isPlayerTurn(sa.getActivatingPlayer())) {
                    return doesSpecifiedCreatureAttackAI(sa.getActivatingPlayer(), card) ?
                            AiPlayDecision.WillPlay : AiPlayDecision.BadEtbEffects;
                } else {
                    return AiPlayDecision.WillPlay; // not our turn, skip this check for the possible Flash use etc.
                }
            }

            CardCollectionView list = game.getCardsIn(ZoneType.Battlefield);

            list = CardLists.getValidCards(list, needsToPlay, card.getController(), card, sa);
            if (list.isEmpty()) {
                return AiPlayDecision.MissingNeededCards;
            }
        }
        if (card.getSVar(needsToPlayVarName).length() > 0) {
            final String needsToPlay = card.getSVar(needsToPlayVarName);
            String sVar = needsToPlay.split(" ")[0];
            String comparator = needsToPlay.split(" ")[1];
            String compareTo = comparator.substring(2);
            int x = AbilityUtils.calculateAmount(card, sVar, sa);
            int y = AbilityUtils.calculateAmount(card, compareTo, sa);

            if (!Expressions.compare(x, comparator, y)) {
                return AiPlayDecision.NeedsToPlayCriteriaNotMet;
            }
        }

        return AiPlayDecision.WillPlay;
    }

    public static Cost getTotalWardCost(Card c) {
        Cost totalCost = new Cost(ManaCost.NO_COST, false);
        for (final KeywordInterface inst : c.getKeywords(Keyword.WARD)) {
            final String keyword = inst.getOriginal();
            final String[] k = keyword.split(":");
            final Cost wardCost = new Cost(k[1], false);
            totalCost = totalCost.add(wardCost);
        }
        return totalCost;
    }

    public static boolean willUntap(Player ai, Card tapped) {
        // TODO use AiLogic on trigger in case card loses all abilities
        // if it's from a static need to also check canUntap
        for (Card card : ai.getGame().getCardsIn(ZoneType.Battlefield)) {
            boolean untapsEachTurn = card.hasSVar("UntapsEachTurn");
            boolean untapsEachOtherTurn = card.hasSVar("UntapsEachOtherPlayerTurn");

            if (untapsEachTurn || untapsEachOtherTurn) {
                String affected = untapsEachTurn ? card.getSVar("UntapsEachTurn")
                        : card.getSVar("UntapsEachOtherPlayerTurn");

                for (String aff : TextUtil.split(affected, ',')) {
                    if (tapped.isValid(aff, ai, tapped, null)
                            && (untapsEachTurn || (untapsEachOtherTurn && ai.equals(card.getController())))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // TODO replace most calls to Player.isCardInPlay because they include phased out
    public static boolean isNonDisabledCardInPlay(final Player ai, final String cardName) {
        for (Card card : ai.getCardsIn(ZoneType.Battlefield, cardName)) {
            // TODO - Better logic to determine if a permanent is disabled by local effects
            // currently assuming any permanent enchanted by another player
            // is disabled and a second copy is necessary
            // will need actual logic that determines if the enchantment is able
            // to disable the permanent or it's still functional and a duplicate is unneeded.
            boolean disabledByEnemy = false;
            for (Card card2 : card.getEnchantedBy()) {
                if (card2.getOwner() != ai) {
                    disabledByEnemy = true;
                    break;
                }
            }
            if (!disabledByEnemy) {
                return true;
            }
        }
        return false;
    }

    // use this function to skip expensive calculations on identical cards
    public static CardCollection dedupeCards(CardCollection cc) {
        if (cc.size() <= 1) {
            return cc;
        }
        CardCollection deduped = new CardCollection();
        for (Card c : cc) {
            boolean unique = true;
            if (c.isInZone(ZoneType.Hand) && !c.hasPerpetual()) {
                for (Card d : deduped) {
                    if (d.isInZone(ZoneType.Hand) && d.getOwner().equals(c.getOwner()) && d.getName().equals(c.getName())) {
                        unique = false;
                        break;
                    }
                }
            }
            if (unique) {
                deduped.add(c);
            }
        }
        return deduped;
    }

    // Determine if the AI has an AI:RemoveDeck:All or an AI:RemoveDeck:Random hint specified.
    // Includes a NPE guard on getRules() which might otherwise be tripped on some cards (e.g. tokens).
    public static boolean isCardRemAIDeck(final Card card) {
        return card.getRules() != null && card.getRules().getAiHints().getRemAIDecks();
    }

    public static boolean isCardRemRandomDeck(final Card card) {
        return card.getRules() != null && card.getRules().getAiHints().getRemRandomDecks();
    }

    public static boolean isCardRemNonCommanderDeck(final Card card) {
        return card.getRules() != null && card.getRules().getAiHints().getRemNonCommanderDecks();
    }

    static class LandEvaluator implements Function<Card, Integer> {
        @Override
        public Integer apply(Card card) {
            return GameStateEvaluator.evaluateLand(card);
        }
    }
}
