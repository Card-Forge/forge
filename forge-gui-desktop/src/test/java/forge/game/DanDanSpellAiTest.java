package forge.game;

import forge.ai.AITest;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilMana;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellApiToAi;
import forge.deck.Deck;
import forge.game.card.Card;
import forge.game.cost.CostSacrifice;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;

import com.google.common.collect.Lists;

public class DanDanSpellAiTest extends AITest {

    @Test
    public void predictUsesPublicIslandFallbackAcrossUnknownLibraryAndHandVariants() {
        String firstChoice = predictNameWithUnknownCards("Dandan", "Brainstorm");
        String secondChoice = predictNameWithUnknownCards("Swords to Plowshares", "Counterspell");

        AssertJUnit.assertEquals("Island", firstChoice);
        AssertJUnit.assertEquals("Island", secondChoice);
    }

    @Test
    public void predictUsesOnlyACurrentlyAuthorizedTopCardName() {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Card predict = addCardToZone("Predict", ai, ZoneType.Hand);
        Card top = addCardToZone("Brainstorm", ai, ZoneType.Library);
        addCardToZone("Dandan", ai, ZoneType.Library);
        SpellAbility sa = predict.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("Island", choosePredictName(ai, sa));
        top.addMayLookTemp(ai);
        AssertJUnit.assertEquals("Brainstorm", choosePredictName(ai, sa));
        top.removeMayLookTemp(ai);

        game.getAction().revealTo(top, ai);
        ai.shuffle(null);
        AssertJUnit.assertEquals("Island", choosePredictName(ai, sa));
    }

    @Test
    public void predictIsOnlyEnabledForDanDan() {
        Game danDan = createDanDanGame();
        Player danDanAi = danDan.getPlayers().get(1);
        addCards("Island", 2, danDanAi);
        addCardsToZone("Island", 12, danDanAi, ZoneType.Library);
        Card danDanPredict = addCardToZone("Predict", danDanAi, ZoneType.Hand);
        danDanPredict.getFirstSpellAbility().setActivatingPlayer(danDanAi);
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay,
                ((PlayerControllerAi) danDanAi.getController()).getAi().canPlaySa(danDanPredict.getFirstSpellAbility()));

        Game constructed = initAndCreateGame();
        Player constructedAi = constructed.getPlayers().get(1);
        addCards("Island", 2, constructedAi);
        addCardsToZone("Island", 12, constructedAi, ZoneType.Library);
        addCardsToZone("Island", 12, constructed.getPlayers().get(0), ZoneType.Library);
        Card constructedPredict = addCardToZone("Predict", constructedAi, ZoneType.Hand);
        constructedPredict.getFirstSpellAbility().setActivatingPlayer(constructedAi);
        AssertJUnit.assertFalse(((PlayerControllerAi) constructedAi.getController()).getAi()
                .canPlaySa(constructedPredict.getFirstSpellAbility()) == AiPlayDecision.WillPlay);
    }

    @Test
    public void daysUndoingRefillsAnEmptyDanDanHandOnlyAfterCombat() {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCardsToZone("Island", 10, ai, ZoneType.Library);
        addCardsToZone("Island", 4, opponent, ZoneType.Hand);
        Card daysUndoing = addCardToZone("Day's Undoing", ai, ZoneType.Hand);
        SpellAbility sa = daysUndoing.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        // Ten shared library cards and four opposing hand cards leave exactly 14 after Day itself is excluded.
        AssertJUnit.assertEquals(10, ai.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(4, opponent.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay,
                ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse(((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa)
                == AiPlayDecision.WillPlay);
    }

    @Test
    public void daysUndoingDeclinesAUsefulHandOrNoOpponentHandAndKeepsConstructedPolicy() {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCardsToZone("Island", 12, ai, ZoneType.Library);
        Card daysUndoing = addCardToZone("Day's Undoing", ai, ZoneType.Hand);
        addCardsToZone("Counterspell", 2, ai, ZoneType.Hand);
        addCardToZone("Island", opponent, ZoneType.Hand);
        SpellAbility sa = daysUndoing.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        // With 12 library cards, two other AI hand cards, and one opposing hand card, the pool is safe at 15.
        AssertJUnit.assertEquals(12, ai.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(1, opponent.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(3, ai.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertFalse(((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa)
                == AiPlayDecision.WillPlay);

        Game noOpponentHandGame = createDanDanGame();
        Player noOpponentHandAi = noOpponentHandGame.getPlayers().get(1);
        addCards("Island", 3, noOpponentHandAi);
        addCardsToZone("Island", 14, noOpponentHandAi, ZoneType.Library);
        Card noOpponentHandDays = addCardToZone("Day's Undoing", noOpponentHandAi, ZoneType.Hand);
        SpellAbility noOpponentHandSa = noOpponentHandDays.getFirstSpellAbility();
        noOpponentHandSa.setActivatingPlayer(noOpponentHandAi);
        noOpponentHandGame.getPhaseHandler().devModeSet(PhaseType.MAIN2, noOpponentHandAi);
        AssertJUnit.assertFalse(((PlayerControllerAi) noOpponentHandAi.getController()).getAi()
                .canPlaySa(noOpponentHandSa) == AiPlayDecision.WillPlay);

        Game constructed = initAndCreateGame();
        Player constructedAi = constructed.getPlayers().get(1);
        addCards("Island", 3, constructedAi);
        Card constructedDays = addCardToZone("Day's Undoing", constructedAi, ZoneType.Hand);
        SpellAbility constructedSa = constructedDays.getFirstSpellAbility();
        constructedSa.setActivatingPlayer(constructedAi);
        constructed.getPhaseHandler().devModeSet(PhaseType.MAIN1, constructedAi);
        AssertJUnit.assertFalse(((PlayerControllerAi) constructedAi.getController()).getAi()
                .canPlaySa(constructedSa) == AiPlayDecision.WillPlay);
    }

    @Test
    public void daysUndoingDoesNotDrawPastAThinLibraryAfterShuffling() {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCardsToZone("Island", 12, ai, ZoneType.Library);
        addCardToZone("Island", opponent, ZoneType.Hand);
        Card daysUndoing = addCardToZone("Day's Undoing", ai, ZoneType.Hand);
        SpellAbility sa = daysUndoing.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        // Twelve shared library cards plus one opposing hand card make a 13-card refill pool.
        AssertJUnit.assertEquals(12, ai.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(1, opponent.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertFalse(((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa)
                == AiPlayDecision.WillPlay);
    }

    @Test
    public void daysUndoingCountsTheSharedGraveyardInItsPostShuffleRefillPool() {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCardsToZone("Island", 3, ai, ZoneType.Library);
        addCardsToZone("Island", 12, opponent, ZoneType.Graveyard);
        addCardToZone("Island", opponent, ZoneType.Hand);
        Card daysUndoing = addCardToZone("Day's Undoing", ai, ZoneType.Hand);
        SpellAbility sa = daysUndoing.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        // Three library cards, twelve shared graveyard cards, and one opposing hand card make 16.
        AssertJUnit.assertEquals(3, ai.getCardsIn(ZoneType.Library).size());
        AssertJUnit.assertEquals(12, ai.getCardsIn(ZoneType.Graveyard).size());
        AssertJUnit.assertEquals(1, opponent.getCardsIn(ZoneType.Hand).size());
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay,
                ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
    }

    @Test
    public void metamorphoseRemovesAValuablePermanentWhenOpponentHasNoReplacementCard() {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCard("Island", opponent);
        Card target = addCard("Dandân", opponent);
        addOpponentPumpTargeting(game, opponent, target);
        Card metamorphose = addCardToZone("Metamorphose", ai, ZoneType.Hand);
        SpellAbility sa = metamorphose.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay,
                ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
        AssertJUnit.assertTrue("the target should be the opponent's valuable permanent",
                sa.getTargets().getTargetCards().contains(target));
    }

    @Test
    public void metamorphoseKeepsItsExistingConstructedPolicy() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCard("Island", opponent);
        Card target = addCard("Dandân", opponent);
        addOpponentPumpTargeting(game, opponent, target);
        addCardToZone("Island", opponent, ZoneType.Hand);
        Card metamorphose = addCardToZone("Metamorphose", ai, ZoneType.Hand);
        SpellAbility sa = metamorphose.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        AssertJUnit.assertEquals(AiPlayDecision.WillPlay,
                ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa));
    }

    @Test
    public void metamorphoseDecisionDoesNotInspectOpponentHandIdentity() {
        AiPlayDecision withIsland = metamorphoseDecisionWithOpponentHandCard("Island");
        AiPlayDecision withCounterspell = metamorphoseDecisionWithOpponentHandCard("Counterspell");

        AssertJUnit.assertEquals(withIsland, withCounterspell);
        AssertJUnit.assertFalse(withIsland == AiPlayDecision.WillPlay);
    }

    @Test
    public void svyeluniteTempleCanPayARealBlueBlueSpellCost() {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Card temple = addCard("Svyelunite Temple", ai);
        temple.setTapped(false);
        Card counterspell = addCardToZone("Counterspell", ai, ZoneType.Hand);
        SpellAbility sa = counterspell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue("the existing mana payment path should use Temple when UU is required",
                ComputerUtilMana.canPayManaCost(sa, ai, 0, false));
        AssertJUnit.assertTrue(ComputerUtilMana.getManaSourcesToPayCost(
                ComputerUtilMana.calculateManaCost(sa.getPayCosts(), sa, ai, true, 0, false), sa, ai, false)
                .contains(temple));

        temple.setTapped(true);
        AssertJUnit.assertFalse("the spell should be unaffordable when Temple cannot activate",
                ComputerUtilMana.canPayManaCost(sa, ai, 0, false));
    }

    @Test
    public void svyeluniteTempleIsSacrificedWhenBlueBlueIsActuallyPaid() {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Card temple = addCard("Svyelunite Temple", ai);
        Card counterspell = addCardToZone("Counterspell", ai, ZoneType.Hand);
        SpellAbility sa = counterspell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue(ComputerUtilMana.payManaCost(sa.getPayCosts(), ai, sa, false));
        AssertJUnit.assertTrue("Temple's sacrifice mode pays the spell's blue-blue cost",
                ai.getCardsIn(ZoneType.Graveyard).contains(temple));
        AssertJUnit.assertTrue("all mana was spent on Counterspell", ai.getManaPool().isEmpty());
    }

    @Test
    public void svyeluniteTempleIsKeptWhenOrdinaryBluePaysForTheSpell() {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Card temple = addCard("Svyelunite Temple", ai);
        temple.setTapped(false);
        Card opt = addCardToZone("Opt", ai, ZoneType.Hand);
        SpellAbility sa = opt.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue(ComputerUtilMana.payManaCost(sa.getPayCosts(), ai, sa, false));
        AssertJUnit.assertTrue("Temple's ordinary blue mode is used instead of sacrificing it", temple.isTapped());
        AssertJUnit.assertTrue(!ai.getCardsIn(ZoneType.Graveyard).contains(temple));
    }

    @Test
    public void svyeluniteTempleIsKeptWhenAnIslandCanPayTheSecondBlue() {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Card temple = addCard("Svyelunite Temple", ai);
        Card island = addCard("Island", ai);
        Card counterspell = addCardToZone("Counterspell", ai, ZoneType.Hand);
        SpellAbility sa = counterspell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue(ComputerUtilMana.payManaCost(sa.getPayCosts(), ai, sa, false));
        AssertJUnit.assertFalse("Temple should not be sacrificed when Island supplies the second blue",
                ai.getCardsIn(ZoneType.Graveyard).contains(temple));
        AssertJUnit.assertTrue("Island should supply one of the two blue mana", island.isTapped());
    }

    @Test
    public void svyeluniteTempleKeepsItsExistingConstructedBlueBlueRefusal() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Svyelunite Temple", ai);
        Card counterspell = addCardToZone("Counterspell", ai, ZoneType.Hand);
        SpellAbility sa = counterspell.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertFalse(ComputerUtilMana.canPayManaCost(sa, ai, 0, false));
    }

    @Test
    public void svyeluniteTempleDoesNotActivateForManaWithoutAUsefulNeed() {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Card temple = addCard("Svyelunite Temple", ai);
        temple.setTapped(false);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        SpellAbility sacrificeForMana = temple.getSpellAbilities().stream()
                .filter(ability -> ability.getPayCosts().hasSpecificCostType(CostSacrifice.class))
                .findFirst().orElseThrow();
        sacrificeForMana.setActivatingPlayer(ai);
        AssertJUnit.assertFalse("Temple should be kept when no cast needs the mana",
                SpellApiToAi.Converter.get(sacrificeForMana).canPlayWithSubs(ai, sacrificeForMana).willingToPlay());
    }

    private String predictNameWithUnknownCards(String libraryCardName, String handCardName) {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card predict = addCardToZone("Predict", ai, ZoneType.Hand);
        addCardToZone(libraryCardName, ai, ZoneType.Library);
        addCardToZone(handCardName, opponent, ZoneType.Hand);
        SpellAbility sa = predict.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);

        return choosePredictName(ai, sa);
    }

    private String choosePredictName(Player ai, SpellAbility sa) {
        return ((PlayerControllerAi) ai.getController()).chooseCardName(sa, face -> true, "Card", "");
    }

    private AiPlayDecision metamorphoseDecisionWithOpponentHandCard(String hiddenCardName) {
        Game game = createDanDanGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCards("Island", 3, ai);
        addCard("Island", opponent);
        Card target = addCard("Dandân", opponent);
        addOpponentPumpTargeting(game, opponent, target);
        addCardToZone(hiddenCardName, opponent, ZoneType.Hand);
        Card metamorphose = addCardToZone("Metamorphose", ai, ZoneType.Hand);
        SpellAbility sa = metamorphose.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        return ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa);
    }

    private void addOpponentPumpTargeting(Game game, Player opponent, Card target) {
        Card giantGrowth = addCardToZone("Giant Growth", opponent, ZoneType.Hand);
        SpellAbility pump = giantGrowth.getFirstSpellAbility();
        pump.setActivatingPlayer(opponent);
        pump.getTargets().add(target);
        game.getStack().addAndUnfreeze(pump);
    }

    private Game createDanDanGame() {
        initAndCreateGame(); // initializes the shared card database before game construction
        List<RegisteredPlayer> players = Lists.newArrayList();
        Deck deck = new Deck();
        players.add(new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("p2", null)));
        players.add(new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("p1", null)));
        GameRules rules = new GameRules(GameType.DanDan);
        Match match = new Match(rules, players, "Dan Dan AI Test");
        Game game = new Game(players, rules, match);
        Player ai = game.getPlayers().get(1);
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    private void addCardsToZone(String name, int count, Player player, ZoneType zone) {
        for (int i = 0; i < count; i++) {
            addCardToZone(name, player, zone);
        }
    }
}
