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
import forge.gui.framework.ICDoc;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.CMatchUI;
import forge.screens.match.VYieldSettings;
import forge.screens.match.views.VYield;
import forge.util.Localizer;

/**
 * Controls the yield panel in the match UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 */
public class CYield implements ICDoc {

    private final CMatchUI matchUI;
    private final VYield view;

    private final ActionListener actAutoPass = evt -> toggleAutoPass();
    private final ActionListener actSettings = evt -> openSettings();

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
        initButton(view.getBtnAutoPass(), actAutoPass);
        initButton(view.getBtnSettings(), actSettings);

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

    /** Disable auto-pass-no-actions if it's currently on. */
    public void cancelAutoPassIfActive() {
        if (FModel.getPreferences().getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS)) {
            toggleAutoPass();
        }
    }

    private void openSettings() {
        new VYieldSettings(matchUI).showDialog();
    }

    private void toggleAutoPass() {
        ForgePreferences prefs = FModel.getPreferences();
        boolean newState = !prefs.getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS);
        prefs.setPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, newState);
        prefs.save();
        updateYieldButtons();
        if (matchUI == null || matchUI.getGameController() == null) {
            return;
        }
        matchUI.getGameController().setYieldInterruptPref(FPref.YIELD_AUTO_PASS_NO_ACTIONS, newState);
        if (newState) {
            matchUI.getGameController().selectButtonOk();
        }
    }

    public void updateYieldButtons() {
        ForgePreferences prefs = FModel.getPreferences();

        boolean yieldEnabled = prefs.getPrefBoolean(FPref.YIELD_EXPERIMENTAL_OPTIONS)
            && matchUI.isHostYieldEnabled();
        boolean canYield = yieldEnabled && canYieldNow();

        view.getBtnAutoPass().setEnabled(canYield);

        updateActiveYieldHighlight();
    }

    private void updateActiveYieldHighlight() {
        boolean autoPassOn = FModel.getPreferences().getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS);
        view.getBtnAutoPass().setHighlighted(autoPassOn);
        view.getBtnAutoPass().setText(Localizer.getInstance().getMessage(
            autoPassOn ? "lblYieldBtnAutoPassOn" : "lblYieldBtnAutoPass"));
    }

    /** False during mulligan, game-over, no-active-phase, or cleanup. */
    private boolean canYieldNow() {
        GameView gameView = matchUI.getGameView();
        if (gameView == null) {
            return false;
        }
        if (gameView.isGameOver()) {
            return false;
        }
        if (gameView.isMulligan()) {
            return false;
        }
        if (gameView.getTurn() < 1) {
            return false;
        }
        if (gameView.getPhase() == null) {
            return false;
        }
        if (gameView.getPhase() == forge.game.phase.PhaseType.CLEANUP) {
            return false;
        }
        if (matchUI.getGameController() == null) {
            return false;
        }
        return true;
    }
}
