package forge.interfaces;

import java.util.List;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.match.TriggerChoice;
import forge.gamemodes.match.YieldMode;
import forge.gamemodes.match.YieldPrefs;
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

    /**
     * Request a full state resync from the server.
     * Called automatically when checksum validation fails to recover from desynchronization.
     */
    void requestResync();

    /**
     * Notify the server that the client's yield mode has changed.
     * Used for network play to sync yield state from client to server.
     * Default implementation does nothing (for local/host games).
     */
    default void notifyYieldStateChanged(PlayerView player, YieldMode mode, YieldPrefs prefs) {
        // Default: no-op for local games
    }

    /** Notify server that auto-yield was toggled for an ability key. */
    default void notifyAutoYieldChanged(String key, boolean autoYield) { }

    /**
     * Notify server that a trigger accept/decline preference changed.
     */
    default void notifyTriggerChoiceChanged(int triggerId, TriggerChoice choice) { }
}
