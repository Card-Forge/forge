package forge.screens.rogue;

import forge.Forge;
import forge.gui.FThreads;
import forge.screens.FScreen;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A question the flow thread asks through a screen. The flow waits for the answer with no timeout; leaving the
 * screen answers nothing, and the controller can open the screen again. A CompletableFuture keeps an answer given
 * before the flow starts waiting, which WaitCallback can lose.
 */
public final class RogueQuestion<T> {
    private final CompletableFuture<T> answer = new CompletableFuture<>();
    private FScreen screen;

    private RogueQuestion() {
    }

    /** On the flow thread: shows the screen the function builds on the GUI thread and waits for its answer. */
    public static <T> T ask(Function<RogueQuestion<T>, FScreen> screenFor) {
        if (FThreads.isGuiThread()) {
            throw new IllegalStateException("A Rogue Commander question was asked on the GUI thread");
        }
        RogueQuestion<T> question = new RogueQuestion<>();
        FThreads.invokeInEdtLater(() -> {
            try {
                question.screen = screenFor.apply(question);
                RogueController.get().hold(question);
                Forge.openScreen(question.screen);
            } catch (RuntimeException e) {
                question.answer.completeExceptionally(e);
            }
        });
        try {
            return RogueController.get().await(question.answer);
        } finally {
            RogueController.get().release(question);
            FThreads.invokeInEdtLater(() -> {
                if (question.screen != null && Forge.getCurrentScreen() == question.screen) {
                    Forge.back();
                }
            });
        }
    }

    /** On the GUI thread. Only the first answer counts. */
    public void answer(T value) {
        answer.complete(value);
    }

    FScreen screen() {
        return screen;
    }
}
