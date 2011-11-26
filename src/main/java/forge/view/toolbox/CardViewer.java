package forge.view.toolbox;

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

import java.util.List;

import static java.util.Collections.unmodifiableList;

/**
 * A simple JPanel that shows three columns: card list, pic, and description.. 
 *
 * @author Forge
 * @version $Id: ListChooser.java 9708 2011-08-09 19:34:12Z jendave $
 */
@SuppressWarnings("serial")
public class CardViewer extends JPanel {

    //Data and number of choices for the list
    private List<CardPrinted> list;

    //initialized before; listeners may be added to it
    private JList jList;
    private CardDetailPanel detail;
    private CardPicturePanel picture;
    
    public CardViewer(List<CardPrinted> list) {
        this.list = unmodifiableList(list);
        jList = new JList(new ChooserListModel());
        detail = new CardDetailPanel(null);
        picture = new CardPicturePanel(null);

        this.add(new JScrollPane(jList));
        this.add(picture);
        this.add(detail);
        this.setLayout( new java.awt.GridLayout(1, 3, 6, 0) );

        // selection is here
        jList.getSelectionModel().addListSelectionListener(new SelListener());
        jList.setSelectedIndex(0);
    }

    private class ChooserListModel extends AbstractListModel {

        private static final long serialVersionUID = 3871965346333840556L;

        public int getSize() { return list.size(); }
        public Object getElementAt(int index) { return list.get(index); }
    }


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
