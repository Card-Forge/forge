package forge.view.toolbox;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * Semi-transparent overlay panel. Should be used with layered panes.
 * 
 */

// Currently used only once, in top level UI, with layering already in place.
// Getter in AllZone: getOverlay()
@SuppressWarnings("serial")
public class FOverlay extends JPanel {
    private JButton btnClose;

    /**
     * Semi-transparent overlay panel. Should be used with layered panes.
     */
    public FOverlay() {
        super();
        btnClose = new JButton("X");
        btnClose.setForeground(Color.white);
        btnClose.setBorder(BorderFactory.createLineBorder(Color.white));
        btnClose.setOpaque(false);
        btnClose.setBackground(new Color(0, 0, 0));
        btnClose.setFocusPainted(false);

        btnClose.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                hideOverlay();
            }
        });
    }

    /** */
    public void showOverlay() {
        setVisible(true);
    }

    /** */
    public void hideOverlay() {
        setVisible(false);
    }

    /**
     * Gets the close button, which must be added dynamically since
     * different overlays have different layouts.  The overlay does
     * not have the close button by default, but a fully working
     * instance is available if required.
     * 
     * @return JButton
     */
    public JButton getBtnClose() {
        return btnClose;
    }

    /** 
     * For some reason, the alpha channel background doesn't work
     * properly on Windows 7, so the paintComponent override is
     * required for a semi-transparent overlay.
     * 
     * @param g &emsp; Graphics object
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(new Color(0, 0, 0, 200));
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
