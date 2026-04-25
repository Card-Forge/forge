/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gamemodes.match;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.gui.interfaces.IGuiGame;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.trackable.TrackableTypes;
import forge.util.Localizer;

import java.util.Map;
import java.util.Set;

/**
 * Manages yield state and logic for the experimental yield system.
 * Handles automatic priority passing, interrupt conditions, and smart suggestions.
 *
 * This class is GUI-layer only and does not modify game state or network protocol.
 * Each client manages its own yield state independently.
 */
public class YieldController {

    private final IGuiGame gui;

    // Legacy auto-pass tracking
    private final Set<PlayerView> autoPassUntilEndOfTurn = Sets.newConcurrentHashSet();

    /**
     * Consolidated yield state for a player. Immutable so that ConcurrentHashMap
     * publication of the value reference is sufficient — readers on the game thread
     * see a consistent snapshot even when writers on the Netty thread replace the
     * map entry via setYieldModeSilent.
     */
    private static final class YieldState {
        final YieldMode mode;
        final Integer startTurn;                          // For UNTIL_END_OF_TURN, UNTIL_BEFORE_COMBAT, UNTIL_END_STEP
        final Boolean startedAtOrAfterPhase;              // For UNTIL_BEFORE_COMBAT and UNTIL_END_STEP
        final forge.game.phase.PhaseType startPhase;      // For UNTIL_NEXT_PHASE
        final Boolean startedDuringOurTurn;               // For UNTIL_YOUR_NEXT_TURN

        private YieldState(YieldMode mode, Integer startTurn, Boolean startedAtOrAfterPhase,
                           forge.game.phase.PhaseType startPhase, Boolean startedDuringOurTurn) {
            this.mode = mode;
            this.startTurn = startTurn;
            this.startedAtOrAfterPhase = startedAtOrAfterPhase;
            this.startPhase = startPhase;
            this.startedDuringOurTurn = startedDuringOurTurn;
        }

        static YieldState of(YieldMode mode) {
            return new YieldState(mode, null, null, null, null);
        }

        YieldState withStartTurn(Integer v) {
            return new YieldState(mode, v, startedAtOrAfterPhase, startPhase, startedDuringOurTurn);
        }
        YieldState withStartedAtOrAfterPhase(Boolean v) {
            return new YieldState(mode, startTurn, v, startPhase, startedDuringOurTurn);
        }
        YieldState withStartPhase(forge.game.phase.PhaseType v) {
            return new YieldState(mode, startTurn, startedAtOrAfterPhase, v, startedDuringOurTurn);
        }
        YieldState withStartedDuringOurTurn(Boolean v) {
            return new YieldState(mode, startTurn, startedAtOrAfterPhase, startPhase, v);
        }
    }

    // Extended yield mode tracking (experimental feature)
    private final Map<PlayerView, YieldState> yieldStates = Maps.newConcurrentMap();

    public YieldController(IGuiGame gui) {
        this.gui = gui;
    }

    public void autoPassUntilEndOfTurn(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure consistent PlayerView instance
        autoPassUntilEndOfTurn.add(player);
    }

    public void autoPassCancel(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure consistent PlayerView instance
        if (!autoPassUntilEndOfTurn.remove(player)) {
            return;
        }

        // Prevent prompt getting stuck on yielding message while actually waiting for next input opportunity
        gui.showPromptMessage(player, "");
        gui.updateButtons(player, false, false, false);
        gui.awaitNextInput();
    }

    public boolean mayAutoPass(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player);
        // Yield modes self-clear when their stop condition fires (end step / your turn / etc).
        // Must run before isAutoPassingNoActions or that short-circuits and the mode never clears.
        if (shouldAutoYieldForPlayer(player)) {
            return true;
        }
        return isAutoPassingNoActions(player);
    }

    /** Persistent preference toggle (YIELD_AUTO_PASS_NO_ACTIONS), not a one-shot yield mode. */
    public boolean isAutoPassingNoActions(PlayerView player) {
        if (!isYieldExperimentalEnabled()) {
            return false;
        }
        boolean prefValue = getInterruptPref(ForgePreferences.FPref.YIELD_AUTO_PASS_NO_ACTIONS);
        if (!prefValue) {
            return false;
        }
        // Interrupt conditions still break through (attackers, targeting, etc.)
        if (shouldInterruptYield(player)) {
            return false;
        }
        // Respect phase-skip settings: pass through unmarked phases even if
        // the player has actions. Auto-pass should be additive to phase-skip,
        // not cause stops at phases the user explicitly set to skip.
        GameView gv = gui.getGameView();
        if (gv != null && gv.getStack() != null && gv.getStack().isEmpty()) {
            PlayerView turnPlayer = gv.getPlayerTurn();
            forge.game.phase.PhaseType phase = gv.getPhase();
            if (turnPlayer != null && phase != null
                    && gui.isUiSetToSkipPhase(turnPlayer, phase)) {
                return true;
            }
        }
        return !player.hasAvailableActions();
    }

    private boolean getInterruptPref(ForgePreferences.FPref pref) {
        forge.interfaces.IGameController controller = gui.getGameController();
        if (controller == null) {
            return "true".equals(pref.getDefault());
        }
        return controller.getYieldInterruptPref(pref);
    }

    public void updateAutoPassPrompt(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player);

        if (autoPassUntilEndOfTurn.contains(player)) {
            gui.cancelAwaitNextInput();
            gui.showPromptMessage(player, Localizer.getInstance().getMessage("lblYieldingUntilEndOfTurn"));
            gui.updateButtons(player, false, true, false);
            return;
        }

        YieldState state = yieldStates.get(player);
        if (state != null && state.mode != null && state.mode != YieldMode.NONE) {
            YieldMode mode = state.mode;
            gui.cancelAwaitNextInput();
            Localizer loc = Localizer.getInstance();
            String message = switch (mode) {
                case UNTIL_NEXT_PHASE -> loc.getMessage("lblYieldingUntilNextPhase");
                case UNTIL_STACK_CLEARS -> loc.getMessage("lblYieldingUntilStackClears");
                case UNTIL_END_OF_TURN -> loc.getMessage("lblYieldingUntilEndOfTurn");
                case UNTIL_YOUR_NEXT_TURN -> loc.getMessage("lblYieldingUntilYourNextTurn");
                case UNTIL_BEFORE_COMBAT -> loc.getMessage("lblYieldingUntilBeforeCombat");
                case UNTIL_END_STEP -> loc.getMessage("lblYieldingUntilEndStep");
                case UNTIL_END_STEP_BEFORE_YOUR_TURN -> loc.getMessage("lblYieldingUntilBeforeYourTurn");
                default -> "";
            };
            gui.showPromptMessage(player, message);
            gui.updateButtons(player, false, true, false);
            return;
        }

        // Persistent auto-pass: clear stale prompt from previous input (e.g. Pay Mana Cost).
        if (isAutoPassingNoActions(player)) {
            gui.cancelAwaitNextInput();
            gui.showPromptMessage(player, Localizer.getInstance().getMessage("lblAutoPassingNoActions"));
            gui.updateButtons(player, false, false, false);
        }
    }

    /** Returns true if a new mode was activated; false if cleared, rejected, or feature disabled. */
    public boolean setYieldMode(PlayerView player, final YieldMode mode) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        if (!isYieldExperimentalEnabled()) {
            // Fall back to legacy behavior for UNTIL_END_OF_TURN
            if (mode == YieldMode.UNTIL_END_OF_TURN) {
                autoPassUntilEndOfTurn.add(player);
                return true;
            }
            return false;
        }

        if (mode == null || mode == YieldMode.NONE) {
            clearYieldMode(player);
            return false;
        }

        GameView gameView = gui.getGameView();

        // Reject UNTIL_STACK_CLEARS on empty stack BEFORE mutating, so rejection leaves state untouched.
        if (mode == YieldMode.UNTIL_STACK_CLEARS && gameView != null
                && (gameView.getStack() == null || gameView.getStack().isEmpty())) {
            return false;
        }

        // Legacy check in shouldAutoYieldForPlayer runs first; clear it so it doesn't override experimental mode.
        autoPassUntilEndOfTurn.remove(player);

        // Bare state on null gameView; lazy-init paths in shouldAutoYieldForPlayer fill timing fields next pass.
        if (gameView == null) {
            yieldStates.put(player, YieldState.of(mode));
            return true;
        }

        forge.game.phase.PhaseType phase = gameView.getPhase();
        int currentTurn = gameView.getTurn();
        PlayerView currentPlayerTurn = gameView.getPlayerTurn();

        YieldState state = switch (mode) {
            case UNTIL_NEXT_PHASE -> YieldState.of(mode).withStartPhase(phase);
            case UNTIL_END_OF_TURN -> YieldState.of(mode).withStartTurn(currentTurn);
            case UNTIL_BEFORE_COMBAT -> YieldState.of(mode)
                .withStartTurn(currentTurn)
                .withStartedAtOrAfterPhase(isAtOrAfterCombat(phase));
            case UNTIL_END_STEP -> YieldState.of(mode)
                .withStartTurn(currentTurn)
                .withStartedAtOrAfterPhase(isAtOrAfterEndStep(phase));
            case UNTIL_YOUR_NEXT_TURN -> YieldState.of(mode)
                .withStartedDuringOurTurn(currentPlayerTurn != null && currentPlayerTurn.equals(player));
            case UNTIL_END_STEP_BEFORE_YOUR_TURN -> YieldState.of(mode)
                .withStartTurn(currentTurn)
                .withStartedAtOrAfterPhase(isAtOrAfterEndStep(phase));
            default -> YieldState.of(mode);
        };
        yieldStates.put(player, state);
        return true;
    }

    public void clearYieldMode(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        clearYieldModeInternal(player);

        gui.showPromptMessage(player, "");
        gui.updateButtons(player, false, false, false);
        gui.awaitNextInput();

        // Notify client to update its local yield state (for network play)
        gui.syncYieldMode(player, YieldMode.NONE);
    }

    /** No callbacks — used on sync from server to avoid recursive loops. */
    public void setYieldModeSilent(PlayerView player, YieldMode mode) {
        player = TrackableTypes.PlayerViewType.lookup(player);
        if (mode == null || mode == YieldMode.NONE) {
            clearYieldModeInternal(player);
            return;
        }
        autoPassUntilEndOfTurn.remove(player);
        yieldStates.put(player, YieldState.of(mode));
    }

    private void clearYieldModeInternal(PlayerView player) {
        yieldStates.remove(player);
        autoPassUntilEndOfTurn.remove(player); // Legacy compatibility
    }

    public YieldMode getYieldMode(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player);
        if (autoPassUntilEndOfTurn.contains(player)) {
            return YieldMode.UNTIL_END_OF_TURN;
        }
        YieldState state = yieldStates.get(player);
        return state != null && state.mode != null ? state.mode : YieldMode.NONE;
    }

    public boolean shouldAutoYieldForPlayer(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player);
        if (autoPassUntilEndOfTurn.contains(player)) {
            return true;
        }

        if (!isYieldExperimentalEnabled()) {
            return false;
        }

        YieldState state = yieldStates.get(player);
        if (state == null || state.mode == null || state.mode == YieldMode.NONE) {
            return false;
        }

        if (shouldInterruptYield(player)) {
            clearYieldMode(player);
            return false;
        }

        GameView gameView = gui.getGameView();
        if (gameView == null) {
            return false;
        }

        // Use network-safe GameView properties instead of gameView.getGame()
        forge.game.phase.PhaseType currentPhase = gameView.getPhase();
        int currentTurn = gameView.getTurn();
        PlayerView currentPlayerTurn = gameView.getPlayerTurn();

        return switch (state.mode) {
            case UNTIL_NEXT_PHASE -> {
                if (state.startPhase == null) {
                    // Lazy-init: gameView was null at set time. Bail in MAIN2 to avoid skipping the stop point.
                    yieldStates.put(player, state.withStartPhase(currentPhase));
                    if (currentPhase == forge.game.phase.PhaseType.MAIN2) {
                        clearYieldMode(player);
                        yield false;
                    }
                    yield true;
                }
                if (currentPhase != state.startPhase) {
                    clearYieldMode(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_STACK_CLEARS -> {
                boolean stackEmpty = gameView.getStack() == null || gameView.getStack().isEmpty();
                if (stackEmpty) {
                    clearYieldMode(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_END_OF_TURN -> {
                if (state.startTurn == null) {
                    yieldStates.put(player, state.withStartTurn(currentTurn));
                    yield true;
                }
                if (currentTurn > state.startTurn) {
                    clearYieldMode(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_YOUR_NEXT_TURN -> {
                boolean isOurTurn = currentPlayerTurn != null && currentPlayerTurn.equals(player);

                if (state.startedDuringOurTurn == null) {
                    state = state.withStartedDuringOurTurn(isOurTurn);
                    yieldStates.put(player, state);
                }

                if (isOurTurn) {
                    // Started during opponent's turn: stop when we reach our turn.
                    // Started during our turn: wait for it to come back (handled below).
                    if (!Boolean.TRUE.equals(state.startedDuringOurTurn)) {
                        clearYieldMode(player);
                        yield false;
                    }
                } else {
                    if (Boolean.TRUE.equals(state.startedDuringOurTurn)) {
                        yieldStates.put(player, state.withStartedDuringOurTurn(false));
                    }
                }
                yield true;
            }
            case UNTIL_BEFORE_COMBAT -> {
                if (state.startTurn == null) {
                    state = state.withStartTurn(currentTurn)
                                 .withStartedAtOrAfterPhase(isAtOrAfterCombat(currentPhase));
                    yieldStates.put(player, state);
                }

                // Stop on different turn, or same turn if started before combat.
                if (isAtOrAfterCombat(currentPhase)) {
                    boolean differentTurn = currentTurn > state.startTurn;
                    boolean sameTurnButStartedBeforeCombat = (currentTurn == state.startTurn.intValue()) && !Boolean.TRUE.equals(state.startedAtOrAfterPhase);

                    if (differentTurn || sameTurnButStartedBeforeCombat) {
                        clearYieldMode(player);
                        yield false;
                    }
                }
                yield true;
            }
            case UNTIL_END_STEP -> {
                if (state.startTurn == null) {
                    state = state.withStartTurn(currentTurn)
                                 .withStartedAtOrAfterPhase(isAtOrAfterEndStep(currentPhase));
                    yieldStates.put(player, state);
                }

                // Stop on different turn, or same turn if started before end step.
                if (isAtOrAfterEndStep(currentPhase)) {
                    boolean differentTurn = currentTurn > state.startTurn;
                    boolean sameTurnButStartedBeforeEndStep = (currentTurn == state.startTurn.intValue()) && !Boolean.TRUE.equals(state.startedAtOrAfterPhase);

                    if (differentTurn || sameTurnButStartedBeforeEndStep) {
                        clearYieldMode(player);
                        yield false;
                    }
                }
                yield true;
            }
            case UNTIL_END_STEP_BEFORE_YOUR_TURN -> {
                if (state.startTurn == null) {
                    yieldStates.put(player, state.withStartTurn(currentTurn)
                        .withStartedAtOrAfterPhase(isAtOrAfterEndStep(currentPhase)));
                    yield true;
                }

                // Stop at the end step of the player immediately before us in turn order.
                if (isAtOrAfterEndStep(currentPhase)) {
                    boolean differentTurn = currentTurn > state.startTurn;
                    boolean sameTurnButStartedBeforeEndStep = (currentTurn == state.startTurn.intValue())
                        && !Boolean.TRUE.equals(state.startedAtOrAfterPhase);

                    if (differentTurn || sameTurnButStartedBeforeEndStep) {
                        if (isPlayerBeforeUs(currentPlayerTurn, player, gameView)) {
                            clearYieldMode(player);
                            yield false;
                        }
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    private boolean shouldInterruptYield(final PlayerView player) {
        GameView gameView = gui.getGameView();
        if (gameView == null) {
            return false;
        }

        forge.game.phase.PhaseType phase = gameView.getPhase();
        PlayerView currentPlayerTurn = gameView.getPlayerTurn();
        forge.game.combat.CombatView combatView = gameView.getCombat();

        if (getInterruptPref(ForgePreferences.FPref.YIELD_INTERRUPT_ON_ATTACKERS)) {
            if (phase == forge.game.phase.PhaseType.COMBAT_DECLARE_ATTACKERS &&
                combatView != null && isBeingAttacked(combatView, player)) {
                return true;
            }
        }

        if (getInterruptPref(ForgePreferences.FPref.YIELD_INTERRUPT_ON_TARGETING)) {
            forge.util.collect.FCollectionView<forge.game.spellability.StackItemView> stack = gameView.getStack();
            if (stack != null) {
                for (forge.game.spellability.StackItemView si : stack) {
                    if (targetsPlayerOrPermanents(si, player)) {
                        return true;
                    }
                }
            }
        }

        if (getInterruptPref(ForgePreferences.FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL)) {
            forge.game.spellability.StackItemView topItem = gameView.peekStack();
            if (topItem != null) {
                PlayerView activatingPlayer = topItem.getActivatingPlayer();
                boolean isOpponent = activatingPlayer != null && !activatingPlayer.equals(player);
                if (isOpponent && targetsPlayerOrPermanents(topItem, player)) {
                    return true;
                }
            }
        }

        if (getInterruptPref(ForgePreferences.FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL)) {
            if (hasMassRemovalOnStack(gameView, player)) {
                return true;
            }
        }

        if (getInterruptPref(ForgePreferences.FPref.YIELD_INTERRUPT_ON_TRIGGERS)) {
            forge.util.collect.FCollectionView<forge.game.spellability.StackItemView> stack = gameView.getStack();
            if (stack != null) {
                for (forge.game.spellability.StackItemView si : stack) {
                    if (si.isTrigger()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isBeingAttacked(forge.game.combat.CombatView combatView, PlayerView player) {
        if (combatView == null) {
            return false;
        }

        forge.util.collect.FCollection<CardView> attackersOfPlayer = combatView.getAttackersOf(player);
        if (attackersOfPlayer != null && !attackersOfPlayer.isEmpty()) {
            return true;
        }

        // Check planeswalkers / battles controlled by the player.
        for (forge.game.GameEntityView defender : combatView.getDefenders()) {
            if (defender instanceof CardView) {
                CardView cardDefender = (CardView) defender;
                PlayerView controller = cardDefender.getController();
                if (controller != null && controller.equals(player)) {
                    forge.util.collect.FCollection<CardView> attackers = combatView.getAttackersOf(defender);
                    if (attackers != null && !attackers.isEmpty()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** Recurses into sub-instances (e.g. Oona, where targeting is in a sub-ability). */
    private boolean targetsPlayerOrPermanents(forge.game.spellability.StackItemView si, PlayerView player) {
        forge.util.collect.FCollectionView<PlayerView> targetPlayers = si.getTargetPlayers();
        if (targetPlayers != null) {
            for (PlayerView target : targetPlayers) {
                if (target.equals(player)) return true;
            }
        }

        forge.util.collect.FCollectionView<CardView> targetCards = si.getTargetCards();
        if (targetCards != null) {
            for (CardView target : targetCards) {
                if (target.getController() != null && target.getController().equals(player)) {
                    return true;
                }
            }
        }

        // Recursively check sub-instances for targeting (handles abilities like Oona)
        forge.game.spellability.StackItemView subInstance = si.getSubInstance();
        if (subInstance != null && targetsPlayerOrPermanents(subInstance, player)) {
            return true;
        }

        return false;
    }

    /** Host-only: walks live engine stack via gameView.getGame(). Opponent spells only. */
    private boolean hasMassRemovalOnStack(GameView gameView, PlayerView player) {
        forge.game.Game game = gameView.getGame();
        if (game == null) {
            return false; // host-only path; defensive
        }
        for (forge.game.spellability.SpellAbilityStackInstance si : game.getStack()) {
            forge.game.player.Player activator = si.getActivatingPlayer();
            if (activator == null || activator.getView().equals(player)) {
                continue;
            }
            if (isMassRemovalInstance(si)) {
                return true;
            }
        }
        return false;
    }

    /** Recurses into sub-instances for modal spells like Farewell. */
    private boolean isMassRemovalInstance(forge.game.spellability.SpellAbilityStackInstance si) {
        forge.game.spellability.SpellAbility sa = si.getSpellAbility();
        if (sa != null && isMassRemovalApi(sa.getApi())) {
            return true;
        }
        forge.game.spellability.SpellAbilityStackInstance subInstance = si.getSubInstance();
        if (subInstance != null && isMassRemovalInstance(subInstance)) {
            return true;
        }
        return false;
    }

    private boolean isMassRemovalApi(forge.game.ability.ApiType api) {
        return api == forge.game.ability.ApiType.DestroyAll
            || api == forge.game.ability.ApiType.DamageAll
            || api == forge.game.ability.ApiType.SacrificeAll
            || api == forge.game.ability.ApiType.ChangeZoneAll;
    }

    private boolean isYieldExperimentalEnabled() {
        return FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.YIELD_EXPERIMENTAL_OPTIONS);
    }

    private boolean isAtOrAfterCombat(forge.game.phase.PhaseType phase) {
        return phase != null &&
            (phase == forge.game.phase.PhaseType.COMBAT_BEGIN || phase.isAfter(forge.game.phase.PhaseType.COMBAT_BEGIN));
    }

    private boolean isAtOrAfterEndStep(forge.game.phase.PhaseType phase) {
        return phase != null &&
            (phase == forge.game.phase.PhaseType.END_OF_TURN || phase == forge.game.phase.PhaseType.CLEANUP);
    }

    /** Player immediately before us in turn order, with wraparound (A's predecessor is the last player). */
    private boolean isPlayerBeforeUs(PlayerView currentPlayerTurn, PlayerView us, GameView gameView) {
        if (currentPlayerTurn == null || us == null) {
            return true; // fallback: stop yielding if we can't determine
        }

        java.util.List<PlayerView> players = new java.util.ArrayList<>(gameView.getPlayers());
        if (players.size() < 2) {
            return true;
        }

        int ourIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).equals(us)) {
                ourIndex = i;
                break;
            }
        }
        if (ourIndex < 0) {
            return true; // fallback
        }

        // The player before us is at (ourIndex - 1 + size) % size
        int prevIndex = (ourIndex - 1 + players.size()) % players.size();
        return players.get(prevIndex).equals(currentPlayerTurn);
    }

    public void removeFromLegacyAutoPass(PlayerView player) {
        autoPassUntilEndOfTurn.remove(player);
    }

    /** Clears all yield state. Called between games so modes don't carry over. */
    public void reset() {
        autoPassUntilEndOfTurn.clear();
        yieldStates.clear();
    }

}
