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

public class Gui_Gear extends JFrame implements NewConstants{
	
	private static final long serialVersionUID = -2124386606846472829L;
	
	private JFrame 			  shopsGUI;
	private JLabel            titleLabel        = new JLabel();
	
	private JLabel 			  gearDescLabel  = new JLabel();

	private JLabel 			  gearPriceLabel = new JLabel();
	private JLabel 			  gearIconLabel  = new JLabel();
	
	private JLabel			  creditsLabel     	= new JLabel();
	
	private ImageIcon		  gearIcon	  	= new ImageIcon();
	    
	private JButton           gearButton 	= new JButton();
    private JButton			  quitButton 	    = new JButton();
    
    private QuestData 		  questData 	    = AllZone.QuestData;
    
    public Gui_Gear(JFrame parent) {
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
    	gearButton.setBounds(new Rectangle(10, 297, 120, 50));
    	gearButton.setText(getButtonText());
    	//buyPlantButton.setIcon(icon);
    	gearButton.addActionListener(new java.awt.event.ActionListener() {
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

    	if (questData.getGearLevel() == 0) 
    	{
    		sb.append("<u><b>Adventurer's Map</b></u><br>");
    		sb.append("These ancient charts should facilitate navigation during your travels significantly.<br>");
    		sb.append("<u>Quest assignments become available more frequently</u>.");
    	}
    	else if (questData.getGearLevel() == 1)
    	{
    		sb.append("<u><b>Adventurer's Zeppelin</b></u><br>");
    		sb.append("This extremely comfortable airship allows for more efficient and safe travel to faraway destinations. <br>");
    		sb.append("<u>Quest assignments become available more frequently, adds +3 to max life</u>.");
    	}
    	else
    	{
    		sb.append("Currently nothing for sale. <br>Please check back later.");
    	}
    	
    	sb.append("</html>");
    	return sb.toString();
    }

    private long getPrice()
    {
    	long l = 0;
    	if (questData.getGearLevel() == 0)
    		l = 2000; 
    	else if (questData.getGearLevel() == 1)
    		l = 5000;
    	
    	return l;
    }
    
    private String getButtonText()
    {
    	return "Buy";
    }
    
    private String getImageString()
    {
    	if (questData.getGearLevel() == 0)
    		return "MapIconLarge.png";
    	else if (questData.getGearLevel() == 1)
    		return "ZeppelinIcon.png";
    	
    	return "";
    }
    
    private void jbInit() throws Exception {
        titleLabel.setFont(new java.awt.Font("sserif", Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("Adventurer's Gear");
        titleLabel.setBounds(new Rectangle(155, 5, 198, 60));
        this.getContentPane().setLayout(null);
        
        /*
        potionStatsLabel.setFont(new Font("sserif", Font.BOLD, 12));
        potionStatsLabel.setText(getStats());
        potionStatsLabel.setBounds(new Rectangle(10, 65, 100, 15));
        */
        
        gearDescLabel.setFont(new Font("sserif", 0, 12));
        gearDescLabel.setText(getDesc());
        gearDescLabel.setBounds(new Rectangle(10, 80, 300, 150));
        
        gearPriceLabel.setFont(new Font("sserif", 0, 12));
        gearPriceLabel.setText("<html><b><u>Price</u></b>: " + getPrice() + " credits</html>");
        gearPriceLabel.setBounds(new Rectangle(10, 230, 150, 15));
        
        creditsLabel.setFont(new Font("sserif", 0, 12));
        creditsLabel.setText("Credits: " + questData.getCredits());
        creditsLabel.setBounds(new Rectangle(10, 265, 150, 15));
        
        gearIcon = getIcon(getImageString());
        gearIconLabel.setText("");
        gearIconLabel.setIcon(gearIcon);
        gearIconLabel.setBounds(new Rectangle(325, 100, 128, 128));
        gearIconLabel.setIconTextGap(0);
        
        //String fileName = "LeafIconSmall.png";
    	//ImageIcon icon = getIcon(fileName);
    	
        gearButton.setEnabled(true);
    	if (questData.getCredits() < getPrice() || questData.getGearLevel() >= 2)
    		gearButton.setEnabled(false);
       
        quitButton.setBounds(new Rectangle(140, 297, 120, 50));
        quitButton.setText("Quit");
        quitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                quitButton_actionPerformed(e);
            }
        });

        //jPanel2.add(quitButton, null);
        this.getContentPane().add(gearButton, null);
        this.getContentPane().add(titleLabel, null);
        this.getContentPane().add(gearDescLabel, null);
        this.getContentPane().add(gearIconLabel, null);
        this.getContentPane().add(gearPriceLabel, null);
        this.getContentPane().add(creditsLabel, null);
        this.getContentPane().add(quitButton,null);
    }
    
    void learnEstatesButton_actionPerformed(ActionEvent e) throws Exception {
	    	questData.subtractCredits(getPrice());
	    	
	    	if (questData.getGearLevel() < 2)
	    	{
	    		questData.addGearLevel(1);
	    	}
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
