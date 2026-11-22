package forge.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * The attack search: the declaration is made by the real phase handler, the AI compares attack sets with rollouts
 * (the opponent's heuristic AI blocks in the copy) and the tests look at who ends up attacking.
 */
public class FlatMonteCarloAttackTest extends AITest {
    private static final int LONG_TIMEOUT_MS = 240_000;

    private Game newGame() {
        final Game game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_FLAT_MONTE_CARLO_MAX = true; // a generous budget: the tests are about the choice, not about speed
        game.AI_TIMEOUT = 20;
        game.getPlayers().get(1).setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        return game;
    }

    private static Card ready(final Card c) {
        c.setSickness(false);
        return c;
    }

    /** Moves the game into the AI's declare attackers step (the AI declares in there) and returns who attacks, sorted. */
    private List<String> attackers(final Game game, final boolean search) {
        final Player ai = game.getPlayers().get(1);
        final AiController brain = ((PlayerControllerAi) ai.getController()).getAi();
        brain.setFlatMonteCarloAttackOverride(search);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
            addCardToZone("Plains", game.getPlayers().get(0), ZoneType.Library);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        game.getPhaseHandler().devAdvanceToPhase(PhaseType.COMBAT_DECLARE_ATTACKERS);
        final List<String> names = new ArrayList<>();
        if (game.getCombat() != null) {
            for (final Card c : game.getCombat().getAttackers()) {
                names.add(c.getName());
            }
        }
        Collections.sort(names);
        if (System.getProperty("flatmc.debug") != null) {
            System.out.println("   attack -> " + names + " search=" + search + " result: " + brain.getLastFlatMonteCarloResult());
        }
        return names;
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void attacksWithEverythingForLethal() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        game.getPlayers().get(0).setLife(5, null);
        ready(addCard("Hill Giant", ai));
        ready(addCard("Grizzly Bears", ai));
        AssertJUnit.assertEquals("3 + 2 damage is exactly lethal", List.of("Grizzly Bears", "Hill Giant"), attackers(game, true));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void doesNotSendAFlimsyCreatureIntoABigBlocker() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        ready(addCard("Savannah Lions", ai));
        ready(addCard("Craw Wurm", game.getPlayers().get(0)));
        AssertJUnit.assertEquals(List.of(), attackers(game, true));
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void alphaStrikeWhenTheBlockerCannotStopLethal() {
        final Game game = newGame();
        final Player ai = game.getPlayers().get(1);
        game.getPlayers().get(0).setLife(6, null);
        for (int i = 0; i < 3; i++) {
            ready(addCard("Hill Giant", ai));
        }
        ready(addCard("Elite Vanguard", game.getPlayers().get(0)));
        AssertJUnit.assertEquals("one chump block leaves 6 damage, exactly lethal", 3, attackers(game, true).size());
    }
}
