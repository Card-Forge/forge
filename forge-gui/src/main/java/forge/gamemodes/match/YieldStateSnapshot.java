package forge.gamemodes.match;

import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.player.AutoYieldStore;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Atomic snapshot of a client's persistent yield state, transmitted at
 * game start (and on reconnection) so the host's PCH proxy is fully
 * seeded in one wire message instead of N.
 */
public record YieldStateSnapshot(
        Set<String> cardYields,
        Set<String> abilityYields,
        Map<Integer, AutoYieldStore.TriggerDecision> triggerDecisions,
        boolean autoYieldsDisabled,
        Map<PlayerView, EnumSet<PhaseType>> skipPhases
) implements Serializable {}
