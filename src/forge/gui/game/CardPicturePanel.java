/**
 * CardPicturePanel.java
 * 
 * Created on 17.02.2010
 */

package forge.gui.game;


import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;

import arcane.ui.ScaledImagePanel;
import arcane.ui.ScaledImagePanel.MultipassType;
import arcane.ui.ScaledImagePanel.ScalingType;
import forge.Card;
import forge.CardContainer;
import forge.ImageCache;


/**
 * The class CardPicturePanel. Shows the full-sized image in a label. if there's no picture, the cardname is
 * displayed instead.
 * 
 * @version V0.0 17.02.2010
 * @author Clemens Koza
 */
public class CardPicturePanel extends JPanel implements CardContainer {
    private static final long serialVersionUID = -3160874016387273383L;
    
    private Card              card;
    
//    private JLabel           label;
//    private ImageIcon        icon;
    private ScaledImagePanel  panel;
    private Image             currentImange;
    
    public CardPicturePanel(Card card) {
        super(new BorderLayout());
//        add(label = new JLabel(icon = new ImageIcon()));
        add(panel = new ScaledImagePanel());
        panel.setScalingBlur(false);
        panel.setScalingType(ScalingType.bicubic);
        panel.setScalingMultiPassType(MultipassType.none);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                update();
            }
            
            @Override
            public void componentResized(ComponentEvent e) {
                update();
            }
        });
        
        setCard(card);
    }
    
    public void update() {
        setCard(getCard());
    }
    
    public void setCard(Card card) {
        this.card = card;
        if(!isShowing()) return;
        Insets i = getInsets();
        Image image = card == null? null:ImageCache.getImage(card, getWidth() - i.left - i.right, getHeight()
                - i.top - i.bottom);
        
        if(image != currentImange) {
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
    
    public Card getCard() {
        return card;
    }
}
