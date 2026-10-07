package forge.game.combat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

/**
 * CR 508.1c/d: an attack is legal if it breaks no restriction and obeys as many requirements as
 * any attack that breaks no restriction could. Every scenario is also compared against a brute
 * force over all possible attacks.
 */
public class AttackConstraintsTest extends SimulationTest {

    private Game game;
    private Player attacking;
    private Player defending;
    private Combat combat;

    private void setUp() {
        game = initAndCreateGame();
        attacking = game.getPlayers().get(1);
        defending = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, attacking);
    }

    private Card creature(String name) {
        return creature(name, attacking);
    }

    private Card creature(String name, Player p) {
        Card c = addCard(name, p);
        c.setSickness(false);
        return c;
    }

    private Card goaded(String name) {
        Card c = creature(name);
        c.addGoad(game.getNextTimestamp(), defending);
        return c;
    }

    private void startCombat() {
        game.getAction().checkStateEffects(true);
        combat = new Combat(attacking);
        game.getPhaseHandler().setCombat(combat);
    }

    private boolean declare(Card... attackers) {
        combat.clearAttackers();
        for (Card c : attackers) {
            combat.addAttacker(c, defending);
        }
        return CombatUtil.validateAttackers(combat);
    }

    private void assertLegal(Card... attackers) {
        AssertJUnit.assertTrue("should be a legal attack: " + List.of(attackers), declare(attackers));
    }

    private void assertIllegal(Card... attackers) {
        AssertJUnit.assertFalse("should be an illegal attack: " + List.of(attackers), declare(attackers));
    }

    /** The fewest requirements any restriction-abiding attack has to break, found by trying them all. */
    private int bruteForceBest() {
        final AttackConstraints constraints = combat.getAttackConstraints();
        final List<Card> creatures = new ArrayList<>(attacking.getCreaturesInPlay());
        final List<List<GameEntity>> options = new ArrayList<>();
        for (Card c : creatures) {
            List<GameEntity> defenders = new ArrayList<>();
            for (GameEntity defender : combat.getDefenders()) {
                // a player is never required to pay a cost to attack
                if (CombatUtil.canAttack(c, defender) && CombatUtil.getAttackCost(game, c, defender) == null) {
                    defenders.add(defender);
                }
            }
            options.add(defenders);
        }
        return bruteForce(constraints, creatures, options, 0, new HashMap<>());
    }

    private static int bruteForce(AttackConstraints constraints, List<Card> creatures, List<List<GameEntity>> options,
            int index, Map<Card, GameEntity> attack) {
        if (index == creatures.size()) {
            int violations = constraints.countViolations(attack);
            return violations == -1 ? Integer.MAX_VALUE : violations;
        }
        int best = bruteForce(constraints, creatures, options, index + 1, attack);
        for (GameEntity defender : options.get(index)) {
            attack.put(creatures.get(index), defender);
            best = Math.min(best, bruteForce(constraints, creatures, options, index + 1, attack));
            attack.remove(creatures.get(index));
        }
        return best;
    }

    private void assertSolverIsOptimal(String board) {
        final AttackConstraints constraints = combat.getAttackConstraints();
        final Pair<Map<Card, GameEntity>, Integer> best = constraints.getLegalAttackers();
        AssertJUnit.assertEquals(board + ": the suggested attack " + best.getLeft() + " must itself be legal",
                best.getRight().intValue(), constraints.countViolations(best.getLeft()));
        AssertJUnit.assertTrue(board + ": the suggested attack " + best.getLeft() + " breaks a restriction", best.getRight() >= 0);
        AssertJUnit.assertEquals(board + ": suggested " + best.getLeft(), bruteForceBest(), best.getRight().intValue());
    }

    private void assertSolverIsOptimal() {
        assertSolverIsOptimal("board");
    }

    @Test
    public void requiredAttackerThatCantAttackAloneStaysHomeWithoutCompany() {
        setUp();
        Card flunkies = goaded("Mogg Flunkies");
        startCombat();

        assertLegal();
        assertIllegal(flunkies);
        assertSolverIsOptimal();
    }

    @Test
    public void requiredAttackerThatCantAttackAloneBringsCompany() {
        setUp();
        Card flunkies = goaded("Mogg Flunkies");
        Card bears = creature("Grizzly Bears");
        startCombat();

        assertLegal(flunkies, bears);
        assertIllegal();
        assertIllegal(bears);
        assertIllegal(flunkies);
        assertSolverIsOptimal();
    }

    @Test
    public void tappedCreatureIsNoCompany() {
        setUp();
        goaded("Mogg Flunkies");
        creature("Grizzly Bears").setTapped(true);
        startCombat();

        assertLegal();
        assertSolverIsOptimal();
    }

    @Test
    public void twoCreaturesThatCantAttackAloneAttackTogether() {
        setUp();
        Card first = goaded("Mogg Flunkies");
        Card second = goaded("Mogg Flunkies");
        startCombat();

        assertLegal(first, second);
        assertIllegal();
        assertSolverIsOptimal();
    }

    @Test
    public void needsTwoOtherAttackers() {
        setUp();
        Card conscripts = goaded("Orcish Conscripts");
        Card bears = creature("Grizzly Bears");
        startCombat();
        assertLegal();
        assertIllegal(conscripts, bears);
        assertSolverIsOptimal();

        Card lions = creature("Savannah Lions");
        startCombat();
        assertLegal(conscripts, bears, lions);
        assertIllegal();
        assertIllegal(conscripts, bears);
        assertSolverIsOptimal();
    }

    @Test
    public void needsBlackOrGreenAttacker() {
        setUp();
        Card puma = goaded("Scarred Puma");
        Card lions = creature("Savannah Lions");
        startCombat();
        assertLegal();
        assertIllegal(puma, lions);
        assertSolverIsOptimal();

        Card bears = creature("Grizzly Bears");
        startCombat();
        assertLegal(puma, bears);
        assertIllegal();
        assertSolverIsOptimal();
    }

    @Test
    public void colorWordChangeAppliesToTheRestriction() {
        setUp();
        Card puma = goaded("Scarred Puma");
        Card lions = creature("Savannah Lions");
        Card bears = creature("Grizzly Bears");
        // "black or green" becomes "black or white"
        puma.addChangedTextColorWord("Green", "White", game.getNextTimestamp(), 0);
        startCombat();

        assertLegal(puma, lions);
        assertIllegal(puma, bears);
        assertIllegal();
        assertSolverIsOptimal();
    }

    @Test
    public void needsAttackerWithGreaterPower() {
        setUp();
        Card okk = goaded("Okk");
        Card bears = creature("Grizzly Bears");
        startCombat();
        assertLegal();
        assertIllegal(okk, bears);
        assertSolverIsOptimal();

        Card wurm = creature("Craw Wurm");
        startCombat();
        assertLegal(okk, wurm);
        assertIllegal();
        assertSolverIsOptimal();
    }

    @Test
    public void companyThatItselfNeedsCompany() {
        setUp();
        Card flunkies = goaded("Mogg Flunkies");
        Card okk = creature("Okk");
        startCombat();
        // Okk is the only company for Mogg Flunkies but nothing bigger attacks with Okk
        assertLegal();
        assertIllegal(flunkies, okk);
        assertSolverIsOptimal();

        Card wurm = creature("Craw Wurm");
        wurm.setTapped(true);
        startCombat();
        assertLegal();
        assertSolverIsOptimal();
    }

    @Test
    public void onlyOneOfTwoRequiredAttackersCanAttackAlone() {
        setUp();
        Card master = goaded("Master of Cruelties");
        Card bears = goaded("Grizzly Bears");
        startCombat();

        assertLegal(master);
        assertLegal(bears);
        assertIllegal(master, bears);
        assertIllegal();
        assertSolverIsOptimal();
    }

    @Test
    public void auraMakesCreatureAttackAlone() {
        setUp();
        Card first = creature("Juggernaut");
        Card second = creature("Juggernaut");
        Card errantry = addCard("Errantry", attacking);
        errantry.attachToEntity(first, null);
        startCombat();

        assertLegal(first);
        assertLegal(second);
        assertIllegal(first, second);
        assertIllegal();
        assertSolverIsOptimal();
    }

    @Test
    public void attackerLimitLeavesNoRoomForCompany() {
        setUp();
        creature("Silent Arbiter", defending);
        Card flunkies = goaded("Mogg Flunkies");
        Card bears = goaded("Grizzly Bears");
        startCombat();

        assertLegal(bears);
        assertIllegal(flunkies);
        assertIllegal(flunkies, bears);
        assertIllegal();
        assertSolverIsOptimal();
    }

    @Test
    public void creatureForcedToJoinAnAttack() {
        setUp();
        Card cyclops = creature("Ekundu Cyclops");
        Card bears = creature("Grizzly Bears");
        startCombat();

        assertLegal();
        assertLegal(bears, cyclops);
        assertIllegal(bears);
        assertSolverIsOptimal();
    }

    @Test
    public void creatureIsNotForcedToJoinAnAttackerThatAttacksAlone() {
        setUp();
        creature("Ekundu Cyclops");
        Card master = creature("Master of Cruelties");
        startCombat();

        assertLegal(master);
        assertSolverIsOptimal();
    }

    @Test
    public void attackCostIsNeverRequired() {
        setUp();
        addCard("Propaganda", defending);
        goaded("Mogg Flunkies");
        creature("Grizzly Bears");
        startCombat();

        assertLegal();
        assertSolverIsOptimal();
    }

    @Test
    public void restrictionThatDependsOnCounters() {
        setUp();
        Card pipsqueak = goaded("Pipsqueak, Rebel Strongarm");
        startCombat();
        assertLegal();
        assertIllegal(pipsqueak);
        assertSolverIsOptimal();

        pipsqueak.setCounters(CounterEnumType.P1P1, 1);
        startCombat();
        assertLegal(pipsqueak);
        assertIllegal();
        assertSolverIsOptimal();
    }

    @Test
    public void playerMustAttackWithACreatureThatCantAttackAlone() {
        setUp();
        addCard("Trove of Temptation", defending);
        Card flunkies = creature("Mogg Flunkies");
        startCombat();
        assertLegal();
        assertSolverIsOptimal();

        Card bears = creature("Grizzly Bears");
        startCombat();
        assertLegal(bears);
        assertLegal(flunkies, bears);
        assertIllegal();
        assertSolverIsOptimal();
    }

    @Test(timeOut = 30000)
    public void declareAttackersStepEndsWithALegalAttack() {
        setUp();
        goaded("Mogg Flunkies");
        game.getAction().checkStateEffects(true);
        playUntilPhase(game, PhaseType.MAIN2);
        AssertJUnit.assertEquals(20, defending.getLife());

        setUp();
        goaded("Mogg Flunkies");
        creature("Grizzly Bears");
        game.getAction().checkStateEffects(true);
        playUntilPhase(game, PhaseType.MAIN2);
        AssertJUnit.assertEquals(15, defending.getLife());
    }

    // the plain creatures are in there twice so that boards have several interchangeable ones
    private static final String[] ATTACKERS = {
            "Mogg Flunkies", "Okk", "Scarred Puma", "Orcish Conscripts", "Master of Cruelties", "Bonded Construct",
            "Grizzly Bears", "Savannah Lions", "Craw Wurm", "Juggernaut", "Ekundu Cyclops",
            "Grizzly Bears", "Savannah Lions"
    };

    @Test
    public void randomBoardsMatchBruteForce() {
        final Random random = new Random(9913);
        for (int i = 0; i < 300; i++) {
            setUp();
            final StringBuilder board = new StringBuilder("board ").append(i).append(':');
            if (random.nextInt(5) == 0) {
                creature("Silent Arbiter", defending);
                board.append(" [Silent Arbiter]");
            }
            if (random.nextInt(8) == 0) {
                addCard("Crawlspace", defending);
                board.append(" [Crawlspace]");
            }
            if (random.nextInt(8) == 0) {
                addCard("Trove of Temptation", defending);
                board.append(" [Trove of Temptation]");
            }
            // a second defender triples the attacks to try instead of doubling them
            final boolean planeswalker = random.nextInt(5) == 0;
            if (planeswalker) {
                addCard("Jace Beleren", defending).setCounters(CounterEnumType.LOYALTY, 3);
                board.append(" [Jace Beleren]");
            }
            final int count = 1 + random.nextInt(planeswalker ? 6 : 9);
            for (int j = 0; j < count; j++) {
                final String name = ATTACKERS[random.nextInt(ATTACKERS.length)];
                final Card c = creature(name);
                board.append(' ').append(name);
                if (random.nextInt(5) < 2) {
                    c.addGoad(game.getNextTimestamp(), defending);
                    board.append("(goaded)");
                }
                if (random.nextInt(5) == 0) {
                    c.setTapped(true);
                    board.append("(tapped)");
                }
            }
            startCombat();
            assertSolverIsOptimal(board.toString());
        }
    }

    private Combat blockWith(Card attacker, Card... blockers) {
        game.getAction().checkStateEffects(true);
        combat = new Combat(attacking);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(attacker, defending);
        for (Card blocker : blockers) {
            combat.addBlocker(attacker, blocker);
        }
        return combat;
    }

    @Test
    public void creatureThatCantBlockAloneNeedsAnotherCreature() {
        setUp();
        Card hulk = creature("Craven Hulk", defending);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertFalse(CombatUtil.canBlock(hulk));

        creature("Grizzly Bears", defending);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(CombatUtil.canBlock(hulk));
    }

    @Test
    public void blockRestrictionsLookAtTheOtherBlockers() {
        setUp();
        Card wurm = creature("Craw Wurm");
        Card conscripts = creature("Orcish Conscripts", defending);
        Card okk = creature("Okk", defending);
        Card bears = creature("Grizzly Bears", defending);
        Card lions = creature("Savannah Lions", defending);
        Card bigBlocker = creature("Craw Wurm", defending);

        AssertJUnit.assertNotNull(CombatUtil.validateBlocks(blockWith(wurm, conscripts, bears), defending));
        AssertJUnit.assertNull(CombatUtil.validateBlocks(blockWith(wurm, conscripts, bears, lions), defending));

        AssertJUnit.assertNotNull(CombatUtil.validateBlocks(blockWith(wurm, okk, bears), defending));
        AssertJUnit.assertNull(CombatUtil.validateBlocks(blockWith(wurm, okk, bigBlocker), defending));
    }
}
