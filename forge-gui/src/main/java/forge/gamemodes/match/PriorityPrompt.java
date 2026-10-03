package forge.gamemodes.match;

import java.util.List;

import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.interfaces.IGameController;
import forge.interfaces.IMacroSystem;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.util.ThreadUtil;

/**
 * The GUI side of a priority question: shows the prompt and turns the player's clicks and buttons into answers or
 * yield updates. The host only asks the priority question and checks the answer.
 */
public final class PriorityPrompt {
    private final AbstractGuiGame gui;
    private final Question.Priority question;
    /** A yield suggestion the host's yield layer made for this prompt; null once answered or when there is none. */
    private volatile SuggestionType suggestion;

    PriorityPrompt(final AbstractGuiGame gui, final Question.Priority question, final SuggestionType suggestion) {
        this.gui = gui;
        this.question = question;
        this.suggestion = suggestion;
    }

    private IGameController controller() {
        return gui.getGameController(question.player());
    }

    void show() {
        final Localizer localizer = Localizer.getInstance();
        final PlayerView player = question.player();
        if (suggestion != null) {
            gui.showPromptMessage(player, suggestionMessage(suggestion));
            gui.updateButtons(player, localizer.getMessage("lblAccept"), localizer.getMessage("lblDecline"), true, true, true);
        } else {
            gui.showPromptMessage(player, turnMessage(gui.getGameView(), player, gui.getDayTime(), controller().macros()));
            final String cancel = question.undoCount() > 0
                    ? localizer.getMessage("lblUndo") + " (" + question.undoCount() + ")"
                    : localizer.getMessage("lblEndTurn");
            gui.updateButtons(player, localizer.getMessage("lblOK"), cancel, true, true, true);
        }
        gui.alertUser();
    }

    private String suggestionMessage(final SuggestionType type) {
        final Localizer localizer = Localizer.getInstance();
        String message = localizer.getMessage(type == SuggestionType.STACK_YIELD
                ? "lblCannotRespondToStackYieldPrompt" : "lblNoActionsAvailableYieldPrompt");
        final DeclineScope scope = controller().getYieldController().getDeclineScope(type.scopePref());
        if (scope == DeclineScope.STACK) {
            message += "\n" + localizer.getMessage("lblYieldSuggestionDeclineHintStack");
        } else if (scope == DeclineScope.TURN) {
            message += "\n" + localizer.getMessage("lblYieldSuggestionDeclineHint");
        }
        return message;
    }

    /** Priority holder, turn, phase, stack and macro state, as the priority prompt shows them. */
    public static String turnMessage(final GameView gameView, final PlayerView priority, final String daytime, final IMacroSystem macros) {
        if (gameView == null) {
            return "";
        }
        final Localizer localizer = Localizer.getInstance();
        final StringBuilder sb = new StringBuilder();
        sb.append(localizer.getMessage("lblPriority")).append(": ").append(priority).append("\n");
        sb.append(localizer.getMessage("lblTurn")).append(": ").append(gameView.getTurn()).append(" (").append(gameView.getPlayerTurn()).append(")");
        if (daytime != null) {
            sb.append("  [").append(localizer.getMessage("lbl" + daytime)).append("]");
        }
        sb.append("\n");
        sb.append(localizer.getMessage("lblPhase")).append(": ").append(gameView.getPhase() == null ? "" : gameView.getPhase().nameForUi).append("\n");
        sb.append(localizer.getMessage("lblStack")).append(": ");
        if (gameView.getStack() != null && !gameView.getStack().isEmpty()) {
            sb.append(gameView.getStack().size()).append(" ").append(localizer.getMessage("lbltoResolve"));
        } else {
            sb.append(localizer.getMessage("lblEmpty"));
        }
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_SHOW_STORM_COUNT_IN_PROMPT) && gameView.getStormCount() > 0) {
            sb.append("\n").append(localizer.getMessage("lblStormCount")).append(": ").append(gameView.getStormCount());
        }
        if (macros != null) {
            final String playbackText = macros.playbackText();
            if (playbackText != null) {
                sb.append("\n").append(macros.isRecording() ? "Macro Recording -- " : "Macro Playback -- ").append(playbackText);
            } else if (macros.isRecording()) {
                sb.append("\n").append("Macro Recording -- ");
            }
        }
        return sb.toString();
    }

    boolean selectCard(final CardView card, final List<CardView> others, final ITriggerEvent triggerEvent) {
        final List<SpellAbilityView> abilities = question.abilities().get(card);
        if (abilities == null) {
            return false;
        }
        if (abilities.stream().noneMatch(SpellAbilityView::canPlay)) {
            // A pointer click on such a card is handled, so it does not fall through to the cards stacked under it
            return triggerEvent != null;
        }
        // Without a click to place a menu at, offer only what can be played
        final List<SpellAbilityView> offered = triggerEvent == null
                ? abilities.stream().filter(SpellAbilityView::canPlay).toList() : abilities;
        final SpellAbilityView chosen = gui.getAbilityToPlay(card, offered, triggerEvent);
        if (chosen != null) {
            controller().answer(new Answer.Play(question.id(), chosen, others));
        }
        return true;
    }

    void selectAbility(final SpellAbilityView ability) {
        controller().answer(new Answer.Play(question.id(), ability, null));
    }

    void ok() {
        final IGameController controller = controller();
        final SuggestionType accepted = suggestion;
        if (accepted == null) {
            passAfterWarning(controller, () -> { });
            return;
        }
        suggestion = null;
        final PlayerView self = question.player();
        // Pass now and leave the yield to cover the priorities after this one
        controller.answer(new Answer.Pass(question.id()));
        if (accepted == SuggestionType.STACK_YIELD) {
            controller.sendYieldUpdate(new YieldUpdate.StackYield(self, true, true));
        } else {
            // UPKEEP because UNTAP has no priority pass, so a marker on UNTAP could never fire
            controller.sendYieldUpdate(new YieldUpdate.SetMarker(self, PhaseType.UPKEEP,
                    YieldController.isPriorityAtOrPastMarker(gui.getGameView(), self, PhaseType.UPKEEP)));
        }
    }

    void cancel() {
        final IGameController controller = controller();
        // Cancel stops macro playback, which the controller handles
        if (controller.macros().isReplaying()) {
            controller.selectButtonCancel();
            return;
        }
        final SuggestionType declined = suggestion;
        if (declined != null) {
            suggestion = null;
            controller.sendYieldUpdate(new YieldUpdate.DeclineSuggestion(question.player(), declined));
            show();
        } else if (question.undoCount() > 0) {
            controller.answer(new Answer.Undo(question.id()));
        } else {
            // Pass now and leave the yield to cover the rest of the turn
            passAfterWarning(controller, () -> controller.sendYieldUpdate(
                    new YieldUpdate.SetAutoPassUntilEndOfTurn(question.player(), true)));
        }
    }

    /** Passes, first asking the player to confirm when passing would lose floating mana; {@code then} runs after the pass. */
    private void passAfterWarning(final IGameController controller, final Runnable then) {
        final Runnable pass = () -> {
            controller.answer(new Answer.Pass(question.id()));
            then.run();
        };
        if (!question.passLosesMana() || !FModel.getPreferences().getPrefBoolean(FPref.UI_MANA_LOST_PROMPT)) {
            pass.run();
            return;
        }
        // Off the GUI thread, because the confirmation blocks and the mobile GUI cannot block its own thread
        ThreadUtil.invokeInGameThread(() -> {
            final Localizer localizer = Localizer.getInstance();
            if (gui.showConfirmDialog(floatingManaWarning(question.passBurns()), localizer.getMessage("lblManaFloating"),
                    localizer.getMessage("lblOK"), localizer.getMessage("lblCancel"))) {
                pass.run();
            }
        });
    }

    /** The warning before passing would lose floating mana. */
    public static String floatingManaWarning(final boolean burns) {
        final Localizer localizer = Localizer.getInstance();
        final String message = localizer.getMessage("lblYouHaveManaFloatingInYourManaPoolCouldBeLostIfPassPriority");
        return burns ? message + " " + localizer.getMessage("lblYouWillTakeManaBurnDamageEqualAmountFloatingManaLostThisWay") : message;
    }
}
