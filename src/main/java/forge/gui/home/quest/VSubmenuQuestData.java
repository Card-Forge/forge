package forge.gui.home.quest;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.view.home.QuestFileLister;
import forge.view.toolbox.FCheckBox;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FPanel;
import forge.view.toolbox.FRadioButton;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;

/** 
 * Singleton instance of "Colors" submenu in "Constructed" group.
 *
 */
public enum VSubmenuQuestData implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /** */
    private final JPanel pnl = new JPanel();
    private final JPanel pnlViewport = new JPanel();
    private final QuestFileLister lstQuests = new QuestFileLister();

    private final JRadioButton radEasy = new FRadioButton("Easy");
    private final JRadioButton radMedium = new FRadioButton("Medium");
    private final JRadioButton radHard = new FRadioButton("Hard");
    private final JRadioButton radExpert = new FRadioButton("Expert");
    private final JRadioButton radFantasy = new FRadioButton("Fantasy");
    private final JRadioButton radClassic = new FRadioButton("Classic");

    private final JCheckBox cbStandardStart = new FCheckBox("Standard (Type 2) Starting Pool");
    private final FLabel btnEmbark = new FLabel.Builder().opaque(true).hoverable(true).text("Embark!").build();

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        pnl.removeAll();
        pnl.setOpaque(false);
        pnl.setLayout(new MigLayout("insets 0, gap 0"));

        // Load quest
        final FPanel pnlTitleLoad = new FPanel();
        pnlTitleLoad.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleLoad.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleLoad.add(new FLabel.Builder().text("Load a previous Quest")
                .fontScaleAuto(false).fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");

        final FScrollPane scr = new FScrollPane(lstQuests);
        scr.setBorder(null);
        scr.getViewport().setBorder(null);

        // New quest
        final FPanel pnlTitleNew = new FPanel();
        pnlTitleNew.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleNew.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleNew.add(new FLabel.Builder().text("Start a new Quest")
                .fontScaleAuto(false).fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");

        final ButtonGroup group1 = new ButtonGroup();
        group1.add(radEasy);
        group1.add(radMedium);
        group1.add(radHard);
        group1.add(radExpert);

        radEasy.setSelected(true);
        radClassic.setSelected(true);

        final ButtonGroup group2 = new ButtonGroup();
        group2.add(radFantasy);
        group2.add(radClassic);

        final JPanel pnlOptions = new JPanel();
        pnlOptions.setOpaque(false);
        pnlOptions.setLayout(new MigLayout("insets 0, gap 0"));

        final String constraints = "w 40%!, h 30px!";
        pnlOptions.add(radEasy, constraints + ", gap 7.5% 2.5% 0 0");
        pnlOptions.add(radFantasy, constraints + ", wrap");
        pnlOptions.add(radMedium, constraints + ", gap 7.5% 2.5% 0 0");
        pnlOptions.add(radClassic, constraints + ", wrap");
        pnlOptions.add(radHard, constraints + ", gap 7.5% 2.5% 0 0");
        pnlOptions.add(cbStandardStart, constraints + ", wrap");
        pnlOptions.add(radExpert, constraints + ", gap 7.5% 2.5% 0 0, wrap");

        pnlOptions.add(btnEmbark, "w 40%!, h 30px!, gap 30% 0 20px 0, span 3 1");

        // Final layout
        pnlViewport.removeAll();
        pnlViewport.setOpaque(false);
        pnlViewport.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        pnlViewport.add(pnlTitleLoad, "w 96%, h 36px!, gap 2% 0 20px 10px");

        pnlViewport.add(new FLabel.Builder().text("Old quest data? Put into "
                + "res/quest/data, and restart Forge.")
                .fontAlign(SwingConstants.CENTER).fontScaleAuto(false).fontSize(12)
                .build(), "w 96%!, h 18px!, gap 2% 0 0 4px");

        pnlViewport.add(scr, "w 96%!, pushy, growy, gap 2% 0 0 30px");

        pnlViewport.add(pnlTitleNew, "w 96%, h 36px!, gap 2% 0 0 10px");
        pnlViewport.add(pnlOptions, "w 96%!, h 200px!, gap 2% 0 0 20px");

        pnl.add(new FScrollPane(pnlViewport), "w 100%!, h 100%!");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroup() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getPanel()
     */
    @Override
    public JPanel getPanel() {
        return pnl;
    }

    /** @return {@link forge.view.home.QuestFileLister} */
    public QuestFileLister getLstQuests() {
        return this.lstQuests;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadEasy() {
        return radEasy;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadMedium() {
        return radMedium;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadHard() {
        return radHard;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadExpert() {
        return radExpert;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadFantasy() {
        return radFantasy;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadClassic() {
        return radClassic;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbStandardStart() {
        return cbStandardStart;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getBtnEmbark() {
        return btnEmbark;
    }
}
