package forge.interfaces;

import java.util.List;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.match.YieldController;
import forge.gamemodes.match.YieldUpdate;
import forge.util.ITriggerEvent;

public interface IGameController {

    boolean mayLookAtAllCards();

    boolean canPlayUnlimitedLands();

    void concede();

    void alphaStrike();

    void useMana(byte color);

    void selectButtonOk();

    void selectButtonCancel();

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

    void passPriority();
    void passPriorityUntilEndOfTurn();

    // Auto-yield preferences
    boolean shouldAutoYield(String key);
    /**
     * @param isAbilityScope true if {@code key} is an ability suffix (Per Ability * modes);
     *   false if {@code key} is the full raw key (Per Card mode). Server-side handlers
     *   route storage by this flag instead of consulting the host's own UI_AUTO_YIELD_MODE.
     */
    void setShouldAutoYield(String key, boolean autoYield, boolean isAbilityScope);
    boolean getDisableAutoYields();
    void setDisableAutoYields(boolean disable);

    // Trigger accept/decline preferences
    boolean shouldAlwaysAcceptTrigger(int trigger);
    boolean shouldAlwaysDeclineTrigger(int trigger);
    void setShouldAlwaysAcceptTrigger(int trigger);
    void setShouldAlwaysDeclineTrigger(int trigger);
    void setShouldAlwaysAskTrigger(int trigger);

    /** Apply a unified yield update envelope to this controller's YieldController. */
    void applyYieldUpdate(YieldUpdate update);

    /**
     * Wire entry for client->host yield update; receivers route to applyYieldUpdate.
     * Named to match the {@code sendYieldUpdate} ProtocolMethod (lookup is by name).
     */
    default void sendYieldUpdate(YieldUpdate update) {
        applyYieldUpdate(update);
    }

    YieldController getYieldController();
}
