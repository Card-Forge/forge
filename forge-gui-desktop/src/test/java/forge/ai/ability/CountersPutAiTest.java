package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import org.testng.annotations.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class CountersPutAiTest extends AITest {

    @Test
    public void testSelfSacrificePutCounterAbilityIsPlayable() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        // Mutagen token activation needs one generic mana and a valid creature target.
        Card mutagenToken = addToken("c_a_mutagen_sac", ai);
        addCard("Plains", ai);
        addCard("Runeclaw Bear", ai);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        // Find the PutCounter ability (with the activation cost)
        SpellAbility putCounterSa = null;
        for (SpellAbility sa : mutagenToken.getSpellAbilities()) {
            if (sa.getDescription().contains("Sacrifice this token: Put a +1/+1")) {
                putCounterSa = sa;
                break;
            }
        }
        assertNotNull("Could not find PutCounter ability on mutagen token", putCounterSa);

        SpellAbilityAi aiLogic = SpellApiToAi.Converter.get(ApiType.PutCounter);
        AiPlayDecision decision = aiLogic.canPlayWithSubs(ai, putCounterSa).decision();

        assertEquals("AI should activate self-sacrifice mutagen PutCounter ability",
                AiPlayDecision.WillPlay, decision);
    }
}
