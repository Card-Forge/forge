package forge;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

public class Gui_PetShop extends JFrame implements NewConstants{
	
	private static final long serialVersionUID = -2910767196498677881L;
	private JFrame 			  shopsGUI;
	private JLabel            titleLabel         = new JLabel();
	
	private JLabel 			  petDescLabel   	 = new JLabel();
	private JLabel			  petStatsLabel  	 = new JLabel();
	private JLabel 			  petPriceLabel  	 = new JLabel();
	private JLabel 			  petIconLabel   	 = new JLabel();
	
	private JLabel			  creditsLabel       = new JLabel();
	
	private ImageIcon		  petIcon			 = new ImageIcon();
	    
	private JButton           buyPetButton   	 = new JButton();
    private JButton			  quitButton 	  	 = new JButton();
    
    private QuestData 		  questData 	 	 = AllZone.QuestData;
    
    public Gui_PetShop(JFrame parent) {
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
        
        shopsGUI = parent;
        
        setup();
        
        //for some reason, the Bazaar window does not return when closing with X
        //for now, just disable X closing:
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        
        Dimension screen = this.getToolkit().getScreenSize();
        setBounds(screen.width / 3, 100, //position
                530, 430); //size
        setVisible(true);
        
        
    }
    
    //only do this ONCE:
    private void setup() {
    	//String fileName = "LeafIconSmall.png";
    	//ImageIcon icon = getIcon(fileName);
    	buyPetButton.setBounds(new Rectangle(10, 297, 120, 50));
    	
    	buyPetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
					buyPetButton_actionPerformed(e);
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
    	if (questData.getWolfPetLevel() == 0)
    	{
    		sb.append("This ferocious animal may have been raised<br> in captivity, but it has been trained to kill.<br>");
    		sb.append("Eats goblins for breakfast.<br><br>");
    		sb.append("<u><b>Level 1</b></u>: 1/1<br>");
    		sb.append("<u><b>Next Level</b></u>: 1/2<br>");
    		sb.append("<u><b>Can learn</b></u>: Flanking");
    	}
    	else if (questData.getWolfPetLevel() == 1)
    	{
    		sb.append("Improve the toughness of your wolf.<br>");
    		sb.append("<u><b>Level 2</b></u>: 1/2<br>");
    		sb.append("<u><b>Next Level</b></u>: 2/2<br>");
    		sb.append("<u><b>Can learn</b></u>: Flanking");
    	}
    	else if (questData.getWolfPetLevel() == 2)
    	{
    		sb.append("Improve the attack power of your wolf.<br>");
    		sb.append("<u><b>Level 3</b></u>: 2/2<br>");
    		sb.append("<u><b>Next Level</b></u>: Flanking<br>");
    		sb.append("<u><b>Can learn</b></u>: Flanking");
    	}
    	else if (questData.getWolfPetLevel() == 3)
    	{
    		sb.append("Gives Flanking to your wolf.<br>");
    		sb.append("<u><b>Level 4</b></u>: 2/2 Flanking<br>");
    	}
    	else if (questData.getWolfPetLevel() >= 4)
    	{
    		//sb.append("Wolf Level Maxed out.<br>");
    		
    		if (questData.getCrocPetLevel() == 0)
    		{
    			sb.append("With its razor sharp teeth, this swamp-dwelling monster is extremely dangerous.");
    			sb.append("Crikey mate!<br><br>");
        		sb.append("<u><b>Level 1</b></u>: 1/1<br>");
        		sb.append("<u><b>Next Level</b></u>: 2/1<br>");
        		sb.append("<u><b>Can learn</b></u>: Swampwalk");
    		}
    		else if (questData.getCrocPetLevel() == 1)
    		{
    			sb.append("Improve the attack power of your croc.<br>");
        		sb.append("<u><b>Level 2</b></u>: 2/1<br>");
        		sb.append("<u><b>Next Level</b></u>: 3/1<br>");
        		sb.append("<u><b>Can learn</b></u>: Swampwalk");
    		}
    		else if (questData.getCrocPetLevel() == 2)
    		{
    			sb.append("Improve the attack power of your croc.<br>");
        		sb.append("<u><b>Level 3</b></u>: 3/1<br>");
        		sb.append("<u><b>Next Level</b></u>: 3/1 Swampwalk<br>");
    		}
    		else if (questData.getCrocPetLevel() == 3)
    		{
    			sb.append("Gives Swampwalk to your croc.<br>");
        		sb.append("<u><b>Level 4</b></u>: 3/1 Swampwalk<br>");
    		}
    		/*
    		else if (questData.getCrocPetLevel() >= 4)
    		{
    			sb.append("Croc Level Maxed out.<br>");
    		}
    		*/

    		else if (questData.getBirdPetLevel() == 0)
    		{
    			sb.append("Unmatched in speed, agility and awareness,<br>");
    			sb.append("this trained hawk makes a fantastic hunter.<br><br>");
        		sb.append("<u><b>Level 1</b></u>: 0/1 Flying<br>");
        		sb.append("<u><b>Next Level</b></u>: 1/1<br>");
        		sb.append("<u><b>Can learn</b></u>: First strike");
    		}
    		else if (questData.getBirdPetLevel() == 1)
    		{
    			sb.append("Improve the attack power of your bird.<br>");
        		sb.append("<u><b>Level 2</b></u>: 1/1<br>");
        		sb.append("<u><b>Next Level</b></u>: 2/1 <br>");
        		sb.append("<u><b>Can learn</b></u>: First strike");
    		}
    		else if (questData.getBirdPetLevel() == 2)
    		{
    			sb.append("Improve the attack power of your bird.<br>");
        		sb.append("<u><b>Level 3</b></u>: 2/1<br>");
        		sb.append("<u><b>Next Level</b></u>: 2/1 First strike<br>");
    		}
    		else if (questData.getBirdPetLevel() == 3)
    		{
    			sb.append("Gives First strike to your bird.<br>");
        		sb.append("<u><b>Level 4</b></u>: 2/1 First strike<br>");
    		}
    		else if (questData.getHoundPetLevel() == 0)
    		{
    			sb.append("Dogs are said to be man's best friend.<br>");
    			sb.append("Definitely not this one.<br><br>");
        		sb.append("<u><b>Level 1</b></u>: 1/1<br>");
        		sb.append("<u><b>Next Level</b></u>: 1/1 Haste<br>");
        		sb.append("<u><b>Can learn</b></u>: Whenever this creature attacks alone,<br> it gets +2/+0 until end of turn.");
    		}
    		else if (questData.getHoundPetLevel() == 1)
    		{
    			sb.append("Gives haste to your hound.<br>");
        		sb.append("<u><b>Level 2</b></u>: 1/1 Haste<br>");
        		sb.append("<u><b>Next Level</b></u>: 2/1 Haste<br>");
        		sb.append("<u><b>Can learn</b></u>: Whenever this creature attacks alone,<br> it gets +2/+0 until end of turn.");
    		}
    		else if (questData.getHoundPetLevel() == 2)
    		{
    			sb.append("Improve the attack power of your hound.<br>");
        		sb.append("<u><b>Level 3</b></u>: 2/1 Haste<br>");
        		sb.append("<u><b>Next Level</b></u>: 2/1 Whenever this creature attacks<br> alone, it gets +2/+0 until end of turn.<br>");
    		}
    		else if (questData.getHoundPetLevel() == 3)
    		{
    			sb.append("Greatly improves your hound's attack power if it<br> attacks alone.<br>");
        		sb.append("<u><b>Level 4</b></u>: 2/1 Haste, whenever this creature attacks alone, it gets +2/+0 until end of turn.<br>");
    		}
    	}//wolfPetLevel >= 4
    	
    	
    	sb.append("</html>");
    	return sb.toString();
    }

    private long getPrice()
    {
    	long l = 0;
    	if (questData.getWolfPetLevel() == 0)
    		l = 250;
    	else if (questData.getWolfPetLevel() == 1)
    		l = 250;
    	else if (questData.getWolfPetLevel() == 2)
    		l = 500;
    	else if (questData.getWolfPetLevel() == 3)
    		l = 550;
    	else if (questData.getWolfPetLevel() >= 4)
    	{
    		if (questData.getCrocPetLevel() == 0)
    			l = 250;
    		else if (questData.getCrocPetLevel() == 1)
    			l = 300;
    		else if (questData.getCrocPetLevel() == 2)
    			l = 450;
    		else if (questData.getCrocPetLevel() == 3)
    			l = 600;
    		else if (questData.getBirdPetLevel() == 0)
    			l = 200;
    		else if (questData.getBirdPetLevel() == 1)
    			l = 300;
    		else if (questData.getBirdPetLevel() == 2)
    			l = 450;
    		else if (questData.getBirdPetLevel() == 3)
    			l = 400;
    		else if (questData.getHoundPetLevel() == 0)
    			l = 200;
    		else if (questData.getHoundPetLevel() == 1)
    			l = 350;
    		else if (questData.getHoundPetLevel() == 2)
    			l = 450;
    		else if (questData.getHoundPetLevel() == 3)
    			l = 750;
    	}
    	return l;
    }
    
    private String getButtonText(String pet)
    {
    	String s = "";
    	if (pet.equals("Wolf")) {
	    	if (questData.getWolfPetLevel() == 0)
	    		s = "Buy " + pet;
	    	else
	    		s = "Train " + pet;
    	}
    	else if (pet.equals("Croc"))
    	{
    		if (questData.getCrocPetLevel() == 0)
	    		s = "Buy " + pet;
	    	else
	    		s = "Train " + pet;
    	}
    	else if (pet.equals("Bird"))
    	{
    		if (questData.getBirdPetLevel() == 0)
	    		s = "Buy " + pet;
	    	else
	    		s = "Train " + pet;
    	}
    	else if (pet.equals("Hound"))
    	{
    		if (questData.getHoundPetLevel() == 0)
	    		s = "Buy " + pet;
	    	else
	    		s = "Train " + pet;
    	}
    	return s;
    }
    
    private String getPetStats()
    {
    	StringBuilder sb = new StringBuilder();
    	if (questData.getWolfPetLevel() < 4) {
	    	if (questData.getWolfPetLevel() == 1)
	    		sb.append("1/1");
	    	else if (questData.getWolfPetLevel() == 2)
	    		sb.append("1/2");
	    	else if (questData.getWolfPetLevel() >= 3)
	    		sb.append("2/2");
	    	
	    	sb.append(" Wolf Pet (current level ");
	    	sb.append(questData.getWolfPetLevel());
    	} //getWolfPetLevel < 4
    	else if (questData.getCrocPetLevel() < 4)
    	{
    		if (questData.getCrocPetLevel() == 1)
	    		sb.append("1/1");
	    	else if (questData.getCrocPetLevel() == 2)
	    		sb.append("2/1");
	    	else if (questData.getCrocPetLevel() >= 3)
	    		sb.append("3/1");

	    	sb.append(" Croc Pet (current level ");
	    	sb.append(questData.getCrocPetLevel());
    	}
    	else if (questData.getBirdPetLevel() < 4)
    	{
    		if (questData.getBirdPetLevel() == 1)
	    		sb.append("0/1");
	    	else if (questData.getBirdPetLevel() == 2)
	    		sb.append("1/1");
	    	else if (questData.getBirdPetLevel() >= 3)
	    		sb.append("2/1");

	    	sb.append(" Bird Pet (current level ");
	    	sb.append(questData.getBirdPetLevel());
    	}
    	else if (questData.getHoundPetLevel() <= 4)
    	{
    		if (questData.getHoundPetLevel() == 1)
	    		sb.append("1/1");
	    	else if (questData.getHoundPetLevel() == 2)
	    		sb.append("1/1");
	    	else if (questData.getHoundPetLevel() >= 3)
	    		sb.append("2/1");

	    	sb.append(" Hound Pet (current level ");
	    	sb.append(questData.getHoundPetLevel());
    	}
    	
    	sb.append("/4)");
    	if (questData.getWolfPetLevel() == 0)
    		return "";
    		
    	return sb.toString();
    }
    
    private String getImageString()
    {
    	String s = "";
    	if (questData.getWolfPetLevel() < 4)
    	{
	    	if (questData.getWolfPetLevel() == 0)
	    		s = "g_1_1_wolf_pet_small.jpg";
	    	else if (questData.getWolfPetLevel() == 1)
	    		s = "g_1_2_wolf_pet_small.jpg";
	    	else if (questData.getWolfPetLevel() == 2)
	    		s = "g_2_2_wolf_pet_small.jpg";
	    	else if (questData.getWolfPetLevel() == 3)
	    		s = "g_2_2_wolf_pet_flanking_small.jpg";
    	}
    	else if (questData.getCrocPetLevel() < 4)
    	{
    		if (questData.getCrocPetLevel() == 0)
	    		s = "b_1_1_crocodile_pet_small.jpg";
	    	else if (questData.getCrocPetLevel() == 1)
	    		s = "b_2_1_crocodile_pet_small.jpg";
	    	else if (questData.getCrocPetLevel() == 2)
	    		s = "b_3_1_crocodile_pet_small.jpg";
	    	else if (questData.getCrocPetLevel() == 3)
	    		s = "b_3_1_crocodile_pet_swampwalk_small.jpg";
    	}
    	else if (questData.getBirdPetLevel() < 4)
    	{
    		if (questData.getBirdPetLevel() == 0)
	    		s = "w_0_1_bird_pet_small.jpg";
	    	else if (questData.getBirdPetLevel() == 1)
	    		s = "w_1_1_bird_pet_small.jpg";
	    	else if (questData.getBirdPetLevel() == 2)
	    		s = "w_2_1_bird_pet_small.jpg";
	    	else if (questData.getBirdPetLevel() == 3)
	    		s = "w_2_1_bird_pet_first_strike_small.jpg";
    	}
    	
    	else if (questData.getHoundPetLevel() < 4)
    	{
    		if (questData.getHoundPetLevel() == 0)
	    		s = "r_1_1_hound_pet_small.jpg";
	    	else if (questData.getHoundPetLevel() == 1)
	    		s = "r_1_1_hound_pet_haste_small.jpg";
	    	else if (questData.getHoundPetLevel() == 2)
	    		s = "r_2_1_hound_pet_small.jpg";
	    	else if (questData.getHoundPetLevel() == 3)
	    		s = "r_2_1_hound_pet_alone_small.jpg";
    	}
    	
    	return s;
    }
    
    private void jbInit() throws Exception {
        titleLabel.setFont(new java.awt.Font("sserif", Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("Beast Emporium");
        titleLabel.setBounds(new Rectangle(150, 5, 198, 60));
        this.getContentPane().setLayout(null);
        
        petStatsLabel.setFont(new Font("sserif", Font.BOLD, 12));
        petStatsLabel.setText(getPetStats());
        petStatsLabel.setBounds(new Rectangle(10, 65, 200, 15));
        
        petDescLabel.setFont(new Font("sserif", 0, 12));
        petDescLabel.setText(getDesc());
        petDescLabel.setBounds(new Rectangle(10, 80, 300, 150));
        
        petPriceLabel.setFont(new Font("sserif", 0, 12));
        petPriceLabel.setText("<html><b><u>Price</u></b>: " + getPrice() + " credits</html>");
        petPriceLabel.setBounds(new Rectangle(10, 230, 150, 15));
        
        creditsLabel.setFont(new Font("sserif", 0, 12));
        creditsLabel.setText("Credits: " + questData.getCredits());
        creditsLabel.setBounds(new Rectangle(10, 265, 150, 15));
        
        petIcon = getIcon(getImageString());
        petIconLabel.setText("");
        petIconLabel.setIcon(petIcon);
        petIconLabel.setBounds(new Rectangle(280, 65, 201, 280));
        petIconLabel.setIconTextGap(0);
        
        if (questData.getWolfPetLevel() < 4)
    		buyPetButton.setText(getButtonText("Wolf"));
    	else if (questData.getCrocPetLevel() < 4)
    		buyPetButton.setText(getButtonText("Croc"));
    	else if (questData.getBirdPetLevel() < 4)
    		buyPetButton.setText(getButtonText("Bird"));
    	else if (questData.getHoundPetLevel() < 4)
    		buyPetButton.setText(getButtonText("Hound"));
        
    	buyPetButton.setEnabled(true);
    	if (questData.getCredits() < getPrice() || questData.getHoundPetLevel() >= 4)
    		buyPetButton.setEnabled(false);
       
        quitButton.setBounds(new Rectangle(140, 297, 120, 50));
        quitButton.setText("Quit");
        quitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                quitButton_actionPerformed(e);
            }
        });


        //jPanel2.add(quitButton, null);
        this.getContentPane().add(buyPetButton, null);
        this.getContentPane().add(titleLabel, null);
        this.getContentPane().add(petStatsLabel, null);
        this.getContentPane().add(petDescLabel, null);
        this.getContentPane().add(petIconLabel, null);
        this.getContentPane().add(petPriceLabel, null);
        this.getContentPane().add(creditsLabel, null);
        this.getContentPane().add(quitButton,null);
    }
    
    void editDeckButton_actionPerformed(ActionEvent e) {
        Command exit = new Command() {
            private static final long serialVersionUID = 4735992294414389187L;
            
            public void execute() {
                new Gui_WinLose();
            }
        };
        Gui_DeckEditor editor = new Gui_DeckEditor();
        
        editor.show(exit);
        
        dispose();
    }//editDeckButton_actionPerformed()
    
    void buyPetButton_actionPerformed(ActionEvent e) throws Exception {
    	questData.subtractCredits(getPrice());
    	
    	if (questData.getWolfPetLevel() < 4)
    		questData.addWolfPetLevel();
    	else if (questData.getCrocPetLevel() < 4)
    		questData.addCrocPetLevel();
    	else if (questData.getBirdPetLevel() < 4)
    		questData.addBirdPetLevel();
    	else if (questData.getHoundPetLevel() < 4)
    		questData.addHoundPetLevel();
    		
    	QuestData.saveData(questData);
    	jbInit();
    }
    
    void restartButton_actionPerformed(ActionEvent e) {
        Constant.Runtime.WinLose.reset();
        AllZone.GameAction.newGame(Constant.Runtime.HumanDeck[0], Constant.Runtime.ComputerDeck[0]);
        AllZone.Display.setVisible(true);
        
        dispose();
    }
    
    private ImageIcon getIcon(String fileName)
    {
    	File base = ForgeProps.getFile(IMAGE_ICON);
    	File file = new File(base, fileName);
    	ImageIcon icon = new ImageIcon(file.toString());
    	return icon;
    }
    
    void quitButton_actionPerformed(ActionEvent e) {
    	QuestData.saveData(questData);
        //new Gui_Shops();
    	shopsGUI.setVisible(true);
    	
        
        dispose();
       
    }
    
    void this_windowClosing(WindowEvent e) {
        quitButton_actionPerformed(null);
    }
    
}
