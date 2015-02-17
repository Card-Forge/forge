package forge.interfaces;

import java.util.List;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
import forge.match.NextGameDecision;
import forge.util.ITriggerEvent;

public interface IGameController {

    boolean mayLookAtAllCards();

    boolean canPlayUnlimitedLands();

    void concede();

    void alphaStrike();

    boolean useMana(byte color);

    void selectButtonOk();

    void selectButtonCancel();

    boolean passPriority();

    boolean passPriorityUntilEndOfTurn();

    void selectPlayer(PlayerView playerView, ITriggerEvent triggerEvent);

    boolean selectCard(CardView cardView,
            List<CardView> otherCardViewsToSelect, ITriggerEvent triggerEvent);

    void selectAbility(SpellAbility sa);

    boolean tryUndoLastAction();

    IDevModeCheats cheat();

    void nextGameDecision(NextGameDecision decision);

    String getActivateDescription(CardView card);
}
