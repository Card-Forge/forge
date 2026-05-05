package forge.gamemodes.match;

import forge.game.GameView;
import forge.game.ability.ApiType;
import forge.game.card.CardView;
import forge.game.combat.CombatView;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbility;
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
import forge.player.PersistentYieldStore;
import forge.player.PlayerControllerHuman;
import forge.util.collect.FCollection;
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

    /** Override wins, FModel is fallback. Synced per-PCH so each client's prefs govern their proxy. */
    private final EnumMap<FPref, Boolean> boolPrefOverrides = new EnumMap<>(FPref.class);
    private final EnumMap<FPref, String>  stringPrefOverrides = new EnumMap<>(FPref.class);

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

    // All mutators synchronized — fields touched from EDT, Netty, game thread.
    // Activating any yield type clears the others — only one yield type may be active at a time (APINA is orthogonal).
    public synchronized void setAutoPassUntilStackEmpty(boolean active) {
        if (active) {
            autoPassUntilEOT = false;
            clearMarker();
        }
        this.autoPassUntilStackEmpty = active;
    }
    public synchronized void setAutoPassUntilEndOfTurn(boolean active) {
        if (active) {
            autoPassUntilStackEmpty = false;
            clearMarker();
        }
        this.autoPassUntilEOT = active;
    }

    public synchronized boolean getBoolPref(FPref pref) {
        Boolean override = boolPrefOverrides.get(pref);
        return override != null ? override : FModel.getPreferences().getPrefBoolean(pref);
    }
    public synchronized void setBoolPref(FPref pref, boolean value) {
        boolPrefOverrides.put(pref, value);
    }

    public synchronized String getStringPref(FPref pref) {
        String override = stringPrefOverrides.get(pref);
        return override != null ? override : FModel.getPreferences().getPref(pref);
    }
    public synchronized void setStringPref(FPref pref, String value) {
        stringPrefOverrides.put(pref, value);
    }

    public DeclineScope getDeclineScope(FPref pref) {
        return DeclineScope.fromPref(getStringPref(pref));
    }

    public Map<FPref, Boolean> snapshotBoolPrefs() {
        return new EnumMap<>(boolPrefOverrides);
    }
    public Map<FPref, String> snapshotStringPrefs() {
        return new EnumMap<>(stringPrefOverrides);
    }

    // setMarker/clearMarker are mutated from EDT (right-click), Netty (wire receive), and game thread.
    public synchronized void setMarker(PlayerView phaseOwner, PhaseType phase, boolean atOrPastAtClick) {
        autoPassUntilEOT = false;
        autoPassUntilStackEmpty = false;
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

    /** Clear all transient yield state so it doesn't carry into the next game of the match. */
    public synchronized void resetForNewGame() {
        autoPassUntilEOT = false;
        autoPassUntilStackEmpty = false;
        autoPassUntilMarker = null;
        hasLeftMarker = false;
        activationOnMarker = false;
        declinedSuggestionTurn.clear();
        lastSeenStackNonEmpty = false;
        wasAutoPassingLastTick = false;
        yieldJustEndedFlag = false;
        autoPassInterrupted = false;
        // boolPrefOverrides / stringPrefOverrides intentionally kept — per-match, not per-game
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
        return new YieldStateSnapshot(cardYields, abilityYields, triggers, getDisableAutoYields(), skipPhases,
                snapshotBoolPrefs(), snapshotStringPrefs());
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
        skipPhases.putAll(snap.skipPhases());
        boolPrefOverrides.clear();
        if (snap.boolPrefOverrides() != null) boolPrefOverrides.putAll(snap.boolPrefOverrides());
        stringPrefOverrides.clear();
        if (snap.stringPrefOverrides() != null) stringPrefOverrides.putAll(snap.stringPrefOverrides());
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
            setAutoPassUntilStackEmpty(u.active());
            return u.active();
        } else if (update instanceof YieldUpdate.SetAutoPassUntilEndOfTurn u) {
            setAutoPassUntilEndOfTurn(u.active());
            return u.active();
        } else if (update instanceof YieldUpdate.TriggerDecision u) {
            setTriggerDecision(u.trigId(), u.decision());
        } else if (update instanceof YieldUpdate.CardAutoYield u) {
            applyAutoYieldFromWire(u.cardKey(), u.active());
        } else if (update instanceof YieldUpdate.SkipPhase u) {
            setSkipPhase(u.turnPlayer(), u.phase(), u.skip());
        } else if (update instanceof YieldUpdate.SetYieldBoolPref u) {
            setBoolPref(u.pref(), u.value());
        } else if (update instanceof YieldUpdate.SetYieldStringPref u) {
            setStringPref(u.pref(), u.value());
        } else if (update instanceof YieldUpdate.SeedFromClient u) {
            applyClientSeed(u.snapshot());
        }
        return false;
    }

    public boolean isYieldActive() {
        return autoPassUntilEOT || autoPassUntilStackEmpty || autoPassUntilMarker != null;
    }

    /** EOT and marker yields can be interrupted. Stack-yield is fire and forget — only stack-empty turns it off. */
    public boolean isInterruptibleYieldActive() {
        return autoPassUntilEOT || autoPassUntilMarker != null;
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
            if (gui != null) gui.applyYieldUpdate(new YieldUpdate.StackYield(local, false));
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
        ctrl.setYieldBoolPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, newVal);
        return newVal;
    }

    /** Eager check at REVEAL/notifyOfValue call sites — no GameEventReveal exists. */
    public void maybeInterruptOnReveal() {
        if (!getBoolPref(FPref.YIELD_INTERRUPT_ON_REVEAL)) return;
        applyInterrupt();
    }

    public void onSpellAbilityCast(SpellAbilityStackInstance si, GameView gameView) {
        if (!shouldEvaluateInterrupts()) return;
        if (si == null) return;
        PlayerView local = owner != null ? owner.getLocalPlayerView() : null;
        if (local == null) return;
        Player activator = si.getActivatingPlayer();
        boolean isOpponent = activator != null && !activator.getView().equals(local);

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
                && isMassRemovalInstance(si)) {
            applyInterrupt();
            return;
        }
        if (si.isTrigger() && getBoolPref(FPref.YIELD_INTERRUPT_ON_TRIGGERS)) {
            applyInterrupt();
        }
    }

    public void onAttackersDeclared(CombatView combat) {
        if (!shouldEvaluateInterrupts()) return;
        PlayerView local = owner != null ? owner.getLocalPlayerView() : null;
        if (local == null) return;
        if (getBoolPref(FPref.YIELD_INTERRUPT_ON_ATTACKERS) && isBeingAttacked(combat, local)) {
            applyInterrupt();
        }
    }

    /** True when interrupt classifiers should run — an interruptible yield is active, or APINA is active with respects-interrupts on. */
    private boolean shouldEvaluateInterrupts() {
        if (isInterruptibleYieldActive()) return true;
        return getBoolPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS)
                && getBoolPref(FPref.YIELD_AUTO_PASS_RESPECTS_INTERRUPTS);
    }

    /** Apply an interrupt: clear any interruptible yield and pause APINA for one prompt. Either, both, or neither may apply. */
    public synchronized void applyInterrupt() {
        if (isInterruptibleYieldActive()) clearActiveYieldAndDispatch();
        if (getBoolPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS)
                && getBoolPref(FPref.YIELD_AUTO_PASS_RESPECTS_INTERRUPTS)) {
            autoPassInterrupted = true;
        }
    }

    private final EnumMap<SuggestionType, Integer> declinedSuggestionTurn = new EnumMap<>(SuggestionType.class);
    private boolean lastSeenStackNonEmpty = false;
    private boolean wasAutoPassingLastTick = false;
    private boolean yieldJustEndedFlag = false;
    private boolean autoPassInterrupted = false;

    public synchronized void onPriorityReceived(boolean stackNonEmpty) {
        if (lastSeenStackNonEmpty && !stackNonEmpty) {
            if (getDeclineScope(FPref.YIELD_DECLINE_SCOPE_STACK_YIELD) == DeclineScope.STACK) {
                declinedSuggestionTurn.remove(SuggestionType.STACK_YIELD);
            }
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
        FCollection<CardView> attackersOfPlayer = combatView.getAttackersOf(player);
        if (attackersOfPlayer != null && !attackersOfPlayer.isEmpty()) return true;
        for (forge.game.GameEntityView defender : combatView.getDefenders()) {
            if (defender instanceof CardView cardDefender) {
                PlayerView controller = cardDefender.getController();
                if (controller != null && controller.equals(player)) {
                    FCollection<CardView> attackers = combatView.getAttackersOf(defender);
                    if (attackers != null && !attackers.isEmpty()) return true;
                }
            }
        }
        return false;
    }

    /** Recurses into sub-instances (e.g. Oona, where targeting is in a sub-ability). */
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
                if (target.getController() != null && target.getController().equals(player)) return true;
            }
        }
        StackItemView subInstance = si.getSubInstance();
        if (subInstance != null && targetsPlayerOrPermanents(subInstance, player)) return true;
        return false;
    }

    /** Recurses into sub-instances for modal spells like Farewell. */
    private static boolean isMassRemovalInstance(SpellAbilityStackInstance si) {
        SpellAbility sa = si.getSpellAbility();
        if (sa != null && isMassRemovalApi(sa.getApi())) return true;
        SpellAbilityStackInstance subInstance = si.getSubInstance();
        return subInstance != null && isMassRemovalInstance(subInstance);
    }

    private static boolean isMassRemovalApi(ApiType api) {
        return api == ApiType.DestroyAll
            || api == ApiType.DamageAll
            || api == ApiType.SacrificeAll
            || api == ApiType.ChangeZoneAll;
    }
}
