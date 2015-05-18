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
package forge.screens.match;

import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import forge.game.card.CardView;
import forge.gui.CardDetailPanel;
import forge.gui.CardPicturePanel;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.FList;
import forge.toolbox.FPanel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;

/**
 * A simple JPanel that shows three columns: card list, pic, and description..
 * 
 * @author Forge
 * @version $Id: ListChooser.java 9708 2011-08-09 19:34:12Z jendave $
 */
@SuppressWarnings("serial")
public class QuestWinLoseCardViewer extends FPanel {

    // Data and number of choices for the list
    private final List<PaperCard> list;

    // initialized before; listeners may be added to it
    private final JList<PaperCard> jList;
    private final CardDetailPanel detail;
    private final CardPicturePanel picture;
    private final FScrollPane scroller;

    /**
     * Instantiates a new quest win lose card viewer.
     * 
     * @param list
     *            the list
     */
    public QuestWinLoseCardViewer(final List<PaperCard> list) {
        this.list = Collections.unmodifiableList(list);
        this.jList = new FList<PaperCard>(new ChooserListModel());
        this.detail = new CardDetailPanel();
        this.picture = new CardPicturePanel();
        this.scroller = new FScrollPane(this.jList, false);

        this.setCornerDiameter(20);
        this.setBorderToggle(false);
        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        picture.setOpaque(false);

        this.setLayout(new MigLayout("insets 0, gap 0"));

        if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
            this.add(scroller, "w 23%!, h 98%!, gap 1% 1% 1% 1%");
            this.add(this.picture, "w 48%!, h 98%!, gap 0 0 1% 1%");
            this.add(this.detail, "w 23%!, h 98%!, gap 1% 1% 1% 1%");
        } else {
            this.add(scroller, "w 32%!, h 98%!, gap 1% 1% 1% 1%");
            this.add(this.picture, "w 32%!, h 98%!, gap 0 0 1% 1%");
            this.add(this.detail, "w 32%!, h 98%!, gap 1% 1% 1% 1%");
        }

        // selection is here
        this.jList.getSelectionModel().addListSelectionListener(new SelListener());
        this.jList.setSelectedIndex(0);
    }

    private class ChooserListModel extends AbstractListModel<PaperCard> {

        private static final long serialVersionUID = 3871965346333840556L;

        @Override
        public int getSize() {
            return QuestWinLoseCardViewer.this.list.size();
        }

        @Override
        public PaperCard getElementAt(final int index) {
            return QuestWinLoseCardViewer.this.list.get(index);
        }
    }

    private class SelListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) {
            final int row = QuestWinLoseCardViewer.this.jList.getSelectedIndex();
            // (String) jList.getSelectedValue();
            if ((row >= 0) && (row < QuestWinLoseCardViewer.this.list.size())) {
                final PaperCard cp = QuestWinLoseCardViewer.this.list.get(row);
                QuestWinLoseCardViewer.this.detail.setCard(CardView.getCardForUi(cp));
                QuestWinLoseCardViewer.this.picture.setItem(cp);
            }
        }
    }
}
