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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.game.card.CardView;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.FButton;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.view.FDialog;

/**
 * A simple class that shows a list of cards in a dialog with preview in its
 * right part.
 * 
 * @author Forge
 * @version $Id: ListChooser.java 9708 2011-08-09 19:34:12Z jendave $
 */
@SuppressWarnings("serial")
public class BoxedProductCardListViewer extends FDialog {

    // Data and number of choices for the list
    private final List<PaperCard> list;

    // initialized before; listeners may be added to it
    private final JList<PaperCard> jList;
    private final CardDetailPanel detail;
    private final CardPicturePanel picture;
    
    private boolean skipTheRest = false;
    
    /**
     * Instantiates a new card list viewer.
     * 
     * @param title
     *            the title
     * @param list
     *            the list
     */
    public BoxedProductCardListViewer(final String title, final List<PaperCard> list) {
        this(title, "", list, null);
    }

    /**
     * Instantiates a new card list viewer.
     * 
     * @param title
     *            the title
     * @param message
     *            the message
     * @param list
     *            the list
     */
    public BoxedProductCardListViewer(final String title, final String message, final List<PaperCard> list) {
        this(title, message, list, null);
    }

    /**
     * Instantiates a new card list viewer.
     * 
     * @param title
     *            the title
     * @param message
     *            the message
     * @param list
     *            the list
     * @param dialogIcon
     *            the dialog icon
     */
    public BoxedProductCardListViewer(final String title, final String message, final List<PaperCard> list, final Icon dialogIcon) {
        this.list = Collections.unmodifiableList(list);
        this.jList = new JList<PaperCard>(new ChooserListModel());
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

        FButton btnOK = new FButton("Next Pack");
        btnOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                BoxedProductCardListViewer.this.processWindowEvent(new WindowEvent(BoxedProductCardListViewer.this, WindowEvent.WINDOW_CLOSING));
            }
        });

        FButton btnCancel = new FButton("Open All Remaining");
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                BoxedProductCardListViewer.this.skipTheRest = true;
                BoxedProductCardListViewer.this.processWindowEvent(new WindowEvent(BoxedProductCardListViewer.this, WindowEvent.WINDOW_CLOSING));
            }
        });
        
        this.add(new FLabel.Builder().text(message).build(), "cell 0 0, spanx 3, gapbottom 4");

        if (FModel.getPreferences().getPrefBoolean(FPref.UI_LARGE_CARD_VIEWERS)) {
            this.add(new FScrollPane(this.jList, true), "cell 0 1, w 225, h 450, ax c");
            this.add(this.picture, "cell 1 1, w 480, growy, pushy, ax c");
            this.add(this.detail, "cell 2 1, w 320, h 500, ax c");
            this.add(btnOK, "cell 1 2, w 150, h 40, ax c, gaptop 6");
            this.add(btnCancel, "cell 2 2, w 205, h 40, ax c, gaptop 6");
        } else {
            this.add(new FScrollPane(this.jList, true), "cell 0 1, w 225, growy, pushy, ax c");
            this.add(this.picture, "cell 1 1, w 225, growy, pushy, ax c");
            this.add(this.detail, "cell 2 1, w 225, growy, pushy, ax c");
            this.add(btnOK, "cell 1 2, w 150, h 26, ax c, gaptop 6");
            this.add(btnCancel, "cell 2 2, w 205, h 26, ax c, gaptop 6");
        }

        // selection is here
        this.jList.getSelectionModel().addListSelectionListener(new SelListener());
        this.jList.setSelectedIndex(0);
    }
    
    public boolean skipTheRest() {
        return skipTheRest;
    }

    private class ChooserListModel extends AbstractListModel<PaperCard> {
        private static final long serialVersionUID = 3871965346333840556L;

        @Override
        public int getSize() {
            return BoxedProductCardListViewer.this.list.size();
        }

        @Override
        public PaperCard getElementAt(final int index) {
            return BoxedProductCardListViewer.this.list.get(index);
        }
    }

    private class CardListFocuser implements WindowFocusListener {
        @Override
        public void windowGainedFocus(final WindowEvent e) {
            BoxedProductCardListViewer.this.jList.grabFocus();
        }

        @Override
        public void windowLostFocus(final WindowEvent e) {
        }
    }

    private class SelListener implements ListSelectionListener {
        @Override
        public void valueChanged(final ListSelectionEvent e) {
            final int row = BoxedProductCardListViewer.this.jList.getSelectedIndex();
            // (String) jList.getSelectedValue();
            if ((row >= 0) && (row < BoxedProductCardListViewer.this.list.size())) {
                final PaperCard cp = BoxedProductCardListViewer.this.list.get(row);
                BoxedProductCardListViewer.this.detail.setCard(CardView.getCardForUi(cp));
                BoxedProductCardListViewer.this.picture.setItem(cp);
            }
        }
    }
}
