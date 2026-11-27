package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.deck.FDeckViewer;
import forge.gamemodes.rogue.RogueDeck;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.effect.DescensionLevel;
import forge.gamemodes.rogue.flow.RogueStart;
import forge.gui.FThreads;
import forge.item.PaperCard;
import forge.util.Localizer;
import java.util.List;

/** The mode's home: the run in progress, if any, and a new run, with the tabs between runs. */
public class RogueRunsScreen extends RogueScreen {
    /** The cards fanned for a new run: the side ones are this much of the middle one's size. */
    private static final float SIDE_CARD = 0.81f;

    private RogueRun run;
    private float bodyHeight;
    private List<RogueDeck> roster = List.of();
    private final RogueButton continueRun;
    private final RogueButton viewDeck;
    private final RogueButton startNewRun;
    private final RogueButton back;
    private final RogueButton runs;
    private final RogueButton aether;
    private final RogueButton codex;
    private final RogueButton history;
    public RogueRunsScreen() {
        super(null);
        Localizer localizer = Localizer.getInstance();
        continueRun = add(new RogueButton(localizer.getMessage("lblRogueContinueRun"), null, RogueButton.State.GOLD, () -> RogueController.get().openMap()));
        viewDeck = add(new RogueButton(localizer.getMessage("lblViewDeck"), null, RogueButton.State.NORMAL,
            () -> FDeckViewer.show(run.getCurrentDeck())));
        startNewRun = add(new RogueButton(localizer.getMessage("lblRogueStartNewRun"), null, RogueButton.State.NORMAL,
            () -> Forge.openScreen(new RogueNewRunScreen())));
        back = add(new RogueButton(localizer.getMessage("lblBack"), null, RogueButton.State.NORMAL, Forge::back));
        runs = add(new RogueButton(localizer.getMessage("lblRogueRuns"), null, RogueButton.State.SELECTED, null));
        aether = add(new RogueButton(localizer.getMessage("lblRogueAether"), null, RogueButton.State.NORMAL,
            () -> Forge.openScreen(new RogueAetherScreen())));
        codex = add(new RogueButton(localizer.getMessage("lblRogueCodex"), null, RogueButton.State.NORMAL,
            () -> Forge.openScreen(new RogueCodexScreen())));
        history = add(new RogueButton(localizer.getMessage("lblRogueHistory"), null, RogueButton.State.NORMAL,
            () -> Forge.openScreen(new RogueHistoryScreen())));
    }

    @Override
    public void onActivate() {
        RogueController controller = RogueController.get();
        run = controller.flow().run();
        if (!controller.busy()) {
            controller.queue(() -> {
                RogueRun loaded = controller.flow().load().orElse(null);
                FThreads.invokeInEdtLater(() -> {
                    run = loaded;
                    continueRun.setVisible(run != null);
                    viewDeck.setVisible(run != null);
                    revalidate();
                });
            });
            controller.betweenRuns();
        }
        roster = RogueStart.roster(false);
        RogueButton.State tabs = RogueStart.tabsUnlocked(RogueMetaProgress.getInstance())
            ? RogueButton.State.NORMAL : RogueButton.State.DISABLED;
        aether.setState(tabs);
        codex.setState(tabs);
        history.setState(tabs);
        continueRun.setVisible(run != null);
        viewDeck.setVisible(run != null);
        revalidate();
    }

    @Override
    protected float backgroundDim() {
        return 0.05f;
    }

    @Override
    protected int tab() {
        return 1;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(List.of(back, runs, aether, codex, history));
    }

    private float currentHeight() {
        float u = RogueLook.unit();
        if (run == null) {
            return 0;
        }
        if (Forge.isLandscapeMode()) {
            return getHeight() - 41 * u;
        }
        // Upright on a short screen, such as a tablet, it gives up height to leave a strip for Start New Run, but never
        // less than its card and carry cards (or None) need above its buttons
        float content = (run.getCarryCards().isEmpty() ? 200 : 250) * u;
        return Math.max(content + 43 * u, Math.min(292 * u, bodyHeight - 53 * u));
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        bodyHeight = h;
        float ch = currentHeight();
        if (Forge.isLandscapeMode()) {
            float cw = run == null ? 0 : 363 * u;
            float by = run == null ? h - 43 * u : 3 * u + ch - 46 * u;
            float left = cardX() + cardWidth(true) + 10 * u;
            buttons(left, by, 363 * u - 17 * u - left);
            startNewRun.setBounds(cw + 14 * u, by, w - cw - 31 * u, 32 * u);
        } else {
            buttons(17 * u, 3 * u + ch - 46 * u, w - 34 * u);
            startNewRun.setBounds(14 * u, h - 46 * u, w - 28 * u, 32 * u);
        }
    }

    /** Continue Run and View Deck share the width three to two; Continue Run stands a little taller. */
    private void buttons(float x, float y, float w) {
        float u = RogueLook.unit();
        float cw = (w - 4 * u) * 0.6f;
        continueRun.setBounds(x, y - 2 * u, cw, 36 * u);
        viewDeck.setBounds(x + cw + 4 * u, y, w - cw - 4 * u, 32 * u);
    }

    private static float cardX() {
        return 17 * RogueLook.unit();
    }

    private static float cardWidth(boolean wide) {
        return (wide ? 118 : 104) * RogueLook.unit();
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        float w = getWidth();
        boolean wide = Forge.isLandscapeMode();
        if (run != null) {
            drawCurrentRun(g, wide ? 360 * u : w - 6 * u, currentHeight(), wide);
        }
        float nx = run == null || !wide ? 3 * u : 366 * u;
        float ny = run == null || wide ? 3 * u : 3 * u + currentHeight() + 4 * u;
        float nw = w - nx - 3 * u;
        float nh = startNewRun.getBottom() + 7 * u - ny;
        RogueLook.paper(g, nx, ny, nw, nh);
        Localizer localizer = Localizer.getInstance();
        float room = startNewRun.getTop() - 10 * u - ny - 50 * u;
        // The fan spreads to about 2.3 middle cards wide, so a narrow panel caps its size too
        float mw = roster.size() < 3 ? 0 : Math.min(Math.min(146 * u, room) / RogueLook.CARD_RATIO, (nw - 24 * u) / 2.3f);
        float mh = mw * RogueLook.CARD_RATIO;
        if (room <= 0) {
            return;
        }
        // The heading and the fan stay together, centred in the room above Start New Run
        float hy = ny + (room - mh) / 2;
        RogueLook.text(g, localizer.getMessage("lblRogueNewRun"), nx, hy + 12 * u, 1.3f, RogueLook.DEEP, nw, Align.center);
        RogueLook.text(g, localizer.getMessage("lblRoguePickYourRogueCommander"), nx, hy + 30 * u, 0.85f, RogueLook.SOFT, nw, Align.center);
        if (mw > 0) {
            float top = hy + 50 * u;
            float sw = mw * SIDE_CARD;
            float cx = nx + nw / 2;
            float sideTop = top + mh * 0.14f;
            fanned(g, commander(roster.get(2)), cx - mw * 0.66f, sideTop, sw, 12);
            fanned(g, commander(roster.get(1)), cx + mw * 0.66f, sideTop, sw, -12);
            RogueLook.card(g, commander(roster.get(0)), cx - mw / 2, top, mw, 1);
        }
    }

    /** A card of the fan, centred on cx and turned by degrees about its centre. */
    private static void fanned(Graphics g, PaperCard card, float cx, float y, float w, float degrees) {
        float h = w * RogueLook.CARD_RATIO;
        g.startRotateTransform(cx, y + h / 2, degrees);
        RogueLook.card(g, card, cx - w / 2, y, w, 1);
        g.endTransform();
    }

    private static PaperCard commander(RogueDeck deck) {
        return deck.getStartDeck().getCommanders().get(0);
    }

    private void drawCurrentRun(Graphics g, float w, float h, boolean wide) {
        float u = RogueLook.unit();
        Localizer localizer = Localizer.getInstance();
        float y = 3 * u;
        RogueLook.gilded(g, 3 * u, y, w, h);
        float cw = cardWidth(wide);
        RogueLook.card(g, commander(run.getSelectedRogueDeck()), cardX(), y + 15 * u, cw, 1);
        float tx = cardX() + cw + 10 * u;
        float tw = w - cw - 38 * u;
        RogueLook.text(g, localizer.getMessage("lblRogueCurrentRun"), tx, y + 13 * u, 1, RogueLook.DEEP);
        float ty = y + 27 * u;
        ty += RogueLook.wrap(g, run.getCurrentCommanderName(), tx, ty, tw, 1.5f, RogueLook.INK, Align.left) + 3 * u;
        int level = run.getDescensionLevel();
        DescensionLevel descension = DescensionLevel.forLevel(level);
        String descensionText = level == 0 ? localizer.getMessage("lblOff")
            : descension == null ? localizer.getMessage("lblRogueLevel", level) : localizer.getMessage("lblRogueNamedLevel", level, descension.name);
        ty += RogueLook.wrap(g, localizer.getMessage("lblRogueDescensionLevel", descensionText), tx, ty, tw, 1, RogueLook.SOFT, Align.left) + 4 * u;
        RogueLook.line(g, tx, ty, tx + tw, ty, 1.2f, RogueLook.RULE);
        ty += 7 * u;
        String[] values = {run.getCurrentLife() + "/" + run.getMaxLife(), String.valueOf(run.getCurrentGold()),
            String.valueOf(run.getCurrentDeck().getMain().countAll()), String.valueOf(run.getRemovalCredits())};
        String[] labels = {localizer.getMessage("lblLife"), localizer.getMessage("lblRogueGold"), localizer.getMessage("lblCards"),
            localizer.getMessage("lblCredits")};
        for (int i = 0; i < 4; i++) {
            float gx = tx + (i % 2) * tw / 2;
            float gy = ty + (i / 2) * 26 * u;
            float s = 13 * u;
            switch (i) {
                case 0 -> RogueLook.item(g, "Life", gx, gy, s);
                case 1 -> RogueLook.skin(g, FSkinImage.QUEST_COIN, gx, gy, s);
                case 2 -> RogueLook.item(g, "Deck", gx, gy, s);
                default -> RogueLook.skin(g, FSkinImage.CARD_IMAGE, gx, gy, s);
            }
            RogueLook.text(g, values[i], gx + 17 * u, gy, 1.1f, RogueLook.INK);
            RogueLook.text(g, labels[i], gx + 17 * u, gy + 12 * u, 0.8f, RogueLook.SOFT);
        }
        float carryX = wide ? tx : cardX();
        float carryY = wide ? ty + 56 * u : y + 170 * u;
        float carryW = (wide ? 30 : 44) * u;
        if (!wide) {
            RogueLook.text(g, localizer.getMessage("lblRogueCarryCards"), carryX, carryY, 1, RogueLook.DEEP);
            carryY += 15 * u;
            if (run.getCarryCards().isEmpty()) {
                RogueLook.text(g, localizer.getMessage("lblNone"), carryX, carryY, 0.9f, RogueLook.SOFT);
            }
        }
        for (int i = 0; i < run.getCarryCards().size(); i++) {
            RogueLook.card(g, run.getCarryCards().get(i).toPaperCard(), carryX + i * (carryW + 6 * u), carryY, carryW, 1);
        }
    }
}
