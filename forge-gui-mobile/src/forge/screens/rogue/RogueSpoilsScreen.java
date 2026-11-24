package forge.screens.rogue;

import forge.Forge;
import forge.Graphics;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.localinstance.skin.FSkinProp;
import forge.util.Localizer;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A battle's result after its match has closed, when there was no card to pick: its spoils, and either the way on or,
 * for a won run, the choice between completing it and Endless Mode. Answers whether the player goes Endless.
 */
public class RogueSpoilsScreen extends RogueScreen {
    private final String title;
    private final String line;
    private final List<RogueSpoils.Spoil> spoils = RogueSpoils.take();
    private final Map<FSkinProp, float[]> where = new EnumMap<>(FSkinProp.class);
    private final List<RogueButton> buttons;

    public RogueSpoilsScreen(RogueRun run, String title, boolean ending, RogueQuestion<Boolean> question) {
        super(run == null ? null : () -> run);
        this.title = title;
        Localizer localizer = Localizer.getInstance();
        if (ending) {
            boolean unlocked = RogueMetaProgress.getInstance().isEndlessModeUnlocked();
            line = unlocked ? localizer.getMessage("lblRogueEndingChoice") : localizer.getMessage("lblRogueUnlockEndlessHint").replace("\n", " ");
            buttons = List.of(
                add(new RogueButton(localizer.getMessage("lblRogueGoEndless"), null,
                    unlocked ? RogueButton.State.NORMAL : RogueButton.State.DISABLED, () -> answer(question, true))),
                add(new RogueButton(localizer.getMessage("lblRogueCompleteRun"), null, RogueButton.State.GOLD, () -> answer(question, false))));
        } else {
            line = "";
            buttons = List.of(add(new RogueButton(localizer.getMessage("lblContinue"), null, RogueButton.State.MAIN,
                () -> answer(question, false))));
        }
    }

    private void answer(RogueQuestion<Boolean> question, boolean endless) {
        for (RogueButton button : buttons) {
            button.setState(RogueButton.State.DISABLED);
        }
        question.answer(endless);
    }

    @Override
    protected String background() {
        return "reward";
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(buttons);
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
    }

    /** Sideways the run bar and the buttons stand at the foot of the ledger panel, the way on last. */
    @Override
    protected boolean layoutSideways(float width, float height, RogueButton menu, RogueRunBar bar) {
        float u = RogueLook.unit();
        float panel = RogueRewardScreen.PANEL * u;
        float y = height - 46 * u;
        for (int i = buttons.size() - 1; i >= 0; i--) {
            buttons.get(i).setBounds(16 * u, y, panel - 26 * u, 30 * u);
            y -= 34 * u;
        }
        if (menu != null) {
            menu.setBounds(panel - 34 * u, 14 * u, 24 * u, 25 * u);
        }
        if (bar != null) {
            bar.setCompact(true);
            bar.setVisible(true);
            bar.setBounds(14 * u, y + 34 * u - 50 * u, panel - 22 * u, 40 * u);
        }
        return true;
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        if (Forge.isLandscapeMode()) {
            RogueSpoils.sidePanel(g, 3 * u, 3 * u, RogueRewardScreen.PANEL * u, getHeight() - 6 * u, title, line, spoils, where);
        } else {
            RogueSpoils.uprightHeader(g, getWidth(), title, line, spoils, where);
        }
    }

    @Override
    protected float[] coinSource(int count) {
        return where.get(RogueSpoils.iconFor(count));
    }
}
