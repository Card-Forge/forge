package forge.game.player.actions;

import java.util.ArrayList;
import java.util.List;

public class ModeChoiceAction extends PlayerAction {
    private final List<String> modeDescriptions;

    public ModeChoiceAction(final List<String> modeDescriptions) {
        super(null, "Choose mode");
        this.modeDescriptions = new ArrayList<>(modeDescriptions);
    }

    public List<String> getModeDescriptions() {
        return modeDescriptions;
    }
}
