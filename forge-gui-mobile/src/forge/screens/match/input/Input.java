package forge.screens.match.input;

import java.util.List;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public interface Input {

    // showMessage() is always the first method called
    void showMessageInitial();

    void selectCard(final Card card, final List<Card> orderedCardOptions);

    void selectAbility(final SpellAbility ab);

    void selectPlayer(final Player player);

    void selectButtonOK();

    void selectButtonCancel();

}