/**
 * ListChooser.java
 *
 * Created on 31.08.2009
 */

package forge.gui;


import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.AllZone;
import forge.Card;
import forge.CardUtil;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.item.CardPrinted;


import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static javax.swing.JOptionPane.*;


/**
 * A simple class that shows a list of cards in a dialog with preview in its right part. 
 *
 * @author Forge
 * @version $Id: ListChooser.java 9708 2011-08-09 19:34:12Z jendave $
 */
public class CardListViewer {

    //Data and number of choices for the list
    private List<CardPrinted> list;

    //Decoration
    private String title;

    //Flag: was the dialog already shown?
    private boolean called;
    //initialized before; listeners may be added to it
    private JList jList;
    private CardDetailPanel detail;
    private CardPicturePanel picture;

    //Temporarily stored for event handlers during show
    private JDialog dialog;
    private JOptionPane optionPane;
    private Action ok;

    public CardListViewer(String title, List<CardPrinted> list) {
        this(title, "", list, null);
    }

    public CardListViewer(String title, String message, List<CardPrinted> list) {
        this(title, message, list, null);
    }
    
    public CardListViewer(String title, String message, List<CardPrinted> list, Icon dialogIcon) {
        this.title = title;
        this.list = unmodifiableList(list);
        jList = new JList(new ChooserListModel());
        detail = new CardDetailPanel(null);
        picture = new CardPicturePanel(null);
        ok = new CloseAction(OK_OPTION, "OK");

        Object[] options = new Object[]{new JButton(ok)};

        JPanel threeCols = new JPanel();
        threeCols.add(new JScrollPane(jList));
        threeCols.add(picture);
        threeCols.add(detail);
        threeCols.setLayout( new java.awt.GridLayout(1, 3, 6, 0) );
        
        optionPane = new JOptionPane(new Object[]{message, threeCols}, 
                INFORMATION_MESSAGE, DEFAULT_OPTION, dialogIcon, options, options[0]);
        
        // selection is here
        jList.getSelectionModel().addListSelectionListener(new SelListener());
    }


    /**
     * Shows the dialog and returns after the dialog was closed.
     *
     * @return a boolean.
     */
    public synchronized boolean show() {
        if (called) throw new IllegalStateException("Already shown");
        jList.setSelectedIndex(0);
        
        dialog = optionPane.createDialog(optionPane.getParent(), title);
        dialog.setSize(720,360);
        dialog.addWindowFocusListener(new CardListFocuser());
        dialog.setVisible(true);
        dialog.toFront();
        
        dialog.dispose();
        called = true;
        return true;
    }

    private class ChooserListModel extends AbstractListModel {

        private static final long serialVersionUID = 3871965346333840556L;

        public int getSize() { return list.size(); }
        public Object getElementAt(int index) { return list.get(index); }
    }

    private class CloseAction extends AbstractAction {

        private static final long serialVersionUID = -8426767786083886936L;
        private int value;

        public CloseAction(int value, String label) {
            super(label);
            this.value = value;
        }


        public void actionPerformed(ActionEvent e) {
            optionPane.setValue(value);
        }
    }
    
    private class CardListFocuser implements WindowFocusListener {

        @Override
        public void windowGainedFocus(final WindowEvent e) {
            jList.grabFocus();
        }

        @Override
        public void windowLostFocus(final WindowEvent e) { } }


    private class SelListener implements ListSelectionListener {
        private Card[] cache = null;
        
        public void valueChanged(final ListSelectionEvent e) {
            int row = jList.getSelectedIndex();
            // (String) jList.getSelectedValue();
            if (row >= 0 && row < list.size()) {
                CardPrinted cp = list.get(row);
                ensureCacheHas(row, cp);
                detail.setCard(cache[row]);
                picture.setCard(cp);
            }
        }
        
        private void ensureCacheHas(int row, CardPrinted cp) {
            if (cache == null) { cache = new Card[list.size()]; }
            if (null == cache[row]) {
                Card card = AllZone.getCardFactory().getCard(cp.getName(), null);
                card.setCurSetCode(cp.getSet());
                card.setImageFilename(CardUtil.buildFilename(card));
                cache[row] = card;
            }            
        }
    }

}
