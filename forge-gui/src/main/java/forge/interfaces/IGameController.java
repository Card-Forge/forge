package forge.interfaces;

import java.util.List;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.gamemodes.match.NextGameDecision;
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

    boolean selectCard(CardView cardView, List<CardView> otherCardViewsToSelect, ITriggerEvent triggerEvent);

    void selectAbility(SpellAbilityView sa);

    void undoLastAction();

    IDevModeCheats cheat();

    IMacroSystem macros();

    void nextGameDecision(NextGameDecision decision);

    String getActivateDescription(CardView card);

    void reorderHand(CardView card, int index);

    // Delta sync and reconnection methods (client -> server)

    /**
     * Acknowledge receipt of a delta or full state packet.
     * @param sequenceNumber the sequence number being acknowledged
     */
    void ackSync(long sequenceNumber);

    /**
     * Request a full state resync from the server.
     * Called automatically when checksum validation fails to recover from desynchronization.
     */
    void requestResync();

}
