package forge;

import java.util.Observable;
import java.util.Observer;

import forge.gui.input.Input;

/**
 * <p>
 * GuiInput class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GuiInput extends MyObservable implements Observer {

    /** The input. */
    private Input input;

    /**
     * <p>
     * Constructor for GuiInput.
     * </p>
     */
    public GuiInput() {
        AllZone.getInputControl().addObserver(this);
        AllZone.getStack().addObserver(this);
        AllZone.getPhase().addObserver(this);
    }

    /** {@inheritDoc} */
    public final void update(final Observable observable, final Object obj) {
        Input tmp = AllZone.getInputControl().updateInput();
        if (tmp != null) {
            setInput(tmp);
        }
    }

    /**
     * <p>
     * Setter for the field <code>input</code>.
     * </p>
     * 
     * @param in
     *            a {@link forge.gui.input.Input} object.
     */
    private void setInput(final Input in) {
        input = in;
        input.showMessage();
    }

    /**
     * <p>
     * showMessage.
     * </p>
     */
    public final void showMessage() {
        getInput().showMessage();
    }

    /**
     * <p>
     * selectButtonOK.
     * </p>
     */
    public final void selectButtonOK() {
        getInput().selectButtonOK();
    }

    /**
     * <p>
     * selectButtonCancel.
     * </p>
     */
    public final void selectButtonCancel() {
        getInput().selectButtonCancel();
    }

    /**
     * <p>
     * selectPlayer.
     * </p>
     * 
     * @param player
     *            a {@link forge.Player} object.
     */
    public final void selectPlayer(final Player player) {
        getInput().selectPlayer(player);
    }

    /**
     * <p>
     * selectCard.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param zone
     *            a {@link forge.PlayerZone} object.
     */
    public final void selectCard(final Card card, final PlayerZone zone) {
        getInput().selectCard(card, zone);
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return getInput().toString();
    }

    /**
     * @return the input
     */
    public Input getInput() {
        return input;
    }
}
