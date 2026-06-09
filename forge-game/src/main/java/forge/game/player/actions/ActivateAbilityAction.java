package forge.game.player.actions;

import forge.game.GameEntityView;

public class ActivateAbilityAction extends PlayerAction {
    private final String abilityDescription;

    public ActivateAbilityAction(GameEntityView cardView, String abilityDescription) {
        super(cardView, "Activate ability");
        this.abilityDescription = abilityDescription;
    }

    public String getAbilityDescription() {
        return abilityDescription;
    }

    @Override
    public String describe() {
        return localize("lblMacroActionActivateAbility", describeEntity(), abilityDescription);
    }
}
