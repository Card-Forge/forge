package forge.gamemodes.match;

import forge.game.GameView;
import forge.game.ability.ApiType;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.spellability.StackItemView;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IGameController;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.player.AutoYieldStore;
import forge.player.LobbyPlayerHuman;
import forge.player.PersistentAutoDecisionStore;
import forge.player.PlayerControllerHuman;
import forge.util.collect.FCollectionView;

import java.util.EnumMap;
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

    /** Yield FPrefs synced per-PCH; enumerated here so the client snapshot includes every value, not just touched overrides.
     * Stored String-typed (see {@link forge.localinstance.properties.IPreferences}); consumers parse via {@link #getBoolPref}/{@link #getStringPref} according to the pref's expected type. */
    private static final EnumSet<FPref> SYNCED_PREFS = EnumSet.of(
            FPref.YIELD_INTERRUPT_ON_ATTACKERS,
            FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL,
            FPref.YIELD_INTERRUPT_ON_TARGETING,
            FPref.YIELD_INTERRUPT_ON_TRIGGERS,
            FPref.YIELD_INTERRUPT_ON_REVEAL,
            FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL,
            FPref.YIELD_AUTO_PASS_NO_ACTIONS,
            FPref.YIELD_AUTO_PASS_RESPECTS_INTERRUPTS,
            FPref.YIELD_SKIP_PHASE_DELAY,
            FPref.YIELD_SKIP_RESOLVE_DELAY,
            FPref.YIELD_SUPPRESS_ON_OWN_TURN,
            FPref.YIELD_SUPPRESS_AFTER_END,
            FPref.YIELD_AVAILABLE_ACTIONS_BUDGET_MS,
            FPref.YIELD_DECLINE_SCOPE_STACK_YIELD,
            FPref.YIELD_DECLINE_SCOPE_NO_ACTIONS,
            // Not yield prefs, but seeded the same way: the host runs preview scans on
            // the remote player's behalf and must use that client's highlight settings, not its own.
            FPref.UI_SHOW_ACTIONABLE_HIGHLIGHTS,
            FPref.UI_SHOW_AUTOTAP_PREVIEW);

    private final PlayerControllerHuman owner;

    private boolean autoPassUntilEOT;
    private boolean autoPassUntilStackEmpty;
    /** When true, an active stack-yield is cleared by interrupt classifiers; when false, only stack-empty turns it off. */
    private boolean stackYieldRespectsInterrupts;
    private YieldMarker autoPassUntilMarker;

    /** Priority has passed through any non-target phase since marker activation. */
    private boolean hasLeftMarker;
    /** Marker was set while priority was already at its target; require a full cycle to fire. */
    private boolean activationOnMarker;

    private final AutoYieldStore localStore = new AutoYieldStore();
    private final Map<PlayerView, EnumSet<PhaseType>> skipPhases = new HashMap<>();

    /** Populated only on the host's proxy of a remote player (via {@link #applyClientSeed} and {@link YieldUpdate.SetYieldPref} envelopes); local controllers always defer to FModel. Override wins, FModel is fallback. */
    private final EnumMap<FPref, String> prefOverrides = new EnumMap<>(FPref.class);

    public YieldController(PlayerControllerHuman owner) {
        this.owner = owner;
    }

    public synchronized String getStringPref(FPref pref) {
        String override = prefOverrides.get(pref);
        return override != null ? override : FModel.getPreferences().getPref(pref);
    }
    public boolean getBoolPref(FPref pref) {
        return Boolean.parseBoolean(getStringPref(pref));
    }
    public synchronized void setPref(FPref pref, String value) {
        prefOverrides.put(pref, value);
    }

    public DeclineScope getDeclineScope(FPref pref) {
        return DeclineScope.fromPref(getStringPref(pref));
    }

    public boolean isSkippingPhase(PlayerView turnPlayer, PhaseType phase) {
        EnumSet<PhaseType> set = skipPhases.get(turnPlayer);
        return set != null && set.contains(phase);
    }
    public void setSkipPhase(PlayerView turnPlayer, PhaseType phase, boolean skip) {
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

    // All mutators synchronized — fields touched from EDT, Netty, game thread.
    // Activating any yield type clears the others — only one yield type may be active at a time (APINA is orthogonal).
    public synchronized void setAutoPassUntilStackEmpty(boolean active, boolean respectsInterrupts) {
        if (active) {
            autoPassUntilEOT = false;
            clearMarker();
        }
        this.autoPassUntilStackEmpty = active;
        this.stackYieldRespectsInterrupts = active && respectsInterrupts;
    }
    public synchronized void setAutoPassUntilEndOfTurn(boolean active) {
        if (active) {
            autoPassUntilStackEmpty = false;
            stackYieldRespectsInterrupts = false;
            clearMarker();
        }
        this.autoPassUntilEOT = active;
    }
    public synchronized void setMarker(PlayerView phaseOwner, PhaseType phase, boolean atOrPastAtClick) {
        autoPassUntilEOT = false;
        autoPassUntilStackEmpty = false;
        stackYieldRespectsInterrupts = false;
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
        if (!phaseOwner.equals(gv.getPlayerTurn())) return false;
        PhaseType currentPhase = gv.getPhase();
        return currentPhase == phase || currentPhase.isAfter(phase);
    }

    public boolean shouldAutoYield() {
        if (autoPassUntilEOT) return true;
        GameView gv = owner != null && owner.getGui() != null ? owner.getGui().getGameView() : null;
        if (autoPassUntilStackEmpty) {
            if (gv != null && gv.peekStack() != null) return true;
            autoPassUntilStackEmpty = false;
            stackYieldRespectsInterrupts = false;
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

    /** True when the active auto-decision scope is per-ability (any tier above per-card/per-game). */
    public boolean isAbilityScope() {
        return !ForgeConstants.AUTO_DECISION_PER_CARD.equals(FModel.getPreferences().getPref(FPref.UI_AUTO_DECISION_MODE));
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
        return ForgeConstants.AUTO_DECISION_PER_ABILITY_INSTALL.equals(FModel.getPreferences().getPref(FPref.UI_AUTO_DECISION_MODE));
    }

    private static AutoYieldStore.Tier activeTier() {
        String mode = FModel.getPreferences().getPref(FPref.UI_AUTO_DECISION_MODE);
        if (ForgeConstants.AUTO_DECISION_PER_CARD.equals(mode))            return AutoYieldStore.Tier.GAME;
        if (ForgeConstants.AUTO_DECISION_PER_ABILITY_SESSION.equals(mode)) return AutoYieldStore.Tier.SESSION;
        return AutoYieldStore.Tier.MATCH;
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
        activeStore().onGameEnd(owner.getGame() == null || owner.getGame().getView().isMatchOver());
    }

    public boolean getDisableAutoYields() {
        return activeStore().isDisabled();
    }
    public void setDisableAutoYields(boolean disable) {
        activeStore().setDisabled(disable);
    }

    public AutoYieldStore.TriggerDecision getTriggerDecision(String key) {
        AutoYieldStore store = activeStore();
        if (key == null || key.isEmpty() || store.isTriggerDecisionsDisabled()) return AutoYieldStore.TriggerDecision.ASK;
        if (!tierAware()) {
            // Cache mode: keys stored at storageKey shape (full or stripped).
            AutoYieldStore.TriggerDecision d = store.getTriggerDecision(AutoYieldStore.Tier.GAME, key);
            if (d != AutoYieldStore.TriggerDecision.ASK) return d;
            return store.getTriggerDecision(AutoYieldStore.Tier.GAME, AutoYieldStore.abilitySuffix(key));
        }
        if (activeModeIsInstall()) {
            return PersistentAutoDecisionStore.get().getTriggerDecision(AutoYieldStore.abilitySuffix(key));
        }
        AutoYieldStore.Tier tier = activeTier();
        boolean abilityScope = tier != AutoYieldStore.Tier.GAME;
        String storageKey = abilityScope ? AutoYieldStore.abilitySuffix(key) : key;
        return store.getTriggerDecision(tier, storageKey);
    }

    /** Tier-aware user-initiated set. Returns the storage key (stripped if ability-scope) for wire propagation. */
    public String setTriggerDecision(String key, AutoYieldStore.TriggerDecision decision, boolean abilityScope) {
        String storageKey = abilityScope ? AutoYieldStore.abilitySuffix(key) : key;
        if (activeModeIsInstall()) {
            PersistentAutoDecisionStore.get().setTriggerDecision(storageKey, decision);
        } else {
            activeStore().setTriggerDecision(activeTier(), storageKey, decision);
        }
        return storageKey;
    }

    public Iterable<Map.Entry<String, AutoYieldStore.TriggerDecision>> getAutoTriggers() {
        if (!tierAware()) return activeStore().getAutoTriggers(AutoYieldStore.Tier.GAME);
        if (activeModeIsInstall()) return PersistentAutoDecisionStore.get().getAutoTriggers();
        return activeStore().getAutoTriggers(activeTier());
    }

    public boolean getDisableAutoTriggers() { return activeStore().isTriggerDecisionsDisabled(); }
    public void setDisableAutoTriggers(boolean disable) { activeStore().setTriggerDecisionsDisabled(disable); }

    /** Build the seed payload from this controller's authoritative store. */
    public YieldStateSnapshot buildClientSnapshot(Map<PlayerView, EnumSet<PhaseType>> skipPhases) {
        boolean abilityScope = activeModeIsInstall() || activeTier() != AutoYieldStore.Tier.GAME;

        Set<String> cardYields = new HashSet<>();
        Set<String> abilityYields = new HashSet<>();
        for (String key : getAutoYields()) {
            if (abilityScope) abilityYields.add(key);
            else cardYields.add(key);
        }

        Map<String, AutoYieldStore.TriggerDecision> cardTriggers = new HashMap<>();
        Map<String, AutoYieldStore.TriggerDecision> abilityTriggers = new HashMap<>();
        for (Map.Entry<String, AutoYieldStore.TriggerDecision> e : getAutoTriggers()) {
            if (abilityScope) abilityTriggers.put(e.getKey(), e.getValue());
            else cardTriggers.put(e.getKey(), e.getValue());
        }

        EnumMap<FPref, String> prefs = new EnumMap<>(FPref.class);
        for (FPref pref : SYNCED_PREFS) prefs.put(pref, FModel.getPreferences().getPref(pref));

        return new YieldStateSnapshot(
                cardYields, abilityYields,
                cardTriggers, abilityTriggers,
                getDisableAutoYields(), getDisableAutoTriggers(),
                skipPhases, prefs);
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
        prefOverrides.clear();
        prefOverrides.putAll(snap.prefOverrides());
    }

    /**
     * Apply a wire envelope variant to local state. Returns true if a yield was
     * activated (caller may want to refresh the prompt UI). Per-method locking is
     * provided by the delegated setters. Unhandled variants no-op — wire routing
     * is responsible for ensuring only valid variants reach each side.
     */
    public boolean apply(YieldUpdate update) {
        if (update instanceof YieldUpdate.SetMarker u) {
            setMarker(u.phaseOwner(), u.phase(), u.atOrPastAtClick());
            return true;
        } else if (update instanceof YieldUpdate.ClearMarker) {
            clearMarker();
        } else if (update instanceof YieldUpdate.StackYield u) {
            setAutoPassUntilStackEmpty(u.active(), u.respectsInterrupts());
            return u.active();
        } else if (update instanceof YieldUpdate.SetAutoPassUntilEndOfTurn u) {
            setAutoPassUntilEndOfTurn(u.active());
            return u.active();
        } else if (update instanceof YieldUpdate.CardAutoYield u) {
            activeStore().setYield(AutoYieldStore.Tier.GAME, u.cardKey(), u.active());
        } else if (update instanceof YieldUpdate.TriggerDecision u) {
            activeStore().setTriggerDecision(AutoYieldStore.Tier.GAME, u.storageKey(), u.decision());
        } else if (update instanceof YieldUpdate.SetDisableYields u) {
            setDisableAutoYields(u.disabled());
        } else if (update instanceof YieldUpdate.SetDisableTriggers u) {
            setDisableAutoTriggers(u.disabled());
        } else if (update instanceof YieldUpdate.SkipPhase u) {
            setSkipPhase(u.turnPlayer(), u.phase(), u.skip());
        } else if (update instanceof YieldUpdate.SetYieldPref u) {
            setPref(u.pref(), u.value());
        } else if (update instanceof YieldUpdate.SeedFromClient u) {
            applyClientSeed(u.snapshot());
        }
        return false;
    }

    public boolean isYieldActive() {
        return autoPassUntilEOT || autoPassUntilStackEmpty || autoPassUntilMarker != null;
    }

    /** EOT, marker, and interruptible stack-yields back off via {@link #applyInterrupt()}. The non-interruptible stack-yield is fire-and-forget — only stack-empty turns it off. */
    public boolean isInterruptibleYieldActive() {
        return autoPassUntilEOT
                || autoPassUntilMarker != null
                || (autoPassUntilStackEmpty && stackYieldRespectsInterrupts);
    }

    public synchronized void clearActiveYieldAndDispatch() {
        PlayerView local = owner != null ? owner.getLocalPlayerView() : null;
        if (local == null) return;
        IGuiGame gui = owner.getGui();
        boolean anyCleared = false;
        if (autoPassUntilMarker != null) {
            clearMarker();
            if (gui != null) gui.applyYieldUpdate(new YieldUpdate.ClearMarker(local));
            anyCleared = true;
        }
        if (autoPassUntilStackEmpty) {
            autoPassUntilStackEmpty = false;
            stackYieldRespectsInterrupts = false;
            if (gui != null) gui.applyYieldUpdate(new YieldUpdate.StackYield(local, false, false));
            anyCleared = true;
        }
        if (autoPassUntilEOT) {
            autoPassUntilEOT = false;
            if (gui != null) gui.applyYieldUpdate(new YieldUpdate.SetAutoPassUntilEndOfTurn(local, false));
            anyCleared = true;
        }
        if (anyCleared && gui != null) gui.updateAutoPassPrompt();
    }

    /** Toggle APINA: flip pref, persist, push to controller. Returns new value. */
    public static boolean toggleAutoPassNoActions(IGameController ctrl) {
        if (ctrl == null) return false;
        ForgePreferences prefs = FModel.getPreferences();
        boolean newVal = !prefs.getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS);
        prefs.setPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, newVal);
        prefs.save();
        ctrl.setYieldPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, String.valueOf(newVal));
        return newVal;
    }

    /**
     * Single unified yield-key action. If any yield is currently active (transient
     * yield state or APINA on), clears everything. Otherwise turns APINA on. So one
     * key acts as both "stop any yielding" and "start auto-passing" depending on state.
     */
    public static void toggleAutoPassOrStopAll(IGameController ctrl) {
        if (ctrl == null) return;
        YieldController yc = ctrl.getYieldController();
        boolean apinaOn = FModel.getPreferences().getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS);
        if (yc.isYieldActive() || apinaOn) {
            yc.clearActiveYieldAndDispatch();
            if (apinaOn) toggleAutoPassNoActions(ctrl);
        } else {
            toggleAutoPassNoActions(ctrl);
        }
    }

    /** Activate auto-pass-until-end-of-turn for {@code local} via the unified envelope path. */
    public static void endTurn(IGameController ctrl, PlayerView local) {
        if (ctrl == null || local == null) return;
        ctrl.sendYieldUpdate(new YieldUpdate.SetAutoPassUntilEndOfTurn(local, true));
    }

    /** Eager check at REVEAL/notifyOfValue call sites — no GameEventReveal exists. */
    public void maybeInterruptOnReveal() {
        if (!getBoolPref(FPref.YIELD_INTERRUPT_ON_REVEAL)) return;
        applyInterrupt();
    }

    public void onSpellAbilityCast(SpellAbilityStackInstance si) {
        if (!shouldEvaluateInterrupts()) return;
        PlayerView local = owner.getLocalPlayerView();
        if (local == null) return;
        boolean isOpponent = !si.getActivatingPlayer().getView().equals(local);

        if (isOpponent && getBoolPref(FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL)) {
            applyInterrupt();
            return;
        }
        StackItemView siv = StackItemView.get(si);
        if (siv != null && getBoolPref(FPref.YIELD_INTERRUPT_ON_TARGETING)
                && targetsPlayerOrPermanents(siv, local)) {
            applyInterrupt();
            return;
        }
        if (isOpponent && getBoolPref(FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL)
                && isMassRemoval(si)) {
            applyInterrupt();
            return;
        }
        if (si.isTrigger() && getBoolPref(FPref.YIELD_INTERRUPT_ON_TRIGGERS)) {
            applyInterrupt();
        }
    }

    public void onAttackersDeclared(CombatView combat) {
        if (!shouldEvaluateInterrupts()) return;
        PlayerView local = owner.getLocalPlayerView();
        if (local == null) return;
        if (getBoolPref(FPref.YIELD_INTERRUPT_ON_ATTACKERS) && isBeingAttacked(combat, local)) {
            applyInterrupt();
        }
    }

    /** True when an event-driven interrupt should be considered: either an interruptible
     *  yield is active, or APINA + RESPECTS_INTERRUPTS are both on (so the next priority
     *  window's auto-pass can be blocked by autoPassInterrupted). */
    public boolean shouldEvaluateInterrupts() {
        if (isInterruptibleYieldActive()) return true;
        return getBoolPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS) && getBoolPref(FPref.YIELD_AUTO_PASS_RESPECTS_INTERRUPTS);
    }

    /** Apply an interrupt: clear any interruptible yield and pause APINA for one prompt. Either, both, or neither may apply. */
    public synchronized void applyInterrupt() {
        if (isInterruptibleYieldActive()) clearActiveYieldAndDispatch();
        if (getBoolPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS) && getBoolPref(FPref.YIELD_AUTO_PASS_RESPECTS_INTERRUPTS)) {
            autoPassInterrupted = true;
        }
    }

    private final EnumMap<SuggestionType, Integer> declinedSuggestionTurn = new EnumMap<>(SuggestionType.class);
    private boolean lastSeenStackNonEmpty = false;
    private boolean wasAutoPassingLastTick = false;
    private boolean yieldJustEndedFlag = false;
    private boolean autoPassInterrupted = false;

    public synchronized void onPriorityReceived(boolean stackNonEmpty) {
        if (lastSeenStackNonEmpty && !stackNonEmpty &&
                getDeclineScope(FPref.YIELD_DECLINE_SCOPE_STACK_YIELD) == DeclineScope.STACK) {
            declinedSuggestionTurn.remove(SuggestionType.STACK_YIELD);
        }
        lastSeenStackNonEmpty = stackNonEmpty;
    }

    public synchronized void declineSuggestion(SuggestionType type) {
        GameView gv = owner != null && owner.getGui() != null ? owner.getGui().getGameView() : null;
        if (gv == null) return;
        declinedSuggestionTurn.put(type, gv.getTurn());
    }

    /**
     * NEVER disables the suggestion entirely; ALWAYS never suppresses;
     * STACK/TURN suppress if declined this turn (STACK also self-clears on stack-empty
     * transition via {@link #onPriorityReceived}).
     */
    public synchronized boolean isSuggestionDeclined(SuggestionType type) {
        DeclineScope scope = getDeclineScope(type.scopePref());
        if (scope == DeclineScope.NEVER) return true;
        if (scope == DeclineScope.ALWAYS) return false;
        GameView gv = owner != null && owner.getGui() != null ? owner.getGui().getGameView() : null;
        if (gv == null) return false;
        Integer turnDeclined = declinedSuggestionTurn.get(type);
        return turnDeclined != null && turnDeclined == gv.getTurn();
    }

    public synchronized void noteMayAutoPassResult(boolean nowMayAutoPass) {
        if (wasAutoPassingLastTick && !nowMayAutoPass) yieldJustEndedFlag = true;
        wasAutoPassingLastTick = nowMayAutoPass;
        if (!nowMayAutoPass) autoPassInterrupted = false;
    }

    /** Self-clearing read of the mayAutoPass true→false edge. */
    public synchronized boolean didYieldJustEnd() {
        boolean f = yieldJustEndedFlag;
        yieldJustEndedFlag = false;
        return f;
    }

    /** Per-tick predicate (distinct from one-shot yields). Used by mayAutoPass. */
    public boolean isAutoPassingNoActions(PlayerView player) {
        if (!getBoolPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS)) return false;
        if (autoPassInterrupted) return false;
        GameView gv = owner != null && owner.getGui() != null ? owner.getGui().getGameView() : null;
        if (gv != null && gv.getStack() != null && gv.getStack().isEmpty()) {
            PlayerView turnPlayer = gv.getPlayerTurn();
            PhaseType phase = gv.getPhase();
            if (turnPlayer != null && phase != null
                    && owner.getGui().isUiSetToSkipPhase(turnPlayer, phase)) {
                return true;
            }
        }
        return player != null && !player.hasAvailableActions();
    }

    private static boolean isBeingAttacked(CombatView combatView, PlayerView player) {
        if (combatView == null) return false;
        if (!combatView.getAttackersOf(player).isEmpty()) return true;
        for (forge.game.GameEntityView defender : combatView.getDefenders()) {
            if (defender instanceof CardView cardDefender && player.equals(cardDefender.getController()) &&
                    !combatView.getAttackersOf(defender).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean targetsPlayerOrPermanents(StackItemView si, PlayerView player) {
        FCollectionView<PlayerView> targetPlayers = si.getTargetPlayers();
        if (targetPlayers != null) {
            for (PlayerView target : targetPlayers) {
                if (target.equals(player)) return true;
            }
        }
        FCollectionView<CardView> targetCards = si.getTargetCards();
        if (targetCards != null) {
            for (CardView target : targetCards) {
                if (player.equals(target.getController())) return true;
            }
        }
        StackItemView subInstance = si.getSubInstance();
        return subInstance != null && targetsPlayerOrPermanents(subInstance, player);
    }

    private static boolean isMassRemoval(SpellAbilityStackInstance si) {
        ApiType api = si.getSpellAbility().getApi();
        if (api == ApiType.DestroyAll
                || api == ApiType.DamageAll
                || api == ApiType.SacrificeAll
                || api == ApiType.ChangeZoneAll) return true;
        SpellAbilityStackInstance subInstance = si.getSubInstance();
        return subInstance != null && isMassRemoval(subInstance);
    }

}
