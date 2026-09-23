package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.AiPlayDecision;
import forge.ai.PlayerControllerAi;
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

public class ChangeZoneAiTest extends AITest {

    @Test
    public void testXTargetCountIgnoresTheAisOwnPermanents() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Island", 12, ai);
        // Distorting Wake targets any nonland permanent, so everything here is a legal target, but
        // the AI only wants the opponent's. Its own used to inflate X past the number it would
        // actually target, leaving it unable to cast the spell at all.
        addCard("Ornithopter", ai);
        addCard("Sol Ring", ai);
        addCard("Runeclaw Bear", ai);

        addCard("Colossal Dreadmaw", opponent);
        addCard("Ancient Brontodon", opponent);

        Card wake = addCardToZone("Distorting Wake", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = null;
        for (SpellAbility s : wake.getSpellAbilities()) {
            if (s.getApi() == ApiType.ChangeZone) {
                sa = s;
                break;
            }
        }
        AssertJUnit.assertNotNull("Distorting Wake should have a ChangeZone spell ability", sa);
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue("AI should still cast an X-targeting bounce while it controls permanents",
                SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
        AssertJUnit.assertEquals("X should match the permanents the AI actually wants to target",
                Integer.valueOf(2), sa.getXManaCostPaid());
        for (Card t : sa.getTargets().getTargetCards()) {
            AssertJUnit.assertFalse("AI should never target its own permanent with this",
                    t.getController().equals(ai));
        }
    }

    @Test
    public void testExhumeWithEmptyOpponentGraveyard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Swamp", 2, ai);
        addCardToZone("Akroma, Angel of Wrath", ai, ZoneType.Graveyard);
        Card exhume = addCardToZone("Exhume", ai, ZoneType.Hand);
        SpellAbility sa = exhume.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue("AI should cast Exhume when only its graveyard has a creature",
                SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
    }

    @Test
    public void testExhumeRequiresAnOwnCreature() {
        for (String ownCard : new String[] {null, "Swamp"}) {
            Game game = initAndCreateGame();
            Player ai = game.getPlayers().get(1);
            Player opponent = game.getPlayers().get(0);

            addCards("Swamp", 2, ai);
            if (ownCard != null) {
                addCardToZone(ownCard, ai, ZoneType.Graveyard);
            }
            addCardToZone("Grizzly Bears", opponent, ZoneType.Graveyard);
            Card exhume = addCardToZone("Exhume", ai, ZoneType.Hand);
            SpellAbility sa = exhume.getSpellAbilities().get(0);
            sa.setActivatingPlayer(ai);

            AssertJUnit.assertFalse("AI should not cast Exhume without an own creature",
                    ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa) == AiPlayDecision.WillPlay);
        }
    }

}
