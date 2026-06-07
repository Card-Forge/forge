package forge.toolbox.special;

import java.util.EnumMap;
import java.util.Map;

import javax.swing.JPanel;

import forge.game.phase.PhaseType;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

public class PhaseIndicator extends JPanel {
    private static final long serialVersionUID = -863730022835609252L;

    private static final String CONSTRAINTS = "w 94%!, h 7.2%, gaptop 1%, gapleft 3%";

    private final Map<PhaseType, PhaseLabel> phaseLabels = new EnumMap<>(PhaseType.class);

    public PhaseIndicator() {
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0 0 1% 0, gap 0, wrap"));
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
        PhaseLabel lbl = new PhaseLabel(caption, phaseType);
        lbl.setToolTipText(Localizer.getInstance().getMessage("htmlPhaseTooltipFmt", phaseType.nameForUi));
        phaseLabels.put(phaseType, lbl);
        add(lbl, CONSTRAINTS);
    }

    public PhaseLabel getLabelFor(final PhaseType phaseType) {
        return phaseLabels.get(phaseType);
    }

    public Iterable<PhaseLabel> allLabels() {
        return phaseLabels.values();
    }

    /** Resets all phase buttons to "inactive". "Enabled" state is preserved. */
    public void resetPhaseButtons() {
        for (PhaseLabel lbl : phaseLabels.values()) {
            lbl.setActive(false);
        }
    }
}
