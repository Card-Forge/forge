package forge.screens.match.views;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.gamemodes.match.YieldMarker;
import forge.gamemodes.match.YieldUpdate;
import forge.interfaces.IGameController;
import forge.screens.match.MatchController;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.util.TextBounds;
import forge.util.Utils;

public class VPhaseIndicator extends FContainer {
    public static final FSkinFont BASE_FONT = FSkinFont.get(11);
    public static final float PADDING_X = Utils.scale(1);
    public static final float PADDING_Y = Utils.scale(2);

    private static final Color YIELD_MARKER_COLOR = new Color(0xFFA528FF);

    private final Map<PhaseType, PhaseLabel> phaseLabels = new HashMap<>();
    private FSkinFont font;
    private PlayerView owner;

    public VPhaseIndicator() {
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

    private void addPhaseLabel(String caption, PhaseType phaseType) {
        phaseLabels.put(phaseType, add(new PhaseLabel(caption, phaseType)));
    }

    public PhaseLabel getLabel(PhaseType phaseType) {
        return phaseLabels.get(phaseType);
    }

    public Iterable<PhaseLabel> allLabels() {
        return phaseLabels.values();
    }

    public void setOwner(PlayerView player) {
        this.owner = player;
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
        private final PhaseType phaseType;
        private boolean stopAtPhase = false;
        private boolean active = false;
        private boolean yieldMarked = false;
        private Runnable onToggled;

        public PhaseLabel(String caption0, PhaseType phaseType0) {
            caption = caption0;
            phaseType = phaseType0;
        }

        public boolean getActive() {
            return active;
        }
        public void setActive(boolean active0) {
            active = active0;
        }

        public PhaseType getPhaseType() {
            return phaseType;
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

        @Override
        public boolean tap(float x, float y, int count) {
            stopAtPhase = !stopAtPhase;
            if (onToggled != null) onToggled.run();
            return true;
        }

        @Override
        public boolean longPress(float x, float y) {
            PlayerView phaseOwner = VPhaseIndicator.this.owner;
            if (phaseOwner == null) {
                return false;
            }
            PlayerView local = MatchController.instance.getCurrentPlayer();
            if (local == null) {
                return false;
            }
            IGameController ctrl = MatchController.instance.getGameController(local);
            if (ctrl == null) {
                return false;
            }
            YieldMarker existing = ctrl.getYieldController().getMarker();
            boolean clickedSameLabel = existing != null
                    && phaseOwner.equals(existing.getPhaseOwner())
                    && phaseType == existing.getPhase();
            if (clickedSameLabel) {
                ctrl.sendYieldUpdate(new YieldUpdate.ClearMarker(local));
            } else {
                // Setting a marker implies we want to stop here — un-skip the cell so the marker can fire.
                stopAtPhase = true;
                ctrl.sendYieldUpdate(new YieldUpdate.SetMarker(phaseOwner, phaseType));
                // Pass current priority so the marker takes effect immediately.
                ctrl.selectButtonOk();
            }
            MatchController.instance.refreshYieldUi(local);
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
            // Two back-to-back triangles centered in the cell, mirroring desktop.
            float size = Math.max(Utils.scale(6f), h * 0.55f);
            float cx = x + (w - size) / 2f;
            float cy = (h - size) / 2f;
            g.fillTriangle(Color.BLACK, cx,            cy,            cx + size / 2f, cy + size / 2f, cx,            cy + size);
            g.fillTriangle(Color.BLACK, cx + size / 2f, cy,            cx + size,      cy + size / 2f, cx + size / 2f, cy + size);
        }
    }
}
