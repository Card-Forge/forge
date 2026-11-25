package forge.ai.sacrifice;

import forge.ai.AITest;
import forge.ai.ComputerUtil;
import forge.ai.LobbyPlayerAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
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
import org.testng.annotations.DataProvider;

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

        final Card centaur = addCard("Accursed Centaur", ai);
        final SpellAbility mandatory = centaur.getTriggers().get(0).ensureAbility();
        mandatory.setActivatingPlayer(ai);
        chosen = ComputerUtil.choosePermanentsToSacrifice(ai, new CardCollection(centaur), 1, mandatory, false, false);
        AssertJUnit.assertTrue("mandatory ExceptSelf still sacrifices its source as a last resort", chosen.contains(centaur));
    }

    @DataProvider
    public Object[][] optionalExceptSelfBoards() {
        return new Object[][] {{0, false}, {3, false}, {3, true}};
    }

    @Test(dataProvider = "optionalExceptSelfBoards")
    public void optionalExceptSelfRequiresAWorthwhileAlternative(final int lands, final boolean token) {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Default");
        final Card braids = addCard("Braids, Arisen Nightmare", ai);
        addCards("Swamp", lands, ai);
        for (final Player player : game.getPlayers()) {
            addCardToZone("Swamp", player, ZoneType.Library);
        }
        final Card goat = token ? addToken("w_0_1_goat", ai) : null;
        final SpellAbility sacrifice = braids.getTriggers().get(0).ensureAbility();
        sacrifice.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("confirmation weighs the actual optional sacrifice", token,
                ai.getController().confirmAction(sacrifice, null, "Sacrifice a permanent?", null));
        final CardCollection options = new CardCollection(ai.getCardsIn(ZoneType.Battlefield));
        final CardCollection chosen = ComputerUtil.choosePermanentsToSacrifice(ai, options, 1, sacrifice, false, true);
        AssertJUnit.assertFalse("an optional ExceptSelf never selects its own source", chosen.contains(braids));
        if (token) {
            AssertJUnit.assertTrue(chosen.contains(goat));
        }

        game.copyLastState();
        AbilityUtils.resolve(sacrifice);
        AssertJUnit.assertTrue(ai.getCreaturesInPlay().contains(braids));
        AssertJUnit.assertEquals("needed lands survive actual resolution", lands, ai.getLandsInPlay().size());
        AssertJUnit.assertEquals("only worthwhile fodder is sacrificed", 1, ai.getCreaturesInPlay().size());
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
        final SpellAbility devour = devourAbility(elder, ai);
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

        Card dragon = addCardToZone("Predator Dragon", ai, ZoneType.Hand);
        final SpellAbility castDragon = dragon.getFirstSpellAbility();
        castDragon.setActivatingPlayer(ai);
        dragon = game.getAction().moveToStack(dragon, castDragon);
        castDragon.setHostCard(dragon);
        game.getPlayers().get(0).setLife(10, null);
        chosen = ComputerUtil.choosePermanentsToSacrifice(ai, options, 3, devourAbility(dragon, ai), false, true);
        AssertJUnit.assertEquals("a lethal Dragon still devours valuable creatures", 3, chosen.size());
        AssertJUnit.assertTrue(chosen.contains(bears));
        game.copyLastState();
        dragon = game.getAction().moveToPlay(dragon, castDragon, AbilityKey.newMap());
        AssertJUnit.assertEquals(6, dragon.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(1, ai.getCreaturesInPlay().size());
    }

    private SpellAbility devourAbility(final Card host, final Player ai) {
        for (final ReplacementEffect replacement : host.getReplacementEffects()) {
            final SpellAbility ability = replacement.ensureAbility();
            if (ability != null && ability.isKeyword(Keyword.DEVOUR)) {
                ability.setActivatingPlayer(ai);
                return ability;
            }
        }
        throw new AssertionError("missing Devour ability on " + host);
    }

    @DataProvider
    public Object[][] typedDevour() {
        return new Object[][] {
            {"Thromok the Insatiable", "w_0_1_goat", 2, 0, 0, false, 2, 4},
            {"Caprichrome", "c_a_treasure_sac", 2, 0, 0, false, 2, 2},
            {"Feasting Hobbit", "c_a_food_sac", 2, 0, 0, false, 2, 6},
            {"Famished Worldsire", null, 0, 6, 0, false, 1, 3},
            {"Famished Worldsire", null, 0, 8, 0, false, 2, 6},
            {"Famished Worldsire", null, 0, 8, 0, true, 1, 3},
            {"Famished Worldsire", null, 0, 6, 3, false, 2, 6}
        };
    }

    @Test(dataProvider = "typedDevour")
    public void devourUsesItsActualTypeAndProspectiveCounters(final String name, final String token,
            final int tokens, final int lands, final int handLands, final boolean expensiveSpell,
            final int expectedSacrifices, final int expectedCounters) {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Default");
        Card host = addCardToZone(name, ai, ZoneType.Hand);
        final SpellAbility cast = host.getFirstSpellAbility();
        cast.setActivatingPlayer(ai);
        host = game.getAction().moveToStack(host, cast);
        cast.setHostCard(host);
        for (int i = 0; i < tokens; i++) {
            addToken(token, ai);
        }
        addCards("Forest", lands, ai);
        for (int i = 0; i < handLands; i++) {
            addCardToZone("Forest", ai, ZoneType.Hand);
        }
        if (expensiveSpell) {
            addCardToZone("Darksteel Colossus", ai, ZoneType.Library);
        }
        final Card ineligible = addCard("Shivan Dragon", ai);
        final SpellAbility devour = devourAbility(host, ai);
        final int power = host.getNetPower();
        final int toughness = host.getNetToughness();
        final CardCollection options = new CardCollection(ai.getCardsIn(ZoneType.Battlefield));
        final CardCollection chosen = ComputerUtil.choosePermanentsToSacrifice(ai, options, options.size(), devour, false, true);
        AssertJUnit.assertEquals(expectedSacrifices, chosen.size());
        AssertJUnit.assertFalse("valuable or ineligible creatures are retained", chosen.contains(ineligible));
        AssertJUnit.assertEquals("honor a smaller requested maximum", 1, ComputerUtil.choosePermanentsToSacrifice(
                ai, options, 1, devour, false, true).size());
        AssertJUnit.assertEquals("temporary evaluation must not add counters", 0, host.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertFalse("temporary evaluation must not remember sacrifices", host.hasRemembered());
        AssertJUnit.assertEquals(power, host.getNetPower());
        AssertJUnit.assertEquals(toughness, host.getNetToughness());
        AssertJUnit.assertEquals(options.size(), ai.getCardsIn(ZoneType.Battlefield).size());

        game.copyLastState();
        host = game.getAction().moveToPlay(host, cast, AbilityKey.newMap());
        AssertJUnit.assertEquals("real Devour resolution adds the predicted counters",
                expectedCounters, host.getCounters(CounterEnumType.P1P1));
        AssertJUnit.assertEquals(lands == 0 ? 0 : lands - expectedSacrifices, ai.getLandsInPlay().size());
        AssertJUnit.assertTrue(ai.getCreaturesInPlay().contains(ineligible));
    }
}
