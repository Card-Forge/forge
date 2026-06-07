package forge.gamemodes.match;

import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.player.AutoYieldStore;

import java.io.Serializable;

/**
 * Unified envelope for yield, trigger, and per-PCH controller-scoped state sync between client and host.
 *
 * Receiver dispatches via exhaustive switch in PlayerControllerHuman
 * (host-side) and NetworkGuiGame (client-side).
 */
public sealed interface YieldUpdate extends Serializable
        permits YieldUpdate.SetMarker,
                YieldUpdate.ClearMarker,
                YieldUpdate.StackYield,
                YieldUpdate.SetAutoPassUntilEndOfTurn,
                YieldUpdate.CardAutoYield,
                YieldUpdate.TriggerDecision,
                YieldUpdate.SetDisableYields,
                YieldUpdate.SetDisableTriggers,
                YieldUpdate.SkipPhase,
                YieldUpdate.SetYieldPref,
                YieldUpdate.SeedFromClient,
                YieldUpdate.ClearAbilityOrders {

    /** {@code atOrPastAtClick}: priority was at-or-past target on owner's turn when the user clicked — computed by the UI so client cache and host PCH initialize identically. */
    record SetMarker(PlayerView phaseOwner, PhaseType phase, boolean atOrPastAtClick) implements YieldUpdate {}

    record ClearMarker(PlayerView player) implements YieldUpdate {}

    record StackYield(PlayerView player, boolean active, boolean respectsInterrupts) implements YieldUpdate {}

    record SetAutoPassUntilEndOfTurn(PlayerView player, boolean active) implements YieldUpdate {}

    record CardAutoYield(String cardKey, boolean active, boolean abilityScope) implements YieldUpdate {}

    /** Param order mirrors {@link CardAutoYield} (key, value, scope). */
    record TriggerDecision(String storageKey, AutoYieldStore.TriggerDecision decision, boolean abilityScope) implements YieldUpdate {}

    /** Runtime toggle of the global auto-yield disable flag — host applies to its remote-cache, client to its local controller. */
    record SetDisableYields(boolean disabled) implements YieldUpdate {}

    /** Runtime toggle of the global auto-trigger disable flag — host applies to its remote-cache, client to its local controller. */
    record SetDisableTriggers(boolean disabled) implements YieldUpdate {}

    record SkipPhase(PlayerView turnPlayer, PhaseType phase, boolean skip) implements YieldUpdate {}

    /** Pref values are stored String-typed in {@link forge.localinstance.properties.PreferencesStore}; callers wrap booleans with {@code String.valueOf} at the call site. */
    record SetYieldPref(FPref pref, String value) implements YieldUpdate {}

    record SeedFromClient(YieldStateSnapshot snapshot) implements YieldUpdate {}

    /** Clears the receiving PCH's saved simultaneous-ability state (both the order map and the remembered-keys set). */
    record ClearAbilityOrders() implements YieldUpdate {}
}
