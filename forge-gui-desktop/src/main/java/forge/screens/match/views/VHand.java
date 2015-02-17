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

import net.miginfocom.swing.MigLayout;
import forge.game.player.PlayerView;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CHand;
import forge.toolbox.FScrollPane;
import forge.view.arcane.HandArea;

/**
 * Assembles Swing components of hand area.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VHand implements IVDoc<CHand> {
    // Fields used with interface IVDoc
    private final CHand control;
    private DragCell parentCell;
    private final EDocID docID;
    private final DragTab tab = new DragTab("Your Hand");

    // Top-level containers
    private final FScrollPane scroller = new FScrollPane(false);
    private final HandArea hand;

    //========= Constructor
    /**
     * Assembles Swing components of a player hand instance.
     * 
     * @param id0 &emsp; {@link forge.gui.framework.EDocID}
     * @param p &emsp; {@link forge.game.player.Player}
     */
    public VHand(final CMatchUI matchUI, final EDocID id0, final PlayerView p) {
        this.hand = new HandArea(matchUI, scroller);
        docID = id0;
        id0.setDoc(this);

        if (p == null) {
            tab.setText("NO PLAYER Hand");
        } else {
            tab.setText(p.getName() + " Hand");
        }

        scroller.setViewportView(VHand.this.hand);

        hand.setOpaque(false);

        control = new CHand(matchUI, p, this);
    }

    //========= Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        final JPanel pnl = parentCell.getBody();
        pnl.setLayout(new MigLayout("insets 0, gap 0"));

        pnl.add(scroller, "w 100%, h 100%!");
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return docID;
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
    public CHand getLayoutControl() {
        return control;
    }

    //========= Retrieval methods
    /**
     * Gets the hand area.
     *
     * @return {@link forge.view.arcane.HandArea}
     */
    public HandArea getHandArea() {
        return VHand.this.hand;
    }

}
