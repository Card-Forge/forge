import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import forge.error.ErrorViewer;


public class Gui_SetEditor extends JFrame {
    private static final long serialVersionUID = 519896860030378941L;
    
    private JLabel            jLabel1          = new JLabel();
    private JScrollPane       jScrollPane1     = new JScrollPane();
    private JScrollPane       jScrollPane2     = new JScrollPane();
    private JPanel            jPanel1          = new JPanel();
    private Border            border1;
    private TitledBorder      titledBorder1;
    private JPanel            jPanel2          = new JPanel();
    private JButton           jButton1         = new JButton();
    private JButton           jButton2         = new JButton();
    private JButton           jButton3         = new JButton();
    private JList             jList1           = new JList();
    private JList             jList2           = new JList();
    private JPanel            jPanel3          = new JPanel();
    private JButton           jButton4         = new JButton();
    private JButton           jButton5         = new JButton();
    private FlowLayout        flowLayout1      = new FlowLayout();
    private JLabel            jLabel2          = new JLabel();
    private JLabel            jLabel3          = new JLabel();
    private JButton           jButton6         = new JButton();
    private JButton           jButton7         = new JButton();
    private JPanel            jPanel4          = new JPanel();
    private Border            border2;
    private TitledBorder      titledBorder2;
    private JLabel            jLabel5          = new JLabel();
    private JLabel            jLabel6          = new JLabel();
    private JLabel            jLabel4          = new JLabel();
    private JLabel            jLabel7          = new JLabel();
    private JLabel            jLabel8          = new JLabel();
    
    public static void main(String[] args) {
        Gui_SetEditor g = new Gui_SetEditor();
        g.setVisible(true);
    }
    
    public Gui_SetEditor() {
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
    }
    
    private void jbInit() throws Exception {
        border1 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder1 = new TitledBorder(border1, "Card Detail");
        border2 = BorderFactory.createEtchedBorder(Color.white, new Color(148, 145, 140));
        titledBorder2 = new TitledBorder(border2, "Set Statistics");
        jLabel1.setBounds(new Rectangle(299, 14, 137, 69));
        jLabel1.setText("Set Editor");
        jLabel1.setFont(new java.awt.Font("Dialog", 0, 26));
        this.getContentPane().setLayout(null);
        jScrollPane1.setBounds(new Rectangle(13, 112, 181, 271));
        jScrollPane2.setBounds(new Rectangle(306, 112, 181, 271));
        jPanel1.setBorder(titledBorder1);
        jPanel1.setBounds(new Rectangle(609, 28, 228, 291));
        jPanel2.setBorder(BorderFactory.createEtchedBorder());
        jPanel2.setBounds(new Rectangle(604, 343, 240, 321));
        jButton1.setBounds(new Rectangle(197, 161, 105, 32));
        jButton1.setText("Add ->");
        jButton2.setBounds(new Rectangle(197, 200, 105, 32));
        jButton2.setText("Add Note ->");
        jButton3.setText("<- Remove");
        jButton3.setBounds(new Rectangle(197, 286, 105, 32));
        jPanel3.setLayout(flowLayout1);
        jPanel3.setBounds(new Rectangle(132, 514, 237, 43));
        jButton4.setText("Cancel");
        jButton5.setText("Save");
        jLabel2.setText("All Available Cards");
        jLabel2.setBounds(new Rectangle(51, 81, 133, 24));
        jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel3.setText("Set");
        jLabel3.setBounds(new Rectangle(305, 82, 179, 22));
        jButton6.setBounds(new Rectangle(495, 119, 53, 54));
        jButton6.setToolTipText("Move a card up in the listing");
        jButton6.setText("Up");
        jButton7.setBounds(new Rectangle(494, 329, 56, 51));
        jButton7.setToolTipText("Move a card down in the listing");
        jButton7.setText("Down");
        jPanel4.setBorder(titledBorder2);
        jPanel4.setBounds(new Rectangle(245, 400, 320, 96));
        jPanel4.setLayout(null);
        jLabel5.setText("White - 10, Green - 10, Black - 10, Red - 10, Blue - 10");
        jLabel5.setBounds(new Rectangle(14, 19, 418, 23));
        jLabel6.setText("Artifact - 2, Land - 1, Multi-Colored - 0, Total - 100");
        jLabel6.setBounds(new Rectangle(14, 45, 418, 23));
        jLabel4.setText("Common - 70, Uncommon - 20, Rare - 10");
        jLabel4.setBounds(new Rectangle(14, 70, 418, 23));
        jLabel7.setText("Elvish Piper - R");
        jLabel7.setBounds(new Rectangle(497, 194, 101, 23));
        jLabel8.setText("Kodoma\'s Reach - C");
        jLabel8.setBounds(new Rectangle(494, 231, 127, 29));
        this.getContentPane().add(jLabel1, null);
        this.getContentPane().add(jScrollPane2, null);
        jScrollPane2.getViewport().add(jList1, null);
        this.getContentPane().add(jScrollPane1, null);
        jScrollPane1.getViewport().add(jList2, null);
        this.getContentPane().add(jButton1, null);
        this.getContentPane().add(jButton2, null);
        this.getContentPane().add(jButton3, null);
        jPanel3.add(jButton5, null);
        jPanel3.add(jButton4, null);
        this.getContentPane().add(jPanel4, null);
        jPanel4.add(jLabel5, null);
        jPanel4.add(jLabel6, null);
        jPanel4.add(jLabel4, null);
        this.getContentPane().add(jPanel3, null);
        this.getContentPane().add(jLabel2, null);
        this.getContentPane().add(jLabel3, null);
        this.getContentPane().add(jButton6, null);
        this.getContentPane().add(jButton7, null);
        this.getContentPane().add(jPanel1, null);
        this.getContentPane().add(jPanel2, null);
        this.getContentPane().add(jLabel7, null);
        this.getContentPane().add(jLabel8, null);
    }
}
