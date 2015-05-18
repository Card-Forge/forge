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

package forge.gui;

import forge.game.card.CardView;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.view.FDialog;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.event.*;
import java.util.Collections;
import java.util.List;

/**
 * A simple class that shows a list of cards in a dialog with preview in its
 * right part and allows the player to select a card. 
 */
@SuppressWarnings("serial")
public class CardListChooser extends FDialog {

    // Data and number of choices for the list
    private final List<PaperCard> list;

    // initialized before; listeners may be added to it
    private final JList<PaperCard> jList;
    private final CardDetailPanel detail;
    private final CardPicturePanel picture;

    public CardListChooser(final String title, final String message, final List<PaperCard> list) {
        this.list = Collections.unmodifiableList(list);
        this.jList = new JList<>(new ChooserListModel());
        this.detail = new CardDetailPanel();
        this.picture = new CardPicturePanel();
        this.picture.setOpaque(false);

        this.setTitle(title);
        
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
            this.setSize(1200, 825);
        } else {
            this.setSize(720, 374);
        }
        
        this.addWindowFocusListener(new CardListFocuser());

        FButton btnOK = new FButton("Select Card");
        btnOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                CardListChooser.this.processWindowEvent(new WindowEvent(CardListChooser.this, WindowEvent.WINDOW_CLOSING));
            }
        });
        
        //Ensure the window can't be closed without user confirmation.
        //Unfortunately this giant block of code is necessary for that.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(final WindowEvent e) {
                
            }

            @Override
            public void windowClosing(final WindowEvent e) {
                if (FOptionPane.showConfirmDialog("Are you sure you want to pick '" + jList.getSelectedValue().getName() + "'?", "Select this card?", false)) {
                    dispose();
                }
            }

            @Override
            public void windowClosed(final WindowEvent e) {

            }

            @Override
            public void windowIconified(final WindowEvent e) {

            }

            @Override
            public void windowDeiconified(final WindowEvent e) {

            }

            @Override
            public void windowActivated(final WindowEvent e) {

            }

            @Override
            public void windowDeactivated(final WindowEvent e) {

            }
        });
        
        this.add(new FLabel.Builder().text(message).build(), "cell 0 0, spanx 3, gapbottom 4");

        if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
            this.add(new FScrollPane(this.jList, true), "cell 0 1, w 225, h 450, ax c");
            this.add(this.picture, "cell 1 1, w 480, growy, pushy, ax c");
            this.add(this.detail, "cell 2 1, w 320, h 500, ax c");
            this.add(btnOK, "cell 1 2, w 150, h 40, ax c, gaptop 6");
        } else {
            this.add(new FScrollPane(this.jList, true), "cell 0 1, w 225, growy, pushy, ax c");
            this.add(this.picture, "cell 1 1, w 225, growy, pushy, ax c");
            this.add(this.detail, "cell 2 1, w 225, growy, pushy, ax c");
            this.add(btnOK, "cell 1 2, w 150, h 26, ax c, gaptop 6");
        }

        // selection is here
        this.jList.getSelectionModel().addListSelectionListener(new SelListener());
        this.jList.setSelectedIndex(0);
    }
    
    public PaperCard getSelectedCard() {
        return jList.getSelectedValue();
    }

    private class ChooserListModel extends AbstractListModel<PaperCard> {
        private static final long serialVersionUID = 1338637387517082484L;

        @Override
        public int getSize() {
            return CardListChooser.this.list.size();
        }

        @Override
        public PaperCard getElementAt(final int index) {
            return CardListChooser.this.list.get(index);
        }
    }

    private class CardListFocuser implements WindowFocusListener {
        @Override
        public void windowGainedFocus(final WindowEvent e) {
            CardListChooser.this.jList.grabFocus();
        }

        @Override
        public void windowLostFocus(final WindowEvent e) {
        }
    }

    private class SelListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) {
            final int row = CardListChooser.this.jList.getSelectedIndex();
            if ((row >= 0) && (row < CardListChooser.this.list.size())) {
                final PaperCard cp = CardListChooser.this.list.get(row);
                CardListChooser.this.detail.setCard(CardView.getCardForUi(cp));
                CardListChooser.this.picture.setItem(cp);
            }
        }
    }
}
