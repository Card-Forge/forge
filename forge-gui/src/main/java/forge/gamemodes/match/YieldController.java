package forge.gamemodes.match;

import forge.game.GameView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.player.AutoYieldStore;
import forge.player.PlayerControllerHuman;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-PlayerControllerHuman yield state holder. Owns markers, stack-yield,
 * autopass-until-end-of-turn, per-card/ability auto-yield, trigger
 * decisions, and skip-phase prefs.
 *
 * Host-side instances are authoritative (full Game access). Client-side
 * instances on NetGameController are caches populated by SERVER-mode wire
 * messages.
 */
public class YieldController {

    private final PlayerControllerHuman owner;

    private boolean autoPassUntilEndOfTurn;

    private YieldMarker marker;
    /** Priority has passed through any non-target phase since marker activation. */
    private boolean hasLeftMarker;
    /** Marker was set while priority was already at its target; require a full cycle to fire. */
    private boolean activationOnMarker;

    /** Survives opponent spells; auto-clears only when stack empties, NOT on cancelYield. */
    private boolean stackYield;

    private final Set<String> cardYields = new HashSet<>();
    private final Set<String> abilityYields = new HashSet<>();
    private boolean autoYieldsDisabled;

    private final Map<Integer, AutoYieldStore.TriggerDecision> triggerDecisions = new HashMap<>();

    private final Map<PlayerView, EnumSet<PhaseType>> skipPhases = new HashMap<>();

    public YieldController(PlayerControllerHuman owner) {
        this.owner = owner;
    }

    public boolean isAutoPassUntilEndOfTurn() {
        return autoPassUntilEndOfTurn;
    }

    public void setAutoPassUntilEndOfTurn(boolean active) {
        this.autoPassUntilEndOfTurn = active;
    }

    public boolean shouldAutoYield() {
        if (autoPassUntilEndOfTurn) return true;

        GameView gv = owner != null && owner.getGui() != null ? owner.getGui().getGameView() : null;

        if (stackYield) {
            if (gv != null && gv.getStack() != null && !gv.getStack().isEmpty()) {
                return true;
            }
            stackYield = false;
        }

        if (marker == null || gv == null) return false;

        PlayerView turnPlayer = gv.getPlayerTurn();
        PhaseType currentPhase = gv.getPhase();

        boolean inMarkerOwnerTurn = turnPlayer != null && turnPlayer.equals(marker.getPhaseOwner());
        boolean atTarget = inMarkerOwnerTurn && currentPhase == marker.getPhase();
        boolean pastTarget = inMarkerOwnerTurn && currentPhase != null
                && marker.getPhase() != null && currentPhase.isAfter(marker.getPhase());

        boolean shouldFire = hasLeftMarker
                && (atTarget || (!activationOnMarker && pastTarget));

        if (shouldFire) {
            clearMarker();
            notifyMarkerCleared();
            return false;
        }
        if (!atTarget && !hasLeftMarker) {
            hasLeftMarker = true;
        }
        return true;
    }

    private void notifyMarkerCleared() {
        if (owner == null || owner.getGui() == null) return;
        PlayerView player = owner.getLocalPlayerView();
        if (player == null) return;
        owner.getGui().applyYieldUpdate(new YieldUpdate.ClearMarker(player));
    }

    /**
     * Engine-driven cancel matching master's autoPassCancel semantics: clears
     * legacy autopass-until-end-of-turn only. Markers and stack-yield survive —
     * markers can target phases on future turns (must outlive turn-boundary
     * cancellation), and stack-yield must resolve the entire stack including
     * post-cancel additions. User-initiated ESC clears all three explicitly.
     */
    public void cancelYield() {
        autoPassUntilEndOfTurn = false;
    }

    public void setCardAutoYield(String key, boolean active, boolean abilityScope) {
        Set<String> bucket = abilityScope ? abilityYields : cardYields;
        if (active) bucket.add(key);
        else bucket.remove(key);
    }

    public boolean shouldAutoYieldKey(String key, boolean abilityScope) {
        if (autoYieldsDisabled) return false;
        Set<String> bucket = abilityScope ? abilityYields : cardYields;
        return bucket.contains(key);
    }

    public Set<String> getCardYields() {
        return Collections.unmodifiableSet(cardYields);
    }

    public Set<String> getAbilityYields() {
        return Collections.unmodifiableSet(abilityYields);
    }

    public void clearAutoYields() {
        cardYields.clear();
        abilityYields.clear();
        triggerDecisions.clear();
    }

    public boolean isAutoYieldsDisabled() {
        return autoYieldsDisabled;
    }

    public void setAutoYieldsDisabled(boolean disabled) {
        this.autoYieldsDisabled = disabled;
    }

    public void setTriggerDecision(int trigId, AutoYieldStore.TriggerDecision decision) {
        if (decision == null || decision == AutoYieldStore.TriggerDecision.ASK) {
            triggerDecisions.remove(trigId);
        } else {
            triggerDecisions.put(trigId, decision);
        }
    }

    public AutoYieldStore.TriggerDecision getTriggerDecision(int trigId) {
        return triggerDecisions.getOrDefault(trigId, AutoYieldStore.TriggerDecision.ASK);
    }

    public Map<Integer, AutoYieldStore.TriggerDecision> getTriggerDecisions() {
        return Collections.unmodifiableMap(triggerDecisions);
    }

    public void setSkipPhase(PlayerView turnPlayer, PhaseType phase, boolean skip) {
        if (turnPlayer == null || phase == null) return;
        EnumSet<PhaseType> set = skipPhases.computeIfAbsent(turnPlayer, k -> EnumSet.noneOf(PhaseType.class));
        if (skip) set.add(phase);
        else set.remove(phase);
    }

    public boolean isSkippingPhase(PlayerView turnPlayer, PhaseType phase) {
        EnumSet<PhaseType> set = skipPhases.get(turnPlayer);
        return set != null && set.contains(phase);
    }

    public Map<PlayerView, EnumSet<PhaseType>> getSkipPhases() {
        return Collections.unmodifiableMap(skipPhases);
    }

    public void setMarker(PlayerView phaseOwner, PhaseType phase) {
        autoPassUntilEndOfTurn = false;
        if (phaseOwner == null || phase == null) {
            clearMarker();
            return;
        }
        marker = new YieldMarker(phaseOwner, phase);
        boolean atMarkerNow = isPriorityAt(marker);
        hasLeftMarker = !atMarkerNow;
        activationOnMarker = atMarkerNow;
    }

    public void clearMarker() {
        marker = null;
        hasLeftMarker = false;
        activationOnMarker = false;
    }

    public YieldMarker getMarker() { return marker; }

    public void setStackYield(boolean active) {
        if (active) autoPassUntilEndOfTurn = false;
        this.stackYield = active;
    }

    public boolean isStackYieldActive() { return stackYield; }

    private boolean isPriorityAt(YieldMarker m) {
        if (m == null || owner == null || owner.getGui() == null) return false;
        GameView gv = owner.getGui().getGameView();
        if (gv == null) return false;
        PlayerView turnPlayer = gv.getPlayerTurn();
        PhaseType phase = gv.getPhase();
        return turnPlayer != null
                && turnPlayer.equals(m.getPhaseOwner())
                && phase == m.getPhase();
    }

    /** Atomic seed of client-persistent state at game start or reconnection. */
    public void applyClientSeed(YieldStateSnapshot snap) {
        cardYields.clear();
        cardYields.addAll(snap.cardYields());
        abilityYields.clear();
        abilityYields.addAll(snap.abilityYields());
        triggerDecisions.clear();
        triggerDecisions.putAll(snap.triggerDecisions());
        autoYieldsDisabled = snap.autoYieldsDisabled();
        skipPhases.clear();
        for (Map.Entry<PlayerView, EnumSet<PhaseType>> e : snap.skipPhases().entrySet()) {
            skipPhases.put(e.getKey(), EnumSet.copyOf(e.getValue()));
        }
    }
}
