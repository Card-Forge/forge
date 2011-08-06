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

public class Gui_Library extends JFrame implements NewConstants{
	
	private static final long serialVersionUID = 2409591658245091210L;
		
	private JFrame 			  shopsGUI;
	private JLabel            titleLabel        = new JLabel();
	
	private JLabel 			  sleightOfHandDescLabel  = new JLabel();

	private JLabel 			  sleightOfHandPriceLabel = new JLabel();
	private JLabel 			  sleightOfHandIconLabel  = new JLabel();
	
	private JLabel			  creditsLabel     	= new JLabel();
	
	private ImageIcon		  sleightOfHandIcon	  	= new ImageIcon();
	    
	private JButton           sleightOfHandButton= new JButton();
    private JButton			  quitButton 	    = new JButton();
    
    private QuestData 		  questData 	    = AllZone.QuestData;
    
    public Gui_Library(JFrame parent) {
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
        titleLabel.setFont(new java.awt.Font("sserif", Font.BOLD, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("Library");
        titleLabel.setBounds(new Rectangle(130, 5, 198, 60));
        this.getContentPane().setLayout(null);
        
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
        
        //String fileName = "LeafIconSmall.png";
    	//ImageIcon icon = getIcon(fileName);
    	
        sleightOfHandButton.setEnabled(true);
    	if (questData.getCredits() < getPrice() || questData.getSleightOfHandLevel() >= 1)
    		sleightOfHandButton.setEnabled(false);
       
        quitButton.setBounds(new Rectangle(140, 297, 120, 50));
        quitButton.setText("Quit");
        quitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                quitButton_actionPerformed(e);
            }
        });


        //jPanel2.add(quitButton, null);
        this.getContentPane().add(sleightOfHandButton, null);
        this.getContentPane().add(titleLabel, null);
        this.getContentPane().add(sleightOfHandDescLabel, null);
        this.getContentPane().add(sleightOfHandIconLabel, null);
        this.getContentPane().add(sleightOfHandPriceLabel, null);
        this.getContentPane().add(creditsLabel, null);
        this.getContentPane().add(quitButton,null);
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
