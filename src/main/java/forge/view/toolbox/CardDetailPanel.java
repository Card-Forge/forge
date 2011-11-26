package forge.view.toolbox;

import java.awt.Color;
import java.awt.Font;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Card;
import forge.CardContainer;
import forge.Constant.Zone;
import forge.Counters;
import forge.GameEntity;
import forge.GuiDisplayUtil;

/** Shows details of card if moused over. */
@SuppressWarnings("serial")
public class CardDetailPanel extends JPanel implements CardContainer {
    private Card card = null;
    private JLabel lblCardName, lblCardCost, lblCardType,
        lblCardID, lblCardPT, lblCardSet, lblCardDmg1, lblCardDmg2;
    private JTextArea tarInfo;
    private static final Color PURPLE = new Color(14381203);

    private FSkin skin;
    private Font f;
    private Color foreground;
    private Color zebra;

    /** */
    public CardDetailPanel() {
        super();
        setLayout(new MigLayout("wrap, insets 0, gap 0"));

        // Styles used in DetailLabel class
        skin = AllZone.getSkin();
        f = skin.getFont1().deriveFont(Font.PLAIN, 11);
        foreground = skin.getClrText();
        zebra = skin.getClrZebra();

        // DetailLabel instances (true = zebra stripe)
        lblCardName = new DetailLabel();
        lblCardCost = new DetailLabel();
        lblCardType = new DetailLabel();
        lblCardPT   = new DetailLabel();
        lblCardDmg1 = new DetailLabel();
        lblCardDmg2 = new DetailLabel();
        lblCardID   = new DetailLabel();
        lblCardSet  = new DetailLabel();

        // Info text area
        tarInfo = new JTextArea();
        tarInfo.setFont(f);
        tarInfo.setLineWrap(true);
        tarInfo.setWrapStyleWord(true);
        tarInfo.setFocusable(false);

        add(lblCardName, "w 100%!");
        add(lblCardCost, "w 100%!");
        add(lblCardType, "w 100%!");
        add(lblCardPT, "w 100%!");
        add(lblCardDmg1, "w 100%!");
        add(lblCardDmg2, "w 100%!");
        add(lblCardID, "w 100%!");
        add(lblCardSet, "w 100%!");
        add(new JScrollPane(tarInfo,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), "w 100%!, h 45%!, south");
        setCard(card);
    }

    /**
     * A fairly long method testing conditions determining which details to show.
     * 
     * @param c &emsp; Card obj
     */
    public void setCard(Card c) {
        lblCardName.setText(" ");
        lblCardCost.setText(" ");
        lblCardType.setText(" ");
        lblCardPT.setText(" ");
        lblCardDmg1.setText(" ");
        lblCardDmg2.setText(" ");
        lblCardID.setText(" ");
        lblCardSet.setText(" ");
        lblCardSet.setOpaque(true);
        lblCardSet.setBorder(null);
        tarInfo.setText(" ");

        this.card = c;
        if (card == null) {
            return;
        }

        boolean faceDown = card.isFaceDown() && card.getController() != AllZone.getHumanPlayer();

        if (!faceDown) {
            lblCardName.setText(card.getName());
            if (!card.getManaCost().equals("") && !card.isLand()) {
                lblCardCost.setText(card.getManaCost());
            }
        }
        else {
            lblCardName.setText("Morph");
        }

        if (!faceDown) {
            lblCardType.setText(GuiDisplayUtil.formatCardType(card));
        }
        else {
            lblCardType.setText("Creature");
        }

        if (card.isCreature()) {
            lblCardPT.setText(card.getNetAttack() + " / " + card.getNetDefense());
            lblCardDmg1.setText("Damage: " + card.getDamage());
            lblCardDmg2.setText("Assigned Damage: " + card.getTotalAssignedDamage());
        }
        if (card.isPlaneswalker()) {
            lblCardDmg2.setText("Assigned Damage: " + card.getTotalAssignedDamage());
        }

        lblCardID.setText("Card ID  " + card.getUniqueNumber());

        // Rarity and set of a face down card should not be visible to the opponent
        if (!card.isFaceDown() || card.getController().isHuman()) {
            String s = card.getCurSetCode();
            if (s.equals("")) {
                lblCardSet.setText("---");
            }
            else {
                lblCardSet.setText(s);
            }
        }

        String csr = card.getCurSetRarity();
        if (csr.equals("Common") || csr.equals("Land")) {
            lblCardSet.setBackground(Color.BLACK);
            lblCardSet.setForeground(Color.WHITE);
            //lblCardSet.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        } else if (csr.equals("Uncommon")) {
            lblCardSet.setBackground(Color.LIGHT_GRAY);
            lblCardSet.setForeground(Color.BLACK);
            //lblCardSet.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        } else if (csr.equals("Rare")) {
            lblCardSet.setBackground(Color.YELLOW);
            lblCardSet.setForeground(Color.BLACK);
            //lblCardSet.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        } else if (csr.equals("Mythic")) {
            lblCardSet.setBackground(Color.RED);
            lblCardSet.setForeground(Color.BLACK);
            //lblCardSet.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        } else if (csr.equals("Special")) {
            // "Timeshifted" or other Special Rarity Cards
            lblCardSet.setBackground(PURPLE);
            lblCardSet.setForeground(Color.BLACK);
            //lblCardSet.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        }

        // Fill the card text
        StringBuilder str = new StringBuilder();

        // Token
        if (card.isToken()) { str.append("Token"); }

        if (!faceDown) {
            if (str.length() != 0) { str.append("\n"); }
            String text = card.getText();

            //LEVEL [0-9]+-[0-9]+
            //LEVEL [0-9]+\+
            String regex = "LEVEL [0-9]+-[0-9]+ ";
            text = text.replaceAll(regex, "$0\r\n");

            regex = "LEVEL [0-9]+\\+ ";
            text = text.replaceAll(regex, "\r\n$0\r\n");

            // Displays keywords that have dots in them a little better:
            regex = "\\., ";
            text = text.replaceAll(regex, ".\r\n");

            str.append(text);
        }

        // Phasing
        if (card.isPhasedOut()) {
            if (str.length() != 0) { str.append("\n"); }
            str.append("Phased Out");
        }

        // Counter text
        Counters[] counters = Counters.values();
        for (Counters counter : counters) {
            if (card.getCounters(counter) != 0) {
                if (str.length() != 0) { str.append("\n"); }
                str.append(counter.getName() + " counters: ");
                str.append(card.getCounters(counter));
            }
        }

        // Regeneration Shields
        int regenShields = card.getShield();
        if (regenShields > 0) {
            if (str.length() != 0) { str.append("\n"); }
            str.append("Regeneration Shield(s): ").append(regenShields);
        }

        // Damage Prevention
        int preventNextDamage = card.getPreventNextDamage();
        if (preventNextDamage > 0) {
            str.append("\n");
            str.append("Prevent the next ").append(preventNextDamage).append(" damage that would be dealt to ");
            str.append(card.getName()).append(" it this turn.");
        }

        // Top revealed
        if (card.hasKeyword("Play with the top card of your library revealed.") && card.getController() != null
                && !card.getController().getZone(Zone.Library).isEmpty()) {
            str.append("\r\nTop card: ");
            str.append(card.getController().getCardsIn(Zone.Library, 1));
        }

        // Chosen type
        if (card.getChosenType() != "") {
            if (str.length() != 0) { str.append("\n"); }
            str.append("(chosen type: ");
            str.append(card.getChosenType());
            str.append(")");
        }

        // Chosen color
        if (!card.getChosenColor().isEmpty()) {
            if (str.length() != 0) { str.append("\n"); }
            str.append("(chosen colors: ");
            str.append(card.getChosenColor());
            str.append(")");
        }

        // Named card
        if (card.getNamedCard() != "") {
            if (str.length() != 0) { str.append("\n"); }
            str.append("(named card: ");
            str.append(card.getNamedCard());
            str.append(")");
        }

        // Equipping
        if (card.getEquipping().size() > 0) {
            if (str.length() != 0) { str.append("\n"); }
            str.append("=Equipping ");
            str.append(card.getEquipping().get(0));
            str.append("=");
        }

        // Equipped by
        if (card.getEquippedBy().size() > 0) {
            if (str.length() != 0) { str.append("\n"); }
            str.append("=Equipped by ");
            for (Iterator<Card> it = card.getEquippedBy().iterator(); it.hasNext();) {
                str.append(it.next());
                if (it.hasNext()) { str.append(", "); }
            }
            str.append("=");
        }

        // Enchanting
        GameEntity entity = card.getEnchanting();
        if (entity != null) {
            if (str.length() != 0) { str.append("\n"); }
            str.append("*Enchanting ");

            if (entity instanceof Card) {
                Card temp = (Card) entity;
                if (temp.isFaceDown() && temp.getController().isComputer()) {
                    str.append("Morph (");
                    str.append(card.getUniqueNumber());
                    str.append(")");
                }
                else {
                    str.append(entity);
                }
            }
            else {
                str.append(entity);
            }
            str.append("*");
        }

        // Enchanted by
        if (card.getEnchantedBy().size() > 0) {
            if (str.length() != 0) { str.append("\n"); }
            str.append("*Enchanted by ");
            for (Iterator<Card> it = card.getEnchantedBy().iterator(); it.hasNext();) {
                str.append(it.next());
                if (it.hasNext()) { str.append(", "); }
            }
            str.append("*");
        }

        // Controlling
        if (card.getGainControlTargets().size() > 0) {
            if (str.length() != 0) { str.append("\n"); }
            str.append("+Controlling: ");
            for (Iterator<Card> it = card.getGainControlTargets().iterator(); it.hasNext();) {
                str.append(it.next());
                if (it.hasNext()) { str.append(", "); }
            }
            str.append("+");
        }

        // Cloned via
        if (card.getCloneOrigin() != null) {
            if (str.length() != 0) { str.append("\n"); }
            str.append("^Cloned via: ");
            str.append(card.getCloneOrigin().getName());
            str.append("^");
        }

        // Imprint
        if (!card.getImprinted().isEmpty()) {
            if (str.length() != 0) { str.append("\n"); }
            str.append("^Imprinting: ");
            for (Iterator<Card> it = card.getImprinted().iterator(); it.hasNext();) {
                str.append(it.next());
                if (it.hasNext()) { str.append(", "); }
            }
            str.append("^");
        }

        // Uncastable
        /*if (card.isUnCastable()) {
            if (str.length() != 0) str.append("\n");
            str.append("This card can't be cast.");
        }*/

        if (card.hasAttachedCardsByMindsDesire()) {
            if (str.length() != 0) { str.append("\n"); }
            Card[] cards = card.getAttachedCardsByMindsDesire();
            str.append("=Attached: ");
            for (Card temp : cards) {
                str.append(temp.getName());
                str.append(" ");
            }
            str.append("=");
        }

        tarInfo.setText(str.toString());
    }

    /** @return Card */
    public Card getCard() {
        return this.card;
    }

    /** A brief JLabel to consolidate styling. */
    private class DetailLabel extends JLabel {
        public DetailLabel(boolean zebra0) {
            super();

            if (zebra0) {
               this.setBackground(zebra);
               this.setOpaque(true);
            }

            this.setFont(f);
            this.setForeground(foreground);
            this.setHorizontalAlignment(SwingConstants.CENTER);
        }

        public DetailLabel() {
            this(false);
        }
    }
}
