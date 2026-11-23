package forge.screens.rogue;

import forge.Forge;
import forge.Graphics;
import forge.gamemodes.rogue.RogueRun;
import forge.screens.FScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A full-width Rogue Commander screen with its buttons at the bottom. Upright, the buttons stand in rows and a run
 * screen's run bar sits directly above them. Sideways, every button stands in one row, a run screen's menu button
 * first.
 */
public abstract class RogueScreen extends FScreen {
    private final RogueRunBar runBar;
    private final RogueButton menuButton;

    /** run is null for a screen outside a run. */
    protected RogueScreen(Supplier<RogueRun> run) {
        super((Header) null);
        if (run == null) {
            runBar = null;
            menuButton = null;
        } else {
            runBar = add(new RogueRunBar(run, this::openMenu));
            menuButton = add(new RogueButton("", "Menu", RogueButton.State.NORMAL, this::openMenu).weight(0.3f));
        }
    }

    /** Upright, top row first. Each button must already be added to the screen. */
    protected abstract List<List<RogueButton>> buttonRows();

    /** Sideways, left to right, the main action last. */
    protected List<RogueButton> wideRow() {
        List<RogueButton> row = new ArrayList<>();
        for (List<RogueButton> buttons : buttonRows()) {
            row.addAll(buttons);
        }
        return row;
    }

    protected abstract void layoutBody(float x, float y, float w, float h);

    /** The run menu's items; a screen outside a run has none. */
    protected List<RogueRunMenu.Item> menuItems() {
        return List.of();
    }

    /** An adventure background name. */
    protected String background() {
        return "title_bg";
    }

    /** How much the background is darkened, 0 to 1. */
    protected float backgroundDim() {
        return 0.1f;
    }

    protected void openMenu() {
        RogueRunMenu.show(menuItems(), runBar == null ? 0 : runBar.getTop());
    }

    @Override
    public FScreen getLandscapeBackdropScreen() {
        return null;
    }

    @Override
    protected void drawBackground(Graphics g) {
        RogueLook.background(g, background(), getWidth(), getHeight(), backgroundDim());
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float u = RogueLook.unit();
        if (Forge.isLandscapeMode()) {
            List<RogueButton> row = new ArrayList<>();
            if (menuButton != null) {
                row.add(menuButton);
                menuButton.setVisible(true);
                runBar.setVisible(false);
            }
            row.addAll(wideRow());
            float total = 0;
            for (RogueButton b : row) {
                total += b.getWeight();
            }
            float x = 2 * u;
            float y = height - 34 * u;
            float k = (width - 4 * u) / total;
            for (RogueButton b : row) {
                b.setBounds(x, y, b.getWeight() * k, 32 * u);
                x += b.getWeight() * k;
            }
            layoutBody(0, 0, width, y - 2 * u);
            return;
        }

        List<List<RogueButton>> rows = buttonRows();
        float rowHeight = 30 * u;
        float top = height - rows.size() * 31 * u - u;
        float y = top;
        for (List<RogueButton> buttons : rows) {
            float gap = 2 * u;
            float w = (width - 6 * u - gap * (buttons.size() - 1)) / buttons.size();
            float x = 3 * u;
            for (RogueButton b : buttons) {
                b.setBounds(x, y, w, rowHeight);
                x += w + gap;
            }
            y += 31 * u;
        }
        if (runBar != null) {
            menuButton.setVisible(false);
            runBar.setVisible(true);
            runBar.setBounds(3 * u, top - 46 * u, width - 6 * u, 44 * u);
            top = runBar.getTop();
        }
        layoutBody(0, 0, width, top - 2 * u);
    }
}
