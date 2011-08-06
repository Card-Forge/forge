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

public class Gui_PlantShop extends JFrame implements NewConstants{
	
	private static final long serialVersionUID = 2409591658245091210L;
	private JFrame 			  shopsGUI;
	private JLabel            titleLabel       = new JLabel();
	
	private JLabel 			  plantDescLabel   = new JLabel();
	private JLabel			  plantStatsLabel  = new JLabel();
	private JLabel 			  plantPriceLabel  = new JLabel();
	private JLabel 			  plantIconLabel   = new JLabel();
	
	private JLabel			  creditsLabel     = new JLabel();
	
	private ImageIcon		  plantIcon		   = new ImageIcon();
	    
	private JButton           buyPlantButton   = new JButton();
    private JButton			  quitButton 	   = new JButton();
    
    private QuestData 		  questData 	   = AllZone.QuestData;
    
    public Gui_PlantShop(JFrame parent) {
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
    	
    	sb.append(" Plant Wall");
    	
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
    	
    	return s;
    }
    
    private void jbInit() throws Exception {
        titleLabel.setFont(new java.awt.Font("sserif", Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("Plant Nursery");
        titleLabel.setBounds(new Rectangle(130, 5, 198, 60));
        this.getContentPane().setLayout(null);
        
        plantStatsLabel.setFont(new Font("sserif", Font.BOLD, 12));
        plantStatsLabel.setText(getStats());
        plantStatsLabel.setBounds(new Rectangle(10, 65, 100, 15));
        
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
    	if (questData.getCredits() < getPrice())
    		buyPlantButton.setEnabled(false);
       
        quitButton.setBounds(new Rectangle(140, 297, 120, 50));
        quitButton.setText("Quit");
        quitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                quitButton_actionPerformed(e);
            }
        });


        //jPanel2.add(quitButton, null);
        this.getContentPane().add(buyPlantButton, null);
        this.getContentPane().add(titleLabel, null);
        this.getContentPane().add(plantStatsLabel, null);
        this.getContentPane().add(plantDescLabel, null);
        this.getContentPane().add(plantIconLabel, null);
        this.getContentPane().add(plantPriceLabel, null);
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
    
    void buyPlantButton_actionPerformed(ActionEvent e) throws Exception {
    	questData.subtractCredits(getPrice());
    	questData.addPlantLevel();
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
