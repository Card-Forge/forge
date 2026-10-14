package forge.ai;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class ComputerUtilOpponentSafetyTest extends AITest {

    @Test
    public void mainPhaseHeuristicsSkipOpponentBattlefieldWhenLastOpponentHasLeft() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);

        Card divination = addCardToZone("Divination", ai, ZoneType.Hand);
        SpellAbility draw = divination.getSpellAbilities().stream()
                .filter(ability -> ability.getApi() == ApiType.Draw).findFirst()
                .orElseThrow(() -> new AssertionError("Divination should have an actual Draw ability"));
        draw.setActivatingPlayer(ai);
        AssertJUnit.assertTrue("Divination is an ordinary playable spell at MAIN1",
                draw.getRestrictions().canPlay(divination, draw));
        Card bear = addCardToZone("Runeclaw Bear", ai, ZoneType.Hand);
        SpellAbility castBear = bear.getSpellAbilities().get(0);
        castBear.setActivatingPlayer(ai);

        opponent.concede();
        game.onPlayerLost(opponent);
        AssertJUnit.assertFalse("the game-over flag is set after onPlayerLost in GameAction", game.isGameOver());
        AssertJUnit.assertNull("the last opponent has left the active player set", ai.getWeakestOpponent());

        AssertJUnit.assertFalse(ComputerUtil.castSpellInMain1(ai, draw));
        AssertJUnit.assertFalse(ComputerUtil.castPermanentInMain1(ai, castBear));
        AssertJUnit.assertEquals(AiPlayDecision.MissingPhaseRestrictions,
                SpellApiToAi.Converter.get(draw).canPlayWithSubs(ai, draw).decision());
    }

}
