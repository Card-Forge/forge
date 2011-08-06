package forge.quest.bazaar;

import forge.AllZone;
import forge.QuestData;
import forge.error.ErrorViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class QuestNurseryStall extends QuestAbstractBazaarStall{
	
	private static final long serialVersionUID = 2409591658245091210L;
	private JLabel            titleLabel       = new JLabel();
	
	private JLabel 			  plantDescLabel   = new JLabel();
	private JLabel			  plantStatsLabel  = new JLabel();
	private JLabel 			  plantPriceLabel  = new JLabel();
	private JLabel 			  plantIconLabel   = new JLabel();
	
	private JLabel			  creditsLabel     = new JLabel();
	
	private ImageIcon		  plantIcon		   = new ImageIcon();
	    
	private JButton           buyPlantButton   = new JButton();
    private QuestData 		  questData 	   = AllZone.QuestData;
    
    public QuestNurseryStall() {
        super("Nursery", "LeafIconSmall.png", "");
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
        
        
        setup();
    }
    
    private void setup() {
    	//buyPlantButton.setIcon(icon);
    	buyPlantButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
					buyPlantButton_actionPerformed(e);
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
    	if (questData.getPlantLevel() == 0)
    	{
    		sb.append("Start each of your battles with this lush, <br> verdant plant on your side.<br>");
    		sb.append("Excellent at blocking the nastiest of critters!<br><br>");
    		sb.append("<u><b>Level 1</b></u>: 0/1<br>");
    		sb.append("<u><b>Next Level</b></u>: 0/2<br>");
    		sb.append("<u><b>Can learn</b></u>: Deathtouch");
    	}
    	else if (questData.getPlantLevel() == 1)
    	{
    		sb.append("Improve the toughness of your plant.<br>");
    		sb.append("<u><b>Level 2</b></u>: 0/2<br>");
    		sb.append("<u><b>Next Level</b></u>: 0/3<br>");
    		sb.append("<u><b>Can learn</b></u>: Deathtouch");
    	}
    	else if (questData.getPlantLevel() == 2)
    	{
    		sb.append("Improve the toughness of your plant.<br>");
    		sb.append("<u><b>Level 3</b></u>: 0/3<br>");
    		sb.append("<u><b>Next Level</b></u>: 1/3<br>");
    		sb.append("<u><b>Can learn</b></u>: Deathtouch");
    	}
    	else if (questData.getPlantLevel() == 3)
    	{
    		sb.append("Improve the power of your plant.<br>");
    		sb.append("<u><b>Level 4</b></u>: 1/3<br>");
    		sb.append("<u><b>Next Level</b></u>: Deathtouch<br>");
    		sb.append("<u><b>Can learn</b></u>: Deathtouch");
    	}
    	else if (questData.getPlantLevel() == 4)
    	{
    		sb.append("Grow venomous thorns on your plant.<br>");
    		sb.append("<u><b>Level 5</b></u>: Deathtouch<br>");
    		sb.append("<u><b>Next Level</b></u>: 1/4<br>");
    	}
    	else if (questData.getPlantLevel() == 5)
    	{
    		sb.append("As well as gaining more toughness,<br>");
    		sb.append("your plant will have healing properties.<br>");
    		sb.append("<u><b>Level 6</b></u>: 1/4 and Tap, you gain 1 life.");
    	}
    	else
    	{
    		sb.append("Plant Level Maxed out.");
    	}
    	
    	sb.append("</html>");
    	return sb.toString();
    }

    private long getPrice()
    {
    	long l = 0;
    	if (questData.getPlantLevel() == 0)
    		l = 100;
    	else if (questData.getPlantLevel() == 1)
    		l = 150;
    	else if (questData.getPlantLevel() == 2)
    		l = 200;
    	else if (questData.getPlantLevel() == 3)
    		l = 300;
    	else if (questData.getPlantLevel() == 4)
    		l = 750;
    	else if (questData.getPlantLevel() == 5)
    		l = 1000;
    	return l;
    }
    
    private String getButtonText()
    {
    	String s = "";
    	if (questData.getPlantLevel() == 0)
    		s = "Buy Plant";
    	else
    		s = "Upgrade Plant";
    	return s;
    }
    
    private String getStats()
    {
    	StringBuilder sb = new StringBuilder();
    	if (questData.getPlantLevel() == 0)
    		sb.append("0/1");
    	else if (questData.getPlantLevel() == 1)
    		sb.append("0/2");
    	else if (questData.getPlantLevel() == 2)
    		sb.append("0/3");
    	else if (questData.getPlantLevel() == 3)
    		sb.append("1/3");
    	else if (questData.getPlantLevel() == 4)
    		sb.append("1/3");
    	else
    		sb.append("1/4");
    	
    	sb.append(" Plant Wall (current level ");
    	sb.append(questData.getPlantLevel());
    	sb.append("/6)");
    	
    	return sb.toString();
    }
    
    private String getImageString()
    {
    	String s = "";
    	if (questData.getPlantLevel() == 0)
    		s = "g_0_1_plant_wall_small.jpg";
    	else if (questData.getPlantLevel() == 1)
    		s = "g_0_2_plant_wall_small.jpg";
    	else if (questData.getPlantLevel() == 2)
    		s = "g_0_3_plant_wall_small.jpg";
    	else if (questData.getPlantLevel() == 3)
    		s = "g_1_3_plant_wall_small.jpg";
    	else if (questData.getPlantLevel() == 4)
    		s = "g_1_3_plant_wall_deathtouch_small.jpg";
    	else if (questData.getPlantLevel() == 5)
    		s = "g_1_4_plant_wall_small.jpg";
    	
    	return s;
    }
    
    private void jbInit() throws Exception {
        titleLabel.setFont(new Font("sserif", Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("Plant Nursery");
        titleLabel.setBounds(new Rectangle(130, 5, 198, 60));
        stallPanel.setLayout(null);
        
        plantStatsLabel.setFont(new Font("sserif", Font.BOLD, 12));
        plantStatsLabel.setText(getStats());
        plantStatsLabel.setBounds(new Rectangle(10, 65, 200, 15));
        
        plantDescLabel.setFont(new Font("sserif", 0, 12));
        plantDescLabel.setText(getDesc());
        plantDescLabel.setBounds(new Rectangle(10, 80, 300, 150));
        
        plantPriceLabel.setFont(new Font("sserif", 0, 12));
        plantPriceLabel.setText("<html><b><u>Price</u></b>: " + getPrice() + " credits</html>");
        plantPriceLabel.setBounds(new Rectangle(10, 230, 150, 15));
        
        creditsLabel.setFont(new Font("sserif", 0, 12));
        creditsLabel.setText("Credits: " + questData.getCredits());
        creditsLabel.setBounds(new Rectangle(10, 265, 150, 15));
        
        plantIcon = getIcon(getImageString());
        plantIconLabel.setText("");
        plantIconLabel.setIcon(plantIcon);
        plantIconLabel.setBounds(new Rectangle(280, 65, 201, 280));
        plantIconLabel.setIconTextGap(0);
        
        //String fileName = "LeafIconSmall.png";
    	//ImageIcon icon = getIcon(fileName);
    	buyPlantButton.setBounds(new Rectangle(10, 297, 120, 50));
    	buyPlantButton.setText(getButtonText());
    	
    	
    	buyPlantButton.setEnabled(true);
    	if (questData.getCredits() < getPrice() || questData.getPlantLevel() >= 6)
    		buyPlantButton.setEnabled(false);
       
        //jPanel2.add(quitButton, null);
        stallPanel.add(buyPlantButton, null);
        stallPanel.add(titleLabel, null);
        stallPanel.add(plantStatsLabel, null);
        stallPanel.add(plantDescLabel, null);
        stallPanel.add(plantIconLabel, null);
        stallPanel.add(plantPriceLabel, null);
        stallPanel.add(creditsLabel, null);
    }
    

    void buyPlantButton_actionPerformed(ActionEvent e) throws Exception {
    	questData.subtractCredits(getPrice());
    	questData.addPlantLevel();
    	jbInit();
    }

}
