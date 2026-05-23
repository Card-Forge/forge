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

    public boolean isObsoleteWhen(final boolean stackEmpty) {
        return !stackWasEmpty && stackEmpty;
    }

    public boolean isStaleFor(final PhaseType currentPhase) {
        return phase != null && currentPhase != null && phase.isBefore(currentPhase);
    }

    public boolean canReplay(final boolean currentStackEmpty, final PhaseType currentPhase) {
        return stackWasEmpty == currentStackEmpty
                && (phase == null || phase == currentPhase || canAdvanceTowardRecordedCombatPass(currentPhase));
    }

    public boolean canReplayDuringAttack(final PhaseType currentPhase) {
        return phase == null || phase == currentPhase;
    }

    public boolean isStackPassFor(final PhaseType currentPhase) {
        return !stackWasEmpty && phase == currentPhase;
    }

    public boolean isTrailingMainPhasePassCandidate(final PhaseType currentPhase) {
        return stackWasEmpty && phase == PhaseType.MAIN1 && currentPhase == PhaseType.MAIN1;
    }

    private boolean canAdvanceTowardRecordedCombatPass(final PhaseType currentPhase) {
        return stackWasEmpty
                && currentPhase != null
                && phase != null
                && currentPhase.isBefore(phase)
                && isCombatPhase(currentPhase)
                && (isCombatPhase(phase) || phase == PhaseType.MAIN2);
    }

    private static boolean isCombatPhase(final PhaseType phase) {
        return phase == PhaseType.COMBAT_BEGIN
                || phase == PhaseType.COMBAT_DECLARE_ATTACKERS
                || phase == PhaseType.COMBAT_DECLARE_BLOCKERS
                || phase == PhaseType.COMBAT_FIRST_STRIKE_DAMAGE
                || phase == PhaseType.COMBAT_DAMAGE
                || phase == PhaseType.COMBAT_END;
    }

    @Override
    public boolean clearsPostStackOrderWait() {
        return true;
    }

    @Override
    public PassPriorityAction asPassPriorityAction() {
        return this;
    }

    @Override
    public String describe() {
        final String phaseText = phase == null ? "" : " " + localize("lblMacroActionDuringPhase", phase.nameForUi);
        final String stackText = localize(stackWasEmpty ? "lblMacroStackEmpty" : "lblMacroStackNotEmpty");
        return localize("lblMacroActionPassPriority", phaseText, stackText);
    }
}
