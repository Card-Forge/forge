/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.match.controllers.CDependencies;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedTextArea;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/** 
 * Assembles Swing components of layer dependencies.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VDependencies implements IVDoc<CDependencies> {

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(Localizer.getInstance().getMessage("lblDependenciesTab"));

    private final SkinnedTextArea tar = new SkinnedTextArea();

    private final CDependencies controller;
    public VDependencies(final CDependencies controller) {
        this.controller = controller;
        tar.setOpaque(false);
        tar.setBorder(new FSkin.MatteSkinBorder(0, 0, 0, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        tar.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tar.setFocusable(false);
        tar.setLineWrap(true);
    }
    
    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().removeAll();
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, wrap"));
        parentCell.getBody().add(tar, "w 95%!, gapleft 3%, gaptop 1%, h 95%");
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell()
     */
    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.REPORT_DEPENDENCIES;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CDependencies getLayoutControl() {
        return controller;
    }

    //========= Observer update methods

    public void updateDependencies(final long cntDependencies, final String desc) {
        tab.setText(cntDependencies > 0 ? (Localizer.getInstance().getMessage("lblDependenciesTab") + " : " + cntDependencies) : Localizer.getInstance().getMessage("lblDependenciesTab"));

        // No need to update this unless it's showing
        if (parentCell == null || !this.equals(parentCell.getSelected())) { return; }

        tar.setText(desc);
    }
}
