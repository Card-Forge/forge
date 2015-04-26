package forge.interfaces;

import java.util.List;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.match.NextGameDecision;
import forge.util.ITriggerEvent;

public interface IGameController {

    boolean mayLookAtAllCards();

    boolean canPlayUnlimitedLands();

    void concede();

    void alphaStrike();

    void useMana(byte color);

    void selectButtonOk();

    void selectButtonCancel();

    void passPriority();

    void passPriorityUntilEndOfTurn();

    void selectPlayer(PlayerView playerView, ITriggerEvent triggerEvent);

    boolean selectCard(CardView cardView,
            List<CardView> otherCardViewsToSelect, ITriggerEvent triggerEvent);

    void selectAbility(SpellAbilityView sa);

    void undoLastAction();

    IDevModeCheats cheat();

    void nextGameDecision(NextGameDecision decision);

    String getActivateDescription(CardView card);

    void reorderHand(CardView card, int index);
}
