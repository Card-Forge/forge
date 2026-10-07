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
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.google.common.collect.*;
import forge.util.IterableUtil;
import org.apache.commons.lang3.tuple.Pair;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityAttackBlockRestrict;
import forge.game.staticability.StaticAbilityMustAttack;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

public class AttackConstraints {

    // the search for the best attack settles for what it found after this many steps
    private static final int MAX_SEARCH_STEPS = 10000;

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

        // Now try all others (plus empty attack) and count their violations. Iterate the ordered
        // FCollection rather than asSet(): the min() below keeps the first entry on a
        // violation-count tie, so insertion order decides which attack is chosen instead of a
        // HashSet keyed by attack-config maps whose hashCodes come from Card identity hashes,
        // i.e. per-JVM-random - which made the AI's attack nondeterministic.
        final FCollection<Map<Card, GameEntity>> legalAttackers = collectLegalAttackers(reqs, myMax);
        for (final Map<Card, GameEntity> attackMap : legalAttackers) {
            final int violations = countViolations(attackMap);
            // only requirements were looked at, a creature might still not be allowed to attack like this
            if (violations != -1) {
                possible.put(attackMap, violations);
            }
        }
        int empty = countViolations(Collections.emptyMap());
        if (empty != -1) {
            possible.put(Collections.emptyMap(), empty);
        }

        if (possibleAttackers.anyMatch(c -> restrictions.get(c).isRestrictedByOthers())) {
            final int fewest = possible.values().stream().mapToInt(Integer::intValue).min().orElse(Integer.MAX_VALUE);
            final AttackSearch search = new AttackSearch(myMax, empty, fewest);
            if (search.best != null) {
                possible.put(search.best, search.bestViolations);
            }
        }

        // take the case with the fewest violations
        return possible.entrySet().stream()
                .min(Comparator.comparingInt(Entry::getValue))
                .map(e -> Pair.of(e.getKey(), e.getValue()))
                .orElseThrow(NoSuchElementException::new);
    }

    private FCollection<Map<Card, GameEntity>> collectLegalAttackers(final List<Attack> reqs, final int maximum) {
        return new FCollection<>
                (collectLegalAttackers(Collections.emptyMap(), deepClone(reqs), maximum));
    }
    private List<Map<Card, GameEntity>> collectLegalAttackers(final Map<Card, GameEntity> attackers, final List<Attack> reqs, final int maximum) {
        final List<Map<Card, GameEntity>> result = Lists.newLinkedList();

        int localMaximum = maximum;
        final boolean isLimited = globalRestrictions.getMax() != null;
        final Map<Card, GameEntity> myAttackers = Maps.newHashMap(attackers);
        final Map<GameEntity, Integer> toDefender = new LinkedHashMap<>();

        while (!reqs.isEmpty()) {
            final Iterator<Attack> iterator = reqs.iterator();
            final Attack req = iterator.next();

            boolean skip = false;
            if (localMaximum <= 0) {
                // can't add any more creatures
                skip = true;
            } else if (req.requirements == 0) {
                // we don't need this creature
                skip = true;
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
                    result.addAll(collectLegalAttackers(myAttackers, clonedReqs, localMaximum));
                }
            }

            // finally: add the creature
            myAttackers.put(req.attacker, req.defender);
            toDefender.merge(req.defender, 1, Integer::sum);
            reqs.removeIf(findAll(req.attacker));
            localMaximum--;
        }

        result.add(myAttackers);

        return result;
    }

    /**
     * Looks for the attack breaking the fewest requirements when creatures are restricted by what else attacks.
     * A creature without any requirement might then have to attack so that another one is able to, which
     * {@link #collectLegalAttackers} doesn't consider. Tries the combinations of the creatures that matter and
     * rates each of them with {@link #countViolations}.
     */
    private final class AttackSearch {
        private final int maximum;
        // attacks each creature could be declared with, the ones fulfilling the most requirements first
        private final Map<Card, List<Attack>> options = new LinkedHashMap<>();
        private final List<Card> pool = Lists.newArrayList();
        // requirements the creatures from this position of the pool onwards could still fulfill
        private final int[] obtainable;
        private final int unrestrained;
        private boolean anyLimits = false;

        private final Map<Card, GameEntity> attackers = new LinkedHashMap<>();
        private final Map<GameEntity, Integer> toDefender = Maps.newHashMap();
        private int fulfilled = 0;
        private int steps = 0;

        private Map<Card, GameEntity> best = null;
        private int bestViolations;

        private AttackSearch(final int maximum, final int emptyViolations, final int fewestViolations) {
            this.maximum = maximum;
            this.bestViolations = fewestViolations;

            for (final Card c : possibleAttackers) {
                final AttackRestriction restriction = restrictions.get(c);
                final List<Attack> attacks = Lists.newArrayList();
                for (final Pair<GameEntity, Integer> req : requirements.get(c).getSortedRequirements()) {
                    final GameEntity defender = req.getLeft();
                    // a player is never required to pay for an attack
                    if (possibleDefenders.contains(defender) && restriction.canAttack(defender)
                            && CombatUtil.getAttackCost(c.getGame(), c, defender) == null) {
                        attacks.add(new Attack(c, defender, req.getRight()));
                    }
                }
                if (!attacks.isEmpty()) {
                    attacks.sort(Comparator.reverseOrder());
                    options.put(c, attacks);
                }
            }
            // creatures that can't get the company they need won't attack, which might leave others without
            while (options.keySet().removeIf(this::lacksCompany)) {
                continue;
            }

            fillPool();
            obtainable = new int[pool.size() + 1];
            for (int i = pool.size() - 1; i >= 0; i--) {
                obtainable[i] = obtainable[i + 1] + options.get(pool.get(i)).get(0).requirements;
            }
            // violations of an attack that would fulfill everything
            unrestrained = emptyViolations - Sets.newHashSet(playerRequirements.values()).size();

            search(0);
        }

        private boolean lacksCompany(final Card c) {
            for (final StaticAbility stAb : restrictions.get(c).getOthersRestrictions()) {
                if (StaticAbilityAttackBlockRestrict.needsOthers(stAb)) {
                    final int company = Math.min(maximum - 1, StaticAbilityAttackBlockRestrict.countOthers(stAb, c, options.keySet()));
                    if (StaticAbilityAttackBlockRestrict.isViolated(stAb, c, company)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Only creatures that are required to attack matter, plus the ones that could enable them to.
         */
        private void fillPool() {
            final Set<Card> forced = Sets.newHashSet();
            final List<Pair<Card, StaticAbility>> restricted = Lists.newArrayList();
            int needed = 1;
            for (final Card c : options.keySet()) {
                forced.addAll(requirements.get(c).getCausesToAttack().keySet());
                for (final StaticAbility stAb : restrictions.get(c).getOthersRestrictions()) {
                    restricted.add(Pair.of(c, stAb));
                    needed = Math.max(needed, StaticAbilityAttackBlockRestrict.getOthersAmount(stAb, c) + 1);
                    anyLimits |= StaticAbilityAttackBlockRestrict.limitsOthers(stAb);
                }
            }
            needed += Sets.newHashSet(playerRequirements.values()).size();

            // creatures without anything of their own are interchangeable if they'd count for the same restrictions
            final Map<Card, List<Object>> kinds = Maps.newHashMap();
            for (final Card c : options.keySet()) {
                if (options.get(c).get(0).requirements > 0 || restrictions.get(c).isRestrictedByOthers()
                        || !requirements.get(c).getCausesToAttack().isEmpty() || forced.contains(c)) {
                    continue;
                }
                final List<Object> kind = Lists.newArrayList();
                for (final Attack attack : options.get(c)) {
                    kind.add(attack.defender);
                }
                for (final Pair<Card, StaticAbility> r : restricted) {
                    kind.add(StaticAbilityAttackBlockRestrict.countsAsOther(r.getRight(), r.getLeft(), c));
                }
                kinds.put(c, kind);
            }
            final Multiset<List<Object>> kindsInPool = HashMultiset.create();

            final Set<Card> chosen = Sets.newLinkedHashSet();
            final List<Card> helpers = Lists.newArrayList();
            for (final Card c : options.keySet()) {
                if (options.get(c).get(0).requirements > 0) {
                    chosen.add(c);
                } else if (options.get(c).stream().anyMatch(a -> playerRequirements.containsKey(a.defender))) {
                    helpers.add(c);
                }
            }

            boolean changed = true;
            while (changed) {
                for (final Card c : chosen) {
                    for (final Card other : requirements.get(c).getCausesToAttack().keySet()) {
                        if (options.containsKey(other)) {
                            helpers.add(other);
                        }
                    }
                    for (final StaticAbility stAb : restrictions.get(c).getOthersRestrictions()) {
                        if (StaticAbilityAttackBlockRestrict.limitsOthers(stAb)) {
                            continue;
                        }
                        for (final Card other : options.keySet()) {
                            if (StaticAbilityAttackBlockRestrict.countsAsOther(stAb, c, other)) {
                                helpers.add(other);
                            }
                        }
                    }
                }
                changed = false;
                for (final Card helper : helpers) {
                    if (chosen.contains(helper)) {
                        continue;
                    }
                    final List<Object> kind = kinds.get(helper);
                    if (kind != null && kindsInPool.count(kind) >= needed) {
                        continue;
                    }
                    if (kind != null) {
                        kindsInPool.add(kind);
                    }
                    chosen.add(helper);
                    changed = true;
                }
                helpers.clear();
            }

            pool.addAll(chosen);
            // get to the attacks fulfilling the most first
            pool.sort(Comparator.comparing((Card c) -> options.get(c).get(0).requirements).reversed());
        }

        private void search(final int index) {
            if (++steps > MAX_SEARCH_STEPS) {
                // keep what was found until now
                return;
            }
            if (unrestrained - fulfilled - obtainable(index) >= bestViolations || !canBecomeLegal(index)) {
                return;
            }
            if (index == pool.size()) {
                final int violations = countViolations(attackers);
                if (violations != -1 && violations < bestViolations) {
                    best = new LinkedHashMap<>(attackers);
                    bestViolations = violations;
                }
                return;
            }

            final Card c = pool.get(index);
            final List<Attack> attacks = options.get(c);
            final boolean wanted = attacks.get(0).requirements > 0;
            if (!wanted) {
                search(index + 1);
            }
            if (attackers.size() < maximum) {
                for (final Attack attack : attacks) {
                    final Integer defMax = globalRestrictions.getDefenderMax().get(attack.defender);
                    if (defMax != null && toDefender.getOrDefault(attack.defender, 0) >= defMax) {
                        continue;
                    }
                    attackers.put(c, attack.defender);
                    toDefender.merge(attack.defender, 1, Integer::sum);
                    fulfilled += attack.requirements;
                    search(index + 1);
                    fulfilled -= attack.requirements;
                    toDefender.merge(attack.defender, -1, Integer::sum);
                    attackers.remove(c);
                }
            }
            if (wanted) {
                search(index + 1);
            }
        }

        private int obtainable(final int index) {
            if (!anyLimits || attackers.isEmpty()) {
                return obtainable[index];
            }
            // leave out the creatures that the attackers chosen until now don't allow to join
            int result = 0;
            final List<Card> joined = Lists.newArrayList(attackers.keySet());
            for (final Card c : pool.subList(index, pool.size())) {
                final int requirements = options.get(c).get(0).requirements;
                if (requirements == 0) {
                    // the pool has the most required first
                    break;
                }
                joined.add(c);
                if (!exceedsLimit(joined)) {
                    result += requirements;
                }
                joined.remove(joined.size() - 1);
            }
            return result;
        }

        private boolean exceedsLimit(final Collection<Card> together) {
            for (final Card attacker : together) {
                for (final StaticAbility stAb : restrictions.get(attacker).getOthersRestrictions()) {
                    if (StaticAbilityAttackBlockRestrict.limitsOthers(stAb)
                            && StaticAbilityAttackBlockRestrict.isViolated(stAb, attacker, together)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Whether the attackers chosen until now could all be allowed to attack once the remaining creatures
         * of the pool are decided on.
         */
        private boolean canBecomeLegal(final int index) {
            List<Card> potential = null;
            for (final Card attacker : attackers.keySet()) {
                for (final StaticAbility stAb : restrictions.get(attacker).getOthersRestrictions()) {
                    if (StaticAbilityAttackBlockRestrict.limitsOthers(stAb)) {
                        if (StaticAbilityAttackBlockRestrict.isViolated(stAb, attacker, attackers.keySet())) {
                            return false;
                        }
                    } else if (StaticAbilityAttackBlockRestrict.needsOthers(stAb)) {
                        if (potential == null) {
                            potential = Lists.newArrayList(attackers.keySet());
                            potential.addAll(pool.subList(index, pool.size()));
                        }
                        if (StaticAbilityAttackBlockRestrict.isViolated(stAb, attacker, potential)) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
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

        result.sort(Comparator.reverseOrder());

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
            result.sort(Comparator.reverseOrder());
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
        if (!isLegal(attackers)) {
            // Violating a restriction!
            return -1;
        }

        int violations = 0;
        for (final Card possibleAttacker : possibleAttackers) {
            final AttackRequirement requirement = requirements.get(possibleAttacker);
            if (requirement != null) {
                violations += requirement.countViolations(attackers.get(possibleAttacker), attackers, this);
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

    private boolean isLegal(final Map<Card, GameEntity> attackers) {
        if (!globalRestrictions.isLegal(attackers)) {
            return false;
        }
        for (final Entry<Card, GameEntity> attacker : attackers.entrySet()) {
            final AttackRestriction restriction = restrictions.get(attacker.getKey());
            if (restriction != null && !restriction.canAttack(attacker.getValue(), attackers)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether the creature could attack along with the attackers without any restriction being violated.
     */
    boolean canJoin(final Card attacker, final Map<Card, GameEntity> attackers) {
        final Map<Card, GameEntity> joined = Maps.newHashMap(attackers);
        for (final GameEntity defender : possibleDefenders) {
            joined.put(attacker, defender);
            if (isLegal(joined)) {
                return true;
            }
        }
        return false;
    }
}
