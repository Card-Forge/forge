package forge.ai.sacrifice;

import forge.ai.AITest;
import forge.ai.ComputerUtil;
import forge.ai.LobbyPlayerAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Tests for AI sacrifice selection and decisions about its value.
 */
public class AiSacrificeDecisionTest extends AITest {

    @Test
    public void testAiSacrificesCreatureToAvoidLethalLifeLoss() {
        Game game = initAndCreateGame();

        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        opponent.setLife(1, null);

        // Player 1 controls Fandaniel
        addCard("Fandaniel, Telophoroi Ascian", p);

        // Player 1 has a sorcery in graveyard (causes 2 life loss per sorcery/instant)
        addCardToZone("The Crystal's Chosen", p, ZoneType.Graveyard);

        // Opponent controls a creature they can sacrifice
        Card adelbert = addCard("Adelbert Steiner", opponent);

        // Fill libraries to prevent draw-death
        for (int i = 0; i < 10; i++) {
            addCardToZone("Plains", p, ZoneType.Library);
            addCardToZone("Plains", opponent, ZoneType.Library);
        }

        // Play until next turn (after Fandaniel's end step trigger resolves)
        this.playUntilNextTurn(game);

        // The game should NOT be over - opponent should have sacrificed to survive
        AssertJUnit.assertFalse("Game should not be over - AI should have sacrificed to survive", game.isGameOver());

        // Adelbert should be in graveyard (sacrificed)
        AssertJUnit.assertTrue("Adelbert Steiner should be in graveyard after being sacrificed",
                opponent.getZone(ZoneType.Graveyard).contains(adelbert));

        // Opponent should still have 1 life (didn't lose life because they sacrificed)
        AssertJUnit.assertEquals("Opponent should still have 1 life after sacrificing", 1, opponent.getLife());
    }


    @Test
    public void testAiDoesNotSacrificeWhenLifeLossIsNotLethal() {
        Game game = initAndCreateGame();

        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        opponent.setLife(10, null);

        // Player 1 controls Fandaniel
        addCard("Fandaniel, Telophoroi Ascian", p);

        // Player 1 has a sorcery in graveyard
        addCardToZone("The Crystal's Chosen", p, ZoneType.Graveyard);

        // Opponent controls a creature
        Card adelbert = addCard("Adelbert Steiner", opponent);

        // Fill libraries
        for (int i = 0; i < 10; i++) {
            addCardToZone("Plains", p, ZoneType.Library);
            addCardToZone("Plains", opponent, ZoneType.Library);
        }

        // Play until next turn (after Fandaniel's end step trigger resolves)
        this.playUntilNextTurn(game);

        // Game should not be over
        AssertJUnit.assertFalse("Game should not be over", game.isGameOver());

        // Adelbert should still be on battlefield (not sacrificed)
        AssertJUnit.assertTrue("Adelbert Steiner should still be on battlefield when life loss isn't lethal",
                opponent.getZone(ZoneType.Battlefield).contains(adelbert));

        // Opponent should have 8 life (lost 2 from not sacrificing)
        AssertJUnit.assertEquals("Opponent should have lost 2 life from not sacrificing", 8, opponent.getLife());
    }


    @Test
    public void testAiSacrificesToPillarTombsToAvoidLethalLifeLoss() {
        Game game = initAndCreateGame();

        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        opponent.setLife(4, null);

        // Player 1 controls Pillar Tombs of Aku
        addCard("Pillar Tombs of Aku", p);

        // Opponent controls a creature they can sacrifice
        Card bear = addCard("Runeclaw Bear", opponent);

        // Fill libraries to prevent draw-death
        for (int i = 0; i < 10; i++) {
            addCardToZone("Plains", p, ZoneType.Library);
            addCardToZone("Plains", opponent, ZoneType.Library);
        }

        // Play two turns - first ends Player 1's turn, second plays through opponent's upkeep
        // where Pillar Tombs triggers and the AI must decide whether to sacrifice
        this.playUntilNextTurn(game);
        this.playUntilNextTurn(game);

        // The game should NOT be over - AI should have sacrificed to survive
        AssertJUnit.assertFalse("Game should not be over - AI should have sacrificed to survive", game.isGameOver());

        // Bear should be in graveyard (sacrificed)
        AssertJUnit.assertTrue("Runeclaw Bear should be in graveyard after being sacrificed",
                opponent.getZone(ZoneType.Graveyard).contains(bear));

        // Opponent should still have 4 life (didn't lose life because they sacrificed)
        AssertJUnit.assertEquals("Opponent should still have 4 life after sacrificing", 4, opponent.getLife());
    }

    @Test
    public void mixedPermanentPreferenceIsOptInAndRetainsMandatoryFallback() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card braids = addCard("Braids, Arisen Nightmare", ai);
        final Card goat = addToken("w_0_1_goat", ai);
        final Card ring = addCard("Sol Ring", ai);
        final Card land = addCard("Swamp", ai);
        final SpellAbility sacrifice = AbilityFactory.getAbility(
                "DB$ Sacrifice | Defined$ You | SacValid$ Permanent | Amount$ 1"
                + " | AIPermanentSacPreference$ DeckMostProminentType", braids);
        sacrifice.setActivatingPlayer(ai);
        final CardCollection options = new CardCollection(ai.getCardsIn(ZoneType.Battlefield));

        CardCollection chosen = ComputerUtil.choosePermanentsToSacrifice(ai, options, 1, sacrifice, false, true);
        AssertJUnit.assertEquals(1, chosen.size());
        AssertJUnit.assertTrue("prefer the nonland token's type", chosen.contains(goat));
        sacrifice.getMapParams().remove("AIPermanentSacPreference");
        chosen = ComputerUtil.choosePermanentsToSacrifice(ai, options, 1, sacrifice, false, true);
        AssertJUnit.assertEquals(1, chosen.size());
        AssertJUnit.assertTrue("ordinary mixed-permanent selection is unchanged", chosen.contains(ring));

        sacrifice.getMapParams().put("AIPermanentSacPreference", "DeckMostProminentType");
        chosen = ComputerUtil.choosePermanentsToSacrifice(
                ai, new CardCollection(land), 1, sacrifice, false, false);
        AssertJUnit.assertTrue("a mandatory sacrifice still uses a land when necessary", chosen.contains(land));
        AssertJUnit.assertTrue(SpellApiToAi.Converter.get(ApiType.Sacrifice).doTrigger(ai, sacrifice, true));
    }

    @Test
    public void optionalArtifactSacrificeUsesFodderRatherThanCreatureScoring() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Experimental");
        final Card host = addCard("Grizzly Bears", ai);
        final Card valuableArtifact = addCard("Crucible of Worlds", ai);
        final SpellAbility sacrifice = AbilityFactory.getAbility(
                "DB$ Sacrifice | Defined$ You | SacValid$ Artifact | Optional$ True | Amount$ 1", host);
        sacrifice.setActivatingPlayer(ai);

        AssertJUnit.assertFalse("a valuable artifact is not cheap creature fodder",
                SpellApiToAi.Converter.get(ApiType.Sacrifice).doTrigger(ai, sacrifice, false));
        final Card treasure = addToken("c_a_treasure_sac", ai);
        AssertJUnit.assertTrue("an expendable artifact makes the optional sacrifice worthwhile",
                SpellApiToAi.Converter.get(ApiType.Sacrifice).doTrigger(ai, sacrifice, false));
        final CardCollection options = new CardCollection();
        options.add(valuableArtifact);
        options.add(treasure);
        final CardCollection chosen = ComputerUtil.choosePermanentsToSacrifice(
                ai, options, 1, sacrifice, false, true);
        AssertJUnit.assertEquals(1, chosen.size());
        AssertJUnit.assertTrue(chosen.contains(treasure));
        AssertJUnit.assertFalse(chosen.contains(valuableArtifact));
    }

    @Test
    public void devourChoosesWorthwhileGrowthWithinTheLimitWithoutMutatingTheHost() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        Card elder = addCardToZone("Thunder-Thrash Elder", ai, ZoneType.Hand);
        final SpellAbility cast = elder.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        elder = game.getAction().moveToStack(elder, cast);
        final Card goat = addToken("w_0_1_goat", ai);
        final Card otherGoat = addToken("w_0_1_goat", ai);
        final Card bears = addCard("Grizzly Bears", ai);
        SpellAbility devour = null;
        for (final ReplacementEffect replacement : elder.getReplacementEffects()) {
            final SpellAbility ability = replacement.ensureAbility();
            if (ability != null && ability.isKeyword(Keyword.DEVOUR)) {
                devour = ability;
                break;
            }
        }
        AssertJUnit.assertNotNull("Devour has its sacrifice ability", devour);
        devour.setActivatingPlayer(ai);
        final CardCollection options = new CardCollection(ai.getCreaturesInPlay());

        CardCollection chosen = ComputerUtil.choosePermanentsToSacrifice(ai, options, 1, devour, false, true);
        AssertJUnit.assertEquals("respect the requested maximum", 1, chosen.size());
        AssertJUnit.assertTrue(chosen.contains(goat) || chosen.contains(otherGoat));
        chosen = ComputerUtil.choosePermanentsToSacrifice(ai, options, 3, devour, false, true);
        AssertJUnit.assertEquals(2, chosen.size());
        AssertJUnit.assertTrue(chosen.contains(goat));
        AssertJUnit.assertTrue(chosen.contains(otherGoat));
        AssertJUnit.assertFalse("three counters are not worth losing the Bears", chosen.contains(bears));
        chosen = ComputerUtil.choosePermanentsToSacrifice(
                ai, new CardCollection(bears), 1, devour, false, true);
        AssertJUnit.assertTrue("a poor trade can result in no sacrifices", chosen.isEmpty());
        AssertJUnit.assertEquals("evaluation must not put counters on the actual creature",
                0, elder.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, elder.getNetPower());
        AssertJUnit.assertEquals(1, elder.getNetToughness());
        AssertJUnit.assertEquals(3, ai.getCreaturesInPlay().size());
    }
}
