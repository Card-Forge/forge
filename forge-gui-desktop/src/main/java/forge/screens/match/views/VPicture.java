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

import javax.swing.JLabel;

import forge.gui.CardPicturePanel;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.localinstance.skin.FSkinProp;
import forge.screens.match.controllers.CPicture;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/** 
 * Assembles Swing components of card picture area.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VPicture implements IVDoc<CPicture> {

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(Localizer.getInstance().getMessage("lblCardPicture"));

    // Top-level containers
    private final CardPicturePanel pnlPicture = new CardPicturePanel();
    private final SkinnedLabel lblFlipcard = new SkinnedLabel();

    private final CPicture controller;

    //========= Constructor
    public VPicture(final CPicture controller) {
        this.controller = controller;
        lblFlipcard.setIcon(FSkin.getIcon(FSkinProp.ICO_FLIPCARD));
        pnlPicture.setOpaque(false);
        lblFlipcard.setVisible(false);
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, align center center"));
        parentCell.getBody().add(lblFlipcard, "pos (50% - 40px) (50% - 60px)");
        parentCell.getBody().add(pnlPicture, "w 100%-6!, h 100%-6!");
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
        return EDocID.CARD_PICTURE;
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
    public CPicture getLayoutControl() {
        return controller;
    }

    //========== Retrieval methods

    /** @return {@link forge.gui.CardPicturePanel} */
    public CardPicturePanel getPnlPicture() {
        return pnlPicture;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblFlipcard() {
        return lblFlipcard;
    }
}
