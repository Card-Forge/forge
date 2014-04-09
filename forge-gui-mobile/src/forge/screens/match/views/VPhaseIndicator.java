package forge.screens.match.views;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinColor.Colors;
import forge.game.phase.PhaseType;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.utils.Utils;

public class VPhaseIndicator extends FContainer {
    public static final float HEIGHT = Utils.AVG_FINGER_HEIGHT * 0.3f;
    private static final FSkinFont labelFont = FSkinFont.get(11);

    private final Map<PhaseType, PhaseLabel> phaseLabels = new HashMap<PhaseType, PhaseLabel>();

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

    public void resetPhaseButtons() {
        for (PhaseLabel lbl : phaseLabels.values()) {
            lbl.setActive(false);
        }
    }

    @Override
    protected void doLayout(float width, float height) {
        float x = 0;
        float padding = 1;
        float dx = width / phaseLabels.size();
        float w = dx - 2 * padding;
        float h = height - 2 * padding;

        for (FDisplayObject lbl : getChildren()) {
            lbl.setBounds(x + padding, padding, w, h);
            x += dx;
        }
    }

    public class PhaseLabel extends FDisplayObject {
        private final String caption;
        private final PhaseType phaseType;
        private boolean stopAtPhase = false;
        private boolean active = false;

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

        @Override
        public boolean tap(float x, float y, int count) {
            stopAtPhase = !stopAtPhase;
            return true;
        }

        @Override
        public void draw(final Graphics g) {
            float w = getWidth();
            float h = getHeight();
            FSkinColor c;

            // Set color according to skip or active state of label
            if (active && stopAtPhase) {
                c = FSkinColor.get(Colors.CLR_PHASE_ACTIVE_ENABLED);
            }
            else if (!active && stopAtPhase) {
                c = FSkinColor.get(Colors.CLR_PHASE_INACTIVE_ENABLED);
            }
            else if (active && !stopAtPhase) {
                c = FSkinColor.get(Colors.CLR_PHASE_ACTIVE_DISABLED);
            }
            else {
                c = FSkinColor.get(Colors.CLR_PHASE_INACTIVE_DISABLED);
            }

            // Center vertically and horizontally. Show border if active.
            //g.fillRoundRect(0, 0, w, h, 5, 5);
            g.fillRect(c, 0, 0, w, h);
            g.drawText(caption, labelFont, Color.BLACK, 0, 0, w, h, false, HAlignment.CENTER, true);
        }
    }
}
