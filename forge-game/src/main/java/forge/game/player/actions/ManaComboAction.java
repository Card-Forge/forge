package forge.game.player.actions;

import forge.card.MagicColor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ManaComboAction extends PlayerAction {
    private final Map<Byte, Integer> manaCombo;

    public ManaComboAction(final Map<Byte, Integer> manaCombo) {
        super(null, "Choose mana combination");
        this.manaCombo = Collections.unmodifiableMap(new LinkedHashMap<>(manaCombo));
    }

    public Map<Byte, Integer> getManaCombo() {
        return manaCombo;
    }

    @Override
    public String describe() {
        final StringBuilder sb = new StringBuilder();
        boolean needComma = false;
        for (final Map.Entry<Byte, Integer> entry : manaCombo.entrySet()) {
            final int amount = entry.getValue() == null ? 0 : entry.getValue();
            if (amount <= 0) {
                continue;
            }
            if (needComma) {
                sb.append(", ");
            }
            sb.append(describeColor(entry.getKey(), amount));
            needComma = true;
        }
        return localize("lblMacroActionChooseManaCombination",
                needComma ? sb.toString() : localize("lblMacroNone"));
    }

    private static String describeColor(final byte color, final int amount) {
        final String colorName = MagicColor.toLongString(color);
        return localize(amount == 1 ? "lblMacroManaAmount" : "lblMacroManaAmountPlural", amount, colorName);
    }
}
