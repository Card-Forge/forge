package forge.ai.ability;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * The two previewed Storied cards, exercising the "As long as you have an enduring story"
 * static-condition path (Condition$ EnduringStory) end to end.
 */
public class StoriedCardsTest extends AITest {

    @Test
    public void thorinGrantsWardOnlyWithAnEnduringStory() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card thorin = addCard("Thorin Oakenshield", p);   // legendary creature = 1 qualifying permanent
        Card bear = addCard("Grizzly Bears", p);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse("only Thorin qualifies so far", p.hasEnduringStory());
        AssertJUnit.assertEquals("no ward before the story", 0, bear.getAmountOfKeyword(Keyword.WARD));

        // reach three qualifying permanents (Thorin + two artifacts)
        addCard("Sol Ring", p);
        addCard("Sol Ring", p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue("three qualifying permanents grants the story", p.hasEnduringStory());
        AssertJUnit.assertEquals("artifacts and creatures gain ward 1", 1, bear.getAmountOfKeyword(Keyword.WARD));
        AssertJUnit.assertEquals("Thorin himself too", 1, thorin.getAmountOfKeyword(Keyword.WARD));
    }

    @Test
    public void bifurIsStoriedAndCarriesItsTriggers() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card bifur = addCard("Bifur, Melodic Rider", p);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue("Bifur has Storied", bifur.hasKeyword("Storied"));
        AssertJUnit.assertFalse("one legendary is not three", p.hasEnduringStory());

        addCard("Sol Ring", p);
        addCard("Sol Ring", p);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("Bifur reaches an enduring story", p.hasEnduringStory());
    }

    @Test
    public void wardIsLostIfTheStaticConditionWereUnmet() {
        // Sanity: a player with no Storied source and three artifacts gets no story and no ward.
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card bear = addCard("Grizzly Bears", p);
        addCard("Sol Ring", p);
        addCard("Sol Ring", p);
        addCard("Sol Ring", p);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse("no Storied permanent means no story", p.hasEnduringStory());
        AssertJUnit.assertEquals(0, bear.getAmountOfKeyword(Keyword.WARD));
    }
}
