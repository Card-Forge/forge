package forge.gamemodes.match;

import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.player.AutoYieldStore;

import java.io.Serializable;

/**
 * Unified envelope for all yield-related sync between client and host.
 *
 * Receiver dispatches via exhaustive switch in PlayerControllerHuman
 * (host-side) and NetworkGuiGame (client-side).
 */
public sealed interface YieldUpdate extends Serializable
        permits YieldUpdate.SetMarker,
                YieldUpdate.ClearMarker,
                YieldUpdate.StackYield,
                YieldUpdate.TriggerDecision,
                YieldUpdate.CardAutoYield,
                YieldUpdate.SkipPhase,
                YieldUpdate.SeedFromClient {

    /** {@code atOrPastAtClick}: priority was at-or-past target on owner's turn when the user clicked — computed by the UI so client cache and host PCH initialize identically. */
    record SetMarker(PlayerView phaseOwner, PhaseType phase, boolean atOrPastAtClick) implements YieldUpdate {}

    record ClearMarker(PlayerView player) implements YieldUpdate {}

    record StackYield(PlayerView player, boolean active) implements YieldUpdate {}

    record TriggerDecision(int trigId, AutoYieldStore.TriggerDecision decision) implements YieldUpdate {}

    record CardAutoYield(String cardKey, boolean active, boolean abilityScope) implements YieldUpdate {}

    record SkipPhase(PlayerView turnPlayer, PhaseType phase, boolean skip) implements YieldUpdate {}

    record SeedFromClient(YieldStateSnapshot snapshot) implements YieldUpdate {}
}
