/**
 * CardPicturePanel.java
 *
 * Created on 17.02.2010
 */

package forge.gui.game;


import arcane.ui.ScaledImagePanel;
import arcane.ui.ScaledImagePanel.MultipassType;
import arcane.ui.ScaledImagePanel.ScalingType;
import forge.Card;
import forge.CardContainer;
import forge.ImageCache;
import forge.card.CardPrinted;
import forge.card.InventoryItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;


/**
 * The class CardPicturePanel. Shows the full-sized image in a label. if there's no picture, the cardname is
 * displayed instead.
 *
 * @author Clemens Koza
 * @version V0.0 17.02.2010
 */
public final class CardPicturePanel extends JPanel implements CardContainer {
    /** Constant <code>serialVersionUID=-3160874016387273383L</code> */
    private static final long serialVersionUID = -3160874016387273383L;

    private Card card;
    private InventoryItem inventoryItem;

    //    private JLabel           label;
//    private ImageIcon        icon;
    private ScaledImagePanel panel;
    private Image currentImange;

    /**
     * <p>Constructor for CardPicturePanel.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public CardPicturePanel(final Card c) {
        super(new BorderLayout());
//        add(label = new JLabel(icon = new ImageIcon()));
        add(panel = new ScaledImagePanel());
        panel.setScalingBlur(false);
        panel.setScalingType(ScalingType.bicubic);
        panel.setScalingMultiPassType(MultipassType.none);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(final ComponentEvent e) {
                update();
            }

            @Override
            public void componentResized(final ComponentEvent e) {
                update();
            }
        });

        setCard(c);
    }

    /**
     * <p>update.</p>
     */
    public void update() { setCard(getCard()); }

    public void setCard(final InventoryItem cp) {
        card = null;
        inventoryItem = cp;
        if (!isShowing()) { return; }

        setImage();
    }

    /** {@inheritDoc} */
    public void setCard(final Card c) {
        card = c;
        inventoryItem = null;
        if (!isShowing()) { return; }

        setImage();
    }

    private void setImage() {
        Insets i = getInsets();
        Image image = null;
        if (inventoryItem != null) {
            image = ImageCache.getImage(inventoryItem, getWidth() - i.left - i.right, getHeight() - i.top - i.bottom); }
        if (card != null && image == null) {
            image = ImageCache.getImage(card, getWidth() - i.left - i.right, getHeight() - i.top - i.bottom); }

        if (image != currentImange) {
            currentImange = image;
            panel.setImage(image, null);
            panel.repaint();
        }
//        if(image == null) {
//            label.setIcon(null);
//            //avoid a hard reference to the image while not needed
//            icon.setImage(null);
//            label.setText(card.isFaceDown()? "Morph":card.getName());
//        } else if(image != icon.getImage()) {
//            icon.setImage(image);
//            label.setIcon(icon);
//        }
    }

    /**
     * <p>Getter for the field <code>card</code>.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getCard() {
        if ( card == null && inventoryItem != null && inventoryItem instanceof CardPrinted ) {
            card = ((CardPrinted) inventoryItem).toForgeCard();
        }
        return card;
    }
}
