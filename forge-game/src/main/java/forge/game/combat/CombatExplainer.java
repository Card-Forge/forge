package forge.game.combat;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityAttackRestrict;
import forge.game.staticability.StaticAbilityCantAttackBlock;
import forge.game.staticability.StaticAbilityMustAttack;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;

/**
 * Human readable explanations of why an attack is not allowed.
 * <p>
 * The checks mirror {@link CombatUtil}; every method returns null (or an empty list) when the action is legal.
 */
public final class CombatExplainer {

    private CombatExplainer() {
    }

    // ////////////////////////////////////
    // ////////// ATTACK METHODS //////////
    // ////////////////////////////////////

    /**
     * Explain why the attacker can't attack the defender, mirroring {@link CombatUtil#canAttack(Card, GameEntity)}.
     *
     * @return the reason, or null if the attacker can attack the defender
     */
    public static String whyCantAttack(final Card attacker, final GameEntity defender) {
        if (CombatUtil.canAttack(attacker, defender)) {
            return null;
        }
        final Localizer loc = Localizer.getInstance();

        if (attacker.isBattle() || !attacker.isCreature()) {
            return loc.getMessage("lblWhyAttackNotCreature", attacker);
        }
        if (attacker.isTapped()) {
            return loc.getMessage("lblWhyAttackTapped", attacker);
        }
        if (attacker.isPhasedOut()) {
            return loc.getMessage("lblWhyAttackPhasedOut", attacker);
        }
        if (CombatUtil.isAttackerSick(attacker, defender)) {
            return loc.getMessage("lblWhyAttackSick", attacker);
        }

        if (attacker.isGoaded()) {
            final boolean goadedByDefender = defender instanceof Player && attacker.isGoadedBy((Player) defender);
            if (goadedByDefender || !(defender instanceof Player)) {
                final List<Player> instead = Lists.newArrayList();
                for (GameEntity ge : CombatUtil.getAllPossibleDefenders(attacker.getController())) {
                    if (!ge.equals(defender) && ge instanceof Player p && !attacker.isGoadedBy(p) && CombatUtil.canAttack(attacker, ge)) {
                        instead.add(p);
                    }
                }
                if (!instead.isEmpty()) {
                    return loc.getMessage("lblWhyAttackGoaded", attacker, defender, Lang.joinHomogenous(instead));
                }
            }
        }

        for (final String keyword : List.of("CARDNAME can't attack.", "CARDNAME can't attack or block.")) {
            if (attacker.hasKeyword(keyword)) {
                return describeKeyword(attacker, keyword);
            }
        }
        if (attacker.isDetained()) {
            return loc.getMessage("lblWhyAttackDetained", attacker);
        }
        final StaticAbility stAb = StaticAbilityCantAttackBlock.findCantAttackAbility(attacker, defender);
        if (stAb != null) {
            return loc.getMessage("lblWhyAttackBecauseOf", attacker, defender, describeSource(stAb));
        }
        return loc.getMessage("lblWhyAttackGeneric", attacker, defender);
    }

    /**
     * Explain why the declared attack is invalid, mirroring {@link CombatUtil#validateAttackers(Combat)}.
     *
     * @return the reasons, or an empty list if the attack is valid
     */
    public static List<String> explainInvalidAttack(final Combat combat) {
        if (CombatUtil.validateAttackers(combat)) {
            return Collections.emptyList();
        }
        final Localizer loc = Localizer.getInstance();
        final List<String> result = Lists.newArrayList();
        final AttackConstraints constraints = combat.getAttackConstraints();
        final Map<Card, GameEntity> declared = combat.getAttackersAndDefenders();

        explainGlobalRestrictions(combat, constraints.getGlobalRestrictions(), declared, result);

        // restrictions of the individual attackers
        for (final Map.Entry<Card, GameEntity> e : declared.entrySet()) {
            final Card attacker = e.getKey();
            final AttackRestriction restriction = constraints.getRestrictions().get(attacker);
            if (restriction == null) {
                continue;
            }
            if (!restriction.canAttack(e.getValue())) {
                final String why = whyCantAttack(attacker, e.getValue());
                result.add(why != null ? why : loc.getMessage("lblWhyAttackGeneric", attacker, e.getValue()));
                continue;
            }
            for (final AttackRestrictionType type : restriction.getViolation(declared)) {
                result.add(describeRestriction(attacker, restriction, type));
            }
        }

        if (result.isEmpty()) {
            explainRequirements(constraints, declared, constraints.getLegalAttackers().getLeft(), result);
        }
        if (result.isEmpty()) {
            result.add(loc.getMessage("lblWhyAttackRequirementsGeneric"));
        }
        return result;
    }

    private static final int MAX_SUGGESTIONS = 3;

    /**
     * Describe a few legal attacks close to the declared one: without one of the declared attackers,
     * with additional attackers, and the attack fulfilling the most requirements.
     * Only meant to be shown when the player asks for it.
     */
    public static String suggestLegalAttacks(final Combat combat) {
        final Localizer loc = Localizer.getInstance();
        final AttackConstraints constraints = combat.getAttackConstraints();
        final Map<Card, GameEntity> declared = combat.getAttackersAndDefenders();
        final Pair<Map<Card, GameEntity>, Integer> best = constraints.getLegalAttackers();
        final int maxViolations = best.getRight();

        final List<Map<Card, GameEntity>> candidates = Lists.newArrayList();
        // without one of the declared attackers
        for (final Card attacker : declared.keySet()) {
            final Map<Card, GameEntity> attack = new LinkedHashMap<>(declared);
            attack.remove(attacker);
            candidates.add(attack);
        }
        // with one more attacker
        final Map<Card, GameEntity> additional = getAdditionalAttackers(combat, declared);
        for (final Map.Entry<Card, GameEntity> e : additional.entrySet()) {
            final Map<Card, GameEntity> attack = new LinkedHashMap<>(declared);
            attack.put(e.getKey(), e.getValue());
            candidates.add(attack);
        }
        // with as many more attackers as needed (e.g. "can't attack unless at least two other creatures attack")
        final Map<Card, GameEntity> filled = new LinkedHashMap<>(declared);
        for (final Map.Entry<Card, GameEntity> e : additional.entrySet()) {
            filled.put(e.getKey(), e.getValue());
            if (isLegalAttack(constraints, filled, maxViolations)) {
                candidates.add(filled);
                break;
            }
        }

        final List<Map<Card, GameEntity>> legal = candidates.stream()
                .filter(attack -> isLegalAttack(constraints, attack, maxViolations))
                .distinct()
                .sorted(Comparator.comparingInt(attack -> countDifferences(declared, attack)))
                .limit(MAX_SUGGESTIONS)
                .collect(Collectors.toCollection(Lists::newArrayList));
        // always include the attack fulfilling the most requirements
        if (!legal.contains(best.getLeft())) {
            if (legal.size() == MAX_SUGGESTIONS) {
                legal.remove(MAX_SUGGESTIONS - 1);
            }
            legal.add(best.getLeft());
        }

        final StringBuilder sb = new StringBuilder(loc.getMessage("lblWhyAttackLegalOptions"));
        for (final Map<Card, GameEntity> attack : legal) {
            sb.append("\n- ").append(describeAttack(attack));
        }
        return sb.toString();
    }

    private static boolean isLegalAttack(final AttackConstraints constraints, final Map<Card, GameEntity> attack, final int maxViolations) {
        final int violations = constraints.countViolations(attack);
        return violations != -1 && violations <= maxViolations;
    }

    /**
     * @return the creatures that could additionally attack without paying a cost, mapped to the defender they'd attack
     */
    private static Map<Card, GameEntity> getAdditionalAttackers(final Combat combat, final Map<Card, GameEntity> declared) {
        final Map<Card, GameEntity> result = new LinkedHashMap<>();
        final List<GameEntity> defenders = Lists.newArrayList(declared.values());
        defenders.addAll(combat.getDefenders());
        for (final Card c : combat.getAttackingPlayer().getCreaturesInPlay()) {
            if (declared.containsKey(c) || !combat.getAttackConstraints().getRestrictions().containsKey(c)) {
                continue;
            }
            // prefer a defender already being attacked
            for (final GameEntity defender : defenders) {
                if (CombatUtil.canAttack(c, defender) && CombatUtil.getAttackCost(c.getGame(), c, defender) == null) {
                    result.put(c, defender);
                    break;
                }
            }
        }
        return result;
    }

    private static int countDifferences(final Map<Card, GameEntity> a, final Map<Card, GameEntity> b) {
        final Set<Card> cards = new HashSet<>(a.keySet());
        cards.addAll(b.keySet());
        return (int) cards.stream().filter(c -> !Objects.equals(a.get(c), b.get(c))).count();
    }

    private static void explainGlobalRestrictions(final Combat combat, final GlobalAttackRestrictions global,
            final Map<Card, GameEntity> declared, final List<String> result) {
        if (global.isLegal(declared)) {
            return;
        }
        final Localizer loc = Localizer.getInstance();
        final Integer max = global.getMax();
        if (max != null && declared.size() > max) {
            final List<StaticAbility> sources = StaticAbilityAttackRestrict.attackRestrictSources(combat.getAttackingPlayer().getGame(), null);
            if (sources.isEmpty()) {
                // the maximum is the sum of the limits of each defender
                for (GameEntity defender : global.getDefenderMax().keySet()) {
                    sources.addAll(StaticAbilityAttackRestrict.attackRestrictSources(defender.getGame(), defender));
                }
            }
            result.add(loc.getMessage("lblWhyAttackMaxTotal", max, describeSources(sources), declared.size()));
        }
        for (final Map.Entry<GameEntity, Integer> e : global.getDefenderMax().entrySet()) {
            final GameEntity defender = e.getKey();
            final long count = declared.values().stream().filter(defender::equals).count();
            if (count == 0 || count <= e.getValue()) {
                continue;
            }
            final String sources = describeSources(StaticAbilityAttackRestrict.attackRestrictSources(defender.getGame(), defender));
            if (e.getValue() == 0) {
                result.add(loc.getMessage("lblWhyAttackDefenderForbidden", defender, sources));
            } else {
                result.add(loc.getMessage("lblWhyAttackMaxDefender", e.getValue(), defender, sources, count));
            }
        }
    }

    private static String describeRestriction(final Card attacker, final AttackRestriction restriction, final AttackRestrictionType type) {
        final String keyword = restriction.getKeyword(type);
        if (keyword == null) {
            return Localizer.getInstance().getMessage("lblWhyAttackCantAttack", attacker);
        }
        return describeKeyword(attacker, keyword);
    }

    /**
     * Only explain the requirements the declared attack violates while the best legal attack doesn't,
     * so requirements which can't be fulfilled anyway aren't reported.
     */
    private static void explainRequirements(final AttackConstraints constraints,
            final Map<Card, GameEntity> declared, final Map<Card, GameEntity> best, final List<String> result) {
        final Localizer loc = Localizer.getInstance();
        final Set<String> lines = new LinkedHashSet<>();

        for (final Map.Entry<Card, AttackRequirement> e : constraints.getRequirements().entrySet()) {
            final Card card = e.getKey();
            final AttackRequirement requirement = e.getValue();
            if (!requirement.hasRequirement()) {
                continue;
            }
            final GameEntity declaredDefender = declared.get(card);
            final GameEntity bestDefender = best.get(card);

            // requirements for this creature itself to attack
            if (requirement.countOwnViolations(declaredDefender) > requirement.countOwnViolations(bestDefender)) {
                if (card.isGoaded()) {
                    lines.add(loc.getMessage("lblWhyAttackGoadedMust", card, Lang.joinHomogenous(card.getGoaded())));
                }
                for (final StaticAbility stAb : StaticAbilityMustAttack.mustAttackSources(card)) {
                    lines.add(loc.getMessage("lblWhyAttackMustAttack", card, describeSource(stAb)));
                }
            }

            // other creatures required to attack because this one attacks
            final Multimap<Card, StaticAbility> declaredCauses = requirement.getViolatedCausesToAttack(declaredDefender, declared);
            final Multimap<Card, StaticAbility> bestCauses = requirement.getViolatedCausesToAttack(bestDefender, best);
            if (declaredCauses.size() > bestCauses.size()) {
                for (final Map.Entry<Card, Collection<StaticAbility>> cause : declaredCauses.asMap().entrySet()) {
                    if (bestCauses.containsKey(cause.getKey())) {
                        continue;
                    }
                    lines.add(loc.getMessage("lblWhyAttackMustAttackWith", cause.getKey(), card,
                            describeSources(cause.getValue())));
                }
            }
        }

        // players required to be attacked with at least one creature
        final Multimap<StaticAbility, GameEntity> playerReqs = Multimaps.invertFrom(constraints.getPlayerRequirements(),
                MultimapBuilder.hashKeys().arrayListValues().<StaticAbility, GameEntity>build());
        for (final Map.Entry<StaticAbility, Collection<GameEntity>> e : playerReqs.asMap().entrySet()) {
            final Collection<GameEntity> defenders = e.getValue();
            if (Collections.disjoint(defenders, declared.values()) && !Collections.disjoint(defenders, best.values())) {
                lines.add(loc.getMessage("lblWhyAttackPlayerMustAttack", Lang.joinHomogenous(defenders), describeSource(e.getKey())));
            }
        }

        result.addAll(lines);
    }

    private static String describeAttack(final Map<Card, GameEntity> attack) {
        final Localizer loc = Localizer.getInstance();
        if (attack.isEmpty()) {
            return loc.getMessage("lblWhyAttackNoAttack");
        }
        return attack.entrySet().stream()
                .map(e -> loc.getMessage("lblWhyAttackAssignment", e.getKey(), e.getValue()))
                .collect(Collectors.joining(", "));
    }

    // ////////////////////////////////////
    // ////////// SOURCE HELPERS //////////
    // ////////////////////////////////////

    /**
     * @return the card responsible for the static ability, followed by its text if it has any
     */
    public static String describeSource(final StaticAbility stAb) {
        final String host = String.valueOf(stAb.getHostCard());
        String desc = stAb.toString();
        if (desc.isEmpty() && stAb.getKeyword() != null) {
            desc = stAb.getKeyword().getTitle();
        }
        return desc.isEmpty() ? host : host + ": " + desc;
    }

    /**
     * @return the keyword text of the card, translated like its other ability texts
     */
    public static String describeKeyword(final Card card, final String keyword) {
        return TextUtil.fastReplace(CardTranslation.translateSingleDescriptionText(keyword, card), "CARDNAME", card.toString());
    }

    private static String describeSources(final Collection<StaticAbility> sources) {
        return sources.stream().distinct().map(CombatExplainer::describeSource).collect(Collectors.joining("; "));
    }
}
