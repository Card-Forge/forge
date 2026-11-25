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
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;
import forge.util.Localizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The run's map. Upright: the map with Enter and Reroll under it, then the run bar and the tabs; a tap on a node opens
 * its details.
 * Sideways: the run and the chosen node in a left column, the map with Enter and Reroll under it, and one row of buttons.
 */
public class RogueMapScreen extends RogueScreen {
    /** Sideways, the width of the column beside the map, in units. */
    private static final float SIDE = 190;
    /** Sideways, the height of the commander's panel under the plane's. */
    private static final float SIDE_BAR = 44;
    /** Upright, the height of the box with the chosen node and Enter. */

    private final RogueMapView map = add(new RogueMapView(this::choose));
    private final Details details = add(new Details(this::shownNode));
    /** Sideways, a node off the current row that the side panel shows instead of the chosen one; null for none. */
    private RoguePathNode viewed;
    private final RogueButton whole;
    private final RogueButton enter;
    private final RogueButton reroll;
    private final RogueButton mapTab;
    private final RogueButton traits;
    private final RogueButton deck;
    private final String folded;
    private final FDisplayObject foldBar;
    private final RogueButton show;
    private RogueFlow.Reroll rerollState;

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
        map.onInspect(this::inspect);
        Localizer localizer = Localizer.getInstance();
        whole = add(new RogueButton("", "Magnifier", RogueButton.State.NORMAL, () -> map.setWhole(!map.isWhole())));
        enter = add(new RogueButton("", null, RogueButton.State.MAIN, () -> RogueController.get().submit(flow()::enter)));
        reroll = add(new RogueButton("", "Refresh", RogueButton.State.NORMAL, this::reroll));
        mapTab = add(new RogueButton(localizer.getMessage("lblRogueMap"), null, RogueButton.State.SELECTED, null));
        traits = add(new RogueButton(localizer.getMessage("lblRogueEffects"), null, RogueButton.State.NORMAL,
            () -> Forge.openScreen(new RogueTraitsScreen())));
        deck = add(new RogueButton(localizer.getMessage("lblDeck"), null, RogueButton.State.NORMAL, RogueDeckEditor::openForRun));
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
        viewed = null;
        map.setInspected(-1);
        RogueRun run = run();
        if (run == null || run.getCurrentNode() == null) {
            return;
        }
        Set<Integer> open = new HashSet<>(run.getPath().getVisibleNodesInCurrentRow(run.getCurrentNode().getRowIndex(), flow().pathContext()));
        map.setRun(run, open);
        RogueFlow.Reroll r = flow().reroll();
        rerollState = r;
        boolean isPlane = run.getCurrentNode() instanceof NodePlanebound;
        enter.setText(flow().enterText());
        reroll.setText(String.valueOf(r.left()));
        reroll.setVisible(isPlane && r.left() > 0);
        updateActions();
        details.setScrollTop(0);
        details.revalidate();
        revalidate();
    }

    private void choose(int index) {
        if (folded != null) {
            return;
        }
        viewed = null;
        map.setInspected(-1);
        updateActions();
        details.revalidate();
        RogueController.get().submit(() -> {
            boolean changed = flow().select(index);
            FThreads.invokeInEdtLater(() -> {
                if (changed) {
                    refresh();
                }
                if (!Forge.isLandscapeMode()) {
                    openChosen();
                }
            });
        });
    }

    /** Upright, the chosen node's details open in a sheet with Enter, and Reroll for a plane. */
    private void openChosen() {
        showNodeSheet(run().getCurrentNode(), true);
    }

    /** A node's details in a sheet sized to them, with Enter and Reroll when it is the chosen node. */
    private void showNodeSheet(RoguePathNode node, boolean chosen) {
        float u = RogueLook.unit();
        float content = new Details(() -> node).paint(null, Forge.getScreenWidth() - 26 * u);
        // The sheet's padding, the button row and the gap above it
        new RogueSheet(new NodeSheet(node, chosen), content / u + 60).show();
    }

    /** Enter and Reroll act on the chosen node, so they wait while another node is shown. */
    private void updateActions() {
        boolean free = folded == null && viewed == null;
        reroll.setState(free && rerollState != null && rerollState.enabled() ? RogueButton.State.NORMAL : RogueButton.State.DISABLED);
        enter.setState(free ? RogueButton.State.MAIN : RogueButton.State.DISABLED);
    }

    private RoguePathNode shownNode() {
        RogueRun run = run();
        return viewed != null ? viewed : run == null ? null : run.getCurrentNode();
    }

    /** Shows a node off the current row, to plan ahead: in the side panel sideways, in a sheet upright. */
    private void inspect(int index) {
        RogueRun run = run();
        if (run == null) {
            return;
        }
        RoguePathNode node = run.getPath().getNode(index);
        if (Forge.isLandscapeMode()) {
            viewed = node;
            map.setInspected(index);
            updateActions();
            details.setScrollTop(0);
            details.revalidate();
        } else {
            showNodeSheet(node, false);
        }
    }

    /** Sideways, after the choice that came with it, shows the plane's sheet; upright the choice already opened it. */
    private void openPlaneAt(int index) {
        if (folded != null || !Forge.isLandscapeMode()) {
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
        if (run().getCurrentNode() instanceof NodePlanebound) {
            showNodeSheet(run().getCurrentNode(), true);
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
        return List.of(List.of(mapTab, traits, deck));
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        if (Forge.isLandscapeMode()) {
            float side = SIDE * u + 4 * u;
            float mx = side + 6 * u;
            float mw = w - side - 12 * u;
            float enterY = h - 37 * u;
            map.setBounds(mx, 9 * u, mw, enterY - 13 * u);
            whole.setBounds(w - 34 * u, 12 * u, 24 * u, 24 * u);
            enter.setBounds(mx, enterY, reroll.isVisible() ? mw - 52 * u : mw, 28 * u);
            reroll.setBounds(mx + mw - 50 * u, enterY, 50 * u, 28 * u);
            details.setVisible(true);
            details.setBounds(0, 10 * u, SIDE * u - 6 * u, h - SIDE_BAR * u - 20 * u);
        } else {
            details.setVisible(false);
            float bw = w - 18 * u;
            float enterY = h - 37 * u;
            map.setBounds(8 * u, 10 * u, w - 16 * u, enterY - 14 * u);
            enter.setBounds(9 * u, enterY, reroll.isVisible() ? bw - 52 * u : bw, 28 * u);
            reroll.setBounds(9 * u + bw - 50 * u, enterY, 50 * u, 28 * u);
            whole.setBounds(w - 34 * u, 14 * u, 24 * u, 24 * u);
        }
        if (foldBar != null) {
            float bw = 200 * u;
            float bx = Forge.isLandscapeMode() ? SIDE * u + 4 * u + (w - SIDE * u - 7 * u - bw) / 2 : (w - bw) / 2;
            float by = map.getBottom() - 36 * u;
            foldBar.setBounds(bx, by, bw, 34 * u);
            show.setBounds(bx + bw - 76 * u, by + 3 * u, 72 * u, 28 * u);
        }
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
            RogueLook.paper(g, SIDE * u + 4 * u, 2 * u, getWidth() - SIDE * u - 7 * u, enter.getBottom() + 7 * u);
        } else {
            RogueLook.paper(g, 3 * u, 3 * u, getWidth() - 6 * u, enter.getBottom() + 6 * u);
        }
    }

    /** Sideways: the plane's panel on top and the commander below it. */
    private void drawSideColumn(Graphics g, RogueRun run, float side) {
        float u = RogueLook.unit();
        float cy = getHeight() - 36 * u - SIDE_BAR * u;
        RogueRunBar.shownElsewhere(run);
        RogueLook.paper(g, 2 * u, cy, side, SIDE_BAR * u);
        RogueLook.avatar(g, run.getSelectedRogueDeck().getAvatarIndex(), 9 * u, cy + 8 * u, 28 * u);
        RogueLook.fit(g, run.getCurrentCommanderName(), 43 * u, cy + 4 * u, 1.1f, RogueLook.INK, side - 50 * u, Align.left);
        float sx = 43 * u;
        float sy = cy + 22 * u;
        RogueLook.item(g, "Life", sx, sy, 13 * u);
        sx = count(g, run.getCurrentLife() + "/" + run.getMaxLife(), sx, sy);
        RogueLook.skin(g, FSkinImage.QUEST_COIN, sx, sy, 13 * u);
        sx = count(g, String.valueOf(run.getCurrentGold()), sx, sy);
        RogueLook.skin(g, FSkinImage.QUEST_GOLD, sx, sy, 13 * u);
        sx = count(g, String.valueOf(RogueMetaProgress.getInstance().getTotalEchoes()), sx, sy);
        RogueLook.skin(g, FSkinImage.CARD_IMAGE, sx, sy, 13 * u);
        count(g, String.valueOf(run.getRemovalCredits()), sx, sy);
        RogueLook.paper(g, 2 * u, 2 * u, side, cy - 4 * u);
    }

    /** Draws a count right of its icon and returns where the next icon goes. */
    private static float count(Graphics g, String text, float x, float y) {
        float u = RogueLook.unit();
        RogueLook.text(g, text, x + 15 * u, y, 1, RogueLook.INK);
        return x + 15 * u + RogueLook.textWidth(text, 1) + 9 * u;
    }

    /** A node's details over a row of Close, then Reroll and Enter when it is the chosen node. */
    private class NodeSheet extends FContainer {
        private final Details details;
        private final RogueButton close;
        private final RogueButton sheetReroll;
        private final RogueButton sheetEnter;

        NodeSheet(RoguePathNode node, boolean chosen) {
            RogueFlow.Reroll r = flow().reroll();
            details = add(new Details(() -> node));
            close = add(new RogueButton(Localizer.getInstance().getMessage("lblClose"), null, RogueButton.State.NORMAL,
                () -> FOverlay.getTopOverlay().hide()));
            sheetReroll = add(new RogueButton(String.valueOf(r.left()), "Refresh", r.enabled() ? RogueButton.State.NORMAL : RogueButton.State.DISABLED,
                () -> {
                    FOverlay.getTopOverlay().hide();
                    reroll();
                }));
            sheetReroll.setVisible(chosen && node instanceof NodePlanebound && r.left() > 0);
            sheetEnter = add(new RogueButton(flow().enterText(), null, RogueButton.State.MAIN, () -> {
                FOverlay.getTopOverlay().hide();
                RogueController.get().submit(flow()::enter);
            }));
            sheetEnter.setVisible(chosen);
        }

        @Override
        protected void doLayout(float width, float height) {
            float u = RogueLook.unit();
            float y = height - 30 * u;
            details.setBounds(0, 0, width, y - 6 * u);
            if (!sheetEnter.isVisible()) {
                close.setBounds(0, y, width, 30 * u);
                return;
            }
            float cw = width * 0.3f;
            float rw = sheetReroll.isVisible() ? 52 * u : 0;
            close.setBounds(0, y, cw, 30 * u);
            sheetReroll.setBounds(cw + 2 * u, y, rw - 2 * u, 30 * u);
            sheetEnter.setBounds(cw + rw + 2 * u, y, width - cw - rw - 2 * u, 30 * u);
        }
    }

    /**
     * A node's details, scrolling when short: sideways the chosen or inspected node in the left column, and the node in
     * a node sheet.
     */
    private class Details extends RogueScrollPane {
        private final Supplier<RoguePathNode> shown;

        Details(Supplier<RoguePathNode> shown) {
            this.shown = shown;
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            return new ScrollBounds(visibleWidth, Math.max(visibleHeight, paint(null, visibleWidth)));
        }

        @Override
        protected void drawBackground(Graphics g) {
            paint(g, getWidth());
        }

        @Override
        protected float arrowBandInset() {
            return 6 * RogueLook.unit();
        }

        /** Draws the content when g is set, and returns its height either way. */
        private float paint(Graphics g, float w) {
            RogueRun run = run();
            RoguePathNode node = shown.get();
            if (run == null || node == null) {
                return 0;
            }
            float u = RogueLook.unit();
            float top = g == null ? 0 : -getScrollTop();
            float side = w - 2 * u;
            Localizer localizer = Localizer.getInstance();
            if (!(node instanceof NodePlanebound n)) {
                String[] text = sideText(node);
                float h = RogueLook.wrappedHeight(text[1], side - 24 * u, 1);
                if (g != null) {
                    stamp(g, node, 2 * u + side / 2, top + 40 * u, 32 * u);
                    RogueLook.text(g, text[0], 2 * u, top + 80 * u, 1.3f, RogueLook.INK, side, Align.center);
                    RogueLook.wrap(g, text[1], 14 * u, top + 100 * u, side - 24 * u, 1, RogueLook.SOFT, Align.center);
                }
                return 100 * u + h + 6 * u;
            }
            boolean down = run.getPath().isFaceDown(run, n);
            if (down) {
                String hint = localizer.getMessage("lblRogueUnknownPlaneHint");
                if (g != null) {
                    RogueLook.text(g, localizer.getMessage("lblRogueUnknownPlanebound"), 12 * u, top + 8 * u, 1.3f, RogueLook.INK);
                    marks(g, run, n, 12 * u, top + 34 * u);
                    RogueLook.wrap(g, hint, 12 * u, top + 54 * u, side - 24 * u, 0.9f, RogueLook.SOFT, Align.left);
                }
                return 54 * u + RogueLook.wrappedHeight(hint, side - 24 * u, 0.9f) + 6 * u;
            }
            RoguePlanebound b = n.getRoguePlanebound();
            PaperCard planeCard = CodexHelper.getPlaneCard(b);
            boolean shown = !down && planeCard != null;
            String rules = shown ? planeCard.getRules().getOracleText().replace("\\n", "\n") : localizer.getMessage("lblRogueHiddenPlanes");
            boolean named = shown && !planeCard.getName().equals(b.planeName());
            String name = down ? "???" : b.planeboundName();
            float nameWidth = side - 54 * u;
            // A long name takes two lines rather than losing its end; everything under it moves down
            boolean wraps = RogueLook.textWidth(name, 1.05f) > nameWidth;
            float shift = wraps ? Math.max(0, RogueLook.wrappedHeight(name, nameWidth, 0.95f) - 14 * u) : 0;
            float rulesTop = (named ? 124 * u : 108 * u) + shift;
            float y = rulesTop + RogueLook.wrappedHeight(rules, side - 24 * u, 0.8f) + 4 * u;
            float rewards = y + 5 * u;
            String reason = node == run.getCurrentNode() && rerollState != null && rerollState.left() > 0 && !rerollState.enabled()
                ? rerollState.reason() : null;
            float end = rewards + 18 * u + (reason == null ? 0 : RogueLook.wrappedHeight(reason, side - 24 * u, 0.8f)) + 4 * u;
            if (g == null) {
                return end;
            }
            g.fillRect(RogueLook.INK, 10 * u, top, side - 16 * u, 52 * u);
            if (down) {
                g.fillRect(new Color(0.45f, 0.39f, 0.31f, 1), 11 * u, top + u, side - 18 * u, 50 * u);
                RogueLook.text(g, "?", 11 * u, top + 14 * u, 2, RogueLook.PALE, side - 18 * u, Align.center);
            } else {
                RogueLook.art(g, planeCard, 11 * u, top + u, side - 18 * u, 50 * u, 1);
            }
            RogueLook.avatar(g, down ? 125 : b.avatarIndex(), 12 * u, top + 58 * u, 26 * u);
            if (wraps) {
                RogueLook.wrap(g, name, 44 * u, top + 57 * u, nameWidth, 0.95f, RogueLook.INK, Align.left);
            } else {
                RogueLook.text(g, name, 44 * u, top + 57 * u, 1.05f, RogueLook.INK);
            }
            RogueLook.fit(g, down ? "???" : b.planeName(), 44 * u, top + shift + 71 * u, 0.95f, RogueLook.SOFT, nameWidth, Align.left);
            marks(g, run, n, 12 * u, top + shift + 92 * u);
            if (named) {
                RogueLook.fit(g, planeCard.getName(), 12 * u, top + shift + 108 * u, 1, RogueLook.DEEP, side - 20 * u, Align.left);
            }
            RogueLook.wrap(g, rules, 12 * u, top + rulesTop, side - 24 * u, 0.8f, shown ? RogueLook.INK : RogueLook.SOFT, Align.left);
            RogueLook.line(g, 12 * u, top + y, side - 8 * u, top + y, 0.8f, RogueLook.SOFT);
            RogueLook.skin(g, FSkinImage.QUEST_COIN, 12 * u, top + rewards, 12 * u);
            RogueLook.text(g, String.valueOf(n.getGoldReward()), 27 * u, top + rewards, 1, RogueLook.INK);
            RogueLook.skin(g, FSkinImage.QUEST_GOLD, 48 * u, top + rewards, 12 * u);
            RogueLook.text(g, String.valueOf(n.getEchoReward()), 63 * u, top + rewards, 1, RogueLook.INK);
            RogueLook.fit(g, localizer.getMessage("lblRogueCardReward"), 84 * u, top + rewards, 1, RogueLook.INK, side - 92 * u, Align.left);
            if (reason != null) {
                RogueLook.wrap(g, reason, 12 * u, top + rewards + 18 * u, side - 24 * u, 0.8f, RogueLook.SOFT, Align.left);
            }
            return end;
        }
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
