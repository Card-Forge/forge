package forge.game.player.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StackOrderAction extends PlayerAction {
    private final List<String> abilityDescriptions;

    public StackOrderAction(final List<String> abilityDescriptions) {
        super(null, "Order simultaneous abilities");
        this.abilityDescriptions = Collections.unmodifiableList(new ArrayList<>(abilityDescriptions));
    }

    public List<String> getAbilityDescriptions() {
        return abilityDescriptions;
    }

    @Override
    public String describe() {
        return localize("lblMacroActionOrderAbilities", describeList(abilityDescriptions));
    }
}
