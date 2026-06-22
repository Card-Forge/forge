package forge.game.player.actions;


public class PayManaFromPoolAction extends PlayerAction{
    private byte colorSelected;
    public PayManaFromPoolAction(byte colorCode) {
        super(null, "Pay mana");
        colorSelected = colorCode;
    }

    public byte getSelectedColor() {
        return colorSelected;
    }

    @Override
    protected void appendDetails(final StringBuilder sb) {
        sb.append(" mana=").append(colorSelected);
    }
}
