package forge.interfaces;

import java.util.Collections;
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

    /**
     * Request a full state resync from the server.
     * Called automatically when checksum validation fails to recover from desynchronization.
     */
    void requestResync();

    // --- Auto-yield preferences (per-player) ---

    default boolean shouldAutoYield(String key) { return false; }
    default void setShouldAutoYield(String key, boolean autoYield) { }
    default Iterable<String> getAutoYields() { return Collections.emptyList(); }
    default void clearAutoYields() { }

    default boolean getDisableAutoYields() { return false; }
    default void setDisableAutoYields(boolean disable) { }

    // --- Trigger accept/decline preferences (per-player) ---

    default boolean shouldAlwaysAcceptTrigger(int trigger) { return false; }
    default boolean shouldAlwaysDeclineTrigger(int trigger) { return false; }
    default void setShouldAlwaysAcceptTrigger(int trigger) { }
    default void setShouldAlwaysDeclineTrigger(int trigger) { }
    default void setShouldAlwaysAskTrigger(int trigger) { }
}
