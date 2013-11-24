package forge.gui.input;

import java.awt.event.MouseEvent;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface Input {

    // showMessage() is always the first method called
    void showMessageInitial();

    void selectCard(Card c, MouseEvent triggerEvent);
    
    void selectAbility(SpellAbility ab);

    void selectPlayer(Player player);

    void selectButtonOK();

    void selectButtonCancel();

}