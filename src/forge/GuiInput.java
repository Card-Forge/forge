package forge;


import forge.gui.input.Input;

import java.util.Observable;
import java.util.Observer;


/**
 * <p>GuiInput class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class GuiInput extends MyObservable implements Observer {
    Input input;

    /**
     * <p>Constructor for GuiInput.</p>
     */
    public GuiInput() {
        AllZone.getInputControl().addObserver(this);
        AllZone.getStack().addObserver(this);
        AllZone.getPhase().addObserver(this);
    }

    /** {@inheritDoc} */
    public void update(Observable observable, Object obj) {
        Input tmp = AllZone.getInputControl().updateInput();
        if (tmp != null) {
            setInput(tmp);
        }
    }

    /**
     * <p>Setter for the field <code>input</code>.</p>
     *
     * @param in a {@link forge.gui.input.Input} object.
     */
    private void setInput(Input in) {
        input = in;
        input.showMessage();
    }

    /**
     * <p>showMessage.</p>
     */
    public void showMessage() {
        input.showMessage();
    }

    /**
     * <p>selectButtonOK.</p>
     */
    public void selectButtonOK() {
        input.selectButtonOK();
    }

    /**
     * <p>selectButtonCancel.</p>
     */
    public void selectButtonCancel() {
        input.selectButtonCancel();
    }

    /**
     * <p>selectPlayer.</p>
     *
     * @param player a {@link forge.Player} object.
     */
    public void selectPlayer(Player player) {
        input.selectPlayer(player);
    }

    /**
     * <p>selectCard.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param zone a {@link forge.PlayerZone} object.
     */
    public void selectCard(Card card, PlayerZone zone) {
        input.selectCard(card, zone);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return input.toString();
    }
}
