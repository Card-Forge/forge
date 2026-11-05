package forge.screens.match.views;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

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
    private final Color scratchFill = new Color();

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

    //compact landscape layout: nothing is drawn until the phase button opens the stops column
    private BooleanSupplier onRight;

    /** @param onRight tells whether the panel's controls are on the right, so the stops open to their left */
    public void setOnRight(BooleanSupplier onRight) {
        this.onRight = onRight;
    }

    private static boolean isCompact() { return VPlayerPanel.isCompactLayout(); }

    /** Compact layout: opens the phase stops if they are hidden, hides them if they are showing. */
    public void toggleStops() {
        if (VPlayerPanel.isUnifiedPhase()) { return; } //the unified phase label owns the stops
        setExpanded(!expandTarget);
    }

    private Runnable onOpenWidthChanged;

    /** Compact layout: called whenever the open width of the stops column changes, so the panel can make room for it. */
    public void setOnOpenWidthChanged(Runnable onOpenWidthChanged0) {
        onOpenWidthChanged = onOpenWidthChanged0;
    }

    /** Compact layout: how much of the stops column is open right now. */
    public float getOpenWidth() {
        return isCompact() ? getWidth() * expand : 0f;
    }

    private void notifyOpenWidthChanged() {
        if (isCompact() && onOpenWidthChanged != null) { onOpenWidthChanged.run(); }
    }

    /** The stops column only takes touches while it is open, so it never blocks the field underneath. */
    public void syncCompactVisibility() {
        setVisible(!isCompact() || (!VPlayerPanel.isUnifiedPhase() && (expand > 0f || expandTarget)));
    }

    //the colours a phase stop label uses: whether it is the current phase, and whether the game stops at it
    private static final Color COMPACT_STOPS_BG = new Color(30 / 255f, 35 / 255f, 40 / 255f, 0.62f); //same glass as the player panel
    private static final Color COMPACT_STOPS_EDGE = new Color(1f, 1f, 1f, 0.12f);
    private static final Color COMPACT_BAND_ON = new Color(1f, 1f, 1f, 0.16f);
    private static final Color COMPACT_BAND = new Color(1f, 1f, 1f, 0.07f);
    private static final Color FROST_FILL = new Color(), FROST_TEXT = new Color(); //scratch colours for the stop labels, render thread only

    static FSkinColor getPhaseColor(boolean active, boolean stopAtPhase) {
        final boolean adv = Forge.isMobileAdventureMode;
        if (active) {
            return FSkinColor.get(stopAtPhase ? (adv ? Colors.ADV_CLR_PHASE_ACTIVE_ENABLED : Colors.CLR_PHASE_ACTIVE_ENABLED)
                    : (adv ? Colors.ADV_CLR_PHASE_ACTIVE_DISABLED : Colors.CLR_PHASE_ACTIVE_DISABLED));
        }
        return FSkinColor.get(stopAtPhase ? (adv ? Colors.ADV_CLR_PHASE_INACTIVE_ENABLED : Colors.CLR_PHASE_INACTIVE_ENABLED)
                : (adv ? Colors.ADV_CLR_PHASE_INACTIVE_DISABLED : Colors.CLR_PHASE_INACTIVE_DISABLED));
    }

    private String getPhaseCaption(PhaseType phase) {
        final PhaseLabel label = phaseLabels.get(phase);
        if (label != null) { return label.caption; }
        return phase == PhaseType.UNTAP ? "UT" : "--";
    }

    /** Compact layout: draws the button that names the current phase and opens the phase stops when pressed. */
    public void drawPhaseButton(Graphics g, float w, float h) {
        final GameView gv = MatchController.instance == null ? null : MatchController.instance.getGameView();
        final PhaseType phase = gv == null ? null : gv.getPhase();
        final boolean myTurn = gv != null && phase != null && gv.getPlayerTurn() != null && gv.getPlayerTurn().equals(player);

        final String text;
        final Color textColor;
        if (myTurn) { //soft band, the text carries the stop colour
            final PhaseLabel label = phaseLabels.get(phase);
            g.fillRect(expandTarget ? COMPACT_BAND_ON : COMPACT_BAND, 0, 0, w, h);
            text = getPhaseCaption(phase);
            textColor = getPhaseColor(true, label != null && label.stopAtPhase).getColor();
        } else {
            g.fillRect(expandTarget ? COMPACT_BAND_ON : COMPACT_BAND, 0, 0, w, h);
            text = "--";
            textColor = player.getHasPriority() ? getPhaseColor(false, true).getColor() : IDLE_NAME_COLOR;
        }
        g.drawText(text, NAME_FONT, textColor, 0, 0, w, h, false, Align.center, true);
        //stops open: the band just lightens, no outline
    }

    //reused colour so the tint doesn't allocate a new FSkinColor every frame
    private Color tint(FSkinColor color, float alpha) {
        final Color c = color.getColor();
        return scratchFill.set(c.r, c.g, c.b, alpha);
    }

    private void setExpanded(boolean open) {
        expandTarget = open;
        idleTime = 0;
        if (open && isCompact() && !VPlayerPanel.isUnifiedPhase()) { setVisible(true); }
        if (expandAnimation == null) {
            expandAnimation = new ExpandAnimation();
            expandAnimation.start();
        }
    }

    private void keepExpanded() { idleTime = 0; }

    @Override
    public boolean tap(float x, float y, int count) { //only reached when no stop label took the tap
        if (!Forge.isLandscapeMode()) { return false; }
        if (isCompact()) { //a stray touch on the open column only keeps it open, the phase button toggles it
            keepExpanded();
            return true;
        }
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

    //compact layout: the stops column slides open over the left edge of the field, then folds away again
    private void drawCompactStops(Graphics g) {
        if (expand <= 0f) { return; }
        final float w = getWidth(), h = getHeight();
        final float revealW = w * expand;
        final boolean right = onRight != null && onRight.getAsBoolean(); //opens towards the left of a right hand control column
        g.startClip(right ? w - revealW : 0, 0, revealW, h);
        try {
            g.fillRect(COMPACT_STOPS_BG, 0, 0, w, h);
            super.draw(g);
            g.drawLine(1, COMPACT_STOPS_EDGE, right ? 0 : w, 0, right ? 0 : w, h);
        } finally {
            g.endClip();
        }
    }

    @Override
    public void draw(Graphics g) {
        if (isCompact()) {
            drawCompactStops(g);
            return;
        }
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
                g.fillRect(tint(active, 0.4f * a), 0, 0, w, h);
                base = FSkinColor.get(Forge.isMobileAdventureMode ? Colors.ADV_CLR_TEXT : Colors.CLR_TEXT).getColor();
                if (phase != null) { text = phase.nameForScripts; }
            } else {
                g.fillRect(tint(inactive, 0.4f * a), 0, 0, w, h);
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
            final float before = expand;
            final boolean more;
            if (expandTarget) {
                expand = Math.min(1f, expand + step);
                if (isCompact()) { //compact layout: the stops stay open until the phase button closes them
                    more = expand < 1f;
                } else {
                    if (expand >= 1f) {
                        idleTime += dt;
                        if (idleTime >= IDLE_SECONDS) { expandTarget = false; }
                    }
                    more = true;
                }
            } else {
                expand = Math.max(0f, expand - step);
                if (expand <= 0f && isCompact()) { setVisible(false); }
                more = expand > 0f;
            }
            if (expand != before) { notifyOpenWidthChanged(); }
            return more;
        }

        @Override
        protected void onEnd(boolean endingAll) {
            if (endingAll) { expand = expandTarget ? 1f : 0f; }
            expandAnimation = null;
            if (isCompact()) {
                syncCompactVisibility();
                notifyOpenWidthChanged();
            }
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

        /** The short caption of the label (UP, DR, M1...). */
        public String getCaption() {
            return caption;
        }

        /** Flips the stop exactly like a tap on the label does, for the unified phase label's dropdown. */
        public void toggleStop() {
            stopAtPhase = !stopAtPhase;
            if (onToggled != null) { onToggled.run(); }
        }

        /** Runs the long press action (yield marking) like a long press on the label does. @return false if there is none */
        public boolean fireLongPress() {
            if (onLongPress == null) { return false; }
            onLongPress.run();
            return true;
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
            } else if (VPlayerPanel.isFrosted()) { //modern: soft translucent cells, the stop colour tints them
                final Color tint = getPhaseColor(active, stopAtPhase).getColor();
                if (isHovered()) { FROST_FILL.set(1f, 1f, 1f, 0.18f); }
                else if (active && stopAtPhase) { FROST_FILL.set(tint.r, tint.g, tint.b, 0.90f); }
                else if (stopAtPhase) { FROST_FILL.set(tint.r, tint.g, tint.b, 0.40f); }
                else if (active) { FROST_FILL.set(1f, 1f, 1f, 0.28f); }
                else { FROST_FILL.set(1f, 1f, 1f, 0.07f); }
                final float gap = Math.max(1f, Utils.scale(1));
                g.fillRect(FROST_FILL, x, gap, w, h - 2 * gap);
                if (active && stopAtPhase) { FROST_TEXT.set(Color.BLACK); }
                else { FROST_TEXT.set(1f, 1f, 1f, active || stopAtPhase ? 0.95f : 0.55f); }
                g.drawText(caption, font, FROST_TEXT, x, 0, w, h, false, Align.center, true);
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
