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
	
	private JLabel 			  wolfPetDescLabel   = new JLabel();
	private JLabel			  wolfPetStatsLabel  = new JLabel();
	private JLabel 			  wolfPetPriceLabel  = new JLabel();
	private JLabel 			  wolfPetIconLabel   = new JLabel();
	
	private JLabel			  creditsLabel       = new JLabel();
	
	private ImageIcon		  wolfPetIcon		 = new ImageIcon();
	    
	private JButton           buyWolfPetButton   = new JButton();
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
    	buyWolfPetButton.setBounds(new Rectangle(10, 297, 120, 50));
    	buyWolfPetButton.setText(getButtonText("Wolf"));
    	//buyPlantButton.setIcon(icon);
    	buyWolfPetButton.addActionListener(new java.awt.event.ActionListener() {
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
    		sb.append("Gives Flanking to your Wolf.<br>");
    		sb.append("<u><b>Level 4</b></u>: 2/2 Flanking<br>");
    	}
    	
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
    	return s;
    }
    
    private String getWolfStats()
    {
    	StringBuilder sb = new StringBuilder();
    	if (questData.getWolfPetLevel() == 0)
    		sb.append("1/1");
    	else if (questData.getWolfPetLevel() == 1)
    		sb.append("1/2");
    	else if (questData.getWolfPetLevel() == 2)
    		sb.append("2/2");
    	else if (questData.getWolfPetLevel() == 3)
    		sb.append("2/2");
    	
    	sb.append(" Wolf Pet");
    	
    	return sb.toString();
    }
    
    private String getImageString()
    {
    	String s = "";
    	if (questData.getWolfPetLevel() == 0)
    		s = "g_1_1_wolf_pet_small.jpg";
    	else if (questData.getWolfPetLevel() == 1)
    		s = "g_1_2_wolf_pet_small.jpg";
    	else if (questData.getWolfPetLevel() == 2)
    		s = "g_2_2_wolf_pet_small.jpg";
    	else if (questData.getWolfPetLevel() == 3)
    		s = "g_2_2_wolf_pet_flanking_small.jpg";
    	
    	return s;
    }
    
    private void jbInit() throws Exception {
        titleLabel.setFont(new java.awt.Font("sserif", Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("Beast Emporium");
        titleLabel.setBounds(new Rectangle(150, 5, 198, 60));
        this.getContentPane().setLayout(null);
        
        wolfPetStatsLabel.setFont(new Font("sserif", Font.BOLD, 12));
        wolfPetStatsLabel.setText(getWolfStats());
        wolfPetStatsLabel.setBounds(new Rectangle(10, 65, 100, 15));
        
        wolfPetDescLabel.setFont(new Font("sserif", 0, 12));
        wolfPetDescLabel.setText(getDesc());
        wolfPetDescLabel.setBounds(new Rectangle(10, 80, 300, 150));
        
        wolfPetPriceLabel.setFont(new Font("sserif", 0, 12));
        wolfPetPriceLabel.setText("<html><b><u>Price</u></b>: " + getPrice() + " credits</html>");
        wolfPetPriceLabel.setBounds(new Rectangle(10, 230, 150, 15));
        
        creditsLabel.setFont(new Font("sserif", 0, 12));
        creditsLabel.setText("Credits: " + questData.getCredits());
        creditsLabel.setBounds(new Rectangle(10, 265, 150, 15));
        
        wolfPetIcon = getIcon(getImageString());
        wolfPetIconLabel.setText("");
        wolfPetIconLabel.setIcon(wolfPetIcon);
        wolfPetIconLabel.setBounds(new Rectangle(280, 65, 201, 280));
        wolfPetIconLabel.setIconTextGap(0);
        
    	buyWolfPetButton.setEnabled(true);
    	if (questData.getCredits() < getPrice())
    		buyWolfPetButton.setEnabled(false);
       
        quitButton.setBounds(new Rectangle(140, 297, 120, 50));
        quitButton.setText("Quit");
        quitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                quitButton_actionPerformed(e);
            }
        });


        //jPanel2.add(quitButton, null);
        this.getContentPane().add(buyWolfPetButton, null);
        this.getContentPane().add(titleLabel, null);
        this.getContentPane().add(wolfPetStatsLabel, null);
        this.getContentPane().add(wolfPetDescLabel, null);
        this.getContentPane().add(wolfPetIconLabel, null);
        this.getContentPane().add(wolfPetPriceLabel, null);
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
    	questData.addWolfPetLevel();
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
