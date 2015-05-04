package forge.screens.home.quest;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameFormat;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.item.PreconDeck;
import forge.model.CardCollections;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.quest.QuestController;
import forge.quest.QuestWorld;
import forge.quest.StartingPoolType;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.*;
import forge.util.storage.IStorage;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.text.WordUtils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Assembles Swing components of quest data submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuQuestData implements IVSubmenu<CSubmenuQuestData> {
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Quest Data");

    private final FLabel lblTitle = new FLabel.Builder()
    .text("Load Quest Data").fontAlign(SwingConstants.CENTER)
    .opaque(true).fontSize(16).build();

    private final FLabel lblTitleNew = new FLabel.Builder().text("Start a new Quest")
            .opaque(true).fontSize(16).build();

    private final FLabel lblOldQuests = new FLabel.Builder().text("Old quest data? Put into \""
            + ForgeConstants.QUEST_SAVE_DIR.replace('\\', '/') + "\" and restart Forge.")
            .fontAlign(SwingConstants.CENTER).fontSize(12).build();

    private final QuestFileLister lstQuests = new QuestFileLister();
    private final FScrollPane scrQuests = new FScrollPane(lstQuests, false);
    private final JPanel pnlOptions = new JPanel();

    /* First column */
    private final FRadioButton radEasy = new FRadioButton("Easy");
    private final FRadioButton radMedium = new FRadioButton("Medium");
    private final FRadioButton radHard = new FRadioButton("Hard");
    private final FRadioButton radExpert = new FRadioButton("Expert");
    private final FCheckBox boxFantasy = new FCheckBox("Fantasy Mode");

    private final FLabel lblStartingWorld = new FLabel.Builder().text("Starting world:").build();
    private final FComboBoxWrapper<QuestWorld> cbxStartingWorld = new FComboBoxWrapper<>();

    /* Second column */
    private final FLabel lblStartingPool = new FLabel.Builder().text("Starting pool:").build();
    private final FComboBoxWrapper<StartingPoolType> cbxStartingPool = new FComboBoxWrapper<>();

    private final FLabel lblUnrestricted = new FLabel.Builder().text("All cards will be available to play.").build();

    private final FLabel lblPreconDeck = new FLabel.Builder().text("Starter/Event deck:").build();
    private final FComboBoxWrapper<String> cbxPreconDeck = new FComboBoxWrapper<>();

    private final FLabel lblFormat = new FLabel.Builder().text("Sanctioned format:").build();
    private final FComboBoxWrapper<GameFormat> cbxFormat = new FComboBoxWrapper<>();

    private final FLabel lblCustomDeck = new FLabel.Builder().text("Custom deck:").build();
    private final FComboBoxWrapper<Deck> cbxCustomDeck = new FComboBoxWrapper<>();

    private final FLabel btnDefineCustomFormat = new FLabel.Builder().opaque(true).hoverable(true).text("Define custom format").build();

    private final FCheckBox boxCompleteSet = new FCheckBox("Start with all cards in selected sets");
    private final FCheckBox boxAllowDuplicates = new FCheckBox("Allow duplicate cards");

    private final FLabel lblPreferredColor = new FLabel.Builder().text("Starting pool colors:").build();
    private final FLabel btnPreferredColors = new FLabel.Builder().opaque(true).hoverable(true).text("Choose Colors").build();

    private final FLabel btnPrizeDefineCustomFormat = new FLabel.Builder().opaque(true).hoverable(true).text("Define custom format").build();

    private final FLabel lblPrizedCards = new FLabel.Builder().text("Prized cards:").build();
    private final FComboBoxWrapper<Object> cbxPrizedCards = new FComboBoxWrapper<>();

    private final FLabel lblPrizeFormat = new FLabel.Builder().text("Sanctioned format:").build();
    private final FComboBoxWrapper<GameFormat> cbxPrizeFormat = new FComboBoxWrapper<>();

    private final FLabel lblPrizeUnrestricted = new FLabel.Builder().text("All cards will be available to win.").build();
    private final FLabel lblPrizeSameAsStarting = new FLabel.Builder().text("Only sets in starting pool will be available.").build();

    private final FCheckBox cboAllowUnlocks = new FCheckBox("Allow unlock of additional editions");

    private final FLabel btnEmbark = new FLabel.Builder().opaque(true)
            .fontSize(16).hoverable(true).text("Embark!").build();

    /* Listeners */
    private final ActionListener alStartingPool = new ActionListener() {
        @SuppressWarnings("incomplete-switch")
        @Override
        public void actionPerformed(final ActionEvent e) {
            final StartingPoolType newVal = getStartingPoolType();
            lblUnrestricted.setVisible(newVal == StartingPoolType.Complete);

            lblPreconDeck.setVisible(newVal == StartingPoolType.Precon);
            cbxPreconDeck.setVisible(newVal == StartingPoolType.Precon);

            lblFormat.setVisible(newVal == StartingPoolType.Rotating);
            cbxFormat.setVisible(newVal == StartingPoolType.Rotating);

            btnDefineCustomFormat.setVisible(newVal == StartingPoolType.CustomFormat);


            final boolean usesDeckList = newVal == StartingPoolType.SealedDeck || newVal == StartingPoolType.DraftDeck || newVal == StartingPoolType.Cube;
            lblCustomDeck.setVisible(usesDeckList);
            cbxCustomDeck.setVisible(usesDeckList);

            if (usesDeckList) {
                cbxCustomDeck.removeAllItems();
                final CardCollections decks = FModel.getDecks();
                switch (newVal) {
                    case SealedDeck:
                        for (final DeckGroup d : decks.getSealed()) { cbxCustomDeck.addItem(d.getHumanDeck()); }
                        break;
                    case DraftDeck:
                        for (final DeckGroup d : decks.getDraft()) { cbxCustomDeck.addItem(d.getHumanDeck()); }
                        break;
                    case Cube:
                        for (final Deck d : decks.getCubes()) { cbxCustomDeck.addItem(d); }
                        break;
                }
            }
        }
    };

    /* Listeners */
    private final ActionListener alPrizesPool = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            final StartingPoolType newVal = getPrizedPoolType();
            lblPrizeUnrestricted.setVisible(newVal == StartingPoolType.Complete);
            cboAllowUnlocks.setVisible(newVal != StartingPoolType.Complete);

            lblPrizeFormat.setVisible(newVal == StartingPoolType.Rotating);
            cbxPrizeFormat.setVisible(newVal == StartingPoolType.Rotating);
            btnPrizeDefineCustomFormat.setVisible(newVal == StartingPoolType.CustomFormat);
            lblPrizeSameAsStarting.setVisible(newVal == null);
        }
    };

    /* Listeners */
    private final ActionListener alStartingWorld = new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent e) {
            updateEnabledFormats();
        }
    };

    /**
     * Aux function for enabling or disabling the format selection according to world selection.
     */
    private void updateEnabledFormats() {
        final QuestWorld qw = FModel.getWorlds().get(getStartingWorldName());
        if (qw != null) {
            cbxStartingPool.setEnabled(qw.getFormat() == null);
            cbxFormat.setEnabled(qw.getFormat() == null);
            cbxCustomDeck.setEnabled(qw.getFormat() == null);
        }
    }

    /**
     * Constructor.
     */
    @SuppressWarnings("unchecked")
    VSubmenuQuestData() {

        lblTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        lblTitleNew.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        final JXButtonPanel difficultyPanel = new JXButtonPanel();
        final String difficulty_constraints = "h 25px!, gapbottom 5";
        difficultyPanel.add(radEasy, difficulty_constraints);
        difficultyPanel.add(radMedium, difficulty_constraints);
        difficultyPanel.add(radHard, difficulty_constraints);
        difficultyPanel.add(radExpert, difficulty_constraints);
        radEasy.setSelected(true);

        boxCompleteSet.setToolTipText("You will start the quest with 4 of each card in the sets you have selected.");
        boxAllowDuplicates.setToolTipText("When your starting pool is generated, duplicates of cards may be included.");

        cbxStartingPool.addItem(StartingPoolType.Complete);
        cbxStartingPool.addItem(StartingPoolType.Rotating);
        cbxStartingPool.addItem(StartingPoolType.CustomFormat);
        cbxStartingPool.addItem(StartingPoolType.Precon);
        cbxStartingPool.addItem(StartingPoolType.DraftDeck);
        cbxStartingPool.addItem(StartingPoolType.SealedDeck);
        cbxStartingPool.addItem(StartingPoolType.Cube);
        cbxStartingPool.addActionListener(alStartingPool);

        // initial adjustment
        alStartingPool.actionPerformed(null);
        alPrizesPool.actionPerformed(null);

        cbxPrizedCards.addItem("Same as starting pool");
        cbxPrizedCards.addItem(StartingPoolType.Complete);
        cbxPrizedCards.addItem(StartingPoolType.Rotating);
        cbxPrizedCards.addItem(StartingPoolType.CustomFormat);
        cbxPrizedCards.addActionListener(alPrizesPool);

        for (final GameFormat gf : FModel.getFormats().getOrderedList()) {
            cbxFormat.addItem(gf);
            cbxPrizeFormat.addItem(gf);
        }

        for (final QuestWorld qw : FModel.getWorlds()) {
            cbxStartingWorld.addItem(qw);
        }
        // Default to 'Main world'
        cbxStartingWorld.setSelectedItem(FModel.getWorlds().get("Main world"));

        cbxStartingWorld.addActionListener(alStartingWorld);
        updateEnabledFormats();

        cboAllowUnlocks.setSelected(true);

        final Map<String, String> preconDescriptions = new HashMap<>();
        final IStorage<PreconDeck> preconDecks = QuestController.getPrecons();

        for (final PreconDeck preconDeck : preconDecks) {
            if (QuestController.getPreconDeals(preconDeck).getMinWins() > 0) {
                continue;
            }
            final String name = preconDeck.getName();
            cbxPreconDeck.addItem(name);
            String description = preconDeck.getDescription();
            description = "<html>" + WordUtils.wrap(description, 40, "<br>", false) + "</html>";
            preconDescriptions.put(name, description);
        }

        // The cbx needs strictly typed renderer
        cbxPreconDeck.setRenderer(new BasicComboBoxRenderer() {
            private static final long serialVersionUID = 3477357932538947199L;

            @SuppressWarnings("rawtypes")
            @Override
            public Component getListCellRendererComponent(
                    final JList list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
                final Component defaultComponent =
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (-1 < index && null != value) {
                    final String val = (String) value;
                    list.setToolTipText(preconDescriptions.get(val));
                }
                return defaultComponent;
            }
        });

        // Fantasy box enabled by Default
        boxFantasy.setSelected(true);
        boxFantasy.setEnabled(true);
        boxCompleteSet.setEnabled(true);
        boxAllowDuplicates.setEnabled(true);

        pnlOptions.setOpaque(false);
        pnlOptions.setLayout(new MigLayout("insets 0, gap 10px, fillx, wrap 2"));

        final JPanel pnlDifficultyMode = new JPanel(new MigLayout("insets 0, gap 1%, flowy"));
        pnlDifficultyMode.add(difficultyPanel, "gapright 4%");
        pnlDifficultyMode.add(boxFantasy, "h 25px!, gapbottom 15, gapright 4%");
        pnlDifficultyMode.add(lblStartingWorld, "h 25px!, hidemode 3");
        cbxStartingWorld.addTo(pnlDifficultyMode, "h 27px!, w 40%, pushx, gapbottom 7");
        pnlDifficultyMode.setOpaque(false);
        pnlOptions.add(pnlDifficultyMode, "w 40%");


        final JPanel pnlRestrictions = new JPanel();
        final String constraints = "h 25px!, ";
        final String hidemode = "hidemode 3, ";
        final String cboWidth = "w 240px!, pushx, ";
        final String cboWidthStart = cboWidth + hidemode;
        final String btnStartingCustomFormatWidth = "w " + (4 + cbxStartingPool.getAutoSizeWidth()) + "px!, ";

        pnlRestrictions.setLayout(new MigLayout("insets 0, gap 10", "[right][left]"));


        pnlRestrictions.add(lblStartingPool, "h 15px!, cell 0 0");
        cbxStartingPool.addTo(pnlRestrictions, constraints + cboWidthStart + " cell 1 0");

        /* --vvv-- out of these 3 groups only one will be visible --vvv-- */
        pnlRestrictions.add(lblUnrestricted, constraints + hidemode + " cell 1 1, ");

        pnlRestrictions.add(lblPreconDeck, constraints + hidemode + " cell 0 1");
        cbxPreconDeck.addTo(pnlRestrictions, constraints + cboWidthStart + " cell 1 1");

        pnlRestrictions.add(lblCustomDeck, constraints + hidemode + " cell 0 1");
        cbxCustomDeck.addTo(pnlRestrictions, constraints + cboWidthStart + " cell 1 1");
        /* --^^^-- out of these 3 groups only one will be visible --^^^-- */

        pnlRestrictions.add(lblFormat, constraints + hidemode + " cell 0 1");
        cbxFormat.addTo(pnlRestrictions, constraints + cboWidthStart + " cell 1 1");

        pnlRestrictions.add(btnDefineCustomFormat, btnStartingCustomFormatWidth + constraints + hidemode + " cell 1 1");

        pnlRestrictions.add(boxAllowDuplicates, "h 15px!, cell 1 2");
        pnlRestrictions.add(boxCompleteSet, "h 15px!, cell 1 3");

        pnlRestrictions.add(lblPreferredColor, constraints + hidemode + "cell 0 4");
        pnlRestrictions.add(btnPreferredColors, btnStartingCustomFormatWidth + constraints + hidemode + "cell 1 4");

        // Prized cards options
        pnlRestrictions.add(lblPrizedCards, constraints + " cell 0 5");
        cbxPrizedCards.addTo(pnlRestrictions, constraints + cboWidth + " cell 1 5");

        pnlRestrictions.add(lblPrizeFormat, constraints + hidemode + "cell 0 6");
        cbxPrizeFormat.addTo(pnlRestrictions, constraints + cboWidthStart + "cell 1 6"); // , skip 1
        pnlRestrictions.add(btnPrizeDefineCustomFormat, btnStartingCustomFormatWidth + constraints + hidemode + "cell 1 6");
        pnlRestrictions.add(lblPrizeSameAsStarting, constraints + hidemode + "cell 1 6");
        pnlRestrictions.add(lblPrizeUnrestricted, constraints + hidemode + "cell 1 6");

        pnlRestrictions.add(cboAllowUnlocks, constraints + "cell 1 7");

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
     * @return {@link forge.screens.home.quest.QuestFileLister}
     */
    public QuestFileLister getLstQuests() {
        return this.lstQuests;
    }

    /**
     * @return {@link forge.toolbox.FLabel}
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
    public void setParentCell(final DragCell cell0) {
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
        return cbxPreconDeck.getSelectedItem();
    }

    public Deck getSelectedDeck() {
        final Object sel = cbxCustomDeck.getSelectedItem();
        return sel != null ? (Deck) sel : null;
    }

    public boolean isUnlockSetsAllowed() {
        return cboAllowUnlocks.isSelected();
    }

    public StartingPoolType getStartingPoolType() {
        return cbxStartingPool.getSelectedItem();
    }

    public StartingPoolType getPrizedPoolType() {
        final Object v = cbxPrizedCards.getSelectedItem();
        return v instanceof StartingPoolType ? (StartingPoolType) v : null;
    }

    public String getStartingWorldName() {
        return cbxStartingWorld.getSelectedItem().toString();
    }

    public boolean isFantasy() {
        return boxFantasy.isSelected();
    }

    public boolean startWithCompleteSet() {
        return boxCompleteSet.isSelected();
    }

    public boolean allowDuplicateCards() {
        return boxAllowDuplicates.isSelected();
    }

    public boolean randomizeColorDistribution() {
        return false;
        //return stringRandomizedDistribution.equals(cbxPreferredColor.getSelectedItem());
    }

    public GameFormat getRotatingFormat() {
        return cbxFormat.getSelectedItem();
    }

    public GameFormat getPrizedRotatingFormat() {
        return cbxPrizeFormat.getSelectedItem();
    }

    public FLabel getBtnCustomFormat() {
        return btnDefineCustomFormat;
    }
    public FLabel getBtnPrizeCustomFormat() {
        return btnPrizeDefineCustomFormat;
    }
    public FLabel getBtnPreferredColors() {
        return btnPreferredColors;
    }

}
