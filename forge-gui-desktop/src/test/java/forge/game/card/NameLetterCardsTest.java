package forge.game.card;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

// the AI chooses the first letter it is offered
public class NameLetterCardsTest extends AITest {

    private Game game;
    private Player me;
    private Player opp;

    @Test
    public void grayMerchantCountsNamesContainingTheLetter() {
        newGame();
        addCard("Grizzly Bears", me);
        addCard("Elvish Mystic", me);
        addCard("Air Elemental", opp);
        game.getAction().checkStateEffects(true);

        game.getAction().moveToPlay(addCardToZone("Gray Merchant of Alphabet", me, ZoneType.Hand), null, null);
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        playUntilStackClear(game);

        // Grizzly Bears and the Merchant itself
        AssertJUnit.assertEquals(22, me.getLife());
        AssertJUnit.assertEquals(18, opp.getLife());
    }

    @Test
    public void leadingPerformanceCountsNamesBeginningWithTheLetter() {
        newGame();
        Card air = addCard("Air Elemental", me);
        Card balduvian = addCard("Balduvian Bears", me);
        Card bears = addCard("Grizzly Bears", me);
        Card theirs = addCard("Alpine Grizzly", opp);
        game.getAction().checkStateEffects(true);

        resolve("Leading Performance", null);

        // A, then B because the second letter has to differ
        AssertJUnit.assertEquals(1, air.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, balduvian.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(0, bears.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(0, theirs.getCounters(CounterEnumType.P1P1));
    }

    @Test
    public void disemvowelCountsEachVowelOnce() {
        newGame();
        Card bears = addCard("Grizzly Bears", opp);
        game.getAction().checkStateEffects(true);

        resolve("Disemvowel", bears);

        AssertJUnit.assertFalse(bears.isInPlay());
        // I, Y, E and A
        AssertJUnit.assertEquals(16, opp.getLife());
    }

    private void newGame() {
        game = initAndCreateGame();
        me = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, me);
    }

    private void resolve(String spell, Card target) {
        SpellAbility sa = addCardToZone(spell, me, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(me);
        if (target != null) {
            sa.getTargets().add(target);
        }
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
    }
}
