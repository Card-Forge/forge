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
    private RogueRun run;
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
        continueRun = add(new RogueButton(localizer.getMessage("lblRogueContinueRun"), null, RogueButton.State.MAIN, () -> RogueController.get().openMap()));
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
    protected List<List<RogueButton>> buttonRows() {
        return List.of(List.of(back, runs, aether, codex, history));
    }

    private float currentHeight() {
        float u = RogueLook.unit();
        if (run == null) {
            return 0;
        }
        return Forge.isLandscapeMode() ? getHeight() - 41 * u : 316 * u;
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        float ch = currentHeight();
        if (Forge.isLandscapeMode()) {
            float cw = run == null ? 0 : 363 * u;
            continueRun.setBounds(138 * u, 3 * u + ch - 42 * u, 130 * u, 32 * u);
            viewDeck.setBounds(270 * u, 3 * u + ch - 42 * u, 84 * u, 32 * u);
            startNewRun.setBounds(cw + 10 * u, h - 39 * u, w - cw - 23 * u, 32 * u);
        } else {
            continueRun.setBounds(12 * u, 3 * u + ch - 42 * u, 160 * u, 32 * u);
            viewDeck.setBounds(174 * u, 3 * u + ch - 42 * u, 84 * u, 32 * u);
            startNewRun.setBounds(12 * u, h - 42 * u, w - 24 * u, 32 * u);
        }
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
        if (roster.size() >= 3) {
            float cx = nx + nw / 2;
            RogueLook.card(g, commander(roster.get(2)), cx - 113 * u, ny + 22 * u, 62 * u, 0.8f);
            RogueLook.card(g, commander(roster.get(1)), cx + 51 * u, ny + 22 * u, 62 * u, 0.8f);
            RogueLook.card(g, commander(roster.get(0)), cx - 37 * u, ny + 10 * u, 74 * u, 1);
        }
        RogueLook.text(g, Localizer.getInstance().getMessage("lblRoguePickYourRogueCommander"), nx, startNewRun.getTop() - 16 * u,
            1, RogueLook.INK, nw, Align.center);
    }

    private static PaperCard commander(RogueDeck deck) {
        return deck.getStartDeck().getCommanders().get(0);
    }

    private void drawCurrentRun(Graphics g, float w, float h, boolean wide) {
        float u = RogueLook.unit();
        Localizer localizer = Localizer.getInstance();
        float y = 3 * u;
        RogueLook.paper(g, 3 * u, y, w, h);
        float cw = (wide ? 118 : 104) * u;
        RogueLook.card(g, commander(run.getSelectedRogueDeck()), 14 * u, y + 12 * u, cw, 1);
        float tx = 14 * u + cw + 10 * u;
        float tw = w - cw - 34 * u;
        RogueLook.text(g, localizer.getMessage("lblRogueCurrentRun"), tx, y + 10 * u, 1, RogueLook.SOFT);
        float ty = y + 24 * u;
        ty += RogueLook.wrap(g, run.getCurrentCommanderName(), tx, ty, tw, 1.5f, RogueLook.INK, Align.left) + 3 * u;
        int level = run.getDescensionLevel();
        DescensionLevel descension = DescensionLevel.forLevel(level);
        String descensionText = level == 0 ? localizer.getMessage("lblOff")
            : descension == null ? localizer.getMessage("lblRogueLevel", level) : localizer.getMessage("lblRogueNamedLevel", level, descension.name);
        ty += RogueLook.wrap(g, localizer.getMessage("lblRogueDescensionLevel", descensionText), tx, ty, tw, 1, RogueLook.INK, Align.left) + 6 * u;
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
        float carryX = wide ? tx : 16 * u;
        float carryY = wide ? ty + 56 * u : y + 166 * u;
        float carryW = (wide ? 30 : 44) * u;
        if (!wide) {
            RogueLook.text(g, localizer.getMessage("lblRogueCarryCards"), carryX, carryY, 1, RogueLook.SOFT);
            carryY += 15 * u;
        }
        for (int i = 0; i < run.getCarryCards().size(); i++) {
            RogueLook.card(g, run.getCarryCards().get(i).toPaperCard(), carryX + i * (carryW + 6 * u), carryY, carryW, 1);
        }
    }
}
