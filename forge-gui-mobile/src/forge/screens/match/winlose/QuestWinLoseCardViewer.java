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
package forge.screens.match.winlose;

import forge.item.PaperCard;
import forge.toolbox.FList;
import forge.toolbox.FPanel;
import java.util.Collections;
import java.util.List;


public class QuestWinLoseCardViewer extends FPanel {
    private final FList<PaperCard> lstCards;

    public QuestWinLoseCardViewer(final List<PaperCard> list) {
        lstCards = new FList<PaperCard>(Collections.unmodifiableList(list));

        // selection is here
        /*lstCards.getSelectionModel().addListSelectionListener(new SelListener());
        lstCards.setSelectedIndex(0);*/
    }

    /*private class SelListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) {
            final int row = lstCards.getSelectedIndex();
            // (String) lstCards.getSelectedValue();
            if ((row >= 0) && (row < QuestWinLoseCardViewer.list.size())) {
                final PaperCard cp = QuestWinLoseCardViewer.list.get(row);
                QuestWinLoseCardViewer.detail.setCard(Card.getCardForUi(cp));
                QuestWinLoseCardViewer.picture.setCard(cp);
            }
        }
    }*/

}
