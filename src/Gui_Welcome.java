import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;


public class Gui_Welcome extends JFrame {
    private static final long serialVersionUID = -7355345901761929563L;
    
    private JLabel            jLabel1          = new JLabel();
    private JRadioButton      jRadioButton1    = new JRadioButton();
    private JRadioButton      jRadioButton2    = new JRadioButton();
    private JButton           nextButton       = new JButton();
    private ButtonGroup       buttonGroup1     = new ButtonGroup();
    
    public Gui_Welcome() {
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
        setSize(399, 317);
        setVisible(true);
    }
    
    private void jbInit() throws Exception {
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 26));
        jLabel1.setText("Welcome");
        jLabel1.setBounds(new Rectangle(119, 8, 210, 69));
        this.getContentPane().setLayout(null);
        jRadioButton1.setText("Single Game - Play against the Computer");
        jRadioButton1.setBounds(new Rectangle(26, 100, 343, 31));
        
        jRadioButton2.setText("Sealed Deck - Create your deck from Booster Packs");
        jRadioButton2.setBounds(new Rectangle(26, 154, 329, 28));
        nextButton.setBounds(new Rectangle(146, 243, 86, 30));
        nextButton.setText("Next ->");
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                nextButton_actionPerformed(e);
            }
        });
        this.getContentPane().add(jLabel1, null);
        this.getContentPane().add(nextButton, null);
        this.getContentPane().add(jRadioButton2, null);
        this.getContentPane().add(jRadioButton1, null);
        buttonGroup1.add(jRadioButton1);
        buttonGroup1.add(jRadioButton2);
    }
    
    void nextButton_actionPerformed(ActionEvent e) {

    }
    
}