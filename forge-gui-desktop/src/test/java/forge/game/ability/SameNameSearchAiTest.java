package forge.game.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.AiPlayDecision;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertEquals;

/** The AI still casts the cards whose search is one ChangeZone with SameNameAsEach$. */
public class SameNameSearchAiTest extends AITest {

    private Game game;
    private Player ai;

    private void setUp(int lands) {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        ai.setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        ai.getZone(ZoneType.Library).removeAllCards(true);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        }
        for (int i = 0; i < lands; i++) {
            addCard("Forest", ai);
        }
        addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", ai);
        addCard("Grizzly Bears", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
    }

    private AiPlayDecision decide(String spellName) {
        Card spell = addCardToZone(spellName, ai, ZoneType.Hand);
        SpellAbility sa = spell.getSpells().get(0);
        sa.setActivatingPlayer(ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    @Test(timeOut = 60000)
    public void aiCastsDoublingChantWithCreaturesToCopy() {
        setUp(5);
        assertEquals(AiPlayDecision.WillPlay, decide("Doubling Chant"));
    }

    @Test(timeOut = 60000)
    public void aiCastsClarionUltimatumWithPermanentsToCopy() {
        setUp(7);
        for (int i = 0; i < 3; i++) {
            addCard("Forest", ai);
        }
        assertEquals(AiPlayDecision.WillPlay, decide("Clarion Ultimatum"));
    }
}
