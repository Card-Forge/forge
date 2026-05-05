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

import forge.gamemodes.match.YieldController;
import forge.gui.framework.ICDoc;
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

        updateAutoPassButtonLabel();
    }

    private void initButton(final JButton button, final ActionListener onClick) {
        button.removeActionListener(onClick);
        button.addActionListener(onClick);
    }

    @Override
    public void update() {
        updateAutoPassButtonLabel();
    }

    private void openSettings() {
        new VYieldSettings(matchUI).showDialog();
    }

    public void toggleAutoPass() {
        YieldController.toggleAutoPassNoActions(matchUI != null ? matchUI.getGameController() : null);
        updateAutoPassButtonLabel();
    }

    private void updateAutoPassButtonLabel() {
        boolean autoPassOn = FModel.getPreferences().getPrefBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS);
        view.getBtnAutoPass().setToggled(!autoPassOn);
        view.getBtnAutoPass().setText(Localizer.getInstance().getMessage(
                autoPassOn ? "lblYieldBtnAutoPassOn" : "lblYieldBtnAutoPass"));
    }
}
