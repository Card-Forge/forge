package forge.screens.rogue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import forge.Forge;
import forge.Graphics;
import forge.gamemodes.rogue.RogueRun;
import forge.item.PaperCard;
import forge.screens.FScreen;
import forge.toolbox.FDisplayObject;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A full-width Rogue Commander screen with its buttons at the bottom. Upright, the buttons stand in rows and a run
 * screen's run bar sits directly above them. Sideways, every button stands in one row, a run screen's menu button
 * first.
 */
public abstract class RogueScreen extends FScreen {
    private FDisplayObject hoverSource;
    private PaperCard hoverCard;
    private String hoverTitle;
    private String hoverText;
    private static final float TRANSITION_SECONDS = 0.5f;
    private static final Color TRANSITION_DIM = new Color(0, 0, 0, 0);
    private static long opened;
    private static WeakReference<RogueScreen> onTop = new WeakReference<>(null);
    private static long onTopFrame;

    private long order = ++opened;
    private RogueScreen from;
    private float transition = 1;
    private boolean nested;
    private float chromeTop;
    private final List<Flight> flights = new ArrayList<>();
    private final RogueRunBar runBar;
    private final RogueButton menuButton;

    /** run is null for a screen outside a run. */
    protected RogueScreen(Supplier<RogueRun> run) {
        super((Header) null);
        if (run == null) {
            runBar = null;
            menuButton = null;
        } else {
            runBar = add(new RogueRunBar(this, run, this::openMenu));
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

    /**
     * Sideways, a screen that places its own buttons, the run menu's among them, does so here and returns true; it then
     * lays out its body too, and may show the run bar, compact, where it likes. menu and bar are null outside a run.
     */
    protected boolean layoutSideways(float width, float height, RogueButton menu, RogueRunBar bar) {
        return false;
    }

    /** Where the coins for a run bar count fly from when it rises, as x, y, w, h; null for the middle of the page. */
    protected float[] coinSource(int count) {
        return null;
    }

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

    /** Opened again from further back, as the open question is: it comes forward like a new screen. */
    void bringForward() {
        order = ++opened;
    }

    /** Whether it carries on from that screen, as one page of a story follows another, so it shows without moving in. */
    protected boolean continues(RogueScreen left) {
        return false;
    }

    /** Its place among the tabs it shares with others, which slide sideways between each other; -1 for none. */
    protected int tab() {
        return -1;
    }

    /**
     * A screen that has just become current draws the one it replaced behind itself while it moves in: forward, it
     * rises over it; back, the old one drops away; between tabs, both pages slide sideways under buttons that stay.
     */
    @Override
    public void draw(Graphics g) {
        if (nested) {
            super.draw(g);
            return;
        }
        long frame = Gdx.graphics.getFrameId();
        if (Forge.getCurrentScreen() == this) {
            RogueScreen top = onTop.get();
            if (top != this) {
                RogueScreen left = top != null && frame - onTopFrame <= 2 ? top : null;
                onTop = new WeakReference<>(this);
                if (left != null && left.getWidth() == getWidth() && left.getHeight() == getHeight() && !continues(left)) {
                    from = left;
                    RogueMotion.play(TRANSITION_SECONDS, t -> transition = t, () -> from = null);
                }
            }
            onTopFrame = frame;
        }
        RogueScreen left = from;
        if (left == null) {
            super.draw(g);
            return;
        }
        float e = Interpolation.pow3Out.apply(transition);
        float w = getWidth();
        float h = getHeight();
        int side = Integer.signum(tab() - left.tab());
        if (tab() >= 0 && left.tab() >= 0 && side != 0) {
            if (g.startClip(0, 0, w, chromeTop)) {
                drawAt(g, left, -side * w * e, 0, 0);
                drawAt(g, this, side * w * (1 - e), 0, 0);
            }
            g.endClip();
            if (g.startClip(0, chromeTop, w, h - chromeTop)) {
                drawAt(g, this, 0, 0, 0);
            }
            g.endClip();
        } else if (left.order > order) {
            drawAt(g, this, 0, 0, 1 - e);
            drawAt(g, left, 0, h * e, 0);
        } else {
            drawAt(g, left, 0, 0, e);
            drawAt(g, this, 0, h * (1 - e), 0);
        }
    }

    /** Paints a flying thing into its current rectangle. */
    interface FlightPainter {
        void paint(Graphics g, float x, float y, float w, float h);
    }

    private static final class Flight {
        final FlightPainter painter;
        final float[] from;
        final float[] to;
        final float delay;
        final float seconds;
        final float arc;
        float time;

        Flight(FlightPainter painter, float[] from, float[] to, float delay, float seconds, float arc) {
            this.painter = painter;
            this.from = from;
            this.to = to;
            this.delay = delay;
            this.seconds = seconds;
            this.arc = arc;
        }
    }

    /**
     * Flies something across the page above everything else, from one rectangle (x, y, w, h) to another, rising by arc
     * on the way. done may be null.
     */
    void fly(FlightPainter painter, float[] from, float[] to, float delay, float seconds, float arc, Runnable done) {
        Flight flight = new Flight(painter, from, to, delay, seconds, arc);
        flights.add(flight);
        RogueMotion.play(delay + seconds, t -> flight.time = t * (delay + seconds), () -> {
            flights.remove(flight);
            if (done != null) {
                done.run();
            }
        });
    }

    /** Desktop: while the pointer rests on a card, shows it large beside it, as Adventure's reward tooltips do. */
    public static void hover(FDisplayObject source, boolean hovered, PaperCard card) {
        if (!(Forge.getCurrentScreen() instanceof RogueScreen screen)) {
            return;
        }
        if (hovered && card != null) {
            screen.hoverSource = source;
            screen.hoverCard = card;
            screen.hoverText = null;
        } else if (screen.hoverSource == source) {
            screen.hoverSource = null;
            screen.hoverCard = null;
            screen.hoverText = null;
        }
    }

    /** Desktop: while the pointer rests on something with a note, such as a defined term, shows it beside the pointer. */
    public static void hoverNote(FDisplayObject source, String title, String text) {
        if (Forge.getCurrentScreen() instanceof RogueScreen screen && text != null) {
            screen.hoverSource = source;
            screen.hoverCard = null;
            screen.hoverTitle = title;
            screen.hoverText = text;
        }
    }

    private void drawHover(Graphics g) {
        if (hoverSource == null || !hoverSource.isHovered()) {
            return;
        }
        if (hoverText != null) {
            RogueTermPopup.draw(g, hoverTitle, hoverText, Forge.mouseMovedX - screenPos.x, Forge.mouseMovedY - screenPos.y, getWidth(), getHeight());
            return;
        }
        if (hoverCard == null) {
            return;
        }
        float gap = 8 * RogueLook.unit();
        float h = Math.min(getHeight() * 0.62f, getHeight() - 2 * gap);
        float w = h / RogueLook.CARD_RATIO;
        float sx = hoverSource.screenPos.x - screenPos.x;
        float sy = hoverSource.screenPos.y - screenPos.y;
        float x = sx + hoverSource.screenPos.width + gap;
        if (x + w > getWidth()) {
            x = sx - gap - w;
        }
        if (x < 0) {
            x = (getWidth() - w) / 2;
        }
        float y = Math.max(gap, Math.min(sy + hoverSource.screenPos.height / 2 - h / 2, getHeight() - h - gap));
        RogueLook.card(g, hoverCard, x, y, w, h, 1);
    }

    @Override
    protected void drawOverlay(Graphics g) {
        super.drawOverlay(g);
        drawHover(g);
        for (int i = 0; i < flights.size(); i++) {
            Flight f = flights.get(i);
            float p = (f.time - f.delay) / f.seconds;
            if (p <= 0 || p >= 1) {
                continue;
            }
            float e = Interpolation.pow3Out.apply(p);
            float x = f.from[0] + (f.to[0] - f.from[0]) * e;
            float y = f.from[1] + (f.to[1] - f.from[1]) * e - f.arc * (float) Math.sin(Math.PI * p);
            float w = f.from[2] + (f.to[2] - f.from[2]) * e;
            float h = f.from[3] + (f.to[3] - f.from[3]) * e;
            f.painter.paint(g, x, y, w, h);
        }
    }

    private void drawAt(Graphics g, RogueScreen screen, float x, float y, float dim) {
        screen.nested = true;
        screen.setLeft(x);
        screen.setTop(y);
        try {
            g.draw(screen);
        } finally {
            screen.setLeft(0);
            screen.setTop(0);
            screen.nested = false;
        }
        if (dim > 0) {
            TRANSITION_DIM.a = 0.55f * dim;
            g.fillRect(TRANSITION_DIM, x, y, getWidth(), getHeight());
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        RogueLook.background(g, background(), getWidth(), getHeight(), backgroundDim());
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        float u = RogueLook.unit();
        if (Forge.isLandscapeMode()) {
            if (menuButton != null) {
                menuButton.setVisible(true);
                runBar.setVisible(false);
            }
            if (layoutSideways(width, height, menuButton, runBar)) {
                chromeTop = height;
                return;
            }
            List<RogueButton> row = new ArrayList<>();
            if (menuButton != null) {
                row.add(menuButton);
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
            chromeTop = y - 2 * u;
            layoutBody(0, 0, width, y - 2 * u);
            return;
        }

        List<List<RogueButton>> rows = buttonRows();
        float rowHeight = 30 * u;
        float top = height - rows.size() * 31 * u - u;
        float y = top;
        for (List<RogueButton> buttons : rows) {
            float gap = 2 * u;
            float total = 0;
            for (RogueButton b : buttons) {
                total += b.getUprightWeight();
            }
            float k = (width - 6 * u - gap * (buttons.size() - 1)) / total;
            float x = 3 * u;
            for (RogueButton b : buttons) {
                float w = b.getUprightWeight() * k;
                b.setBounds(x, y, w, rowHeight);
                x += w + gap;
            }
            y += 31 * u;
        }
        if (runBar != null) {
            menuButton.setVisible(false);
            runBar.setCompact(false);
            runBar.setVisible(true);
            runBar.setBounds(3 * u, top - 46 * u, width - 6 * u, 44 * u);
            top = runBar.getTop();
        }
        chromeTop = top - 2 * u;
        layoutBody(0, 0, width, top - 2 * u);
    }
}
