package forge.game.player.actions;


public class PayManaFromPoolAction extends PlayerAction{
    private byte colorSelected;
    public PayManaFromPoolAction(byte colorCode) {
        super(null);

        name = "Pay mana";
        colorSelected = colorCode;
    }

    public byte getSelectedColor() {
        return colorSelected;
    }
}
