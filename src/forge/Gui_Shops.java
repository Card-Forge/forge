
package forge;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


public class Gui_Shops extends JFrame implements NewConstants {
    
	private static final long serialVersionUID = 5462223825071747531L;
	
	private JFrame 			  questGUI;
	
	private JLabel            titleLabel       = new JLabel();
    private JButton           petShopButton    = new JButton();
    private JButton           plantShopButton  = new JButton();
    private JButton 		  healthShopButton = new JButton();
    private JButton			  bankButton	   = new JButton();
    private JButton           quitButton       = new JButton();

    private JPanel            jPanel2          = new JPanel();
    @SuppressWarnings("unused")
    // titledBorder1
    private TitledBorder      titledBorder1;
    @SuppressWarnings("unused")
    // border1
    private Border            border1;
    
    
    public Gui_Shops(JFrame questGUI) {
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
        
        this.questGUI = questGUI;
        
        setup();
        
        //for some reason, the Bazaar window does not return when closing with X
        //for now, just disable X closing:
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); 
        
        Dimension screen = this.getToolkit().getScreenSize();
        setBounds(screen.width / 3, 100, //position
                286, 630); //size
        setVisible(true);
    }
    
    private void setup() {
        ;
        
    }//setup();
    
    private void jbInit() throws Exception {
        titledBorder1 = new TitledBorder("");
        border1 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titleLabel.setFont(new java.awt.Font("Dialog", 0, 22));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setText("Bazaar");
        titleLabel.setBounds(new Rectangle(40, 0, 198, 60));
        this.getContentPane().setLayout(null);
        
        String fileName = "FoxIconSmall.png";
    	ImageIcon icon = getIcon(fileName);
        petShopButton.setBounds(new Rectangle(25, 20, 180, 77));
        petShopButton.setText("Beasts");
        petShopButton.setIcon(icon);
        petShopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                petShopButton_actionPerformed(e);
            }
        });
        fileName = "LeafIconSmall.png";
    	icon = getIcon(fileName);
        plantShopButton.setBounds(new Rectangle(25, 105, 180, 77));
        plantShopButton.setText("Nursery");
        plantShopButton.setIcon(icon);
        plantShopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                plantShopButton_actionPerformed(e);
            }
        });
        
        fileName = "BottlesIconSmall.png";
    	icon = getIcon(fileName);
        healthShopButton.setBounds(new Rectangle(25, 190, 180, 77));
        healthShopButton.setText("Apothecary");
        healthShopButton.setIcon(icon);
        healthShopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                healthShopButton_actionPerformed(e);
            }
        });
        
        fileName = "BoxIconSmall.png";
    	icon = getIcon(fileName);
        bankButton.setBounds(new Rectangle(25, 275, 180, 77));
        bankButton.setText("Treasury");
        bankButton.setIcon(icon);
        bankButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                treasuryButton_actionPerformed(e);
            }
        });
        
        quitButton.setBounds(new Rectangle(45, 445, 180, 77));
        quitButton.setText("Quit");
        quitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                quitButton_actionPerformed(e);
            }
        });

        jPanel2.setBorder(BorderFactory.createLineBorder(Color.black));
        jPanel2.setBounds(new Rectangle(20, 50, 230, 375));
        jPanel2.setLayout(null);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                this_windowClosing(e);
            }
        });

        this.getContentPane().add(jPanel2, null);
        jPanel2.add(plantShopButton, null);
        jPanel2.add(bankButton, null);
        jPanel2.add(healthShopButton, null);
        //jPanel2.add(quitButton, null);
        jPanel2.add(petShopButton, null);
        this.getContentPane().add(titleLabel, null);
        this.getContentPane().add(quitButton, null);
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
    
    void continueButton_actionPerformed(ActionEvent e) {
        //open up "Game" screen
//    AllZone.Computer_Play.reset();//sometimes computer has creature in play in the 2nd game of the match
        AllZone.GameAction.newGame(Constant.Runtime.HumanDeck[0], Constant.Runtime.ComputerDeck[0]);
        AllZone.Display.setVisible(true);
        
        dispose();
    }
    
    void petShopButton_actionPerformed(ActionEvent e) {
    	Gui_PetShop g = new Gui_PetShop(this);
        g.setVisible(true);
        
        dispose();
    }
    
    void plantShopButton_actionPerformed(ActionEvent e) {
    	Gui_PlantShop g = new Gui_PlantShop(this);
        g.setVisible(true);
        
        dispose();
    }
    
    void healthShopButton_actionPerformed(ActionEvent e) {
    	Gui_ElixirShop g = new Gui_ElixirShop(this);
        g.setVisible(true);
        
        dispose();
        
    }
    
    void treasuryButton_actionPerformed(ActionEvent e){
    	Gui_Treasury g = new Gui_Treasury(this);
    	g.setVisible(true);
    	
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
           questGUI.setVisible(true);
           dispose();
       
    }
    
    void this_windowClosing(WindowEvent e) {
        quitButton_actionPerformed(null);
    }
}
