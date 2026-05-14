package forge.gamemodes.match;

import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.player.AutoYieldStore;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Atomic snapshot of a client's persistent yield + trigger state, transmitted at
 * game start (and on reconnection) so the host's PCH proxy is fully seeded in
 * one wire message instead of N.
 *
 * Trigger decisions split by scope (parallel to yields) so the host's remote
 * cache can match incoming game-time keys against either bucket.
 */
public record YieldStateSnapshot(
        Set<String> cardYields,
        Set<String> abilityYields,
        Map<String, AutoYieldStore.TriggerDecision> cardTriggerDecisions,
        Map<String, AutoYieldStore.TriggerDecision> abilityTriggerDecisions,
        boolean autoYieldsDisabled,
        boolean autoTriggersDisabled,
        Map<PlayerView, EnumSet<PhaseType>> skipPhases
) implements Serializable {}
