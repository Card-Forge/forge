package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.gamemodes.rogue.RogueRun;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FDisplayObject;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A question or story at a node: a heading with a mark and text, and a body such as choices, with buttons, on one
 * parchment column; sideways it stands in the middle of the scene's art. Upright the main button, or the last one, runs along the bottom and the
 * others stand in rows of up to three above it.
 */
public class RogueSceneScreen extends RogueScreen {
    /** Draws the heading's mark into a 34-unit square at x, y. */
    public interface Mark {
        void draw(Graphics g, float x, float y, float size);
    }

    private final String background;
    private final String title;
    private final Mark mark;
    private final TextPane text;
    private final FDisplayObject body;
    private final List<RogueButton> buttons;
    private float bodyBottom;
    private float columnX;
    private float columnW;

    /** run is null for a screen without the run bar; text and mark may be null. */
    public RogueSceneScreen(Supplier<RogueRun> run, String background, String title, String text, Mark mark,
                            FDisplayObject body, List<RogueButton> buttons) {
        super(run);
        this.background = background;
        this.title = title;
        this.mark = mark;
        this.text = text == null || text.isEmpty() ? null : add(new TextPane(text));
        this.body = body == null ? null : add(body);
        this.buttons = buttons;
        for (RogueButton b : buttons) {
            add(b);
        }
    }

    /** While a question is open: Leave goes to Runs and answers nothing; the question shows again on return. */
    public static List<RogueRunMenu.Item> questionMenu() {
        Localizer localizer = Localizer.getInstance();
        return List.of(new RogueRunMenu.Item(localizer.getMessage("lblLeave"), RogueController::backToRuns),
            new RogueRunMenu.Item(localizer.getMessage("lblSettings"), () -> SettingsScreen.show(false)));
    }

    @Override
    protected List<RogueRunMenu.Item> menuItems() {
        return questionMenu();
    }

    @Override
    protected String background() {
        return background;
    }

    @Override
    protected float backgroundDim() {
        return 0.2f;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        List<List<RogueButton>> rows = new ArrayList<>();
        if (buttons.isEmpty()) {
            return rows;
        }
        RogueButton last = buttons.get(buttons.size() - 1);
        boolean mainAlone = buttons.size() == 1 || last.getState() == RogueButton.State.MAIN
            || last.getState() == RogueButton.State.DISABLED;
        int n = mainAlone ? buttons.size() - 1 : buttons.size();
        int from = Math.max(0, n - (n == 4 ? 2 : 3));
        while (n > 0) {
            rows.add(0, new ArrayList<>(buttons.subList(from, n)));
            n = from;
            from = Math.max(0, n - 3);
        }
        if (mainAlone) {
            rows.add(List.of(last));
        }
        return rows;
    }

    @Override
    protected List<RogueButton> wideRow() {
        return buttons;
    }

    @Override
    protected boolean continues(RogueScreen left) {
        return left instanceof RogueSceneScreen;
    }

    private float headHeight(float w) {
        float u = RogueLook.unit();
        float tx = mark == null ? 0 : 42 * u;
        float titleHeight = RogueLook.textWidth(title, 1.5f) > w - tx ? RogueLook.wrappedHeight(title, w - tx, 1.25f) : 0;
        return Math.max(40 * u, titleHeight + 6 * u);
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        columnW = Forge.isLandscapeMode() ? Math.min(w - 24 * u, 340 * u) : w - 6 * u;
        columnX = (w - columnW) / 2;
        float hw = columnW - 26 * u;
        float line = y + 10 * u + headHeight(hw);
        float top = line + 2 * u;
        if (text != null) {
            float th = Math.min(text.getPreferredHeight(hw), h * (Forge.isLandscapeMode() ? 0.35f : 0.45f));
            text.setBounds(columnX + 13 * u, line + 6 * u, hw, th);
            top = line + 6 * u + th;
        }
        float bodyTop = top + 8 * u;
        bodyBottom = h - 8 * u;
        if (body != null) {
            body.setBounds(columnX + 5 * u, bodyTop - 2 * u, columnW - 10 * u, bodyBottom - bodyTop + 2 * u);
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        float w = getWidth();
        RogueLook.paper(g, columnX, 3 * u, columnW, bodyBottom + 8 * u - 3 * u);
        drawHead(g, columnX + 13 * u, 13 * u, columnW - 26 * u);
    }

    private void drawHead(Graphics g, float x, float y, float w) {
        float u = RogueLook.unit();
        float tx = x;
        if (mark != null) {
            mark.draw(g, x, y, 34 * u);
            tx = x + 42 * u;
        }
        if (RogueLook.textWidth(title, 1.5f) > x + w - tx) {
            RogueLook.wrap(g, title, tx, y + u, x + w - tx, 1.25f, RogueLook.INK, Align.left);
        } else {
            RogueLook.text(g, title, tx, y + 6 * u, 1.5f, RogueLook.INK);
        }
        float line = y + headHeight(w);
        RogueLook.line(g, x, line, x + w, line, 0.8f, RogueLook.SOFT);
    }

    /** A portrait mark. */
    public static Mark face(int avatar) {
        return (g, x, y, size) -> RogueLook.avatar(g, avatar, x + size * 0.03f, y + size * 0.03f, size * 0.97f);
    }

    /** A building from adventure's tileset in a round stamp. */
    public static Mark building(int sx, int sy, int sourceSize) {
        return (g, x, y, size) -> {
            float r = size / 2;
            g.fillCircle(RogueMapView.NODE_FACE, x + r, y + r, r);
            RogueLook.ring(g, x + r, y + r, r, 1.2f, RogueLook.INK);
            RogueLook.building(g, sx, sy, sourceSize, sourceSize, x + size * 0.15f, y + size * 0.12f, size * 0.7f, size * 0.7f, 1);
        };
    }

    /** The scene's text, scrolling when it is longer than its share of the page. */
    private static final class TextPane extends RogueScrollPane {
        private final RogueText text;

        TextPane(String raw) {
            text = add(new RogueText(raw, null, 1, RogueLook.INK));
        }

        float getPreferredHeight(float w) {
            return text.getPreferredHeight(w);
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float h = text.getPreferredHeight(visibleWidth);
            text.setBounds(0, 0, visibleWidth, h);
            return new ScrollBounds(visibleWidth, h);
        }
    }
}
