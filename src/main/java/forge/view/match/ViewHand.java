package forge.view.match;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Card;
import forge.GuiDisplayUtil;
import forge.ImageCache;
import forge.control.match.ControlHand;

import forge.gui.skin.FRoundedPanel;

/** 
 * VIEW - Swing components for user hand.
 *
 */
@SuppressWarnings("serial")
public class ViewHand extends JScrollPane {
    private FRoundedPanel pnlContent;
    private ControlHand control;
    private List<CardPanel> cardPanels = new ArrayList<CardPanel>();

    /**
     * VIEW - Swing components for user hand.
     */
    public ViewHand() {
        super(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

        setOpaque(false);
        getViewport().setOpaque(false);
        getHorizontalScrollBar().setUnitIncrement(16);
        setBorder(null);

        // After all components are in place, instantiate controller.
        control = new ControlHand(this);
    }

    /** @return ControlHand */
    public ControlHand getController() {
        return control;
    }

    /**
     * Rebuilds layout of the hand panel.  Card panels are removed,
     * the height and card aspect ratio are used to set layout column width,
     * then card panels are added to the fresh layout.
     * 
     */
    // This design choice was made to allow the functionality of a JPanel
    // while maintaining a scale-able view.  Overridden paint methods could
    // do this, but require heavy coding.
    public void refreshLayout() {
        // Remove all panels and recalculate layout scaling based on aspect ratio.
        pnlContent = new FRoundedPanel();
        pnlContent.setBackground(AllZone.getSkin().getClrTheme());
        pnlContent.setCorners(new boolean[] {true, false, false, true});
        pnlContent.setLayout(new MigLayout("insets 3 10 3 10"));
        pnlContent.setSize(getViewport().getSize());
        pnlContent.validate();
        this.setViewportView(pnlContent);

        int h = getViewport().getHeight() - 6;
        pnlContent.setLayout(new MigLayout("align center"));

        // Re-insert panel instances. Possible memory management problem
        // from re-adding pre-existing panels. Doublestrike 22-11-11
        cardPanels = new ArrayList<CardPanel>();
        for (Card c : control.getCards()) {
            CardPanel temp = new CardPanel(c);
            cardPanels.add(temp);
            pnlContent.add(temp, "h " + h + "px!, w " + (int) (h * 0.7) + "px!");
            control.addCardPanelListeners(temp);
        }
        // Notify system of change.
    }

    /**
     * 
     */
    public class CardPanel extends JPanel {
        private static final long serialVersionUID = 509877513760665415L;
        private Card card = null;
        private Image img;
        private int w, h = 0;

        /**
         * <p>Constructor for CardPanel.</p>
         *
         * @param c &emsp; Card object.
         */
        public CardPanel(Card c) {
            super();
            this.card = c;
            this.img = ImageCache.getImage(card);

            setToolTipText("<html>" + c.getName() + "<br>" + GuiDisplayUtil.formatCardType(c) + "</html>");

            // No image?
            if (img == null) {
                setBorder(new LineBorder(new Color(240, 240, 240), 1));
                setLayout(new MigLayout("wrap, insets 2, gap 0"));
                setOpaque(true);
                setBackground(new Color(200, 200, 200));

                JLabel lblManaCost = new JLabel(c.getManaCost());
                lblManaCost.setHorizontalAlignment(SwingConstants.RIGHT);

                JLabel lblCardName = new JLabel(c.getName());
                lblCardName.setHorizontalAlignment(SwingConstants.CENTER);

                JLabel lblPowerToughness = new JLabel("");
                lblPowerToughness.setHorizontalAlignment(SwingConstants.RIGHT);

                if (c.isFaceDown()) {
                    lblCardName.setText("Morph");
                    lblManaCost.setText("");
                }

                if (c.isCreature()) {
                    lblPowerToughness.setText(c.getNetAttack() + " / " + c.getNetDefense());
                }

                add(lblManaCost, "w 90%!");
                add(lblCardName, "w 90%!");
                add(lblPowerToughness, "w 90%!, gaptop 25");
            }
            else {
                setBorder(new LineBorder(Color.black, 1));
                w = img.getWidth(null);
                h = img.getHeight(null);
            }
        }

        /** @return Card */
        public Card getCard() {
            return this.card;
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (img != null) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.drawImage(img, 0, 0, getWidth(), getHeight(), 0, 0, w, h, null);
            }
            else {
                g.setColor(new Color(200, 200, 200));
                g.drawRect(1, 1, getWidth(), getHeight());
            }
        }
    }

    /** @return List<CardPanel> */
    public List<CardPanel> getCardPanels() {
        return cardPanels;
    }
}
