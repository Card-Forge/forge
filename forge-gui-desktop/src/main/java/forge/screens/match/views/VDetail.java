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

import forge.assets.FSkinProp;
import forge.game.GameView;
import forge.gui.CardDetailPanel;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.match.controllers.CDetail;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedLabel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/** 
 * Assembles Swing components of card detail area.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VDetail implements IVDoc<CDetail> {

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Card Detail");

    // Top-level containers
    private final CardDetailPanel pnlDetail = new CardDetailPanel();
    private final SkinnedLabel lblFlipcard = new SkinnedLabel();

    private final CDetail controller;

    //========= Constructor
    public VDetail(final CDetail controller) {
        this.controller = controller;
        lblFlipcard.setIcon(FSkin.getIcon(FSkinProp.ICO_FLIPCARD));
        lblFlipcard.setVisible(false);
    }

    public void setGameView(final GameView gameView) {
        this.pnlDetail.setGameView(gameView);
    }

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
    public CDetail getLayoutControl() {
        return controller;
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
