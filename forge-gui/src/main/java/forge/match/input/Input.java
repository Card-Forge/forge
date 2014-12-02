package forge.match.input;

import java.util.List;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.util.ITriggerEvent;

public interface Input {
    PlayerView getOwner();

    void showMessageInitial();

    boolean selectCard(Card card, final List<Card> otherCardsToSelect, ITriggerEvent triggerEvent);

    String getActivateAction(Card card);

    boolean selectAbility(SpellAbility ab);

    void selectPlayer(Player player, ITriggerEvent triggerEvent);

    void selectButtonOK();

    void selectButtonCancel();
}