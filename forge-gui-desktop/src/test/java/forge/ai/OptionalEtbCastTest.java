package forge.ai;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class OptionalEtbCastTest extends AITest {

    @Test
    public void testAiCastsACreatureWhoseOptionalEntersEffectCannotDoAnything() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Forest", 4, ai);
        // "When this creature enters, you may search your library for a Food card ...": the effect is
        // Optional$ True, so it must not stop the AI from casting the creature when there is no Food to find.
        Card courier = addCardToZone("Courier of Comestibles", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = courier.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
    }
}
