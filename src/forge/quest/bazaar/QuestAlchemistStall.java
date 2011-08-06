package forge.quest.bazaar;

import forge.QuestData;
import forge.error.ErrorViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class QuestAlchemistStall extends QuestAbstractBazaarStall{


	private JLabel 			  potionDescLabel   = new JLabel();
	private JLabel			  potionStatsLabel  = new JLabel();
	private JLabel 			  potionPriceLabel  = new JLabel();
	private JLabel 			  potionIconLabel   = new JLabel();
	
	private JLabel			  creditsLabel     = new JLabel();

	private JButton           buyPotionButton   = new JButton();
    

    protected QuestAlchemistStall() {
        super("Alchemist", "BottlesIconSmall.png", "");

        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }

        setup();
    }

    //only do this ONCE:
    private void setup() {
    	buyPotionButton.setBounds(new Rectangle(10, 297, 120, 50));
    	buyPotionButton.setText(getButtonText());
    	buyPotionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
					buyPotionButton_actionPerformed(e);
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
            }
        });
        
    }//setup();
    
    private String getDesc()
    {
    	StringBuilder sb = new StringBuilder();
    	sb.append("<html>");

		sb.append("Gives +1 to max Life.<br>");
		sb.append("<u>Current life:</u>" + questData.getLife());
    	
    	sb.append("</html>");
    	return sb.toString();
    }

    private long getPrice()
    {
    	long l = 0;
    	if (questData.getLife() < 20)
    		l = 250;
    	else if (questData.getLife() < 25)
    		l = 500;
    	else 
    		l = 750;
    		
    	return l;
    }
    
    private String getButtonText()
    {
    	return "Buy Elixir";
    }
    
    private String getStats()
    {
    	return "Elixir of Health";
    }

    
    private void jbInit() throws Exception {

        
        potionStatsLabel.setFont(new Font("sserif", Font.BOLD, 12));
        potionStatsLabel.setText(getStats());
        potionStatsLabel.setBounds(new Rectangle(10, 65, 100, 15));
        
        potionDescLabel.setFont(new Font("sserif", 0, 12));
        potionDescLabel.setText(getDesc());
        potionDescLabel.setBounds(new Rectangle(10, 80, 300, 150));
        
        potionPriceLabel.setFont(new Font("sserif", 0, 12));
        potionPriceLabel.setText("<html><b><u>Price</u></b>: " + getPrice() + " credits</html>");
        potionPriceLabel.setBounds(new Rectangle(10, 230, 150, 15));
        
        creditsLabel.setFont(new Font("sserif", 0, 12));
        creditsLabel.setText("Credits: " + questData.getCredits());
        creditsLabel.setBounds(new Rectangle(10, 265, 150, 15));
        
        potionIconLabel.setText("");
        potionIconLabel.setIcon(icon);
        potionIconLabel.setBounds(new Rectangle(280, 65, 201, 280));
        potionIconLabel.setIconTextGap(0);
        
        //String fileName = "LeafIconSmall.png";
    	//ImageIcon icon = getIcon(fileName);
    	
    	buyPotionButton.setEnabled(true);
    	if (questData.getCredits() < getPrice() || questData.getLife() >= 30)
    		buyPotionButton.setEnabled(false);
       

        //jPanel2.add(quitButton, null);
        stallPanel.add(buyPotionButton, null);
        stallPanel.add(potionStatsLabel, null);
        stallPanel.add(potionDescLabel, null);
        stallPanel.add(potionIconLabel, null);
        stallPanel.add(potionPriceLabel, null);
        stallPanel.add(creditsLabel, null);
    }

    void buyPotionButton_actionPerformed(ActionEvent e) throws Exception {
	    	questData.subtractCredits(getPrice());
	    	questData.addLife(1);
	    	QuestData.saveData(questData);
	    	jbInit();
    }
    

}
