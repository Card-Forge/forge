package forge.view;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;

import forge.AllZone;
import forge.view.toolbox.FOverlay;

/** 
 * Parent JFrame for Forge UI.
 *
 */
@SuppressWarnings("serial")
public class GuiTopLevel extends JFrame {
    private JLayeredPane lpnContent;

    /**
     * Parent JFrame for Forge UI.
     */
    public GuiTopLevel() {
        super();
        setMinimumSize(new Dimension(800, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);

        lpnContent = new JLayeredPane();
        lpnContent.setOpaque(true);
        setContentPane(lpnContent);
        addOverlay();

        setVisible(true);
    }

    /** 
     * Adds overlay panel to modal layer.  Used when removeAll()
     * has been called on the JLayeredPane parent.
     */
    public void addOverlay() {
        final FOverlay pnlOverlay = new FOverlay();
        AllZone.setOverlay(pnlOverlay);
        pnlOverlay.setOpaque(false);
        pnlOverlay.setVisible(false);
        pnlOverlay.setBounds(0, 0, getWidth(), getHeight());
        lpnContent.add(pnlOverlay, JLayeredPane.MODAL_LAYER);
    }
}
