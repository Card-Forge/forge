package forge.match.input;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.ITriggerEvent;


public interface Input {
    void showMessageInitial();

    boolean selectCard(Card c, ITriggerEvent triggerEvent);
    
    void selectAbility(SpellAbility ab);

    void selectPlayer(Player player, ITriggerEvent triggerEvent);

    void selectButtonOK();

    void selectButtonCancel();
}