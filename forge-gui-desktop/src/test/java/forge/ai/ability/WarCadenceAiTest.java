package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class WarCadenceAiTest extends AITest {

    @Test
    public void taxesBlockersForALethalSwing() {
        Game game = boardWithWarCadence(18, null);

        playUntilPhase(game, PhaseType.COMBAT_DECLARE_ATTACKERS);

        assertTrue(taxIsUp(game));
    }

    @Test
    public void doesNotPayIntoAFog() {
        Game game = boardWithWarCadence(18, "Kami of False Hope");

        playUntilPhase(game, PhaseType.COMBAT_DECLARE_ATTACKERS);

        assertFalse(taxIsUp(game));
    }

    private Game boardWithWarCadence(int oppLife, String oppExtra) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCard("War Cadence", ai);
        addCards("Mountain", 6, ai);
        addCards("Colossal Dreadmaw", 3, ai);
        addCards("Colossal Dreadmaw", 2, opp); // blockers, so the tax has something to bite on
        if (oppExtra != null) {
            addCard(oppExtra, opp);
        }
        opp.setLife(oppLife, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            c.setSickness(false);
        }
        return game;
    }

    private boolean taxIsUp(Game game) {
        for (Card c : game.getCardsIn(ZoneType.Command)) {
            if (c.getName().startsWith("War Cadence") && c.getName().endsWith("'s Effect")) {
                return true;
            }
        }
        return false;
    }
}
