package forge.screens.home.quest;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameFormat;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestWorld;
import forge.gamemodes.quest.StartingPoolType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.item.PreconDeck;
import forge.localinstance.properties.ForgeConstants;
import forge.model.CardCollections;
import forge.model.FModel;
import forge.screens.home.EMenuGroup;
import forge.screens.home.IVSubmenu;
import forge.screens.home.VHomeUI;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBoxWrapper;
import forge.toolbox.FLabel;
import forge.toolbox.FRadioButton;
import forge.toolbox.FSkin;
import forge.toolbox.JXButtonPanel;
import forge.util.Localizer;
import forge.util.WordUtil;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of quest data submenu singleton.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VSubmenuQuestStart implements IVSubmenu<CSubmenuQuestStart> {
    SINGLETON_INSTANCE;
    final Localizer localizer = Localizer.getInstance();

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab(localizer.getMessage("lblStartanewQuest"));

    private final FLabel lblTitleNew = new FLabel.Builder().text(localizer.getMessage("lblStartanewQuest")).opaque(true).fontSize(16).build();

    String str= ForgeConstants.QUEST_SAVE_DIR.replace('\\', '/');
    private final JPanel pnlOptions = new JPanel();

    /* First column */
    private final FRadioButton radEasy = new FRadioButton(localizer.getMessage("questDifficultyEasy"));
    private final FRadioButton radMedium = new FRadioButton(localizer.getMessage("questDifficultyMedium"));
    private final FRadioButton radHard = new FRadioButton(localizer.getMessage("questDifficultyHard"));
    private final FRadioButton radExpert = new FRadioButton(localizer.getMessage("questDifficultyExpert"));
    private final FCheckBox boxFantasy = new FCheckBox(localizer.getMessage("rbFantasyMode"));
    private final FCheckBox boxCommander = new FCheckBox(localizer.getMessage("rbCommanderSubformat"));

    private final FLabel lblStartingWorld = new FLabel.Builder().text(localizer.getMessage("lblStartingWorld") + ":").build();
    private final FComboBoxWrapper<QuestWorld> cbxStartingWorld = new FComboBoxWrapper<>();
    /* Second column */
    private final FLabel lblStartingPool = new FLabel.Builder().text(localizer.getMessage("lblStartingPool") + ":").build();
    private final FComboBoxWrapper<StartingPoolType> cbxStartingPool = new FComboBoxWrapper<>();
    private final FLabel lblUnrestricted = new FLabel.Builder().text(localizer.getMessage("lblAllCardsAvailable")).build();

    private final FLabel lblPreconDeck = new FLabel.Builder().text(localizer.getMessage("lblStarterEventdeck") +":").build();
    private final FComboBoxWrapper<String> cbxPreconDeck = new FComboBoxWrapper<>();
    private final FLabel lblFormat = new FLabel.Builder().text(localizer.getMessage("lblSanctionedFormat") + ":").build();
    private final FComboBoxWrapper<GameFormat> cbxFormat = new FComboBoxWrapper<>();
    private final FLabel lblCustomDeck = new FLabel.Builder().text(localizer.getMessage("lblCustomdeck")).build();
    private final FComboBoxWrapper<Deck> cbxCustomDeck = new FComboBoxWrapper<>();
    private final FLabel btnDefineCustomFormat = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("lblDefineCustomFormat")).build();
    private final FLabel btnSelectFormat = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("lblSelectFormat")).build();


    private final FCheckBox boxCompleteSet = new FCheckBox(localizer.getMessage("lblStartWithAllCards"));
    private final FCheckBox boxAllowDuplicates = new FCheckBox(localizer.getMessage("lblAllowDuplicateCards"));

    private final FLabel lblPreferredColor = new FLabel.Builder().text(localizer.getMessage("lblStartingPoolDistribution")).build();
    private final FLabel btnPreferredColors = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("lblChooseDistribution")).build();

    private final FLabel btnPrizeDefineCustomFormat = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("lblDefineCustomFormat")).build();

    private final FLabel btnPrizeSelectFormat = new FLabel.Builder().opaque(true).hoverable(true).text(localizer.getMessage("lblSelectFormat")).build();

    private final FLabel lblPrizedCards = new FLabel.Builder().text(localizer.getMessage("lblPrizedCards")).build();
    private final FComboBoxWrapper<Object> cbxPrizedCards = new FComboBoxWrapper<>();
    private final FLabel lblPrizeFormat = new FLabel.Builder().text(localizer.getMessage("lblSanctionedFormat") + ":").build();
    private final FComboBoxWrapper<GameFormat> cbxPrizeFormat = new FComboBoxWrapper<>();
    private final FLabel lblPrizeUnrestricted = new FLabel.Builder().text(localizer.getMessage("lblAllCardsAvailableWin")).build();
    private final FLabel lblPrizeSameAsStarting = new FLabel.Builder().text(localizer.getMessage("lblOnlySetsInStarting")).build();

    private final FCheckBox cboAllowUnlocks = new FCheckBox(localizer.getMessage("lblAllowUnlockAdEd"));

    private final FLabel btnEmbark = new FLabel.Builder().opaque(true)
            .fontSize(16).hoverable(true).text(localizer.getMessage("lblEmbark")).build();

    /* Listeners */
    private final ActionListener alStartingPool = new ActionListener() {
        @SuppressWarnings("incomplete-switch")
        @Override
        public void actionPerformed(final ActionEvent e) {
            final StartingPoolType newVal = getStartingPoolType();
            lblUnrestricted.setVisible(newVal == StartingPoolType.Complete);

            lblPreconDeck.setVisible(newVal == StartingPoolType.Precon);
            cbxPreconDeck.setVisible(newVal == StartingPoolType.Precon);

            lblFormat.setVisible(newVal == StartingPoolType.Sanctioned);
            cbxFormat.setVisible(newVal == StartingPoolType.Sanctioned);

            btnDefineCustomFormat.setVisible(newVal == StartingPoolType.CustomFormat);
            btnSelectFormat.setVisible(newVal == StartingPoolType.Casual);


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

            lblPrizeFormat.setVisible(newVal == StartingPoolType.Sanctioned);
            cbxPrizeFormat.setVisible(newVal == StartingPoolType.Sanctioned);
            btnPrizeDefineCustomFormat.setVisible(newVal == StartingPoolType.CustomFormat);
            btnPrizeSelectFormat.setVisible(newVal == StartingPoolType.Casual);
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
    VSubmenuQuestStart() {

        lblTitleNew.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));

        final JXButtonPanel difficultyPanel = new JXButtonPanel();
        final String difficulty_constraints = "h 25px!, gapbottom 5";
        difficultyPanel.add(radEasy, difficulty_constraints);
        difficultyPanel.add(radMedium, difficulty_constraints);
        difficultyPanel.add(radHard, difficulty_constraints);
        difficultyPanel.add(radExpert, difficulty_constraints);
        radEasy.setSelected(true);

        boxCompleteSet.setToolTipText(localizer.getMessage("lblboxCompleteSet"));
        boxAllowDuplicates.setToolTipText(localizer.getMessage("lblboxAllowDuplicates"));
        lblStartingWorld.setLabelFor(cbxStartingWorld.getComponent());
        lblStartingPool.setLabelFor(cbxStartingPool.getComponent());
        lblPreconDeck.setLabelFor(cbxPreconDeck.getComponent());
        lblFormat.setLabelFor(cbxFormat.getComponent());
        lblCustomDeck.setLabelFor(cbxCustomDeck.getComponent());
        lblPrizedCards.setLabelFor(cbxPrizedCards.getComponent());
        lblPrizeFormat.setLabelFor(cbxPrizeFormat.getComponent());

        cbxStartingPool.addItem(StartingPoolType.Complete);
        cbxStartingPool.addItem(StartingPoolType.Sanctioned);
        cbxStartingPool.addItem(StartingPoolType.Casual);
        cbxStartingPool.addItem(StartingPoolType.CustomFormat);
        cbxStartingPool.addItem(StartingPoolType.Precon);
        cbxStartingPool.addItem(StartingPoolType.DraftDeck);
        cbxStartingPool.addItem(StartingPoolType.SealedDeck);
        cbxStartingPool.addItem(StartingPoolType.Cube);
        cbxStartingPool.addActionListener(alStartingPool);

        // initial adjustment
        alStartingPool.actionPerformed(null);
        alPrizesPool.actionPerformed(null);

        cbxPrizedCards.addItem(localizer.getMessage("lblSameAsStartingPool"));
        cbxPrizedCards.addItem(StartingPoolType.Complete);
        cbxPrizedCards.addItem(StartingPoolType.Sanctioned);
        cbxPrizedCards.addItem(StartingPoolType.Casual);
        cbxPrizedCards.addItem(StartingPoolType.CustomFormat);
        cbxPrizedCards.addActionListener(alPrizesPool);

        for (final GameFormat gf : FModel.getFormats().getSanctionedList()) {
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

        for (final PreconDeck preconDeck : QuestController.getPrecons()) {
            if (QuestController.getPreconDeals(preconDeck).getMinWins() > 0) {
                continue;
            }
            final String name = preconDeck.getName();
            cbxPreconDeck.addItem(name);
            String description = preconDeck.getDescription();
            preconDescriptions.put(name, WordUtil.wordWrapAsHTML(description));
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

        // Fantasy box selected by Default
        boxFantasy.setSelected(true);
        boxFantasy.setEnabled(true);

        // Commander box unselected by Default
        boxCommander.setSelected(false);
        boxCommander.setEnabled(true);

        boxCommander.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        if(!isCommander()) return; //do nothing if unselecting Commander Subformat
                        //Otherwise, set the starting world to Random Commander
                        cbxStartingWorld.setSelectedItem(FModel.getWorlds().get("Random Commander"));
                    }
                }

        );

        boxCompleteSet.setEnabled(true);
        boxAllowDuplicates.setEnabled(true);

        pnlOptions.setOpaque(false);
        pnlOptions.setLayout(new MigLayout("insets 0, gap 10px, fillx, wrap 2"));

        final JPanel pnlDifficultyMode = new JPanel(new MigLayout("insets 0, gap 1%, flowy"));
        pnlDifficultyMode.add(difficultyPanel, "gapright 4%");
        pnlDifficultyMode.add(boxFantasy, "h 25px!, gapbottom 15, gapright 4%");
        pnlDifficultyMode.add(boxCommander, "h 25px!, gapbottom 15, gapright 4%");
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
        pnlRestrictions.add(btnSelectFormat, btnStartingCustomFormatWidth + constraints + hidemode + " cell 1 1");

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
        pnlRestrictions.add(btnPrizeSelectFormat, btnStartingCustomFormatWidth + constraints + hidemode + "cell 1 6");
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

        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(lblTitleNew, "w 98%, h 30px!, gap 1% 0 15px 10px");
        VHomeUI.SINGLETON_INSTANCE.getPnlDisplay().add(pnlOptions, "w 98%!, growy, pushy, gap 1% 0 0 0");

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
        return localizer.getMessage("lblStartanewQuest");
    }

    /* (non-Javadoc)
     * @see forge.gui.home.IVSubmenu#getMenuName()
     */
    @Override
    public EDocID getItemEnum() {
        return getDocumentID();
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
        return EDocID.HOME_QUESTSTART;
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
    public CSubmenuQuestStart getLayoutControl() {
        return CSubmenuQuestStart.SINGLETON_INSTANCE;
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

    /**
     * Auth. Imakuni
     * @return True if the "Commander Subformat" check box is selected.
     */
    public boolean isCommander() {
        return boxCommander.isSelected();
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

    public GameFormat getCasualFormat() {
        return cbxFormat.getSelectedItem();
    }

    public GameFormat getPrizedCasualFormat() {
        return cbxPrizeFormat.getSelectedItem();
    }

    public FLabel getBtnCustomFormat() {
        return btnDefineCustomFormat;
    }
    public FLabel getBtnSelectFormat() {
        return btnSelectFormat;
    }
    public FLabel getBtnPrizeSelectFormat() {
        return btnPrizeSelectFormat;
    }
    public FLabel getBtnPrizeCustomFormat() {
        return btnPrizeDefineCustomFormat;
    }
    public FLabel getBtnPreferredColors() {
        return btnPreferredColors;
    }

}
