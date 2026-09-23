package forge.ai.blocking;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.testng.annotations.Test;

import forge.ai.PlayerControllerAi;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
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

    private Game game;
    private Player attacker;
    private Player defender;
    private Combat combat;

    private void setUp(int defenderLife, List<String> attackerNames, int tokens, List<String> defenderNames) {
        game = initAndCreateGame();
        attacker = game.getPlayers().get(1);
        defender = game.getPlayers().get(0);
        defender.setLife(defenderLife, null);

        List<Card> attacking = new java.util.ArrayList<>(addTokens(INSECT, tokens, attacker));
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

    private Set<String> declareBlocks() {
        long start = System.nanoTime();
        ((PlayerControllerAi) defender.getController()).getAi().declareBlockersFor(defender, combat);
        long ms = (System.nanoTime() - start) / 1_000_000;

        Set<String> blocks = new TreeSet<>();
        for (Card a : combat.getAttackers()) {
            for (Card b : combat.getBlockers(a)) {
                blocks.add(a.getName() + "<-" + b.getName());
            }
        }
        System.out.println("#11810 life=" + defender.getLife() + " blocks=" + blocks + " took " + ms + " ms");
        return blocks;
    }

    // expected blocks are what the AI chose before the speed-up; only the time spent should change

    @Test
    public void lethalEvasiveSwarmStillChumpsTheGroundAttackers() {
        setUp(20, GROUND_ATTACKERS, 66, DEFENDERS);
        assertEquals(declareBlocks(), Set.of("Craterhoof Behemoth<-Llanowar Elves", "Grizzly Bears<-Runeclaw Bear",
                "Hill Giant<-Craw Wurm", "Hill Giant<-Grizzly Bears"));
    }

    @Test
    public void nonLethalEvasiveSwarmStillMakesGoodBlocks() {
        setUp(200, GROUND_ATTACKERS, 66, DEFENDERS);
        assertEquals(declareBlocks(), Set.of("Grizzly Bears<-Centaur Courser", "Hill Giant<-Craw Wurm"));
    }

    /** A blocker with reach can block the fliers, so they must stay in the search. */
    @Test
    public void reachBlockerStillBlocksAFlier() {
        setUp(3, List.of(), 3, List.of("Giant Spider"));
        Set<String> blocks = declareBlocks();
        assertEquals(blocks.size(), 1, "3 lethal 1/1 fliers, one Giant Spider: it has to chump one");
        assertTrue(blocks.iterator().next().endsWith("<-Giant Spider"));
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
        assertEquals(declareBlocks(), Set.of("Prized Unicorn<-Runeclaw Bear"));
    }
}
