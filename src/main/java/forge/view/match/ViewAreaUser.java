package forge.view.match;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import forge.view.toolbox.FPanel;

/** 
 * Parent panel for display of input, hand, and dock.
 * SHOULD PROBABLY COLLAPSE INTO TOP LEVEL.
 *
 */
@SuppressWarnings("serial")
public class ViewAreaUser extends FPanel {
    private ViewDock pnlDock;
    private ViewHand pnlHand;

    private JPanel pnlMessage;
    private ViewInput pnlInput;

    /**
     * Assembles user area of match UI.
     */
    public ViewAreaUser() {
        super();
        setOpaque(false);
        setLayout(new MigLayout("fill, insets 0, gap 0"));

        // Input panel
        pnlInput = new ViewInput();

        // Hand panel
        pnlHand = new ViewHand();

        // Dock panel
        pnlDock = new ViewDock();

        // A.D.D.
        add(pnlInput, "h 100%!, west, w 200px!");
        add(pnlHand, "grow, gapleft 5");
        add(pnlDock, "growx, h 50px!, south, gaptop 5, gapleft 5");
    }

    /** @return ViewDock */
    public ViewDock getPnlDock() {
        return pnlDock;
    }

    /** @return ViewHand */
    public ViewHand getPnlHand() {
        return pnlHand;
    }

    /** @return JPanel */
    public JPanel getPnlMessage() {
        return pnlMessage;
    }

    /** @return ViewInput */
    public ViewInput getPnlInput() {
        return pnlInput;
    }
}
