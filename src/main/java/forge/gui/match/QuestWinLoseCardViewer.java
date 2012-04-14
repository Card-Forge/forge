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
package forge.gui.match;

import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import forge.Card;
import forge.gui.CardDetailPanel;
import forge.gui.CardPicturePanel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.item.CardPrinted;

/**
 * A simple JPanel that shows three columns: card list, pic, and description..
 * 
 * @author Forge
 * @version $Id: ListChooser.java 9708 2011-08-09 19:34:12Z jendave $
 */
@SuppressWarnings("serial")
public class QuestWinLoseCardViewer extends FPanel {

    // Data and number of choices for the list
    private final List<CardPrinted> list;

    // initialized before; listeners may be added to it
    private final JList jList;
    private final CardDetailPanel detail;
    private final CardPicturePanel picture;
    private final FScrollPane scroller;

    /**
     * Instantiates a new quest win lose card viewer.
     * 
     * @param list
     *            the list
     */
    public QuestWinLoseCardViewer(final List<CardPrinted> list) {
        this.list = Collections.unmodifiableList(list);
        this.jList = new FList(new ChooserListModel());
        this.detail = new CardDetailPanel(null);
        this.picture = new CardPicturePanel(null);
        this.scroller = new FScrollPane(this.jList);

        this.setCornerDiameter(20);
        this.setBorderToggle(false);
        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        picture.setOpaque(false);
        scroller.setBorder(null);

        this.setLayout(new MigLayout("insets 0, gap 0"));
        this.add(scroller, "w 32%!, h 98%!, gap 1% 1% 1% 1%");
        this.add(this.picture, "w 32%!, h 98%!, gap 0 0 1% 1%");
        this.add(this.detail, "w 32%!, h 98%!, gap 1% 1% 1% 1%");

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
                this.cache[row] = cp.toForgeCard();
            }
        }
    }

}
