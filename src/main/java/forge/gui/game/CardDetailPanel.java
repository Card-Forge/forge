/**
 * CardDetailPanel.java
 *
 * Created on 17.02.2010
 */

package forge.gui.game;


import forge.*;
//import forge.view.swing.OldGuiNewGame;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import java.awt.Color;
import java.awt.*;
import java.util.Iterator;


/**
 * The class CardDetailPanel. Shows the details of a card.
 *
 * @author Clemens Koza
 * @version V0.0 17.02.2010
 */
public class CardDetailPanel extends JPanel implements CardContainer {
    /** Constant <code>serialVersionUID=-8461473263764812323L</code> */
    private static final long serialVersionUID = -8461473263764812323L;

    private static Color PURPLE = new Color(14381203);
    
    private Card card;

    private JLabel nameCostLabel;
    private JLabel typeLabel;
    private JLabel powerToughnessLabel;
    private JLabel damageLabel;
    private JLabel idLabel;
    private JLabel setInfoLabel;
    private JTextArea cdArea;

    /**
     * <p>Constructor for CardDetailPanel.</p>
     *
     * @param card a {@link forge.Card} object.
     */
    public CardDetailPanel(Card card) {
        setLayout(new GridLayout(2, 0, 0, 5));
        setBorder(new EtchedBorder());

        JPanel cdLabels = new JPanel(new GridLayout(0, 1, 0, 5));
        cdLabels.add(nameCostLabel = new JLabel());
        cdLabels.add(typeLabel = new JLabel());
        cdLabels.add(powerToughnessLabel = new JLabel());
        cdLabels.add(damageLabel = new JLabel());

        JPanel IDR = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridwidth = 2;
        c.weightx = 1.0;
        IDR.add(idLabel = new JLabel(), c);

        c.gridwidth = 1;
        c.weightx = 0.3;
        IDR.add(setInfoLabel = new JLabel(), c);

        cdLabels.add(IDR);

        add(cdLabels);
        nameCostLabel.setHorizontalAlignment(SwingConstants.CENTER);
        typeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        powerToughnessLabel.setHorizontalAlignment(SwingConstants.CENTER);
        //cdLabel7.setSize(100, cdLabel7.getHeight());

        setInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        add(new JScrollPane(cdArea = new JTextArea(4, 12)));
        cdArea.setLineWrap(true);
        cdArea.setWrapStyleWord(true);

        if (!Singletons.getModel().getPreferences().lafFonts) {
        	nameCostLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        	typeLabel.setFont(new java.awt.Font("Dialog", 0, 14));
            powerToughnessLabel.setFont(new java.awt.Font("Dialog", 0, 14));
            damageLabel.setFont(new java.awt.Font("Dialog", 0, 14));
            idLabel.setFont(new java.awt.Font("Dialog", 0, 14));


            java.awt.Font f = new java.awt.Font("Dialog", 0, 14);
		    f = f.deriveFont(java.awt.Font.BOLD);
		    setInfoLabel.setFont(f);

		    cdArea.setFont(new java.awt.Font("Dialog", 0, 14));
        }

        setCard(card);
    }

    /** {@inheritDoc} */
    public void setCard(Card card) {
    	nameCostLabel.setText("");
    	typeLabel.setText("");
        powerToughnessLabel.setText("");
        damageLabel.setText("");
        idLabel.setText("");
        setInfoLabel.setText("");
        setInfoLabel.setOpaque(false);
        setInfoLabel.setBorder(null);
        cdArea.setText("");
        setBorder(GuiDisplayUtil.getBorder(card));

        this.card = card;
        if (card == null) return;

        boolean faceDown = card.isFaceDown() && card.getController() != AllZone.getHumanPlayer();
        if (!faceDown) {
            if (card.getManaCost().equals("") || card.isLand()) nameCostLabel.setText(card.getName());
            else nameCostLabel.setText(card.getName() + " - " + card.getManaCost());
        } else nameCostLabel.setText("Morph");

        if (!faceDown) typeLabel.setText(GuiDisplayUtil.formatCardType(card));
        else typeLabel.setText("Creature");

        if (card.isCreature()) {
            powerToughnessLabel.setText(card.getNetAttack() + " / " + card.getNetDefense());
            damageLabel.setText("Damage: " + card.getDamage() + " Assigned Damage: " + card.getTotalAssignedDamage());
        }
        if (card.isPlaneswalker()) damageLabel.setText("Assigned Damage: " + card.getTotalAssignedDamage());

        idLabel.setText("Card ID  " + card.getUniqueNumber());

        //rarity and set of a face down card should not be visible to the opponent
        if (!card.isFaceDown() || card.getController().isHuman()) setInfoLabel.setText(card.getCurSetCode());

        if (!setInfoLabel.getText().equals("")) {
        	setInfoLabel.setOpaque(true);
            String csr = card.getCurSetRarity();
            if (csr.equals("Common") || csr.equals("Land")) {
            	setInfoLabel.setBackground(Color.BLACK);
            	setInfoLabel.setForeground(Color.WHITE);
            	setInfoLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
            } else if (csr.equals("Uncommon")) {
            	setInfoLabel.setBackground(Color.LIGHT_GRAY);
            	setInfoLabel.setForeground(Color.BLACK);
            	setInfoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            } else if (csr.equals("Rare")) {
            	setInfoLabel.setBackground(Color.YELLOW);
            	setInfoLabel.setForeground(Color.BLACK);
            	setInfoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            } else if (csr.equals("Mythic")) {
            	setInfoLabel.setBackground(Color.RED);
            	setInfoLabel.setForeground(Color.BLACK);
            	setInfoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            } else if (csr.equals("Special")) {
            	// "Timeshifted" or other Special Rarity Cards
            	setInfoLabel.setBackground(PURPLE);
            	setInfoLabel.setForeground(Color.BLACK);
            	setInfoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            }
            //cdLabel7.setText(card.getCurSetCode());
        }

        //fill the card text

        StringBuilder area = new StringBuilder();

        //Token
        if (card.isToken()) area.append("Token");

        if (!faceDown) {
            //card text
            if (area.length() != 0) area.append("\n");
            String text = card.getText();
            //LEVEL [0-9]+-[0-9]+
            //LEVEL [0-9]+\+

            String regex = "LEVEL [0-9]+-[0-9]+ ";
            text = text.replaceAll(regex, "$0\r\n");

            regex = "LEVEL [0-9]+\\+ ";
            text = text.replaceAll(regex, "\r\n$0\r\n");

            //displays keywords that have dots in them a little better:
            regex = "\\., ";
            text = text.replaceAll(regex, ".\r\n");

            area.append(text);
        }

        //counter text
        Counters[] counters = Counters.values();
        for (Counters counter : counters) {
            if (card.getCounters(counter) != 0) {
                if (area.length() != 0) area.append("\n");
                area.append(counter.getName() + " counters: ");
                area.append(card.getCounters(counter));
            }
        }

        // Regeneration Shields
        int regenShields = card.getShield();
        if (regenShields > 0) {
            if (area.length() != 0) area.append("\n");
            area.append("Regeneration Shield(s): ").append(regenShields);
        }

        // Damage Prevention
        int preventNextDamage = card.getPreventNextDamage();
        if (preventNextDamage > 0) {
            area.append("\n");
            area.append("Prevent the next ").append(preventNextDamage).append(" damage that would be dealt to ");
            area.append(card.getName()).append(" it this turn.");
        }

        //top revealed
        if (card.hasKeyword("Play with the top card of your library revealed.") && card.getController() != null
                && !AllZoneUtil.getPlayerCardsInLibrary(card.getController()).isEmpty()) {
            area.append("\r\nTop card: ");
            area.append(AllZoneUtil.getPlayerCardsInLibrary(card.getController(), 1));
        }

        //chosen type
        if (card.getChosenType() != "") {
            if (area.length() != 0) area.append("\n");
            area.append("(chosen type: ");
            area.append(card.getChosenType());
            area.append(")");
        }

        //chosen color
        if (card.getChosenColor() != "") {
            if (area.length() != 0) area.append("\n");
            area.append("(chosen color: ");
            area.append(card.getChosenColor());
            area.append(")");
        }

        //named card
        if (card.getNamedCard() != "") {
            if (area.length() != 0) area.append("\n");
            area.append("(named card: ");
            area.append(card.getNamedCard());
            area.append(")");
        }

        //equipping
        if (card.getEquipping().size() > 0) {
            if (area.length() != 0) area.append("\n");
            area.append("=Equipping ");
            area.append(card.getEquipping().get(0));
            area.append("=");
        }

        //equipped by
        if (card.getEquippedBy().size() > 0) {
            if (area.length() != 0) area.append("\n");
            area.append("=Equipped by ");
            for (Iterator<Card> it = card.getEquippedBy().iterator(); it.hasNext(); ) {
                area.append(it.next());
                if (it.hasNext()) area.append(", ");
            }
            area.append("=");
        }

        //enchanting
        GameEntity entity = card.getEnchanting();
        if (entity != null){ 
            if (area.length() != 0) area.append("\n");
            area.append("*Enchanting ");
            
            if (entity instanceof Card){
                Card c = (Card)entity;
                if (c.isFaceDown() && c.getController().isComputer()){
                    area.append("Morph (");
                    area.append(card.getUniqueNumber());
                    area.append(")");
                }
                else{
                    area.append(entity);
                }
            }
            else{
                area.append(entity);
            }
            area.append("*");
        }

        //enchanted by
        if (card.getEnchantedBy().size() > 0) {
            if (area.length() != 0) area.append("\n");
            area.append("*Enchanted by ");
            for (Iterator<Card> it = card.getEnchantedBy().iterator(); it.hasNext(); ) {
                area.append(it.next());
                if (it.hasNext()) area.append(", ");
            }
            area.append("*");
        }

        //controlling
        if (card.getGainControlTargets().size() > 0) {
            if (area.length() != 0) area.append("\n");
            area.append("+Controlling: ");
            for (Iterator<Card> it = card.getGainControlTargets().iterator(); it.hasNext(); ) {
                area.append(it.next());
                if (it.hasNext()) area.append(", ");
            }
            area.append("+");
        }

        //cloned via
        if (card.getCloneOrigin() != null) {
            if (area.length() != 0) area.append("\n");
            area.append("^Cloned via: ");
            area.append(card.getCloneOrigin().getName());
            area.append("^");
        }

        //Imprint
        if (!card.getImprinted().isEmpty()) {
            if (area.length() != 0) area.append("\n");
            area.append("^Imprinting: ");
            for (Iterator<Card> it = card.getImprinted().iterator(); it.hasNext(); ) {
                area.append(it.next());
                if (it.hasNext()) area.append(", ");
            }
            area.append("^");
        }

        //uncastable
        if (card.isUnCastable()) {
            if (area.length() != 0) area.append("\n");
            area.append("This card can't be cast.");
        }

        if (card.hasAttachedCards()) {
            if (area.length() != 0) area.append("\n");
            Card[] cards = card.getAttachedCards();
            area.append("=Attached: ");
            for (Card c : cards) {
                area.append(c.getName());
                area.append(" ");
            }
            area.append("=");
        }

        cdArea.setText(area.toString());
    }

    /**
     * <p>Getter for the field <code>card</code>.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getCard() {
        return card;
    }
}
