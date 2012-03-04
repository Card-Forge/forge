package forge.control;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.SwingUtilities;

import forge.gui.toolbox.FLabel;
import forge.quest.data.bazaar.QuestStallManager;
import forge.view.ViewBazaarUI;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ControlBazaarUI {
    private final ViewBazaarUI view;
    private final ComponentListener cadResize;

    /**
     * Controls top-level instance of bazaar.
     * @param v0 &emsp; {@link forge.view.ViewBazaarUI}
     */
    public ControlBazaarUI(ViewBazaarUI v0) {
        view = v0;

        cadResize = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                view.revalidate();
            }
        };

        addListeners();
    }

    private void addListeners() {
        this.view.removeComponentListener(cadResize);
        this.view.addComponentListener(cadResize);
    }

    /** Populate all stalls, and select first one. */
    public void initBazaar() {
        view.populateStalls();
        ((FLabel) view.getPnlAllStalls().getComponent(0)).setSelected(true);
        showStall(QuestStallManager.getStallNames().get(0));
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public void showStall(final String s0) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                view.getPnlSingleStall().setStall(QuestStallManager.getStall(s0));
                view.getPnlSingleStall().updateStall();
            }
        });
    }
}
