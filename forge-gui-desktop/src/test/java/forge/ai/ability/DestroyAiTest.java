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
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

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
