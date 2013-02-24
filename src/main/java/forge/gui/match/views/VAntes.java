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

import java.awt.Dimension;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.Card;
import forge.game.player.Player;
import forge.gui.CardPicturePanel;
import forge.gui.WrapLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.match.controllers.CAntes;
import forge.gui.toolbox.FLabel;

/** 
 * Assembles Swing components of card ante area.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VAntes implements IVDoc<CAntes> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Ante");

    // Other fields
    private final Comparator<AntePanel> c = new Comparator<AntePanel>() {
        @Override
        public int compare(AntePanel arg0, AntePanel arg1) {
            return arg0.getID().compareTo(arg1.getID());
        }
    };

    private final JPanel pnl = new JPanel();
    private final JScrollPane scroller = new JScrollPane(pnl);
    private final SortedSet<AntePanel> allAntes = new TreeSet<AntePanel>(c);

    //========== Constructor
    private VAntes() {
        pnl.setLayout(new WrapLayout());
        pnl.setOpaque(false);
        scroller.setBorder(null);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0"));
        parentCell.getBody().add(scroller, "w 100%!, h 100%!");
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
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CAntes getLayoutControl() {
        return CAntes.SINGLETON_INSTANCE;
    }

    //========== Setters / getters
    /**
     * @param p0 &emsp; {@link forge.game.player.Player}
     * @param c0 &emsp; {@link forge.Card}
     */
    public void addAnteCard(final Player p0, final Card c0) {
        final AntePanel pnlTemp = new AntePanel(p0, c0);
        allAntes.add(pnlTemp);
        pnl.add(pnlTemp);
    }

    /**
     * @param p0 &emsp; {@link forge.game.player.Player}
     * @param c0 &emsp; {@link forge.Card}
     */
    public void removeAnteCard(final Player p0, final Card c0) {
        final Iterator<AntePanel> itr = allAntes.iterator();
        while (itr.hasNext()) {
            final AntePanel pnlTemp = itr.next();

            if (pnlTemp.getPlayer().equals(p0) && pnlTemp.getCard().equals(c0)) {
                pnl.remove(pnlTemp);
                itr.remove();
            }
        }
    }

    /** */
    public void clearAnteCards() {
        allAntes.clear();
    }

    //========= Private class handling
    @SuppressWarnings("serial")
    private class AntePanel extends JPanel {
        private final Player player;
        private final Card card;
        /**
         * 
         * @param p0 &emsp; {@link forge.game.player.Player}
         * @param c0 &emsp; {@link forge.Card}
         */
        public AntePanel(final Player p0, final Card c0) {
            super();
            player = p0;
            card = c0;

            final Dimension d = new Dimension(160, 250);
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);

            setOpaque(false);
            setLayout(new MigLayout("gap 0, insets 0, wrap"));
            add(new FLabel.Builder().fontSize(14).text(player.getName())
                    .fontAlign(SwingConstants.CENTER).build(), "w 160px, h 20px");
            CardPicturePanel picPanel = new CardPicturePanel();
            add(picPanel, "w 160px, h 230px");
            picPanel.setCard(c0);
        }

        /** @return {@link forge.game.player.Player} */
        public Player getPlayer() {
            return player;
        }

        /** @return {@link forge.Card} */
        public Card getCard() {
            return card;
        }

        public String getID() {
            return player.getName() + card.getName();
        }
    }
}
