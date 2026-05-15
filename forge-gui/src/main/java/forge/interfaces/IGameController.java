package forge.interfaces;

import java.util.List;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.match.YieldController;
import forge.gamemodes.match.YieldUpdate;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.player.AutoYieldStore.TriggerDecision;
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

    // Auto-yield preferences
    default boolean shouldAutoYield(String key) {
        return getYieldController().shouldAutoYield(key);
    }
    /**
     * @param isAbilityScope true if {@code key} is an ability suffix (Per Ability * modes);
     *   false if {@code key} is the full raw key (Per Card mode). Server-side handlers
     *   route storage by this flag instead of consulting the host's own UI_AUTO_DECISION_MODE.
     */
    void setShouldAutoYield(String key, boolean autoYield, boolean isAbilityScope);
    void setDisableAutoYields(boolean disable);

    // Trigger accept/decline preferences
    default TriggerDecision getTriggerDecision(String key) {
        return getYieldController().getTriggerDecision(key);
    }
    void setTriggerDecision(String key, TriggerDecision decision, boolean isAbilityScope);
    void setDisableAutoTriggers(boolean disable);

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

    /** Setter dispatches the per-PCH envelope; reads go via getYieldController(). Pref values are String-typed in {@link forge.localinstance.properties.PreferencesStore}, so callers wrap booleans with {@code String.valueOf}. */
    void setYieldPref(FPref pref, String value);
}
