package forge.gui.home.quest;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.text.WordUtils;

import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.ICDoc;
import forge.gui.framework.IVDoc;
import forge.gui.home.EMenuGroup;
import forge.gui.home.EMenuItem;
import forge.gui.home.ICSubmenu;
import forge.gui.home.IVSubmenu;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FPanel;
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
public enum VSubmenuQuestData implements IVSubmenu, IVDoc {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Quest Data");

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

    private final JRadioButton radCompleteStart = new FRadioButton("Unrestricted Starting Pool");
    private final JRadioButton radStandardStart = new FRadioButton("Standard (Type 2) Starting Pool");
    private final JRadioButton radPreconStart = new FRadioButton("Preconstructed Deck: ");
    private final JComboBox cbxPrecon = new JComboBox();

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
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");

        final FScrollPane scr = new FScrollPane(lstQuests);
        scr.setBorder(null);
        scr.getViewport().setBorder(null);

        // New quest
        final FPanel pnlTitleNew = new FPanel();
        pnlTitleNew.setLayout(new MigLayout("insets 0, align center"));
        pnlTitleNew.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        pnlTitleNew.add(new FLabel.Builder().text("Start a new Quest")
                .fontSize(16).build(), "h 95%!, gap 0 0 2.5% 0");

        final ButtonGroup group1 = new ButtonGroup();
        group1.add(radEasy);
        group1.add(radMedium);
        group1.add(radHard);
        group1.add(radExpert);
        radEasy.setSelected(true);

        final ButtonGroup group2 = new ButtonGroup();
        group2.add(radFantasy);
        group2.add(radClassic);
        radClassic.setSelected(true);

        final ActionListener preconListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                cbxPrecon.setEnabled(radPreconStart.isSelected());
            }
        };

        final Map<String, String> preconDescriptions = new HashMap<String, String>();
        IStorageView<PreconDeck> preconDecks = QuestController.getPrecons();
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
        group3.add(radCompleteStart);
        radCompleteStart.addActionListener(preconListener);
        group3.add(radStandardStart);
        radStandardStart.addActionListener(preconListener);
        group3.add(radPreconStart);
        radPreconStart.addActionListener(preconListener);
        radCompleteStart.setSelected(true);
        cbxPrecon.setEnabled(false);
        final JPanel pnlOptions = new JPanel();
        pnlOptions.setOpaque(false);
        pnlOptions.setLayout(new MigLayout("insets 0, gap 0"));

        final String constraints = "w 40%!, h 30px!";
        pnlOptions.add(radEasy, constraints + ", gap 7.5% 2.5% 0 0");
        pnlOptions.add(radFantasy, constraints + ", wrap");
        pnlOptions.add(radMedium, constraints + ", gap 7.5% 2.5% 0 0");
        pnlOptions.add(radClassic, constraints + ", wrap");
        pnlOptions.add(radHard, constraints + ", gap 7.5% 2.5% 0 0");
        pnlOptions.add(radCompleteStart, constraints + ", wrap");
        pnlOptions.add(radExpert, constraints + ", gap 7.5% 2.5% 0 0 ");
        pnlOptions.add(radStandardStart, constraints + ", wrap");
        pnlOptions.add(radPreconStart, constraints + ", wrap, skip");
        pnlOptions.add(cbxPrecon, "gap 20 0, w 30%!, h 35px!, wrap, skip");

        pnlOptions.add(btnEmbark, "w 40%!, h 30px!, gap 30% 0 20px 0, span 3 1");

        // Final layout
        pnlViewport.removeAll();
        pnlViewport.setOpaque(false);
        pnlViewport.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        pnlViewport.add(pnlTitleLoad, "w 96%, h 36px!, gap 2% 0 20px 10px");

        pnlViewport.add(new FLabel.Builder().text("Old quest data? Put into "
                + "res/quest/data, and restart Forge.")
                .fontAlign(SwingConstants.CENTER).fontSize(12)
                .build(), "w 96%!, h 18px!, gap 2% 0 0 4px");

        pnlViewport.add(scr, "w 96%!, pushy, growy, gap 2% 0 0 30px");

        pnlViewport.add(pnlTitleNew, "w 96%, h 36px!, gap 2% 0 0 10px");
        pnlViewport.add(pnlOptions, "w 96%!, h 250px!, gap 2% 0 0 20px");

        pnl.add(new FScrollPane(pnlViewport), "w 100%!, h 100%!");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getGroup()
     */
    @Override
    public EMenuGroup getGroupEnum() {
        return EMenuGroup.QUEST;
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#getPanel()
     */
    @Override
    public JPanel getPanel() {
        return pnl;
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
    public String getItemEnum() {
        return EMenuItem.QUEST_DATA.toString();
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getSubmenuControl()
     */
    @Override
    public ICSubmenu getSubmenuControl() {
        return CSubmenuQuestData.SINGLETON_INSTANCE;
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
     * @return {@link javax.swing.JRadioButton}
     */
    public JRadioButton getRadFantasy() {
        return radFantasy;
    }

    /**
     * @return {@link javax.swing.JRadioButton}
     */
    public JRadioButton getRadClassic() {
        return radClassic;
    }

    public JRadioButton getRadCompleteStart() {
        return radCompleteStart;
    }

    /**
     * @return {@link javax.swing.JCheckBox}
     */
    public JRadioButton getRadStandardStart() {
        return radStandardStart;
    }

    public JRadioButton getRadPreconStart() {
        return radPreconStart;
    }

    public String getPrecon() {
        return (String) cbxPrecon.getSelectedItem();
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
