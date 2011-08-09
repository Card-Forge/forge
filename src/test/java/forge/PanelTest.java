package forge;

import forge.error.ErrorViewer;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.Color;
import java.awt.*;


/**
 * <p>PanelTest class.</p>
 *
 * @author Forge
 * @version $Id$
 */
@Test(groups = {"UnitTest"}, timeOut = 1000, enabled = false)
public class PanelTest extends JFrame {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private JPanel jPanel1 = new JPanel();
    private JLabel jLabel1 = new JLabel();

    /**
     *
     */
    @Test(timeOut = 1000, enabled = false)
    public void PhaseTest1() {
        PanelTest p = new PanelTest();
        p.setSize(300, 300);
        p.setVisible(true);
    }

    /**
     * <p>Constructor for PanelTest.</p>
     */
    public PanelTest() {
        try {
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            ex.printStackTrace();
        }
    }

    /**
     * <p>jbInit.</p>
     *
     * @throws java.lang.Exception if any.
     */
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
