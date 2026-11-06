package forge.ai;

import java.util.List;
import java.util.function.Predicate;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.TimedAbilityGuard;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Flat Monte Carlo and the abilities that are only worth their cost at one moment of the turn: creature lands
 * (Mutavault, Stirring Wildwood), Vehicles (crew), Mounts (saddle) and Spacecraft (station).
 *
 * <p>Report: Flat MC kept activating them at odd times (animating a land in the second main phase or at the end of the
 * opponent's turn, crewing after combat, stationing with nothing to gain). Cause: discovery handed the rollouts the
 * plays the heuristics had turned down on timing, and the rollouts cannot see the waste. The rules now live in
 * {@link TimedAbilityGuard}; the first half of this class pins every rule down at the guard (fast, exact), the second
 * half asks the AI for its next play the way the game does, with Flat MC on.
 *
 * <p>Reading a failure: the message names the scenario. A "wasted" expectation that fails means the AI may again spend
 * the mana at a bad moment; an "allowed" one that fails means the guard is too strict and the AI would never use the
 * ability at the moment it is meant for.
 */
public class FlatMonteCarloTimedAbilityTest extends AITest {
    private static final int LONG_TIMEOUT_MS = 240_000;

    private Game game;
    private Player ai;
    private Player opp;

    private void setUp() {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);
    }

    // ------------------------------------------------------------------------------------------------ helpers

    private void phase(final PhaseType phase, final Player turnPlayer) {
        game.getPhaseHandler().devModeSet(phase, turnPlayer);
        game.getAction().checkStateEffects(true);
    }

    /** The opponent's turn at the declare attackers step, with the given creatures attacking the AI. */
    private void opponentAttacks(final Card... attackers) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp, false);
        final Combat combat = new Combat(opp);
        for (final Card attacker : attackers) {
            attacker.setSickness(false);
            combat.addAttacker(attacker, ai);
        }
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
    }

    private SpellAbility ability(final Card c, final Predicate<SpellAbility> wanted) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isAbility() && wanted.test(sa)) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no such ability on " + c);
    }

    private SpellAbility animateOf(final Card c) {
        return ability(c, sa -> sa.getApi() == ApiType.Animate && !sa.isKeyword(Keyword.CREW));
    }

    private SpellAbility crewOf(final Card c) {
        return ability(c, sa -> sa.isKeyword(Keyword.CREW));
    }

    private SpellAbility saddleOf(final Card c) {
        return ability(c, sa -> sa.isKeyword(Keyword.SADDLE));
    }

    private SpellAbility stationOf(final Card c) {
        return ability(c, sa -> sa.isKeyword(Keyword.STATION));
    }

    private String verdict(final SpellAbility sa) {
        return ComboGuard.problem(ai, sa, null, ComboPlan.EMPTY);
    }

    private void resolve(final SpellAbility sa) {
        // the effect itself, without the stack and its priority passing: what the test needs is the resulting board
        sa.setActivatingPlayer(ai);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
    }

    private void assertWasted(final String reason, final String scenario, final SpellAbility sa) {
        AssertJUnit.assertEquals(scenario, reason, verdict(sa));
    }

    private void assertAllowed(final String scenario, final SpellAbility sa) {
        AssertJUnit.assertNull(scenario, verdict(sa));
    }

    private Card manland() {
        final Card vault = addCard("Mutavault", ai);
        vault.setSickness(false);
        return vault;
    }

    private Card vehicle() {
        final Card copter = addCard("Smuggler's Copter", ai);
        copter.setSickness(false);
        return copter;
    }

    private Card spacecraft() {
        final Card ship = addCard("Wurmwall Sweeper", ai);
        ship.setSickness(false);
        return ship;
    }

    // ------------------------------------------------------------------------------------------------ what counts

    @Test
    public void onlyTheTimedAbilitiesAreRecognised() {
        setUp();
        AssertJUnit.assertTrue("Mutavault's animation", TimedAbilityGuard.isTimed(animateOf(manland())));
        AssertJUnit.assertTrue("a Vehicle's crew", TimedAbilityGuard.isTimed(crewOf(vehicle())));
        AssertJUnit.assertTrue("a Mount's saddle", TimedAbilityGuard.isTimed(saddleOf(addCard("Bridled Bighorn", ai))));
        AssertJUnit.assertTrue("a Spacecraft's station", TimedAbilityGuard.isTimed(stationOf(spacecraft())));

        final Card shivan = addCard("Shivan Dragon", ai);
        for (final SpellAbility sa : shivan.getSpellAbilities()) {
            AssertJUnit.assertFalse("a creature pumping itself is not a timed ability: " + sa, TimedAbilityGuard.isTimed(sa));
        }
        final Card vault = addCard("Mutavault", ai);
        for (final SpellAbility sa : vault.getSpellAbilities()) {
            if (sa.isManaAbility()) {
                AssertJUnit.assertFalse("the mana ability of a manland", TimedAbilityGuard.isTimed(sa));
            }
        }
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        AssertJUnit.assertFalse("a spell", TimedAbilityGuard.isTimed(bolt.getSpellAbilities().get(0)));
    }

    @Test
    public void theOpponentsPermanentsAreNotTheGuardsBusiness() {
        setUp();
        final Card theirs = addCard("Mutavault", opp);
        theirs.setSickness(true);
        final SpellAbility animate = ability(theirs, sa -> sa.getApi() == ApiType.Animate);
        phase(PhaseType.MAIN2, ai);
        AssertJUnit.assertNull("not the AI's land", TimedAbilityGuard.problem(ai, animate));
    }

    // ------------------------------------------------------------------------------------------------ manlands

    @Test
    public void manlandIsAnimatedBeforeAttackersAreDeclared() {
        setUp();
        final SpellAbility animate = animateOf(manland());
        phase(PhaseType.MAIN1, ai);
        assertAllowed("first main phase, untapped, no summoning sickness: it can attack", animate);
        phase(PhaseType.COMBAT_BEGIN, ai);
        assertAllowed("beginning of combat: still before attackers", animate);
    }

    @Test
    public void manlandIsNotAnimatedAfterTheAttackIsOver() {
        setUp();
        final SpellAbility animate = animateOf(manland());
        for (final PhaseType p : new PhaseType[] { PhaseType.COMBAT_DECLARE_ATTACKERS, PhaseType.COMBAT_DECLARE_BLOCKERS,
                PhaseType.COMBAT_DAMAGE, PhaseType.COMBAT_END, PhaseType.MAIN2, PhaseType.END_OF_TURN }) {
            phase(p, ai);
            assertWasted("manland-wasted", "own turn, " + p + ": nothing left to attack with it", animate);
        }
    }

    @Test
    public void manlandIsNotAnimatedInTheUpkeepOrDrawStep() {
        setUp();
        final SpellAbility animate = animateOf(manland());
        for (final PhaseType p : new PhaseType[] { PhaseType.UPKEEP, PhaseType.DRAW }) {
            phase(p, ai);
            assertWasted("manland-wasted", "own turn, " + p + ": too early, it would be a creature for nothing", animate);
        }
    }

    @Test
    public void manlandThatCannotAttackIsNotAnimated() {
        setUp();
        final Card vault = manland();
        final SpellAbility animate = animateOf(vault);
        phase(PhaseType.MAIN1, ai);
        vault.setSickness(true);
        assertWasted("manland-wasted", "summoning sick (played this turn)", animate);
        vault.setSickness(false);
        vault.setTapped(true);
        assertWasted("manland-wasted", "tapped (for mana, or by its own attack)", animate);
    }

    @Test
    public void manlandThatIsAlreadyACreatureIsNotAnimatedAgain() {
        setUp();
        final Card vault = manland();
        phase(PhaseType.MAIN1, ai);
        resolve(animateOf(vault));
        AssertJUnit.assertTrue("setup: the land is a creature now", vault.isCreature());
        assertWasted("manland-wasted", "a second animation does nothing", animateOf(vault));
    }

    @Test
    public void manlandWithExtrasIsTreatedTheSame() {
        setUp();
        final Card wildwood = addCard("Stirring Wildwood", ai);
        wildwood.setSickness(false);
        final SpellAbility animate = animateOf(wildwood);
        phase(PhaseType.MAIN1, ai);
        assertAllowed("Stirring Wildwood before combat", animate);
        phase(PhaseType.MAIN2, ai);
        assertWasted("manland-wasted", "Stirring Wildwood after combat (the Reach it grants does not matter)", animate);
    }

    @Test
    public void manlandBlocksAGroundAttacker() {
        setUp();
        final Card vault = manland();
        final SpellAbility animate = animateOf(vault);
        opponentAttacks(addCard("Grizzly Bears", opp));
        assertAllowed("the opponent attacks on the ground: a blocker is worth {1}", animate);
        vault.setTapped(true);
        assertWasted("manland-wasted", "a tapped land cannot block", animate);
    }

    @Test
    public void manlandCannotBlockAFlyer() {
        setUp();
        final SpellAbility animate = animateOf(manland());
        opponentAttacks(addCard("Air Elemental", opp));
        assertWasted("manland-wasted", "the only attacker flies: a 2/2 Mutavault cannot block it", animate);
    }

    @Test
    public void manlandBlocksWhenOneOfSeveralAttackersCanBeBlocked() {
        setUp();
        final SpellAbility animate = animateOf(manland());
        opponentAttacks(addCard("Air Elemental", opp), addCard("Grizzly Bears", opp));
        assertAllowed("the flyer cannot be blocked, the bear can", animate);
    }

    @Test
    public void manlandIsNotAnimatedOnTheOpponentsTurnOutsideTheAttack() {
        setUp();
        final SpellAbility animate = animateOf(manland());
        addCard("Grizzly Bears", opp);
        for (final PhaseType p : new PhaseType[] { PhaseType.UPKEEP, PhaseType.DRAW, PhaseType.MAIN1, PhaseType.COMBAT_BEGIN,
                PhaseType.MAIN2, PhaseType.END_OF_TURN }) {
            phase(p, opp);
            assertWasted("manland-wasted", "opponent's " + p + ": no attackers to block", animate);
        }
    }

    @Test
    public void manlandIsNotAnimatedAfterBlockersAreDeclared() {
        setUp();
        final SpellAbility animate = animateOf(manland());
        opponentAttacks(addCard("Grizzly Bears", opp));
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false);
        assertWasted("manland-wasted", "too late to block once blockers are declared", animate);
    }

    @Test
    public void manlandAtDeclareAttackersWithoutACombatIsNotAnimated() {
        setUp();
        final SpellAbility animate = animateOf(manland());
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp, true);
        assertWasted("manland-wasted", "the step came without any attacker", animate);
    }

    // ------------------------------------------------------------------------------------------------ vehicles

    @Test
    public void vehicleIsCrewedBeforeTheAttack() {
        setUp();
        final SpellAbility crew = crewOf(vehicle());
        addCard("Grizzly Bears", ai);
        phase(PhaseType.MAIN1, ai);
        assertAllowed("first main phase, the Vehicle has been under control since the turn began", crew);
        phase(PhaseType.COMBAT_BEGIN, ai);
        assertAllowed("beginning of combat", crew);
    }

    @Test
    public void vehicleIsNotCrewedAfterTheAttackOrTooEarly() {
        setUp();
        final SpellAbility crew = crewOf(vehicle());
        addCard("Grizzly Bears", ai);
        for (final PhaseType p : new PhaseType[] { PhaseType.UPKEEP, PhaseType.DRAW, PhaseType.COMBAT_DECLARE_ATTACKERS,
                PhaseType.COMBAT_END, PhaseType.MAIN2, PhaseType.END_OF_TURN }) {
            phase(p, ai);
            assertWasted("crew-wasted", "own turn, " + p, crew);
        }
    }

    @Test
    public void vehicleThatCannotAttackIsNotCrewed() {
        setUp();
        final Card copter = vehicle();
        final SpellAbility crew = crewOf(copter);
        addCard("Grizzly Bears", ai);
        phase(PhaseType.MAIN1, ai);
        copter.setSickness(true);
        assertWasted("crew-wasted", "the Vehicle came under control this turn: it cannot attack", crew);
        copter.setSickness(false);
        copter.setTapped(true);
        assertWasted("crew-wasted", "a tapped Vehicle cannot attack", crew);
    }

    @Test
    public void vehicleThatIsAlreadyCrewedIsNotCrewedAgain() {
        setUp();
        final Card copter = vehicle();
        addCard("Grizzly Bears", ai);
        phase(PhaseType.MAIN1, ai);
        resolve(crewOf(copter));
        AssertJUnit.assertTrue("setup: the Vehicle is a creature now", copter.isCreature());
        assertWasted("crew-wasted", "it already is a creature this turn", crewOf(copter));
    }

    @Test
    public void vehicleBlocksOnlyWhenTheOpponentAttacksAndItCanBlock() {
        setUp();
        final Card copter = vehicle();
        final SpellAbility crew = crewOf(copter);
        addCard("Grizzly Bears", ai);
        opponentAttacks(addCard("Air Elemental", opp));
        assertAllowed("the Copter flies: it can block the flyer", crew);
        copter.setTapped(true);
        assertWasted("crew-wasted", "a tapped Vehicle cannot block", crew);
    }

    @Test
    public void vehicleIsNotCrewedOnTheOpponentsTurnWithoutAnAttack() {
        setUp();
        final SpellAbility crew = crewOf(vehicle());
        addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", opp);
        for (final PhaseType p : new PhaseType[] { PhaseType.UPKEEP, PhaseType.DRAW, PhaseType.MAIN1, PhaseType.COMBAT_BEGIN,
                PhaseType.MAIN2, PhaseType.END_OF_TURN }) {
            phase(p, opp);
            assertWasted("crew-wasted", "opponent's " + p + ": nothing to block", crew);
        }
    }

    @Test
    public void vehicleIsNotCrewedAfterBlockersAreDeclared() {
        setUp();
        final SpellAbility crew = crewOf(vehicle());
        addCard("Grizzly Bears", ai);
        opponentAttacks(addCard("Grizzly Bears", opp));
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false);
        assertWasted("crew-wasted", "too late to block", crew);
    }

    // ------------------------------------------------------------------------------------------------ saddle

    @Test
    public void mountIsSaddledBeforeTheAttack() {
        setUp();
        final Card mount = addCard("Bridled Bighorn", ai);
        mount.setSickness(false);
        addCard("Grizzly Bears", ai);
        phase(PhaseType.MAIN1, ai);
        assertAllowed("an untapped Mount that can attack, in the first main phase", saddleOf(mount));
    }

    @Test
    public void mountIsNotSaddledAtTheWrongTimeOrWithoutAReason() {
        setUp();
        final Card mount = addCard("Bridled Bighorn", ai);
        mount.setSickness(false);
        addCard("Grizzly Bears", ai);
        final SpellAbility saddle = saddleOf(mount);
        for (final PhaseType p : new PhaseType[] { PhaseType.COMBAT_BEGIN, PhaseType.COMBAT_DECLARE_ATTACKERS, PhaseType.MAIN2,
                PhaseType.END_OF_TURN }) {
            phase(p, ai);
            assertWasted("saddle-wasted", "own turn, " + p + ": the attack is not ahead any more", saddle);
        }
        phase(PhaseType.MAIN1, opp);
        assertWasted("saddle-wasted", "the opponent's turn: saddle is a sorcery", saddle);

        phase(PhaseType.MAIN1, ai);
        mount.setSickness(true);
        assertWasted("saddle-wasted", "a Mount that cannot attack gains nothing from being saddled", saddle);
        mount.setSickness(false);
        mount.setTapped(true);
        assertWasted("saddle-wasted", "a tapped Mount cannot attack", saddle);
        mount.setTapped(false);
        mount.setSaddled(true);
        assertWasted("saddle-wasted", "already saddled", saddle);
    }

    // ------------------------------------------------------------------------------------------------ stations

    @Test
    public void stationIsUsedAfterCombatWithACreatureToTap() {
        setUp();
        final SpellAbility station = stationOf(spacecraft());
        addCard("Grizzly Bears", ai);
        phase(PhaseType.MAIN2, ai);
        assertAllowed("second main phase: the creature has done its attacking, charge builds up slowly", station);
    }

    @Test
    public void stationBeforeCombatOnlyWhenItReachesTheThreshold() {
        setUp();
        final SpellAbility station = stationOf(spacecraft());
        final Card bears = addCard("Grizzly Bears", ai); // power 2 of the 4 needed
        phase(PhaseType.MAIN1, ai);
        assertWasted("station-wasted", "first main phase, 2 of 4: an attacker lost for a card that is still not a creature", station);
        bears.setSickness(false);
        addCard("Craw Wurm", ai); // power 6: but the cheapest creature is the one that is tapped
        assertWasted("station-wasted", "the weakest creature is tapped for the cost, and that is still only 2 of 4", station);
    }

    @Test
    public void stationBeforeCombatWhenEveryCreatureReachesTheThreshold() {
        setUp();
        final SpellAbility station = stationOf(spacecraft());
        addCard("Craw Wurm", ai);
        phase(PhaseType.MAIN1, ai);
        assertAllowed("first main phase, one 6-power creature turns the Spacecraft into a creature at once", station);
    }

    @Test
    public void stationNeedsACreatureWithPowerToTap() {
        setUp();
        final Card ship = spacecraft();
        final SpellAbility station = stationOf(ship);
        phase(PhaseType.MAIN2, ai);
        assertWasted("station-wasted", "no other creature at all", station);
        addCard("Wall of Omens", ai); // 0/4
        assertWasted("station-wasted", "a creature without power adds no counters", station);
        final Card bears = addCard("Grizzly Bears", ai);
        bears.setTapped(true);
        assertWasted("station-wasted", "the only creature with power is tapped", station);
        bears.setTapped(false);
        assertAllowed("an untapped creature with power", station);
    }

    @Test
    public void stationIsNotUsedOnceTheCardHasAllItsCounters() {
        setUp();
        final Card ship = spacecraft();
        addCard("Grizzly Bears", ai);
        ship.addCounterInternal(CounterEnumType.CHARGE, 4, ai, false, null, null);
        phase(PhaseType.MAIN2, ai);
        assertWasted("station-wasted", "4 of 4 charge counters: more would change nothing", stationOf(ship));
    }

    @Test
    public void stationIsASorceryOfTheAisOwnMainPhase() {
        setUp();
        final SpellAbility station = stationOf(spacecraft());
        addCard("Grizzly Bears", ai);
        for (final PhaseType p : new PhaseType[] { PhaseType.UPKEEP, PhaseType.COMBAT_BEGIN, PhaseType.COMBAT_DECLARE_ATTACKERS,
                PhaseType.COMBAT_END, PhaseType.END_OF_TURN }) {
            phase(p, ai);
            assertWasted("station-wasted", "own turn, " + p, station);
        }
        phase(PhaseType.MAIN2, opp);
        assertWasted("station-wasted", "the opponent's main phase", station);
    }

    // ------------------------------------------------------------------------------------------------ the AI's next play

    /** What the AI does with priority right now: the host card of its next play, or null when it passes. */
    private String nextPlay() {
        final List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        return chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
    }

    private void lands(final String name, final int count) {
        addCards(name, count, ai);
    }

    private void libraries() {
        fillLibrary(ai, 20);
        fillLibrary(opp, 20);
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcDoesNotAnimateAManlandAfterCombat() {
        setUp();
        libraries();
        lands("Island", 3);
        manland();
        addCard("Hill Giant", opp);
        phase(PhaseType.MAIN2, ai);
        final String play = nextPlay();
        AssertJUnit.assertFalse("second main phase: animating Mutavault is a waste" + ", but the AI chose: " + play, "Mutavault".equals(play));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcDoesNotAnimateAManlandThatCannotAttack() {
        setUp();
        libraries();
        lands("Island", 3);
        final Card vault = manland();
        vault.setSickness(true);
        phase(PhaseType.MAIN1, ai);
        final String play = nextPlay();
        AssertJUnit.assertFalse("played this turn: it cannot attack" + ", but the AI chose: " + play, "Mutavault".equals(play));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcDoesNotAnimateAManlandAtTheOpponentsEndStep() {
        setUp();
        libraries();
        lands("Island", 3);
        manland();
        addCard("Hill Giant", opp);
        phase(PhaseType.END_OF_TURN, opp);
        final String play = nextPlay();
        AssertJUnit.assertFalse("the opponent's end step" + ", but the AI chose: " + play, "Mutavault".equals(play));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcDoesNotAnimateAManlandToBlockAFlyer() {
        setUp();
        libraries();
        lands("Island", 3);
        manland();
        opponentAttacks(addCard("Air Elemental", opp));
        final String play = nextPlay();
        AssertJUnit.assertFalse("a land without reach cannot block a flyer" + ", but the AI chose: " + play, "Mutavault".equals(play));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcDoesNotCrewAfterCombat() {
        setUp();
        libraries();
        lands("Island", 3);
        vehicle();
        addCard("Grizzly Bears", ai);
        addCard("Hill Giant", opp);
        phase(PhaseType.MAIN2, ai);
        final String play = nextPlay();
        AssertJUnit.assertFalse("second main phase: crewing the Copter is a waste" + ", but the AI chose: " + play, "Smuggler's Copter".equals(play));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcDoesNotCrewAtTheOpponentsEndStep() {
        setUp();
        libraries();
        lands("Island", 3);
        vehicle();
        addCard("Grizzly Bears", ai);
        phase(PhaseType.END_OF_TURN, opp);
        final String play = nextPlay();
        AssertJUnit.assertFalse("the opponent's end step" + ", but the AI chose: " + play, "Smuggler's Copter".equals(play));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcDoesNotCrewAVehicleThatCameThisTurn() {
        setUp();
        libraries();
        lands("Island", 3);
        final Card copter = vehicle();
        copter.setSickness(true);
        addCard("Grizzly Bears", ai);
        phase(PhaseType.MAIN1, ai);
        final String play = nextPlay();
        AssertJUnit.assertFalse("it cannot attack this turn" + ", but the AI chose: " + play, "Smuggler's Copter".equals(play));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcDoesNotStationAFullSpacecraft() {
        setUp();
        libraries();
        lands("Island", 3);
        final Card ship = spacecraft();
        ship.addCounterInternal(CounterEnumType.CHARGE, 4, ai, false, null, null);
        addCard("Grizzly Bears", ai);
        phase(PhaseType.MAIN2, ai);
        final String play = nextPlay();
        AssertJUnit.assertFalse("nothing left to gain" + ", but the AI chose: " + play, "Wurmwall Sweeper".equals(play));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void flatMcDoesNotStationBeforeCombatForNothing() {
        setUp();
        libraries();
        lands("Island", 3);
        spacecraft();
        addCard("Grizzly Bears", ai).setSickness(false);
        phase(PhaseType.MAIN1, ai);
        final String play = nextPlay();
        AssertJUnit.assertFalse("tapping the only attacker for 2 of 4 counters" + ", but the AI chose: " + play, "Wurmwall Sweeper".equals(play));
    }
}
