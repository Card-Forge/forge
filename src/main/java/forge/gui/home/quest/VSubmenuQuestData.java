package forge.gui.home.quest;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
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
import forge.deck.CardCollections;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameFormat;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.home.EMenuGroup;
import forge.gui.home.IVSubmenu;
import forge.gui.home.VHomeUI;
import forge.gui.toolbox.FCheckBox;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FRadioButton;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.item.PreconDeck;
import forge.quest.QuestController;
import forge.quest.StartingPoolType;
import forge.util.IStorage;
import forge.util.IStorageView;

/**
 * Assembles Swing components of quest data submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuQuestData implements IVSubmenu<CSubmenuQuestData> {
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

    /* Fist column */ 
    private final JRadioButton radEasy = new FRadioButton("Easy");
    private final JRadioButton radMedium = new FRadioButton("Medium");
    private final JRadioButton radHard = new FRadioButton("Hard");
    private final JRadioButton radExpert = new FRadioButton("Expert");
    private final JCheckBox boxFantasy = new FCheckBox("Fantasy Mode");
    
    /* Second column */
    
    private final JLabel lblStartingPool = new FLabel.Builder().text("Starting pool:").build();
    private final JComboBox cbxStartingPool = new JComboBox();

    private final JLabel lblUnrestricted = new FLabel.Builder().text("All cards will be avaliable to play.").build();
    
    private final JLabel lblPreconDeck = new FLabel.Builder().text("Starter/Event deck:").build();
    private final JComboBox cbxPreconDeck = new JComboBox();

    private final JLabel lblFormat = new FLabel.Builder().text("Sanctioned format:").build();
    private final JComboBox cbxFormat = new JComboBox();

    private final JLabel lblCustomDeck = new FLabel.Builder().text("Custom deck:").build();
    private final JComboBox cbxCustomDeck = new JComboBox();
    
    private final FLabel btnDefineCustomFormat = new FLabel.Builder().opaque(true).hoverable(true).text("Define custom format").build();

    private final JLabel lblPrizedCards = new FLabel.Builder().text("Prized cards:").build();
    private final JComboBox cbxPrizedCards = new JComboBox();
    
    private final JCheckBox cboAllowUnlocks = new FCheckBox("Allow sets unlock");
    
    private final FLabel btnEmbark = new FLabel.Builder().opaque(true)
            .fontSize(16).hoverable(true).text("Embark!").build();

    /* Listeners */ 
    private final ActionListener alStartingPool = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            StartingPoolType newVal = getStartingPoolType();
            lblUnrestricted.setVisible(newVal == StartingPoolType.Complete);
            
            lblPreconDeck.setVisible(newVal == StartingPoolType.Precon);
            cbxPreconDeck.setVisible(newVal == StartingPoolType.Precon);

            lblFormat.setVisible(newVal == StartingPoolType.Rotating);
            cbxFormat.setVisible(newVal == StartingPoolType.Rotating);
            
            btnDefineCustomFormat.setVisible(newVal == StartingPoolType.CustomFormat);
            
            lblCustomDeck.setVisible(newVal == StartingPoolType.SealedDeck || newVal == StartingPoolType.DraftDeck);
            cbxCustomDeck.setVisible(newVal == StartingPoolType.SealedDeck || newVal == StartingPoolType.DraftDeck);
            
            if (newVal == StartingPoolType.SealedDeck || newVal == StartingPoolType.DraftDeck) {
                cbxCustomDeck.removeAllItems();
                CardCollections decks = Singletons.getModel().getDecks();
                IStorage<DeckGroup> storage = newVal == StartingPoolType.SealedDeck ? decks.getSealed() : decks.getDraft();  
                if( newVal == StartingPoolType.SealedDeck ) { 
                    for(DeckGroup d : storage ) {
                        cbxCustomDeck.addItem(d.getHumanDeck());
                    }
                }
            }
        }
    }; 
    
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

        
        cbxStartingPool.addItem(StartingPoolType.Complete);
        cbxStartingPool.addItem(StartingPoolType.Rotating);
        cbxStartingPool.addItem(StartingPoolType.CustomFormat);
        cbxStartingPool.addItem(StartingPoolType.Precon);
        cbxStartingPool.addItem(StartingPoolType.DraftDeck);
        cbxStartingPool.addItem(StartingPoolType.SealedDeck);
        cbxStartingPool.addActionListener(alStartingPool);
        
        // initial adjustment
        alStartingPool.actionPerformed(null);
        
        cbxPrizedCards.addItem("Same format as staring pool");
        cbxPrizedCards.addItem("More options to come (WIP)");
        
        for (GameFormat gf : Singletons.getModel().getFormats()) {
            cbxFormat.addItem(gf);
        }

        final Map<String, String> preconDescriptions = new HashMap<String, String>();
        IStorageView<PreconDeck> preconDecks = QuestController.getPrecons();

        for (PreconDeck preconDeck : preconDecks) {
            if (preconDeck.getRecommendedDeals().getMinWins() > 0) {
                continue;
            }
            String name = preconDeck.getName();
            cbxPreconDeck.addItem(name);
            String description = preconDeck.getDescription();
            description = "<html>" + WordUtils.wrap(description, 40, "<br>", false) + "</html>";
            preconDescriptions.put(name, description);
        }

        cbxPreconDeck.setRenderer(new BasicComboBoxRenderer() {
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

        // Fantasy box enabled by Default
        boxFantasy.setSelected(true);
        boxFantasy.setEnabled(true);

        pnlOptions.setOpaque(false);
        pnlOptions.setLayout(new MigLayout("insets 0, gap 10px, fillx, wrap 2"));
        
        JPanel pnlDifficultyMode = new JPanel();
        pnlDifficultyMode.setLayout(new MigLayout("insets 0, gap 1%, flowy"));
        final String n_constraints = "h 27px!";
        pnlDifficultyMode.add(radEasy, n_constraints + ", gap 0 4% 0 5px");
        pnlDifficultyMode.add(radMedium, n_constraints + ", gap 0 4% 0 5px");
        pnlDifficultyMode.add(radHard, n_constraints + ", gap 0 4% 0 5px");
        pnlDifficultyMode.add(radExpert, n_constraints + ", gap 0 4% 0 5px");
        pnlDifficultyMode.add(boxFantasy, n_constraints + ", gap 0 4% 0 5px");
        pnlDifficultyMode.setOpaque(false);
        pnlOptions.add(pnlDifficultyMode, "w 40%");
        

        JPanel pnlRestrictions = new JPanel();
        final String constraints = "h 27px!, ";
        final String lblWidth = "w 40%, ";
        final String hidemode = "hidemode 3, ";
        final String lblWidthStart = lblWidth + hidemode;
        final String cboWidth = "pushx, ";
        final String cboWidthStart = cboWidth + hidemode;

        pnlRestrictions.setLayout(new MigLayout("insets 0, gap 0, wrap 2", "[120, al right][240, fill]", "[|]12[|]"));
        pnlRestrictions.add(lblStartingPool, constraints + lblWidthStart);
        pnlRestrictions.add(cbxStartingPool, constraints + cboWidthStart);
        
        /* out of these 3 groups only one will be visible */
        pnlRestrictions.add(lblUnrestricted, constraints + hidemode + "spanx 2" );
        
        pnlRestrictions.add(lblPreconDeck, constraints + lblWidthStart);
        pnlRestrictions.add(cbxPreconDeck, constraints + cboWidthStart);

        pnlRestrictions.add(lblCustomDeck, constraints + lblWidthStart);
        pnlRestrictions.add(cbxCustomDeck, constraints + cboWidthStart); // , skip 1

        pnlRestrictions.add(lblFormat, constraints + lblWidthStart);
        pnlRestrictions.add(cbxFormat, constraints + cboWidthStart); // , skip 1

        pnlRestrictions.add(btnDefineCustomFormat, constraints + hidemode + "spanx 2, w 240px");
        
        // Prized cards options
        pnlRestrictions.add(lblPrizedCards, constraints + lblWidth);
        pnlRestrictions.add(cbxPrizedCards, constraints + cboWidth);
        
        pnlRestrictions.add(cboAllowUnlocks, constraints + "spanx 2, ax center");
        cboAllowUnlocks.setOpaque(false);
        pnlRestrictions.setOpaque(false);
        pnlOptions.add(pnlRestrictions, "pushx, ay top");
        
        pnlOptions.add(btnEmbark, "w 300px!, h 30px!, ax center, span 2, gap 0 0 15px 30px");
    }

    /* (non-Javadoc)
     * @see forge.view.home.IViewSubmenu#populate()
     */
    @Override
    public void populate() {
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().removeAll();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().setLayout(new MigLayout("insets 0, gap 0, wrap"));

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitle, "w 98%!, h 30px!, gap 1% 0 15px 15px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblOldQuests, "w 98%, h 30px!, gap 1% 0 0 5px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(scrQuests, "w 98%!, growy, pushy, gap 1% 0 0 20px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitleNew, "w 98%, h 30px!, gap 1% 0 0 10px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlOptions, "w 98%!, gap 1% 0 0 0");

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().repaintSelf();
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().revalidate();
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
    public CSubmenuQuestData getLayoutControl() {
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


    public int getSelectedDifficulty() {
        if (radEasy.isSelected()) {
            return 0;
        } else if (radMedium.isSelected()) {
            return 1;
        } else if (radHard.isSelected()) {
            return 2;
        } else if (radExpert.isSelected()) {
            return 3;
        } 
        return 0;
    }

    public String getSelectedPrecon() {
        return cbxPreconDeck.getSelectedItem().toString();
    }

    public Deck getSelectedDeck() {
        Object sel = cbxCustomDeck.getSelectedItem();
        return sel instanceof Deck ? (Deck) sel : null; 
    }

    public boolean isUnlockSetsAllowed() {
        return cboAllowUnlocks.isSelected();
    }

    public StartingPoolType getStartingPoolType() {
        return (StartingPoolType) cbxStartingPool.getSelectedItem();
    }

    public boolean isFantasy() {
        return boxFantasy.isSelected();
    }

    public GameFormat getRotatingFormat() {
        return (GameFormat) cbxFormat.getSelectedItem();
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public FLabel getBtnCustomFormat() {
        // TODO Auto-generated method stub
        return btnDefineCustomFormat;
    }
}
