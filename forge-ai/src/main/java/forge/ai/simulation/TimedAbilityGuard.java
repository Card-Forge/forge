package forge.ai.simulation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.ai.ability.AnimateAi;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.zone.ZoneType;

/**
 * Abilities that are only worth their cost at one moment of the turn, and wasted at any other: a creature land
 * (Mutavault, Stirring Wildwood), a Vehicle's crew, a Mount's saddle and a Spacecraft's station.
 *
 * <p>The ordinary heuristics know these timings (see {@code AnimateAi} and {@code CountersPutAi}), but Flat Monte Carlo
 * adds discovered plays and weighs them with rollouts, and the board evaluation cannot see why animating a land in the
 * second main phase is a waste: nothing is lost in the rollout's score, so the play looks free. This guard states the
 * timing rules in one place, for both the plays the heuristics approved and the plays discovery added.
 *
 * <ul>
 *   <li><b>Animate yourself until end of turn</b> (manland, artifact that becomes a creature) and <b>Crew</b>: on the AI's
 *   turn only before attackers are declared (first main phase, beginning of combat) and only when the permanent is
 *   untapped and not summoning sick; on the opponent's turn only once attackers are declared, when it is untapped and
 *   can block one of the creatures attacking the AI. Never when it already is a creature.</li>
 *   <li><b>Saddle</b>: a sorcery-speed ability, for attacking: first main phase of the AI's turn, the Mount untapped,
 *   not summoning sick and not saddled yet.</li>
 *   <li><b>Station</b>: a sorcery-speed ability that taps a creature: in the AI's main phase with an empty stack, with
 *   a creature to tap and counters still to gain. In the first main phase only when the tap reaches a threshold (the
 *   tapped creature is one that would have attacked), otherwise after combat.</li>
 * </ul>
 */
public final class TimedAbilityGuard {
    private TimedAbilityGuard() {
    }

    /** The kinds of ability with one good moment. */
    enum Kind { MANLAND, CREW, SADDLE, STATION }

    /** Charge counters a station card needs, as written in its static abilities ("counters_GE12_CHARGE"). */
    private static final Pattern CHARGE_THRESHOLD = Pattern.compile("counters_GE(\\d+)_CHARGE");

    /** Which kind of timed ability this is, or null for any other play. */
    static Kind kindOf(final SpellAbility sa) {
        if (sa == null || !sa.isAbility() || sa.getHostCard() == null) {
            return null;
        }
        final SpellAbility root = sa.getRootAbility();
        if (root.isKeyword(Keyword.CREW) || sa.isKeyword(Keyword.CREW)) {
            return Kind.CREW;
        }
        if (root.isKeyword(Keyword.SADDLE) || sa.isKeyword(Keyword.SADDLE)) {
            return Kind.SADDLE;
        }
        if (root.isKeyword(Keyword.STATION) || sa.isKeyword(Keyword.STATION)) {
            return Kind.STATION;
        }
        if (isSelfAnimate(root)) {
            return Kind.MANLAND;
        }
        return null;
    }

    /**
     * Whether the play is one of the timed abilities. The heuristics' verdict on such a play is final: when they turn it
     * down, discovery must not hand it to the rollouts again.
     */
    public static boolean isTimed(final SpellAbility sa) {
        return kindOf(sa) != null;
    }

    /** "Until end of turn, this permanent becomes a creature": a manland, or an artifact that animates itself. */
    private static boolean isSelfAnimate(final SpellAbility root) {
        final Card host = root.getHostCard();
        if (root.getApi() != ApiType.Animate || root.usesTargeting() || host == null) {
            return false;
        }
        if (!"Self".equals(root.getParamOrDefault("Defined", "Self")) || "Permanent".equals(root.getParam("Duration"))) {
            return false; // Awaken-style permanent changes are not a matter of timing
        }
        // a creature that pumps itself (Figure of Destiny, a firebreather) is a pump, not an animation; a land that
        // already is a creature stays on the list so that a second animation is refused
        return host.isLand() || !host.isCreature();
    }

    /**
     * Why the play should not be made now, or null when its timing is fine (or it is not a timed ability).
     * The reasons are "manland-wasted", "crew-wasted", "saddle-wasted" and "station-wasted".
     */
    public static String problem(final Player ai, final SpellAbility sa) {
        try {
            return problemImpl(ai, sa);
        } catch (final RuntimeException e) {
            return null; // a refinement only: never let it break a turn
        }
    }

    private static String problemImpl(final Player ai, final SpellAbility sa) {
        final Kind kind = kindOf(sa);
        if (kind == null || ai == null) {
            return null;
        }
        final Card host = sa.getHostCard();
        if (!ai.equals(host.getController()) || !host.isInZone(ZoneType.Battlefield)) {
            return null; // not the AI's permanent, or not in play: nothing this guard can say
        }
        final boolean wasted;
        switch (kind) {
            case MANLAND:
            case CREW:
                wasted = animationWasted(ai, sa, host, kind);
                break;
            case SADDLE:
                wasted = saddleWasted(ai, host);
                break;
            case STATION:
                wasted = stationWasted(ai, host);
                break;
            default:
                wasted = false;
                break;
        }
        if (!wasted) {
            return null;
        }
        switch (kind) {
            case CREW:
                return "crew-wasted";
            case SADDLE:
                return "saddle-wasted";
            case STATION:
                return "station-wasted";
            default:
                return "manland-wasted";
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // manlands and crew

    private static boolean animationWasted(final Player ai, final SpellAbility sa, final Card host, final Kind kind) {
        if (host.isCreature()) {
            return true; // already a creature this turn (a crewed Vehicle, an animated land)
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final Card animated = animatedCopy(sa, host);
        if (ph.isPlayerTurn(ai)) {
            // the permanent has to attack: before attackers are declared, untapped, and free of summoning sickness
            // (a Vehicle that came under the AI's control this turn cannot attack either)
            final boolean timing = ph.is(PhaseType.MAIN1) || ph.is(PhaseType.COMBAT_BEGIN);
            return !timing || host.isTapped() || sick(host, animated);
        }
        // the opponent's turn: to block, once their attackers are declared and before blockers are
        final Combat combat = ph.getCombat();
        if (!ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS) || combat == null || host.isTapped()) {
            return true;
        }
        boolean blocksSomething = false;
        for (final Card attacker : combat.getAttackers()) {
            if (!ai.equals(combat.getDefenderPlayerByAttacker(attacker))) {
                continue; // attacking another player (a team game): nothing to block for the AI
            }
            if (animated == null || CombatUtil.canBlock(attacker, animated)) {
                blocksSomething = true;
                break;
            }
        }
        return !blocksSomething;
    }

    /** The permanent as it would be once the ability resolved (its haste, power and keywords count), or null. */
    private static Card animatedCopy(final SpellAbility sa, final Card host) {
        try {
            if (sa.getActivatingPlayer() == null) {
                return null;
            }
            return AnimateAi.becomeAnimated(host, sa.getRootAbility());
        } catch (final RuntimeException e) {
            return null; // not worth failing the guard for: the plain card is judged instead
        }
    }

    private static boolean sick(final Card host, final Card animated) {
        return animated != null ? animated.isSick() : host.hasSickness();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // saddle

    private static boolean saddleWasted(final Player ai, final Card host) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (host.isSaddled() || host.isTapped()) {
            return true; // saddled already, or a tapped Mount cannot attack
        }
        if (!ph.isPlayerTurn(ai) || !ph.is(PhaseType.MAIN1) || !ai.getGame().getStack().isEmpty()) {
            return true; // only as a sorcery, and only worth it before the attack
        }
        return host.hasSickness(); // a Mount that cannot attack is not helped by being saddled
    }

    // ---------------------------------------------------------------------------------------------------------------
    // station

    private static boolean stationWasted(final Player ai, final Card host) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))
                || !ai.getGame().getStack().isEmpty()) {
            return true; // only as a sorcery
        }
        final int charge = host.getCounters(CounterEnumType.CHARGE);
        if (charge >= highestThreshold(host)) {
            return true; // nothing more to gain
        }
        // the creature the AI taps is its weakest one with power: it is what the cost is paid with
        int weakest = Integer.MAX_VALUE;
        for (final Card c : CardLists.filter(ai.getCreaturesInPlay(), x -> !x.equals(host) && x.isUntapped())) {
            if (c.getNetPower() > 0) {
                weakest = Math.min(weakest, c.getNetPower());
            }
        }
        if (weakest == Integer.MAX_VALUE) {
            return true; // nothing to tap, or only creatures without power
        }
        if (ph.is(PhaseType.MAIN1)) {
            // tapping before combat takes an attacker away: only for a step up (the card becomes a creature, or reaches
            // its next threshold); the slow build-up is done after combat
            return !reachesThreshold(host, charge, charge + weakest);
        }
        return false;
    }

    /** Whether the counters going from {@code from} to {@code to} cross one of the thresholds of the card. */
    static boolean reachesThreshold(final Card host, final int from, final int to) {
        for (final int t : thresholds(host)) {
            if (from < t && to >= t) {
                return true;
            }
        }
        return false;
    }

    private static int highestThreshold(final Card host) {
        int max = 0;
        for (final int t : thresholds(host)) {
            max = Math.max(max, t);
        }
        return max;
    }

    /**
     * The charge counts at which the card changes: those named in its static abilities.
     */
    static int[] thresholds(final Card host) {
        final java.util.TreeSet<Integer> found = new java.util.TreeSet<>();
        for (final StaticAbility st : host.getStaticAbilities()) {
            for (final String key : new String[] { "Affected", "Condition", "IsPresent" }) {
                final String value = st.getParam(key);
                if (value == null) {
                    continue;
                }
                final Matcher m = CHARGE_THRESHOLD.matcher(value);
                while (m.find()) {
                    found.add(Integer.parseInt(m.group(1)));
                }
            }
        }
        final int[] result = new int[found.size()];
        int i = 0;
        for (final int t : found) {
            result[i++] = t;
        }
        return result;
    }
}
