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

    private final FButton btnAutoPass = new FButton(localizer.getMessage("lblYieldBtnAutoPass"));
    private final FButton btnSettings = new FButton("...");

    private final CYield controller;

    public VYield(final CYield controller) {
        this.controller = controller;

        java.awt.Font smallFont = FSkin.getBoldFont(11).getBaseFont();
        btnAutoPass.setFont(smallFont);
        btnSettings.setFont(smallFont);

        btnAutoPass.setUseHighlightMode(true);
        btnSettings.setUseHighlightMode(true);

        btnAutoPass.setToolTipText(localizer.getMessage("lblYieldBtnAutoPassTooltip"));
        btnSettings.setToolTipText(localizer.getMessage("lblInterruptSettingsTooltip"));
    }

    @Override
    public void populate() {
        JPanel container = parentCell.getBody();

        boolean largerButtons = FModel.getPreferences().getPrefBoolean(FPref.UI_FOR_TOUCHSCREN);
        String heightConstraint = largerButtons ? "h 40px:40px:60px" : "hmin 20px";

        container.setLayout(new MigLayout("gap 1px!, insets 2px, fillx"));

        container.add(btnAutoPass, "growx, pushx, w 83%, " + heightConstraint + ", gaptop 2px");
        container.add(btnSettings, "w 17%, " + heightConstraint + ", gaptop 2px");
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

    public FButton getBtnAutoPass() { return btnAutoPass; }
    public FButton getBtnSettings() { return btnSettings; }
}
