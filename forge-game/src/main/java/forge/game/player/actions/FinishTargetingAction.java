package forge.game.player.actions;

public class FinishTargetingAction extends PlayerAction {
    public FinishTargetingAction() {
        super(null, "Finish game entity");
    }

    @Override
    public boolean isTargetSelectionAction() {
        return true;
    }

    @Override
    public String describe() {
        return localize("lblMacroActionFinishSelecting");
    }
}
