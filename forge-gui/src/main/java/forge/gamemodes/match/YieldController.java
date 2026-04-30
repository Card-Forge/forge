package forge.gamemodes.match;

import forge.game.GameView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.interfaces.IGameController;
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

    private boolean autoPassUntilStackEmpty;
    private boolean autoPassUntilEOT;
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

    public boolean autoPassUntilEOT() {
        return autoPassUntilStackEmpty;
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

    public void setMarker(PlayerView phaseOwner, PhaseType phase) {
        autoPassUntilEOT = false;
        if (phaseOwner == null || phase == null) {
            clearMarker();
            return;
        }
        autoPassUntilMarker = new YieldMarker(phaseOwner, phase);
        // Activating at-or-past target on the owner's current turn must wait for next turn's
        // occurrence; otherwise pastTarget would fire and clear the marker on the same turn.
        boolean atOrPast = isPriorityAtOrPastMarker(autoPassUntilMarker);
        hasLeftMarker = !atOrPast;
        activationOnMarker = atOrPast;
    }

    public void clearMarker() {
        autoPassUntilMarker = null;
        hasLeftMarker = false;
        activationOnMarker = false;
    }

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

    /**
     * User-initiated ESC: clear marker, legacy autopass, and stack-yield (the three "active"
     * yield forms). Persistent per-card auto-yields are unaffected. Marker and stack-yield go
     * over the wire via the controller; legacy autopass is local to host PCH.
     *
     * @return true if any of the three were active and got cleared (caller should refresh UI
     *         and suppress the ESC fallthrough); false if nothing was active.
     */
    public boolean cancelGenericYields(PlayerView local, IGameController controller) {
        boolean hadMarker = autoPassUntilMarker != null;
        boolean hadLegacy = autoPassUntilEOT;
        boolean hadStackYield = autoPassUntilStackEmpty;
        if (!hadMarker && !hadLegacy && !hadStackYield) return false;
        if (hadMarker) controller.sendYieldUpdate(new YieldUpdate.ClearMarker(local));
        if (hadLegacy && controller instanceof PlayerControllerHuman pch) pch.autoPassCancel();
        if (hadStackYield) controller.sendYieldUpdate(new YieldUpdate.StackYield(local, false));
        return true;
    }

    public boolean shouldAutoYield() {
        if (autoPassUntilEOT) return true;

        GameView gv = owner != null && owner.getGui() != null ? owner.getGui().getGameView() : null;

        if (autoPassUntilStackEmpty) {
            if (gv != null && gv.getStack() != null && !gv.getStack().isEmpty()) {
                return true;
            }
            autoPassUntilStackEmpty = false;
        }

        if (autoPassUntilMarker == null || gv == null) return false;

        PlayerView turnPlayer = gv.getPlayerTurn();
        PhaseType currentPhase = gv.getPhase();

        boolean inMarkerOwnerTurn = turnPlayer != null && turnPlayer.equals(autoPassUntilMarker.getPhaseOwner());
        boolean atTarget = inMarkerOwnerTurn && currentPhase == autoPassUntilMarker.getPhase();
        boolean pastTarget = inMarkerOwnerTurn && currentPhase != null
                && autoPassUntilMarker.getPhase() != null && currentPhase.isAfter(autoPassUntilMarker.getPhase());

        boolean shouldFire = hasLeftMarker
                && (atTarget || (!activationOnMarker && pastTarget));

        if (shouldFire) {
            clearMarker();
            if (owner != null && owner.getGui() != null) {
                PlayerView player = owner.getLocalPlayerView();
                if (player != null) {
                    owner.getGui().applyYieldUpdate(new YieldUpdate.ClearMarker(player));
                }
            }
            return false;
        }
        if (!atTarget && !hasLeftMarker) {
            hasLeftMarker = true;
        }
        return true;
    }

    // ---- Auto-yield (per-card/ability) and trigger decisions ----

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
