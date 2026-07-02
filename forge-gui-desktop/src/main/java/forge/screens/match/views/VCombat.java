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
import forge.screens.match.controllers.CCombat;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedTextArea;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

import javax.swing.ScrollPaneConstants;

/** 
 * Assembles Swing components of combat report.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VCombat implements IVDoc<CCombat> {

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(Localizer.getInstance().getMessage("lblCombatTab"));

    private final SkinnedTextArea tar = new SkinnedTextArea();
    private final FScrollPane scroller = new FScrollPane(tar, false,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    private final CCombat controller;
    public VCombat(final CCombat controller) {
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
        parentCell.getBody().add(scroller, "w 95%!, gapleft 3%, gaptop 1%, h 95%");
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
        return EDocID.REPORT_COMBAT;
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
    public CCombat getLayoutControl() {
        return controller;
    }

    //========= Observer update methods

    public void updateCombat(final int cntAttackers, final String desc) {
        // No need to update this unless it's showing
        if (parentCell == null || !this.equals(parentCell.getSelected())) { return; }

        tab.setText(cntAttackers > 0 ? (Localizer.getInstance().getMessage("lblCombatTab") + " : " + cntAttackers) : Localizer.getInstance().getMessage("lblCombatTab"));
        tar.setText(desc);
    }
}
