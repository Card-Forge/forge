package forge.player;

import forge.ai.AITest;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.phase.PhaseType;
import forge.game.player.PlaySpellAbility;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.Answer;
import forge.gamemodes.match.Question;
import forge.gamemodes.match.SuggestionType;
import forge.gamemodes.match.YieldUpdate;
import forge.gamemodes.match.input.InputPassPriority;
import forge.gui.interfaces.IGuiGame;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * The priority question: it must offer what the engine allows on every card the player can see, nothing that would
 * identify a hidden card, and the host must apply only answers that are legal now.
 */
public class PriorityQuestionShould extends AITest {

    private static final String TAP_FOR_LIFE = "A:AB$ GainLife | Cost$ T | LifeAmount$ 1 | SpellDescription$ You gain 1 life.";

    private Game game;
    private Player human;
    private Player opponent;

    private Map<SpellAbilityView, SpellAbility> offered() {
        return new PlayerControllerHuman(game, human, new LobbyPlayerHuman("Human")).collectPriorityAbilities();
    }

    private void start() {
        game = initAndCreateGame();
        questionsAsked = 0;
        human = game.getPlayers().get(1);
        opponent = game.getPlayers().get(0);
        gui = mock(IGuiGame.class);
        when(gui.getGameView()).thenReturn(game.getView());
        controller = new PlayerControllerHuman(game, human, new LobbyPlayerHuman("Human"));
        controller.setGui(gui);
    }

    private Card put(final Player owner, final ZoneType zone, final String name, final String... scriptLines) {
        final List<String> script = new ArrayList<>(List.of("Name:" + name, "ManaCost:0"));
        script.addAll(List.of(scriptLines));
        final Card card = CardFactory.getCard(new PaperCard(CardRules.fromScript(script), "", CardRarity.Common), owner, game);
        card.setGameTimestamp(game.getNextTimestamp());
        owner.getZone(zone).add(card);
        return card;
    }

    private static List<String> offeredFor(final Map<SpellAbilityView, SpellAbility> offered, final Card card) {
        return offered.keySet().stream().filter(v -> card.getView().equals(v.getHostCard()))
                .map(v -> v.getDescription() + (v.canPlay() ? "" : " [unplayable]")).toList();
    }

    @Test
    public void neverOfferCardsThePlayerCannotSee() {
        // Listing an ability would identify the card. The player's own library top has an ability usable from the
        // library, so only the visibility rule keeps it out; the opponent's cards are excluded by other rules as well
        start();
        final Card theirHand = put(opponent, ZoneType.Hand, "Secret", "Types:Instant",
                "A:SP$ GainLife | LifeAmount$ 3 | SpellDescription$ You gain 3 life.");
        final Card theirTop = put(opponent, ZoneType.Library, "Hidden", "Types:Artifact", TAP_FOR_LIFE);
        final Card myTop = put(human, ZoneType.Library, "Mine", "Types:Artifact",
                "A:AB$ GainLife | Cost$ 0 | ActivationZone$ Library | LifeAmount$ 1 | SpellDescription$ You gain 1 life.");

        final Map<SpellAbilityView, SpellAbility> offered = offered();

        assertTrue(offeredFor(offered, theirHand).isEmpty(), "the opponent's hand");
        assertTrue(offeredFor(offered, theirTop).isEmpty(), "the top of the opponent's library");
        assertTrue(offeredFor(offered, myTop).isEmpty(), "the top of the player's own library");
    }

    @Test
    public void offerAnotherPlayersCardOnlyForAbilitiesThePlayerCanUseNow() {
        // "Any player may activate" passes the activator check, so only the playability rule keeps a currently
        // unusable one from arriving as a disabled entry on an opponent's permanent
        start();
        final Card shared = put(opponent, ZoneType.Battlefield, "Shared", "Types:Artifact",
                "A:AB$ GainLife | Cost$ T | AnyPlayer$ True | LifeAmount$ 1 | SpellDescription$ Any player gains 1 life.");
        shared.setTapped(true);

        assertTrue(offeredFor(offered(), shared).isEmpty());
    }

    private IGuiGame gui;
    private PlayerControllerHuman controller;
    private InputPassPriority prompt;
    private Question.Priority question;

    private int questionsAsked;

    /** Puts a real priority prompt on the input queue, as the game does, and keeps the question the mocked GUI is asked. */
    private void showPrompt() {
        prompt = new InputPassPriority(controller);
        controller.getInputQueue().setInput(prompt);
        question = nextQuestion();
    }

    /** Waits for the next question, because the queue shows a prompt on the GUI thread. */
    private Question.Priority nextQuestion() {
        questionsAsked++;
        verify(gui, timeout(5000).atLeast(questionsAsked)).setQuestion(any(), any());
        return lastQuestion();
    }

    private Question.Priority lastQuestion() {
        final ArgumentCaptor<Question> asked = ArgumentCaptor.forClass(Question.class);
        verify(gui, atLeastOnce()).setQuestion(any(), asked.capture());
        final List<Question> all = asked.getAllValues();
        questionsAsked = all.size();
        return (Question.Priority) all.get(all.size() - 1);
    }

    private void tapForMana(final Card land) {
        final SpellAbility mana = land.getManaAbilities().get(0);
        mana.setActivatingPlayer(human);
        PlaySpellAbility.playSpellAbility(controller, human, mana);
    }

    private SpellAbilityView offeredView(final Card card) {
        return question.abilities().get(card.getView()).get(0);
    }

    @Test
    public void refuseToPlayAnAbilityThatCannotBePlayedNow() {
        // Only activated abilities can arrive flagged unplayable, and nothing later re-checks them before play
        start();
        final Card relic = put(human, ZoneType.Battlefield, "Relic", "Types:Artifact", TAP_FOR_LIFE);
        relic.setTapped(true);
        showPrompt();

        prompt.answer(new Answer.Play(question.id(), offeredView(relic), null));

        assertNull(prompt.getChosenSa(), "the prompt is still waiting");
    }

    @Test
    public void ignoreAnAnswerToAnOlderQuestion() {
        start();
        final Card relic = put(human, ZoneType.Battlefield, "Relic", "Types:Artifact", TAP_FOR_LIFE);
        showPrompt();

        prompt.answer(new Answer.Play(question.id() - 1, offeredView(relic), null));
        assertNull(prompt.getChosenSa(), "a stale answer is ignored");

        prompt.answer(new Answer.Play(question.id(), offeredView(relic), null));
        assertNotNull(prompt.getChosenSa(), "the same answer to the current question is played");
    }

    @Test
    public void repeatAManaAbilityOnTheOtherSelectedCards() {
        // Selecting a stack of lands taps each of them for the same mana, as one action
        start();
        final Card first = addCard("Forest", human);
        final Card second = addCard("Forest", human);
        final Card third = addCard("Forest", human);
        showPrompt();

        prompt.answer(new Answer.Play(question.id(), offeredView(first), List.of(second.getView(), third.getView())));

        assertNotNull(prompt.getChosenSa());
        assertEquals(prompt.getChosenSa().size(), 3);
    }

    @Test
    public void askAgainAfterAnUndoWithTheUndoneState() {
        // Undo changes the game while the prompt stays up, so the question must be rebuilt rather than re-sent
        start();
        final Card forest = addCard("Forest", human);
        tapForMana(forest);
        showPrompt();
        assertTrue(question.undoCount() > 0, "tapping the land can be undone");
        assertFalse(offeredView(forest).canPlay(), "the tapped land cannot tap again");

        prompt.answer(new Answer.Undo(question.id()));

        final Question.Priority asked = lastQuestion();
        assertTrue(asked.id() > question.id(), "a new question was asked");
        assertEquals(asked.undoCount(), 0);
        assertTrue(asked.abilities().get(forest.getView()).get(0).canPlay(), "the untapped land can tap again");
    }

    @Test
    public void sendTheYieldLayersSuggestionAheadOfTheQuestionAndRecordADecline() {
        // The suggestion is the yield layer's, on its own channel; the question itself carries no interface policy
        start();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opponent);
        // Set every preference the suggestion reads, so the machine's own preferences cannot change the outcome
        controller.getYieldController().setPref(FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS, "TURN");
        controller.getYieldController().setPref(FPref.YIELD_DECLINE_SCOPE_STACK_YIELD, "NEVER");
        controller.getYieldController().setPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, "false");
        controller.getYieldController().setPref(FPref.YIELD_SUPPRESS_AFTER_END, "false");
        controller.getYieldController().setPref(FPref.YIELD_SUPPRESS_ON_OWN_TURN, "false");
        // As the available-actions scan reports when the player can do nothing
        human.getView().setHasAvailableActions(false);
        showPrompt();

        final YieldUpdate.Suggest suggest = new YieldUpdate.Suggest(human.getView(), SuggestionType.NO_ACTIONS);
        final InOrder order = inOrder(gui);
        order.verify(gui).applyYieldUpdate(suggest);
        order.verify(gui).setQuestion(any(), any());

        controller.applyYieldUpdate(new YieldUpdate.DeclineSuggestion(human.getView(), SuggestionType.NO_ACTIONS));
        prompt.showMessageInitial();

        verify(gui, times(1)).applyYieldUpdate(suggest);
    }

    @Test
    public void sendAPromptsYieldsToThePlayerWhoseTurnIsControlled() {
        // A GUI acts for a player it controls through its own player's controller, so a decline made on the
        // controlled player's prompt must still land on the controlled player's yield settings
        start();
        final PlayerControllerHuman controlled = new PlayerControllerHuman(opponent, new LobbyPlayerHuman("Controlled"), controller);
        controlled.getYieldController().setPref(FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS, "TURN");
        controller.getYieldController().setPref(FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS, "TURN");
        controller.getInputQueue().setInput(new InputPassPriority(controlled));

        controller.applyYieldUpdate(new YieldUpdate.DeclineSuggestion(opponent.getView(), SuggestionType.NO_ACTIONS));

        assertTrue(controlled.getYieldController().isSuggestionDeclined(SuggestionType.NO_ACTIONS), "the controlled player declined");
        assertFalse(controller.getYieldController().isSuggestionDeclined(SuggestionType.NO_ACTIONS), "the controlling player did not");
    }

    @Test
    public void flagThatPassingWouldLoseFloatingMana() {
        // The GUI warns before a pass from this flag, so it must reflect the pool when the question is asked
        start();
        showPrompt();
        assertFalse(question.passLosesMana(), "an empty pool has nothing to lose");

        tapForMana(addCard("Forest", human));
        showPrompt();

        assertTrue(question.passLosesMana(), "floating mana empties when the phase ends");
        assertFalse(question.passBurns(), "no mana burn without a rule that deals it");
    }
}
