package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * On a converge or sunburst card X only buys colors, so the AI announces the least X that reaches
 * the most of them - whether the count lands on the card as counters or as the size of the effect.
 */
public class ConvergeXCastTest extends AITest {

    @Test
    public void sunburstCountsEveryColourItCouldPayFor() {
        Game game = newGame();
        Player ai = game.getPlayers().get(1);
        fiveColours(ai);
        addCardToZone("Engineered Explosives", ai, ZoneType.Hand);

        settleAndPlay(game, ai);

        Card ee = findCardWithName(game, "Engineered Explosives");
        assertNotNull("the AI cast it", ee);
        assertEquals("sunburst counted all five colours", 5,
                ee.getCounters(CounterEnumType.CHARGE));
    }

    /**
     * Sweep the Skies counts its colors in Y, so X is not the token count and is easy to leave
     * unannounced - but it is still what buys the colors. Asserted on the announced X rather than
     * on a played turn, because TokenAi gates token spells behind a random roll.
     */
    @Test
    public void convergeSizesAnEffectTheCostDoesNotName() {
        Game game = newGame();
        Player ai = game.getPlayers().get(1);
        fiveColours(ai);
        addCard("Island", ai);
        Card sweep = addCardToZone("Sweep the Skies", ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = sweep.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        SpellApiToAi.Converter.get(sa.getApi()).canPlayWithSubs(ai, sa);

        assertEquals("X paid for the four extra colours", 4, sa.getXManaCostPaid().intValue());
        assertEquals("so all five are spent", 5, ComputerUtilMana.getConvergeCount(sa, ai));
    }

    private Game newGame() {
        Game game = initAndCreateGame();
        game.getPlayers().get(0).setTeam(1);
        game.getPlayers().get(1).setTeam(0);
        return game;
    }

    private void fiveColours(Player p) {
        addCard("Plains", p);
        addCard("Island", p);
        addCard("Swamp", p);
        addCard("Mountain", p);
        addCard("Forest", p);
    }

    private void settleAndPlay(Game game, Player ai) {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        gameLoopUntilNextPhase(game);
    }
}
