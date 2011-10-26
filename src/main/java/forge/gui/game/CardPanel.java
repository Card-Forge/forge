package forge.gui.game;

import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import forge.Card;
import forge.CardContainer;
import forge.GuiDisplayUtil;
import forge.ImageCache;

/**
 * The class CardPanel. A card panel stores a card to display it on the
 * battlefield. An image is used if available.
 * 
 * @author Forge
 * @version $Id$
 */
public class CardPanel extends JPanel implements CardContainer {
    /** Constant <code>serialVersionUID=509877513760665415L</code>. */
    private static final long serialVersionUID = 509877513760665415L;
    private Card card;

    /**
     * <p>
     * Constructor for CardPanel.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     */
    public CardPanel(final Card card) {
        setCard(card);
    }

    /**
     * <p>
     * addComponents.
     * </p>
     */
    private void addComponents() {
        Card c = getCard();
        Image cardImage = ImageCache.getImage(c);
        if (cardImage == null) {
            // show the card as text
            setLayout(new GridLayout(0, 1));

            add(new JLabel(c.isFaceDown() ? "Morph" : c.getName() + "   " + c.getManaCost()));
            add(new JLabel(GuiDisplayUtil.formatCardType(c)));

            JLabel tapLabel = new JLabel("Tapped");
            tapLabel.setBackground(Color.white);
            tapLabel.setOpaque(true);

            if (c.isTapped()) {
                add(tapLabel);
            }

            if (c.isCreature()) {
                add(new JLabel(c.getNetAttack() + " / " + c.getNetDefense()));
            }
        } else {
            // show the card image
            setLayout(new GridLayout(1, 1));
            add(new JLabel(new ImageIcon(cardImage)));
        }
    }

    /** {@inheritDoc} */
    public final void setCard(final Card card) {
        this.card = card;
        setBorder(GuiDisplayUtil.getBorder(card));
        addComponents();
    }

    /**
     * <p>
     * Getter for the field <code>card</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getCard() {
        return card;
    }

    // ~
    /** The connected card. */
    public CardPanel connectedCard;
    // ~
}
