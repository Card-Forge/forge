package forge.control.input;

import forge.Card;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface Input {

    // showMessage() is always the first method called
    void showMessage();

    void selectCard(Card c, boolean isMetaDown);

    void selectPlayer(Player player);

    void selectButtonOK();

    void selectButtonCancel();

}