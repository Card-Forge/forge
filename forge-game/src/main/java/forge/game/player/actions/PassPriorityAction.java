package forge.game.player.actions;

import forge.game.phase.PhaseType;

public class PassPriorityAction extends PlayerAction {
    private final boolean stackWasEmpty;
    private final PhaseType phase;

    public PassPriorityAction() {
        this(true, null);
    }

    public PassPriorityAction(final boolean stackWasEmpty, final PhaseType phase) {
        super(null, "Pass Priority");
        this.stackWasEmpty = stackWasEmpty;
        this.phase = phase;
    }

    public boolean wasStackEmpty() {
        return stackWasEmpty;
    }

    public PhaseType getPhase() {
        return phase;
    }

    @Override
    public String describe() {
        final String phaseText = phase == null ? "" : " " + localize("lblMacroActionDuringPhase", describePhase(phase));
        final String stackText = localize(stackWasEmpty ? "lblMacroStackEmpty" : "lblMacroStackNotEmpty");
        return localize("lblMacroActionPassPriority", phaseText, stackText);
    }

    private static String describePhase(final PhaseType phase) {
        return switch (phase) {
            case MAIN1 -> localize("lblMacroPhaseMain1");
            case MAIN2 -> localize("lblMacroPhaseMain2");
            default -> phase.nameForUi;
        };
    }
}
