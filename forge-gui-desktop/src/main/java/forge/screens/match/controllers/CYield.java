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
package forge.screens.match.controllers;

import java.awt.event.ActionListener;

import javax.swing.JButton;

import forge.game.GameView;
import forge.game.player.PlayerView;
import forge.gamemodes.match.YieldMode;
import forge.gui.framework.ICDoc;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.views.VYield;

/**
 * Controls the yield panel in the match UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public class CYield implements ICDoc {

    private final CMatchUI matchUI;
    private final VYield view;

    // Yield button action listeners
    private final ActionListener actNextPhase = evt -> yieldUntilNextPhase();
    private final ActionListener actClearStack = evt -> yieldUntilStackClears();
    private final ActionListener actCombat = evt -> yieldUntilCombat();
    private final ActionListener actEndStep = evt -> yieldUntilEndStep();
    private final ActionListener actEndTurn = evt -> yieldUntilEndTurn();
    private final ActionListener actYourTurn = evt -> yieldUntilYourTurn();

    public CYield(final CMatchUI matchUI) {
        this.matchUI = matchUI;
        this.view = new VYield(this);
    }

    public final CMatchUI getMatchUI() {
        return matchUI;
    }

    public final VYield getView() {
        return view;
    }

    @Override
    public void register() {
    }

    @Override
    public void initialize() {
        // Initialize button action listeners
        initButton(view.getBtnNextPhase(), actNextPhase);
        initButton(view.getBtnClearStack(), actClearStack);
        initButton(view.getBtnCombat(), actCombat);
        initButton(view.getBtnEndStep(), actEndStep);
        initButton(view.getBtnEndTurn(), actEndTurn);
        initButton(view.getBtnYourTurn(), actYourTurn);

        // Set initial button state
        updateYieldButtons();
    }

    private void initButton(final JButton button, final ActionListener onClick) {
        button.removeActionListener(onClick);
        button.addActionListener(onClick);
    }

    @Override
    public void update() {
        updateYieldButtons();
    }

    /**
     * Toggle yield mode: if the mode is already active, clear it; otherwise activate it.
     * When activating, also pass priority. When clearing, just cancel auto-yield.
     */
    private void toggleYieldMode(YieldMode mode) {
        if (matchUI == null || matchUI.getCurrentPlayer() == null) return;
        PlayerView player = matchUI.getCurrentPlayer();
        if (matchUI.getYieldMode(player) == mode) {
            matchUI.clearYieldMode(player);
        } else {
            matchUI.setYieldMode(player, mode);
            if (matchUI.getGameController() != null) {
                matchUI.getGameController().selectButtonOk();
            }
        }
    }

    // Yield action methods - toggle yield mode on/off
    private void yieldUntilNextPhase() { toggleYieldMode(YieldMode.UNTIL_NEXT_PHASE); }
    private void yieldUntilStackClears() { toggleYieldMode(YieldMode.UNTIL_STACK_CLEARS); }
    private void yieldUntilCombat() { toggleYieldMode(YieldMode.UNTIL_BEFORE_COMBAT); }
    private void yieldUntilEndStep() { toggleYieldMode(YieldMode.UNTIL_END_STEP); }
    private void yieldUntilEndTurn() { toggleYieldMode(YieldMode.UNTIL_END_OF_TURN); }
    private void yieldUntilYourTurn() { toggleYieldMode(YieldMode.UNTIL_YOUR_NEXT_TURN); }

    /**
     * Update yield buttons enabled state based on game state.
     * Buttons are disabled during mulligan, sideboarding, and game over.
     * Active yield mode button is highlighted (toggled).
     */
    public void updateYieldButtons() {
        ForgePreferences prefs = FModel.getPreferences();

        // Check if experimental yield options are enabled
        boolean yieldEnabled = prefs.getPrefBoolean(FPref.YIELD_EXPERIMENTAL_OPTIONS);

        // Check if we can yield (not in mulligan, sideboard, or game over)
        boolean canYield = yieldEnabled && canYieldNow();

        // Enable/disable all yield buttons based on whether we can yield
        view.getBtnNextPhase().setEnabled(canYield);
        view.getBtnCombat().setEnabled(canYield);
        view.getBtnEndStep().setEnabled(canYield);
        view.getBtnEndTurn().setEnabled(canYield);
        view.getBtnYourTurn().setEnabled(canYield);

        // Clear Stack also requires items on stack
        boolean stackHasItems = matchUI.getGameView() != null
            && matchUI.getGameView().getStack() != null
            && !matchUI.getGameView().getStack().isEmpty();
        view.getBtnClearStack().setEnabled(canYield && stackHasItems);

        // Highlight active yield button
        updateActiveYieldHighlight();
    }

    /**
     * Update button highlight state to show the currently active yield mode.
     * Active yield button is highlighted (red), others are normal (blue).
     */
    private void updateActiveYieldHighlight() {
        // Get current yield mode for the current player
        YieldMode currentMode = YieldMode.NONE;
        PlayerView currentPlayer = matchUI.getCurrentPlayer();
        if (currentPlayer != null) {
            currentMode = matchUI.getYieldMode(currentPlayer);
        }

        // Set highlight state based on active yield mode
        // Highlighted = red (active), not highlighted = blue (normal)
        view.getBtnNextPhase().setHighlighted(currentMode == YieldMode.UNTIL_NEXT_PHASE);
        view.getBtnClearStack().setHighlighted(currentMode == YieldMode.UNTIL_STACK_CLEARS);
        view.getBtnCombat().setHighlighted(currentMode == YieldMode.UNTIL_BEFORE_COMBAT);
        view.getBtnEndStep().setHighlighted(currentMode == YieldMode.UNTIL_END_STEP);
        view.getBtnEndTurn().setHighlighted(currentMode == YieldMode.UNTIL_END_OF_TURN);
        view.getBtnYourTurn().setHighlighted(currentMode == YieldMode.UNTIL_YOUR_NEXT_TURN);
    }

    /**
     * Check if we're in a state where yielding makes sense.
     * Returns false during mulligan, sideboarding, game over, cleanup/discard, etc.
     */
    private boolean canYieldNow() {
        GameView gameView = matchUI.getGameView();
        if (gameView == null) {
            return false;
        }

        // Can't yield if game is over
        if (gameView.isGameOver()) {
            return false;
        }

        // Can't yield during mulligan (explicit flag)
        if (gameView.isMulligan()) {
            return false;
        }

        // Can't yield if game hasn't started yet (turn 0 = pre-game/mulligan phase)
        if (gameView.getTurn() < 1) {
            return false;
        }

        // Can't yield if no phase set (game not fully started)
        if (gameView.getPhase() == null) {
            return false;
        }

        // Can't yield during cleanup phase (when discarding to hand size)
        if (gameView.getPhase() == forge.game.phase.PhaseType.CLEANUP) {
            return false;
        }

        // Can't yield if no game controller
        if (matchUI.getGameController() == null) {
            return false;
        }

        return true;
    }
}
