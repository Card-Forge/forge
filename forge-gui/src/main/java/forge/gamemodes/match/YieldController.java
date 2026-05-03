package forge.gamemodes.match;

import forge.game.GameView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.AutoYieldStore;
import forge.player.LobbyPlayerHuman;
import forge.player.PersistentAutoDecisionStore;
import forge.player.PlayerControllerHuman;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
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

    private boolean autoPassUntilEOT;
    private boolean autoPassUntilStackEmpty;
    private YieldMarker autoPassUntilMarker;

    /** Priority has passed through any non-target phase since marker activation. */
    private boolean hasLeftMarker;
    /** Marker was set while priority was already at its target; require a full cycle to fire. */
    private boolean activationOnMarker;

    private final AutoYieldStore localStore = new AutoYieldStore();
    private final Map<PlayerView, EnumSet<PhaseType>> skipPhases = new HashMap<>();

    public YieldController(PlayerControllerHuman owner) {
        this.owner = owner;
    }

    public boolean isSkippingPhase(PlayerView turnPlayer, PhaseType phase) {
        EnumSet<PhaseType> set = skipPhases.get(turnPlayer);
        return set != null && set.contains(phase);
    }
    public void setSkipPhase(PlayerView turnPlayer, PhaseType phase, boolean skip) {
        if (turnPlayer == null || phase == null) return;
        EnumSet<PhaseType> set = skipPhases.computeIfAbsent(turnPlayer, k -> EnumSet.noneOf(PhaseType.class));
        if (skip) set.add(phase);
        else set.remove(phase);
    }

    public boolean autoPassUntilStackEmpty() {
        return autoPassUntilStackEmpty;
    }
    public boolean autoPassUntilEndOfTurn() {
        return autoPassUntilEOT;
    }
    public YieldMarker getAutoPassUntilMarker() {
        return autoPassUntilMarker;
    }

    public void setAutoPassUntilStackEmpty(boolean active) {
        if (active) autoPassUntilEOT = false;
        this.autoPassUntilStackEmpty = active;
    }
    public void setAutoPassUntilEOTWithoutInterruptions(boolean active) {
        this.autoPassUntilEOT = active;
    }

    // setMarker/clearMarker are mutated from EDT (right-click), Netty (wire receive), and game thread.
    public synchronized void setMarker(PlayerView phaseOwner, PhaseType phase, boolean atOrPastAtClick) {
        autoPassUntilEOT = false;
        if (phaseOwner == null || phase == null) {
            clearMarker();
            return;
        }
        autoPassUntilMarker = new YieldMarker(phaseOwner, phase);
        // Activating at-or-past target on the owner's current turn must wait for next turn's
        // occurrence; otherwise pastTarget would fire and clear the marker on the same turn.
        hasLeftMarker = !atOrPastAtClick;
        activationOnMarker = atOrPastAtClick;
    }
    public synchronized void clearMarker() {
        autoPassUntilMarker = null;
        hasLeftMarker = false;
        activationOnMarker = false;
    }

    /** Click-site helper: true when priority is at or past {@code phase} on {@code phaseOwner}'s current turn. */
    public static boolean isPriorityAtOrPastMarker(GameView gv, PlayerView phaseOwner, PhaseType phase) {
        PhaseType currentPhase = gv.getPhase();
        if (!phaseOwner.equals(gv.getPlayerTurn())) return false;
        if (currentPhase == null) return false;
        return currentPhase == phase || currentPhase.isAfter(phase);
    }

    public boolean shouldAutoYield() {
        if (autoPassUntilEOT) return true;
        GameView gv = owner != null && owner.getGui() != null ? owner.getGui().getGameView() : null;
        if (autoPassUntilStackEmpty) {
            if (gv != null && gv.peekStack() != null) return true;
            autoPassUntilStackEmpty = false;
        }
        if (autoPassUntilMarker != null && gv != null) {
            PlayerView turnPlayer = gv.getPlayerTurn();
            PhaseType currentPhase = gv.getPhase();
            boolean inMarkerOwnerTurn = autoPassUntilMarker.getPhaseOwner().equals(turnPlayer);
            boolean atTarget = inMarkerOwnerTurn && currentPhase == autoPassUntilMarker.getPhase();
            boolean pastTarget = inMarkerOwnerTurn && currentPhase != null && currentPhase.isAfter(autoPassUntilMarker.getPhase());
            if (hasLeftMarker && (atTarget || (!activationOnMarker && pastTarget))) {
                clearMarker();
                PlayerView local = owner.getLocalPlayerView();
                if (local != null) owner.getGui().applyYieldUpdate(new YieldUpdate.ClearMarker(local));
            }
            if (!atTarget && !hasLeftMarker) hasLeftMarker = true;
        }
        return autoPassUntilMarker != null;
    }

    // ---- Auto-yield (per-card/ability) and trigger decisions ----

    public boolean shouldAutoYield(String key) {
        AutoYieldStore store = activeStore();
        if (store.isDisabled()) return false;
        if (!tierAware()) {
            // Cache: keys stored at storageKey shape (full or stripped)
            return store.shouldYield(AutoYieldStore.Tier.GAME, key)
                    || store.shouldYield(AutoYieldStore.Tier.GAME, AutoYieldStore.abilitySuffix(key));
        }
        if (activeModeIsInstall()) {
            return PersistentAutoDecisionStore.get().contains(AutoYieldStore.abilitySuffix(key));
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
            PersistentAutoDecisionStore.get().setYield(storageKey, autoYield);
        } else {
            activeStore().setYield(activeTier(), storageKey, autoYield);
        }
        return storageKey;
    }

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
        return ForgeConstants.AUTO_YIELD_PER_ABILITY_INSTALL.equals(FModel.getPreferences().getPref(FPref.UI_AUTO_YIELD_MODE));
    }

    private static AutoYieldStore.Tier activeTier() {
        String mode = FModel.getPreferences().getPref(FPref.UI_AUTO_YIELD_MODE);
        if (ForgeConstants.AUTO_YIELD_PER_CARD.equals(mode))            return AutoYieldStore.Tier.GAME;
        if (ForgeConstants.AUTO_YIELD_PER_ABILITY_SESSION.equals(mode)) return AutoYieldStore.Tier.SESSION;
        return AutoYieldStore.Tier.MATCH;
    }

    /** Cache-mode write of a wire-received update. Storage key is already at the right shape. */
    public void applyAutoYieldFromWire(String storageKey, boolean active) {
        activeStore().setYield(AutoYieldStore.Tier.GAME, storageKey, active);
    }

    public Iterable<String> getAutoYields() {
        AutoYieldStore store = activeStore();
        if (!tierAware()) return store.getYields(AutoYieldStore.Tier.GAME);
        if (activeModeIsInstall()) return PersistentAutoDecisionStore.get().getYields();
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

    public boolean shouldAlwaysAcceptTrigger(String key) {
        return readTriggerDecision(key) == AutoYieldStore.TriggerDecision.ACCEPT;
    }
    public boolean shouldAlwaysDeclineTrigger(String key) {
        return readTriggerDecision(key) == AutoYieldStore.TriggerDecision.DECLINE;
    }

    /** Tier-aware user-initiated set. Returns the storage key (stripped if ability-scope) for wire propagation. */
    public String setAlwaysAcceptTrigger(String key, boolean abilityScope) {
        return writeTriggerDecision(key, abilityScope, AutoYieldStore.TriggerDecision.ACCEPT);
    }
    public String setAlwaysDeclineTrigger(String key, boolean abilityScope) {
        return writeTriggerDecision(key, abilityScope, AutoYieldStore.TriggerDecision.DECLINE);
    }
    public String setAlwaysAskTrigger(String key, boolean abilityScope) {
        return writeTriggerDecision(key, abilityScope, AutoYieldStore.TriggerDecision.ASK);
    }

    /** Cache-mode write of a wire-received trigger decision. Storage key is already at the right shape. */
    public void applyTriggerDecisionFromWire(String storageKey, AutoYieldStore.TriggerDecision decision) {
        activeStore().setTriggerDecision(AutoYieldStore.Tier.GAME, storageKey, decision);
    }

    public Iterable<java.util.Map.Entry<String, AutoYieldStore.TriggerDecision>> getAutoTriggers() {
        if (!tierAware()) return activeStore().getAutoTriggers(AutoYieldStore.Tier.GAME);
        if (activeTriggerModeIsInstall()) return PersistentAutoDecisionStore.get().getAutoTriggers();
        return activeStore().getAutoTriggers(activeTriggerTier());
    }

    public boolean getDisableAutoTriggers() { return activeStore().isTriggerDecisionsDisabled(); }
    public void setDisableAutoTriggers(boolean disable) { activeStore().setTriggerDecisionsDisabled(disable); }

    private AutoYieldStore.TriggerDecision readTriggerDecision(String key) {
        if (key == null || key.isEmpty()) return AutoYieldStore.TriggerDecision.ASK;
        AutoYieldStore store = activeStore();
        if (store.isTriggerDecisionsDisabled()) return AutoYieldStore.TriggerDecision.ASK;
        if (!tierAware()) {
            // Cache mode: keys stored at storageKey shape (full or stripped).
            AutoYieldStore.TriggerDecision d = store.getTriggerDecision(AutoYieldStore.Tier.GAME, key);
            if (d != AutoYieldStore.TriggerDecision.ASK) return d;
            return store.getTriggerDecision(AutoYieldStore.Tier.GAME, AutoYieldStore.abilitySuffix(key));
        }
        if (activeTriggerModeIsInstall()) {
            return PersistentAutoDecisionStore.get().getTriggerDecision(AutoYieldStore.abilitySuffix(key));
        }
        AutoYieldStore.Tier tier = activeTriggerTier();
        boolean abilityScope = tier != AutoYieldStore.Tier.GAME;
        String storageKey = abilityScope ? AutoYieldStore.abilitySuffix(key) : key;
        return store.getTriggerDecision(tier, storageKey);
    }

    private String writeTriggerDecision(String key, boolean abilityScope, AutoYieldStore.TriggerDecision decision) {
        String storageKey = abilityScope ? AutoYieldStore.abilitySuffix(key) : key;
        if (activeTriggerModeIsInstall()) {
            PersistentAutoDecisionStore.get().setTriggerDecision(storageKey, decision);
        } else {
            activeStore().setTriggerDecision(activeTriggerTier(), storageKey, decision);
        }
        return storageKey;
    }

    private static boolean activeTriggerModeIsInstall() {
        return ForgeConstants.AUTO_TRIGGER_PER_ABILITY_INSTALL.equals(FModel.getPreferences().getPref(FPref.UI_AUTO_TRIGGER_MODE));
    }

    private static AutoYieldStore.Tier activeTriggerTier() {
        String mode = FModel.getPreferences().getPref(FPref.UI_AUTO_TRIGGER_MODE);
        if (ForgeConstants.AUTO_TRIGGER_PER_CARD.equals(mode))            return AutoYieldStore.Tier.GAME;
        if (ForgeConstants.AUTO_TRIGGER_PER_ABILITY_SESSION.equals(mode)) return AutoYieldStore.Tier.SESSION;
        return AutoYieldStore.Tier.MATCH;
    }

    /** Build the seed payload from this controller's authoritative store. */
    public YieldStateSnapshot buildClientSnapshot(Map<PlayerView, EnumSet<PhaseType>> skipPhases) {
        Set<String> cardYields = new HashSet<>();
        Set<String> abilityYields = new HashSet<>();
        boolean yieldAbilityScope = activeModeIsInstall() || activeTier() != AutoYieldStore.Tier.GAME;
        for (String key : getAutoYields()) {
            if (yieldAbilityScope) abilityYields.add(key);
            else cardYields.add(key);
        }

        Map<String, AutoYieldStore.TriggerDecision> cardTriggers = new HashMap<>();
        Map<String, AutoYieldStore.TriggerDecision> abilityTriggers = new HashMap<>();
        boolean trigAbilityScope = activeTriggerModeIsInstall() || activeTriggerTier() != AutoYieldStore.Tier.GAME;
        for (Map.Entry<String, AutoYieldStore.TriggerDecision> e : getAutoTriggers()) {
            if (trigAbilityScope) abilityTriggers.put(e.getKey(), e.getValue());
            else cardTriggers.put(e.getKey(), e.getValue());
        }

        return new YieldStateSnapshot(
                cardYields, abilityYields,
                cardTriggers, abilityTriggers,
                getDisableAutoYields(), getDisableAutoTriggers(),
                skipPhases);
    }

    /** Atomic seed of client-persistent state at game start or reconnection. Cache mode only. */
    public void applyClientSeed(YieldStateSnapshot snap) {
        localStore.clear();
        for (String k : snap.cardYields()) localStore.setYield(AutoYieldStore.Tier.GAME, k, true);
        for (String k : snap.abilityYields()) localStore.setYield(AutoYieldStore.Tier.GAME, k, true);
        for (Map.Entry<String, AutoYieldStore.TriggerDecision> e : snap.cardTriggerDecisions().entrySet()) {
            localStore.setTriggerDecision(AutoYieldStore.Tier.GAME, e.getKey(), e.getValue());
        }
        for (Map.Entry<String, AutoYieldStore.TriggerDecision> e : snap.abilityTriggerDecisions().entrySet()) {
            localStore.setTriggerDecision(AutoYieldStore.Tier.GAME, e.getKey(), e.getValue());
        }
        localStore.setDisabled(snap.autoYieldsDisabled());
        localStore.setTriggerDecisionsDisabled(snap.autoTriggersDisabled());
        skipPhases.clear();
        skipPhases.putAll(snap.skipPhases());
    }
}
