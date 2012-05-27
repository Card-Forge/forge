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

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import forge.gui.CardDetailPanel;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.match.controllers.CDetail;
import forge.gui.toolbox.FSkin;

/** 
 * Assembles Swing components of card detail area.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VDetail implements IVDoc {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Card Detail");

    // Top-level containers
    private final CardDetailPanel pnlDetail = new CardDetailPanel(null);
    private final JLabel lblFlipcard = new JLabel(
            FSkin.getIcon(FSkin.InterfaceIcons.ICO_FLIPCARD));

    //========= Overridden methods
    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, center"));
        parentCell.getBody().add(lblFlipcard, "pos (50% - 40px) (50% - 60px)");
        parentCell.getBody().add(pnlDetail, "w 100%!, h 100%!");
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
        return EDocID.CARD_DETAIL;
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
        return CDetail.SINGLETON_INSTANCE;
    }

    //========= Retrieval methods

    /** @return {@link forge.gui.CardDetailPanel} */
    public CardDetailPanel getPnlDetail() {
        return pnlDetail;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblFlipcard() {
        return lblFlipcard;
    }
}
