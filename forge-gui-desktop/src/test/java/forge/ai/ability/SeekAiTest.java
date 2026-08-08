package forge.ai.ability;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class SeekAiTest extends AITest {

    /**
     * Runecarved Obelisk taps and sacrifices itself to seek a card whose mana value is at most the
     * number of charge counters on it. With no charge counters nothing in the library can match, and
     * SeekEffect skips an empty pool after the cost has already been paid, so activating it throws
     * the artifact away for nothing.
     */
    @Test
    public void doesNotSacrificeItselfSeekingAnEmptyPool() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCard("Runecarved Obelisk", ai);
        // no charge counters, and nothing in the library is free
        for (int i = 0; i < 10; i++) {
            addCardToZone("Hill Giant", ai, ZoneType.Library);
        }
        game.getAction().checkStateEffects(true);

        playUntilStackClear(game);

        AssertJUnit.assertEquals("Runecarved Obelisk should not have been sacrificed", 1,
                countCardsWithName(game, "Runecarved Obelisk"));
    }

    /** With enough charge counters the library can answer, so the ability is worth its cost. */
    @Test
    public void seeksWhenTheLibraryCanAnswer() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card obelisk = addCard("Runecarved Obelisk", ai);
        obelisk.setCounters(CounterEnumType.CHARGE, 6);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Hill Giant", ai, ZoneType.Library);
        }
        game.getAction().checkStateEffects(true);

        int handBefore = ai.getCardsIn(ZoneType.Hand).size();
        playUntilStackClear(game);

        AssertJUnit.assertEquals("Runecarved Obelisk should have been sacrificed to seek", 0,
                countCardsWithName(game, "Runecarved Obelisk"));
        AssertJUnit.assertEquals("the sought card should be in hand", handBefore + 1,
                ai.getCardsIn(ZoneType.Hand).size());
    }
}
