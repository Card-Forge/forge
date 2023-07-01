package forge;

import java.awt.Color;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.testng.annotations.Test;

import forge.gui.error.BugReporter;

/**
 * <p>
 * PanelTest class.
 * </p>
 * 
 * @author Forge
 * @version $Id: PanelTest.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
@Test(groups = { "UnitTest" }, timeOut = 1000, enabled = false)
public class PanelTest extends JFrame {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final JPanel jPanel1 = new JPanel();
    private final JLabel jLabel1 = new JLabel();

    /**
     * Phase test1.
     */
    @Test(timeOut = 1000, enabled = false)
    public void phaseTest1() {
        PanelTest p = new PanelTest();
        p.setSize(300, 300);
        p.setVisible(true);
    }

    /**
     * <p>
     * Constructor for PanelTest.
     * </p>
     */
    public PanelTest() {
        try {
            this.jbInit();
        }
        catch (final Exception ex) {
            BugReporter.reportException(ex);
            ex.printStackTrace();
        }
    }

    /**
     * <p>
     * jbInit.
     * </p>
     * 
     * @throws java.lang.Exception
     *             if any.
     */
    private void jbInit() throws Exception {
        this.getContentPane().setLayout(null);
        this.jPanel1.setForeground(Color.orange);
        this.jPanel1.setBounds(new Rectangle(15, 36, 252, 156));
        this.jLabel1.setFont(new java.awt.Font("Dialog", 1, 12));
        this.jLabel1.setForeground(new Color(70, 90, 163));
        this.jLabel1.setText("jLabel1");
        this.getContentPane().add(this.jPanel1, null);
        this.jPanel1.add(this.jLabel1, null);
    }
}
