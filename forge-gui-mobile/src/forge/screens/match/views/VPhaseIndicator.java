package forge.screens.match.views;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.animation.ForgeAnimation;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.game.GameView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.match.MatchController;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.util.TextBounds;
import forge.util.Utils;

public class VPhaseIndicator extends FContainer {
    public static boolean HIDE_PHASESTOP = FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_HIDE_PHASESTOP);
    public static final FSkinFont BASE_FONT = FSkinFont.get(11);
    public static final float PADDING_X = Utils.scale(1);
    public static final float PADDING_Y = Utils.scale(2);

    private static final Color YIELD_MARKER_COLOR = new Color(0xFFA528FF);

    private final Map<PhaseType, PhaseLabel> phaseLabels = new HashMap<>();
    private FSkinFont font;
    private static final FSkinFont NAME_FONT = FSkinFont.get(14);
    private static final Color IDLE_NAME_COLOR = new Color(0.65f, 0.65f, 0.65f, 1f);
    private static final float NAME_PADDING = Utils.scale(2);
    private static final float EXPAND_DURATION = 0.18f;
    private static final float IDLE_SECONDS = 5.5f; //stops stay up this long after the last tap

    private final PlayerView player;
    private float expand; //landscape only: 0 = phase name, 1 = phase stops
    private boolean expandTarget;
    private float idleTime;
    private ExpandAnimation expandAnimation;
    private Color scratchColor = new Color();

    public VPhaseIndicator(PlayerView player0) {
        player = player0;
        addPhaseLabel("UP", PhaseType.UPKEEP);
        addPhaseLabel("DR", PhaseType.DRAW);
        addPhaseLabel("M1", PhaseType.MAIN1);
        addPhaseLabel("BC", PhaseType.COMBAT_BEGIN);
        addPhaseLabel("DA", PhaseType.COMBAT_DECLARE_ATTACKERS);
        addPhaseLabel("DB", PhaseType.COMBAT_DECLARE_BLOCKERS);
        addPhaseLabel("FS", PhaseType.COMBAT_FIRST_STRIKE_DAMAGE);
        addPhaseLabel("CD", PhaseType.COMBAT_DAMAGE);
        addPhaseLabel("EC", PhaseType.COMBAT_END);
        addPhaseLabel("M2", PhaseType.MAIN2);
        addPhaseLabel("ET", PhaseType.END_OF_TURN);
        addPhaseLabel("CL", PhaseType.CLEANUP);
    }

    private boolean isCollapsedMode() { return Forge.isLandscapeMode() && !expandTarget; }

    private void setExpanded(boolean open) {
        expandTarget = open;
        idleTime = 0;
        if (expandAnimation == null) {
            expandAnimation = new ExpandAnimation();
            expandAnimation.start();
        }
    }

    private void keepExpanded() { idleTime = 0; }

    @Override
    public boolean tap(float x, float y, int count) { //only reached when no stop label took the tap
        if (!Forge.isLandscapeMode()) { return false; }
        setExpanded(!expandTarget);
        return true;
    }

    //upright letters, one per line, reading downward and centered in the column
    private void drawStacked(Graphics g, String text, Color color) {
        final float maxW = getWidth() - 2 * NAME_PADDING;
        final float maxH = getHeight() - 2 * NAME_PADDING;

        float units = 0; //a space counts as half a line
        for (int i = 0; i < text.length(); i++) {
            units += text.charAt(i) == ' ' ? 0.5f : 1f;
        }

        FSkinFont font = NAME_FONT;
        while (font.canShrink() && (font.getLineHeight() * 0.9f * units > maxH || font.getBounds("W").width > maxW)) {
            font = font.shrink();
        }

        final float lineH = font.getLineHeight();
        final float step = lineH * 0.9f;
        float y = (getHeight() - step * units) / 2;
        for (int i = 0; i < text.length(); i++) {
            final char ch = text.charAt(i);
            if (ch == ' ') {
                y += step * 0.5f;
                continue;
            }
            //box is a full line tall so drawText doesn't shrink the glyph further
            g.drawText(String.valueOf(ch), font, color, 0, y - (lineH - step) / 2, getWidth(), lineH + 2, false, Align.center, true);
            y += step;
        }
    }

    @Override
    public void draw(Graphics g) {
        if (!Forge.isLandscapeMode() || !HIDE_PHASESTOP) { // TODO: Add better design or recreate a new widget
            super.draw(g);
            return;
        }

        final float w = getWidth(), h = getHeight();
        //g.fillRect(FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_THEME2 : Colors.CLR_THEME2), 0, 0, w, h);

        final GameView gv = MatchController.instance == null ? null : MatchController.instance.getGameView();
        final PhaseType phase = gv == null ? null : gv.getPhase();
        final FSkinColor active = FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_PHASE_INACTIVE_ENABLED : Colors.CLR_PHASE_INACTIVE_ENABLED);
        final FSkinColor inactive = FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_PHASE_INACTIVE_DISABLED : Colors.CLR_PHASE_INACTIVE_DISABLED);

        if (expand < 1f && phase != null) {
            final float a = 1f - expand;
            final boolean myTurn = gv.getPlayerTurn() != null && gv.getPlayerTurn().equals(player);
            String text = null;
            Color base;
            if (myTurn) {
                g.fillRect(active.alphaColor(0.4f * a), 0, 0, w, h);
                base = FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_TEXT : Colors.CLR_TEXT).getColor();
                if (phase != null) { text = phase.nameForScripts; }
            } else {
                g.fillRect(inactive.alphaColor(0.4f * a), 0, 0, w, h);
                base = player.getHasPriority() ? active.getColor() : IDLE_NAME_COLOR;
                text = "--";//Forge.getLocalizer().getMessage("lblPriority");
            }
            if (text != null) {
                scratchColor.set(base.r, base.g, base.b, a);
                final float cx = w / 2, cy = h / 2;
                final float len = h - 2 * NAME_PADDING, thick = w - 2 * NAME_PADDING;
                g.startRotateTransform(cx, cy, 90); //use -90 to read top-to-bottom
                try {
                    g.drawText(text, NAME_FONT, scratchColor, cx - len / 2, cy - thick / 2, len, thick, false, Align.center, true);
                } finally {
                    g.endTransform();
                }
            }
        }
        /*if (expand < 1f && gv != null) {
            final float a = 1f - expand;
            final boolean myTurn = gv.getPlayerTurn() != null && gv.getPlayerTurn().equals(player);
            String text = null;
            Color base;
            if (myTurn) {
                g.fillRect(active.alphaColor(0.35f * a), 0, 0, w, h); //turn owner tint
                base = FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_TEXT : Colors.CLR_TEXT).getColor();
                if (phase != null) { text = phase.nameForScripts; }
            } else {
                g.fillRect(inactive.alphaColor(0.35f * a), 0, 0, w, h); //turn owner tint
                base = player.getHasPriority() ? active.getColor() : IDLE_NAME_COLOR;
                text = "|";//Forge.getLocalizer().getMessage("lblPriority");
            }
            if (text != null) {
                drawStacked(g, text.toUpperCase(), new Color(base.r, base.g, base.b, a));
            }
        }*/

        if (expand >= 1f) {
            super.draw(g);
        } else if (expand > 0f) { //unfold from the middle
            final float revealH = h * expand;
            g.startClip(0, (h - revealH) / 2, w, revealH);
            try {
                super.draw(g);
            } finally {
                g.endClip();
            }
        }
    }

    private class ExpandAnimation extends ForgeAnimation {
        @Override
        protected boolean advance(float dt) {
            final float step = dt / EXPAND_DURATION;
            if (expandTarget) {
                expand = Math.min(1f, expand + step);
                if (expand >= 1f) {
                    idleTime += dt;
                    if (idleTime >= IDLE_SECONDS) { expandTarget = false; }
                }
                return true;
            }
            expand = Math.max(0f, expand - step);
            return expand > 0f;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            if (endingAll) { expand = expandTarget ? 1f : 0f; }
            expandAnimation = null;
        }
    }

    private void addPhaseLabel(String caption, PhaseType phaseType) {
        phaseLabels.put(phaseType, add(new PhaseLabel(caption)));
    }

    public PhaseLabel getLabel(PhaseType phaseType) {
        return phaseLabels.get(phaseType);
    }

    public Iterable<PhaseLabel> allLabels() {
        return phaseLabels.values();
    }

    public void resetPhaseButtons() {
        for (PhaseLabel lbl : phaseLabels.values()) {
            lbl.setActive(false);
        }
    }

    public void resetFont() {
        font = BASE_FONT;
    }

    public float getPreferredHeight(float width) {
        //build string to use to determine ideal font
        float w = width / phaseLabels.size();
        w -= 2 * PADDING_X;
        resetFont();
        return _getPreferredHeight(w);
    }
    private float _getPreferredHeight(float w) {
        TextBounds bounds = null;
        for (PhaseLabel lbl : phaseLabels.values()) {
            bounds = font.getBounds(lbl.caption);
            if (bounds.width > w) {
                if (font.canShrink()) {
                    font = font.shrink();
                    return _getPreferredHeight(w);
                }
                break;
            }
        }
        return bounds.height + 2 * PADDING_Y;
    }

    @Override
    protected void doLayout(float width, float height) {
        if (width > height) {
            float x = 0;
            float w = width / phaseLabels.size();
            float h = height;

            for (FDisplayObject lbl : getChildren()) {
                lbl.setBounds(x, 0, w, h);
                x += w;
            }
        }
        else {
            float padding = Utils.scale(1);
            float y = 0;
            float w = width - 2 * padding;
            float h = height / phaseLabels.size();

            for (FDisplayObject lbl : getChildren()) {
                lbl.setBounds(padding, y + padding, w, h - 2 * padding);
                y += h;
            }
        }
    }

    public class PhaseLabel extends FDisplayObject {
        private final String caption;
        private boolean stopAtPhase = false;
        private boolean active = false;
        private boolean yieldMarked = false;
        private Runnable onToggled;
        private Runnable onLongPress;

        public PhaseLabel(String caption0) {
            caption = caption0;
        }

        public boolean getActive() {
            return active;
        }
        public void setActive(boolean active0) {
            active = active0;
        }

        public boolean getStopAtPhase() {
            return stopAtPhase;
        }
        public void setStopAtPhase(boolean stopAtPhase0) {
            stopAtPhase = stopAtPhase0;
        }

        public boolean isYieldMarked() {
            return yieldMarked;
        }
        public void setYieldMarked(boolean v) {
            this.yieldMarked = v;
        }

        /** Fires after the user toggles this label by tapping. */
        public void setOnToggled(Runnable r) {
            onToggled = r;
        }

        /** Fires when the user long-presses this label. */
        public void setOnLongPress(Runnable r) {
            onLongPress = r;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            if (isCollapsedMode()) { return false; } //let the indicator expand instead
            stopAtPhase = !stopAtPhase;
            if (onToggled != null) onToggled.run();
            keepExpanded();
            return true;
        }

        @Override
        public boolean longPress(float x, float y) {
            if (onLongPress == null || isCollapsedMode()) {
                return false;
            }
            keepExpanded();
            onLongPress.run();
            return true;
        }

        @Override
        public void draw(final Graphics g) {
            float x = PADDING_X;
            float w = getWidth() - 2 * PADDING_X;
            float h = getHeight();

            //determine back color according to skip or active state of label
            if (yieldMarked) {
                g.fillRect(YIELD_MARKER_COLOR, x, 0, w, h);
                drawChevron(g, x, w, h);
                // Skip the caption when marked — chevron replaces the phase abbreviation.
            } else {
                FSkinColor backColor;
                if (active && stopAtPhase) {
                    backColor = Forge.isMobileAdventureMode ? FSkinColor.get(Colors.ADV_CLR_PHASE_ACTIVE_ENABLED) : FSkinColor.get(Colors.CLR_PHASE_ACTIVE_ENABLED);
                }
                else if (!active && stopAtPhase) {
                    backColor = Forge.isMobileAdventureMode ? FSkinColor.get(Colors.ADV_CLR_PHASE_INACTIVE_ENABLED) : FSkinColor.get(Colors.CLR_PHASE_INACTIVE_ENABLED);
                }
                else if (active && !stopAtPhase) {
                    backColor = Forge.isMobileAdventureMode ? FSkinColor.get(Colors.ADV_CLR_PHASE_ACTIVE_DISABLED) : FSkinColor.get(Colors.CLR_PHASE_ACTIVE_DISABLED);
                }
                else {
                    backColor = Forge.isMobileAdventureMode ? FSkinColor.get(Colors.ADV_CLR_PHASE_INACTIVE_DISABLED) : FSkinColor.get(Colors.CLR_PHASE_INACTIVE_DISABLED);
                }
                g.fillRect(isHovered() ? backColor.brighter() : backColor, x, 0, w, h);
                g.drawText(caption, isHovered() && font.canIncrease() ? font.increase() : font, Color.BLACK, x, 0, w, h, false, Align.center, true);
            }
        }

        private void drawChevron(final Graphics g, float x, float w, float h) {
            // Two back-to-back triangles centered in the cell
            float size = Math.max(Utils.scale(6f), h * 0.55f);
            float cx = x + (w - size) / 2f;
            float cy = (h - size) / 2f;
            g.fillTriangle(Color.BLACK, cx,            cy,            cx + size / 2f, cy + size / 2f, cx,            cy + size);
            g.fillTriangle(Color.BLACK, cx + size / 2f, cy,            cx + size,      cy + size / 2f, cx + size / 2f, cy + size);
        }
    }
}
