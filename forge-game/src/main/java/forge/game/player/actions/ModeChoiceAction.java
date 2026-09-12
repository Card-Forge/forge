package forge.game.player.actions;

import java.util.List;

public class ModeChoiceAction extends PlayerAction {
    private final List<String> modeDescriptions;

    public ModeChoiceAction(final List<String> modeDescriptions) {
        super(null, "Choose mode");
        this.modeDescriptions = modeDescriptions;
    }

    public List<String> getModeDescriptions() {
        return modeDescriptions;
    }

    @Override
    protected void appendDetails(final StringBuilder sb) {
        sb.append(" modes=").append(modeDescriptions);
    }
}
