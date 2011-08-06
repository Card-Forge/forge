package forge;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;


/**
 * ExternalPanel.java
 * 
 * Created on 13.08.2009
 */


/**
 * The class ExternalPanel. A panel with which some other component can be shown in an external window.
 * 
 * @version V0.0 13.08.2009
 * @author Clemens Koza
 */
public class ExternalPanel extends JPanel {

	private static final long serialVersionUID = 9098962430872706173L;
	private Component child, head;
    private JFrame    frame;
    
    public ExternalPanel(Component child) {
        this(child, BorderLayout.EAST);
    }
    
    public ExternalPanel(Component child, String side) {
        super(new BorderLayout());
        add(this.child = child);
        JButton b = new JButton();
        b.setPreferredSize(new Dimension(6, 6));
        b.setToolTipText("Click to move component into an extra Window");
        b.addActionListener(new ExternListener());
        head = b;
        setHeadSide(side);
    }
    
    public void setHeadSide(String side) {
        remove(head);
        add(head, side);
    }
    
    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if(comp != child && comp != head) throw new IllegalArgumentException();
        super.addImpl(comp, constraints, index);
    }
    
    private final class ExternListener extends WindowAdapter implements ActionListener {
        private void bringOut() {
            frame = new JFrame();
            frame.addWindowListener(this);
            frame.addWindowStateListener(this);
            frame.add(child);
            frame.getRootPane().setPreferredSize(child.getSize());
            frame.pack();
            frame.setVisible(true);
            validate();
            repaint();
        }
        
        private void bringIn() {
            add(child);
            frame.dispose();
            frame = null;
            validate();
            repaint();
        }
        
        public void actionPerformed(ActionEvent e) {
            if(frame == null) bringOut();
            else bringIn();
        }
        
        @Override
        public void windowClosing(WindowEvent e) {
            bringIn();
        }
    }
}
