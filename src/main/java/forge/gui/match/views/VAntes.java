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

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Card;
import forge.game.player.Player;
import forge.gui.CardPicturePanel;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.match.controllers.CAntes;

/** 
 * Assembles Swing components of card ante area.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VAntes implements IVDoc {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Ante");
    private CardPicturePanel humanAnte = new CardPicturePanel(null);
    private CardPicturePanel computerAnte = new CardPicturePanel(null);

    //========== Constructor
    private VAntes() {
        humanAnte.setOpaque(false);
        computerAnte.setOpaque(false);
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 1%, gap 1%"));
        parentCell.getBody().add(humanAnte, "w 47%!, h 100%!");
        parentCell.getBody().add(computerAnte, "w 47%!, h 100%!");
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
        return EDocID.CARD_ANTES;
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
        return CAntes.SINGLETON_INSTANCE;
    }

    //========== Setters / getters
    /**
     * @param p0 &emsp; {@link forge.game.player.Player}
     * @param c0 &emsp; {@link forge.Card}
     */
    public void setAnteCard(final Player p0, final Card c0) {
        if (p0.equals(AllZone.getComputerPlayer())) {
            computerAnte.setCard(c0);
            computerAnte.revalidate();
        }
        else if (p0.equals(AllZone.getHumanPlayer())) {
            humanAnte.setCard(c0);
            humanAnte.revalidate();
        }
    }
}
