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
package forge.quest.gui;

import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.AllZone;
import forge.Card;
import forge.CardUtil;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.item.CardPrinted;

/**
 * A simple JPanel that shows three columns: card list, pic, and description..
 * 
 * @author Forge
 * @version $Id: ListChooser.java 9708 2011-08-09 19:34:12Z jendave $
 */
@SuppressWarnings("serial")
public class QuestWinLoseCardViewer extends JPanel {

    // Data and number of choices for the list
    private final List<CardPrinted> list;

    // initialized before; listeners may be added to it
    private final JList jList;
    private final CardDetailPanel detail;
    private final CardPicturePanel picture;

    /**
     * Instantiates a new quest win lose card viewer.
     * 
     * @param list
     *            the list
     */
    public QuestWinLoseCardViewer(final List<CardPrinted> list) {
        this.list = Collections.unmodifiableList(list);
        this.jList = new JList(new ChooserListModel());
        this.detail = new CardDetailPanel(null);
        this.picture = new CardPicturePanel(null);

        this.add(new JScrollPane(this.jList));
        this.add(this.picture);
        this.add(this.detail);
        this.setLayout(new java.awt.GridLayout(1, 3, 6, 0));

        // selection is here
        this.jList.getSelectionModel().addListSelectionListener(new SelListener());
        this.jList.setSelectedIndex(0);
    }

    private class ChooserListModel extends AbstractListModel {

        private static final long serialVersionUID = 3871965346333840556L;

        @Override
        public int getSize() {
            return QuestWinLoseCardViewer.this.list.size();
        }

        @Override
        public Object getElementAt(final int index) {
            return QuestWinLoseCardViewer.this.list.get(index);
        }
    }

    private class SelListener implements ListSelectionListener {
        private Card[] cache = null;

        @Override
        public void valueChanged(final ListSelectionEvent e) {
            final int row = QuestWinLoseCardViewer.this.jList.getSelectedIndex();
            // (String) jList.getSelectedValue();
            if ((row >= 0) && (row < QuestWinLoseCardViewer.this.list.size())) {
                final CardPrinted cp = QuestWinLoseCardViewer.this.list.get(row);
                this.ensureCacheHas(row, cp);
                QuestWinLoseCardViewer.this.detail.setCard(this.cache[row]);
                QuestWinLoseCardViewer.this.picture.setCard(cp);
            }
        }

        private void ensureCacheHas(final int row, final CardPrinted cp) {
            if (this.cache == null) {
                this.cache = new Card[QuestWinLoseCardViewer.this.list.size()];
            }
            if (null == this.cache[row]) {
                final Card card = AllZone.getCardFactory().getCard(cp.getName(), null);
                card.setCurSetCode(cp.getSet());
                card.setImageFilename(CardUtil.buildFilename(card));
                this.cache[row] = card;
            }
        }
    }

}
