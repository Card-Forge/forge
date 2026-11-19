package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.Random;
import java.util.function.BooleanSupplier;

public class DestroyAiTest extends AITest {

    @Test
    public void testDustBowlDoesNotSpendExtraLandOnLowPriorityLand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        Card dustBowl = addCard("Dust Bowl", ai);
        addCards("Forest", 5, ai);
        addCardToZone("Forest", ai, ZoneType.Hand);
        addCards("Forest", 5, opponent);
        addCard("Tranquil Cove", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse("AI should not spend Dust Bowl plus another land on a low-priority nonbasic",
                canPlayDestroyAbility(ai, dustBowl));
    }

    @Test
    public void testDustBowlCanAnswerHighPriorityLand() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        Card dustBowl = addCard("Dust Bowl", ai);
        addCards("Forest", 5, ai);
        addCardToZone("Forest", ai, ZoneType.Hand);
        addCards("Forest", 5, opponent);
        addCards("Grizzly Bears", 3, opponent);
        addCard("Gaea's Cradle", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue("AI should still spend Dust Bowl on a high-priority land",
                canPlayDestroyAbility(ai, dustBowl));
    }

    @Test
    public void testBeastWithinTargetsExpensiveOpposingPermanent() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        Card beastWithin = addCardToZone("Beast Within", ai, ZoneType.Hand);
        addCards("Forest", 3, ai);
        addCards("Plains", 6, opponent);
        addCard("Grizzly Bears", opponent);
        Card engine = addCard("Wurmcoil Engine", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opponent);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = findDestroyAbility(beastWithin);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("AI should trade Beast Within's 3/3 token for an expensive threat",
                withMostWillingRemovalRoll(() -> SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay()));
        AssertJUnit.assertEquals(engine, sa.getTargetCard());
    }

    @Test
    public void testBeastWithinNotSpentOnCheapPermanent() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        Card beastWithin = addCardToZone("Beast Within", ai, ZoneType.Hand);
        addCards("Forest", 3, ai);
        addCards("Plains", 2, opponent);
        addCard("Grizzly Bears", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opponent);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse("AI should not hand over a 3/3 to kill a 2-drop",
                withMostWillingRemovalRoll(() -> canPlayDestroyAbility(ai, beastWithin)));
    }

    /** ComputerUtilCard.useRemovalNow rolls nextFloat() against the target's value; pin the roll at 0. */
    private static boolean withMostWillingRemovalRoll(BooleanSupplier check) {
        final Random previous = MyRandom.getRandom();
        try {
            MyRandom.setRandom(new Random() {
                private static final long serialVersionUID = 1L;

                @Override
                public int nextInt() {
                    return 0;
                }
            });
            return check.getAsBoolean();
        } finally {
            MyRandom.setRandom(previous);
        }
    }

    private boolean canPlayDestroyAbility(Player ai, Card source) {
        SpellAbility sa = findDestroyAbility(source);
        sa.setActivatingPlayer(ai);
        return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay();
    }

    private SpellAbility findDestroyAbility(Card card) {
        for (SpellAbility sa : card.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Destroy) {
                return sa;
            }
        }
        return null;
    }
}
