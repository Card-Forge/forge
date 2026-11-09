package forge.ai.simulation;

import java.util.List;
import java.util.function.Predicate;

import forge.ai.RunawayLoop;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The loops of {@code ai-combos.txt} that nothing starts by itself: the pieces are on the table and the AI has to
 * go round the loop, step by step, until the opponent is dead. A step is one activation, handed out whenever the
 * stack is empty (so that the triggers of the one before are resolved); the AI's own rollouts cannot see the end of a
 * loop, and the ability logic of the cards (a Walking Ballista that pings when it kills, a Triskelion) never starts
 * it. The loops that the AI does go round by itself (Splinter Twin, Kiki-Jiki, Saheeli Rai) are not here.
 * <ul>
 *   <li><b>Heliod, Sun-Crowned + Walking Ballista</b>: Heliod gives the Ballista lifelink, and every ping gains a
 *       life, which gives the Ballista back the counter it spent (from two counters on: a lone one is grown first).</li>
 *   <li><b>Mikaeus, the Unhallowed + Triskelion</b>: the Triskelion has undying, and shoots itself dead whenever
 *       only two counters are left; it comes back with four, and every counter above two goes at the opponent.</li>
 *   <li><b>Freed from the Real + Bloom Tender</b>: the Tender taps for green and blue, the Freed untaps it for blue:
 *       one green mana more every round, in the mana pool, for a Walking Ballista from the hand with an X as big as
 *       the opponent's life, and then the pings.</li>
 *   <li><b>High Tide + Palinchron + Stroke of Genius</b>: with High Tide cast, every Island taps for two. Palinchron
 *       untaps seven lands when it enters and goes back to the hand for {2}{U}{U}: round after round, the Islands are
 *       tapped, Palinchron is bounced and cast again, and the mana in the pool grows by five. When it covers the
 *       opponent's library, a Stroke of Genius for that much draws all of it: they lose at their next draw.</li>
 * </ul>
 * Every ability is activated at most {@link RunawayLoop#LIMIT} times a turn.
 */
public final class LoopRunner {
    /** A round of the Triskelion is four pings, two of them for the opponent: twenty life take ten rounds. */
    static final int MAX_ACTIVATIONS = 300;

    private LoopRunner() {
    }

    /**
     * The next step of a loop that is on the table, or null. {@code approve} is the AI's own check that the play can
     * be made (cost, no failed cast before); the targets are set here, before it is asked.
     */
    public static SpellAbility next(final Player ai, final List<SpellAbility> candidates,
                                    final Predicate<SpellAbility> approve) {
        if (ai == null || candidates == null || candidates.isEmpty()) {
            return null;
        }
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        Player target = null;
        for (final Player opp : ai.getOpponents()) {
            if (opp.getLife() > 0) {
                target = opp;
                break;
            }
        }
        if (target == null) {
            return null;
        }
        final SpellAbility heliod = heliodAndBallista(ai, candidates, target, approve);
        if (heliod != null) {
            return heliod;
        }
        final SpellAbility triskelion = mikaeusAndTriskelion(ai, candidates, target, approve);
        if (triskelion != null) {
            return triskelion;
        }
        final SpellAbility tender = freedAndBloomTender(ai, candidates, target, approve);
        if (tender != null) {
            return tender;
        }
        return highTideAndPalinchron(ai, candidates, target, approve);
    }

    private static Card inPlay(final Player ai, final String name) {
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    private static SpellAbility activated(final List<SpellAbility> candidates, final Card host, final ApiType api) {
        for (final SpellAbility sa : candidates) {
            if (sa != null && sa.getHostCard() == host && sa.isActivatedAbility() && sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    /** The ability aimed at the entity, when it can be, is under its limit, and the AI approves it. */
    private static SpellAbility aimed(final Player ai, final SpellAbility sa, final forge.game.GameEntity at,
                                      final Predicate<SpellAbility> approve) {
        return aimed(ai, sa, at, approve, RunawayLoop.LIMIT);
    }

    private static SpellAbility aimed(final Player ai, final SpellAbility sa, final forge.game.GameEntity at,
                                      final Predicate<SpellAbility> approve, final int limit) {
        if (sa == null) {
            return null;
        }
        sa.setActivatingPlayer(ai);
        sa.resetTargets();
        if (!sa.canTarget(at) || RunawayLoop.exceeded(sa, limit)) {
            return null;
        }
        sa.getTargets().add(at);
        return approve.test(sa) ? sa : null;
    }

    private static SpellAbility heliodAndBallista(final Player ai, final List<SpellAbility> candidates,
                                                  final Player opp, final Predicate<SpellAbility> approve) {
        final Card heliod = inPlay(ai, "Heliod, Sun-Crowned");
        final Card ballista = inPlay(ai, "Walking Ballista");
        if (heliod == null || ballista == null || ballista.getCounters(CounterEnumType.P1P1) < 1) {
            return null;
        }
        if (ballista.getCounters(CounterEnumType.P1P1) < 2) {
            // the ping that spends the last counter kills the Ballista, and the counter that Heliod's trigger puts on
            // a creature only lands on one that is still there: grow it first ({4}: a +1/+1 counter)
            final SpellAbility grow = activated(candidates, ballista, ApiType.PutCounter);
            if (grow == null) {
                return null;
            }
            grow.setActivatingPlayer(ai);
            grow.resetTargets();
            return !RunawayLoop.exceeded(grow) && approve.test(grow) ? grow : null;
        }
        if (!ballista.hasKeyword(Keyword.LIFELINK)) {
            return aimed(ai, activated(candidates, heliod, ApiType.Pump), ballista, approve);
        }
        return aimed(ai, activated(candidates, ballista, ApiType.DealDamage), opp, approve);
    }

    private static SpellAbility mikaeusAndTriskelion(final Player ai, final List<SpellAbility> candidates,
                                                     final Player opp, final Predicate<SpellAbility> approve) {
        final Card mikaeus = inPlay(ai, "Mikaeus, the Unhallowed");
        final Card triskelion = inPlay(ai, "Triskelion");
        if (mikaeus == null || triskelion == null || !triskelion.hasKeyword(Keyword.UNDYING)
                || triskelion.getCounters(CounterEnumType.P1P1) < 1) {
            return null;
        }
        // Triskelion is a 1/1 and Mikaeus gives it +1/+1, so spending its counters at the opponent never kills it. It has to
        // shoot itself: with n counters left and d damage on it, the last two pings at itself are lethal (d + n >= 2),
        // and undying brings it back with three counters and one more. Every counter above two is for the opponent.
        final int counters = triskelion.getCounters(CounterEnumType.P1P1);
        final boolean face = counters > 2 || triskelion.getDamage() + counters < 2;
        return aimed(ai, activated(candidates, triskelion, ApiType.DealDamage), face ? opp : triskelion, approve,
                MAX_ACTIVATIONS);
    }

    /** Mana an X of the opponent's life needs from the Ballista (XX) and the pool must hold before it is cast. */
    private static int manaNeeded(final Player opp) {
        return Math.min(80, 2 * Math.max(1, opp.getLife()) + 2);
    }

    private static SpellAbility freedAndBloomTender(final Player ai, final List<SpellAbility> candidates,
                                                    final Player opp, final Predicate<SpellAbility> approve) {
        final Card freed = inPlay(ai, "Freed from the Real");
        final Card tender = inPlay(ai, "Bloom Tender");
        if (freed == null || tender == null || freed.getEnchantingCard() != tender) {
            return null;
        }
        final Card ballistaInPlay = inPlay(ai, "Walking Ballista");
        if (ballistaInPlay != null && ballistaInPlay.getCounters(CounterEnumType.P1P1) >= 1) {
            return aimed(ai, activated(candidates, ballistaInPlay, ApiType.DealDamage), opp, approve, MAX_ACTIVATIONS);
        }
        SpellAbility cast = null;
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host != null && sa.isSpell() && host.isInZone(ZoneType.Hand) && ai.equals(host.getController())
                    && host.getName().equals("Walking Ballista")) {
                cast = sa;
                break;
            }
        }
        if (cast == null) {
            return null; // nothing to spend the mana on
        }
        if (ai.getManaPool().totalMana() >= manaNeeded(opp)) {
            cast.setActivatingPlayer(ai);
            cast.resetTargets();
            if (forge.ai.ComputerUtilCost.setMaxXValue(cast, ai, false) >= 1 && approve.test(cast)) {
                return cast;
            }
            return null;
        }
        // one round: the Tender for mana while it is untapped, the Freed to untap it
        if (!tender.isTapped()) {
            for (final SpellAbility sa : candidates) {
                if (sa != null && sa.getHostCard() == tender && sa.isManaAbility()) {
                    sa.setActivatingPlayer(ai);
                    if (!RunawayLoop.exceeded(sa, MAX_ACTIVATIONS) && approve.test(sa)) {
                        return sa;
                    }
                }
            }
            return null;
        }
        final SpellAbility untap = activated(candidates, freed, ApiType.Untap);
        if (untap == null) {
            return null;
        }
        untap.setActivatingPlayer(ai);
        return !RunawayLoop.exceeded(untap, MAX_ACTIVATIONS) && approve.test(untap) ? untap : null;
    }

    private static final String HIGH_TIDE = "High Tide";
    private static final String PALINCHRON = "Palinchron";
    private static final String STROKE = "Stroke of Genius";

    /** Untapped Islands needed before High Tide: it takes one, and the first Palinchron needs five mana after (four Islands, two each). */
    static final int ISLANDS_NEEDED = 5;

    private static boolean tideActive(final Player ai) {
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Command)) {
            // the effect of the spell is a card of the command zone named "High Tide's Effect"
            if (c.getName().startsWith(HIGH_TIDE)) {
                return true;
            }
        }
        return false;
    }

    private static int untappedIslands(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!c.isTapped() && c.getType().hasSubtype("Island")) {
                n++;
            }
        }
        return n;
    }

    private static boolean inHand(final Player ai, final String name) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** The spell of that name from the hand, or null. */
    private static SpellAbility castFromHand(final Player ai, final List<SpellAbility> candidates, final String name) {
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host != null && sa.isSpell() && !sa.isLandAbility() && host.isInZone(ZoneType.Hand)
                    && ai.equals(host.getController()) && host.getName().equals(name)) {
                return sa;
            }
        }
        return null;
    }

    /** Whether the three cards of High Tide, Palinchron and Stroke of Genius are the AI's to go round with. */
    private static boolean tidePieces(final Player ai) {
        return (inHand(ai, HIGH_TIDE) || tideActive(ai)) && inHand(ai, STROKE)
                && (inHand(ai, PALINCHRON) || inPlay(ai, PALINCHRON) != null);
    }

    /**
     * High Tide and Stroke of Genius are not cast on their own while the loop is waiting for its Islands: the AI threw
     * three High Tides and two Strokes away, one by one, before it had the lands to go round with.
     */
    public static boolean held(final Player ai, final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (host == null || !sa.isSpell() || !host.isInZone(ZoneType.Hand) || !ai.equals(host.getController())
                || !(host.getName().equals(HIGH_TIDE) || host.getName().equals(STROKE))) {
            return false;
        }
        return tidePieces(ai) && !tideActive(ai) && untappedIslands(ai) < ISLANDS_NEEDED;
    }

    private static SpellAbility highTideAndPalinchron(final Player ai, final List<SpellAbility> candidates,
                                                      final Player opp, final Predicate<SpellAbility> approve) {
        final SpellAbility stroke = castFromHand(ai, candidates, STROKE);
        final Card palinchron = inPlay(ai, PALINCHRON);
        final SpellAbility recast = castFromHand(ai, candidates, PALINCHRON);
        if (stroke == null || palinchron == null && recast == null) {
            return null;
        }
        final int library = opp.getCardsIn(ZoneType.Library).size();
        if (library < 1 || !opp.canDraw() || opp.cantLoseCheck(forge.game.player.GameLossReason.Milled)
                || opp.isCardInPlay("Laboratory Maniac") || !stroke.canTarget(opp)) {
            return null;
        }
        if (!tideActive(ai)) {
            final SpellAbility tide = castFromHand(ai, candidates, HIGH_TIDE);
            if (tide == null || untappedIslands(ai) < ISLANDS_NEEDED) {
                return null;
            }
            tide.setActivatingPlayer(ai);
            return approve.test(tide) ? tide : null;
        }
        final int pool = ai.getManaPool().totalMana();
        if (pool >= library + 3) {
            stroke.setActivatingPlayer(ai);
            stroke.resetTargets();
            stroke.getTargets().add(opp);
            if (forge.ai.ComputerUtilCost.setMaxXValue(stroke, ai, false) >= library && approve.test(stroke)) {
                return stroke;
            }
        }
        // the Islands first, all of them: what Palinchron untaps is tapped again for two each, which is the gain
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host != null && sa.isManaAbility() && !host.isTapped() && ai.equals(host.getController())
                    && host.getType().hasSubtype("Island")) {
                sa.setActivatingPlayer(ai);
                if (!RunawayLoop.exceeded(sa, MAX_ACTIVATIONS) && approve.test(sa)) {
                    return sa;
                }
            }
        }
        if (palinchron != null) {
            final SpellAbility bounce = activated(candidates, palinchron, ApiType.ChangeZone);
            if (bounce != null && pool >= 9) { // {2}{U}{U} to bounce and {3}{U}{U} to cast again: with less the loop is stuck
                bounce.setActivatingPlayer(ai);
                bounce.resetTargets();
                return !RunawayLoop.exceeded(bounce) && approve.test(bounce) ? bounce : null;
            }
            return null;
        }
        if (pool >= 5) {
            recast.setActivatingPlayer(ai);
            recast.resetTargets();
            return approve.test(recast) ? recast : null;
        }
        return null;
    }
}
