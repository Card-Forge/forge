package forge.game.player.actions;

import java.util.LinkedHashMap;
import java.util.Map;

public class ManaComboAction extends PlayerAction {
    private final Map<Byte, Integer> manaCombo;

    public ManaComboAction(final Map<Byte, Integer> manaCombo) {
        super(null, "Choose mana combination");
        this.manaCombo = new LinkedHashMap<>(manaCombo);
    }

    public Map<Byte, Integer> getManaCombo() {
        return manaCombo;
    }

    @Override
    protected void appendDetails(final StringBuilder sb) {
        sb.append(" manaCombo=").append(manaCombo);
    }
}
