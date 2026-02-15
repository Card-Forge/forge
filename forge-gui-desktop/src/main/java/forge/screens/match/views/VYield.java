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
package forge.screens.match.views;

import javax.swing.JPanel;

import forge.gamemodes.match.YieldController;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.controllers.CYield;
import forge.toolbox.FButton;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of the yield controls panel.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VYield implements IVDoc<CYield> {

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblYieldOptions"));

    // Yield control buttons
    private final FButton btnNextPhase = new FButton(localizer.getMessage("lblYieldBtnNextPhase"));
    private final FButton btnClearStack = new FButton(localizer.getMessage("lblYieldBtnClearStack"));
    private final FButton btnCombat = new FButton(localizer.getMessage("lblYieldBtnCombat"));
    private final FButton btnEndStep = new FButton(localizer.getMessage("lblYieldBtnEndStep"));
    private final FButton btnEndTurn = new FButton(localizer.getMessage("lblYieldBtnEndTurn"));
    private final FButton btnYourTurn = new FButton(localizer.getMessage("lblYieldBtnYourTurn"));

    private final CYield controller;

    public VYield(final CYield controller) {
        this.controller = controller;

        // Use smaller font to fit button text
        java.awt.Font smallFont = FSkin.getBoldFont(11).getBaseFont();
        btnNextPhase.setFont(smallFont);
        btnClearStack.setFont(smallFont);
        btnCombat.setFont(smallFont);
        btnEndStep.setFont(smallFont);
        btnEndTurn.setFont(smallFont);
        btnYourTurn.setFont(smallFont);

        // Enable highlight mode: blue by default, red when active yield
        btnNextPhase.setUseHighlightMode(true);
        btnClearStack.setUseHighlightMode(true);
        btnCombat.setUseHighlightMode(true);
        btnEndStep.setUseHighlightMode(true);
        btnEndTurn.setUseHighlightMode(true);
        btnYourTurn.setUseHighlightMode(true);

        // Set tooltips on yield buttons with dynamic hotkey text
        updateTooltips();
    }

    /**
     * Update button tooltips with current keyboard shortcut bindings.
     * Call this after keyboard shortcuts are changed.
     */
    public void updateTooltips() {
        ForgePreferences prefs = FModel.getPreferences();
        btnNextPhase.setToolTipText(localizer.getMessage("lblYieldBtnNextPhaseTooltip",
            getShortcutDisplayText(prefs.getPref(FPref.SHORTCUT_YIELD_UNTIL_NEXT_PHASE))));
        btnClearStack.setToolTipText(localizer.getMessage("lblYieldBtnClearStackTooltip",
            getShortcutDisplayText(prefs.getPref(FPref.SHORTCUT_YIELD_UNTIL_STACK_CLEARS))));
        btnCombat.setToolTipText(localizer.getMessage("lblYieldBtnCombatTooltip",
            getShortcutDisplayText(prefs.getPref(FPref.SHORTCUT_YIELD_UNTIL_BEFORE_COMBAT))));
        btnEndStep.setToolTipText(localizer.getMessage("lblYieldBtnEndStepTooltip",
            getShortcutDisplayText(prefs.getPref(FPref.SHORTCUT_YIELD_UNTIL_END_STEP))));
        btnEndTurn.setToolTipText(localizer.getMessage("lblYieldBtnEndTurnTooltip",
            getShortcutDisplayText(prefs.getPref(FPref.SHORTCUT_YIELD_UNTIL_END_OF_TURN))));
        btnYourTurn.setToolTipText(localizer.getMessage("lblYieldBtnYourTurnTooltip",
            getShortcutDisplayText(prefs.getPref(FPref.SHORTCUT_YIELD_UNTIL_YOUR_NEXT_TURN))));
    }

    private String getShortcutDisplayText(String codeString) {
        return YieldController.formatShortcutDisplayText(codeString);
    }

    @Override
    public void populate() {
        JPanel container = parentCell.getBody();

        boolean largerButtons = FModel.getPreferences().getPrefBoolean(FPref.UI_FOR_TOUCHSCREN);
        String buttonConstraints = largerButtons
            ? "w 10:33%, h 40px:40px:60px"
            : "w 10:33%, hmin 24px";

        // Two-row layout: 3 buttons on top, 3 on bottom
        container.setLayout(new MigLayout("wrap 3, gap 2px!, insets 3px"));

        // Row 1: Next Phase, Combat, End Step
        container.add(btnNextPhase, buttonConstraints);
        container.add(btnCombat, buttonConstraints);
        container.add(btnEndStep, buttonConstraints);

        // Row 2: End Turn, Your Turn, Clear Stack
        container.add(btnEndTurn, buttonConstraints);
        container.add(btnYourTurn, buttonConstraints);
        container.add(btnClearStack, buttonConstraints);
    }

    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    @Override
    public EDocID getDocumentID() {
        return EDocID.REPORT_YIELD;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CYield getLayoutControl() {
        return controller;
    }

    // Button getters
    public FButton getBtnNextPhase() { return btnNextPhase; }
    public FButton getBtnClearStack() { return btnClearStack; }
    public FButton getBtnCombat() { return btnCombat; }
    public FButton getBtnEndStep() { return btnEndStep; }
    public FButton getBtnEndTurn() { return btnEndTurn; }
    public FButton getBtnYourTurn() { return btnYourTurn; }
}
