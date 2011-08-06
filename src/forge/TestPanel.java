package forge;
import java.awt.Color;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import forge.error.ErrorViewer;


public class TestPanel extends JFrame {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    private JPanel            jPanel1          = new JPanel();
    private JLabel            jLabel1          = new JLabel();
    
    public static void main(String[] args) {
        TestPanel p = new TestPanel();
        p.setSize(300, 300);
        p.setVisible(true);
    }
    
    public TestPanel() {
        try {
            jbInit();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            ex.printStackTrace();
        }
    }
    
    private void jbInit() throws Exception {
        this.getContentPane().setLayout(null);
        jPanel1.setForeground(Color.orange);
        jPanel1.setBounds(new Rectangle(15, 36, 252, 156));
        jLabel1.setFont(new java.awt.Font("Dialog", 1, 12));
        jLabel1.setForeground(new Color(70, 90, 163));
        jLabel1.setText("jLabel1");
        this.getContentPane().add(jPanel1, null);
        jPanel1.add(jLabel1, null);
    }
}
