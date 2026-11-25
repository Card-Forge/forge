package forge.ai.simulation;

import java.util.List;
import java.util.function.Predicate;

import forge.ai.ComputerUtilCost;
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
        SacMarks.restore(ai);
        // a plan that is turned down takes its sacrifice marks back at once, as does a step that finds nothing to do
        final Predicate<SpellAbility> guarded = sa -> {
            final boolean ok = approve.test(sa);
            if (!ok) {
                SacMarks.restore(ai);
            }
            return ok;
        };
        final SpellAbility step = step(ai, candidates, guarded);
        if (step == null) {
            SacMarks.restore(ai);
        }
        return step;
    }

    private static SpellAbility step(final Player ai, final List<SpellAbility> candidates,
                                     final Predicate<SpellAbility> approve) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        final boolean dip = doubleDip(ai);
        if (!ai.getGame().getStack().isEmpty()
                || !(dip || ph.isPlayerTurn(ai) && (ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2)))) {
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
        if (dip) {
            // the opponent's end step: the lands untap again before the AI's own turn, so what is spent now is free.
            // Time Spiral and the permanents are sorceries, so only the loop (instants) that the solver sees win goes here
            return highTideDeck(ai, candidates, target, approve);
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
        final SpellAbility melira = SacrificeLoopPlan.next(ai, candidates, approve);
        if (melira != null) {
            return melira;
        }
        final SpellAbility helm = ExtraCombatPlan.next(ai, candidates, approve);
        if (helm != null) {
            return helm;
        }
        final SpellAbility triggers = TriggerLoopPlan.next(ai, candidates, approve);
        if (triggers != null) {
            return triggers;
        }
        final SpellAbility flicker = FlickerLoopPlan.next(ai, candidates, approve);
        if (flicker != null) {
            return flicker;
        }
        final SpellAbility breach = BreachPlan.next(ai, candidates, approve);
        if (breach != null) {
            return breach;
        }
        final SpellAbility goblins = GoblinPlan.next(ai, candidates, approve);
        if (goblins != null) {
            return goblins;
        }
        final SpellAbility artifacts = ArtifactLoopPlan.next(ai, candidates, approve);
        if (artifacts != null) {
            return artifacts;
        }
        final SpellAbility rings = StackLoops.next(ai, candidates, approve);
        if (rings != null) {
            return rings;
        }
        final SpellAbility manaLoop = ManaLoopPlan.next(ai, candidates, approve);
        if (manaLoop != null) {
            return manaLoop;
        }
        final SpellAbility palinchron = highTideAndPalinchron(ai, candidates, target, approve);
        if (palinchron != null) {
            return palinchron;
        }
        final SpellAbility elves = ElfLoop.next(ai, candidates, target, approve);
        if (elves != null) {
            return elves;
        }
        final SpellAbility aluren = AlurenPlan.next(ai, candidates, target, approve);
        if (aluren != null) {
            return aluren;
        }
        final SpellAbility upheaval = UpheavalPlan.next(ai, candidates, approve);
        if (upheaval != null) {
            return upheaval;
        }
        final SpellAbility affinity = AffinityPlan.next(ai, candidates, approve);
        if (affinity != null) {
            return affinity;
        }
        final SpellAbility deck = highTideDeck(ai, candidates, target, approve);
        if (deck != null) {
            return deck;
        }
        final SpellAbility refill = tideRefill(ai, candidates, approve);
        if (refill != null) {
            return refill;
        }
        final SpellAbility setup = tideSetup(ai, candidates, approve);
        return setup != null ? setup : tideDig(ai, candidates, approve);
    }

    /**
     * The turns before the one that goes off: a High Tide player casts every cantrip with the mana that is left over
     * (the cheapest first), looking for High Tide, what untaps, the finisher and the lands to go round with. The mana
     * kept back is the price of a counterspell of the hand, and the library is not dug into when it is nearly empty.
     * Not while the loop is running (or being tried in a copy), and not when the hand can already go off.
     */
    private static SpellAbility tideDig(final Player ai, final List<SpellAbility> candidates,
                                        final Predicate<SpellAbility> approve) {
        if (tideActive(ai) || TideSolver.running() || !TidePlan.isTideDeck(ai)
                || ai.getCardsIn(ZoneType.Library).size() < 8 || tideReady(ai)) {
            return null;
        }
        int untapped = ai.getManaPool().totalMana();
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand() && !c.isTapped()) {
                untapped++;
            }
        }
        final int spare = untapped - counterReserve(ai);
        for (final String name : TidePlan.CANTRIPS) {
            final SpellAbility cast = castFromHand(ai, candidates, name);
            if (cast != null && cast.getHostCard().getCMC() <= spare) {
                cast.setActivatingPlayer(ai);
                cast.resetTargets();
                if (approve.test(cast)) {
                    return cast;
                }
            }
        }
        return null;
    }

    static Card inPlay(final Player ai, final String name) {
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    static SpellAbility activated(final List<SpellAbility> candidates, final Card host, final ApiType api) {
        for (final SpellAbility sa : candidates) {
            if (sa != null && sa.getHostCard() == host && sa.isActivatedAbility() && sa.getApi() == api) {
                return sa;
            }
        }
        return null;
    }

    /** The ability aimed at the entity, when it can be, is under its limit, and the AI approves it. */
    static SpellAbility aimed(final Player ai, final SpellAbility sa, final forge.game.GameEntity at,
                                      final Predicate<SpellAbility> approve) {
        return aimed(ai, sa, at, approve, RunawayLoop.LIMIT);
    }

    static SpellAbility aimed(final Player ai, final SpellAbility sa, final forge.game.GameEntity at,
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
            if (ComputerUtilCost.setMaxXValue(cast, ai, false) >= 1 && approve.test(cast)) {
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

    static boolean inHand(final Player ai, final String name) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /** The spell of that name from the hand, or null. */
    public static SpellAbility castFromHand(final Player ai, final List<SpellAbility> candidates, final String name) {
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
        if (ElfLoop.held(ai, sa) || AlurenPlan.pointlessHarpy(ai, sa) || RampPlan.wolfRunWaits(ai, sa) || UpheavalPlan.held(ai, sa)) {
            return true;
        }
        final String landfall = LandfallGuard.fetchProblem(ai, sa);
        if (landfall != null) {
            VetoLog.note(ai, landfall + " | " + (sa.getHostCard() == null ? "?" : sa.getHostCard().getName()));
            return true;
        }
        final Card host = sa == null ? null : sa.getHostCard();
        if (host == null || !sa.isSpell() || !host.isInZone(ZoneType.Hand) || !ai.equals(host.getController())
                || !(host.getName().equals(HIGH_TIDE) || drawsX(host.getName()) || host.getName().equals(BRAIN_FREEZE)
                        || host.getName().equals(TURNABOUT))) {
            return false;
        }
        if (tideDeckPieces(ai) && !tideActive(ai)) {
            return !tideReadyOrSolved(ai);
        }
        if (refillPieces(ai) && !tideActive(ai) && (host.getName().equals(HIGH_TIDE) || host.getName().equals(TURNABOUT))) {
            return !refillReady(ai);
        }
        if ((drawsX(host.getName()) || host.getName().equals(BRAIN_FREEZE)) && !tideActive(ai) && !tidePieces(ai)
                && TidePlan.isTideDeck(ai)) {
            // the finishers are only cast by the loop (at the opponent, or on the AI itself by drawForMana, which counts
            // what it draws): left to the rollouts they went on the AI itself for X=1 when the hand had no High Tide.
            // The loop casts from this list too, so it is not applied while High Tide is in effect or the loop has its pieces
            return true;
        }
        return !host.getName().equals(TURNABOUT) && !host.getName().equals(BRAIN_FREEZE) && tidePieces(ai)
                && !tideActive(ai) && untappedIslands(ai) < ISLANDS_NEEDED;
    }

    /**
     * The finishers of a High Tide deck while High Tide is in effect: the loop casts them at the opponent when the mana
     * is there (see {@link #next}); when it has declined, a Blue Sun's Zenith that draws five for the AI itself shuffles
     * the finisher away, and a Brain Freeze that cannot mill the library is spent for nothing. They stay in the hand
     * (and are kept off the discards by {@link #withoutTideKeys}) for the turn that can win.
     */
    public static boolean keptForTheLoop(final Player ai, final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (host == null || !sa.isSpell() || !host.isInZone(ZoneType.Hand) || !ai.equals(host.getController())
                || !(drawsX(host.getName()) || host.getName().equals(BRAIN_FREEZE))) {
            return false;
        }
        return tideActive(ai) && TidePlan.isTideDeck(ai);
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
            if (ComputerUtilCost.setMaxXValue(stroke, ai, false) >= library && approve.test(stroke)) {
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

    private static final String TURNABOUT = "Turnabout";
    private static final String MIND_OVER_MATTER = "Mind Over Matter";
    private static final String ZENITH = "Blue Sun's Zenith";
    private static final String BRAIN_FREEZE = "Brain Freeze";
    private static final String CANDELABRA = "Candelabra of Tawnos";

    /** Counterspells that cost nothing: they need no mana kept back. */
    private static final java.util.Set<String> FREE_COUNTERS = java.util.Set.of("Force of Will", "Force of Negation",
            "Daze", "Foil", "Pact of Negation");

    private static boolean isCounter(final Card c) {
        final SpellAbility sa = c.getFirstSpellAbility();
        return sa != null && sa.getApi() == ApiType.Counter;
    }

    /**
     * The mana kept back for a counterspell in the hand when the finisher is cast, so that it can be backed up
     * against a counter of the opponent's: the price of the cheapest one, nothing when one is free (Force of Will).
     */
    public static int counterReserve(final Player ai) {
        int cheapest = -1;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (!isCounter(c)) {
                continue;
            }
            if (FREE_COUNTERS.contains(c.getName())) {
                return 0;
            }
            cheapest = cheapest < 0 ? c.getCMC() : Math.min(cheapest, c.getCMC());
        }
        return Math.max(0, cheapest);
    }

    /** A finisher that draws X (Stroke of Genius, Blue Sun's Zenith): {X}{2}{U} or {X}{U}{U}{U}, three mana besides X. */
    private static boolean drawsX(final String name) {
        return name.equals(STROKE) || name.equals(ZENITH);
    }

    private static boolean hasDrawFinisher(final Player ai) {
        return inHand(ai, STROKE) || inHand(ai, ZENITH);
    }

    /** Any card that ends the game here: the two draw spells and Brain Freeze. */
    private static boolean hasFinisher(final Player ai) {
        return hasDrawFinisher(ai) || inHand(ai, BRAIN_FREEZE);
    }

    /** How many High Tides are in effect (each is another blue mana for every Island). */
    static int tideCount(final Player ai) {
        int n = 0;
        for (final Card c : ai.getGame().getCardsIn(ZoneType.Command)) {
            if (c.getName().startsWith(HIGH_TIDE)) {
                n++;
            }
        }
        return n;
    }

    /** Something that untaps the lands: Turnabout, Mind Over Matter, Candelabra of Tawnos (in the hand or in play). */
    private static boolean hasUntapper(final Player ai) {
        return inHand(ai, TURNABOUT) || inHand(ai, MIND_OVER_MATTER) || inPlay(ai, MIND_OVER_MATTER) != null
                || inHand(ai, CANDELABRA) || inPlay(ai, CANDELABRA) != null;
    }

    /** A finisher with High Tide and something to untap the lands, and no Palinchron: the loop of the High Tide decks. */
    private static boolean tideDeckPieces(final Player ai) {
        return (inHand(ai, HIGH_TIDE) || tideActive(ai)) && hasFinisher(ai)
                && !inHand(ai, PALINCHRON) && inPlay(ai, PALINCHRON) == null
                && (tideActive(ai) || hasUntapper(ai)); // with High Tide in effect the untap may be spent already (Time Spiral is exiled)
    }

    /** The cards the loop needs: the finishers, High Tide, what untaps, and the counterspells that protect them. */
    private static boolean isTideKey(final Player ai, final Card c) {
        final String n = c.getName();
        return drawsX(n) || n.equals(BRAIN_FREEZE) || n.equals(HIGH_TIDE) || n.equals(TURNABOUT)
                || n.equals(MIND_OVER_MATTER) || n.equals(CANDELABRA) || n.equals("Time Spiral") || isCounter(c);
    }

    /**
     * The cards of the list that may be thrown away (Mind Over Matter's cost, a discard): never a finisher, High Tide,
     * the untap spells or the counterspells, while the loop is in the hand.
     */
    public static forge.game.card.CardCollection withoutTideKeys(final Player ai, final forge.game.card.CardCollection cards) {
        if (!hasFinisher(ai) || !(inHand(ai, HIGH_TIDE) || tideActive(ai))) {
            return cards;
        }
        final forge.game.card.CardCollection safe = new forge.game.card.CardCollection();
        for (final Card c : cards) {
            if (!isTideKey(ai, c)) {
                safe.add(c);
            }
        }
        return safe;
    }

    private static int opponentLibrary(final Player ai) {
        int library = 0;
        for (final Player opp : ai.getOpponents()) {
            library = Math.max(library, opp.getCardsIn(ZoneType.Library).size());
        }
        return library;
    }

    /** The mana the hand must be able to make: the library to draw and the price of the spell, a margin, and a counterspell's price. */
    private static int tideTarget(final Player ai) {
        if (hasDrawFinisher(ai)) {
            return opponentLibrary(ai) + 3 + 2 + counterReserve(ai);
        }
        return 2 + 2 + counterReserve(ai); // Brain Freeze: the storm count does the rest
    }

    /** Brain Freeze mills three for itself and three for every spell cast before it: whether the spells of the turn are enough. */
    private static boolean stormCovers(final Player ai) {
        final int spells = ai.getSpellsCastThisTurn() + count(ai, HIGH_TIDE) + count(ai, TURNABOUT)
                + count(ai, MIND_OVER_MATTER) + count(ai, CANDELABRA);
        return 3 * (1 + spells) >= opponentLibrary(ai);
    }

    private static int count(final Player ai, final String name) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(name)) {
                n++;
            }
        }
        return n;
    }

    /** The estimate, or the loop played out in a copy of the game (which never holds anything back for the estimate). */
    static boolean tideReadyOrSolved(final Player ai) {
        return TideSolver.running() || tideReady(ai) || TideSolver.wins(ai);
    }

    /** Whether the hand can make what the finisher needs this turn: High Tide, and a loop of untaps with it. */
    static boolean tideReady(final Player ai) {
        if (hasDrawFinisher(ai)) {
            return projectedTideMana(ai) >= tideTarget(ai);
        }
        return stormCovers(ai) && projectedTideMana(ai) >= tideTarget(ai);
    }

    /**
     * What the hand can make this turn with High Tide, as many copies as there are in the hand (each is another mana
     * for every Island, cast before the Islands are tapped): every land taps, each Turnabout untaps all of them and they
     * tap again, Candelabra of Tawnos untaps an Island for each mana paid, and Mind Over Matter turns each card
     * discarded into an untapped Island. An estimate, a little low on purpose: it decides whether the loop is started
     * and whether High Tide, the finishers and Turnabout are kept for the turn.
     */
    public static int projectedTideMana(final Player ai) {
        int islands = 0;
        int others = 0;
        int untappedIslands = 0;
        int untappedOthers = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (!c.isLand()) {
                continue;
            }
            final boolean island = c.getType().hasSubtype("Island");
            if (island) {
                islands++;
            } else {
                others++;
            }
            if (!c.isTapped()) {
                if (island) {
                    untappedIslands++;
                } else {
                    untappedOthers++;
                }
            }
        }
        final int copies = count(ai, HIGH_TIDE);
        final int mult = 1 + tideCount(ai) + copies; // blue mana for every Island once all the High Tides are cast
        int mana = ai.getManaPool().totalMana() + mult * untappedIslands + untappedOthers - 2 * copies;
        final int all = mult * islands + others;
        final int turnabouts = count(ai, TURNABOUT);
        for (int i = 0; i < turnabouts; i++) {
            if (mana >= 4) {
                mana += all - 4;
            }
        }
        if ((inPlay(ai, CANDELABRA) != null || inHand(ai, CANDELABRA) && mana >= 1) && mana >= 1) {
            // {X}, {T}: untap X lands: X Islands cost X and give mult times X
            mana += (mult - 1) * Math.min(islands, mana);
        }
        final boolean momInPlay = inPlay(ai, MIND_OVER_MATTER) != null;
        final boolean momInHand = inHand(ai, MIND_OVER_MATTER);
        if (momInPlay || momInHand) {
            int fodder = 0;
            for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
                if (!isTideKey(ai, c)) {
                    fodder++;
                }
            }
            if (momInPlay) {
                mana += mult * fodder;
            } else if (mana >= 6) {
                mana += mult * fodder - 6;
            }
        }
        return mana;
    }

    /** The first spell of that name in the hand as a play of the list, or null; a copy with the targets cleared. */
    static SpellAbility aimedAtPlayer(final Player ai, final SpellAbility sa, final Player target) {
        sa.setActivatingPlayer(ai);
        sa.resetTargets();
        if (!sa.canTarget(target)) {
            return null;
        }
        sa.getTargets().add(target);
        return sa;
    }

    /**
     * The High Tide decks (Stroke of Genius, Blue Sun's Zenith or Brain Freeze to win; Turnabout, Mind Over Matter,
     * Candelabra of Tawnos to untap, Time Spiral and the cantrips to dig, without Palinchron): as many High Tides as
     * there are, cast before the Islands are tapped, every land for mana, an untap, the Islands tapped again, and the
     * finisher for the opponent's library. Nothing is started unless the hand can make enough mana for it, and
     * the price of a counterspell of the hand is kept back to protect the finisher.
     */
    private static SpellAbility highTideDeck(final Player ai, final List<SpellAbility> candidates, final Player opp,
                                             final Predicate<SpellAbility> approve) {
        if (!tideDeckPieces(ai)) {
            return null;
        }
        final int library = opp.getCardsIn(ZoneType.Library).size();
        if (library < 1 || !opp.canDraw() || opp.cantLoseCheck(forge.game.player.GameLossReason.Milled)
                || opp.isCardInPlay("Laboratory Maniac")) {
            return null;
        }
        if (!tideActive(ai)) {
            final SpellAbility tide = castFromHand(ai, candidates, HIGH_TIDE);
            if (tide == null || !tideReadyOrSolved(ai)) {
                return null;
            }
            tide.setActivatingPlayer(ai);
            return approve.test(tide) ? tide : null;
        }
        final int pool = ai.getManaPool().totalMana();
        final int reserve = counterReserve(ai);
        // the draw spells: one card more than the library when the mana allows, so that they lose at once
        for (final String name : new String[] { STROKE, ZENITH }) {
            final SpellAbility draw = castFromHand(ai, candidates, name);
            if (draw != null && pool >= library + 3 + reserve && aimedAtPlayer(ai, draw, opp) != null) {
                final int x = Math.min(forge.ai.ComputerUtilCost.setMaxXValue(draw, ai, false) - reserve, library + 1);
                if (x >= library) {
                    draw.setXManaCostPaid(x);
                    if (approve.test(draw)) {
                        return draw;
                    }
                }
            }
        }
        // Brain Freeze mills three for itself and each spell cast before it
        final SpellAbility freeze = castFromHand(ai, candidates, BRAIN_FREEZE);
        if (freeze != null && pool >= 2 + reserve && 3 * (1 + ai.getSpellsCastThisTurn()) >= library
                && aimedAtPlayer(ai, freeze, opp) != null && approve.test(freeze)) {
            return freeze;
        }
        // more High Tides while two Islands or more are untapped: each is another mana for every Island tapped after it
        if (untappedIslands(ai) >= 2) {
            final SpellAbility more = castFromHand(ai, candidates, HIGH_TIDE);
            if (more != null) {
                more.setActivatingPlayer(ai);
                if (approve.test(more)) {
                    return more;
                }
            }
        }
        // every land, the Islands for the most each
        SpellAbility other = null;
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host != null && sa.isManaAbility() && host.isLand() && !host.isTapped() && ai.equals(host.getController())) {
                sa.setActivatingPlayer(ai);
                if (RunawayLoop.exceeded(sa, MAX_ACTIVATIONS) || !approve.test(sa)) {
                    continue;
                }
                if (host.getType().hasSubtype("Island")) {
                    return sa;
                }
                other = other == null ? sa : other;
            }
        }
        if (other != null) {
            return other;
        }
        final SpellAbility engine = engineStep(ai, candidates, approve, pool);
        return engine != null ? engine : drawForMana(ai, candidates, approve, pool, reserve);
    }

    /** Finisher cards left to find: the other draw spell and Brain Freeze of the hand, and those of the library. */
    private static int finishersOutside(final Player ai, final String drawn) {
        int n = 0;
        for (final ZoneType zone : new ZoneType[] { ZoneType.Hand, ZoneType.Library }) {
            for (final Card c : ai.getCardsIn(zone)) {
                if (drawsX(c.getName()) || c.getName().equals(BRAIN_FREEZE)) {
                    n++;
                }
            }
        }
        return inHand(ai, drawn) ? n - 1 : n; // the one that is cast does not count
    }

    /**
     * The loop has made all the mana it can and still cannot pay for the finisher: Stroke of Genius or Blue Sun's
     * Zenith is cast on the AI itself with the mana that is floating, to draw into more High Tides, Turnabouts and a
     * Time Spiral (more mana again, which is a better chance at the finisher). Only when another finisher is left to
     * win with, in the hand or the library, and never deeper into the library than leaves a margin before it is empty.
     */
    private static SpellAbility drawForMana(final Player ai, final List<SpellAbility> candidates,
                                            final Predicate<SpellAbility> approve, final int pool, final int reserve) {
        final int own = ai.getCardsIn(ZoneType.Library).size();
        for (final String name : new String[] { ZENITH, STROKE }) {
            final SpellAbility draw = castFromHand(ai, candidates, name);
            if (draw == null || pool < 5 + reserve || finishersOutside(ai, name) < 1 || own < 14
                    || aimedAtPlayer(ai, draw, ai) == null) {
                continue;
            }
            final int x = Math.min(forge.ai.ComputerUtilCost.setMaxXValue(draw, ai, false) - reserve,
                    Math.min(own - 10, 12));
            if (x >= 3) {
                draw.setXManaCostPaid(x);
                if (approve.test(draw)) {
                    return draw;
                }
            }
            draw.resetTargets();
        }
        return null;
    }

    /**
     * What untaps the lands, in the order that makes the most mana: Turnabout (all the lands, every time they are
     * tapped), Candelabra of Tawnos (X Islands for X mana, once, with the lands tapped again), Mind Over Matter (an
     * Island for every card thrown away). Each is cast (once: never a second Mind Over Matter or Candelabra) when it
     * is not in play and the mana is in the pool. Null when there is nothing left to untap with.
     */
    private static SpellAbility engineStep(final Player ai, final List<SpellAbility> candidates,
                                           final Predicate<SpellAbility> approve, final int pool) {
        final SpellAbility turnabout = castFromHand(ai, candidates, TURNABOUT);
        if (turnabout != null && pool >= 4) {
            turnabout.setActivatingPlayer(ai);
            for (SpellAbility node = turnabout; node != null; node = node.getSubAbility()) {
                if (node.usesTargeting()) {
                    node.resetTargets();
                    if (!node.canTarget(ai)) {
                        return null;
                    }
                    node.getTargets().add(ai);
                }
            }
            if (approve.test(turnabout)) {
                return turnabout;
            }
        }
        // Candelabra of Tawnos: pay X, untap X tapped Islands
        final Card candelabra = inPlay(ai, CANDELABRA);
        if (candelabra != null && !candelabra.isTapped() && pool >= 1) {
            final SpellAbility untapX = activated(candidates, candelabra, ApiType.Untap);
            if (untapX != null) {
                final List<Card> tapped = new java.util.ArrayList<>();
                for (final Card island : ai.getCardsIn(ZoneType.Battlefield)) {
                    if (island.isTapped() && island.getType().hasSubtype("Island")) {
                        tapped.add(island);
                    }
                }
                final int x = Math.min(tapped.size(), pool);
                if (x >= 1) {
                    untapX.setActivatingPlayer(ai);
                    untapX.resetTargets();
                    boolean ok = true;
                    for (int i = 0; i < x && ok; i++) {
                        ok = untapX.canTarget(tapped.get(i));
                        if (ok) {
                            untapX.getTargets().add(tapped.get(i));
                        }
                    }
                    if (ok) {
                        untapX.setXManaCostPaid(x);
                        if (approve.test(untapX)) {
                            return untapX;
                        }
                    }
                }
            }
        } else if (candelabra == null && pool >= 1) {
            final SpellAbility cast = castFromHand(ai, candidates, CANDELABRA);
            if (cast != null) {
                cast.setActivatingPlayer(ai);
                cast.resetTargets();
                return approve.test(cast) ? cast : null;
            }
        }
        final Card mom = inPlay(ai, MIND_OVER_MATTER);
        if (mom != null) {
            // a card to throw away that is not a key card: the discard is protected, but with none left it would not be
            boolean fodder = false;
            for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
                fodder |= !isTideKey(ai, c);
            }
            if (fodder) {
                for (final Card island : ai.getCardsIn(ZoneType.Battlefield)) {
                    if (island.isTapped() && island.getType().hasSubtype("Island")) {
                        final SpellAbility untap = aimed(ai, activated(candidates, mom, ApiType.TapOrUntap), island,
                                approve, MAX_ACTIVATIONS);
                        if (untap != null) {
                            return untap;
                        }
                        break;
                    }
                }
            }
        } else if (pool >= 6) {
            final SpellAbility cast = castFromHand(ai, candidates, MIND_OVER_MATTER);
            if (cast != null) {
                cast.setActivatingPlayer(ai);
                cast.resetTargets();
                return approve.test(cast) ? cast : null;
            }
        }
        return timeSpiralStep(ai, candidates, approve, pool);
    }

    /**
     * Time Spiral is the last untap of the turn (a sorcery, so it is cast in the main phase): it costs five, untaps six
     * lands (each Island a mana more for every High Tide), shuffles the graveyard back into the library and draws seven
     * cards, which are more Turnabouts and High Tides, and fodder for Mind Over Matter. It is cast only when what it
     * untaps is worth the five, and only once the cheaper untaps are done with.
     */
    private static SpellAbility timeSpiralStep(final Player ai, final List<SpellAbility> candidates,
                                               final Predicate<SpellAbility> approve, final int pool) {
        if (pool < 5) {
            return null;
        }
        final SpellAbility spiral = castFromHand(ai, candidates, TIME_SPIRAL);
        if (spiral == null) {
            return null;
        }
        int tapped = 0;
        for (final Card land : ai.getCardsIn(ZoneType.Battlefield)) {
            if (land.isLand() && land.isTapped()) {
                tapped++;
            }
        }
        if (tapped < 4) {
            return null;
        }
        spiral.setActivatingPlayer(ai);
        spiral.resetTargets();
        return approve.test(spiral) ? spiral : null;
    }

    private static final String TIME_SPIRAL = "Time Spiral";

    /** A hand without a finisher but with Time Spiral: the draw of seven is what it is played for. */
    private static boolean refillPieces(final Player ai) {
        return !hasFinisher(ai) && inHand(ai, TIME_SPIRAL) && (inHand(ai, HIGH_TIDE) || tideActive(ai))
                && TidePlan.isTideDeck(ai);
    }

    /** The mana the hand makes (High Tides, Turnabouts, Candelabra, Mind Over Matter), to pay for Time Spiral (six) and keep some over. */
    private static int refillMana(final Player ai) {
        return projectedTideMana(ai);
    }

    /** Whether the High Tide turn without a finisher in hand is worth starting: Time Spiral is paid for, or the copy shows a win. */
    static boolean refillReady(final Player ai) {
        return TideSolver.running() || refillMana(ai) >= 8 || TideSolver.wins(ai);
    }

    /**
     * The opponent's end step of a High Tide deck: High Tide, Turnabout and the finishers are instants the lands pay
     * for now, and they untap in the AI's own untap step, so the mana is a second turn's worth (the double dip).
     */
    static boolean doubleDip(final Player ai) {
        final PhaseHandler ph = ai.getGame().getPhaseHandler();
        return !ph.isPlayerTurn(ai) && ph.is(PhaseType.END_OF_TURN) && ai.getGame().getStack().isEmpty()
                && TidePlan.isTideDeck(ai);
    }

    /**
     * High Tide decks without a finisher in the hand, with Time Spiral: every High Tide first (each is another mana for
     * every Island), every land tapped, Turnabout on the lands to tap them again, and with all that mana floating
     * Time Spiral, which untaps six lands more and draws seven new cards, a finisher among them. The hand is shuffled
     * away by it, so Turnabout is cast before it, and it is not cast while the hand holds a finisher.
     */
    private static SpellAbility tideRefill(final Player ai, final List<SpellAbility> candidates,
                                           final Predicate<SpellAbility> approve) {
        if (!refillPieces(ai)) {
            return null;
        }
        if (!tideActive(ai)) {
            final SpellAbility tide = castFromHand(ai, candidates, HIGH_TIDE);
            if (tide == null || !refillReady(ai)) {
                return null;
            }
            tide.setActivatingPlayer(ai);
            return approve.test(tide) ? tide : null;
        }
        if (untappedIslands(ai) >= 2) {
            final SpellAbility more = castFromHand(ai, candidates, HIGH_TIDE);
            if (more != null) {
                more.setActivatingPlayer(ai);
                if (approve.test(more)) {
                    return more;
                }
            }
        }
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host != null && sa.isManaAbility() && host.isLand() && !host.isTapped() && ai.equals(host.getController())) {
                sa.setActivatingPlayer(ai);
                if (!RunawayLoop.exceeded(sa, MAX_ACTIVATIONS) && approve.test(sa)) {
                    return sa;
                }
            }
        }
        final int pool = ai.getManaPool().totalMana();
        final SpellAbility engine = engineStep(ai, candidates, approve, pool);
        if (engine != null) {
            return engine;
        }
        if (pool >= 6) {
            final SpellAbility spiral = castFromHand(ai, candidates, TIME_SPIRAL);
            if (spiral != null) {
                spiral.setActivatingPlayer(ai);
                return approve.test(spiral) ? spiral : null;
            }
        }
        return null;
    }

    /**
     * A second Mind Over Matter or Candelabra of Tawnos is wasted mana: one of them does everything that two would (they
     * untap lands, and each can do it for as long as the mana and cards last). Returns the reason, or null.
     */
    public static String redundantEngine(final Player ai, final SpellAbility sa) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (ai == null || host == null || !sa.isSpell() || !host.isInZone(ZoneType.Hand)) {
            return null;
        }
        final String n = host.getName();
        if ((n.equals(MIND_OVER_MATTER) || n.equals(CANDELABRA)) && inPlay(ai, n) != null) {
            return "duplicate-engine";
        }
        return null;
    }

    /**
     * A High Tide turn that cannot win yet puts its permanents in play for the turn that can: Candelabra of Tawnos for
     * a mana, Mind Over Matter when six mana are left over (one of each, see {@link #redundantEngine}). They are what
     * turns High Tide into an untap loop next turn, and the mana would not be used otherwise.
     */
    private static SpellAbility tideSetup(final Player ai, final List<SpellAbility> candidates,
                                          final Predicate<SpellAbility> approve) {
        if (tideActive(ai) || !TidePlan.isTideDeck(ai) || TideSolver.running()) {
            return null;
        }
        boolean tide = inHand(ai, HIGH_TIDE);
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            tide |= c.getName().equals(HIGH_TIDE);
        }
        if (!tide) {
            return null;
        }
        int untapped = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isLand() && !c.isTapped()) {
                untapped++;
            }
        }
        for (final String name : new String[] { CANDELABRA, MIND_OVER_MATTER }) {
            final int needed = name.equals(CANDELABRA) ? 1 : 8; // six for Mind Over Matter and a margin
            if (untapped >= needed && inPlay(ai, name) == null) {
                final SpellAbility cast = castFromHand(ai, candidates, name);
                if (cast != null) {
                    cast.setActivatingPlayer(ai);
                    cast.resetTargets();
                    if (approve.test(cast)) {
                        return cast;
                    }
                }
            }
        }
        return null;
    }
}
