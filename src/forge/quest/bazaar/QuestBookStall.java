package forge.quest.bazaar;

import forge.QuestData;
import forge.error.ErrorViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class QuestBookStall extends QuestAbstractBazaarStall{
	
	private static final long serialVersionUID = 2409591658245091210L;
		
	
	private JLabel            titleLabel        = new JLabel();
	
	private JLabel 			  sleightOfHandDescLabel  = new JLabel();

	private JLabel 			  sleightOfHandPriceLabel = new JLabel();
	private JLabel 			  sleightOfHandIconLabel  = new JLabel();
	
	private JLabel			  creditsLabel     	= new JLabel();
	
	private ImageIcon		  sleightOfHandIcon	  	= new ImageIcon();
	    
	private JButton           sleightOfHandButton= new JButton();
    
    public QuestBookStall() {
        super("Bookstore", "BookIconSmall.png", "");
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
       
        setup();
    }
    
    //only do this ONCE:
    private void setup() {
    	sleightOfHandButton.setBounds(new Rectangle(10, 297, 120, 50));
    	sleightOfHandButton.setText(getButtonText());
    	//buyPlantButton.setIcon(icon);
    	sleightOfHandButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
					learnEstatesButton_actionPerformed(e);
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

    	if (questData.getSleightOfHandLevel() == 0) 
    	{
    		sb.append("<u><b>Sleight of Hand Vol. I</b></u><br>");
    		sb.append("These volumes explain how to perform the most difficult of sleights.<br>");
    		sb.append("<u>Your first mulligan is <b>free</b></u>.");
    	}
    	else
    	{
    		sb.append("Currently nothing to study at the Library. <br>Please check back later.");
    	}
    	
    	sb.append("</html>");
    	return sb.toString();
    }

    private long getPrice()
    {
    	long l = 0;
    	if (questData.getSleightOfHandLevel() == 0)
    		l = 2000;    		
    	
    	return l;
    }
    
    private String getButtonText()
    {
    	return "Learn Sleight";
    }
    
    private String getImageString()
    {
    	return "BookIcon.png";
    }
    
    private void jbInit() throws Exception {
        titleLabel.setFont(new Font("sserif", Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("Library");
        titleLabel.setBounds(new Rectangle(130, 5, 198, 60));
        stallPanel.setLayout(null);
        
        /*
        potionStatsLabel.setFont(new Font("sserif", Font.BOLD, 12));
        potionStatsLabel.setText(getStats());
        potionStatsLabel.setBounds(new Rectangle(10, 65, 100, 15));
        */
        
        sleightOfHandDescLabel.setFont(new Font("sserif", 0, 12));
        sleightOfHandDescLabel.setText(getDesc());
        sleightOfHandDescLabel.setBounds(new Rectangle(10, 80, 300, 150));
        
        sleightOfHandPriceLabel.setFont(new Font("sserif", 0, 12));
        sleightOfHandPriceLabel.setText("<html><b><u>Price</u></b>: " + getPrice() + " credits</html>");
        sleightOfHandPriceLabel.setBounds(new Rectangle(10, 230, 150, 15));
        
        creditsLabel.setFont(new Font("sserif", 0, 12));
        creditsLabel.setText("Credits: " + questData.getCredits());
        creditsLabel.setBounds(new Rectangle(10, 265, 150, 15));
        
        sleightOfHandIcon = getIcon(getImageString());
        sleightOfHandIconLabel.setText("");
        sleightOfHandIconLabel.setIcon(sleightOfHandIcon);
        sleightOfHandIconLabel.setBounds(new Rectangle(300, 135, 128, 128));
        sleightOfHandIconLabel.setIconTextGap(0);
        
        sleightOfHandButton.setEnabled(true);
    	if (questData.getCredits() < getPrice() || questData.getSleightOfHandLevel() >= 1)
    		sleightOfHandButton.setEnabled(false);
       


        //jPanel2.add(quitButton, null);
        stallPanel.add(sleightOfHandButton, null);
        stallPanel.add(titleLabel, null);
        stallPanel.add(sleightOfHandDescLabel, null);
        stallPanel.add(sleightOfHandIconLabel, null);
        stallPanel.add(sleightOfHandPriceLabel, null);
        stallPanel.add(creditsLabel, null);
    }
    
    void learnEstatesButton_actionPerformed(ActionEvent e) throws Exception {
	    	questData.subtractCredits(getPrice());
	    	
	    	if (questData.getSleightOfHandLevel() < 1)
	    	{
	    		questData.addSleightOfHandLevel(1);
	    	}
	    	QuestData.saveData(questData);
	    	jbInit();
    }
    

    
}
