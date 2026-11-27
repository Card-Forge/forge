package forge.screens.rogue;

import forge.Forge;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.flow.RogueStart;
import forge.screens.FScreen;
import forge.util.Localizer;
import java.util.List;
import java.util.function.Supplier;

/** The tabs between runs: Back, Runs, Aether, Codex, History. Runs stays below the others, so they replace each other. */
final class RogueHomeTabs {
    static final int AETHER = 2;
    static final int CODEX = 3;
    static final int HISTORY = 4;

    private RogueHomeTabs() {
    }

    /** Adds the tabs to a screen other than Runs, with this one selected. */
    static List<RogueButton> add(RogueScreen screen, int selected) {
        Localizer localizer = Localizer.getInstance();
        RogueButton.State open = RogueStart.tabsUnlocked(RogueMetaProgress.getInstance())
            ? RogueButton.State.NORMAL : RogueButton.State.DISABLED;
        return List.of(
            screen.add(new RogueButton(localizer.getMessage("lblBack"), null, RogueButton.State.NORMAL, Forge::back)),
            screen.add(new RogueButton(localizer.getMessage("lblRogueRuns"), null, RogueButton.State.NORMAL, Forge::back)),
            tab(screen, localizer.getMessage("lblRogueAether"), selected == AETHER, open, RogueAetherScreen::new),
            tab(screen, localizer.getMessage("lblRogueCodex"), selected == CODEX, open, RogueCodexScreen::new),
            tab(screen, localizer.getMessage("lblRogueHistory"), selected == HISTORY, open, RogueHistoryScreen::new));
    }

    private static RogueButton tab(RogueScreen screen, String text, boolean selected, RogueButton.State open, Supplier<FScreen> next) {
        return screen.add(new RogueButton(text, null, selected ? RogueButton.State.SELECTED : open,
            selected ? null : () -> Forge.openScreen(next.get(), true)));
    }
}
