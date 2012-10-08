package forge.gui.home.quest;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.text.WordUtils;

import forge.Singletons;
import forge.game.GameFormat;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.item.PreconDeck;
import forge.quest.QuestController;
import forge.util.IStorageView;

/**
 * Assembles Swing components of quest data submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuQuestData implements IVSubmenu {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Quest Data");

    /** */
    private final FLabel lblTitle = new FLabel.Builder()
        .text("Load Quest Data").fontAlign(SwingConstants.CENTER)
        .opaque(true).fontSize(16).build();

    private final JLabel lblTitleNew = new FLabel.Builder().text("Start a new Quest")
            .opaque(true).fontSize(16).build();

    private final JLabel lblOldQuests = new FLabel.Builder().text("Old quest data? Put into "
            + "res/quest/data, and restart Forge.")
            .fontAlign(SwingConstants.CENTER).fontSize(12).build();

    private final QuestFileLister lstQuests = new QuestFileLister();
    private final FScrollPane scrQuests = new FScrollPane(lstQuests);
    private final JPanel pnlOptions = new JPanel();

    private final JRadioButton radEasy = new FRadioButton("Easy");
    private final JRadioButton radMedium = new FRadioButton("Medium");
    private final JRadioButton radHard = new FRadioButton("Hard");
    private final JRadioButton radExpert = new FRadioButton("Expert");

    private final JCheckBox boxFantasy = new FCheckBox("Fantasy Mode");
    private final JCheckBox boxFormatPersist = new FCheckBox("Enforce format during quest");

    private final JRadioButton radUnrestricted = new FRadioButton("Unrestricted Starting Pool");
    private final JRadioButton radRotatingStart = new FRadioButton("Format: ");
    private final JComboBox cbxFormat = new JComboBox();

    private final JRadioButton radPreconStart = new FRadioButton("Preconstructed Deck: ");
    private final JComboBox cbxPrecon = new JComboBox();

    private final FLabel btnEmbark = new FLabel.Builder().opaque(true)
            .fontSize(16).hoverable(true).text("Embark!").build();

    /**
     * Constructor.
     */
    private VSubmenuQuestData() {
        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        lblTitleNew.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        scrQuests.setBorder(null);

        final ButtonGroup group1 = new ButtonGroup();
        group1.add(radEasy);
        group1.add(radMedium);
        group1.add(radHard);
        group1.add(radExpert);
        radEasy.setSelected(true);

        cbxFormat.removeAllItems();

        for (GameFormat gf : Singletons.getModel().getFormats()) {
            cbxFormat.addItem(gf.getName());
        }

        final Map<String, String> preconDescriptions = new HashMap<String, String>();
        IStorageView<PreconDeck> preconDecks = QuestController.getPrecons();

        cbxPrecon.removeAllItems();

        for (PreconDeck preconDeck : preconDecks) {
            if (preconDeck.getRecommendedDeals().getMinWins() > 0) {
                continue;
            }
            String name = preconDeck.getName();
            cbxPrecon.addItem(name);
            String description = preconDeck.getDescription();
            description = "<html>" + WordUtils.wrap(description, 40, "<br>", false) + "</html>";
            preconDescriptions.put(name, description);
        }

        cbxPrecon.setRenderer(new BasicComboBoxRenderer() {
            private static final long serialVersionUID = 3477357932538947199L;

            @Override
            public Component getListCellRendererComponent(
                    JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component defaultComponent =
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (-1 < index && null != value) {
                    String val = (String) value;
                    list.setToolTipText(preconDescriptions.get(val));
                }
                return defaultComponent;
            }
        });

        final ButtonGroup group3 = new ButtonGroup();
        group3.add(radUnrestricted);
        group3.add(radRotatingStart);
        group3.add(radPreconStart);

        cbxFormat.setEnabled(false);
        boxFormatPersist.setSelected(true);
        boxFormatPersist.setEnabled(false);
        radUnrestricted.setSelected(true);
        cbxPrecon.setEnabled(false);
        radMedium.setEnabled(true);

        // Fantasy box enabled by Default
        boxFantasy.setSelected(true);
        boxFantasy.setEnabled(true);

        pnlOptions.setOpaque(false);
        pnlOptions.setLayout(new MigLayout("insets 0, gap 0, wrap 2"));

        final String constraints = "w 47%!, h 27px!";
        pnlOptions.add(radEasy, constraints + ", gap 1% 4% 0 5px");
        pnlOptions.add(radUnrestricted, constraints);

        pnlOptions.add(radMedium, constraints + ", gap 1% 4% 0 5px");
        pnlOptions.add(radRotatingStart, constraints);

        pnlOptions.add(radHard, constraints + ", gap 1% 4% 0 5px");
        pnlOptions.add(cbxFormat, constraints);

        pnlOptions.add(radExpert, constraints + ", gap 1% 4% 0 5px");
        pnlOptions.add(boxFormatPersist, constraints);

        pnlOptions.add(boxFantasy, constraints + ", gap 1% 4% 0 5px");
        pnlOptions.add(radPreconStart, constraints);

        pnlOptions.add(cbxPrecon, constraints + ", skip 1");
        pnlOptions.add(btnEmbark, "w 300px!, h 30px!, ax center, span 2, gap 0 0 15px 30px");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0, wrap"));
        parentCell.getBody().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
        parentCell.getBody().add(lblOldQuests, "w 98%, h 30px!, gap 1% 0 0 5px");
        parentCell.getBody().add(scrQuests, "w 98%!, growy, pushy, gap 1% 0 0 20px");
        parentCell.getBody().add(lblTitleNew, "w 98%, h 30px!, gap 1% 0 0 10px");
        parentCell.getBody().add(pnlOptions, "w 98%!, gap 1% 0 0 0");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.QUEST;
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuTitle()
     */
    @Override
    public String getMenuTitle() {
        return "New / Load Quest";
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public EDocID getItemEnum() {
        return EDocID.HOME_QUESTDATA;
    }

    /**
     * @return {@link forge.gui.home.quest.QuestFileLister}
     */
    public QuestFileLister getLstQuests() {
        return this.lstQuests;
    }

    /**
     * @return {@link javax.swing.JRadioButton}
     */
    public JRadioButton getRadEasy() {
        return radEasy;
    }

    /**
     * @return {@link javax.swing.JRadioButton}
     */
    public JRadioButton getRadMedium() {
        return radMedium;
    }

    /**
     * @return {@link javax.swing.JRadioButton}
     */
    public JRadioButton getRadHard() {
        return radHard;
    }

    /**
     * @return {@link javax.swing.JRadioButton}
     */
    public JRadioButton getRadExpert() {
        return radExpert;
    }

    /**
     * @return {@link javax.swing.JCheckBox}
     */
    public JCheckBox getBoxFantasy() {
        return boxFantasy;
    }

    /**
     * @return {@link javax.swing.JCheckBox}
     */
    public JCheckBox getBoxPersist() {
        return boxFormatPersist;
    }

    /**
     * @return {@link javax.swing.JRadioButton}
     */
    public JRadioButton getRadUnrestricted() {
        return radUnrestricted;
    }

    /** @return {@link javax.swing.JCheckBox} */
    public JRadioButton getRadRotatingStart() {
        return radRotatingStart;
    }

    /** @return {@link javax.swing.JRadioButton} */
    public JRadioButton getRadPreconStart() {
        return radPreconStart;
    }

    /** @return {@link javax.swing.JComboBox} */
    public JComboBox getCbxFormat() {
        return this.cbxFormat;
    }

    /** @return {@link javax.swing.JComboBox} */
    public JComboBox getCbxPrecon() {
        return this.cbxPrecon;
    }

    /** @return {@link java.lang.String} */
    public String getPrecon() {
        return (String) cbxPrecon.getSelectedItem();
    }

    /** @return {@link java.lang.String} */
    public String getFormat() {
        return (String) cbxFormat.getSelectedItem();
    }

    /**
     * @return {@link forge.gui.toolbox.FLabel}
     */
    public FLabel getBtnEmbark() {
        return btnEmbark;
    }

    //========== Overridden from IVDoc

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.HOME_QUESTDATA;
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
        return CSubmenuQuestData.SINGLETON_INSTANCE;
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
