package forge.view.home;

import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;
import forge.control.home.ControlConstructed;
import forge.view.ViewHomeUI;
import forge.view.toolbox.FCheckBox;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FProgressBar;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;
import forge.view.toolbox.SubTab;

/** 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class ViewConstructed extends JPanel {
    private final JPanel pnlTabber, tabHuman, tabAI, pnlStart;
    private final ConstructedDeckSelectPanel pnlHuman, pnlAI;
    private final JCheckBox cbSingletons, cbArtifacts, cbRemoveSmall;
    private final JScrollPane scrContent;
    private final JButton btnStart;
    private final FProgressBar barProgress;
    private  ControlConstructed control;

    /**
     * 
     * TODO: Write javadoc for Constructor.
     * @param v0 &emsp; {@link forge.view.ViewHomeUI}
     */
    public ViewConstructed(ViewHomeUI v0) {
        // Instantiation
        pnlTabber = new JPanel();
        pnlStart = new JPanel();
        pnlHuman = new ConstructedDeckSelectPanel();
        pnlAI = new ConstructedDeckSelectPanel();

        tabHuman = new SubTab("Human Deck Select");
        tabAI = new SubTab("AI Deck Select");

        scrContent = new FScrollPane(null);
        scrContent.setBorder(null);
        scrContent.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrContent.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        cbSingletons = new FCheckBox("Singleton Mode");
        cbArtifacts = new FCheckBox("Remove Artifacts");
        cbRemoveSmall = new FCheckBox("Remove Small Creatures");
        btnStart = new StartButton(v0);
        barProgress = new FProgressBar();

        // Population
        populateTabber();
        populateStart();

        // Styling and layout
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
        this.add(pnlTabber, "w 98%!, gap 1% 0 20px 10px");
        this.add(scrContent, "w 98%!, h 48%!, gap 1% 0 0 10px");
        this.add(pnlStart, "w 98%!, gap 1% 0 0 20px");

        // After all components are instantiated, fire up control.
        this.control = new ControlConstructed(this);
        showHumanTab();
    }

    private void populateTabber() {
        final String tabberConstraints = "w 50%!, h 20px!";

        tabHuman.setToolTipText("Global preference options");
        tabAI.setToolTipText("Human and AI avatar select");

        pnlTabber.setOpaque(false);
        pnlTabber.setLayout(new MigLayout("insets 0, gap 0, align center"));

        pnlTabber.add(tabHuman, tabberConstraints);
        pnlTabber.add(tabAI, tabberConstraints + ", wrap");
    }

    private void populateStart() {
        final String rowConstraints = "ax center, gap 0 0 0 5px";

        final JLabel lblBlurb1 = new FLabel.Builder()
                .text("Mouse over a list above for more information.")
                .fontScaleAuto(false).build();
        JLabel lblBlurb2 = new FLabel.Builder().fontStyle(Font.PLAIN)
                .text("Deck generation options:")
                    .fontScaleAuto(false).build();

        barProgress.setVisible(false);
        pnlStart.setOpaque(false);
        lblBlurb1.setFont(FSkin.getFont(12));
        lblBlurb2.setFont(FSkin.getFont(12));

        pnlStart.setLayout(new MigLayout("insets 0, gap 0, wrap, align center, hidemode 3"));

        pnlStart.add(lblBlurb1, rowConstraints + ", h 12px!");
        pnlStart.add(lblBlurb2, rowConstraints + ", h 12px!");
        pnlStart.add(cbSingletons, rowConstraints);
        pnlStart.add(cbArtifacts, rowConstraints);
        pnlStart.add(cbRemoveSmall, rowConstraints);
        pnlStart.add(btnStart, rowConstraints + ", gap 0 0 0 0");
        pnlStart.add(barProgress, rowConstraints + ", w 150px!, h 30px!");
    }

    /** */
    public final void showHumanTab() {
        this.scrContent.setViewportView(pnlHuman);
        control.updateTabber(tabHuman);
    }

    /** */
    public final void showAITab() {
        this.scrContent.setViewportView(pnlAI);
        control.updateTabber(tabAI);
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getTabHuman() {
        return this.tabHuman;
    }

    /** @return {@link javax.swing.JPanel} */
    public JPanel getTabAI() {
        return this.tabAI;
    }

    /** @return {@link forge.view.toolbox.FProgressBar} */
    public FProgressBar getBarProgress() {
        return this.barProgress;
    }

    /** @return {@link javax.swing.JPanel} */
    public ConstructedDeckSelectPanel getPnlHuman() {
        return this.pnlHuman;
    }

    /** @return {@link javax.swing.JPanel} */
    public ConstructedDeckSelectPanel getPnlAI() {
        return this.pnlAI;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbSingletons() {
        return this.cbSingletons;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbRemoveSmall() {
        return this.cbRemoveSmall;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbArtifacts() {
        return this.cbArtifacts;
    }

    /**
     * 
     * TODO: Write javadoc for this method.
     * @return {@link forge.control.home.ControlConstructed}
     */
    public ControlConstructed getControl() {
        return this.control;
    }
}
