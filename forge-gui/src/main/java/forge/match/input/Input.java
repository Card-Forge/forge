package forge.match.input;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.awt.event.MouseEvent;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface Input {

    // showMessage() is always the first method called
    void showMessageInitial();

    void selectCard(Card c, MouseEvent triggerEvent);
    
    void selectAbility(SpellAbility ab);

    void selectPlayer(Player player, MouseEvent triggerEven);

    void selectButtonOK();

    void selectButtonCancel();

}