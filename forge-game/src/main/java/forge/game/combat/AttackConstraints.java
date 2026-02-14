package forge.game.combat;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.*;
import forge.util.IterableUtil;
import org.apache.commons.lang3.tuple.Pair;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.staticability.StaticAbilityMustAttack;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.maps.LinkedHashMapToAmount;
import forge.util.maps.MapToAmount;
import forge.util.maps.MapToAmountUtil;

public class AttackConstraints {

    private final CardCollection possibleAttackers;
    private final FCollectionView<GameEntity> possibleDefenders;
    private final GlobalAttackRestrictions globalRestrictions;

    private final Map<Card, AttackRestriction> restrictions = Maps.newHashMap();
    private final Map<Card, AttackRequirement> requirements = Maps.newHashMap();
    private final List<Set<GameEntity>> playerRequirements;

    public AttackConstraints(final Combat combat) {
        final Game game = combat.getAttackingPlayer().getGame();
        possibleAttackers = combat.getAttackingPlayer().getCreaturesInPlay();
        possibleDefenders = combat.getDefenders();
        globalRestrictions = GlobalAttackRestrictions.getGlobalRestrictions(combat.getAttackingPlayer(), possibleDefenders);
        playerRequirements = StaticAbilityMustAttack.mustAttackSpecific(combat.getAttackingPlayer(), possibleDefenders);

        // Number of "must attack" constraints on each creature with a magnet counter (equal to the number of permanents requiring that constraint).
        int nMagnetRequirements = 0;
        final CardCollectionView magnetAttackers = CardLists.filter(possibleAttackers, CardPredicates.hasCounter(CounterEnumType.MAGNET));
        // Only require if a creature with a magnet counter on it attacks.
        if (!magnetAttackers.isEmpty()) {
            nMagnetRequirements = CardLists.getAmountOfKeyword(
                    game.getCardsIn(ZoneType.Battlefield),
                    "If a creature with a magnet counter on it attacks, all creatures with magnet counters on them attack if able.");
        }

        final MapToAmount<Card> attacksIfOtherAttacks = new LinkedHashMapToAmount<>();
        for (final Card possibleAttacker : possibleAttackers) {
            attacksIfOtherAttacks.add(possibleAttacker, possibleAttacker.getAmountOfKeyword("If a creature you control attacks, CARDNAME also attacks if able."));
        }

        for (final Card possibleAttacker : possibleAttackers) {
            restrictions.put(possibleAttacker, new AttackRestriction(possibleAttacker, possibleDefenders));

            final MapToAmount<Card> causesToAttack = new LinkedHashMapToAmount<>();
            for (final Entry<Card, Integer> entry : attacksIfOtherAttacks.entrySet()) {
                if (entry.getKey() != possibleAttacker) {
                    causesToAttack.add(entry.getKey(), entry.getValue());
                }
            }

            // Number of "all must attack" requirements on this attacker
            final int nAllMustAttack = possibleAttacker.getAmountOfKeyword("If CARDNAME attacks, all creatures you control attack if able.");
            for (final Card c : possibleAttackers) {
                if (c != possibleAttacker) {
                    causesToAttack.add(c, nAllMustAttack);
                }
            }

            if (possibleAttacker.getCounters(CounterEnumType.MAGNET) > 0) {
                for (final Card c : magnetAttackers) {
                    if (c != possibleAttacker) {
                        causesToAttack.add(c, nMagnetRequirements);
                    }
                }
            }

            final AttackRequirement r = new AttackRequirement(possibleAttacker, causesToAttack, possibleDefenders);
            requirements.put(possibleAttacker, r);
        }
    }

    public Map<Card, AttackRestriction> getRestrictions() {
        return restrictions;
    }
    public GlobalAttackRestrictions getGlobalRestrictions() {
        return globalRestrictions;
    }

    public Map<Card, AttackRequirement> getRequirements() {
        return requirements;
    }

    /**
     * Get a set of legal attackers.
     * 
     * @return a {@link Pair} of
     *         <ul>
     *         <li>A {@link Map} mapping attacking creatures to defenders;</li>
     *         <li>The number of requirements fulfilled by this attack.</li>
     *         </ul>
     */
    public Pair<Map<Card, GameEntity>, Integer> getLegalAttackers() {
        final int myMax = Math.min(Objects.requireNonNullElse(globalRestrictions.getMax(), Integer.MAX_VALUE), possibleAttackers.size());
        if (myMax == 0) {
            return Pair.of(Collections.emptyMap(), 0);
        }

        final MapToAmount<Map<Card, GameEntity>> possible = new LinkedHashMapToAmount<>();
        final List<Attack> reqs = getSortedFilteredRequirements();
        final CardCollection myPossibleAttackers = new CardCollection(possibleAttackers);

        // First, remove all requirements of creatures that aren't going attack this combat anyway
        final CardCollection attackersToRemove = new CardCollection();
        for (final Card attacker : myPossibleAttackers) {
            final Set<AttackRestrictionType> types = restrictions.get(attacker).getTypes();
            if ((types.contains(AttackRestrictionType.NEED_TWO_OTHERS)     && myMax <= 2
                    ) || (
                 types.contains(AttackRestrictionType.NOT_ALONE)           && myMax <= 1
                    ) || (
                 types.contains(AttackRestrictionType.NEED_BLACK_OR_GREEN) && myMax <= 1
                    ) || (
                 types.contains(AttackRestrictionType.NEED_GREATER_POWER)  && myMax <= 1
                            )) {
                reqs.removeAll(findAll(reqs, attacker));
                attackersToRemove.add(attacker);
            }
        }
        myPossibleAttackers.removeAll(attackersToRemove);
        attackersToRemove.clear();

        // Next, remove creatures with constraints that can't be fulfilled.
        for (final Card attacker : myPossibleAttackers) {
            final Set<AttackRestrictionType> types = restrictions.get(attacker).getTypes();
            if (types.contains(AttackRestrictionType.NEED_BLACK_OR_GREEN)) {
                if (!myPossibleAttackers.anyMatch(AttackRestrictionType.NEED_BLACK_OR_GREEN.getPredicate(attacker))) {
                    attackersToRemove.add(attacker);
                }
            } else if (types.contains(AttackRestrictionType.NEED_GREATER_POWER)) {
                if (!myPossibleAttackers.anyMatch(AttackRestrictionType.NEED_GREATER_POWER.getPredicate(attacker))) {
                    attackersToRemove.add(attacker);
                }
            }
        }
        myPossibleAttackers.removeAll(attackersToRemove);
        for (final Card toRemove : attackersToRemove) {
            reqs.removeAll(findAll(reqs, toRemove));
        }

        // First, successively try each creature that must attack alone.
        for (final Card attacker : myPossibleAttackers) {
            if (restrictions.get(attacker).getTypes().contains(AttackRestrictionType.ONLY_ALONE)) {
                final Attack attack = findFirst(reqs, attacker);
                if (attack == null) {
                    // no requirements, we don't care anymore
                    continue;
                }
                final Map<Card, GameEntity> attackMap = ImmutableMap.of(attack.attacker, attack.defender);
                final int violations = countViolations(attackMap);
                if (violations != -1) {
                    possible.put(attackMap, violations);
                }
                // remove them from the requirements, as they'll not be relevant to this calculation any more
                reqs.removeAll(findAll(reqs, attacker));
            }
        }

        // Now try all others (plus empty attack) and count their violations
        final FCollection<Map<Card, GameEntity>> legalAttackers = collectLegalAttackers(reqs, myMax);
        possible.putAll(Maps.asMap(legalAttackers.asSet(), this::countViolations));
        int empty = countViolations(Collections.emptyMap());
        if (empty != -1) {
            possible.put(Collections.emptyMap(), empty);
        }
 
        // take the case with the fewest violations
        return MapToAmountUtil.min(possible);
    }

    private FCollection<Map<Card, GameEntity>> collectLegalAttackers(final List<Attack> reqs, final int maximum) {
        return new FCollection<>
                (collectLegalAttackers(Collections.emptyMap(), deepClone(reqs), new CardCollection(), maximum));
    }
    private List<Map<Card, GameEntity>> collectLegalAttackers(final Map<Card, GameEntity> attackers, final List<Attack> reqs, final CardCollection reserved, final int maximum) {
        final List<Map<Card, GameEntity>> result = Lists.newLinkedList();

        int localMaximum = maximum;
        final boolean isLimited = globalRestrictions.getMax() != null;
        final Map<Card, GameEntity> myAttackers = Maps.newHashMap(attackers);
        final MapToAmount<GameEntity> toDefender = new LinkedHashMapToAmount<>();
        int attackersNeeded = 0;

        outer: while (!reqs.isEmpty()) {
            final Iterator<Attack> iterator = reqs.iterator();
            final Attack req = iterator.next();
            final boolean isReserved = reserved.contains(req.attacker);

            boolean skip = false;
            if (!isReserved) {
                if (localMaximum <= 0) {
                    // can't add any more creatures (except reserved creatures)
                    skip = true;
                } else if (req.requirements == 0 && attackersNeeded == 0 && reserved.isEmpty()) {
                    // we don't need this creature
                    skip = true;
                }
            }
            final Integer defMax = globalRestrictions.getDefenderMax().get(req.defender);
            if (defMax != null && toDefender.count(req.defender) >= defMax) {
                // too many to this defender already
                skip = true;
            } else if (null != CombatUtil.getAttackCost(req.attacker.getGame(), req.attacker, req.defender)) {
                // has to pay a cost: skip!
                skip = true;
            }

            if (skip) {
                iterator.remove();
                continue;
            }

            boolean haveTriedWithout = false;
            final AttackRestriction restriction = restrictions.get(req.attacker);
            final AttackRequirement requirement = requirements.get(req.attacker);

            // construct the predicate restrictions
            final Collection<Predicate<Card>> predicateRestrictions = Lists.newLinkedList();
            for (final AttackRestrictionType rType : restriction.getTypes()) {
                final Predicate<Card> predicate = rType.getPredicate(req.attacker);
                if (predicate != null) {
                    predicateRestrictions.add(predicate);
                }
            }

            if (!requirement.getCausesToAttack().isEmpty()) {
                final List<Attack> clonedReqs = deepClone(reqs);
                for (final Entry<Card, Integer> causesToAttack : requirement.getCausesToAttack().entrySet()) {
                    for (final Attack a : findAll(reqs, causesToAttack.getKey())) {
                        a.requirements += causesToAttack.getValue();
                    }
                }
                // if maximum < no of possible attackers, try both with and without this creature
                if (isLimited) {
                    // try without
                    clonedReqs.removeAll(findAll(clonedReqs, req.attacker));
                    final CardCollection clonedReserved = new CardCollection(reserved);
                    result.addAll(collectLegalAttackers(myAttackers, clonedReqs, clonedReserved, localMaximum));
                    haveTriedWithout = true;
                }
            }

            for (final Predicate<Card> predicateRestriction : predicateRestrictions) {
                if (Sets.union(myAttackers.keySet(), reserved.asSet()).stream().anyMatch(predicateRestriction)) {
                    // predicate fulfilled already, ignore!
                    continue;
                }
                // otherwise: reserve first creature to match it!
                final Attack match = findFirst(reqs, predicateRestriction);
                if (match == null) {
                    // no match: remove this creature completely
                    reqs.removeAll(findAll(reqs, req.attacker));
                    continue outer;
                }
                // found one! add it to reserve and lower local maximum
                reserved.add(match.attacker);
                localMaximum--;

                // if limited, try both with and without this creature
                if (!haveTriedWithout && isLimited) {
                    // try without
                    final List<Attack> clonedReqs = deepClone(reqs);
                    clonedReqs.removeAll(findAll(clonedReqs, req.attacker));
                    final CardCollection clonedReserved = new CardCollection(reserved);
                    result.addAll(collectLegalAttackers(myAttackers, clonedReqs, clonedReserved, localMaximum));
                    haveTriedWithout = true;
                }
            }

            // finally: add the creature
            myAttackers.put(req.attacker, req.defender);
            toDefender.add(req.defender);
            reqs.removeAll(findAll(reqs, req.attacker));
            reserved.remove(req.attacker);
            localMaximum--;

            // need two other attackers: set that number to the number of attackers we still need (but never < 0)
            if (restrictions.get(req.attacker).getTypes().contains(AttackRestrictionType.NEED_TWO_OTHERS)) {
                final int previousNeeded = attackersNeeded;
                attackersNeeded = Math.max(3 - (myAttackers.size() + reserved.size()), 0);
                localMaximum -= Math.max(attackersNeeded - previousNeeded, 0);
            } else if (restrictions.get(req.attacker).getTypes().contains(AttackRestrictionType.NOT_ALONE)) {
                attackersNeeded = Math.max(2 - (myAttackers.size() + reserved.size()), 0);
            }
        }

        // success if we've added everything we want
        if (reserved.isEmpty() && attackersNeeded == 0) {
            result.add(myAttackers);
        }

        return result;
    }

    private final static class Attack implements Comparable<Attack> {
        private final Card attacker;
        private final GameEntity defender;
        private int requirements;
        private Attack(final Attack other) {
            this(other.attacker, other.defender, other.requirements);
        }
        private Attack(final Card attacker, final GameEntity defender, final int requirements) {
            this.attacker = attacker;
            this.defender = defender;
            this.requirements = requirements;
        }
        @Override
        public int compareTo(final Attack other) {
            return Integer.compare(this.requirements, other.requirements);
        }
        @Override
        public String toString() {
            return "[" + requirements + "] " + attacker + " to " + defender; 
        }
    }

    private List<Attack> getSortedFilteredRequirements() {
        final List<Attack> result = Lists.newArrayList();
        final Map<Card, List<Pair<GameEntity, Integer>>> sortedRequirements = Maps.transformValues(requirements, AttackRequirement::getSortedRequirements);
        for (final Entry<Card, List<Pair<GameEntity, Integer>>> reqList : sortedRequirements.entrySet()) {
            final AttackRestriction restriction = restrictions.get(reqList.getKey());
            final List<Pair<GameEntity, Integer>> list = reqList.getValue();
            for (Pair<GameEntity, Integer> attackReq : list) {
                if (restriction.canAttack(attackReq.getLeft())) {
                    result.add(new Attack(reqList.getKey(), attackReq.getLeft(), attackReq.getRight()));
                }
            }
        }

        Collections.sort(result);

        List<Set<GameEntity>> playerReqs = Lists.newArrayList(playerRequirements);
        CardCollection usedAttackers = new CardCollection();
        FCollection<GameEntity> excludedDefenders = new FCollection<>();
        MapToAmount<GameEntity> sortedPlayerReqs = new LinkedHashMapToAmount<>();
        sortedPlayerReqs.addAll(Iterables.concat(playerReqs));
        while (!sortedPlayerReqs.isEmpty()) {
            Pair<GameEntity, Integer> playerReq = MapToAmountUtil.max(sortedPlayerReqs);
            // find best attack to also fulfill the additional requirements
            Attack bestMatch = Iterables.getLast(IterableUtil.filter(result, att -> !usedAttackers.contains(att.attacker) && att.defender.equals(playerReq.getLeft())), null);
            if (bestMatch != null) {
                bestMatch.requirements += playerReq.getRight();
                usedAttackers.add(bestMatch.attacker);
                // recalculate remaining requirements
                playerReqs.removeIf(s -> s.contains(playerReq.getLeft()));
                sortedPlayerReqs.clear();
                sortedPlayerReqs.addAll(Iterables.concat(playerReqs));
            } else {
                excludedDefenders.add(playerReq.getLeft());
            }
            sortedPlayerReqs.keySet().removeAll(excludedDefenders);
        }
        if (!usedAttackers.isEmpty()) {
            // order could have changed
            Collections.sort(result);
        }

        return Lists.reverse(result);
    }
    private static List<Attack> deepClone(final List<Attack> original) {
        final List<Attack> newList = Lists.newLinkedList();
        for (final Attack attack : original) {
            newList.add(new Attack(attack));
        }
        return newList;
    }
    private static Attack findFirst(final List<Attack> reqs, final Predicate<Card> predicate) {
        for (final Attack req : reqs) {
            if (predicate.test(req.attacker)) {
                return req;
            }
        }
        return null;
    }
    private static Attack findFirst(final List<Attack> reqs, final Card attacker) {
        return findFirst(reqs, attacker::equals);
    }
    private static Collection<Attack> findAll(final List<Attack> reqs, final Card attacker) {
        return Collections2.filter(reqs, input -> input.attacker.equals(attacker));
    }

    /**
     * @param attackers
     *            a {@link Map} of each attacking {@link Card} to the
     *            {@link GameEntity} it's attacking.
     * @return the number of requirements violated by this attack, or -1 if a
     *         restriction is violated.
     */
    public final int countViolations(final Map<Card, GameEntity> attackers) {
        if (!globalRestrictions.isLegal(attackers)) {
            return -1;
        }
        for (final Entry<Card, GameEntity> attacker : attackers.entrySet()) {
            final AttackRestriction restriction = restrictions.get(attacker.getKey());
            if (restriction != null && !restriction.canAttack(attacker.getKey(), attackers)) {
                // Violating a restriction!
                return -1;
            }
        }

        int violations = 0;
        for (final Card possibleAttacker : possibleAttackers) {
            final AttackRequirement requirement = requirements.get(possibleAttacker);
            if (requirement != null) {
                violations += requirement.countViolations(attackers.get(possibleAttacker), attackers);
            }
        }

        for (Set<GameEntity> defSet : playerRequirements) {
            if (Collections.disjoint(defSet, attackers.values())) {
                violations++;
            }
        }

        return violations;
    }
}
