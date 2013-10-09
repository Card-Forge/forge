package forge.view;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import forge.Command;
import forge.Singletons;
import forge.control.ControlBazaarUI;
import forge.control.FControl;
import forge.control.FControl.Screens;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FPanel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.SkinImage;
import forge.quest.bazaar.QuestBazaarManager;
import forge.quest.gui.ViewStall;
import forge.view.FNavigationBar.INavigationTabData;


/** Lays out containers and borders for resizeable layout and
 *  instantiates top-level controller for bazaar UI. */
@SuppressWarnings("serial")
public class ViewBazaarUI extends FPanel implements INavigationTabData {
    private final JPanel pnlAllStalls;
    private final ViewStall pnlSingleStall;
    private final ControlBazaarUI control;
    private FLabel previousSelected;
    private final QuestBazaarManager bazaar;

    /** Lays out containers and borders for resizeable layout and
     *  instantiates top-level controller for bazaar UI.
     * @param bazaar0 */
    public ViewBazaarUI(QuestBazaarManager bazaar0) {
        super();

        // Final inits
        this.pnlAllStalls = new JPanel();
        this.pnlSingleStall = new ViewStall(this);
        this.bazaar = bazaar0;

        // Component styling
        this.setCornerDiameter(0);
        this.setBorderToggle(false);
        this.skin.setBackgroundTexture(FSkin.getIcon(FSkin.Backgrounds.BG_TEXTURE));
        this.setLayout(new MigLayout("insets 0, gap 0"));
        pnlAllStalls.setOpaque(false);
        pnlAllStalls.setLayout(new MigLayout("insets 0, gap 0, wrap, align center"));

        // Layout
        this.add(pnlAllStalls, "w 25%!, h 100%!");
        this.add(pnlSingleStall, "w 75%!, h 100%!");

        // Instantiate control
        control = new ControlBazaarUI(this, bazaar);
        control.initBazaar();
        previousSelected = ((FLabel) pnlAllStalls.getComponent(0));
    }

    /** */
    public void populateStalls() {
        for (final String s : bazaar.getStallNames()) {

            final FLabel lbl = new FLabel.ButtonBuilder().text(s + "  ")
                    .fontAlign(SwingConstants.RIGHT).iconInBackground(true)
                    .fontSize(16).icon(FSkin.getIcon(bazaar.getStall(s).getIcon())).build();

            pnlAllStalls.add(lbl, "h 80px!, w 90%!, gap 0 0 10px 10px");

            lbl.setCommand(new Command() {
                @Override
                public void run() {
                    if (previousSelected != null) { previousSelected.setSelected(false); }
                    lbl.setSelected(true);
                    previousSelected = lbl;
                    control.showStall(s);
                }
            });
        }
    }

    /** */
    public void refreshLastInstance() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                pnlSingleStall.updateStall();
            }
        });
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
     * @return {@link forge.quest.gui.ViewStall}
     */
    public ViewStall getPnlSingleStall() {
        return this.pnlSingleStall;
    }
    
    /* (non-Javadoc)
     * @see forge.view.FNavigationBar.INavigationTabData#getTabCaption()
     */
    @Override
    public String getTabCaption() {
        return "Bazaar";
    }

    /* (non-Javadoc)
     * @see forge.view.FNavigationBar.INavigationTabData#getTabIcon()
     */
    @Override
    public SkinImage getTabIcon() {
        return FSkin.getIcon(FSkin.QuestIcons.ICO_BOTTLES);
    }

    /* (non-Javadoc)
     * @see forge.view.FNavigationBar.INavigationTabData#getTabDestScreen()
     */
    @Override
    public Screens getTabDestScreen() {
        return Screens.QUEST_BAZAAR;
    }

    /* (non-Javadoc)
     * @see forge.view.FNavigationBar.INavigationTabData#canCloseTab()
     */
    @Override
    public String getTabCloseButtonTooltip() {
        return "Leave Bazaar";
    }

    /* (non-Javadoc)
     * @see forge.view.FNavigationBar.INavigationTabData#onClosingTab()
     */
    @Override
    public boolean onClosingTab() {
        Singletons.getControl().changeState(FControl.Screens.HOME_SCREEN);
        return true;
    }
}
