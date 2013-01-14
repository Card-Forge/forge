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
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.item.CardPrinted;

/**
 * A simple class that shows a list of cards in a dialog with preview in its
 * right part.
 * 
 * @author Forge
 * @version $Id: ListChooser.java 9708 2011-08-09 19:34:12Z jendave $
 */
public class CardListViewer {

    // Data and number of choices for the list
    private final List<CardPrinted> list;

    // Decoration
    private final String title;

    // Flag: was the dialog already shown?
    private boolean called;
    // initialized before; listeners may be added to it
    private final JList jList;
    private final CardDetailPanel detail;
    private final CardPicturePanel picture;

    // Temporarily stored for event handlers during show
    private JDialog dialog;
    private final JOptionPane optionPane;
    private final Action ok;

    /**
     * Instantiates a new card list viewer.
     * 
     * @param title
     *            the title
     * @param list
     *            the list
     */
    public CardListViewer(final String title, final List<CardPrinted> list) {
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
    public CardListViewer(final String title, final String message, final List<CardPrinted> list) {
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
    public CardListViewer(final String title, final String message, final List<CardPrinted> list, final Icon dialogIcon) {
        this.title = title;
        this.list = Collections.unmodifiableList(list);
        this.jList = new JList(new ChooserListModel());
        this.detail = new CardDetailPanel(null);
        this.picture = new CardPicturePanel(null);
        this.ok = new CloseAction(JOptionPane.OK_OPTION, "OK");

        final Object[] options = new Object[] { new JButton(this.ok) };

        final JPanel threeCols = new JPanel();
        threeCols.add(new JScrollPane(this.jList));
        threeCols.add(this.picture);
        threeCols.add(this.detail);
        threeCols.setLayout(new java.awt.GridLayout(1, 3, 6, 0));

        this.optionPane = new JOptionPane(new Object[] { message, threeCols }, JOptionPane.INFORMATION_MESSAGE,
                JOptionPane.DEFAULT_OPTION, dialogIcon, options, options[0]);

        // selection is here
        this.jList.getSelectionModel().addListSelectionListener(new SelListener());
    }

    /**
     * Shows the dialog and returns after the dialog was closed.
     * 
     * @return a boolean.
     */
    public synchronized boolean show() {
        if (this.called) {
            throw new IllegalStateException("Already shown");
        }
        this.jList.setSelectedIndex(0);

        this.dialog = this.optionPane.createDialog(this.optionPane.getParent(), this.title);
        this.dialog.setSize(720, 360);
        this.dialog.addWindowFocusListener(new CardListFocuser());
        this.dialog.setVisible(true);
        this.dialog.toFront();

        this.dialog.dispose();
        this.called = true;
        return true;
    }

    private class ChooserListModel extends AbstractListModel {

        private static final long serialVersionUID = 3871965346333840556L;

        @Override
        public int getSize() {
            return CardListViewer.this.list.size();
        }

        @Override
        public Object getElementAt(final int index) {
            return CardListViewer.this.list.get(index);
        }
    }

    private class CloseAction extends AbstractAction {

        private static final long serialVersionUID = -8426767786083886936L;
        private final int value;

        public CloseAction(final int value, final String label) {
            super(label);
            this.value = value;
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            CardListViewer.this.optionPane.setValue(this.value);
        }
    }

    private class CardListFocuser implements WindowFocusListener {

        @Override
        public void windowGainedFocus(final WindowEvent e) {
            CardListViewer.this.jList.grabFocus();
        }

        @Override
        public void windowLostFocus(final WindowEvent e) {
        }
    }

    private class SelListener implements ListSelectionListener {

        @Override
        public void valueChanged(final ListSelectionEvent e) {
            final int row = CardListViewer.this.jList.getSelectedIndex();
            // (String) jList.getSelectedValue();
            if ((row >= 0) && (row < CardListViewer.this.list.size())) {
                final CardPrinted cp = CardListViewer.this.list.get(row);
                CardListViewer.this.detail.setCard(cp.getMatchingForgeCard());
                CardListViewer.this.picture.setCard(cp);
            }
        }


    }

}
