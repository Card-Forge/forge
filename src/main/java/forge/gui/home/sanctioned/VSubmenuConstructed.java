package forge.gui.home.sanctioned;

import java.awt.Font;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.LblHeader;
import forge.gui.home.StartButton;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.ExperimentalLabel;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;

/** 
 * Assembles Swing components of constructed submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 *
 */
public enum VSubmenuConstructed implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Constructed Mode");

    /** */
    private final LblHeader lblTitle = new LblHeader("Sanctioned Format: Constructed");

    private final FLabel lblDecklist1 = new FLabel.Builder()
        .text("Double click a non-random deck for its decklist.")
        .fontSize(12).build();

    private final FLabel lblDecklist2 = new FLabel.Builder()
        .text("Double click a non-random deck for its decklist.")
        .fontSize(12).build();

    private final JPanel pnlRadiosHuman = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final JPanel pnlRadiosAI = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
    private final JPanel pnlStart = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));

    private final StartButton btnStart  = new StartButton();
    private final JList lstDecksUser   = new FList();
    private final JList lstDecksAI      = new FList();

    private final JRadioButton radColorsHuman = new FRadioButton("Fully random color deck");
    private final JRadioButton radThemesHuman = new FRadioButton("Semi-random theme deck");
    private final JRadioButton radCustomHuman = new FRadioButton("Custom user deck");
    private final JRadioButton radQuestsHuman = new FRadioButton("Quest opponent deck");

    private final JRadioButton radColorsAI = new FRadioButton("Fully random color deck");
    private final JRadioButton radThemesAI = new FRadioButton("Semi-random theme deck");
    private final JRadioButton radCustomAI = new FRadioButton("Custom user deck");
    private final JRadioButton radQuestsAI = new FRadioButton("Quest opponent deck");

    private final JCheckBox cbSingletons = new FCheckBox("Singleton Mode");
    private final JCheckBox cbArtifacts = new FCheckBox("Remove Artifacts");
    private final JCheckBox cbRemoveSmall = new FCheckBox("Remove Small Creatures");

    private final JScrollPane scrDecksUser  = new FScrollPane(lstDecksUser,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private final JScrollPane scrDecksAI  = new FScrollPane(lstDecksAI,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private final ExperimentalLabel btnHumanRandom = new ExperimentalLabel("Random");
    private final ExperimentalLabel btnAIRandom = new ExperimentalLabel("Random");

    private VSubmenuConstructed() {
        // Radio button group: Human
        final ButtonGroup grpRadiosHuman = new ButtonGroup();
        grpRadiosHuman.add(radCustomHuman);
        grpRadiosHuman.add(radQuestsHuman);
        grpRadiosHuman.add(radColorsHuman);
        grpRadiosHuman.add(radThemesHuman);

        // Radio button group: AI
        final ButtonGroup grpRadiosAI = new ButtonGroup();
        grpRadiosAI.add(radCustomAI);
        grpRadiosAI.add(radQuestsAI);
        grpRadiosAI.add(radColorsAI);
        grpRadiosAI.add(radThemesAI);

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        // Radio button panels: Human and AI
        final String strRadioConstraints = "w 100%!, h 30px!";

        pnlRadiosHuman.setOpaque(false);
        pnlRadiosHuman.add(new FLabel.Builder().text("Select your deck:")
                .fontStyle(Font.BOLD).fontSize(16)
                .fontAlign(SwingConstants.LEFT).build(), strRadioConstraints);
        pnlRadiosHuman.add(lblDecklist1, "h 20px!, gap 0 0 0 10px");
        pnlRadiosHuman.add(radCustomHuman, strRadioConstraints);
        pnlRadiosHuman.add(radQuestsHuman, strRadioConstraints);
        pnlRadiosHuman.add(radColorsHuman, strRadioConstraints);
        pnlRadiosHuman.add(radThemesHuman, strRadioConstraints);
        pnlRadiosHuman.add(btnHumanRandom, "w 200px!, h 30px!, gap 0 0 10px 0, ax center");

        pnlRadiosAI.setOpaque(false);
        pnlRadiosAI.add(new FLabel.Builder().text("Select an AI deck:")
                .fontStyle(Font.BOLD).fontSize(16)
                .fontAlign(SwingConstants.LEFT).build(), strRadioConstraints);
        pnlRadiosAI.add(lblDecklist2, "h 20px!, gap 0 0 0 10px");
        pnlRadiosAI.add(radCustomAI, strRadioConstraints);
        pnlRadiosAI.add(radQuestsAI, strRadioConstraints);
        pnlRadiosAI.add(radColorsAI, strRadioConstraints);
        pnlRadiosAI.add(radThemesAI, strRadioConstraints);
        pnlRadiosAI.add(btnAIRandom, "w 200px!, h 30px!, gap 0 0 10px 0, ax center");

        final String strCheckboxConstraints = "w 200px!, h 30px!, gap 0 20px 0 0";
        pnlStart.setOpaque(false);
        pnlStart.add(cbSingletons, strCheckboxConstraints);
        pnlStart.add(btnStart, "span 1 3, growx, pushx, align center");
        pnlStart.add(cbArtifacts, strCheckboxConstraints);
        pnlStart.add(cbRemoveSmall, strCheckboxConstraints);
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getGroupEnum()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.SANCTIONED;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "Constructed";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getItemEnum()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_CONSTRUCTED;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap 2, ax right"));
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 80%!, h 40px!, gap 0 0 15px 15px, span 2, ax right");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlRadiosAI, "w 44%!, gap 0 0 20px 20px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlRadiosHuman, "w 44%!, gap 4% 4% 20px 20px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrDecksAI, "w 44%!, growy, pushy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrDecksUser, "w 44%!, gap 4% 4% 0 0, growy, pushy");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlStart, "span 2, gap 0 0 50px 50px, ax center");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstUserDecks() {
        return this.lstDecksUser;
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstDecksAI() {
        return this.lstDecksAI;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    /** @return {@link forge.gui.toolbox.ExperimentalLabel} */
    public ExperimentalLabel getBtnHumanRandom() {
        return this.btnHumanRandom;
    }

    /** @return {@link forge.gui.toolbox.ExperimentalLabel} */
    public ExperimentalLabel getBtnAIRandom() {
        return this.btnAIRandom;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadColorsHuman() {
        return this.radColorsHuman;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadThemesHuman() {
        return this.radThemesHuman;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadCustomHuman() {
        return this.radCustomHuman;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadQuestsHuman() {
        return this.radQuestsHuman;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadColorsAI() {
        return this.radColorsAI;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadThemesAI() {
        return this.radThemesAI;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadCustomAI() {
        return this.radCustomAI;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadQuestsAI() {
        return this.radQuestsAI;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbSingletons() {
        return cbSingletons;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbArtifacts() {
        return cbArtifacts;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JCheckBox getCbRemoveSmall() {
        return cbRemoveSmall;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_CONSTRUCTED;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public ICDoc getLayoutControl() {
        return CSubmenuConstructed.SINGLETON_INSTANCE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell(forge.gui.framework.DragCell)
     */
    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return parentCell;
    }
}
