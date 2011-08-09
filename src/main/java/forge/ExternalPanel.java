package forge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


/**
 * ExternalPanel.java
 *
 * Created on 13.08.2009
 */


/**
 * The class ExternalPanel. A panel with which some other component can be shown in an external window.
 *
 * @author Clemens Koza
 * @version V0.0 13.08.2009
 */
public class ExternalPanel extends JPanel {

    /** Constant <code>serialVersionUID=9098962430872706173L</code> */
    private static final long serialVersionUID = 9098962430872706173L;
    private Component child, head;
    private JFrame frame;

    /**
     * <p>Constructor for ExternalPanel.</p>
     *
     * @param child a {@link java.awt.Component} object.
     */
    public ExternalPanel(Component child) {
        this(child, BorderLayout.EAST);
    }

    /**
     * <p>Constructor for ExternalPanel.</p>
     *
     * @param child a {@link java.awt.Component} object.
     * @param side a {@link java.lang.String} object.
     */
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

    /**
     * <p>setHeadSide.</p>
     *
     * @param side a {@link java.lang.String} object.
     */
    public void setHeadSide(String side) {
        remove(head);
        add(head, side);
    }

    /** {@inheritDoc} */
    @Override
    protected void addImpl(Component comp, Object constraints, int index) {
        if (comp != child && comp != head) throw new IllegalArgumentException();
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
            if (frame == null) bringOut();
            else bringIn();
        }

        @Override
        public void windowClosing(WindowEvent e) {
            bringIn();
        }
    }
}
