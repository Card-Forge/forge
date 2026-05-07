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
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class PumpAiTest extends AITest {
    @Test
    public void testCounterBoonWrapperTargetsOwnCommander() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        ai.setTeam(0);
        opponent.setTeam(1);

        Card forge = addCard("Forge of Heroes", ai);
        Card aiCommander = addCard("Elvish Mystic", ai);
        Card opponentCommander = addCard("Emrakul, the Aeons Torn", opponent);
        aiCommander.setCommander(true);
        opponentCommander.setCommander(true);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility forgeSa = findSAWithPrefix(forge, "{T}: Choose target commander");
        AssertJUnit.assertNotNull(forgeSa);
        forgeSa.setActivatingPlayer(ai);
        AssertJUnit.assertNotNull("Forge of Heroes should have counter sub-abilities", forgeSa.getSubAbility());
        AssertJUnit.assertEquals("P1P1", forgeSa.getSubAbility().getParam("CounterType"));
        AssertJUnit.assertEquals("ParentTarget", forgeSa.getSubAbility().getParam("Defined"));
        AssertJUnit.assertTrue("AI commander should be a legal Forge of Heroes target", forgeSa.canTarget(aiCommander));
        AssertJUnit.assertTrue("Opponent commander should be a legal Forge of Heroes target", forgeSa.canTarget(opponentCommander));

        SpellAbilityAi pumpAi = SpellApiToAi.Converter.get(ApiType.Pump);
        AiPlayDecision decision = pumpAi.canPlayWithSubs(ai, forgeSa).decision();

        AssertJUnit.assertEquals("AI should activate Forge of Heroes for its own commander",
                AiPlayDecision.WillPlay, decision);
        AssertJUnit.assertEquals("AI should not put a beneficial commander counter on an opponent's commander",
                aiCommander, forgeSa.getTargetCard());
    }
}
