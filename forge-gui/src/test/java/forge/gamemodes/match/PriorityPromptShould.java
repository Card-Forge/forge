package forge.gamemodes.match;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.interfaces.IGameController;
import forge.interfaces.IMacroSystem;
import forge.trackable.Tracker;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import org.mockito.InOrder;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * While the priority question is pending, the GUI turns the player's clicks and buttons into answers or yield updates, so
 * the host never interprets a click. These pin that mapping; with none pending every call must reach the host.
 */
public class PriorityPromptShould {

    private final Tracker tracker = new Tracker();
    private final PlayerView me = new PlayerView(1, tracker);
    private final CardView card = new CardView(10, tracker);
    private final CardView other = new CardView(11, tracker);
    private AbstractGuiGame gui;
    private IGameController host;
    private SpellAbilityView playable;

    @BeforeMethod
    public void setUp() {
        // The prompt draws its own labels
        Localizer.getInstance().initialize("en-US", "res/languages/");
        gui = mock(AbstractGuiGame.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
        host = mock(IGameController.class);
        when(host.macros()).thenReturn(mock(IMacroSystem.class));
        when(host.getYieldController()).thenReturn(mock(YieldController.class));
        gui.setOriginalGameController(me, host);
        playable = ability(true);
    }

    private static SpellAbilityView ability(final boolean canPlay) {
        final SpellAbilityView view = mock(SpellAbilityView.class);
        when(view.canPlay()).thenReturn(canPlay);
        return view;
    }

    private void ask(final int undoCount) {
        gui.setQuestion(me, new Question.Priority(7, me, Map.of(card, List.of(playable), other, List.of(ability(true))),
                undoCount, false, false));
    }

    @Test
    public void answerACardClickWithoutSendingTheClickToTheHost() {
        ask(0);
        doReturn(playable).when(gui).getAbilityToPlay(any(), any(), any());

        final boolean handled = gui.selectCard(card, null, null);

        assertTrue(handled);
        verify(host).answer(new Answer.Play(7, playable, null));
        verify(host, never()).selectCard(any(), any(), any());
    }

    @Test
    public void reportATapOnACardWithNothingPlayableAsUnhandled() {
        // Without a pointer event (mobile), false lets the GUI fall back to the zoom view
        gui.setQuestion(me, new Question.Priority(7, me, Map.of(card, List.of(ability(false))), 0, false, false));

        assertFalse(gui.selectCard(card, null, null));
        verify(host, never()).answer(any());
    }

    @Test
    public void reportAPointerClickOnACardWithNothingPlayableAsHandled() {
        // A desktop click on such a card must not fall through to cards stacked under it or attached to it
        gui.setQuestion(me, new Question.Priority(7, me, Map.of(card, List.of(ability(false))), 0, false, false));

        assertTrue(gui.selectCard(card, null, mock(ITriggerEvent.class)));
        verify(host, never()).answer(any());
    }

    @Test
    public void carryTheOtherSelectedCardsSoTheHostCanRepeatAManaAbility() {
        ask(0);
        doReturn(playable).when(gui).getAbilityToPlay(any(), any(), any());

        gui.selectCard(card, List.of(other), null);

        verify(host).answer(new Answer.Play(7, playable, List.of(other)));
    }

    @Test
    public void passOnOk() {
        ask(0);
        gui.selectButtonOk();
        verify(host).answer(new Answer.Pass(7));
    }

    @Test
    public void undoOnCancelWhenSomethingCanBeUndone() {
        ask(2);
        gui.selectButtonCancel();
        verify(host).answer(new Answer.Undo(7));
    }

    @Test
    public void endTheTurnByPassingNowThenYieldingForTheRestOfTheTurn() {
        // Passing first works even where the yield cannot pass by itself, such as while a macro records
        ask(0);
        gui.selectButtonCancel();

        final InOrder order = inOrder(host);
        order.verify(host).answer(new Answer.Pass(7));
        order.verify(host).sendYieldUpdate(new YieldUpdate.SetAutoPassUntilEndOfTurn(me, true));
    }

    @Test
    public void acceptASuggestionByPassingNowThenYielding() {
        gui.applyYieldUpdate(new YieldUpdate.Suggest(me, SuggestionType.STACK_YIELD));
        ask(0);

        gui.selectButtonOk();

        final InOrder order = inOrder(host);
        order.verify(host).answer(new Answer.Pass(7));
        order.verify(host).sendYieldUpdate(new YieldUpdate.StackYield(me, true, true));
    }

    @Test
    public void declineASuggestionThenOfferThePlainPrompt() {
        gui.applyYieldUpdate(new YieldUpdate.Suggest(me, SuggestionType.STACK_YIELD));
        ask(0);

        gui.selectButtonCancel();
        gui.selectButtonOk();

        verify(host).sendYieldUpdate(new YieldUpdate.DeclineSuggestion(me, SuggestionType.STACK_YIELD));
        verify(host).answer(new Answer.Pass(7));
    }

    @Test
    public void letCancelStopMacroPlayback() {
        final IMacroSystem macros = mock(IMacroSystem.class);
        when(macros.isReplaying()).thenReturn(true);
        when(host.macros()).thenReturn(macros);
        ask(2);

        gui.selectButtonCancel();

        verify(host).selectButtonCancel();
        verify(host, never()).answer(any());
    }

    @Test
    public void passEveryCallToTheHostWhenNoQuestionIsPending() {
        gui.selectCard(card, null, null);
        gui.selectAbility(playable);
        gui.selectButtonOk();
        gui.selectButtonCancel();

        verify(host).selectCard(card, null, null);
        verify(host).selectAbility(playable);
        verify(host).selectButtonOk();
        verify(host).selectButtonCancel();
        verify(host, never()).answer(any());
    }

    @Test
    public void withdrawAPendingQuestionWhenTheGameEnds() {
        ask(0);

        gui.afterGameEnd();
        gui.selectButtonOk();

        verify(host).selectButtonOk();
        verify(host, never()).answer(any());
    }
}
