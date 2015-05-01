package forge.screens.bazaar;

import javax.swing.SwingUtilities;

import com.google.common.collect.Iterables;

import forge.gui.framework.ICDoc;
import forge.quest.bazaar.QuestBazaarManager;
import forge.toolbox.FLabel;

public enum CBazaarUI implements ICDoc {
    SINGLETON_INSTANCE;

    private final VBazaarUI view = VBazaarUI.SINGLETON_INSTANCE;

    /**
     * Controls top-level instance of bazaar.
     * @param v0 &emsp; {@link forge.screens.bazaar.VBazaarUI}
     * @param bazaar
     */
    private CBazaarUI() {
    }

    /** Populate all stalls, and select first one. */
    public void initBazaar(final QuestBazaarManager bazaar) {
        view.populateStalls();
        ((FLabel) view.getPnlAllStalls().getComponent(0)).setSelected(true);
        showStall(Iterables.get(bazaar.getStallNames(), 0), bazaar);
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public void showStall(final String s0, final QuestBazaarManager bazaar) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {
                view.getPnlSingleStall().setStall(bazaar.getStall(s0));
                view.getPnlSingleStall().updateStall();
            }
        });
    }

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }
}
