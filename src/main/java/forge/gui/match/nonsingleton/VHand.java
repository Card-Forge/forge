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

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;
import forge.game.player.Player;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.view.arcane.HandArea;

/**
 * Assembles Swing components of hand area.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VHand implements IVDoc {
    // Fields used with interface IVDoc
    private final CHand control;
    private DragCell parentCell;
    private final EDocID docID;
    private final DragTab tab = new DragTab("Your Hand");

    // Other fields
    private Player player = null;

    // Top-level containers
    private final JScrollPane scroller = new JScrollPane();
    private final HandArea hand = new HandArea(scroller);

    //========= Constructor
    /**
     * Assembles Swing components of a player hand instance.
     * 
     * @param id0 &emsp; {@link forge.gui.framework.EDocID}
     * @param player0 &emsp; {@link forge.game.player.Player}
     */
    public VHand(final EDocID id0, final Player player0) {
        docID = id0;
        id0.setDoc(this);
        tab.setText(player0.getName() + " Hand");

        player = player0;

        scroller.setBorder(null);
        scroller.setViewportView(VHand.this.hand);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);

        hand.setOpaque(false);

        control = new CHand(player, this);
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
     * @see forge.gui.framework.IVDoc#getControl()
     */
    @Override
    public ICDoc getControl() {
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

    /**
     * Gets the player currently associated with this hand.
     * @return {@link forge.game.player.Player}
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Sets the player currently associated with this field.
     * @param player0 &emsp; {@link forge.game.player.Player}
     */
    public void setPlayer(final Player player0) {
        this.player = player0;
        if (player0 != null) { tab.setText(player0.getName() + " Field"); }
    }
}
