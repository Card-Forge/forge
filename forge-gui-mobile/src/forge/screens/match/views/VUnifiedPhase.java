package forge.screens.match.views;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FImage;
import forge.assets.FSkinFont;
import forge.game.GameView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.screens.match.MatchController;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.util.Utils;

/**
 * Compact landscape layout, "Unified Phase Label" setting: one wide label to the right of the stack menu that names the
 * current phase. Tapping it opens a dropdown below with one row per player (small avatar and name) and that player's
 * phase stops laid out horizontally. Cells work exactly like the old per panel phase labels: tap toggles the stop,
 * long press marks a yield.
 */
public class VUnifiedPhase extends FDisplayObject {
    /** The phases in the order they are shown, same as the per panel labels. */
    static final PhaseType[] ORDER = {
        PhaseType.UPKEEP, PhaseType.DRAW, PhaseType.MAIN1, PhaseType.COMBAT_BEGIN,
        PhaseType.COMBAT_DECLARE_ATTACKERS, PhaseType.COMBAT_DECLARE_BLOCKERS, PhaseType.COMBAT_FIRST_STRIKE_DAMAGE,
        PhaseType.COMBAT_DAMAGE, PhaseType.COMBAT_END, PhaseType.MAIN2, PhaseType.END_OF_TURN, PhaseType.CLEANUP
    };

    private static final FSkinFont NAME_FONT = FSkinFont.get(13);
    private static final FSkinFont CAPTION_FONT = FSkinFont.get(11);
    private static final FSkinFont PLAYER_FONT = FSkinFont.get(12);
    private static final float OPEN_SECONDS = 0.16f;

    private static final Color GLASS = new Color(30 / 255f, 35 / 255f, 40 / 255f, 0.62f); //same glass as the player panels
    private static final Color BAND = new Color(1f, 1f, 1f, 0.07f);
    private static final Color BAND_OPEN = new Color(1f, 1f, 1f, 0.16f);
    private static final Color EDGE = new Color(1f, 1f, 1f, 0.12f);
    private static final Color MENU_BG = new Color(20 / 255f, 24 / 255f, 29 / 255f, 0.94f); //denser than the panels, it floats over cards
    private static final Color ROW_TURN = new Color(1f, 1f, 1f, 0.10f);
    private static final Color YIELD = new Color(0xFFA528FF);
    private static final Color IDLE_TEXT = new Color(0.72f, 0.72f, 0.72f, 1f);
    private static final Color FILL = new Color(), TEXT = new Color(), SCRATCH = new Color(); //render thread only

    private final Supplier<List<VPlayerPanel>> panels;
    private final Menu menu;

    /** @param panels0 the player panels, top to bottom; read live so panels of players who left disappear */
    public VUnifiedPhase(Supplier<List<VPlayerPanel>> panels0) {
        panels = panels0;
        menu = new Menu();
    }

    public Menu getMenu() {
        return menu;
    }

    static String getPhaseName(PhaseType phase) {
        if (phase == null) { return "--"; }
        return switch (phase) {
            case UNTAP -> "Untap";
            case UPKEEP -> "Upkeep";
            case DRAW -> "Draw";
            case MAIN1 -> "Main 1";
            case COMBAT_BEGIN -> "Begin Combat";
            case COMBAT_DECLARE_ATTACKERS -> "Declare Attackers";
            case COMBAT_DECLARE_BLOCKERS -> "Declare Blockers";
            case COMBAT_FIRST_STRIKE_DAMAGE -> "First Strike";
            case COMBAT_DAMAGE -> "Combat Damage";
            case COMBAT_END -> "End Combat";
            case MAIN2 -> "Main 2";
            case END_OF_TURN -> "End Step";
            case CLEANUP -> "Cleanup";
            default -> phase.nameForScripts;
        };
    }

    private static float getArrowSize() {
        return Utils.scale(4);
    }

    /** @return the width that fits the longest phase name and the arrow */
    public float getPreferredWidth() {
        float max = NAME_FONT.getBounds(getPhaseName(PhaseType.UNTAP)).width;
        for (PhaseType phase : ORDER) {
            max = Math.max(max, NAME_FONT.getBounds(getPhaseName(phase)).width);
        }
        return Math.round(max + 3 * Utils.scale(8) + 2 * getArrowSize());
    }

    private VPlayerPanel findPanel(PlayerView player) {
        for (VPlayerPanel panel : panels.get()) {
            if (panel.getPlayer().equals(player)) { return panel; }
        }
        return null;
    }

    @Override
    public boolean tap(float x, float y, int count) {
        if (!VPlayerPanel.isUnifiedPhase()) { return false; }
        menu.toggle();
        return true;
    }

    @Override
    public void draw(Graphics g) {
        if (!VPlayerPanel.isUnifiedPhase()) { //the setting or the compact layout was turned off
            menu.close();
            return;
        }
        final float w = getWidth(), h = getHeight();
        final GameView gv = MatchController.instance == null ? null : MatchController.instance.getGameView();
        final PhaseType phase = gv == null ? null : gv.getPhase();
        final PlayerView turn = gv == null ? null : gv.getPlayerTurn();
        final boolean open = menu.isOpen();

        g.fillRect(GLASS, 0, 0, w, h);
        g.fillRect(open ? BAND_OPEN : BAND, 0, 0, w, h);
        g.drawLine(1, EDGE, 0, 0, 0, h);

        //the name carries the stop colour of the player whose turn it is, like the old panel button did
        Color textColor = IDLE_TEXT;
        if (phase != null && turn != null) {
            final VPlayerPanel panel = findPanel(turn);
            final VPhaseIndicator.PhaseLabel label = panel == null ? null : panel.getPhaseIndicator().getLabel(phase);
            textColor = VPhaseIndicator.getPhaseColor(true, label != null && label.getStopAtPhase()).getColor();
        }
        final float padX = Utils.scale(8);
        final float arrow = getArrowSize();
        g.drawText(getPhaseName(phase), NAME_FONT, textColor, padX, 0, w - 3 * padX - 2 * arrow, h, false, Align.center, true);

        //small arrow on the right: down while closed, up while open
        final float cx = w - padX - arrow;
        final float cy = h / 2;
        SCRATCH.set(1f, 1f, 1f, open ? 0.95f : 0.6f);
        if (open) {
            g.fillTriangle(SCRATCH, cx - arrow, cy + arrow / 2, cx + arrow, cy + arrow / 2, cx, cy - arrow / 2);
        } else {
            g.fillTriangle(SCRATCH, cx - arrow, cy - arrow / 2, cx + arrow, cy - arrow / 2, cx, cy + arrow / 2);
        }
    }

    /** The dropdown: a row per player, each with the 12 phase stop cells. */
    public class Menu extends FContainer {
        private final float pad = Utils.scale(4), gap = Utils.scale(4);
        private final List<PlayerRow> rows = new ArrayList<>();
        private final List<VPlayerPanel> built = new ArrayList<>();
        private boolean openTarget;
        private float progress; //0 closed, 1 fully open
        private MenuAnimation animation;
        private float areaW, top, maxH;
        private float rowH = Utils.scale(30), avatarW = Utils.scale(26), nameW = Utils.scale(60);

        private Menu() {
            setVisible(false);
        }

        public boolean isOpen() {
            return openTarget;
        }

        public void toggle() {
            setOpen(!openTarget);
        }

        public void close() {
            setOpen(false);
        }

        public void setOpen(boolean open) {
            if (openTarget == open) { return; }
            openTarget = open;
            if (open) {
                sync();
                applyBounds();
                setVisible(true);
            }
            if (animation == null) {
                animation = new MenuAnimation();
                animation.start();
            }
        }

        /**
         * @param areaW0 width of the screen, the dropdown is right aligned to it
         * @param top0 where the dropdown starts (the bottom of the label)
         * @param maxH0 room available below it
         */
        public void layoutFor(float areaW0, float top0, float maxH0) {
            areaW = areaW0;
            top = top0;
            maxH = maxH0;
            if (openTarget || progress > 0f) {
                sync();
                applyBounds();
            }
        }

        //rebuilds the rows when the players changed (someone left)
        private void sync() {
            final List<VPlayerPanel> current = panels.get();
            if (built.equals(current)) { return; }
            for (PlayerRow row : rows) { remove(row); }
            rows.clear();
            built.clear();
            built.addAll(current);
            for (VPlayerPanel panel : current) {
                rows.add(add(new PlayerRow(panel)));
            }
            revalidate();
        }

        private void applyBounds() {
            if (areaW <= 0f) { return; }
            final int n = Math.max(1, built.size());
            float nameMax = 0;
            for (VPlayerPanel panel : built) {
                nameMax = Math.max(nameMax, PLAYER_FONT.getBounds(panel.getPlayer().getName()).width);
            }
            nameW = Math.round(Math.max(Utils.scale(44), Math.min(nameMax + Utils.scale(12), Utils.scale(100))));
            rowH = Math.round(Math.max(Utils.scale(20), Math.min(Utils.scale(34), (maxH - 2 * pad) / n)));
            avatarW = rowH - Utils.scale(4);
            final float fixed = pad + avatarW + gap + nameW + gap + pad;
            final float cell = Math.max(Utils.scale(16), Math.min(Utils.scale(30), (areaW - fixed) / ORDER.length));
            final float w = Math.min(areaW, Math.round(fixed + cell * ORDER.length));
            setBounds(areaW - w, top, w, 2 * pad + rowH * n);
            revalidate();
        }

        @Override
        protected void doLayout(float width, float height) {
            float y = pad;
            for (PlayerRow row : rows) {
                row.setBounds(0, y, width, rowH);
                y += rowH;
            }
        }

        @Override
        public void drawBackground(Graphics g) {
            final float w = getWidth(), h = getHeight();
            g.fillRect(MENU_BG, 0, 0, w, h);
            g.drawLine(1, EDGE, 0, 0, 0, h);
            g.drawLine(1, EDGE, w, 0, w, h);
            g.drawLine(1, EDGE, 0, h, w, h);
        }

        @Override
        public void draw(Graphics g) {
            if (progress <= 0f || !VPlayerPanel.isUnifiedPhase()) { return; }
            if (progress >= 1f) {
                super.draw(g);
                return;
            }
            g.startClip(0, 0, getWidth(), getHeight() * progress); //unfolds downwards from the label
            try {
                super.draw(g);
            } finally {
                g.endClip();
            }
        }

        private class MenuAnimation extends ForgeAnimation {
            @Override
            protected boolean advance(float dt) {
                final float step = dt / OPEN_SECONDS;
                progress = openTarget ? Math.min(1f, progress + step) : Math.max(0f, progress - step);
                return openTarget ? progress < 1f : progress > 0f;
            }

            @Override
            protected void onEnd(boolean endingAll) {
                animation = null;
                if (endingAll) { progress = openTarget ? 1f : 0f; }
                if (!openTarget && progress <= 0f) { setVisible(false); }
            }
        }

        /** One player: avatar, name and the stop cells. */
        private class PlayerRow extends FContainer {
            private final VPlayerPanel panel;
            private final PlayerView player;
            private final Strip strip;

            private PlayerRow(VPlayerPanel panel0) {
                panel = panel0;
                player = panel0.getPlayer();
                strip = add(new Strip());
            }

            @Override
            protected void doLayout(float width, float height) {
                final float stripX = pad + avatarW + gap + nameW + gap;
                strip.setBounds(stripX, 0, Math.max(1f, width - stripX - pad), height);
            }

            @Override
            public void drawBackground(Graphics g) {
                final float w = getWidth(), h = getHeight();
                final GameView gv = MatchController.instance == null ? null : MatchController.instance.getGameView();
                final boolean turn = gv != null && player.equals(gv.getPlayerTurn());
                if (turn) { //the player whose turn it is gets a lit row and an accent bar
                    g.fillRect(ROW_TURN, 0, 0, w, h);
                    final Color accent = VPhaseIndicator.getPhaseColor(true, true).getColor();
                    SCRATCH.set(accent.r, accent.g, accent.b, 1f);
                    g.fillRect(SCRATCH, 0, 0, Math.max(2f, Utils.scale(3)), h);
                }
                final float s = Math.max(1f, Math.min(avatarW, h - 2)); //just the player's avatar image, not a whole VAvatar
                final FImage image = MatchController.getPlayerAvatar(player);
                if (image != null) { image.draw(g, pad, (h - s) / 2, s, s); }
                final float nameX = pad + avatarW + gap;
                final float dot = Math.max(4f, Utils.scale(6));
                g.drawText(player.getName(), PLAYER_FONT, turn ? Color.WHITE : IDLE_TEXT, nameX, 0, nameW - dot - gap / 2, h, false, Align.left, true);
                if (player.getHasPriority()) { //small dot after the name for whoever holds priority
                    final Color c = VPhaseIndicator.getPhaseColor(false, true).getColor();
                    SCRATCH.set(c.r, c.g, c.b, 1f);
                    g.fillRoundRect(SCRATCH, nameX + nameW - dot, (h - dot) / 2, dot, dot, dot / 2);
                }
            }

            /** The 12 cells of one player. */
            private class Strip extends FDisplayObject {
                private VPhaseIndicator.PhaseLabel labelAt(float x) {
                    int i = (int) (x / (getWidth() / ORDER.length));
                    i = Math.max(0, Math.min(ORDER.length - 1, i));
                    return panel.getPhaseIndicator().getLabel(ORDER[i]);
                }

                @Override
                public boolean tap(float x, float y, int count) {
                    final VPhaseIndicator.PhaseLabel label = labelAt(x);
                    if (label == null) { return false; }
                    label.toggleStop();
                    return true;
                }

                @Override
                public boolean longPress(float x, float y) {
                    final VPhaseIndicator.PhaseLabel label = labelAt(x);
                    return label != null && label.fireLongPress();
                }

                @Override
                public void draw(Graphics g) {
                    final float cw = getWidth() / ORDER.length;
                    final float h = getHeight();
                    final float cellGap = Math.max(1f, Utils.scale(1));
                    final float inset = Math.max(2f, Utils.scale(3));
                    final float radius = Utils.scale(3);
                    final float bar = Math.max(2f, Utils.scale(2));
                    final float y = inset, ch = h - 2 * inset;
                    for (int i = 0; i < ORDER.length; i++) {
                        final VPhaseIndicator.PhaseLabel label = panel.getPhaseIndicator().getLabel(ORDER[i]);
                        if (label == null) { continue; }
                        final float x = i * cw + cellGap;
                        final float w = cw - 2 * cellGap;
                        final boolean active = label.getActive();
                        final boolean stop = label.getStopAtPhase();

                        if (label.isYieldMarked()) { //the chevron replaces the caption, as on the old labels
                            g.fillRoundRect(YIELD, x, y, w, ch, radius);
                            final float size = Math.max(Utils.scale(6f), ch * 0.55f);
                            final float cx = x + (w - size) / 2f;
                            final float cy = y + (ch - size) / 2f;
                            g.fillTriangle(Color.BLACK, cx, cy, cx + size / 2f, cy + size / 2f, cx, cy + size);
                            g.fillTriangle(Color.BLACK, cx + size / 2f, cy, cx + size, cy + size / 2f, cx + size / 2f, cy + size);
                            continue;
                        }

                        final Color tint = VPhaseIndicator.getPhaseColor(active, stop).getColor();
                        //current phase is bright, a phase the game stops at is tinted and underlined, the rest is dim
                        if (active && stop) { FILL.set(tint.r, tint.g, tint.b, 0.95f); }
                        else if (active) { FILL.set(1f, 1f, 1f, 0.32f); }
                        else if (stop) { FILL.set(tint.r, tint.g, tint.b, 0.42f); }
                        else { FILL.set(1f, 1f, 1f, 0.06f); }
                        g.fillRoundRect(FILL, x, y, w, ch, radius);
                        if (stop) {
                            FILL.set(tint.r, tint.g, tint.b, 1f);
                            g.fillRect(FILL, x + radius, y + ch - bar, w - 2 * radius, bar);
                        }
                        if (active && stop) { TEXT.set(0f, 0f, 0f, 1f); }
                        else { TEXT.set(1f, 1f, 1f, active || stop ? 0.95f : 0.5f); }
                        g.drawText(label.getCaption(), CAPTION_FONT, TEXT, x, y, w, ch - (stop ? bar : 0f), false, Align.center, true);
                    }
                }
            }
        }
    }
}
