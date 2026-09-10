package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class PumpAllAiTest extends AITest {

    @Test
    public void testToxicDelugeSweepsOpponentBoard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Swamp", 3, ai);
        Card deluge = addCardToZone("Toxic Deluge", ai, ZoneType.Hand);
        addCards("Grizzly Bears", 3, opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = findPumpAllAbility(deluge);
        AssertJUnit.assertNotNull("Toxic Deluge should have a PumpAll spell ability", sa);
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue("AI should cast Toxic Deluge to sweep an opposing board it does not share",
                SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
        AssertJUnit.assertEquals("AI should pay exactly enough life to kill 2/2s",
                Integer.valueOf(2), sa.getXManaCostPaid());
    }

    @Test
    public void testFlowstoneSlideSweepsForItsManaX() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Mountain", 6, ai);
        Card slide = addCardToZone("Flowstone Slide", ai, ZoneType.Hand);
        addCards("Grizzly Bears", 3, opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = findPumpAllAbility(slide);
        AssertJUnit.assertNotNull("Flowstone Slide should have a PumpAll spell ability", sa);
        sa.setActivatingPlayer(ai);

        // +X/-X, so unlike Toxic Deluge the survivors get power out of it - which only counts
        // against the AI on a turn the opponent still has a combat left.
        AssertJUnit.assertTrue("AI should cast Flowstone Slide to sweep an opposing board",
                SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
        AssertJUnit.assertEquals("AI should pay the cheapest X that kills 2/2s",
                Integer.valueOf(2), sa.getXManaCostPaid());
    }

    @Test
    public void testAlreadyPaidXOnATriggerIsNotReannounced() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Grizzly Bears", 3, opponent);
        Card meathook = addCard("The Meathook Massacre", ai);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = null;
        for (Trigger t : meathook.getTriggers()) {
            SpellAbility overriding = t.getOverridingAbility();
            if (overriding != null && overriding.getApi() == ApiType.PumpAll) {
                sa = overriding;
                break;
            }
        }
        AssertJUnit.assertNotNull("The Meathook Massacre should have a PumpAll trigger", sa);

        // X here was paid when the enchantment was cast. Trying to re-announce it finds no X in the
        // trigger's own cost and yields nothing to pay, which would make the AI refuse the trigger.
        sa.setActivatingPlayer(ai);
        sa.setXManaCostPaid(3);

        AssertJUnit.assertTrue("AI should take a trigger that wipes the opposing board",
                SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
        AssertJUnit.assertEquals("X paid on casting must survive the AI's evaluation of the trigger",
                Integer.valueOf(3), sa.getXManaCostPaid());
    }

    @Test
    public void aiSweepsAnOpposingBoardInARealTurn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Swamp", 6, ai);
        addCardToZone("Toxic Deluge", ai, ZoneType.Hand);
        addCards("Grizzly Bears", 3, opponent);
        fillLibrary(ai, 10);
        fillLibrary(opponent, 10);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        AssertJUnit.assertEquals("the sweep should have resolved and killed the 2/2s",
                0, countCardsWithName(game, "Grizzly Bears", ZoneType.Battlefield));
        AssertJUnit.assertEquals("paying only the life the kill needed", 18, ai.getLife());
    }

    @Test
    public void aiSweepsForFlowstoneSlideInARealTurn() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Mountain", 8, ai);
        addCardToZone("Flowstone Slide", ai, ZoneType.Hand);
        addCards("Grizzly Bears", 3, opponent);
        fillLibrary(ai, 10);
        fillLibrary(opponent, 10);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        AssertJUnit.assertEquals("the sweep should have resolved and killed the 2/2s",
                0, countCardsWithName(game, "Grizzly Bears", ZoneType.Battlefield));
    }

    @Test
    public void aiPicksAnXThatSparesItsOwnBoard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Swamp", 6, ai);
        addCardToZone("Toxic Deluge", ai, ZoneType.Hand);
        addCards("Grizzly Bears", 3, opponent);
        addCards("Hill Giant", 2, ai);
        fillLibrary(ai, 10);
        fillLibrary(opponent, 10);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        AssertJUnit.assertEquals("the 2/2s it was aimed at should be gone",
                0, countCardsWithName(game, "Grizzly Bears", ZoneType.Battlefield));
        AssertJUnit.assertEquals("its own 3/3s should survive the X it chose",
                2, countCardsWithName(game, "Hill Giant", ZoneType.Battlefield));
    }
    private SpellAbility findPumpAllAbility(Card card) {
        for (SpellAbility sa : card.getSpellAbilities()) {
            if (sa.getApi() == ApiType.PumpAll) {
                return sa;
            }
        }
        return null;
    }
}
