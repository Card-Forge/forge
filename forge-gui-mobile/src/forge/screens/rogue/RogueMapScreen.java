package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinImage;
import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.RoguePlanebound;
import forge.gamemodes.rogue.RoguePlaneboundType;
import forge.gamemodes.rogue.RogueRun;
import forge.gamemodes.rogue.RogueTutorialTriggers;
import forge.gamemodes.rogue.flow.RogueFlow;
import forge.gamemodes.rogue.path.NodePlanebound;
import forge.gamemodes.rogue.path.RoguePathNode;
import forge.gui.FThreads;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.settings.SettingsScreen;
import forge.item.PaperCard;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The run's map. Upright: the chosen node large on top, the map, Enter and Reroll, then the run bar and the tabs.
 * Sideways: the run and the chosen node in a left column, the map, and one row of buttons.
 */
public class RogueMapScreen extends RogueScreen {
    /** Sideways, the width of the column beside the map, in units. */
    private static final float SIDE = 190;

    private final RogueMapView map = add(new RogueMapView(this::choose, false));
    private final RogueButton whole;
    private final RogueButton plane;
    private final RogueButton enter;
    private final RogueButton reroll;
    private final RogueButton mapTab;
    private final RogueButton traits;
    private final RogueButton deck;
    private final RogueButton codex;
    private final String folded;
    private final FDisplayObject foldBar;
    private final RogueButton show;

    /** Opened from the Runs screen, which Back returns to. */
    public RogueMapScreen() {
        this(null);
    }

    /**
     * folded is the title of the question this map was opened from, or null. With a question open, the map can
     * only be looked at: Enter is disabled and Show returns to the question.
     */
    public RogueMapScreen(String folded) {
        super(() -> RogueController.get().flow().run());
        this.folded = folded;
        map.onOpen(this::openPlaneAt);
        Localizer localizer = Localizer.getInstance();
        whole = add(new RogueButton("", "Magnifier", RogueButton.State.NORMAL, () -> Forge.openScreen(new RogueWholeMapScreen())));
        plane = add(new RogueButton(localizer.getMessage("lblPlane"), null, RogueButton.State.NORMAL, this::openPlane));
        enter = add(new RogueButton("", null, RogueButton.State.MAIN, () -> RogueController.get().submit(flow()::enter)));
        reroll = add(new RogueButton("", "Refresh", RogueButton.State.NORMAL, this::reroll));
        mapTab = add(new RogueButton(localizer.getMessage("lblRogueMap"), null, RogueButton.State.SELECTED, null));
        traits = add(new RogueButton(localizer.getMessage("lblRogueTraits"), null, RogueButton.State.NORMAL,
            () -> Forge.openScreen(new RogueTraitsScreen())));
        deck = add(new RogueButton(localizer.getMessage("lblDeck"), null, RogueButton.State.NORMAL, RogueDeckEditor::openForRun));
        codex = add(new RogueButton(localizer.getMessage("lblRogueCodex"), null, RogueButton.State.NORMAL,
            () -> Forge.openScreen(new RogueCodexScreen())));
        enter.weight(180 / 58f);
        reroll.weight(50 / 58f);
        foldBar = folded == null ? null : add(new FDisplayObject() {
            @Override
            public void draw(Graphics g) {
                float u = RogueLook.unit();
                float w = getWidth();
                float h = getHeight();
                g.fillRect(RogueLook.INK, -2 * u, -2 * u, w + 4 * u, h + 4 * u);
                g.fillRect(RogueLook.GOLD, -0.8f * u, -0.8f * u, w + 1.6f * u, h + 1.6f * u);
                g.fillRect(RogueLook.PAPER, 0, 0, w, h);
                RogueLook.fit(g, folded, 10 * u, 10 * u, 1.1f, RogueLook.INK, w - 90 * u, Align.left);
            }
        });
        show = folded == null ? null : add(new RogueButton(localizer.getMessage("lblShow"), null, RogueButton.State.MAIN, Forge::back));
    }

    private boolean introPending;

    /** The first look at a new run's map starts at its far end; its tutorials wait until the scroll has ended. */
    public void playIntro() {
        introPending = true;
        map.playIntro(() -> {
            introPending = false;
            RogueRun run = run();
            if (run != null) {
                RogueController.get().showTutorials(RogueTutorialTriggers.onMap(run));
            }
        });
    }

    private static RogueFlow flow() {
        return RogueController.get().flow();
    }

    private static RogueRun run() {
        return flow().run();
    }

    @Override
    public void onActivate() {
        map.resumeIntro();
        RogueRun run = run();
        if (run != null && !RogueController.get().busy() && folded == null) {
            RogueController.get().queue(() -> run.getRunTimer().start());
            if (!introPending) {
                RogueController.get().showTutorials(RogueTutorialTriggers.onMap(run));
            }
        }
        refresh();
    }

    /** Reads the run again; call on the GUI thread while the flow is idle or asking. */
    public void refresh() {
        RogueRun run = run();
        if (run == null || run.getCurrentNode() == null) {
            return;
        }
        Set<Integer> open = new HashSet<>(run.getPath().getVisibleNodesInCurrentRow(run.getCurrentNode().getRowIndex(), flow().pathContext()));
        map.setRun(run, open);
        RogueFlow.Reroll r = flow().reroll();
        boolean isPlane = run.getCurrentNode() instanceof NodePlanebound;
        enter.setText(flow().enterText());
        reroll.setText(String.valueOf(r.left()));
        reroll.setVisible(isPlane && r.left() > 0);
        reroll.setState(r.enabled() && folded == null ? RogueButton.State.NORMAL : RogueButton.State.DISABLED);
        enter.setState(folded == null ? RogueButton.State.MAIN : RogueButton.State.DISABLED);
        plane.setVisible(isPlane);
        revalidate();
    }

    private void choose(int index) {
        if (folded != null) {
            return;
        }
        RogueController.get().submit(() -> {
            if (flow().select(index)) {
                FThreads.invokeInEdtLater(this::refresh);
            }
        });
    }

    /** After the choice that came with it, shows the plane's sheet. */
    private void openPlaneAt(int index) {
        if (folded != null) {
            return;
        }
        RogueController.get().queue(() -> {
            flow().select(index);
            FThreads.invokeInEdtLater(() -> {
                refresh();
                openPlane();
            });
        });
    }

    private void reroll() {
        RogueController.get().submit(() -> {
            flow().rerollPlane();
            FThreads.invokeInEdtLater(this::refresh);
        });
    }

    private void openPlane() {
        if (run().getCurrentNode() instanceof NodePlanebound node) {
            new RogueSheet(new RoguePlaneSheet(run(), node, flow().reroll(), () -> {
                FOverlay.getTopOverlay().hide();
                reroll();
            }, () -> {
                FOverlay.getTopOverlay().hide();
                RogueController.get().submit(flow()::enter);
            }), 400).show();
        }
    }


    private void abandon() {
        Localizer localizer = Localizer.getInstance();
        RogueDialog.show(localizer.getMessage("lblRogueAbandonRun"), localizer.getMessage("lblRogueAbandonRunConfirm"),
            List.of(localizer.getMessage("lblCancel"), localizer.getMessage("lblRogueAbandonRun")), choice -> {
                if (choice == 1) {
                    RogueController.get().submit(flow()::abandon);
                }
            });
    }

    @Override
    protected List<RogueRunMenu.Item> menuItems() {
        Localizer localizer = Localizer.getInstance();
        if (folded != null) {
            return RogueSceneScreen.questionMenu();
        }
        List<RogueRunMenu.Item> items = new ArrayList<>();
        items.add(new RogueRunMenu.Item(localizer.getMessage("btnSaveQuit"), Forge::back));
        items.add(new RogueRunMenu.Item(localizer.getMessage("lblRogueAbandonRun"), this::abandon));
        items.add(new RogueRunMenu.Item(localizer.getMessage("lblSettings"), () -> SettingsScreen.show(false)));
        if (FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED)) {
            items.add(new RogueRunMenu.Item(localizer.getMessage("lblRogueDevNextNode"), () -> RogueController.get().submit(() -> {
                flow().devNextNode();
                FThreads.invokeInEdtLater(this::refresh);
            })));
            items.add(new RogueRunMenu.Item(localizer.getMessage("lblRogueDevWinRun"), () -> RogueController.get().submit(flow()::devWinRun)));
        }
        return items;
    }

    /** Back, like Save and Quit, saves the run; the Runs screen shows next. */
    @Override
    public void onClose(Consumer<Boolean> canCloseCallback) {
        if (folded == null) {
            RogueController.get().queue(() -> {
                if (run() != null && !flow().save()) {
                    Localizer localizer = Localizer.getInstance();
                    MobileRogueDecisions.dialog(localizer.getMessage("lblRogueCommander"), localizer.getMessage("lblSavefailed"),
                        List.of(localizer.getMessage("lblOK")));
                }
            });
        }
        if (canCloseCallback != null) {
            canCloseCallback.accept(true);
        }
    }

    @Override
    protected int tab() {
        return folded == null ? 0 : -1;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(List.of(mapTab, traits, deck, codex));
    }

    @Override
    protected List<RogueButton> wideRow() {
        List<RogueButton> row = new ArrayList<>(List.of(mapTab, traits, deck, codex));
        if (reroll.isVisible()) {
            row.add(reroll);
        }
        row.add(enter);
        enter.weight(reroll.isVisible() ? 180 / 58f : 288 / 58f);
        return row;
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        if (foldBar != null) {
            float bw = 200 * u;
            float bx = Forge.isLandscapeMode() ? SIDE * u + 4 * u + (w - SIDE * u - 7 * u - bw) / 2 : (w - bw) / 2;
            float by = Forge.isLandscapeMode() ? h - 40 * u : h - 72 * u;
            foldBar.setBounds(bx, by, bw, 34 * u);
            show.setBounds(bx + bw - 76 * u, by + 3 * u, 72 * u, 28 * u);
        }
        if (Forge.isLandscapeMode()) {
            float side = SIDE * u + 4 * u;
            map.setBounds(side + 6 * u, 9 * u, w - side - 12 * u, h - 14 * u);
            whole.setBounds(w - 34 * u, 12 * u, 24 * u, 24 * u);
            plane.setBounds(0, 0, 0, 0);
            return;
        }
        float top = 108 * u;
        float enterY = h - 30 * u;
        boolean isPlane = plane.isVisible();
        enter.setBounds(3 * u, enterY, isPlane && reroll.isVisible() ? 190 * u : w - 6 * u, 30 * u);
        reroll.setBounds(195 * u, enterY, w - 198 * u, 30 * u);
        map.setBounds(8 * u, top + 7 * u, w - 16 * u, enterY - 2 * u - top - 14 * u);
        whole.setBounds(w - 34 * u, top + 11 * u, 24 * u, 24 * u);
        plane.setBounds(w - 107 * u, top - 38 * u, 94 * u, 26 * u);
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        RogueRun run = run();
        if (run == null || run.getCurrentNode() == null) {
            return;
        }
        float u = RogueLook.unit();
        if (Forge.isLandscapeMode()) {
            drawSideColumn(g, run, SIDE * u);
            RogueLook.paper(g, SIDE * u + 4 * u, 2 * u, getWidth() - SIDE * u - 7 * u, map.getBottom() + 5 * u - 2 * u);
        } else {
            drawEnemy(g, run, 3 * u, 3 * u, getWidth() - 6 * u, 103 * u);
            RogueLook.paper(g, 3 * u, 108 * u, getWidth() - 6 * u, map.getBottom() + 7 * u - 108 * u);
        }
    }

    private void drawSideColumn(Graphics g, RogueRun run, float side) {
        float u = RogueLook.unit();
        RogueLook.paper(g, 2 * u, 2 * u, side, 60 * u);
        RogueLook.avatar(g, run.getSelectedRogueDeck().getAvatarIndex(), 11 * u, 11 * u, 26 * u);
        RogueLook.fit(g, run.getCurrentCommanderName(), 43 * u, 8 * u, 1.1f, RogueLook.INK, side - 52 * u, Align.left);
        RogueLook.item(g, "Life", 43 * u, 25 * u, 13 * u);
        RogueLook.text(g, run.getCurrentLife() + "/" + run.getMaxLife(), 58 * u, 25 * u, 1, RogueLook.INK);
        RogueLook.skin(g, FSkinImage.QUEST_COIN, 11 * u, 42 * u, 13 * u);
        RogueLook.text(g, String.valueOf(run.getCurrentGold()), 26 * u, 42 * u, 1, RogueLook.INK);
        RogueLook.skin(g, FSkinImage.QUEST_GOLD, 50 * u, 42 * u, 13 * u);
        RogueLook.text(g, String.valueOf(RogueMetaProgress.getInstance().getTotalEchoes()), 65 * u, 42 * u, 1, RogueLook.INK);
        RogueLook.skin(g, FSkinImage.CARD_IMAGE, 98 * u, 42 * u, 13 * u);
        RogueLook.text(g, String.valueOf(run.getRemovalCredits()), 113 * u, 42 * u, 1, RogueLook.INK);
        float h = getHeight() - 38 * u;
        RogueLook.paper(g, 2 * u, 64 * u, side, h - 62 * u);
        RoguePathNode node = run.getCurrentNode();
        if (!(node instanceof NodePlanebound n)) {
            String[] text = sideText(node);
            stamp(g, node, 2 * u + side / 2, 112 * u, 32 * u);
            RogueLook.text(g, text[0], 2 * u, 152 * u, 1.3f, RogueLook.INK, side, Align.center);
            RogueLook.wrap(g, text[1], 14 * u, 172 * u, side - 24 * u, 1, RogueLook.SOFT, Align.center);
            return;
        }
        boolean down = run.getPath().isFaceDown(run, n);
        RoguePlanebound b = n.getRoguePlanebound();
        PaperCard planeCard = CodexHelper.getPlaneCard(b);
        g.fillRect(RogueLook.INK, 10 * u, 72 * u, side - 16 * u, 52 * u);
        if (down) {
            g.fillRect(new Color(0.45f, 0.39f, 0.31f, 1), 11 * u, 73 * u, side - 18 * u, 50 * u);
            RogueLook.text(g, "?", 11 * u, 86 * u, 2, RogueLook.PALE, side - 18 * u, Align.center);
        } else {
            RogueLook.art(g, planeCard, 11 * u, 73 * u, side - 18 * u, 50 * u, 1);
        }
        RogueLook.avatar(g, down ? 125 : b.avatarIndex(), 12 * u, 130 * u, 26 * u);
        RogueLook.fit(g, down ? "???" : b.planeboundName(), 44 * u, 129 * u, 1.05f, RogueLook.INK, side - 54 * u, Align.left);
        RogueLook.fit(g, down ? "???" : b.planeName(), 44 * u, 143 * u, 0.95f, RogueLook.SOFT, side - 54 * u, Align.left);
        marks(g, run, n, 12 * u, 164 * u);
        float bottom = getHeight() - 38 * u;
        float rewards = bottom - 18 * u;
        String rules = down || planeCard == null ? Localizer.getInstance().getMessage("lblRogueHiddenPlanes")
            : planeCard.getRules().getOracleText().replace("\\n", "\n");
        if (g.startClip(10 * u, 178 * u, side - 16 * u, rewards - 182 * u)) {
            RogueLook.wrap(g, rules, 12 * u, 180 * u, side - 20 * u, 0.8f, down ? RogueLook.SOFT : RogueLook.INK, Align.left);
            g.endClip();
        }
        RogueLook.line(g, 12 * u, rewards - 3 * u, side - 8 * u, rewards - 3 * u, 0.8f, RogueLook.SOFT);
        RogueLook.skin(g, FSkinImage.QUEST_COIN, 12 * u, rewards, 12 * u);
        RogueLook.text(g, String.valueOf(n.getGoldReward()), 27 * u, rewards, 1, RogueLook.INK);
        RogueLook.skin(g, FSkinImage.QUEST_GOLD, 48 * u, rewards, 12 * u);
        RogueLook.text(g, String.valueOf(n.getEchoReward()), 63 * u, rewards, 1, RogueLook.INK);
        RogueLook.fit(g, Localizer.getInstance().getMessage("lblRogueCardReward"), 84 * u, rewards, 1, RogueLook.INK, side - 92 * u, Align.left);
    }

    /** Sideways, the plane's panel shows its details; a tap on it opens the whole sheet, with Reroll Plane. */
    @Override
    public boolean tap(float x, float y, int count) {
        float u = RogueLook.unit();
        RogueRun run = run();
        if (Forge.isLandscapeMode() && folded == null && x < SIDE * u && y > 64 * u && y < getHeight() - 36 * u
                && run != null && run.getCurrentNode() instanceof NodePlanebound) {
            openPlane();
            return true;
        }
        return super.tap(x, y, count);
    }

    /** Upright: the planebound with the plane's art beside it, or the side node with its building. */
    private void drawEnemy(Graphics g, RogueRun run, float x, float y, float w, float h) {
        float u = RogueLook.unit();
        RogueLook.paper(g, x, y, w, h);
        RoguePathNode node = run.getCurrentNode();
        if (!(node instanceof NodePlanebound n)) {
            String[] text = sideText(node);
            stamp(g, node, x + 40 * u, y + h / 2, 28 * u);
            RogueLook.text(g, text[0], x + 78 * u, y + 14 * u, 1.5f, RogueLook.INK);
            RogueLook.wrap(g, text[1], x + 78 * u, y + 38 * u, w - 92 * u, 1, RogueLook.SOFT, Align.left);
            return;
        }
        boolean down = run.getPath().isFaceDown(run, n);
        RoguePlanebound b = n.getRoguePlanebound();
        float ax = x + w - 104 * u;
        float aw = 94 * u;
        float ay = y + 10 * u;
        float ah = h - 46 * u;
        g.fillRect(RogueLook.INK, ax - u, ay - u, aw + 2 * u, ah + 2 * u);
        if (down) {
            g.fillRect(new Color(0.45f, 0.39f, 0.31f, 1), ax, ay, aw, ah);
            RogueLook.text(g, "?", ax, ay + ah / 2 - 12 * u, 1.8f, RogueLook.PALE, aw, Align.center);
        } else {
            RogueLook.art(g, CodexHelper.getPlaneCard(b), ax, ay, aw, ah, 1);
        }
        RogueLook.avatar(g, down ? 125 : b.avatarIndex(), x + 12 * u, y + 12 * u, 50 * u);
        float tx = x + 70 * u;
        float tw = ax - tx - 6 * u;
        Localizer localizer = Localizer.getInstance();
        RogueLook.fit(g, down ? "???" : b.planeboundName(), tx, y + 9 * u, 1.25f, RogueLook.INK, tw, Align.left);
        RogueLook.fit(g, kind(n), tx, y + 27 * u, 1, RogueLook.SOFT, tw, Align.left);
        RogueLook.fit(g, down ? localizer.getMessage("lblRogueHiddenPlanes") : b.planeName(), tx, y + 42 * u, 1, RogueLook.SOFT, tw, Align.left);
        marks(g, run, n, x + 14 * u, y + h - 30 * u);
    }

    private static String kind(NodePlanebound n) {
        Localizer localizer = Localizer.getInstance();
        return switch (n.getPlaneboundType()) {
            case BOSS -> localizer.getMessage("lblRogueBossPlanebound");
            case ELITE -> localizer.getMessage("lblRogueElitePlanebound");
            case NORMAL -> localizer.getMessage("lblRoguePlanebound");
        };
    }

    /** The node's kind and desktop's tooltip text for it. */
    static String[] sideText(RoguePathNode node) {
        Localizer localizer = Localizer.getInstance();
        String text = switch (node.getType()) {
            case SANCTUM -> localizer.getMessage("lblRogueSanctumTooltip");
            case BAZAAR -> localizer.getMessage("lblRogueBazaarTooltip");
            case CHEST -> localizer.getMessage("lblRogueChestTooltip");
            case EVENT, PLANEBOUND -> localizer.getMessage("lblRogueEventTooltip");
        };
        String[] parts = text.split(": ", 2);
        return parts.length == 2 ? parts : new String[]{text, ""};
    }

    static void stamp(Graphics g, RoguePathNode node, float cx, float cy, float r) {
        g.fillCircle(RogueMapView.NODE_FACE, cx, cy, r);
        RogueLook.ring(g, cx, cy, r, 1.2f, RogueLook.INK);
        RogueMapView.building(g, node, cx, cy, r * 1.45f, 1);
    }

    /** Life, Elite or Boss, Wrathful and Cursed, each with its mark and a word or count. */
    static void marks(Graphics g, RogueRun run, NodePlanebound n, float x, float y) {
        float u = RogueLook.unit();
        Localizer localizer = Localizer.getInstance();
        RogueLook.item(g, "Life", x, y, 11 * u);
        RogueLook.text(g, String.valueOf(n.getPlaneboundLife(run.getPath().countPlaneboundRowsUpTo(n.getRowIndex()))), x + 13 * u, y - u, 1, RogueLook.INK);
        x += 34 * u;
        if (n.getPlaneboundType() == RoguePlaneboundType.ELITE) {
            RogueLook.item(g, "Star", x, y, 11 * u);
            RogueLook.text(g, localizer.getMessage("lblElite"), x + 13 * u, y - u, 1, RogueLook.INK);
            x += 40 * u;
        }
        if (n.getPlaneboundType() == RoguePlaneboundType.BOSS) {
            RogueLook.skin(g, FSkinImage.POISON, x, y, 11 * u);
            RogueLook.text(g, localizer.getMessage("lblRogueBoss"), x + 13 * u, y - u, 1, RogueLook.INK);
            x += 40 * u;
        }
        if (n.getWrathfulCount() > 0) {
            RogueLook.building(g, 304, 288, 16, 16, x, y - u, 12 * u, 12 * u, 1);
            RogueLook.text(g, String.valueOf(n.getWrathfulCount()), x + 13 * u, y - u, 1, RogueLook.INK);
            x += 24 * u;
        }
        if (n.getCursedCount() > 0) {
            RogueLook.item(g, "BlackRune", x, y - u, 12 * u);
            RogueLook.text(g, String.valueOf(n.getCursedCount()), x + 13 * u, y - u, 1, RogueLook.INK);
        }
    }
}
