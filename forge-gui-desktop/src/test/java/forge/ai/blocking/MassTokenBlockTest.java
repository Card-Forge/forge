package forge.ai.blocking;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import forge.ai.PlayerControllerAi;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

/**
 * Issue #11810: the AI spent minutes declaring blockers against 66 flying deathtouch tokens
 * plus a few ground creatures, while holding only ground blockers. Every block search asked
 * each blocker about each flier, and each of those asks scanned every static ability in play.
 */
public class MassTokenBlockTest extends SimulationTest {

    private static final String INSECT = "g_1_1_insect_flying_deathtouch";
    private static final List<String> GROUND_ATTACKERS =
            List.of("Hornet Queen", "Craterhoof Behemoth", "Grizzly Bears", "Hill Giant");
    private static final List<String> DEFENDERS = List.of("Grizzly Bears", "Hill Giant", "Llanowar Elves",
            "Centaur Courser", "Runeclaw Bear", "Craw Wurm", "Gray Ogre", "Trained Armodon");

    private Player defender;
    private Combat combat;

    private void setUp(int defenderLife, List<String> attackerNames, int tokens, List<String> defenderNames) {
        Game game = initAndCreateGame();
        Player attacker = game.getPlayers().get(1);
        defender = game.getPlayers().get(0);
        defender.setLife(defenderLife, null);

        List<Card> attacking = new ArrayList<>(addTokens(INSECT, tokens, attacker));
        for (String name : attackerNames) {
            attacking.add(addCard(name, attacker));
        }
        for (String name : defenderNames) {
            addCard(name, defender).setSickness(false);
        }

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, attacker);
        game.getAction().checkStateEffects(true);
        combat = new Combat(attacker);
        for (Card c : attacking) {
            c.setSickness(false);
            combat.addAttacker(c, defender);
        }
    }

    private void declareBlocks() {
        ((PlayerControllerAi) defender.getController()).getAi().declareBlockersFor(defender, combat);
    }

    private void assertNoFlierIsBlocked() {
        for (Card a : combat.getAttackers()) {
            if (a.hasKeyword(Keyword.FLYING)) {
                assertTrue(combat.getBlockers(a).isEmpty(), a + " can't be blocked by ground creatures");
            }
        }
    }

    @Test
    public void lethalEvasiveSwarmStillChumpsTheGroundAttackers() {
        setUp(20, GROUND_ATTACKERS, 66, DEFENDERS);
        declareBlocks();

        assertNoFlierIsBlocked();
        for (Card a : combat.getAttackers()) {
            if (!a.hasKeyword(Keyword.FLYING)) {
                assertFalse(combat.getBlockers(a).isEmpty(), "facing lethal damage, " + a + " should be blocked");
            }
        }
    }

    @Test
    public void nonLethalEvasiveSwarmOnlyMakesSafeBlocks() {
        setUp(200, GROUND_ATTACKERS, 66, DEFENDERS);
        declareBlocks();

        assertNoFlierIsBlocked();
        assertFalse(combat.getAllBlockers().isEmpty(), "there are good blocks to make against the ground attackers");
        for (Card a : combat.getAttackers()) {
            for (Card b : combat.getBlockers(a)) {
                boolean survives = b.getNetToughness() > a.getNetPower();
                boolean kills = b.getNetPower() >= a.getNetToughness();
                assertTrue(survives || kills, "life isn't in danger, so " + b + " shouldn't chump " + a);
            }
        }
    }

    /**
     * The speed-up itself: the block search must not keep asking blockers about attackers none of
     * them can block, nor scan every static ability for a block cost per attacker.
     */
    @Test
    public void unblockableSwarmIsNotSearched() {
        setUp(20, GROUND_ATTACKERS, 66, DEFENDERS);
        AtomicInteger blockCostScans = new AtomicInteger();
        AtomicInteger tokenBlockChecks = new AtomicInteger();

        try (MockedStatic<CombatUtil> ignored = Mockito.mockStatic(CombatUtil.class, invocation -> {
            String name = invocation.getMethod().getName();
            Object[] args = invocation.getArguments();
            if (name.equals("getBlockCost")) {
                blockCostScans.incrementAndGet();
            } else if (name.equals("canBlock") && args.length == 3 && args[0] instanceof Card a
                    && args[2] instanceof Combat && a.isToken()) {
                tokenBlockChecks.incrementAndGet();
            }
            return invocation.callRealMethod();
        })) {
            declareBlocks();
        }

        assertEquals(tokenBlockChecks.get(), 0, "no blocker can block a flying token, so none should be asked to");
        assertTrue(blockCostScans.get() < 100,"block costs were scanned " + blockCostScans + " times");
    }

    /** A blocker with reach can block the fliers, so they must stay in the search. */
    @Test
    public void reachBlockerStillBlocksAFlier() {
        setUp(3, List.of(), 3, List.of("Giant Spider"));
        declareBlocks();

        assertEquals(combat.getAllBlockers().size(), 1, "3 lethal 1/1 fliers, one Giant Spider: it has to chump one");
        assertEquals(combat.getAllBlockers().get(0).getName(), "Giant Spider");
    }

    /** mustBlockAnAttacker now checks lure requirements before block costs; the requirement must still bind. */
    @Test
    public void lureRequirementStillBinds() {
        setUp(20, List.of("Prized Unicorn", "Grizzly Bears"), 0, List.of("Runeclaw Bear"));
        Card bear = combat.getAttackers().stream().filter(c -> c.getName().equals("Grizzly Bears")).findFirst().get();
        Card unicorn = combat.getAttackers().stream().filter(c -> c.getName().equals("Prized Unicorn")).findFirst().get();
        Card blocker = defender.getCreaturesInPlay().get(0);

        assertFalse(CombatUtil.canBlock(bear, blocker, combat), "able to block the Unicorn, so it can't block anything else");
        assertTrue(CombatUtil.canBlock(unicorn, blocker, combat));
        declareBlocks();
        assertEquals(combat.getBlockers(unicorn).size(), 1);
        assertTrue(combat.getBlockers(bear).isEmpty());
    }
}
