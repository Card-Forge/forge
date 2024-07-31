package forge.screens.bazaar;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import forge.gamemodes.quest.bazaar.QuestBazaarManager;
import forge.gui.GuiBase;
import forge.gui.UiCommand;
import forge.gui.framework.FScreen;
import forge.gui.framework.IVTopLevelUI;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.home.quest.ViewStall;
import forge.toolbox.FLabel;
import forge.toolbox.FPanel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.view.FView;
import net.miginfocom.swing.MigLayout;


/** Lays out containers and borders for resizeable layout and
 *  instantiates top-level controller for bazaar UI. */
public enum VBazaarUI implements IVTopLevelUI {
    /** */
    SINGLETON_INSTANCE;

    private JPanel pnlAllStalls;
    private ViewStall pnlSingleStall;
    private FLabel previousSelected;
    private QuestBazaarManager bazaar;

    /** Lays out containers and borders for resizeable layout and
     *  instantiates top-level controller for bazaar UI.
     * @param bazaar0 */
    VBazaarUI() {
    }

    /** */
    @SuppressWarnings("serial")
    public void populateStalls() {
        for (final String s : bazaar.getStallNames()) {

            final FLabel lbl = new FLabel.ButtonBuilder().text(s + "  ")
                    .fontAlign(SwingConstants.RIGHT).iconInBackground(true).selectable()
                    .fontSize(16).icon((SkinImage)GuiBase.getInterface().getSkinIcon(bazaar.getStall(s).getIcon())).build();

            pnlAllStalls.add(lbl, "h 80px!, w 90%!, gap 0 0 10px 10px");

            lbl.setCommand((UiCommand) () -> {
                if (previousSelected != null) { previousSelected.setSelected(false); }
                lbl.setSelected(true);
                previousSelected = lbl;
                lbl.requestFocusInWindow();
                CBazaarUI.SINGLETON_INSTANCE.showStall(s, bazaar);
            });
        }
    }

    /** */
    public void refreshLastInstance() {
        SwingUtilities.invokeLater(() -> pnlSingleStall.updateStall());
    }

    /**
     * TODO: Write javadoc for this method.
     * @return {@link javax.swing.JPanel}
     */
    public JPanel getPnlAllStalls() {
        return this.pnlAllStalls;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return {@link forge.screens.home.quest.ViewStall}
     */
    public ViewStall getPnlSingleStall() {
        return this.pnlSingleStall;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#instantiate()
     */
    @Override
    public void instantiate() {
        // Final inits
        this.pnlAllStalls = new JPanel();
        this.pnlSingleStall = new ViewStall(this);
        this.bazaar = FModel.getQuest().getBazaar();

        pnlAllStalls.setOpaque(false);
        pnlAllStalls.setLayout(new MigLayout("insets 0, gap 0, wrap, align center"));

        // Instantiate control
        CBazaarUI.SINGLETON_INSTANCE.initBazaar(this.bazaar);
        previousSelected = ((FLabel) pnlAllStalls.getComponent(0));
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#populate()
     */
    @Override
    public void populate() {
        FPanel pnl = FView.SINGLETON_INSTANCE.getPnlInsets();
        pnl.setBorder((Border)null);
        pnl.setLayout(new MigLayout("insets 0, gap 0"));
        pnl.setBackgroundTexture(FSkin.getIcon(FSkinProp.BG_TEXTURE));
        
        pnl.add(pnlAllStalls, "w 25%!, h 100%!");
        pnl.add(pnlSingleStall, "w 75%!, h 100%!");

        SwingUtilities.invokeLater(() -> {
            if (previousSelected != null) {
                previousSelected.requestFocusInWindow();
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVTopLevelUI#onSwitching(forge.gui.framework.FScreen)
     */
    @Override
    public boolean onSwitching(FScreen fromScreen, FScreen toScreen) {
        return true;
    }
 
    /* (non-Javadoc)
     * @see forge.view.FNavigationBar.INavigationTabData#onClosingTab()
     */
    @Override
    public boolean onClosing(FScreen screen) {
        return true;
    }
}
