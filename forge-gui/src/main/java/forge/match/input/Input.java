package forge.match.input;

import java.awt.event.MouseEvent;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;


public interface Input {
    void showMessageInitial();

    void selectCard(Card c, MouseEvent triggerEvent);
    
    void selectAbility(SpellAbility ab);

    void selectPlayer(Player player, MouseEvent triggerEvent);

    void selectButtonOK();

    void selectButtonCancel();
}