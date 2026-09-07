package forge.ai.ability;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Storied (Hobbit / #11282): if you control three or more artifacts, legendaries and/or Sagas,
 * you have an enduring story for the rest of the game. Mirrors Ascend / the city's blessing.
 */
public class StoriedTest extends AITest {

    /** Give a permanent the keyword the way a Storied card would carry it. */
    private Card storiedSource(Player p) {
        // Any permanent can host the keyword; use a vanilla artifact so it also counts toward the three.
        Card c = addCard("Sol Ring", p);
        c.addIntrinsicKeyword("Storied");
        c.updateKeywordsCache();
        return c;
    }

    @Test
    public void notStoriedBelowThree() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        storiedSource(p);                  // artifact #1 (also the Storied source)
        addCard("Sol Ring", p);            // artifact #2
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse("two qualifying permanents is not enough", p.hasEnduringStory());
    }

    @Test
    public void storiedAtThreeArtifacts() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        storiedSource(p);
        addCard("Sol Ring", p);
        addCard("Sol Ring", p);            // three artifacts
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue("three artifacts should grant an enduring story", p.hasEnduringStory());
    }

    @Test
    public void countsArtifactsLegendariesAndSagasTogether() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        storiedSource(p);                          // artifact
        addCard("Sol Ring", p);                    // artifact
        addCard("History of Benalia", p);          // Saga (enchantment)
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue("artifacts and Sagas count together", p.hasEnduringStory());
    }

    @Test
    public void enduringOnceGained() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card src = storiedSource(p);
        Card a2 = addCard("Sol Ring", p);
        Card a3 = addCard("Sol Ring", p);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue(p.hasEnduringStory());

        // drop back below three - the story is kept "for the rest of the game"
        game.getAction().moveTo(ZoneType.Graveyard, a2, null, null);
        game.getAction().moveTo(ZoneType.Graveyard, a3, null, null);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue("enduring story persists below the threshold", p.hasEnduringStory());
        AssertJUnit.assertNotNull(src);
    }

    @Test
    public void legendaryArtifactsCountOnlyOnce() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // Two permanents that are BOTH artifact and legendary. If the union double-counted them
        // (once as artifact, once as legendary) this would wrongly reach three.
        Card src = addCard("Sword of Kaldra", p);
        src.addIntrinsicKeyword("Storied");
        src.updateKeywordsCache();
        addCard("Helm of Kaldra", p);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse("a legendary artifact must count once, not twice", p.hasEnduringStory());

        // adding a third distinct qualifying permanent does reach three
        addCard("Sol Ring", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("third qualifying permanent grants it", p.hasEnduringStory());
    }

    @Test
    public void legendaryNonArtifactsCount() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card src = addCard("Sol Ring", p);              // artifact
        src.addIntrinsicKeyword("Storied");
        src.updateKeywordsCache();
        addCard("Grand Arbiter Augustin IV", p);        // legendary creature
        addCard("Thalia, Guardian of Thraben", p);      // legendary creature
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue("legendary non-artifacts count toward the three", p.hasEnduringStory());
    }

    @Test
    public void opponentDoesNotGetIt() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        storiedSource(p);
        addCard("Sol Ring", p);
        addCard("Sol Ring", p);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(p.hasEnduringStory());
        AssertJUnit.assertFalse("only the Storied card's controller gets it", opp.hasEnduringStory());
    }
}
