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
        this.setCard(card);
    }

    /**
     * <p>
     * addComponents.
     * </p>
     */
    private void addComponents() {
        final Card c = this.getCard();
        final Image cardImage = ImageCache.getImage(c);
        if (cardImage == null) {
            // show the card as text
            this.setLayout(new GridLayout(0, 1));

            this.add(new JLabel(c.isFaceDown() ? "Morph" : c.getName() + "   " + c.getManaCost()));
            this.add(new JLabel(GuiDisplayUtil.formatCardType(c)));

            final JLabel tapLabel = new JLabel("Tapped");
            tapLabel.setBackground(Color.white);
            tapLabel.setOpaque(true);

            if (c.isTapped()) {
                this.add(tapLabel);
            }

            if (c.isCreature()) {
                this.add(new JLabel(c.getNetAttack() + " / " + c.getNetDefense()));
            }
        } else {
            // show the card image
            this.setLayout(new GridLayout(1, 1));
            this.add(new JLabel(new ImageIcon(cardImage)));
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void setCard(final Card card) {
        this.card = card;
        this.setBorder(GuiDisplayUtil.getBorder(card));
        this.addComponents();
    }

    /**
     * <p>
     * Getter for the field <code>card</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    @Override
    public final Card getCard() {
        return this.card;
    }

    /**
     * @return the connectedCard
     */
    public CardPanel getConnectedCard() {
        return connectedCard;
    }

    /**
     * @param connectedCard
     *            the connectedCard to set
     */
    public void setConnectedCard(CardPanel connectedCard) {
        this.connectedCard = connectedCard; // TODO: Add 0 to parameter's name.
    }

    // ~
    /** The connected card. */
    private CardPanel connectedCard;
    // ~
}
