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

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.GameLog;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.match.controllers.CLog;
import forge.gui.toolbox.FSkin;

/** 
 * Assembles Swing components of game log report.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VLog implements IVDoc {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Log");

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
        return EDocID.REPORT_LOG;
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
    public ICDoc getLayoutControl() {
        return CLog.SINGLETON_INSTANCE;
    }

    //========== Observer update methods
    /** */
    public void updateConsole() {
        // No need to update this unless it's showing
        if (!parentCell.getSelected().equals(this)) { return; }

        final GameLog gl = AllZone.getGameLog();

        parentCell.getBody().setLayout(new MigLayout("insets 1%, gap 1%, wrap"));
        parentCell.getBody().removeAll();

        // by default, grab everything logging level 3 or less
        // TODO - some option to make this configurable is probably desirable
        // TODO - add these components to resize adapter in constructor
        JTextArea tar = new JTextArea(gl.getLogText(3));
        tar.setOpaque(false);
        tar.setBorder(null);
        tar.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

        tar.setFocusable(false);
        tar.setEditable(false);
        tar.setLineWrap(true);
        tar.setWrapStyleWord(true);

        JScrollPane jsp = new JScrollPane(tar);
        jsp.setOpaque(false);
        jsp.setBorder(null);
        jsp.getViewport().setOpaque(false);

        parentCell.getBody().add(jsp, "w 98%!");
    }
}
