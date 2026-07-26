package forge.game.player.actions;

import java.util.List;

public class StackOrderAction extends PlayerAction {
    private final List<String> abilityDescriptions;

    public StackOrderAction(final List<String> abilityDescriptions) {
        super(null, "Order simultaneous abilities");
        this.abilityDescriptions = abilityDescriptions;
    }

    public List<String> getAbilityDescriptions() {
        return abilityDescriptions;
    }

    @Override
    protected void appendDetails(final StringBuilder sb) {
        sb.append(" order=").append(abilityDescriptions);
    }
}
