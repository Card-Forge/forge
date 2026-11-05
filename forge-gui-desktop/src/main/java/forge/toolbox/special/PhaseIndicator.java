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
        PhaseLabel lbl = new PhaseLabel(caption);
        lbl.setToolTipText(Localizer.getInstance().getMessage("htmlPhaseTooltipFmt", phaseType.nameForUi));
        phaseLabels.put(phaseType, lbl);
        add(lbl, CONSTRAINTS);
    }

    /**
     * Arena player bar: the phase ladder flanks the portrait, so the chips are moved into
     * two rows, beginning of turn through declare blockers, then the rest of the turn.
     */
    public JPanel[] createArenaLadder() {
        final JPanel first = new JPanel(new MigLayout("insets 0, gap 1, ay center, ax right"));
        final JPanel second = new JPanel(new MigLayout("insets 0, gap 1, ay center"));
        first.setOpaque(false);
        second.setOpaque(false);
        int i = 0;
        for (final Map.Entry<PhaseType, PhaseLabel> e : phaseLabels.entrySet()) {
            final int kind = switch (e.getKey()) {
                case MAIN1, MAIN2 -> PhaseLabel.ARENA_MAIN;
                case COMBAT_DECLARE_ATTACKERS -> PhaseLabel.ARENA_ATTACK;
                case COMBAT_DECLARE_BLOCKERS -> PhaseLabel.ARENA_BLOCK;
                default -> PhaseLabel.ARENA_STEP;
            };
            e.getValue().setArenaKind(kind);
            // the same spacing either side of the portrait: the step between main and attack takes up what the three between block and main do
            final String size = e.getKey() == PhaseType.COMBAT_BEGIN ? "w 29!, h 34!" : kind == PhaseLabel.ARENA_STEP ? "w 9!, h 34!" : "w 34!, h 34!";
            (i++ < phaseLabels.size() / 2 ? first : second).add(e.getValue(), size);
        }
        return new JPanel[] {first, second};
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
