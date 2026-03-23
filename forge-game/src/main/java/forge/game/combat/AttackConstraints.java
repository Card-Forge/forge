package forge.game.combat;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.*;
import forge.util.IterableUtil;
import org.apache.commons.lang3.tuple.Pair;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMustAttack;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

public class AttackConstraints {

    private final CardCollection possibleAttackers;
    private final FCollectionView<GameEntity> possibleDefenders;
    private final GlobalAttackRestrictions globalRestrictions;

    private final Map<Card, AttackRestriction> restrictions = Maps.newHashMap();
    private final Map<Card, AttackRequirement> requirements = Maps.newHashMap();
    private final Multimap<GameEntity, StaticAbility> playerRequirements;

    public AttackConstraints(final Combat combat) {
        possibleAttackers = combat.getAttackingPlayer().getCreaturesInPlay();
        possibleDefenders = combat.getDefenders();
        globalRestrictions = GlobalAttackRestrictions.getGlobalRestrictions(combat.getAttackingPlayer(), possibleDefenders);
        playerRequirements = StaticAbilityMustAttack.mustAttackSpecific(combat.getAttackingPlayer(), possibleDefenders);

        // TODO extend for "SharedTurnModes"
        for (final Card possibleAttacker : possibleAttackers) {
            restrictions.put(possibleAttacker, new AttackRestriction(possibleAttacker, possibleDefenders));

            final Multimap<Card, StaticAbility> causesToAttack = StaticAbilityMustAttack.getAttackRequirements(possibleAttacker,
                    possibleAttackers.stream().filter(p -> !p.equals(possibleAttacker)).collect(Collectors.toList()));

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

        final Map<Map<Card, GameEntity>, Integer> possible = new LinkedHashMap<>();
        final List<Attack> reqs = getSortedFilteredRequirements();

        // Now try all others (plus empty attack) and count their violations
        final FCollection<Map<Card, GameEntity>> legalAttackers = collectLegalAttackers(reqs, myMax);
        possible.putAll(Maps.asMap(legalAttackers.asSet(), this::countViolations));
        int empty = countViolations(Collections.emptyMap());
        if (empty != -1) {
            possible.put(Collections.emptyMap(), empty);
        }

        // take the case with the fewest violations
        return possible.entrySet().stream()
                .min(Comparator.comparingInt(Entry::getValue))
                .map(e -> Pair.of(e.getKey(), e.getValue()))
                .orElseThrow(NoSuchElementException::new);
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
        final Map<GameEntity, Integer> toDefender = new LinkedHashMap<>();
        int attackersNeeded = 0;

        while (!reqs.isEmpty()) {
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
            if (defMax != null && toDefender.getOrDefault(req.defender, 0) >= defMax) {
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

            final AttackRequirement requirement = requirements.get(req.attacker);

            if (!requirement.getCausesToAttack().isEmpty()) {
                final List<Attack> clonedReqs = deepClone(reqs);
                for (final Entry<Card, Collection<StaticAbility>> causesToAttack : requirement.getCausesToAttack().asMap().entrySet()) {
                    for (final Attack a : IterableUtil.filter(reqs, findAll(causesToAttack.getKey()))) {
                        a.requirements += causesToAttack.getValue().size();
                    }
                }
                // if maximum < no of possible attackers, try both with and without this creature
                if (isLimited) {
                    // try without
                    clonedReqs.removeIf(findAll(req.attacker));
                    final CardCollection clonedReserved = new CardCollection(reserved);
                    result.addAll(collectLegalAttackers(myAttackers, clonedReqs, clonedReserved, localMaximum));
                }
            }

            // finally: add the creature
            myAttackers.put(req.attacker, req.defender);
            toDefender.merge(req.defender, 1, Integer::sum);
            reqs.removeIf(findAll(req.attacker));
            reserved.remove(req.attacker);
            localMaximum--;
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

        Collections.sort(result, Comparator.reverseOrder());

        Multimap<GameEntity, StaticAbility> playerReqs = MultimapBuilder.hashKeys().arrayListValues().build(playerRequirements);
        CardCollection usedAttackers = new CardCollection();
        while (!playerReqs.isEmpty()) {
            Map.Entry<GameEntity, Collection<StaticAbility>> playerReq = playerReqs.asMap().entrySet().stream()
                    .max(Comparator.comparing(e -> e.getValue().size())).orElse(null);
            // find best attack to also fulfill the additional requirements
            Attack bestMatch = result.stream().filter(att -> !usedAttackers.contains(att.attacker) && att.defender.equals(playerReq.getKey())).findFirst().orElse(null);
            if (bestMatch != null) {
                bestMatch.requirements += playerReq.getValue().size();
                usedAttackers.add(bestMatch.attacker);
                // recalculate remaining requirements
                playerReqs.values().removeAll(playerReq.getValue());
            } else {
                playerReqs.removeAll(playerReq.getKey());
            }
        }
        if (!usedAttackers.isEmpty()) {
            // order could have changed
            Collections.sort(result, Comparator.reverseOrder());
        }

        return result;
    }
    private static List<Attack> deepClone(final List<Attack> original) {
        final List<Attack> newList = Lists.newLinkedList();
        for (final Attack attack : original) {
            newList.add(new Attack(attack));
        }
        return newList;
    }
    private static Predicate<Attack> findAll(final Card attacker) {
        return input -> input.attacker.equals(attacker);
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

        Multimap<StaticAbility, GameEntity> inverted = MultimapBuilder.hashKeys().arrayListValues().build();
        for (Collection<GameEntity> defSet : Multimaps.invertFrom(playerRequirements, inverted).asMap().values()) {
            if (Collections.disjoint(defSet, attackers.values())) {
                violations++;
            }
        }

        return violations;
    }
}
