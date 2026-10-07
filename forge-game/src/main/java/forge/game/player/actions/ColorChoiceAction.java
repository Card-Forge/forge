package forge.game.player.actions;

import forge.card.MagicColor;

public class ColorChoiceAction extends PlayerAction {
    private final byte color;

    public ColorChoiceAction(final byte color) {
        super(null, "Choose color");
        this.color = color;
    }

    public byte getColor() {
        return color;
    }

    @Override
    protected void appendDetails(final StringBuilder sb) {
        sb.append(" color=").append(MagicColor.toShortString(color));
    }
}
