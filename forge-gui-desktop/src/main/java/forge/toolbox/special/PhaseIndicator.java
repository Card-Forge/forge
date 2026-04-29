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
        addPhaseLabel("UP", PhaseType.UPKEEP,                      "htmlPhaseUpkeepTooltip");
        addPhaseLabel("DR", PhaseType.DRAW,                        "htmlPhaseDrawTooltip");
        addPhaseLabel("M1", PhaseType.MAIN1,                       "htmlPhaseMain1Tooltip");
        addPhaseLabel("BC", PhaseType.COMBAT_BEGIN,                "htmlPhaseBeginCombatTooltip");
        addPhaseLabel("DA", PhaseType.COMBAT_DECLARE_ATTACKERS,    "htmlPhaseDeclareAttackersTooltip");
        addPhaseLabel("DB", PhaseType.COMBAT_DECLARE_BLOCKERS,     "htmlPhaseDeclareBlockersTooltip");
        addPhaseLabel("FS", PhaseType.COMBAT_FIRST_STRIKE_DAMAGE,  "htmlPhaseFirstStrikeDamageTooltip");
        addPhaseLabel("CD", PhaseType.COMBAT_DAMAGE,               "htmlPhaseCombatDamageTooltip");
        addPhaseLabel("EC", PhaseType.COMBAT_END,                  "htmlPhaseEndCombatTooltip");
        addPhaseLabel("M2", PhaseType.MAIN2,                       "htmlPhaseMain2Tooltip");
        addPhaseLabel("ET", PhaseType.END_OF_TURN,                 "htmlPhaseEndTurnTooltip");
        addPhaseLabel("CL", PhaseType.CLEANUP,                     "htmlPhaseCleanupTooltip");
    }

    private void addPhaseLabel(String caption, PhaseType phaseType, String tooltipKey) {
        PhaseLabel lbl = new PhaseLabel(caption, phaseType);
        lbl.setToolTipText(Localizer.getInstance().getMessage(tooltipKey));
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
