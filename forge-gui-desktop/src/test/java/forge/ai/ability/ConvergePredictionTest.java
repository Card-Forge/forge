package forge.ai.ability;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import forge.game.card.Card;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * Converge sizes a spell by the colors spent casting it, which are unknown until it is cast. The AI
 * predicts the payment so it does not evaluate every converge effect as if it were empty.
 */
public class ConvergePredictionTest extends AITest {

    @Test
    public void bringToLightSearchesForWhatItCanActuallyAfford() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);

        addCard("Plains", ai);
        addCard("Island", ai);
        addCard("Swamp", ai);
        addCard("Mountain", ai);
        addCard("Forest", ai);
        fillLibrary(ai, 20);
        fillLibrary(opp, 20);
        // mana value 5, so only a legal target once converge counts all five colours - and the
        // library is full of mana value 2 Bears it could settle for instead
        addCardToZone("Serra Angel", ai, ZoneType.Library);

        addCardToZone("Bring to Light", ai, ZoneType.Hand);

        // past turn 3 the AI picks the best creature rather than the cheapest castable one
        for (int i = 0; i < 4; i++) {
            ai.incrementTurn();
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai, false, 4);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        assertEquals("the AI cast Bring to Light", 1,
                countCardsWithName(game, "Bring to Light", ZoneType.Graveyard));

        assertNotNull("it took the best card it could reach, not the cheapest",
                findCardWithName(game, "Serra Angel"));
    }

    @Test
    public void convergeSizesTheSpellThatBoughtTheColours() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);

        for (String l : new String[]{"Plains", "Island", "Swamp", "Mountain", "Forest"}) {
            addCard(l, ai);
        }
        fillLibrary(ai, 15);
        fillLibrary(opp, 15);
        // X is the only thing buying colours here, and nothing else announces it
        addCardToZone("Chamber Sentry", ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);

        Card sentry = findCardWithName(game, "Chamber Sentry");
        assertNotNull("the AI cast Chamber Sentry", sentry);
        assertEquals("sunburst counted all five colours", 5, sentry.getNetToughness());
    }

    /**
     * The token count is a separate SVar from the one X announces, so the per-API override never
     * matched here and PermanentAi's loop is not on this path. Only the prediction sizes it.
     */
    @Test
    public void convergeSizesATokenSpellOnTheXPath() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        for (String l : new String[]{"Plains", "Island", "Swamp", "Mountain", "Forest"}) {
            addCard(l, ai);
        }
        addCard("Island", ai);
        addCard("Island", ai);
        fillLibrary(ai, 15);
        // X U U, and TokenAmount reads Count$Converge off a different SVar than X does
        addCardToZone("Sweep the Skies", ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);

        assertEquals("one thopter per colour spent", 5,
                countCardsWithName(game, "Thopter Token", ZoneType.Battlefield));
    }

    /**
     * DrawAi used to override calculateAmount for Count$Converge because it read zero before
     * anything had been paid. It reads the prediction now, so the override is gone and the spell
     * still has to size itself correctly.
     */
    @Test
    public void convergeSizesADrawSpellWithoutTheAiOverride() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        for (String l : new String[]{"Plains", "Island", "Swamp", "Mountain", "Forest"}) {
            addCard(l, ai);
        }
        fillLibrary(ai, 20);
        // 2B, so the best it can do is black plus two other colours
        addCardToZone("Painful Truths", ai, ZoneType.Hand);

        final int lifeBefore = ai.getLife();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);

        assertEquals("drew one card per colour spent", 3, ai.getCardsIn(ZoneType.Hand).size());
        assertEquals("and paid the same in life", 3, lifeBefore - ai.getLife());
    }
}
