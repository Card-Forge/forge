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
 * A free {T} untap with AILogic$ PoolExtraMana is worth its tap when reusing a mana source reaches
 * a spell we cannot otherwise pay for, and not otherwise.
 */
public class UntapAiTest extends AITest {

    /**
     * Four Forests, two of them tapped, and a three-drop in hand: the AI cannot pay right now but
     * untapping a Forest gets it there. The old gate asked whether the AI had enough mana *sources*
     * counting tapped ones, so this - the case the card exists for - never fired.
     */
    @Test
    public void rampsWhenTappedOutButSourceRich() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Voyaging Satyr", ai).setSickness(false);
        addCard("Forest", ai);
        addCard("Forest", ai);
        addCard("Forest", ai).setTapped(true);
        addCard("Forest", ai).setTapped(true);
        addCardToZone("Centaur Courser", ai, ZoneType.Hand); // {2}{G}
        fillLibrary(ai, 10);

        moveToMain2(game, ai);
        gameLoopUntilNextPhase(game);

        AssertJUnit.assertEquals("AI should have untapped a Forest to reach the three-drop", 1,
                countCardsWithName(game, "Centaur Courser"));
    }

    /** With nothing in hand the untap reaches nothing, so the Satyr should keep its tap. */
    @Test
    public void holdsWhenNothingToRampInto() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card satyr = addCard("Voyaging Satyr", ai);
        satyr.setSickness(false);
        addCard("Forest", ai);
        addCard("Forest", ai);
        addCard("Forest", ai).setTapped(true);
        addCard("Forest", ai).setTapped(true);
        fillLibrary(ai, 10);

        moveToMain2(game, ai);
        gameLoopUntilNextPhase(game);

        AssertJUnit.assertTrue("Voyaging Satyr should not have tapped itself for nothing",
                satyr.isUntapped());
    }

    /**
     * Kiora's Follower can untap any permanent, so the untap decision and the target choice have to
     * agree: the reason is mana, so the target must be the mana source and not the most expensive
     * tapped permanent on the board.
     */
    @Test
    public void untapsTheManaSourceNotTheBiggestPermanent() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Kiora's Follower", ai).setSickness(false);
        addCard("Forest", ai);
        addCard("Forest", ai);
        Card tappedLand = addCard("Forest", ai);
        tappedLand.setTapped(true);
        Card fatty = addCard("Colossal Dreadmaw", ai);
        fatty.setTapped(true);
        fatty.setSickness(false);
        addCardToZone("Centaur Courser", ai, ZoneType.Hand); // {2}{G}
        fillLibrary(ai, 10);

        moveToMain2(game, ai);
        gameLoopUntilNextPhase(game);

        AssertJUnit.assertEquals("AI should have untapped the Forest and cast the three-drop", 1,
                countCardsWithName(game, "Centaur Courser"));
        AssertJUnit.assertTrue("the Dreadmaw was untapped instead of the mana source",
                fatty.isTapped());
    }

    /**
     * Past declare blockers on the opponent's turn the lands untap on their own next turn anyway,
     * so untapping only pays for something we can hold up. With an empty hand there is nothing.
     */
    @Test
    public void holdsOnOpponentsTurnWithNothingToHoldUp() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card satyr = addCard("Voyaging Satyr", ai);
        satyr.setSickness(false);
        addCard("Forest", ai);
        addCard("Forest", ai).setTapped(true);
        fillLibrary(ai, 10);
        fillLibrary(opp, 10);

        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);

        AssertJUnit.assertTrue("Voyaging Satyr should not have untapped a land it cannot use",
                satyr.isUntapped());
    }
}
