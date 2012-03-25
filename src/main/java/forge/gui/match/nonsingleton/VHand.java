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
package forge.gui.match.nonsingleton;

import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;
import arcane.ui.HandArea;
import forge.gui.layout.DragTab;
import forge.gui.layout.EDocID;
import forge.gui.layout.ICDoc;
import forge.gui.layout.IVDoc;

/**
 * Assembles Swing components of hand area.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VHand implements IVDoc {
    /** */
    SINGLETON_INSTANCE;

    private final JPanel pnl = new JPanel();
    private final DragTab tab = new DragTab("Your Hand");

    private final JScrollPane scroller = new JScrollPane();
    private final HandArea hand = new HandArea(scroller);

    /* (non-Javadoc)
     * @see forge.gui.layout.IVDoc#populate()
     */
    @Override
    public void populate() {
        scroller.setViewportView(VHand.this.hand);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);
        hand.setOpaque(false);

        pnl.removeAll();
        pnl.setLayout(new MigLayout("insets 0, gap 0"));
        pnl.add(scroller, "w 100%, h 100%!");
    }

    /* (non-Javadoc)
     * @see forge.gui.layout.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.YOUR_HAND;
    }

    /* (non-Javadoc)
     * @see forge.gui.layout.IVDoc#getDocument()
     */
    @Override
    public Component getDocument() {
        return pnl;
    }

    /* (non-Javadoc)
     * @see forge.gui.layout.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /**
     * Gets the hand area.
     *
     * @return {@link arcane.ui.HandArea}
     */
    public HandArea getHandArea() {
        return VHand.this.hand;
    }

    /* (non-Javadoc)
     * @see forge.gui.layout.IVDoc#getControl()
     */
    @Override
    public ICDoc getControl() {
        return null;
    }
}
