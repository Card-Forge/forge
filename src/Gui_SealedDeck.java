



import java.awt.FlowLayout;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import forge.error.ErrorViewer;


public class Gui_SealedDeck extends JFrame {
    private static final long serialVersionUID = -5073942967852403230L;
    private JLabel            jLabel1          = new JLabel();
    private JPanel            jPanel1          = new JPanel();
    private JButton           jButton3         = new JButton();
    private JButton           jButton2         = new JButton();
    private FlowLayout        flowLayout1      = new FlowLayout();
    private JComboBox         boosterComboBox  = new JComboBox();
    private JLabel            jLabel5          = new JLabel();
    private JLabel            jLabel4          = new JLabel();
    private JComboBox         setComboBox      = new JComboBox();
    
    public Gui_SealedDeck() {
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
    }
    
    private void jbInit() throws Exception {
        jLabel1.setBounds(new Rectangle(118, 14, 220, 69));
        jLabel1.setText("Sealed Deck");
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 26));
        this.getContentPane().setLayout(null);
        jPanel1.setBounds(new Rectangle(89, 230, 237, 43));
        jPanel1.setLayout(flowLayout1);
        jButton3.setText("Next ->");
        jButton2.setText("<- Back");
        boosterComboBox.setBounds(new Rectangle(240, 173, 51, 23));
        jLabel5.setText("Booster Packs Per Person");
        jLabel5.setBounds(new Rectangle(89, 173, 164, 21));
        jLabel4.setText("Set");
        jLabel4.setBounds(new Rectangle(89, 121, 30, 15));
        setComboBox.setBounds(new Rectangle(136, 115, 161, 23));
        jPanel1.add(jButton2, null);
        jPanel1.add(jButton3, null);
        this.getContentPane().add(jLabel1, null);
        this.getContentPane().add(setComboBox, null);
        this.getContentPane().add(jLabel4, null);
        this.getContentPane().add(jLabel5, null);
        this.getContentPane().add(boosterComboBox, null);
        this.getContentPane().add(jPanel1, null);
    }
}