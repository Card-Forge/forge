package forge.gamemodes.match;

import forge.game.GameView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.AutoYieldStore;
import forge.player.LobbyPlayerHuman;
import forge.player.PersistentYieldStore;
import forge.player.PlayerControllerHuman;

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
 * <p>Auto-yield state and trigger decisions live in a single {@link AutoYieldStore}
 * resolved by {@link #activeStore()}:
 * <ul>
 *   <li>Host PCH for a local player → the {@link LobbyPlayerHuman}'s persistent store
 *       (tier-aware via FPref).</li>
 *   <li>Host PCH for a remote player → a per-game {@link #localStore} cache populated
 *       by client wire messages.</li>
 *   <li>{@link forge.gamemodes.net.client.NetGameController}'s controller (owner == null)
 *       → its session-lifetime {@link #localStore}, tier-aware via the local user's FPref.</li>
 * </ul>
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

    /**
     * Backing store used in cache mode (host PCH for remote, NetGameController on
     * client). For host PCH for a local player, {@link #activeStore()} returns the
     * LobbyPlayer's persistent store and this field is unused.
     */
    private final AutoYieldStore localStore = new AutoYieldStore();

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

    public void setMarker(PlayerView phaseOwner, PhaseType phase) {
        autoPassUntilEndOfTurn = false;
        if (phaseOwner == null || phase == null) {
            clearMarker();
            return;
        }
        marker = new YieldMarker(phaseOwner, phase);
        // Activating at-or-past target on the owner's current turn must wait for next turn's
        // occurrence; otherwise pastTarget would fire and clear the marker on the same turn.
        boolean atOrPast = isPriorityAtOrPastMarker(marker);
        hasLeftMarker = !atOrPast;
        activationOnMarker = atOrPast;
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

    private boolean isPriorityAtOrPastMarker(YieldMarker m) {
        if (m == null || owner == null || owner.getGui() == null) return false;
        GameView gv = owner.getGui().getGameView();
        if (gv == null) return false;
        PlayerView turnPlayer = gv.getPlayerTurn();
        PhaseType phase = gv.getPhase();
        if (turnPlayer == null || !turnPlayer.equals(m.getPhaseOwner())) return false;
        if (phase == null || m.getPhase() == null) return false;
        return phase == m.getPhase() || phase.isAfter(m.getPhase());
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

    // ---- Auto-yield (per-card/ability) and trigger decisions ----

    /**
     * Cache mode (host PCH for remote, or NetGameController) → {@link #localStore}.
     * Tier-aware mode (host PCH for local player) → LobbyPlayer's persistent store.
     */
    private AutoYieldStore activeStore() {
        if (owner != null && !owner.isRemoteClient()) {
            return ((LobbyPlayerHuman) owner.getLobbyPlayer()).getYieldStore();
        }
        return localStore;
    }

    /** True if FPref tier/install logic applies (local user context). False for the host's remote-cache mode. */
    private boolean tierAware() {
        return owner == null || !owner.isRemoteClient();
    }

    private static boolean activeModeIsInstall() {
        return ForgeConstants.AUTO_YIELD_PER_ABILITY_INSTALL.equals(
                FModel.getPreferences().getPref(FPref.UI_AUTO_YIELD_MODE));
    }

    private static AutoYieldStore.Tier activeTier() {
        String mode = FModel.getPreferences().getPref(FPref.UI_AUTO_YIELD_MODE);
        if (ForgeConstants.AUTO_YIELD_PER_CARD.equals(mode))            return AutoYieldStore.Tier.GAME;
        if (ForgeConstants.AUTO_YIELD_PER_ABILITY_SESSION.equals(mode)) return AutoYieldStore.Tier.SESSION;
        return AutoYieldStore.Tier.MATCH;
    }

    public boolean shouldAutoYield(String key) {
        AutoYieldStore store = activeStore();
        if (store.isDisabled()) return false;
        if (!tierAware()) {
            // Cache: keys stored at storageKey shape (full or stripped). Check both.
            return store.shouldYield(AutoYieldStore.Tier.GAME, key)
                || store.shouldYield(AutoYieldStore.Tier.GAME, AutoYieldStore.abilitySuffix(key));
        }
        if (activeModeIsInstall()) {
            return PersistentYieldStore.get().contains(AutoYieldStore.abilitySuffix(key));
        }
        AutoYieldStore.Tier tier = activeTier();
        boolean abilityScope = tier != AutoYieldStore.Tier.GAME;
        String storageKey = abilityScope ? AutoYieldStore.abilitySuffix(key) : key;
        return store.shouldYield(tier, storageKey);
    }

    /** Tier-aware user-initiated set. Returns the storage key (stripped if ability-scope) for wire propagation. */
    public String setShouldAutoYield(String key, boolean autoYield, boolean abilityScope) {
        String storageKey = abilityScope ? AutoYieldStore.abilitySuffix(key) : key;
        if (activeModeIsInstall()) {
            PersistentYieldStore.get().setYield(storageKey, autoYield);
        } else {
            activeStore().setYield(activeTier(), storageKey, autoYield);
        }
        return storageKey;
    }

    /** Cache-mode write of a wire-received update. Storage key is already at the right shape. */
    public void applyAutoYieldFromWire(String storageKey, boolean active) {
        activeStore().setYield(AutoYieldStore.Tier.GAME, storageKey, active);
    }

    public Iterable<String> getAutoYields() {
        AutoYieldStore store = activeStore();
        if (!tierAware()) return store.getYields(AutoYieldStore.Tier.GAME);
        if (activeModeIsInstall()) return PersistentYieldStore.get().getYields();
        return store.getYields(activeTier());
    }

    public void clearAutoYields() {
        if (!tierAware()) {
            localStore.clear();
            return;
        }
        boolean matchOver = owner.getGame() == null || owner.getGame().getView().isMatchOver();
        activeStore().onGameEnd(matchOver);
    }

    public boolean getDisableAutoYields() {
        return activeStore().isDisabled();
    }

    public void setDisableAutoYields(boolean disable) {
        activeStore().setDisabled(disable);
    }

    public boolean shouldAlwaysAcceptTrigger(int trigger) {
        return activeStore().getTriggerDecision(trigger) == AutoYieldStore.TriggerDecision.ACCEPT;
    }

    public boolean shouldAlwaysDeclineTrigger(int trigger) {
        return activeStore().getTriggerDecision(trigger) == AutoYieldStore.TriggerDecision.DECLINE;
    }

    public void setAlwaysAcceptTrigger(int trigger) {
        activeStore().setTriggerDecision(trigger, AutoYieldStore.TriggerDecision.ACCEPT);
    }

    public void setAlwaysDeclineTrigger(int trigger) {
        activeStore().setTriggerDecision(trigger, AutoYieldStore.TriggerDecision.DECLINE);
    }

    public void setAlwaysAskTrigger(int trigger) {
        activeStore().setTriggerDecision(trigger, AutoYieldStore.TriggerDecision.ASK);
    }

    public void setTriggerDecision(int trigger, AutoYieldStore.TriggerDecision decision) {
        activeStore().setTriggerDecision(trigger, decision);
    }

    /** Build the seed payload from this controller's authoritative store. */
    public YieldStateSnapshot buildClientSnapshot(Map<PlayerView, EnumSet<PhaseType>> skipPhases) {
        Set<String> cardYields = new HashSet<>();
        Set<String> abilityYields = new HashSet<>();
        boolean abilityScope = activeModeIsInstall() || activeTier() != AutoYieldStore.Tier.GAME;
        for (String key : getAutoYields()) {
            if (abilityScope) abilityYields.add(key);
            else cardYields.add(key);
        }
        // Trigger decisions are per-game; deltas flow during play.
        Map<Integer, AutoYieldStore.TriggerDecision> triggers = new HashMap<>();
        return new YieldStateSnapshot(cardYields, abilityYields, triggers, getDisableAutoYields(),
                skipPhases == null ? new HashMap<>() : skipPhases);
    }

    /** Atomic seed of client-persistent state at game start or reconnection. Cache mode only. */
    public void applyClientSeed(YieldStateSnapshot snap) {
        localStore.clear();
        for (String k : snap.cardYields()) localStore.setYield(AutoYieldStore.Tier.GAME, k, true);
        for (String k : snap.abilityYields()) localStore.setYield(AutoYieldStore.Tier.GAME, k, true);
        for (Map.Entry<Integer, AutoYieldStore.TriggerDecision> e : snap.triggerDecisions().entrySet()) {
            localStore.setTriggerDecision(e.getKey(), e.getValue());
        }
        localStore.setDisabled(snap.autoYieldsDisabled());
        skipPhases.clear();
        for (Map.Entry<PlayerView, EnumSet<PhaseType>> e : snap.skipPhases().entrySet()) {
            skipPhases.put(e.getKey(), EnumSet.copyOf(e.getValue()));
        }
    }
}
