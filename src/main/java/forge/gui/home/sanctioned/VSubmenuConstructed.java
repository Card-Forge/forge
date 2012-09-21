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
import forge.gui.home.StartButton;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FList;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPane;

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
    private final JPanel pnl            = new JPanel();
    private final StartButton btnStart  = new StartButton();
    private final JList lstHumanDecks   = new FList();
    private final JList lstAIDecks      = new FList();

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

    private final JScrollPane scrHumanDecks  = new FScrollPane(lstHumanDecks,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private final JScrollPane scrAIDecks  = new FScrollPane(lstAIDecks,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    private final FLabel btnHumanRandom = new FLabel.Builder().text("Random").fontSize(14).opaque(true)
            .hoverable(true).build();

    private final FLabel btnAIRandom = new FLabel.Builder().text("Random").fontSize(14).opaque(true)
            .hoverable(true).build();

    private VSubmenuConstructed() {
        // Radio button group: Human
        final ButtonGroup grpRadiosHuman = new ButtonGroup();
        grpRadiosHuman.add(radColorsHuman);
        grpRadiosHuman.add(radThemesHuman);
        grpRadiosHuman.add(radCustomHuman);
        grpRadiosHuman.add(radQuestsHuman);

        // Radio button group: AI
        final ButtonGroup grpRadiosAI = new ButtonGroup();
        grpRadiosAI.add(radColorsAI);
        grpRadiosAI.add(radThemesAI);
        grpRadiosAI.add(radCustomAI);
        grpRadiosAI.add(radQuestsAI);
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
        // TODO: This will be done each time the panel is shown.
        // Most of the prep work can be done once in the constructor,
        // should speed things up a little bit.  This should be
        // checked in all populate() methods; it probably happens other places.
        // Not complicated; easy to do if have time. Doublestrike 21-9-12

        // Deck panels: Human and AI
        final JPanel pnlDecksHuman = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
        final JPanel pnlDecksAI = new JPanel(new MigLayout("insets 0, gap 0, wrap"));

        // Add deck scrollers and random buttons: Human and AI
        pnlDecksHuman.setOpaque(false);
        pnlDecksHuman.add(scrHumanDecks, "w 100%!, pushy, growy");
        pnlDecksHuman.add(btnHumanRandom, "w 100%!, h 30px!, gap 0 0 10px 10px");

        pnlDecksAI.setOpaque(false);
        pnlDecksAI.add(scrAIDecks, "w 100%!, pushy, growy");
        pnlDecksAI.add(btnAIRandom, "w 100%!, h 30px!, gap 0 0 10px 0");

        // Radio button panels: Human and AI
        final JPanel pnlRadiosHuman = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
        final JPanel pnlRadiosAI = new JPanel(new MigLayout("insets 0, gap 0, wrap"));
        final String strRadioConstraints = "w 100%!, h 30px!";

        // Add radio buttons: Human
        pnlRadiosHuman.setOpaque(false);
        pnlRadiosHuman.add(new FLabel.Builder().text("Select your deck:")
                .fontStyle(Font.BOLD).fontSize(16)
                .fontAlign(SwingConstants.LEFT).build(), strRadioConstraints);
        pnlRadiosHuman.add(radColorsHuman, strRadioConstraints);
        pnlRadiosHuman.add(radThemesHuman, strRadioConstraints);
        pnlRadiosHuman.add(radCustomHuman, strRadioConstraints);
        pnlRadiosHuman.add(radQuestsHuman, strRadioConstraints);

        // Add radio buttons: AI
        pnlRadiosAI.setOpaque(false);
        pnlRadiosAI.add(new FLabel.Builder().text("Select an AI deck:")
                .fontStyle(Font.BOLD).fontSize(16)
                .fontAlign(SwingConstants.LEFT).build(), strRadioConstraints);
        pnlRadiosAI.add(radColorsAI, strRadioConstraints);
        pnlRadiosAI.add(radThemesAI, strRadioConstraints);
        pnlRadiosAI.add(radCustomAI, strRadioConstraints);
        pnlRadiosAI.add(radQuestsAI, strRadioConstraints);

        final JPanel pnlStart = new JPanel(new MigLayout("insets 0, gap 0, wrap 2"));
        final String strCheckboxConstraints = "w 200px!, h 30px!, gap 0 20px 0 0";
        pnlStart.setOpaque(false);
        pnlStart.add(cbSingletons, strCheckboxConstraints);
        pnlStart.add(btnStart, "span 1 3, growx, pushx, align center");
        pnlStart.add(cbArtifacts, strCheckboxConstraints);
        pnlStart.add(cbRemoveSmall, strCheckboxConstraints);

        pnl.removeAll();
        pnl.setOpaque(false);
        pnl.setLayout(new MigLayout("insets 0, gap 0, wrap 2, align center"));

        final String strLeftConstraints = "w 200px, pushy, growy, gap 0 20px 25px 25px";
        final String strRightConstraints = "w 30%!, pushy, growy, gap 0 20px 25px 25px";

        pnl.add(pnlRadiosAI, strLeftConstraints);
        pnl.add(pnlDecksAI, strRightConstraints);
        pnl.add(pnlRadiosHuman, strLeftConstraints);
        pnl.add(pnlDecksHuman, strRightConstraints);

        pnl.add(pnlStart, "w 220px + 30%, span 2 1, gap 0 0 0 50px");

        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0"));
        parentCell.getBody().add(pnl, "w 98%!, h 98%!, gap 1% 0 1% 0");
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstHumanDecks() {
        return this.lstHumanDecks;
    }

    /** @return {@link javax.swing.JList} */
    public JList getLstAIDecks() {
        return this.lstAIDecks;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnStart() {
        return this.btnStart;
    }

    /** @return {@link forge.gui.toolbox.FLabel} */
    public FLabel getBtnHumanRandom() {
        return this.btnHumanRandom;
    }

    /** @return {@link forge.gui.toolbox.FLabel} */
    public FLabel getBtnAIRandom() {
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
