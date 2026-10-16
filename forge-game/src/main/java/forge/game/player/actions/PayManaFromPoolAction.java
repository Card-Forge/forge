package forge.game.player.actions;

import forge.card.MagicColor;

public class PayManaFromPoolAction extends PlayerAction {
    private final byte colorSelected;

    public PayManaFromPoolAction(byte colorCode) {
        super(null, "Pay mana");
        colorSelected = colorCode;
    }

    public byte getSelectedColor() {
        return colorSelected;
    }

    @Override
    public String describe() {
        return localize("lblMacroActionPayManaFromPool", MagicColor.toSymbol(colorSelected));
    }
}
