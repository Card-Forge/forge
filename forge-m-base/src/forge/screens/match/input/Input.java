package forge.screens.match.input;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public interface Input {

    // showMessage() is always the first method called
    void showMessageInitial();

    void selectCard(Card c);
    
    void selectAbility(SpellAbility ab);

    void selectPlayer(Player player);

    void selectButtonOK();

    void selectButtonCancel();

}