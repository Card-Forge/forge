package forge.screens.bazaar;

import com.google.common.collect.Iterables;

import forge.UiCommand;
import forge.gui.framework.ICDoc;
import forge.quest.bazaar.QuestBazaarManager;
import forge.toolbox.FLabel;

import javax.swing.*;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum CBazaarUI implements ICDoc {
    SINGLETON_INSTANCE;

    /**
     * Controls top-level instance of bazaar.
     * @param v0 &emsp; {@link forge.screens.bazaar.VBazaarUI}
     * @param bazaar
     */
    private CBazaarUI() {
    }

    /** Populate all stalls, and select first one. */
    public void initBazaar(QuestBazaarManager bazaar) {
        VBazaarUI.SINGLETON_INSTANCE.populateStalls();
        ((FLabel) VBazaarUI.SINGLETON_INSTANCE.getPnlAllStalls().getComponent(0)).setSelected(true);
        showStall(Iterables.get(bazaar.getStallNames(), 0), bazaar);
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public void showStall(final String s0, final QuestBazaarManager bazaar) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                VBazaarUI.SINGLETON_INSTANCE.getPnlSingleStall().setStall(bazaar.getStall(s0));
                VBazaarUI.SINGLETON_INSTANCE.getPnlSingleStall().updateStall();
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public UiCommand getCommandOnSelect() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        // TODO Auto-generated method stub
        
    }
}
