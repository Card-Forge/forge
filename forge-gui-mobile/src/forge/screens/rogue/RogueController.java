package forge.screens.rogue;

import com.badlogic.gdx.Gdx;
import forge.Forge;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueTutorial;
import forge.gamemodes.rogue.RogueTutorialTriggers;
import forge.gamemodes.rogue.flow.RogueFlow;
import forge.gamemodes.rogue.flow.RogueStart;
import forge.gui.FThreads;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.tinylog.Logger;

/**
 * Runs Rogue Commander's flow on one thread of its own, one job at a time. Its name must not start with "Game":
 * ThreadUtil.isGameThread tests that prefix, and the engine would then run a whole match inline on it.
 */
public final class RogueController {
    private static RogueController instance;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Rogue flow");
        thread.setDaemon(true);
        return thread;
    });
    private final MobileRogueDecisions decisions = new MobileRogueDecisions();
    private final RogueFlow flow = new RogueFlow(decisions);
    private final AtomicInteger jobs = new AtomicInteger();
    private final AtomicInteger waiting = new AtomicInteger();
    private volatile RogueQuestion<?> open;

    private RogueController() {
    }

    public static synchronized RogueController get() {
        if (instance == null) {
            instance = new RogueController();
        }
        return instance;
    }

    public RogueFlow flow() {
        return flow;
    }

    public boolean busy() {
        return jobs.get() > 0;
    }

    /** A job runs and is not waiting for the player, so the page's buttons would only be ignored. */
    public boolean working() {
        return jobs.get() > 0 && waiting.get() == 0;
    }

    /** On the flow thread: waits for the player's answer, during which the buttons work. */
    <T> T await(CompletableFuture<T> answer) {
        waiting.incrementAndGet();
        Gdx.graphics.requestRendering();
        try {
            return answer.join();
        } finally {
            waiting.decrementAndGet();
        }
    }

    /** A player's command: ignored while another job runs, and shows the question again while one waits. */
    public void submit(Runnable job) {
        if (!submitIfIdle(job)) {
            reshowOpenQuestion();
        }
    }

    /** Work a screen does as it opens or closes: skipped while another job runs or waits. */
    public boolean submitIfIdle(Runnable job) {
        if (!jobs.compareAndSet(0, 1)) {
            return false;
        }
        execute(job);
        return true;
    }

    /** Work that must follow what runs now, such as the step after a match: always runs, in order. */
    public void queue(Runnable job) {
        jobs.incrementAndGet();
        execute(job);
    }

    private void execute(Runnable job) {
        executor.execute(() -> {
            try {
                job.run();
            } catch (RuntimeException e) {
                recover(e);
            } finally {
                jobs.decrementAndGet();
                Gdx.graphics.requestRendering();
            }
        });
    }

    /** The run in memory may be half changed, so the last save replaces it. */
    private void recover(RuntimeException e) {
        Logger.error(e, "Rogue Commander step failed");
        flow.setRun(null);
        // A run that failed to start has no save to go back to
        String message = flow.load().isPresent() ? "lblRogueStepFailed" : "lblRogueStepFailedNoRun";
        Localizer localizer = Localizer.getInstance();
        decisions.showMessage(localizer.getMessage("lblRogueCommander"), localizer.getMessage(message));
    }

    void hold(RogueQuestion<?> question) {
        open = question;
    }

    void release(RogueQuestion<?> question) {
        if (open == question) {
            open = null;
        }
    }

    /** On the GUI thread: returns to the mode's home, the Runs screen, opening it when the way back has none. */
    public static void backToRuns() {
        for (int i = 0; i < 12 && Forge.getCurrentScreen() instanceof RogueScreen
                && !(Forge.getCurrentScreen() instanceof RogueRunsScreen); i++) {
            Forge.back();
        }
        if (!(Forge.getCurrentScreen() instanceof RogueRunsScreen)) {
            RogueController controller = get();
            boolean runsHoldSomething = controller.flow().run() != null || RogueStart.tabsUnlocked(RogueMetaProgress.getInstance());
            Forge.openScreen(runsHoldSomething ? new RogueRunsScreen() : new RogueNewRunScreen());
        }
    }

    /** What the mode's home tells each time it opens, as desktop's start page does: stories between runs, then due tutorials. */
    public void betweenRuns() {
        queue(() -> {
            flow.betweenRuns();
            for (RogueTutorial tutorial : RogueTutorialTriggers.onStartPage(RogueMetaProgress.getInstance())) {
                decisions.showTutorial(tutorial);
            }
        });
    }

    /** Shows the tutorials not seen yet, in order, as one job; none to show takes no job. */
    public void showTutorials(List<RogueTutorial> tutorials) {
        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        List<RogueTutorial> due = new ArrayList<>();
        for (RogueTutorial tutorial : tutorials) {
            if (!progress.hasSeenTutorial(tutorial) && !RogueNodeQuestions.DESKTOP_ONLY.contains(tutorial)) {
                due.add(tutorial);
            }
        }
        if (!due.isEmpty()) {
            queue(() -> due.forEach(decisions::showTutorial));
        }
    }

    /** Shows the map of the run in memory after the jobs before it, or the open question again if one waits. */
    public void openMap() {
        if (reshowOpenQuestion()) {
            return;
        }
        queue(() -> {
            RogueRun run = flow.run();
            if (run != null) {
                run.getPath().revealVisible(run);
                FThreads.invokeInEdtLater(() -> Forge.openScreen(new RogueMapScreen()));
            }
        });
    }

    /** On the GUI thread: shows the open question again. Returns false when none is open. */
    public boolean reshowOpenQuestion() {
        RogueQuestion<?> question = open;
        if (question == null || question.screen() == null) {
            return false;
        }
        if (question.screen() instanceof RogueScreen screen) {
            screen.bringForward();
        }
        Forge.openScreen(question.screen());
        return true;
    }
}
