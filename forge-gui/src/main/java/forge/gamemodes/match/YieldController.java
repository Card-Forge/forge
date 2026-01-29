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
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.trackable.TrackableTypes;
import forge.util.Localizer;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    /**
     * Callback interface for GUI updates and game state access.
     */
    public interface YieldCallback {
        void showPromptMessage(PlayerView player, String message);
        void updateButtons(PlayerView player, boolean ok, boolean cancel, boolean focusOk);
        void awaitNextInput();
        void cancelAwaitNextInput();
        GameView getGameView();
    }

    private final YieldCallback callback;

    // Legacy auto-pass tracking
    private final Set<PlayerView> autoPassUntilEndOfTurn = Sets.newHashSet();

    // Extended yield mode tracking (experimental feature)
    private final Map<PlayerView, YieldMode> playerYieldMode = Maps.newHashMap();
    private final Map<PlayerView, Integer> yieldStartTurn = Maps.newHashMap(); // Track turn when yield was set
    private final Map<PlayerView, Integer> yieldCombatStartTurn = Maps.newHashMap(); // Track turn when combat yield was set
    private final Map<PlayerView, Boolean> yieldCombatStartedAtOrAfterCombat = Maps.newHashMap(); // Was yield set at/after combat?
    private final Map<PlayerView, Integer> yieldEndStepStartTurn = Maps.newHashMap(); // Track turn when end step yield was set
    private final Map<PlayerView, Boolean> yieldEndStepStartedAtOrAfterEndStep = Maps.newHashMap(); // Was yield set at/after end step?
    private final Map<PlayerView, Boolean> yieldYourTurnStartedDuringOurTurn = Maps.newHashMap(); // Was yield set during our turn?
    private final Map<PlayerView, forge.game.phase.PhaseType> yieldNextPhaseStartPhase = Maps.newHashMap(); // Track phase when next phase yield was set

    // Smart suggestion decline tracking (reset each turn)
    private final Map<PlayerView, Set<String>> declinedSuggestionsThisTurn = Maps.newHashMap();
    private final Map<PlayerView, Integer> declinedSuggestionsTurn = Maps.newHashMap();

    /**
     * Create a new YieldController with the given callback for GUI updates.
     * @param callback the callback interface for GUI operations
     */
    public YieldController(YieldCallback callback) {
        this.callback = callback;
    }

    /**
     * Automatically pass priority until reaching the Cleanup phase of the current turn.
     * This is the legacy auto-pass behavior.
     */
    public void autoPassUntilEndOfTurn(final PlayerView player) {
        autoPassUntilEndOfTurn.add(player);
    }

    /**
     * Cancel auto-pass for the given player.
     */
    public void autoPassCancel(final PlayerView player) {
        if (!autoPassUntilEndOfTurn.remove(player)) {
            return;
        }

        // Prevent prompt getting stuck on yielding message while actually waiting for next input opportunity
        callback.showPromptMessage(player, "");
        callback.updateButtons(player, false, false, false);
        callback.awaitNextInput();
    }

    /**
     * Check if auto-pass is active for the given player (legacy or experimental).
     */
    public boolean mayAutoPass(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player);
        // Check legacy auto-pass first
        if (autoPassUntilEndOfTurn.contains(player)) {
            return true;
        }
        // Check experimental yield system
        return shouldAutoYieldForPlayer(player);
    }

    /**
     * Update the prompt message to show current yield status.
     */
    public void updateAutoPassPrompt(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player);

        // Check legacy auto-pass first
        if (autoPassUntilEndOfTurn.contains(player)) {
            callback.cancelAwaitNextInput();
            String cancelKey = getCancelShortcutDisplayText();
            callback.showPromptMessage(player, Localizer.getInstance().getMessage("lblYieldingUntilEndOfTurn", cancelKey));
            callback.updateButtons(player, false, true, false);
            return;
        }

        // Check experimental yield modes
        YieldMode mode = playerYieldMode.get(player);
        if (mode != null && mode != YieldMode.NONE) {
            callback.cancelAwaitNextInput();
            Localizer loc = Localizer.getInstance();
            String cancelKey = getCancelShortcutDisplayText();
            String message = switch (mode) {
                case UNTIL_NEXT_PHASE -> loc.getMessage("lblYieldingUntilNextPhase", cancelKey);
                case UNTIL_STACK_CLEARS -> loc.getMessage("lblYieldingUntilStackClears", cancelKey);
                case UNTIL_END_OF_TURN -> loc.getMessage("lblYieldingUntilEndOfTurn", cancelKey);
                case UNTIL_YOUR_NEXT_TURN -> loc.getMessage("lblYieldingUntilYourNextTurn", cancelKey);
                case UNTIL_BEFORE_COMBAT -> loc.getMessage("lblYieldingUntilBeforeCombat", cancelKey);
                case UNTIL_END_STEP -> loc.getMessage("lblYieldingUntilEndStep", cancelKey);
                default -> "";
            };
            callback.showPromptMessage(player, message);
            callback.updateButtons(player, false, true, false);
        }
    }

    /**
     * Set the yield mode for a player.
     */
    public void setYieldMode(PlayerView player, final YieldMode mode) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        if (!isYieldExperimentalEnabled()) {
            // Fall back to legacy behavior for UNTIL_END_OF_TURN
            if (mode == YieldMode.UNTIL_END_OF_TURN) {
                autoPassUntilEndOfTurn.add(player);
            }
            return;
        }

        if (mode == YieldMode.NONE) {
            clearYieldMode(player);
            return;
        }

        playerYieldMode.put(player, mode);
        GameView gameView = callback.getGameView();

        // Track current phase for UNTIL_NEXT_PHASE mode
        if (mode == YieldMode.UNTIL_NEXT_PHASE && gameView != null && gameView.getGame() != null) {
            forge.game.phase.PhaseHandler ph = gameView.getGame().getPhaseHandler();
            yieldNextPhaseStartPhase.put(player, ph.getPhase());
        }
        // Track turn number for UNTIL_END_OF_TURN mode
        if (mode == YieldMode.UNTIL_END_OF_TURN && gameView != null && gameView.getGame() != null) {
            yieldStartTurn.put(player, gameView.getGame().getPhaseHandler().getTurn());
        }
        // Track turn and phase state for UNTIL_BEFORE_COMBAT mode
        if (mode == YieldMode.UNTIL_BEFORE_COMBAT && gameView != null && gameView.getGame() != null) {
            forge.game.phase.PhaseHandler ph = gameView.getGame().getPhaseHandler();
            yieldCombatStartTurn.put(player, ph.getTurn());
            forge.game.phase.PhaseType phase = ph.getPhase();
            boolean atOrAfterCombat = phase != null &&
                (phase == forge.game.phase.PhaseType.COMBAT_BEGIN || phase.isAfter(forge.game.phase.PhaseType.COMBAT_BEGIN));
            yieldCombatStartedAtOrAfterCombat.put(player, atOrAfterCombat);
        }
        // Track turn and phase state for UNTIL_END_STEP mode
        if (mode == YieldMode.UNTIL_END_STEP && gameView != null && gameView.getGame() != null) {
            forge.game.phase.PhaseHandler ph = gameView.getGame().getPhaseHandler();
            yieldEndStepStartTurn.put(player, ph.getTurn());
            forge.game.phase.PhaseType phase = ph.getPhase();
            boolean atOrAfterEndStep = phase != null &&
                (phase == forge.game.phase.PhaseType.END_OF_TURN || phase == forge.game.phase.PhaseType.CLEANUP);
            yieldEndStepStartedAtOrAfterEndStep.put(player, atOrAfterEndStep);
        }
        // Track if UNTIL_YOUR_NEXT_TURN was started during our turn
        if (mode == YieldMode.UNTIL_YOUR_NEXT_TURN && gameView != null && gameView.getGame() != null) {
            forge.game.phase.PhaseHandler ph = gameView.getGame().getPhaseHandler();
            forge.game.player.Player playerObj = gameView.getGame().getPlayer(player);
            boolean isOurTurn = ph.getPlayerTurn().equals(playerObj);
            yieldYourTurnStartedDuringOurTurn.put(player, isOurTurn);
        }
    }

    /**
     * Clear yield mode for a player.
     */
    public void clearYieldMode(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        playerYieldMode.remove(player);
        yieldStartTurn.remove(player);
        yieldCombatStartTurn.remove(player);
        yieldCombatStartedAtOrAfterCombat.remove(player);
        yieldEndStepStartTurn.remove(player);
        yieldEndStepStartedAtOrAfterEndStep.remove(player);
        yieldYourTurnStartedDuringOurTurn.remove(player);
        yieldNextPhaseStartPhase.remove(player);
        autoPassUntilEndOfTurn.remove(player); // Legacy compatibility

        callback.showPromptMessage(player, "");
        callback.updateButtons(player, false, false, false);
        callback.awaitNextInput();
    }

    /**
     * Get the current yield mode for a player.
     */
    public YieldMode getYieldMode(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        // Check legacy auto-pass first
        if (autoPassUntilEndOfTurn.contains(player)) {
            return YieldMode.UNTIL_END_OF_TURN;
        }
        YieldMode mode = playerYieldMode.get(player);
        return mode != null ? mode : YieldMode.NONE;
    }

    /**
     * Check if auto-yield should be active for a player based on current game state.
     */
    public boolean shouldAutoYieldForPlayer(PlayerView player) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        // Check legacy system first
        if (autoPassUntilEndOfTurn.contains(player)) {
            return true;
        }

        if (!isYieldExperimentalEnabled()) {
            return false;
        }

        YieldMode mode = playerYieldMode.get(player);
        if (mode == null || mode == YieldMode.NONE) {
            return false;
        }

        // Check interrupt conditions
        if (shouldInterruptYield(player)) {
            clearYieldMode(player);
            return false;
        }

        GameView gameView = callback.getGameView();
        if (gameView == null || gameView.getGame() == null) {
            return false;
        }

        forge.game.Game game = gameView.getGame();
        forge.game.phase.PhaseHandler ph = game.getPhaseHandler();

        return switch (mode) {
            case UNTIL_NEXT_PHASE -> {
                forge.game.phase.PhaseType startPhase = yieldNextPhaseStartPhase.get(player);
                forge.game.phase.PhaseType currentPhase = ph.getPhase();
                if (startPhase == null) {
                    yieldNextPhaseStartPhase.put(player, currentPhase);
                    yield true;
                }
                if (currentPhase != startPhase) {
                    clearYieldMode(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_STACK_CLEARS -> {
                boolean stackEmpty = game.getStack().isEmpty() && !game.getStack().hasSimultaneousStackEntries();
                if (stackEmpty) {
                    clearYieldMode(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_END_OF_TURN -> {
                // Yield until end of the turn when yield was set - clear when turn number changes
                Integer startTurn = yieldStartTurn.get(player);
                int currentTurn = ph.getTurn();
                if (startTurn == null) {
                    // Turn wasn't tracked when yield was set - track it now
                    yieldStartTurn.put(player, currentTurn);
                    yield true;
                }
                if (currentTurn > startTurn) {
                    clearYieldMode(player);
                    yield false;
                }
                yield true;
            }
            case UNTIL_YOUR_NEXT_TURN -> {
                // Yield until our turn starts
                forge.game.player.Player playerObj = game.getPlayer(player);
                boolean isOurTurn = ph.getPlayerTurn().equals(playerObj);
                Boolean startedDuringOurTurn = yieldYourTurnStartedDuringOurTurn.get(player);

                if (startedDuringOurTurn == null) {
                    // Tracking wasn't set - initialize it now
                    yieldYourTurnStartedDuringOurTurn.put(player, isOurTurn);
                    startedDuringOurTurn = isOurTurn;
                }

                if (isOurTurn) {
                    // If we started during our turn, we need to wait until it's our turn AGAIN
                    // (i.e., we left our turn and came back)
                    // If we started during opponent's turn, stop when we reach our turn
                    if (!Boolean.TRUE.equals(startedDuringOurTurn)) {
                        clearYieldMode(player);
                        yield false;
                    }
                } else {
                    // Not our turn - if we started during our turn, mark that we've left it
                    if (Boolean.TRUE.equals(startedDuringOurTurn)) {
                        // We've left our turn, now waiting for it to come back
                        yieldYourTurnStartedDuringOurTurn.put(player, false);
                    }
                }
                yield true;
            }
            case UNTIL_BEFORE_COMBAT -> {
                forge.game.phase.PhaseType phase = ph.getPhase();
                Integer startTurn = yieldCombatStartTurn.get(player);
                Boolean startedAtOrAfterCombat = yieldCombatStartedAtOrAfterCombat.get(player);
                int currentTurn = ph.getTurn();

                if (startTurn == null) {
                    // Tracking wasn't set - initialize it now
                    yieldCombatStartTurn.put(player, currentTurn);
                    boolean atOrAfterCombat = phase != null &&
                        (phase == forge.game.phase.PhaseType.COMBAT_BEGIN || phase.isAfter(forge.game.phase.PhaseType.COMBAT_BEGIN));
                    yieldCombatStartedAtOrAfterCombat.put(player, atOrAfterCombat);
                    startTurn = currentTurn;
                    startedAtOrAfterCombat = atOrAfterCombat;
                }

                // Check if we should stop: we're at or past combat on a DIFFERENT turn than when we started,
                // OR we're at combat on the SAME turn but we started BEFORE combat
                boolean atOrAfterCombatNow = phase != null &&
                    (phase == forge.game.phase.PhaseType.COMBAT_BEGIN || phase.isAfter(forge.game.phase.PhaseType.COMBAT_BEGIN));

                if (atOrAfterCombatNow) {
                    boolean differentTurn = currentTurn > startTurn;
                    boolean sameTurnButStartedBeforeCombat = (currentTurn == startTurn.intValue()) && !Boolean.TRUE.equals(startedAtOrAfterCombat);

                    if (differentTurn || sameTurnButStartedBeforeCombat) {
                        clearYieldMode(player);
                        yield false;
                    }
                }
                yield true;
            }
            case UNTIL_END_STEP -> {
                forge.game.phase.PhaseType phase = ph.getPhase();
                Integer startTurn = yieldEndStepStartTurn.get(player);
                Boolean startedAtOrAfterEndStep = yieldEndStepStartedAtOrAfterEndStep.get(player);
                int currentTurn = ph.getTurn();

                if (startTurn == null) {
                    // Tracking wasn't set - initialize it now
                    yieldEndStepStartTurn.put(player, currentTurn);
                    boolean atOrAfterEndStep = phase != null &&
                        (phase == forge.game.phase.PhaseType.END_OF_TURN || phase == forge.game.phase.PhaseType.CLEANUP);
                    yieldEndStepStartedAtOrAfterEndStep.put(player, atOrAfterEndStep);
                    startTurn = currentTurn;
                    startedAtOrAfterEndStep = atOrAfterEndStep;
                }

                // Check if we should stop: we're at or past end step on a DIFFERENT turn than when we started,
                // OR we're at end step on the SAME turn but we started BEFORE end step
                boolean atOrAfterEndStepNow = phase != null &&
                    (phase == forge.game.phase.PhaseType.END_OF_TURN || phase == forge.game.phase.PhaseType.CLEANUP);

                if (atOrAfterEndStepNow) {
                    boolean differentTurn = currentTurn > startTurn;
                    boolean sameTurnButStartedBeforeEndStep = (currentTurn == startTurn.intValue()) && !Boolean.TRUE.equals(startedAtOrAfterEndStep);

                    if (differentTurn || sameTurnButStartedBeforeEndStep) {
                        clearYieldMode(player);
                        yield false;
                    }
                }
                yield true;
            }
            default -> false;
        };
    }

    /**
     * Check if yield should be interrupted based on game conditions.
     */
    private boolean shouldInterruptYield(final PlayerView player) {
        GameView gameView = callback.getGameView();
        if (gameView == null || gameView.getGame() == null) {
            return false;
        }

        forge.game.Game game = gameView.getGame();
        forge.game.player.Player p = game.getPlayer(player);
        if (p == null) {
            return false; // Can't determine player, don't interrupt
        }
        ForgePreferences prefs = FModel.getPreferences();
        forge.game.phase.PhaseType phase = game.getPhaseHandler().getPhase();

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_ATTACKERS)) {
            // Only interrupt if there are creatures attacking THIS player or their planeswalkers/battles
            if (phase == forge.game.phase.PhaseType.COMBAT_DECLARE_ATTACKERS &&
                game.getCombat() != null && isBeingAttacked(game, p)) {
                return true;
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_BLOCKERS)) {
            // Only interrupt if there are creatures attacking THIS player or their planeswalkers/battles
            if (phase == forge.game.phase.PhaseType.COMBAT_DECLARE_BLOCKERS &&
                game.getCombat() != null && isBeingAttacked(game, p)) {
                return true;
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_TARGETING)) {
            for (forge.game.spellability.StackItemView si : gameView.getStack()) {
                if (targetsPlayerOrPermanents(si, p)) {
                    return true;
                }
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_OPPONENT_SPELL)) {
            if (!game.getStack().isEmpty()) {
                forge.game.spellability.SpellAbility topSa = game.getStack().peekAbility();
                // Exclude triggered abilities - if they target you, the "targeting" setting handles that
                if (topSa != null && !topSa.isTrigger() && !topSa.getActivatingPlayer().equals(p)) {
                    return true;
                }
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_COMBAT)) {
            if (phase == forge.game.phase.PhaseType.COMBAT_BEGIN) {
                YieldMode mode = playerYieldMode.get(player);
                // Don't interrupt UNTIL_END_OF_TURN on our own turn
                if (!(mode == YieldMode.UNTIL_END_OF_TURN && game.getPhaseHandler().getPlayerTurn().equals(p))) {
                    return true;
                }
            }
        }

        if (prefs.getPrefBoolean(ForgePreferences.FPref.YIELD_INTERRUPT_ON_MASS_REMOVAL)) {
            if (hasMassRemovalOnStack(game, p)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if the player is being attacked (directly or via planeswalkers/battles).
     */
    private boolean isBeingAttacked(forge.game.Game game, forge.game.player.Player p) {
        forge.game.combat.Combat combat = game.getCombat();
        if (combat == null) {
            return false;
        }

        // Check if player is being attacked directly
        if (!combat.getAttackersOf(p).isEmpty()) {
            return true;
        }

        // Check if any planeswalkers or battles controlled by the player are being attacked
        for (forge.game.GameEntity defender : combat.getDefenders()) {
            if (defender instanceof forge.game.card.Card) {
                forge.game.card.Card card = (forge.game.card.Card) defender;
                if (card.getController().equals(p) && !combat.getAttackersOf(defender).isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if a stack item targets the player or their permanents.
     */
    private boolean targetsPlayerOrPermanents(forge.game.spellability.StackItemView si, forge.game.player.Player p) {
        PlayerView pv = p.getView();

        for (PlayerView target : si.getTargetPlayers()) {
            if (target.equals(pv)) return true;
        }

        for (CardView target : si.getTargetCards()) {
            if (target.getController() != null && target.getController().equals(pv)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if there's a mass removal spell on the stack that could affect the player's permanents.
     * Only interrupts if the spell was cast by an opponent AND the player has permanents that match.
     */
    private boolean hasMassRemovalOnStack(forge.game.Game game, forge.game.player.Player p) {
        if (game.getStack().isEmpty()) {
            return false;
        }

        for (forge.game.spellability.SpellAbilityStackInstance si : game.getStack()) {
            forge.game.spellability.SpellAbility sa = si.getSpellAbility();
            if (sa == null) continue;

            // Only interrupt for opponent's spells
            if (sa.getActivatingPlayer() == null || sa.getActivatingPlayer().equals(p)) {
                continue;
            }

            // Check if this is a mass removal spell type
            if (isMassRemovalSpell(sa, game, p)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine if a spell ability is a mass removal effect that could affect the player.
     */
    private boolean isMassRemovalSpell(forge.game.spellability.SpellAbility sa, forge.game.Game game, forge.game.player.Player p) {
        forge.game.ability.ApiType api = sa.getApi();
        if (api == null) {
            return false;
        }

        // Check the main ability and all sub-abilities (for modal spells like Farewell)
        forge.game.spellability.SpellAbility current = sa;
        while (current != null) {
            if (checkSingleAbilityForMassRemoval(current, game, p)) {
                return true;
            }
            current = current.getSubAbility();
        }

        return false;
    }

    /**
     * Check if a single ability (not including sub-abilities) is mass removal affecting the player.
     */
    private boolean checkSingleAbilityForMassRemoval(forge.game.spellability.SpellAbility sa, forge.game.Game game, forge.game.player.Player p) {
        forge.game.ability.ApiType api = sa.getApi();
        if (api == null) {
            return false;
        }

        String apiName = api.name();

        // DestroyAll - Wrath of God, Day of Judgment, Damnation
        if ("DestroyAll".equals(apiName)) {
            return playerHasMatchingPermanents(sa, game, p, "ValidCards");
        }

        // ChangeZoneAll with Destination=Exile or Graveyard - Farewell, Merciless Eviction
        if ("ChangeZoneAll".equals(apiName)) {
            String destination = sa.getParam("Destination");
            if ("Exile".equals(destination) || "Graveyard".equals(destination)) {
                // Check Origin - only care about Battlefield
                String origin = sa.getParam("Origin");
                if (origin != null && origin.contains("Battlefield")) {
                    return playerHasMatchingPermanents(sa, game, p, "ChangeType");
                }
            }
        }

        // DamageAll - Blasphemous Act, Chain Reaction
        if ("DamageAll".equals(apiName)) {
            return playerHasMatchingPermanents(sa, game, p, "ValidCards");
        }

        // SacrificeAll - All Is Dust, Bane of Progress
        if ("SacrificeAll".equals(apiName)) {
            return playerHasMatchingPermanents(sa, game, p, "ValidCards");
        }

        return false;
    }

    /**
     * Check if the player has any permanents that match the spell's filter parameter.
     */
    private boolean playerHasMatchingPermanents(forge.game.spellability.SpellAbility sa, forge.game.Game game, forge.game.player.Player p, String filterParam) {
        String validFilter = sa.getParam(filterParam);

        // Get all permanents controlled by the player
        forge.game.card.CardCollectionView playerPermanents = p.getCardsIn(forge.game.zone.ZoneType.Battlefield);
        if (playerPermanents.isEmpty()) {
            return false;  // No permanents = no reason to interrupt
        }

        // If no filter specified, assume it affects all permanents
        if (validFilter == null || validFilter.isEmpty()) {
            return true;
        }

        // Check if any of the player's permanents match the filter
        for (forge.game.card.Card card : playerPermanents) {
            try {
                if (card.isValid(validFilter.split(","), sa.getActivatingPlayer(), sa.getHostCard(), sa)) {
                    return true;
                }
            } catch (Exception e) {
                // If validation fails, be conservative and assume it might affect us
                return true;
            }
        }

        return false;
    }

    /**
     * Check if experimental yield options are enabled in preferences.
     */
    private boolean isYieldExperimentalEnabled() {
        return FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.YIELD_EXPERIMENTAL_OPTIONS);
    }

    /**
     * Get the total number of players in the game.
     */
    public int getPlayerCount() {
        GameView gameView = callback.getGameView();
        return gameView != null && gameView.getGame() != null
            ? gameView.getGame().getPlayers().size()
            : 0;
    }

    /**
     * Mark a suggestion as declined for the current turn.
     */
    public void declineSuggestion(PlayerView player, String suggestionType) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        GameView gameView = callback.getGameView();
        if (gameView == null || gameView.getGame() == null) return;

        int currentTurn = gameView.getGame().getPhaseHandler().getTurn();
        Integer storedTurn = declinedSuggestionsTurn.get(player);

        // Reset if turn changed
        if (storedTurn == null || storedTurn != currentTurn) {
            declinedSuggestionsThisTurn.put(player, Sets.newHashSet());
            declinedSuggestionsTurn.put(player, currentTurn);
        }

        declinedSuggestionsThisTurn.get(player).add(suggestionType);
    }

    /**
     * Check if a suggestion has been declined for the current turn.
     */
    public boolean isSuggestionDeclined(PlayerView player, String suggestionType) {
        player = TrackableTypes.PlayerViewType.lookup(player); // ensure we use the correct player instance
        GameView gameView = callback.getGameView();
        if (gameView == null || gameView.getGame() == null) return false;

        int currentTurn = gameView.getGame().getPhaseHandler().getTurn();
        Integer storedTurn = declinedSuggestionsTurn.get(player);

        if (storedTurn == null || storedTurn != currentTurn) {
            return false; // Turn changed, reset
        }

        Set<String> declined = declinedSuggestionsThisTurn.get(player);
        return declined != null && declined.contains(suggestionType);
    }

    /**
     * Check if the legacy auto-pass is in the set (for AbstractGuiGame internal use).
     */
    public boolean isInLegacyAutoPass(PlayerView player) {
        return autoPassUntilEndOfTurn.contains(player);
    }

    /**
     * Remove a player from legacy auto-pass (for AbstractGuiGame internal use).
     */
    public void removeFromLegacyAutoPass(PlayerView player) {
        autoPassUntilEndOfTurn.remove(player);
    }

    /**
     * Get the display text for the yield cancel keyboard shortcut.
     * @return Human-readable shortcut text, e.g., "Escape" or "Ctrl+Escape"
     */
    public String getCancelShortcutDisplayText() {
        String codeString = FModel.getPreferences().getPref(FPref.SHORTCUT_YIELD_CANCEL);
        if (codeString == null || codeString.isEmpty()) {
            return "";
        }
        List<String> codes = new ArrayList<>(Arrays.asList(codeString.trim().split(" ")));
        List<String> displayText = new ArrayList<>();
        for (String s : codes) {
            if (!s.isEmpty()) {
                try {
                    displayText.add(KeyEvent.getKeyText(Integer.parseInt(s)));
                } catch (NumberFormatException e) {
                    displayText.add(s);
                }
            }
        }
        return String.join("+", displayText);
    }
}
