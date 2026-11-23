package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkinImage;
import forge.deck.FDeckViewer;
import forge.gamemodes.rogue.CodexHelper;
import forge.gamemodes.rogue.CodexHelper.CodexState;
import forge.gamemodes.rogue.RogueDeck;
import forge.gamemodes.rogue.RogueMetaProgress;
import forge.gamemodes.rogue.effect.CustomRunModification;
import forge.gamemodes.rogue.effect.DescensionLevel;
import forge.gamemodes.rogue.flow.RogueStart;
import forge.gui.FThreads;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;
import forge.util.Localizer;
import forge.util.MyRandom;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Choosing the commander, Descension level and custom run for a new run. */
public class RogueNewRunScreen extends RogueScreen {
    private static final Color LOCKED_DOT = new Color(0.45f, 0.42f, 0.38f, 1);

    private List<RogueDeck> roster = List.of();
    private int index;
    /** How many slots the commanders still have to move left; negative moves them right. */
    private float slide;
    private ForgeAnimation slideAnimation;
    private int descension;
    private boolean custom;
    private final Set<CustomRunModification> modifications = EnumSet.noneOf(CustomRunModification.class);

    private final RogueButton previous;
    private final RogueButton next;
    private final RogueButton descensionButton;
    private final RogueButton customRun;
    private final RogueButton startingDeck;
    private final RogueButton start;
    private final RogueButton back;
    private final RogueButton random;
    private final Details details;
    private final List<RogueButton> dev = new ArrayList<>();

    public RogueNewRunScreen() {
        super(null);
        Localizer localizer = Localizer.getInstance();
        previous = add(new RogueButton("<", null, RogueButton.State.NORMAL, () -> step(-1)));
        next = add(new RogueButton(">", null, RogueButton.State.NORMAL, () -> step(1)));
        descensionButton = add(new RogueButton("", null, RogueButton.State.NORMAL, this::openDescensionSheet));
        customRun = add(new RogueButton(localizer.getMessage("lblRogueCustomRun"), null, RogueButton.State.NORMAL,
            () -> Forge.openScreen(new RogueCustomRunScreen(this))));
        startingDeck = add(new RogueButton(localizer.getMessage("lblRogueStartingDeck"), null, RogueButton.State.NORMAL,
            () -> FDeckViewer.show(deck().getStartDeck())));
        start = add(new RogueButton(localizer.getMessage("lblRogueStartRun"), null, RogueButton.State.MAIN, this::begin));
        back = add(new RogueButton(localizer.getMessage("lblBack"), null, RogueButton.State.NORMAL, Forge::back));
        random = add(new RogueButton(localizer.getMessage("lblRandom"), null, RogueButton.State.NORMAL, this::rollRandom));
        details = add(new Details());
        start.weight(130 / 80f);
        if (FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED)) {
            dev.add(add(new RogueButton(unlockAllText(), null, RogueButton.State.NORMAL, this::devToggleUnlockAll)));
            dev.add(add(new RogueButton(localizer.getMessage("lblRogueDevNpcProgress"), null, RogueButton.State.NORMAL,
                () -> Forge.openScreen(new RogueNpcProgressScreen()))));
        }
    }

    private static String unlockAllText() {
        return Localizer.getInstance().getMessage(RogueMetaProgress.getInstance().isDevUnlockAll() ? "lblRogueDevLockCommanders" : "lblRogueDevUnlockAll");
    }

    private void devToggleUnlockAll() {
        RogueController.get().submit(() -> {
            RogueMetaProgress progress = RogueMetaProgress.getInstance();
            progress.setDevUnlockAll(!progress.isDevUnlockAll());
            FThreads.invokeInEdtLater(() -> {
                dev.get(0).setText(unlockAllText());
                reloadRoster();
                update();
            });
        });
    }

    @Override
    public void onActivate() {
        if (!RogueController.get().busy() && RogueController.get().flow().run() == null) {
            RogueController.get().betweenRuns();
        }
        reloadRoster();
        update();
    }

    void reloadRoster() {
        RogueDeck current = roster.isEmpty() ? null : deck();
        roster = RogueStart.roster(custom && modifications.contains(CustomRunModification.EXPERIMENT));
        index = Math.max(0, current == null ? 0 : roster.indexOf(current));
        if (index >= roster.size()) {
            index = 0;
        }
    }

    RogueDeck deck() {
        return roster.isEmpty() ? null : roster.get(index);
    }

    RogueStart selection() {
        return new RogueStart(deck(), custom, EnumSet.copyOf(modifications), descension);
    }

    boolean isCustom() {
        return custom;
    }

    void setCustom(boolean custom) {
        this.custom = custom;
        reloadRoster();
        clampDescension();
        update();
    }

    Set<CustomRunModification> modifications() {
        return modifications;
    }

    void setModifications(Set<CustomRunModification> on) {
        boolean experimental = modifications.contains(CustomRunModification.EXPERIMENT);
        modifications.clear();
        modifications.addAll(on);
        if (experimental != modifications.contains(CustomRunModification.EXPERIMENT)) {
            reloadRoster();
        }
        update();
    }

    int descension() {
        return descension;
    }

    void setDescension(int level) {
        descension = level;
        update();
    }

    private void clampDescension() {
        RogueDeck d = deck();
        if (descension > 0 && (d == null && !custom
            || RogueStart.descensionLockText(RogueMetaProgress.getInstance(), d, descension, custom) != null)) {
            descension = 0;
        }
    }

    private void step(int delta) {
        if (roster.isEmpty()) {
            return;
        }
        index = (index + delta + roster.size()) % roster.size();
        slide += delta;
        if (slideAnimation == null) {
            slideAnimation = new Slide();
            slideAnimation.start();
        }
        clampDescension();
        update();
    }

    /** Moves the commanders the remaining slots toward their places, faster the further they still have to go. */
    private class Slide extends ForgeAnimation {
        @Override
        protected boolean advance(float dt) {
            float step = Math.max(4f, Math.abs(slide) * 6f) * dt;
            slide = Math.abs(slide) <= step ? 0 : slide - Math.signum(slide) * step;
            return slide != 0;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            slide = 0;
            slideAnimation = null;
        }
    }

    /** Spins through at least eight commanders and stops on an unlocked one. */
    private void rollRandom() {
        List<Integer> unlocked = new ArrayList<>();
        for (int i = 0; i < roster.size(); i++) {
            if (roster.get(i).isUnlocked()) {
                unlocked.add(i);
            }
        }
        if (unlocked.isEmpty()) {
            return;
        }
        int target = unlocked.get(MyRandom.getRandom().nextInt(unlocked.size()));
        int distance = (target - index + roster.size()) % roster.size();
        while (distance < 8) {
            distance += roster.size();
        }
        int steps = distance;
        random.setState(RogueButton.State.DISABLED);
        Timer.schedule(new Timer.Task() {
            private int done;

            @Override
            public void run() {
                step(1);
                if (++done == steps) {
                    cancel();
                    random.setState(RogueButton.State.NORMAL);
                }
            }
        }, 0, 0.06f, steps - 1);
    }

    void openDescensionSheet() {
        new RogueSheet(new DescensionSheet(), 330).show();
    }

    void begin() {
        RogueStart selection = selection();
        if (selection.canBegin(RogueMetaProgress.getInstance())) {
            RogueController.get().submit(() -> RogueController.get().flow().begin(selection));
        }
    }

    private void update() {
        Localizer localizer = Localizer.getInstance();
        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        RogueDeck d = deck();
        boolean unlocked = d != null && d.isUnlocked();
        descensionButton.setText(localizer.getMessage("lblRogueDescensionLevel",
            descension == 0 ? localizer.getMessage("lblOff") : String.valueOf(descension)));
        descensionButton.setState(progress.isDescensionModeUnlocked() && (unlocked || custom)
            ? RogueButton.State.NORMAL : RogueButton.State.DISABLED);
        customRun.setState(!progress.isCustomRunsUnlocked() ? RogueButton.State.DISABLED
            : custom ? RogueButton.State.SELECTED : RogueButton.State.NORMAL);
        startingDeck.setState(unlocked ? RogueButton.State.NORMAL : RogueButton.State.DISABLED);
        start.setState(selection().canBegin(progress) ? RogueButton.State.MAIN : RogueButton.State.DISABLED);
    }

    @Override
    protected float backgroundDim() {
        return 0;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        List<List<RogueButton>> rows = new ArrayList<>();
        if (!dev.isEmpty()) {
            rows.add(dev);
        }
        rows.add(List.of(descensionButton, customRun, startingDeck));
        rows.add(List.of(start));
        rows.add(List.of(back, random));
        return rows;
    }

    @Override
    protected List<RogueButton> wideRow() {
        back.weight(52 / 80f);
        random.weight(66 / 80f);
        descensionButton.weight(100 / 80f);
        startingDeck.weight(90 / 80f);
        List<RogueButton> row = new ArrayList<>(List.of(back, random));
        row.addAll(dev);
        row.addAll(List.of(descensionButton, customRun, startingDeck, start));
        return row;
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        if (Forge.isLandscapeMode()) {
            previous.setBounds(4 * u, 203 * u, 36 * u, 28 * u);
            next.setBounds(210 * u, 203 * u, 36 * u, 28 * u);
            details.setBounds(264 * u, 12 * u, w - 278 * u, h - 22 * u);
        } else {
            previous.setBounds(5 * u, 258 * u, 40 * u, 28 * u);
            next.setBounds(w - 45 * u, 258 * u, 40 * u, 28 * u);
            details.setBounds(15 * u, 299 * u, w - 30 * u, buttonsTop() - 309 * u);
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        RogueDeck d = deck();
        if (d == null) {
            return;
        }
        float u = RogueLook.unit();
        boolean wide = Forge.isLandscapeMode();
        if (wide) {
            g.startClip(0, 0, 250 * u, getHeight() - 36 * u);
            drawCarousel(g, new float[]{54 * u, 5 * u, 142 * u}, new float[]{-50 * u, 46 * u, 100 * u}, new float[]{200 * u, 46 * u, 100 * u});
            g.endClip();
            dots(g, 125 * u, 215 * u);
            RogueLook.paper(g, 252 * u, 2 * u, getWidth() - 254 * u, getHeight() - 38 * u);
        } else {
            drawCarousel(g, new float[]{getWidth() / 2 - 85 * u, 16 * u, 170 * u}, new float[]{-84 * u, 60 * u, 110 * u},
                new float[]{getWidth() - 26 * u, 60 * u, 110 * u});
            dots(g, getWidth() / 2, 270 * u);
            RogueLook.paper(g, 3 * u, 290 * u, getWidth() - 6 * u, buttonsTop() - 292 * u);
        }
    }

    /** Upright, the top of the highest button row. */
    private float buttonsTop() {
        return buttonRows().get(0).get(0).getTop();
    }

    private static PaperCard commander(RogueDeck deck) {
        return deck.getStartDeck().getCommanders().get(0);
    }

    /** Each slot is x, y and width. The middle card is drawn last, over its neighbours. */
    private void drawCarousel(Graphics g, float[] centre, float[] left, float[] right) {
        int n = roster.size();
        int reach = n < 5 ? n / 2 : 2;
        for (int distance = reach; distance >= 0; distance--) {
            for (int k : distance == 0 ? new int[]{0} : new int[]{-distance, distance}) {
                float p = k + slide;
                float a = Math.abs(p);
                float[] side = p < 0 ? left : right;
                float x;
                float y;
                float w;
                float alpha;
                if (a <= 1) {
                    x = centre[0] + (side[0] - centre[0]) * a;
                    y = centre[1] + (side[1] - centre[1]) * a;
                    w = centre[2] + (side[2] - centre[2]) * a;
                    alpha = 1 - 0.25f * a;
                } else {
                    x = side[0] + (side[0] - centre[0]) * (a - 1);
                    y = side[1];
                    w = side[2];
                    alpha = Math.max(0, 0.75f * (2 - a));
                }
                if (alpha > 0) {
                    drawCommander(g, roster.get(((index + k) % n + n) % n), x, y, w, alpha);
                }
            }
        }
    }

    /** A locked commander shows the sleeve and a lock instead of its card. */
    private void drawCommander(Graphics g, RogueDeck d, float x, float y, float w, float alpha) {
        if (d.isUnlocked()) {
            RogueLook.card(g, commander(d), x, y, w, alpha);
            return;
        }
        float u = RogueLook.unit();
        if (alpha < 1) {
            g.setAlphaComposite(alpha);
        }
        RogueLook.back(g, d.getSleeveIndex(), x, y, w);
        float cx = x + w / 2;
        float cy = y + w * 0.7f;
        g.fillCircle(RogueLook.INK, cx, cy, 24 * u);
        g.fillCircle(RogueLook.PAPER, cx, cy, 22 * u);
        RogueLook.skin(g, FSkinImage.LOCK, cx - 13 * u, cy - 13 * u, 26 * u);
        if (alpha < 1) {
            g.resetAlphaComposite();
        }
    }

    private void dots(Graphics g, float centre, float y) {
        float u = RogueLook.unit();
        float s = centre - roster.size() * 11 * u / 2;
        for (int i = 0; i < roster.size(); i++) {
            float cx = s + (i * 11 + 5) * u;
            g.fillCircle(RogueLook.INK, cx, y, 4 * u);
            if (i == index) {
                g.fillCircle(RogueLook.GOLD, cx, y, 3 * u);
            } else {
                g.fillCircle(roster.get(i).isUnlocked() ? RogueLook.PALE : LOCKED_DOT, cx, y, 2.8f * u);
            }
        }
    }

    /** Draws the commander's details from y; returns where they end. */
    private float drawText(Graphics g, RogueDeck d, float x, float y, float w) {
        float u = RogueLook.unit();
        Localizer localizer = Localizer.getInstance();
        RogueMetaProgress progress = RogueMetaProgress.getInstance();
        if (!d.isUnlocked()) {
            RogueLook.fit(g, "???", x, y, 1.6f, RogueLook.INK, w, Align.left);
            RogueLook.text(g, localizer.getMessage("lblRogueUnlockLabel"), x, y + 24 * u, 1, RogueLook.SOFT);
            RogueLook.skin(g, FSkinImage.LOCK, x, y + 41 * u, 13 * u);
            return y + 40 * u + RogueLook.wrap(g, d.getUnlockDescription(), x + 18 * u, y + 40 * u, w - 18 * u, 1.1f, RogueLook.INK, Align.left);
        }
        RogueLook.fit(g, d.getCommanderCardName(), x, y, 1.6f, RogueLook.INK, w, Align.left);
        y += 24 * u;
        y += RogueLook.wrap(g, d.getDescription(), x, y, w, 1, RogueLook.INK, Align.left) + 6 * u;
        if (!RogueStart.tabsUnlocked(progress)) {
            RogueLook.line(g, x, y, x + w, y, 0.8f, RogueLook.SOFT);
            y += 4 * u;
            for (String lock : new String[]{localizer.getMessage("lblRogueUnlockDescensionHint"),
                    localizer.getMessage("lblRogueCustomRunWin2RunsWithAnyCommanderToUnlock"), localizer.getMessage("lblRogueUnlockAetherHint")}) {
                RogueLook.skin(g, FSkinImage.LOCK, x, y + u, 11 * u);
                y += RogueLook.wrap(g, lock, x + 15 * u, y, w - 15 * u, 1, RogueLook.INK, Align.left) + 3 * u;
            }
            return y;
        }
        y += RogueLook.wrap(g, d.getThemeDescription(), x, y, w, 1, RogueLook.TEAL, Align.left) + 8 * u;
        RogueLook.line(g, x, y, x + w, y, 0.8f, RogueLook.SOFT);
        y += 5 * u;
        String name = d.getCommanderCardName();
        List<CodexState> pool = new ArrayList<>();
        for (PaperCard card : d.getRewardPoolCards()) {
            pool.add(CodexHelper.state(progress, d, card));
        }
        String[] values = {String.valueOf(progress.getRunsStartedWithCommander(name)), String.valueOf(progress.getRunsWonWithCommander(name)),
            String.valueOf(progress.getMaxDescensionWon(name)), CodexHelper.countSeen(pool) + "/" + pool.size()};
        String[] labels = {localizer.getMessage("lblRogueRuns"), localizer.getMessage("lblRogueWon"),
            localizer.getMessage("lblRogueDescension"), localizer.getMessage("lblRogueCardsSeen")};
        for (int i = 0; i < 4; i++) {
            RogueLook.text(g, values[i], x + i * w / 4, y, 1.3f, RogueLook.INK, w / 4, Align.center);
            RogueLook.text(g, labels[i], x + i * w / 4, y + 17 * u, 0.8f, RogueLook.SOFT, w / 4, Align.center);
        }
        return y + 30 * u;
    }

    /** The commander's details, scrolling when the buttons leave too little room. */
    private final class Details extends RogueScrollPane {
        private float contentHeight;

        Details() {
            add(new FDisplayObject() {
                @Override
                public void draw(Graphics g) {
                    RogueDeck d = deck();
                    if (d == null) {
                        return;
                    }
                    float end = drawText(g, d, 0, 0, getWidth());
                    if (Math.abs(end - contentHeight) > 1) {
                        contentHeight = end;
                        Details.this.revalidate();
                    }
                }
            });
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            getChildAt(0).setBounds(0, 0, visibleWidth, Math.max(contentHeight, visibleHeight));
            return new ScrollBounds(visibleWidth, contentHeight);
        }
    }

    /** The Descension levels: open ones can be chosen; the next locked one says how to unlock it. */
    private class DescensionSheet extends FContainer {
        private final RogueButton done = add(new RogueButton(Localizer.getInstance().getMessage("lblDone"), null,
            RogueButton.State.MAIN, () -> FOverlay.getTopOverlay().hide()));

        @Override
        protected void doLayout(float width, float height) {
            float u = RogueLook.unit();
            done.setBounds(0, height - 30 * u, width, 30 * u);
        }

        @Override
        public boolean tap(float x, float y, int count) {
            float u = RogueLook.unit();
            float top = levelsTop();
            int cols = columns();
            float bw = tileWidth();
            int level = (int) ((y - top) / (31 * u)) * cols + Math.min(cols - 1, (int) (x / (bw + 4 * u)));
            if (y >= top && level <= DescensionLevel.getMaxLevel() && isOpen(level)) {
                setDescension(level);
            }
            return true;
        }

        private boolean isOpen(int level) {
            return level == 0 || RogueStart.descensionLockText(RogueMetaProgress.getInstance(), deck(), level, custom) == null;
        }

        private String commanderLock() {
            return RogueStart.descensionLockText(RogueMetaProgress.getInstance(), deck(), 0, custom);
        }

        /** Sideways the sheet is short, so the levels take two rows instead of four. */
        private int columns() {
            return Forge.isLandscapeMode() ? 4 : 2;
        }

        private float tileWidth() {
            return (getWidth() - (columns() - 1) * 4 * RogueLook.unit()) / columns();
        }

        private float levelsTop() {
            float u = RogueLook.unit();
            return (26 + (commanderLock() == null ? 0 : 34)) * u;
        }

        @Override
        protected void drawBackground(Graphics g) {
            float u = RogueLook.unit();
            float w = getWidth();
            Localizer localizer = Localizer.getInstance();
            RogueLook.text(g, localizer.getMessage("lblRogueDescensionMode"), 0, 0, 1.5f, RogueLook.INK, w, Align.center);
            String lock = commanderLock();
            if (lock != null) {
                RogueLook.skin(g, FSkinImage.LOCK, 0, 27 * u, 13 * u);
                RogueLook.wrap(g, lock, 18 * u, 26 * u, w - 18 * u, 1, RogueLook.INK, Align.left);
            }
            float y = levelsTop();
            int cols = columns();
            float bw = tileWidth();
            for (int i = 0; i <= DescensionLevel.getMaxLevel(); i++) {
                float bx = (i % cols) * (bw + 4 * u);
                float by = y + (i / cols) * 31 * u;
                boolean open = isOpen(i);
                boolean chosen = i == descension;
                if (open) {
                    RogueLook.tile(g, bx, by, bw, 27 * u, chosen);
                } else {
                    RogueLook.dashTile(g, bx, by, bw, 27 * u);
                    RogueLook.skin(g, FSkinImage.LOCK, bx + bw - 18 * u, by + 8 * u, 11 * u);
                }
                String label = i == 0 ? localizer.getMessage("lblOff") : i + "  " + DescensionLevel.forLevel(i).name;
                RogueLook.fit(g, label, bx + 7 * u, by + 7 * u, 1, open ? RogueLook.INK : RogueLook.SOFT, bw - (open ? 24 : 32) * u, Align.left);
                if (chosen) {
                    RogueLook.item(g, "OK", bx + bw - 20 * u, by + 6 * u, 14 * u);
                }
            }
            y += ((DescensionLevel.getMaxLevel() + cols) / cols) * 31 * u + 4 * u;
            RogueLook.line(g, 0, y, w, y, 0.8f, RogueLook.SOFT);
            y += 4 * u;
            g.startClip(0, 0, w, done.getTop() - 4 * u);
            if (lock == null) {
                for (int i = 1; i <= DescensionLevel.getMaxLevel(); i++) {
                    String levelLock = RogueStart.descensionLockText(RogueMetaProgress.getInstance(), deck(), i, custom);
                    if (levelLock != null) {
                        RogueLook.skin(g, FSkinImage.LOCK, 0, y + u, 11 * u);
                        RogueLook.wrap(g, levelLock, 15 * u, y, w - 15 * u, 1, RogueLook.SOFT, Align.left);
                        break;
                    }
                    DescensionLevel level = DescensionLevel.forLevel(i);
                    y += RogueLook.wrap(g, localizer.getMessage("lblRogueNamedLevel", i, level.name) + ": " + level.description,
                        0, y, w, 1, RogueLook.INK, Align.left) + 3 * u;
                }
            }
            g.endClip();
        }
    }
}
