package forge.screens.rogue;

import forge.Forge;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.flow.RogueStart;
import forge.gui.FThreads;
import forge.screens.home.HomeScreen;
import forge.util.Localizer;
import java.io.UncheckedIOException;
import java.util.List;

/** Opens Rogue Commander: the open question if one waits, else the Runs screen. */
public final class RogueMenu {
    private static boolean homeButtonAdded;

    private RogueMenu() {
    }

    public static void launch() {
        if (Forge.isLandscapeMode() && !homeButtonAdded) {
            homeButtonAdded = true;
            HomeScreen.instance.addButtonForMode("-" + Forge.getLocalizer().getMessage("lblRogueCommander"), e -> launch());
        }
        if (RogueController.get().reshowOpenQuestion()) {
            return;
        }
        try {
            RogueMetaProgress.getInstance();
        } catch (UncheckedIOException e) {
            Localizer localizer = Localizer.getInstance();
            RogueDialog.show(localizer.getMessage("lblRogueCommander"), localizer.getMessage("lblRogueProgressUnreadable"),
                List.of(localizer.getMessage("lblOK")), choice -> { });
            return;
        }
        openHome();
    }

    /** The Runs screen, or New Run when Runs would hold nothing: no run in progress and its tabs still locked. */
    static void openHome() {
        RogueController controller = RogueController.get();
        if (controller.busy()) {
            Forge.openScreen(new RogueRunsScreen());
            return;
        }
        controller.queue(() -> {
            boolean runsHoldSomething = controller.flow().load().isPresent() || RogueStart.tabsUnlocked(RogueMetaProgress.getInstance());
            FThreads.invokeInEdtLater(() -> Forge.openScreen(runsHoldSomething ? new RogueRunsScreen() : new RogueNewRunScreen()));
        });
    }
}
