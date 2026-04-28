package forge.gamemodes.match;

import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.player.AutoYieldStore;

import java.io.Serializable;

/**
 * Unified envelope for all yield-related sync between client and host.
 * One sealed type with seven cases replaces eight per-method ProtocolMethod
 * entries. Both directions (CLIENT->HOST, HOST->CLIENT) ride this envelope.
 *
 * Receiver dispatches via exhaustive switch in PlayerControllerHuman
 * (host-side) and NetworkGuiGame (client-side).
 */
public sealed interface YieldUpdate extends Serializable
        permits YieldUpdate.SetMarker,
                YieldUpdate.ClearMarker,
                YieldUpdate.SetStackYield,
                YieldUpdate.SetTriggerDecision,
                YieldUpdate.SetCardAutoYield,
                YieldUpdate.SetSkipPhase,
                YieldUpdate.SeedFromClient {

    record SetMarker(PlayerView phaseOwner, PhaseType phase) implements YieldUpdate {}

    record ClearMarker(PlayerView player) implements YieldUpdate {}

    record SetStackYield(PlayerView player, boolean active) implements YieldUpdate {}

    record SetTriggerDecision(int trigId, AutoYieldStore.TriggerDecision decision) implements YieldUpdate {}

    record SetCardAutoYield(String cardKey, boolean active, boolean abilityScope) implements YieldUpdate {}

    record SetSkipPhase(PlayerView turnPlayer, PhaseType phase, boolean skip) implements YieldUpdate {}

    /** Atomic snapshot of client's persistent yield state. Sent by client at game start and reconnection only - not host->client. */
    record SeedFromClient(YieldStateSnapshot snapshot) implements YieldUpdate {}
}
