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

import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import net.miginfocom.swing.MigLayout;
import forge.GameLog.LogEntry;
import forge.Singletons;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.match.controllers.CLog;
import forge.gui.toolbox.FSkin;

/** 
 * Assembles Swing components of game log report.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VLog implements IVDoc<CLog> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Log");

    // Other fields
    private final JPanel pnl = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final JScrollPane scroller = new JScrollPane(pnl);

    //========== Constructor
    private VLog() {
        scroller.setOpaque(false);
        scroller.setBorder(null);
        scroller.getViewport().setOpaque(false);

        pnl.setOpaque(false);
    }

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
    public CLog getLayoutControl() {
        return CLog.SINGLETON_INSTANCE;
    }

    //========== Observer update methods
    /** */
    public void updateConsole() {
        // No need to update this unless it's showing
        if (!parentCell.getSelected().equals(this)) { return; }

        // TODO - some option to make this configurable is probably desirable
        // By default, grab everything log level 3 or less.
        final List<LogEntry> data = Singletons.getModel().getGame().getGameLog().getLogEntries(3);
        final int size = data.size();

        pnl.removeAll();

        for (int i = size-1; i >= 0; i--) {
            JTextArea tar = new JTextArea(data.get(i).getMessage());

            if (i % 2 == 1) { tar.setOpaque(false); }
            else { tar.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA)); }
            tar.setBorder(new EmptyBorder(2, 2, 2, 2));
            tar.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));

            tar.setFocusable(false);
            tar.setEditable(false);
            tar.setLineWrap(true);
            tar.setWrapStyleWord(true);

            pnl.add(tar, "w 98%!, gap 1% 0 0 0");
        }

        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, wrap"));
        parentCell.getBody().add(scroller, "w 98%!, pushy, growy, gap 1% 0 5px 5px");
    }
}
