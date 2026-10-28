package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.ai.simulation.CommanderPlan;
import forge.ai.simulation.DeckPlan;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;

/** The Commander plan: strategies of the commander, known combos, and the terms it adds to a rollout. */
public class FlatMonteCarloCommanderTest extends AITest {

    private Game commanderGame(final String commander) {
        final Deck deck = new Deck();
        deck.getOrCreate(DeckSection.Commander).add(commander, 1);
        final List<RegisteredPlayer> players = Lists.newArrayList(
                RegisteredPlayer.forCommander(deck).setPlayer(new LobbyPlayerAi("p2", null)),
                RegisteredPlayer.forCommander(deck).setPlayer(new LobbyPlayerAi("p1", null)));
        final GameRules rules = new GameRules(GameType.Commander);
        final Game game = new Game(players, rules, new Match(rules, players, "Test"));
        for (final Player p : game.getPlayers()) {
            p.initVariantsZones(p.getRegisteredPlayer());
        }
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(1));
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    private void library(final Player p, final Object... list) {
        for (int i = 0; i < list.length; i += 2) {
            for (int n = 0; n < (Integer) list[i + 1]; n++) {
                addCardToZone((String) list[i], p, ZoneType.Library);
            }
        }
    }

    @Test
    public void aGameWithoutCommandersHasNoCommanderPlan() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        library(ai, "Bonesplitter", 4);
        final CommanderPlan plan = CommanderPlan.scan(ai);
        AssertJUnit.assertFalse(plan.isActive());
        AssertJUnit.assertEquals(0, plan.term(ai));
        AssertJUnit.assertFalse(DeckPlan.forSetting("auto", ai).commander.isActive());
    }

    @Test
    public void equipmentUnderASramCommanderIsVoltron() {
        final Game game = commanderGame("Sram, Senior Edificer");
        final Player ai = game.getPlayers().get(1);
        library(ai, "Bonesplitter", 1, "Short Sword", 1, "Leonin Scimitar", 1, "Sword of Fire and Ice", 1);
        final CommanderPlan plan = CommanderPlan.scan(ai);
        AssertJUnit.assertTrue(plan.isActive());
        AssertJUnit.assertTrue(plan.getCommanderNames().contains("Sram, Senior Edificer"));
        AssertJUnit.assertTrue("voltron: " + plan.getStrategies(), plan.has(CommanderPlan.Strategy.VOLTRON));
        AssertJUnit.assertTrue(plan.getStrategies().size() <= CommanderPlan.MAX_STRATEGIES);
    }

    @Test
    public void aVoltronCommanderMakesAnOtherwiseMidrangeDeckAggro() {
        final Game game = commanderGame("Sram, Senior Edificer");
        final Player ai = game.getPlayers().get(1);
        library(ai, "Bonesplitter", 1, "Short Sword", 1, "Leonin Scimitar", 1, "Sword of Fire and Ice", 1);
        final DeckPlan plan = DeckPlan.forSetting("auto", ai);
        AssertJUnit.assertTrue(plan.commander.isActive());
        AssertJUnit.assertEquals(DeckPlan.Archetype.AGGRO, plan.archetype);
        AssertJUnit.assertTrue(plan.describe().contains("CMD"));
    }

    @Test
    public void aKnownComboIsFoundOnlyWithAllItsPieces() {
        final Game game = commanderGame("Krenko, Mob Boss");
        final Player ai = game.getPlayers().get(1);
        library(ai, "Thassa's Oracle", 1);
        AssertJUnit.assertFalse("one piece is no combo", CommanderPlan.scan(ai).hasCombos());
        library(ai, "Demonic Consultation", 1);
        final CommanderPlan plan = CommanderPlan.scan(ai);
        AssertJUnit.assertTrue(plan.hasCombos());
        AssertJUnit.assertTrue(plan.pieceNames().contains("Thassa's Oracle"));
        AssertJUnit.assertTrue(plan.pieceNames().contains("Demonic Consultation"));
    }

    @Test
    public void theCommanderAndComboPiecesAreKeyCards() {
        final Game game = commanderGame("Krenko, Mob Boss");
        final Player ai = game.getPlayers().get(1);
        final Card oracle = addCardToZone("Thassa's Oracle", ai, ZoneType.Library);
        addCardToZone("Demonic Consultation", ai, ZoneType.Library);
        final Card bear = addCardToZone("Runeclaw Bear", ai, ZoneType.Library);
        final CommanderPlan plan = CommanderPlan.scan(ai);
        AssertJUnit.assertTrue(plan.isKeyCard(oracle));
        AssertJUnit.assertTrue(plan.isKeyCard(ai.getCommanders().get(0)));
        AssertJUnit.assertFalse(plan.isKeyCard(bear));
    }

    @Test
    public void aComboPieceInHandIsNotDiscardedWhileItsPartnerIsThere() {
        final Game game = commanderGame("Krenko, Mob Boss");
        final Player ai = game.getPlayers().get(1);
        final Card oracle = addCardToZone("Thassa's Oracle", ai, ZoneType.Hand);
        final Card consult = addCardToZone("Demonic Consultation", ai, ZoneType.Hand);
        final Card bear = addCardToZone("Runeclaw Bear", ai, ZoneType.Hand);
        final CommanderPlan plan = CommanderPlan.scan(ai);

        final forge.game.card.CardCollection hand = new forge.game.card.CardCollection(ai.getCardsIn(ZoneType.Hand));
        final forge.game.card.CardCollection safe = plan.withoutProtected(ai, hand);
        AssertJUnit.assertFalse(safe.contains(oracle));
        AssertJUnit.assertFalse(safe.contains(consult));
        AssertJUnit.assertTrue(safe.contains(bear));
    }

    @Test
    public void theCommanderOnTheTableAndItsDamageAreWorthPoints() {
        final Game game = commanderGame("Krenko, Mob Boss");
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final CommanderPlan plan = CommanderPlan.scan(ai);
        final int inCommandZone = plan.term(ai);

        final Card cmd = ai.getCommanders().get(0);
        final Card onTable = game.getAction().moveToPlay(cmd, ai, null, AbilityKey.newMap());
        AssertJUnit.assertNotNull(onTable);
        final int onBattlefield = plan.term(ai);
        AssertJUnit.assertTrue("commander on the table is worth more", onBattlefield > inCommandZone);

        opp.addCommanderDamage(game.getCardState(cmd), 10);
        AssertJUnit.assertTrue("commander damage dealt is progress", plan.term(ai) > onBattlefield);
    }

    @Test
    public void commanderDamageTakenIsAClock() {
        final Game game = commanderGame("Krenko, Mob Boss");
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final CommanderPlan plan = CommanderPlan.scan(ai);
        final int before = plan.term(ai);
        ai.addCommanderDamage(opp.getCommanders().get(0), 16);
        AssertJUnit.assertTrue("16 of 21 from one commander is a danger", plan.term(ai) < before);
    }

    @Test
    public void theTermIsBounded() {
        final Game game = commanderGame("Krenko, Mob Boss");
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final CommanderPlan plan = CommanderPlan.scan(ai);
        ai.addCommanderDamage(opp.getCommanders().get(0), 100);
        AssertJUnit.assertTrue(plan.term(ai) >= -300);
    }
}
