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

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
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
    private final FButton btnBeforeYourTurn = new FButton(localizer.getMessage("lblYieldBtnBeforeYourTurn"));
    private final FButton btnAutoPass = new FButton(localizer.getMessage("lblYieldBtnAutoPass"));
    private final FButton btnSettings = new FButton(localizer.getMessage("lblSettings"));

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
        btnBeforeYourTurn.setFont(smallFont);
        btnAutoPass.setFont(smallFont);
        btnSettings.setFont(smallFont);

        // Enable highlight mode: blue by default, red when active yield
        btnNextPhase.setUseHighlightMode(true);
        btnClearStack.setUseHighlightMode(true);
        btnCombat.setUseHighlightMode(true);
        btnEndStep.setUseHighlightMode(true);
        btnEndTurn.setUseHighlightMode(true);
        btnYourTurn.setUseHighlightMode(true);
        btnBeforeYourTurn.setUseHighlightMode(true);
        btnAutoPass.setUseHighlightMode(true);
        btnSettings.setUseHighlightMode(true);

        // Set tooltips on yield buttons
        btnNextPhase.setToolTipText(localizer.getMessage("lblYieldBtnNextPhaseTooltip"));
        btnClearStack.setToolTipText(localizer.getMessage("lblYieldBtnClearStackTooltip"));
        btnCombat.setToolTipText(localizer.getMessage("lblYieldBtnCombatTooltip"));
        btnEndStep.setToolTipText(localizer.getMessage("lblYieldBtnEndStepTooltip"));
        btnEndTurn.setToolTipText(localizer.getMessage("lblYieldBtnEndTurnTooltip"));
        btnYourTurn.setToolTipText(localizer.getMessage("lblYieldBtnYourTurnTooltip"));
        btnBeforeYourTurn.setToolTipText(localizer.getMessage("lblYieldBtnBeforeYourTurnTooltip"));
        btnAutoPass.setToolTipText(localizer.getMessage("lblYieldBtnAutoPassTooltip"));
        btnSettings.setToolTipText(localizer.getMessage("lblInterruptSettingsTooltip"));
    }

    @Override
    public void populate() {
        JPanel container = parentCell.getBody();

        boolean largerButtons = FModel.getPreferences().getPrefBoolean(FPref.UI_FOR_TOUCHSCREN);
        String buttonConstraints = largerButtons
            ? "w 10:50%, h 40px:40px:60px"
            : "w 10:50%, hmin 20px";

        // 2-column layout
        container.setLayout(new MigLayout("wrap 2, gap 1px!, insets 2px"));

        // Row 1: Auto-Pass toggle (full width, emphasized at top)
        String fullWidthConstraints = largerButtons
            ? "span 2, w 10:100%, h 40px:40px:60px"
            : "span 2, w 10:100%, hmin 20px";
        container.add(btnAutoPass, "gaptop 2px, " + fullWidthConstraints);

        // Themed separators
        String sepConstraints = "newline, span 2, growx, gaptop 3px, gapbottom 1px";
        javax.swing.JSeparator sep1 = new javax.swing.JSeparator();
        sep1.setForeground(FSkin.getColor(FSkin.Colors.CLR_BORDERS).getColor());
        container.add(sep1, sepConstraints);

        // Yield buttons in game-flow order (2 columns)
        container.add(btnNextPhase, buttonConstraints);
        container.add(btnCombat, buttonConstraints);
        container.add(btnEndStep, buttonConstraints);
        container.add(btnEndTurn, buttonConstraints);
        container.add(btnBeforeYourTurn, buttonConstraints);
        container.add(btnYourTurn, buttonConstraints);
        container.add(btnClearStack, buttonConstraints);

        // Separator before settings — newline forces it below Clear Stack
        javax.swing.JSeparator sep2 = new javax.swing.JSeparator();
        sep2.setForeground(FSkin.getColor(FSkin.Colors.CLR_BORDERS).getColor());
        container.add(sep2, sepConstraints);

        // Settings (full width)
        container.add(btnSettings, fullWidthConstraints);
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
    public FButton getBtnAutoPass() { return btnAutoPass; }
    public FButton getBtnSettings() { return btnSettings; }
    public FButton getBtnBeforeYourTurn() { return btnBeforeYourTurn; }
}
