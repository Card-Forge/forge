package forge.game;

import com.google.common.eventbus.Subscribe;
import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.deck.Deck;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventShuffle;
import forge.game.event.GameEventZone;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.replacement.ReplacementEffect;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DanDanDrawTest extends AITest {
    @Test
    public void oneDrawInstructionAlternatesBetweenPlayers() {
        Game danDan = newGame(true);
        Player active = danDan.getRegisteredPlayers().get(1);
        Player nonactive = danDan.getRegisteredPlayers().get(0);
        danDan.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        RevealRecordingController activeController = new RevealRecordingController(danDan, active);
        RevealRecordingController nonactiveController = new RevealRecordingController(danDan, nonactive);
        active.dangerouslySetController(activeController);
        nonactive.dangerouslySetController(nonactiveController);
        List<Card> tops = addKnownTop(active, 4);

        Card daysUndoing = addCardToZone("Day's Undoing", active, ZoneType.Graveyard);
        var drawInstruction = daysUndoing.getFirstSpellAbility().getSubAbility();
        drawInstruction.putParam("NumCards", "2");
        drawInstruction.putParam("Reveal", "All");
        drawInstruction.putParam("RememberDrawn", "True");
        drawInstruction.setActivatingPlayer(active);
        AbilityUtils.resolve(drawInstruction);

        assertEquals(activeController.undoBarrierReasons, List.of("Card drawn", "Card drawn"),
                "both completed draw batches reach the active controller's reveal callback under the draw barrier");
        assertEquals(nonactiveController.undoBarrierReasons, List.of("Card drawn", "Card drawn"),
                "both completed draw batches reach the nonactive controller's reveal callback under the draw barrier");
        assertEquals(ids(active.getCardsIn(ZoneType.Hand)), List.of(tops.get(0).getId(), tops.get(2).getId()));
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of(tops.get(1).getId(), tops.get(3).getId()));
        assertEquals(active.getNumDrawnThisTurn(), 2);
        assertEquals(nonactive.getNumDrawnThisTurn(), 2);
        assertTrue(tops.stream().allMatch(daysUndoing::isRemembered),
                "the outer Draw effect remembers each player's completed result");
        assertEquals(activeController.revealedCardIds,
                List.of(List.of(tops.get(0).getId(), tops.get(2).getId()),
                        List.of(tops.get(1).getId(), tops.get(3).getId())));
        assertEquals(nonactiveController.revealedCardIds, activeController.revealedCardIds,
                "Reveal All sends each completed player result as one grouped reveal to both players");
        assertEquals(ids(danDan.getCardsIn(ZoneType.Library)), List.of());
    }

    @Test
    public void siblingWholeEventReplacementsEachCaptureTheirOwnDrawResult() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 6);
        addCardToZone("Quantum Riddler", active, ZoneType.Battlefield);
        addCardToZone("Quantum Riddler", nonactive, ZoneType.Battlefield);

        Card daysUndoing = addCardToZone("Day's Undoing", active, ZoneType.Graveyard);
        var drawInstruction = daysUndoing.getFirstSpellAbility().getSubAbility();
        drawInstruction.putParam("NumCards", "2");
        drawInstruction.setActivatingPlayer(active);
        AbilityUtils.resolve(drawInstruction);

        assertEquals(ids(active.getCardsIn(ZoneType.Hand)),
                List.of(tops.get(0).getId(), tops.get(2).getId(), tops.get(4).getId()));
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)),
                List.of(tops.get(1).getId(), tops.get(3).getId(), tops.get(5).getId()));
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of());
    }

    @Test
    public void exhaustedSharedLibraryFailsBothPlayersAfterTheirSuccessfulSlots() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 2);

        Card divination = addCardToZone("Divination", active, ZoneType.Graveyard);
        SpellAbility drawInstruction = divination.getSpells().get(0);
        drawInstruction.putParam("Defined", "Player");
        drawInstruction.putParam("NumCards", "2");
        drawInstruction.setActivatingPlayer(active);
        AbilityUtils.resolve(drawInstruction);

        assertEquals(ids(active.getCardsIn(ZoneType.Hand)), List.of(tops.get(0).getId()));
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of(tops.get(1).getId()));
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of());
        assertTrue(active.isInGame(), "the failed draw is pending until state-based actions are checked");
        assertTrue(nonactive.isInGame(), "the failed draw is pending until state-based actions are checked");
        game.getAction().checkStateEffects(true);
        assertTrue(active.hasLost(), "the active player loses after attempting the failed draw");
        assertTrue(nonactive.hasLost(), "the nonactive player loses after attempting the failed draw");
    }

    @Test
    public void individualDrawReplacementPreventsOnlyItsOwnersFailedDraw() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> top = addKnownTop(active, 1);
        addCardToZone("Obstinate Familiar", active, ZoneType.Battlefield);
        ReplacementChoiceController controller = new ReplacementChoiceController(game, active, true);
        active.dangerouslySetController(controller);

        Card divination = addCardToZone("Divination", active, ZoneType.Graveyard);
        SpellAbility draw = divination.getSpells().get(0);
        draw.putParam("Defined", "Player");
        draw.putParam("NumCards", "2");
        draw.setActivatingPlayer(active);
        AbilityUtils.resolve(draw);

        assertEquals(controller.drawReplacementChoices, 2,
                "the active player's Familiar replacement is accepted for both draw attempts");
        assertEquals(ids(active.getCardsIn(ZoneType.Hand)), List.of());
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of(top.get(0).getId()));
        assertEquals(active.getNumDrawnThisTurn(), 0);
        assertEquals(nonactive.getNumDrawnThisTurn(), 1);
        assertTrue(active.isInGame());
        assertTrue(nonactive.isInGame());

        game.getAction().checkStateEffects(true);
        assertEquals(active.hasLost(), false,
                "the player whose failed draw was replaced does not lose");
        assertEquals(nonactive.hasLost(), true,
                "the other player loses after their unreplaceable empty-library draw");
    }

    @Test
    public void individualDrawReplacementFinishesItsNestedDrawBeforeNextSlot() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 6);
        addCardToZone("Thought Reflection", active, ZoneType.Battlefield);

        Card daysUndoing = addCardToZone("Day's Undoing", active, ZoneType.Graveyard);
        var drawInstruction = daysUndoing.getFirstSpellAbility().getSubAbility();
        drawInstruction.putParam("NumCards", "2");
        drawInstruction.setActivatingPlayer(active);
        AbilityUtils.resolve(drawInstruction);

        assertEquals(ids(active.getCardsIn(ZoneType.Hand)), List.of(
                tops.get(0).getId(), tops.get(1).getId(), tops.get(3).getId(), tops.get(4).getId()));
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of(tops.get(2).getId(), tops.get(5).getId()));
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of());
    }

    @Test
    public void wholeReplacementAncestrySurvivesNestedIndividualDraws() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 8);
        addCardToZone("Quantum Riddler", active, ZoneType.Battlefield);
        addCardToZone("Thought Reflection", active, ZoneType.Battlefield);

        Card daysUndoing = addCardToZone("Day's Undoing", active, ZoneType.Graveyard);
        var drawInstruction = daysUndoing.getFirstSpellAbility().getSubAbility();
        drawInstruction.putParam("NumCards", "2");
        drawInstruction.setActivatingPlayer(active);
        AbilityUtils.resolve(drawInstruction);

        assertEquals(ids(active.getCardsIn(ZoneType.Hand)), List.of(
                tops.get(0).getId(), tops.get(1).getId(), tops.get(3).getId(), tops.get(4).getId(),
                tops.get(6).getId(), tops.get(7).getId()));
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of(tops.get(2).getId(), tops.get(5).getId()));
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of());
    }

    @Test
    public void rememberedDrawResultIsReadyForTheRealSubAbility() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 2);
        Card sorcerer = addCardToZone("Krovikan Sorcerer", active, ZoneType.Battlefield);
        var drawAndDiscard = sorcerer.getSpellAbilities().stream()
                .filter(ability -> ability.hasParam("RememberDrawn"))
                .findFirst().orElseThrow();
        drawAndDiscard.setActivatingPlayer(active);

        AbilityUtils.resolve(drawAndDiscard);

        List<Integer> hand = ids(active.getCardsIn(ZoneType.Hand));
        List<Integer> graveyard = ids(active.getCardsIn(ZoneType.Graveyard));
        assertEquals(hand.size(), 1);
        assertEquals(graveyard.size(), 1);
        assertTrue(tops.stream().anyMatch(card -> card.getId() == hand.get(0)));
        assertTrue(tops.stream().anyMatch(card -> card.getId() == graveyard.get(0)));
    }

    @Test
    public void allReplacedDrawsRemainAvailableToTheRealSubAbility() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 4);
        addCardToZone("Thought Reflection", active, ZoneType.Battlefield);
        Card sorcerer = addCardToZone("Krovikan Sorcerer", active, ZoneType.Battlefield);
        var drawAndDiscard = sorcerer.getSpellAbilities().stream()
                .filter(ability -> ability.hasParam("RememberDrawn"))
                .findFirst().orElseThrow();
        drawAndDiscard.putParam("RememberDrawn", "AllReplaced");
        drawAndDiscard.setActivatingPlayer(active);

        AbilityUtils.resolve(drawAndDiscard);

        List<Integer> hand = ids(active.getCardsIn(ZoneType.Hand));
        List<Integer> graveyard = ids(active.getCardsIn(ZoneType.Graveyard));
        assertEquals(hand.size(), 3);
        assertEquals(graveyard.size(), 1);
        assertTrue(hand.stream().allMatch(id -> tops.stream().anyMatch(card -> card.getId() == id)));
        assertTrue(tops.stream().anyMatch(card -> card.getId() == graveyard.get(0)));
    }

    @Test
    public void actualDaysUndoingInterleavesTheSharedTwelveToFourteenCardPool() {
        assertActualDaysUndoingPool(12, 6, 6, true, true);
        assertActualDaysUndoingPool(13, 7, 6, false, true);
        assertActualDaysUndoingPool(14, 7, 7, false, false);
    }

    @Test
    public void upToDrawHonorsZeroAndEachPlayersChosenCount() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 2);
        NumberChoosingController activeController = new NumberChoosingController(game, active, 0);
        NumberChoosingController nonactiveController = new NumberChoosingController(game, nonactive, 1);
        active.dangerouslySetController(activeController);
        nonactive.dangerouslySetController(nonactiveController);

        Card daysUndoing = addCardToZone("Day's Undoing", active, ZoneType.Graveyard);
        var drawInstruction = daysUndoing.getFirstSpellAbility().getSubAbility();
        drawInstruction.putParam("Defined", "Player");
        drawInstruction.putParam("NumCards", "2");
        drawInstruction.putParam("Upto", "True");
        drawInstruction.setActivatingPlayer(active);
        AbilityUtils.resolve(drawInstruction);

        assertEquals(activeController.numberChoices, 1);
        assertEquals(nonactiveController.numberChoices, 1);
        assertEquals(activeController.drawChoices, 1);
        assertEquals(nonactiveController.drawChoices, 1);
        assertEquals(ids(active.getCardsIn(ZoneType.Hand)), List.of());
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of(tops.get(0).getId()));
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of(tops.get(1).getId()));
    }

    @Test
    public void optionalDrawUsesEachActualPlayersDecision() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 1);
        DrawConfirmationController activeController = new DrawConfirmationController(game, active, false);
        DrawConfirmationController nonactiveController = new DrawConfirmationController(game, nonactive, true);
        active.dangerouslySetController(activeController);
        nonactive.dangerouslySetController(nonactiveController);
        Card kwain = addCardToZone("Kwain, Itinerant Meddler", active, ZoneType.Battlefield);
        SpellAbility ability = kwain.getSpellAbilities().stream()
                .filter(candidate -> candidate.getApi() == forge.game.ability.ApiType.Draw)
                .findFirst().orElseThrow();
        assertEquals(active.getController(), activeController);
        assertEquals(nonactive.getController(), nonactiveController);
        ability.setActivatingPlayer(active);

        AbilityUtils.resolve(ability);

        assertEquals(activeController.drawChoices, 1);
        assertEquals(nonactiveController.drawChoices, 1);
        assertEquals(ids(active.getCardsIn(ZoneType.Hand)), List.of());
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of(tops.get(0).getId()));
        assertEquals(active.getLife(), 20);
        assertEquals(nonactive.getLife(), 21);
    }

    @Test
    public void duplicateDefinedPlayerMultipliesOneDrawRequest() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 4);

        Card daysUndoing = addCardToZone("Day's Undoing", active, ZoneType.Graveyard);
        var drawInstruction = daysUndoing.getFirstSpellAbility().getSubAbility();
        drawInstruction.putParam("Defined", "You & You");
        drawInstruction.putParam("NumCards", "2");
        drawInstruction.setActivatingPlayer(active);
        AbilityUtils.resolve(drawInstruction);

        assertEquals(ids(active.getCardsIn(ZoneType.Hand)), ids(tops));
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of());
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of());
    }

    @Test
    public void separateCutADealDrawAbilitiesKeepTheirOwnInstructions() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 2);
        Card cutADeal = addCardToZone("Cut a Deal", active, ZoneType.Hand);

        resolveSpell(game, cutADeal, active);

        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of(tops.get(0).getId()));
        assertEquals(ids(active.getCardsIn(ZoneType.Hand)), List.of(tops.get(1).getId()),
                "the subability is a separate draw instruction after the opponent's first request");
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of());
    }

    @Test
    public void wholeEventDrawReplacementCountSetsAlternatingSlots() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 5);
        addCardToZone("Quantum Riddler", nonactive, ZoneType.Battlefield);

        Card daysUndoing = addCardToZone("Day's Undoing", active, ZoneType.Graveyard);
        var drawInstruction = daysUndoing.getFirstSpellAbility().getSubAbility();
        drawInstruction.putParam("NumCards", "2");
        drawInstruction.setActivatingPlayer(active);
        AbilityUtils.resolve(drawInstruction);

        assertEquals(ids(active.getCardsIn(ZoneType.Hand)), List.of(tops.get(0).getId(), tops.get(2).getId()));
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)),
                List.of(tops.get(1).getId(), tops.get(3).getId(), tops.get(4).getId()));
        assertEquals(active.getNumDrawnThisTurn(), 2);
        assertEquals(nonactive.getNumDrawnThisTurn(), 3);
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of());
    }

    @Test
    public void almsCollectorReplacementDrawsOnlyItsReplacementPlayers() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 4);
        addCardToZone("Alms Collector", active, ZoneType.Battlefield);

        Card daysUndoing = addCardToZone("Day's Undoing", active, ZoneType.Graveyard);
        var drawInstruction = daysUndoing.getFirstSpellAbility().getSubAbility();
        drawInstruction.putParam("Defined", "Opponent");
        drawInstruction.putParam("NumCards", "2");
        drawInstruction.setActivatingPlayer(active);
        AbilityUtils.resolve(drawInstruction);

        assertEquals(ids(active.getCardsIn(ZoneType.Hand)), List.of(tops.get(0).getId()));
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of(tops.get(1).getId()));
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of(tops.get(2).getId(), tops.get(3).getId()));
    }

    @Test
    public void almsReplacementRecipientsInterleaveWithAnotherPlayerRequest() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 4);
        addCardToZone("Alms Collector", active, ZoneType.Battlefield);

        Card daysUndoing = addCardToZone("Day's Undoing", active, ZoneType.Graveyard);
        var drawInstruction = daysUndoing.getFirstSpellAbility().getSubAbility();
        drawInstruction.putParam("Defined", "Player");
        drawInstruction.putParam("NumCards", "2");
        drawInstruction.setActivatingPlayer(active);
        AbilityUtils.resolve(drawInstruction);

        assertEquals(ids(active.getCardsIn(ZoneType.Hand)),
                List.of(tops.get(0).getId(), tops.get(2).getId(), tops.get(3).getId()));
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of(tops.get(1).getId()));
        assertEquals(active.getNumDrawnThisTurn(), 3);
        assertEquals(nonactive.getNumDrawnThisTurn(), 1);
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of());
    }

    @Test
    public void optionalIndividualReplacementCompletesBeforeNextPlayerDraws() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 4);
        addCardToZone("Obstinate Familiar", active, ZoneType.Battlefield);
        ReplacementChoiceController controller = new ReplacementChoiceController(game, active, false);
        active.dangerouslySetController(controller);

        Card daysUndoing = addCardToZone("Day's Undoing", active, ZoneType.Graveyard);
        var drawInstruction = daysUndoing.getFirstSpellAbility().getSubAbility();
        drawInstruction.putParam("NumCards", "2");
        drawInstruction.setActivatingPlayer(active);
        AbilityUtils.resolve(drawInstruction);

        assertEquals(controller.drawReplacementChoices, 2,
                "the optional individual replacement is offered for each active-player draw");
        assertEquals(ids(active.getCardsIn(ZoneType.Hand)), List.of(tops.get(0).getId(), tops.get(2).getId()));
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of(tops.get(1).getId(), tops.get(3).getId()));
    }

    @Test
    public void individualDrawReplacementChangesTopBeforeFollowingSlot() {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> tops = addKnownTop(active, 8);
        addCardToZone("Underrealm Lich", active, ZoneType.Battlefield);

        Card daysUndoing = addCardToZone("Day's Undoing", active, ZoneType.Graveyard);
        var drawInstruction = daysUndoing.getFirstSpellAbility().getSubAbility();
        drawInstruction.putParam("NumCards", "2");
        drawInstruction.setActivatingPlayer(active);
        AbilityUtils.resolve(drawInstruction);

        List<Integer> activeDraws = ids(active.getCardsIn(ZoneType.Hand));
        assertEquals(activeDraws.size(), 2);
        assertTrue(tops.subList(0, 3).stream().anyMatch(card -> card.getId() == activeDraws.get(0)));
        assertTrue(tops.subList(4, 7).stream().anyMatch(card -> card.getId() == activeDraws.get(1)));
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)), List.of(tops.get(3).getId(), tops.get(7).getId()));
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of());
    }

    private List<Card> addKnownTop(Player backingPlayer, int count) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Card card = createCard(i % 2 == 0 ? "Island" : "Memory Lapse", backingPlayer);
            backingPlayer.getZone(ZoneType.Library).add(card);
            cards.add(card);
        }
        return cards;
    }

    private void assertActualDaysUndoingPool(int poolSize, int expectedActiveHand, int expectedNonactiveHand,
            boolean activeLoses, boolean nonactiveLoses) {
        Game game = newGame(true);
        Player active = game.getRegisteredPlayers().get(0);
        Player nonactive = game.getRegisteredPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, active);
        List<Card> pool = addDaysUndoingPool(active, nonactive, poolSize);
        Card daysUndoing = addCardToZone("Day's Undoing", active, ZoneType.Hand);
        int spellId = daysUndoing.getId();
        SpellAbility spell = daysUndoing.getSpells().get(0);
        spell.setActivatingPlayer(active);
        game.getStack().addAndUnfreeze(spell);
        ShuffleEventRecorder shuffles = new ShuffleEventRecorder();
        game.subscribeToEvents(shuffles);
        ZoneEventRecorder zones = new ZoneEventRecorder();
        game.subscribeToEvents(zones);

        Random originalRandom = MyRandom.getRandom();
        try {
            MyRandom.setRandom(new Random(20261008L + poolSize));
            game.getStack().resolveStack();
        } finally {
            MyRandom.setRandom(originalRandom);
        }

        assertEquals(ids(active.getCardsIn(ZoneType.Hand)).size(), expectedActiveHand);
        assertEquals(ids(nonactive.getCardsIn(ZoneType.Hand)).size(), expectedNonactiveHand);
        assertEquals(allHandIds(active, nonactive), new HashSet<>(ids(pool)),
                "each starting hand/graveyard card is drawn at most once into the shared result hands");
        assertEquals(ids(game.getCardsIn(ZoneType.Library)), List.of());
        assertEquals(ids(game.getCardsIn(ZoneType.Graveyard)), List.of());
        assertTrue(zones.exileAddedCardIds.contains(spellId),
                "the active caster's end-turn effect exiles the resolving spell");
        assertTrue(game.getPhaseHandler().is(PhaseType.CLEANUP));
        assertEquals(active.hasLost(), activeLoses);
        assertEquals(nonactive.hasLost(), nonactiveLoses);
        assertEquals(shuffles.playerIds.size(), 2);
        assertEquals(new HashSet<>(shuffles.playerIds), Set.of(active.getId(), nonactive.getId()),
                "each player receives a separately attributed shuffle event");
    }

    private List<Card> addDaysUndoingPool(Player active, Player nonactive, int count) {
        List<Card> pool = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            pool.add(addCardToZone(i % 2 == 0 ? "Island" : "Memory Lapse", active, ZoneType.Hand));
            pool.add(addCardToZone(i % 2 == 0 ? "Memory Lapse" : "Island", nonactive, ZoneType.Hand));
        }
        for (int i = 12; i < count; i++) {
            pool.add(addCardToZone(i % 2 == 0 ? "Island" : "Memory Lapse", active, ZoneType.Graveyard));
        }
        return pool;
    }

    private void resolveSpell(Game game, Card card, Player caster) {
        SpellAbility spell = card.getSpells().get(0);
        spell.setActivatingPlayer(caster);
        game.getStack().addAndUnfreeze(spell);
        game.getStack().resolveStack();
    }

    private Set<Integer> allHandIds(Player active, Player nonactive) {
        Set<Integer> ids = new HashSet<>(ids(active.getCardsIn(ZoneType.Hand)));
        ids.addAll(ids(nonactive.getCardsIn(ZoneType.Hand)));
        return ids;
    }

    private Game newGame(boolean danDan) {
        ArrayList<RegisteredPlayer> players = new ArrayList<>();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("first", null)));
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("second", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        if (danDan) {
            rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        }
        Match match = new Match(rules, players, "DanDan draw sequencing test");
        Game game = match.createGame();
        game.setAge(GameStage.Play);
        return game;
    }

    private static final class ReplacementChoiceController extends PlayerControllerAi {
        private final boolean accept;
        private int drawReplacementChoices;

        private ReplacementChoiceController(Game game, Player player, boolean accept) {
            super(game, player, new LobbyPlayerAi(player.getName(), null));
            this.accept = accept;
        }

        @Override
        public boolean confirmReplacementEffect(ReplacementEffect replacementEffect, SpellAbility effectSA,
                GameEntity affected, String question) {
            if (replacementEffect.getHostCard().getName().equals("Obstinate Familiar")) {
                drawReplacementChoices++;
                return accept;
            }
            return super.confirmReplacementEffect(replacementEffect, effectSA, affected, question);
        }
    }

    private static final class RevealRecordingController extends PlayerControllerAi {
        private final List<List<Integer>> revealedCardIds = new ArrayList<>();
        private final List<String> undoBarrierReasons = new ArrayList<>();

        private RevealRecordingController(Game game, Player player) {
            super(game, player, new LobbyPlayerAi(player.getName(), null));
        }

        @Override
        public void reveal(CardCollectionView cards, ZoneType zone, Player owner, String messagePrefix,
                boolean addSuffix) {
            undoBarrierReasons.add(getGame().getUndoManager().whyNot());
            revealedCardIds.add(ids(cards));
        }
    }

    private static final class NumberChoosingController extends PlayerControllerAi {
        private final int choice;
        private int numberChoices;
        private int drawChoices;

        private NumberChoosingController(Game game, Player player, int choice) {
            super(game, player, new LobbyPlayerAi(player.getName(), null));
            this.choice = choice;
        }

        @Override
        public int chooseNumber(SpellAbility sa, String title, int min, int max) {
            numberChoices++;
            return Math.max(min, Math.min(max, choice));
        }

        @Override
        public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message,
                List<String> options, Card cardToShow, Map<String, Object> params) {
            drawChoices++;
            return true;
        }
    }

    private static final class DrawConfirmationController extends PlayerControllerAi {
        private final boolean accept;
        private int drawChoices;

        private DrawConfirmationController(Game game, Player player, boolean accept) {
            super(game, player, new LobbyPlayerAi(player.getName(), null));
            this.accept = accept;
        }

        @Override
        public boolean confirmAction(SpellAbility sa, PlayerActionConfirmMode mode, String message,
                List<String> options, Card cardToShow, Map<String, Object> params) {
            drawChoices++;
            return accept;
        }
    }

    private static final class ShuffleEventRecorder {
        private final List<Integer> playerIds = new ArrayList<>();

        @Subscribe
        public void onShuffle(GameEventShuffle event) {
            playerIds.add(event.player().getId());
        }
    }

    private static final class ZoneEventRecorder {
        private final List<Integer> exileAddedCardIds = new ArrayList<>();

        @Subscribe
        public void onZone(GameEventZone event) {
            if (event.zoneType() == ZoneType.Exile && event.mode() == EventValueChangeType.Added
                    && event.card() != null) {
                exileAddedCardIds.add(event.card().getId());
            }
        }
    }

    private static List<Integer> ids(Iterable<Card> cards) {
        List<Integer> result = new ArrayList<>();
        for (Card card : cards) result.add(card.getId());
        return result;
    }
}
