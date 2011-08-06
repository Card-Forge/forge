/**
 * CardDetailPanel.java
 * 
 * Created on 17.02.2010
 */

package forge.gui.game;


import java.awt.GridLayout;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import forge.AllZone;
import forge.Card;
import forge.CardContainer;
import forge.Counters;
import forge.GuiDisplayUtil;
import forge.Gui_NewGame;


/**
 * The class CardDetailPanel. Shows the details of a card.
 * 
 * @version V0.0 17.02.2010
 * @author Clemens Koza
 */
public class CardDetailPanel extends JPanel implements CardContainer {
    private static final long serialVersionUID = -8461473263764812323L;
    
    private Card              card;
    
    private JLabel            cdLabel1;
    private JLabel            cdLabel2;
    private JLabel            cdLabel3;
    private JLabel            cdLabel4;
    private JLabel            cdLabel5;
    private JLabel            cdLabel6;
    private JTextArea         cdArea;
    
    public CardDetailPanel(Card card) {
        setLayout(new GridLayout(2, 0, 0, 5));
        setBorder(new EtchedBorder());
        
        JPanel cdLabels = new JPanel(new GridLayout(0, 1, 0, 5));
        cdLabels.add(cdLabel1 = new JLabel());
        cdLabels.add(cdLabel2 = new JLabel());
        cdLabels.add(cdLabel3 = new JLabel());
        cdLabels.add(cdLabel4 = new JLabel());
        cdLabels.add(cdLabel6 = new JLabel());
        cdLabels.add(cdLabel5 = new JLabel());
        add(cdLabels);
        cdLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        cdLabel2.setHorizontalAlignment(SwingConstants.CENTER);
        cdLabel3.setHorizontalAlignment(SwingConstants.CENTER);
        
        add(new JScrollPane(cdArea = new JTextArea(4, 12)));
        cdArea.setLineWrap(true);
        cdArea.setWrapStyleWord(true);
        
        if(!Gui_NewGame.useLAFFonts.isSelected()) {
            cdLabel1.setFont(new java.awt.Font("Dialog", 0, 14));
            cdLabel2.setFont(new java.awt.Font("Dialog", 0, 14));
            cdLabel3.setFont(new java.awt.Font("Dialog", 0, 14));
            cdLabel4.setFont(new java.awt.Font("Dialog", 0, 14));
            cdLabel5.setFont(new java.awt.Font("Dialog", 0, 14));
            cdLabel6.setFont(new java.awt.Font("Dialog", 0, 14));
            cdArea.setFont(new java.awt.Font("Dialog", 0, 14));
        }
        
        setCard(card);
    }
    
    public void setCard(Card card) {
        cdLabel1.setText("");
        cdLabel2.setText("");
        cdLabel3.setText("");
        cdLabel4.setText("");
        cdLabel5.setText("");
        cdLabel6.setText("");
        cdArea.setText("");
        setBorder(GuiDisplayUtil.getBorder(card));
        
        this.card = card;
        if(card == null) return;
        
        boolean faceDown = card.isFaceDown() && card.getController() != AllZone.HumanPlayer;
        if(!faceDown) {
            if(card.isLand()) cdLabel1.setText(card.getName());
            else cdLabel1.setText(card.getName() + "  - " + card.getManaCost());
        } else cdLabel1.setText("Morph");
        
        if(!faceDown) cdLabel2.setText(GuiDisplayUtil.formatCardType(card));
        else cdLabel2.setText("Creature");
        
        if(card.isCreature()) {
            cdLabel3.setText(card.getNetAttack() + " / " + card.getNetDefense());
            cdLabel4.setText("Damage: " + card.getDamage() + " Assigned Damage: " + card.getTotalAssignedDamage());
        }
        if(card.isPlaneswalker()) cdLabel4.setText("Assigned Damage: " + card.getTotalAssignedDamage());
        
        cdLabel5.setText("Card ID  " + card.getUniqueNumber());
        

        //fill the card text
        
        StringBuilder area = new StringBuilder();
        
        //Token
        if(card.isToken()) area.append("Token");
        
        if(!faceDown) {
            //card text
            if(area.length() != 0) area.append("\n");
            String text = card.getText();
            //LEVEL [0-9]+-[0-9]+
            //LEVEL [0-9]+\+
            
            String regex = "LEVEL [0-9]+-[0-9]+ ";
            text = text.replaceAll(regex,"$0\r\n");
            
            regex = "LEVEL [0-9]+\\+ ";
            text = text.replaceAll(regex,"\r\n$0\r\n");
            
            //displays keywords that have dots in them a little better:
            regex = "\\., ";
            text = text.replaceAll(regex,".\r\n");
            
            area.append(text);
        }
        
        //counter text
        Counters[] counters = Counters.values();
        for(Counters counter:counters) {
            if(card.getCounters(counter) != 0) {
                if(area.length() != 0) area.append("\n");
                area.append(counter.getName() + " counters: ");
                area.append(card.getCounters(counter));
            }
        }
        
        //top revealed
        if(card.getKeyword().contains("Play with the top card of your library revealed.") &&
           !card.getTopCardName().equals(""))
        {
        	area.append("\r\nTop card: ");
        	area.append(card.getTopCardName());
        }
        
        //chosen type
        if(card.getChosenType() != "") {
            if(area.length() != 0) area.append("\n");
            area.append("(chosen type: ");
            area.append(card.getChosenType());
            area.append(")");
        }
        
        //chosen color
        if(card.getChosenColor() != "") {
            if(area.length() != 0) area.append("\n");
            area.append("(chosen color: ");
            area.append(card.getChosenColor());
            area.append(")");
        }
        
        //named card
        if(card.getNamedCard() != "") {
            if(area.length() != 0) area.append("\n");
            area.append("(named card: ");
            area.append(card.getNamedCard());
            area.append(")");
        }
        
        //equipping
        if(card.getEquipping().size() > 0) {
            if(area.length() != 0) area.append("\n");
            area.append("=Equipping ");
            area.append(card.getEquipping().get(0));
            area.append("=");
        }
        
        //equipped by
        if(card.getEquippedBy().size() > 0) {
            if(area.length() != 0) area.append("\n");
            area.append("=Equipped by ");
            for(Iterator<Card> it = card.getEquippedBy().iterator(); it.hasNext();) {
                area.append(it.next());
                if(it.hasNext()) area.append(", ");
            }
            area.append("=");
        }
        
        //enchanting
        if(card.getEnchanting().size() > 0) {
            if(area.length() != 0) area.append("\n");
            area.append("*Enchanting ");
            area.append(card.getEnchanting().get(0));
            area.append("*");
        }
        
        //enchanted by
        if(card.getEnchantedBy().size() > 0) {
        	if(area.length() != 0) area.append("\n");
            area.append("*Enchanted by ");
            for(Iterator<Card> it = card.getEnchantedBy().iterator(); it.hasNext();) {
                area.append(it.next());
                if(it.hasNext()) area.append(", ");
            }
            area.append("*");
        }
        
        //uncastable
        if(card.isUnCastable()) {
            if(area.length() != 0) area.append("\n");
            area.append("This card can't be cast.");
        }
        
        if(card.hasAttachedCards()) {
        	if (area.length()!= 0) area.append("\n");
        	Card[] cards = card.getAttachedCards();
        	area.append("=Attached: ");
        	for (Card c:cards) {
        		area.append(c.getName());
        		area.append(" ");
        	}
        	area.append("=");
        }
        
        cdArea.setText(area.toString());
    }
    
    public Card getCard() {
        return card;
    }
}
