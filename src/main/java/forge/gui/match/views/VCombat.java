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
package forge.gui.match.views;

import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.match.controllers.CCombat;
import forge.gui.toolbox.FSkin;

/** 
 * Assembles Swing components of combat report.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VCombat implements IVDoc {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Combat");

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        // (Panel uses observers to update, no permanent components here.)
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
        return EDocID.MATCH_COMBAT;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getControl()
     */
    @Override
    public ICDoc getControl() {
        return CCombat.SINGLETON_INSTANCE;
    }

    //========= Observer update methods

    /** @param s0 &emsp; {@link java.lang.String} */
    public void updateCombat(final String s0) {
        // No need to update this unless it's showing
        if (!parentCell.getSelected().equals(this)) { return; }

        parentCell.getBody().removeAll();
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, wrap"));

        final Border border = new MatteBorder(0, 0, 0, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS));

        tab.setText("Combat : " + AllZone.getCombat().getAttackers().size());

        final JTextArea tar = new JTextArea(s0);
        tar.setOpaque(false);
        tar.setBorder(border);
        tar.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tar.setFocusable(false);
        tar.setLineWrap(true);
        parentCell.getBody().add(tar, "w 95%!, gapleft 3%, gaptop 1%, h 95%");
    }
}
