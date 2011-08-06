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

public class Gui_ElixirShop extends JFrame implements NewConstants{
	
	private static final long serialVersionUID = 2409591658245091210L;
		
	private JFrame 			  shopsGUI;
	private JLabel            titleLabel       = new JLabel();
	
	private JLabel 			  potionDescLabel   = new JLabel();
	private JLabel			  potionStatsLabel  = new JLabel();
	private JLabel 			  potionPriceLabel  = new JLabel();
	private JLabel 			  potionIconLabel   = new JLabel();
	
	private JLabel			  creditsLabel     = new JLabel();
	
	private ImageIcon		  potionIcon	   = new ImageIcon();
	    
	private JButton           buyPotionButton   = new JButton();
    private JButton			  quitButton 	   = new JButton();
    
    private QuestData 		  questData 	   = AllZone.QuestData;
    
    public Gui_ElixirShop(JFrame parent) {
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
    	buyPotionButton.setBounds(new Rectangle(10, 297, 120, 50));
    	buyPotionButton.setText(getButtonText());
    	//buyPlantButton.setIcon(icon);
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
    
    private String getImageString()
    {
    	return "ElixirIcon.png";
    }
    
    private void jbInit() throws Exception {
        titleLabel.setFont(new java.awt.Font("sserif", Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("Apothecary");
        titleLabel.setBounds(new Rectangle(130, 5, 198, 60));
        this.getContentPane().setLayout(null);
        
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
        
        potionIcon = getIcon(getImageString());
        potionIconLabel.setText("");
        potionIconLabel.setIcon(potionIcon);
        potionIconLabel.setBounds(new Rectangle(280, 65, 201, 280));
        potionIconLabel.setIconTextGap(0);
        
        //String fileName = "LeafIconSmall.png";
    	//ImageIcon icon = getIcon(fileName);
    	
    	buyPotionButton.setEnabled(true);
    	if (questData.getCredits() < getPrice() || questData.getLife() >= 30)
    		buyPotionButton.setEnabled(false);
       
        quitButton.setBounds(new Rectangle(140, 297, 120, 50));
        quitButton.setText("Quit");
        quitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                quitButton_actionPerformed(e);
            }
        });


        //jPanel2.add(quitButton, null);
        this.getContentPane().add(buyPotionButton, null);
        this.getContentPane().add(titleLabel, null);
        this.getContentPane().add(potionStatsLabel, null);
        this.getContentPane().add(potionDescLabel, null);
        this.getContentPane().add(potionIconLabel, null);
        this.getContentPane().add(potionPriceLabel, null);
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
    
    void buyPotionButton_actionPerformed(ActionEvent e) throws Exception {
	    	questData.subtractCredits(getPrice());
	    	questData.addLife(1);
	    	QuestData.saveData(questData);
	    	jbInit();
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
