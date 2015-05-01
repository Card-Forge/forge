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

import java.awt.Dimension;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.gui.CardPicturePanel;
import forge.gui.WrapLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.match.controllers.CAntes;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;

/**
 * Assembles Swing components of card ante area.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VAntes implements IVDoc<CAntes> {
    private final CAntes controller;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Ante");

    private final JPanel pnl = new JPanel();
    private final FScrollPane scroller = new FScrollPane(pnl, false);
    private final SortedSet<AntePanel> allAntes = new TreeSet<AntePanel>();

    //========== Constructor
    public VAntes(final CAntes controller) {
        this.controller = controller;
        pnl.setLayout(new WrapLayout());
        pnl.setOpaque(false);
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
        return controller;
    }

    public void update() {
        allAntes.clear();
        pnl.removeAll();
        final GameView gameView = controller.getMatchUI().getGameView();
        if (gameView == null) {
            return;
        }

        for (final PlayerView p : gameView.getPlayers()) {
            final Iterable<CardView> ante = p.getAnte();
            if (ante != null) {
                for (final CardView c : ante) {
                    final AntePanel pnlTemp = new AntePanel(c);
                    allAntes.add(pnlTemp);
                    pnl.add(pnlTemp);
                }
            }
        }
    }

    //========= Private class handling
    @SuppressWarnings("serial")
    private class AntePanel extends JPanel implements Comparable<AntePanel> {
        private final CardView card;

        public AntePanel(final CardView c) {
            super();
            card = c;

            final Dimension d = new Dimension(160, 250);
            setPreferredSize(d);
            setMaximumSize(d);
            setMinimumSize(d);

            setOpaque(false);
            setLayout(new MigLayout("gap 0, insets 0, wrap"));
            add(new FLabel.Builder().fontSize(14).text(card.getOwner().getName())
                .fontAlign(SwingConstants.CENTER).build(), "w 160px, h 20px");
            final CardPicturePanel picPanel = new CardPicturePanel();
            add(picPanel, "w 160px, h 230px");
            picPanel.setCard(c.getCurrentState());
        }

        @Override
        public int compareTo(final AntePanel o) {
            return o.card.getId() - card.getId();
        }
    }
}
