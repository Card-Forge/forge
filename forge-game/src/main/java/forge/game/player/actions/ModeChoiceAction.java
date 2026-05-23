package forge.game.player.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModeChoiceAction extends PlayerAction {
    private final List<String> modeDescriptions;

    public ModeChoiceAction(final List<String> modeDescriptions) {
        super(null, "Choose mode");
        this.modeDescriptions = Collections.unmodifiableList(new ArrayList<>(modeDescriptions));
    }

    public List<String> getModeDescriptions() {
        return modeDescriptions;
    }

    @Override
    public String describe() {
        return localize(modeDescriptions.size() == 1 ? "lblMacroActionChooseMode" : "lblMacroActionChooseModes",
                describeList(modeDescriptions));
    }
}
