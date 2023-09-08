package forge.screens.quest;

import java.util.*;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameFormat;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestMode;
import forge.gamemodes.quest.QuestUtil;
import forge.gamemodes.quest.QuestWorld;
import forge.gamemodes.quest.StartingPoolPreferences;
import forge.gamemodes.quest.StartingPoolPreferences.PoolType;
import forge.gamemodes.quest.StartingPoolType;
import forge.gamemodes.quest.data.DeckConstructionRules;
import forge.gamemodes.quest.data.GameFormatQuest;
import forge.gamemodes.quest.data.QuestPreferences.QPref;
import forge.gui.FThreads;
import forge.gui.UiCommand;
import forge.gui.util.SOptionPane;
import forge.item.PaperCard;
import forge.item.PreconDeck;
import forge.itemmanager.filters.ArchivedFormatSelect;
import forge.localinstance.properties.ForgeConstants;
import forge.model.CardCollections;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.NewGameMenu;
import forge.screens.quest.QuestMenu.LaunchReason;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FLabel;
import forge.toolbox.FNumericTextField;
import forge.toolbox.FOptionPane;
import forge.toolbox.FRadioButton;
import forge.toolbox.FRadioButton.RadioButtonGroup;
import forge.toolbox.FScrollPane;
import forge.util.FileUtil;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class NewQuestScreen extends FScreen {

    private static final float EMBARK_BTN_HEIGHT = 2 * Utils.AVG_FINGER_HEIGHT;
    private static final float PADDING = FOptionPane.PADDING;

    private final List<String> customFormatCodes = new ArrayList<>();
    private final List<String> customPrizeFormatCodes = new ArrayList<>();

    private final class FColorCheckBox extends FCheckBox {
        private FColorCheckBox(final String text0) {
            super(text0);
        }
    }

    private final FScrollPane scroller = add(new FScrollPane() {

        private int colorBoxCount = 0;

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float x = PADDING;
            float y = PADDING;
            float right = visibleWidth - PADDING;
            float h = cbxStartingWorld.getHeight();

            float gapY = PADDING / 2;

            for (FDisplayObject obj : getChildren()) {
                if (!obj.isVisible()) {
                    continue;
                }

                if (obj instanceof FColorCheckBox) {
                    float visWidth = (visibleWidth - PADDING) / 3;
                    float xAdjustment = visWidth * (colorBoxCount++ % 3);
                    if (colorBoxCount == 1 || colorBoxCount == 4) {
                        xAdjustment += PADDING;
                    }
                    obj.setBounds(xAdjustment, y, visWidth, h); //make label take up half of line so combo boxes all the same width
                    x += obj.getWidth();
                    if (colorBoxCount % 3 == 0) {
                        y += h + gapY;
                        x = PADDING;
                    }
                    continue;
                }

                if (obj instanceof FLabel && obj != lblPoolDistribution && obj != lblPoolOptions && obj != lblPreferredColor) {
                    //display labels inline before object following them
                    FLabel lbl = (FLabel) obj;
                    if (lbl.getText().endsWith(":")) {
                        obj.setBounds(x, y, visibleWidth / 2 - x, h); //make label take up half of line so combo boxes all the same width
                        x += obj.getWidth();
                        continue;
                    } else if (lbl.getAlignment() == Align.right) {
                        y -= gapY; //remove most of the padding above description text
                    }
                }

                //other objects should take up remainder of current line
                obj.setBounds(x, y, right - x, h);
                x = PADDING;
                y += h + gapY;
            }
            return new ScrollBounds(visibleWidth, y);
        }
    });

    @SuppressWarnings("unused")
    private final FLabel lblStartingWorld = scroller.add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblStartingWorld") + ":").build());
    private final FComboBox<QuestWorld> cbxStartingWorld = scroller.add(new FComboBox<>());

    @SuppressWarnings("unused")
    private final FLabel lblDifficulty = scroller.add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblDifficulty") + ":").build());
    private final FComboBox<String> cbxDifficulty = scroller.add(new FComboBox<>(new String[]{
        Forge.getLocalizer().getMessage("questDifficultyEasy"), Forge.getLocalizer().getMessage("questDifficultyMedium"),
        Forge.getLocalizer().getMessage("questDifficultyHard"), Forge.getLocalizer().getMessage("questDifficultyExpert")
    }));

    @SuppressWarnings("unused")
    private final FLabel lblStartingPool = scroller.add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblStartingPool") + ":").build());
    private final FComboBox<StartingPoolType> cbxStartingPool = scroller.add(new FComboBox<>());

    private final FLabel lblUnrestricted = scroller.add(new FLabel.Builder().align(Align.right).font(FSkinFont.get(12)).text(Forge.getLocalizer().getMessage("lblAllCardsAvailable")).build());

    private final FLabel lblPreconDeck = scroller.add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblStarterEventdeck") + ":").build());
    private final FComboBox<String> cbxPreconDeck = scroller.add(new FComboBox<>());

    private final FLabel lblFormat = scroller.add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblSelectFormat") + ":").build());
    private final FComboBox<GameFormat> cbxFormat = scroller.add(new FComboBox<>());

    private final FLabel lblCustomDeck = scroller.add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblCustomdeck") + ":").build());
    private final FComboBox<Deck> cbxCustomDeck = scroller.add(new FComboBox<>());

    private final FLabel btnSelectFormat = scroller.add(new FLabel.ButtonBuilder().text(Forge.getLocalizer().getMessage("lblChooseFormats")).build());

    private final FLabel lblPoolDistribution = scroller.add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblStartingPoolDistribution") + ":").build());
    private final FRadioButton radBalanced = scroller.add(new FRadioButton(Forge.getLocalizer().getMessage("lblBalanced")));
    private final FRadioButton radSurpriseMe = scroller.add(new FRadioButton(Forge.getLocalizer().getMessage("lblSurpriseMe")));
    private final FRadioButton radRandom = scroller.add(new FRadioButton(Forge.getLocalizer().getMessage("lblRandom")));
    private final FRadioButton radBoosters = scroller.add(new FRadioButton(Forge.getLocalizer().getMessage("lblBoosters")));
    private final FNumericTextField numberOfBoostersField = scroller.add(new FNumericTextField(10));

    private final FLabel lblPreferredColor = scroller.add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblStartingPoolColors") + ":").build());
    private final FColorCheckBox cbBlack = scroller.add(new FColorCheckBox(Forge.getLocalizer().getMessage("lblBlack")));
    private final FColorCheckBox cbBlue = scroller.add(new FColorCheckBox(Forge.getLocalizer().getMessage("lblBlue")));
    private final FColorCheckBox cbGreen = scroller.add(new FColorCheckBox(Forge.getLocalizer().getMessage("lblGreen")));
    private final FColorCheckBox cbRed = scroller.add(new FColorCheckBox(Forge.getLocalizer().getMessage("lblRed")));
    private final FColorCheckBox cbWhite = scroller.add(new FColorCheckBox(Forge.getLocalizer().getMessage("lblWhite")));
    private final FColorCheckBox cbColorless = scroller.add(new FColorCheckBox(Forge.getLocalizer().getMessage("lblColorless")));

    private final FLabel lblPoolOptions = scroller.add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblStartingPoolOptions") + ":").build());
    private final FCheckBox cbCompleteSet = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblStartWithAllCards")));
    private final FCheckBox cbDuplicateCards = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblAllowDuplicatesStartingPool")));
    private final FCheckBox cbIncludeArtifacts = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblIncludeArtifactsStartingPool")));

    @SuppressWarnings("unused")
    private final FLabel lblPrizedCards = scroller.add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblPrizedCards") + ":").build());
    private final FComboBox<Object> cbxPrizedCards = scroller.add(new FComboBox<>());

    private final FLabel lblPrizeFormat = scroller.add(new FLabel.Builder().text(Forge.getLocalizer().getMessage("lblDefinedFormat") + ":").build());
    private final FComboBox<GameFormat> cbxPrizeFormat = scroller.add(new FComboBox<>());

    private final FLabel lblPrizeUnrestricted = scroller.add(new FLabel.Builder().align(Align.right).font(FSkinFont.get(12)).text(Forge.getLocalizer().getMessage("lblAllCardsAvailableWin")).build());
    private final FLabel lblPrizeSameAsStarting = scroller.add(new FLabel.Builder().align(Align.right).font(FSkinFont.get(12)).text(Forge.getLocalizer().getMessage("lblOnlyInStartingPoolAvailable")).build());
    private final FLabel btnPrizeSelectFormat = scroller.add(new FLabel.ButtonBuilder().text(Forge.getLocalizer().getMessage("lblChooseFormats")).build());

    private final FCheckBox cbAllowUnlocks = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("lblAllowUnlockAdEd")));
    private final FCheckBox cbFantasy = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("rbFantasyMode")));
    private final FCheckBox cbCommander = scroller.add(new FCheckBox(Forge.getLocalizer().getMessage("rbCommanderSubformat")));

    private final FLabel btnEmbark = add(new FLabel.ButtonBuilder()
            .font(FSkinFont.get(22)).text(Forge.getLocalizer().getMessage("lblEmbark")).icon(FSkinImage.QUEST_ZEP).command(event -> {
                //create new quest in game thread so option panes can wait for input
                ThreadUtil.invokeInGameThread(() -> newQuest());
            }).build());

    public NewQuestScreen() {
        super(null, NewGameMenu.getMenu());

        cbxStartingPool.addItem(StartingPoolType.Complete);
        cbxStartingPool.addItem(StartingPoolType.Sanctioned);
        cbxStartingPool.addItem(StartingPoolType.Casual);
        cbxStartingPool.addItem(StartingPoolType.Precon);
        cbxStartingPool.addItem(StartingPoolType.DraftDeck);
        cbxStartingPool.addItem(StartingPoolType.SealedDeck);
        cbxStartingPool.addItem(StartingPoolType.Cube);
        cbxStartingPool.setChangedHandler(event -> {
            updateStartingPoolOptions();
            scroller.revalidate();
        });

        cbxPrizedCards.addItem(Forge.getLocalizer().getMessage("lblSameAsStartingPool"));
        cbxPrizedCards.addItem(StartingPoolType.Complete);
        cbxPrizedCards.addItem(StartingPoolType.Sanctioned);
        cbxPrizedCards.addItem(StartingPoolType.Casual);
        cbxPrizedCards.setChangedHandler(event -> {
            updatePrizeOptions();
            scroller.revalidate();
        });

        for (GameFormat gf : FModel.getFormats().getSanctionedList()) {
            cbxFormat.addItem(gf);
            cbxPrizeFormat.addItem(gf);
        }

        RadioButtonGroup distributionGroup = new RadioButtonGroup();
        radBalanced.setGroup(distributionGroup);
        radBalanced.setSelected(true);
        radRandom.setGroup(distributionGroup);
        radBoosters.setGroup(distributionGroup);
        radSurpriseMe.setGroup(distributionGroup);
        numberOfBoostersField.setEnabled(false);

        @SuppressWarnings("serial")
        UiCommand colorBoxEnabler = () -> {
            cbBlack.setEnabled(radBalanced.isSelected());
            cbBlue.setEnabled(radBalanced.isSelected());
            cbGreen.setEnabled(radBalanced.isSelected());
            cbRed.setEnabled(radBalanced.isSelected());
            cbWhite.setEnabled(radBalanced.isSelected());
            cbColorless.setEnabled(radBalanced.isSelected());
            cbIncludeArtifacts.setEnabled(!radSurpriseMe.isSelected());
            numberOfBoostersField.setEnabled(radBoosters.isSelected());
        };

        radBalanced.setCommand(colorBoxEnabler);
        radRandom.setCommand(colorBoxEnabler);
        radBoosters.setCommand(colorBoxEnabler);
        radSurpriseMe.setCommand(colorBoxEnabler);

        for (QuestWorld qw : FModel.getWorlds()) {
            cbxStartingWorld.addItem(qw);
        }
        // Default to 'Main world'
        cbxStartingWorld.setSelectedItem(FModel.getWorlds().get("Main world"));

        cbxStartingWorld.setChangedHandler(event -> updateEnabledFormats());

        updateStartingPoolOptions();
        updatePrizeOptions();
        updateEnabledFormats();

        cbAllowUnlocks.setSelected(true);

        for (PreconDeck preconDeck : QuestController.getPrecons()) {
            if (QuestController.getPreconDeals(preconDeck).getMinWins() > 0) {
                continue;
            }
            String name = preconDeck.getName();
            cbxPreconDeck.addItem(name);
        }

        // disable the very powerful sets -- they can be unlocked later for a high price
        final List<String> unselectableSets = new ArrayList<>();
        unselectableSets.add("LEA");
        unselectableSets.add("LEB");
        unselectableSets.add("MBP");
        unselectableSets.add("VAN");
        unselectableSets.add("ARC");
        unselectableSets.add("PC2");

        btnSelectFormat.setCommand(event -> {
            ArchivedFormatSelect archivedFormatSelect = new ArchivedFormatSelect();
            archivedFormatSelect.setOnCloseCallBack(() -> {
                customFormatCodes.clear();
                btnSelectFormat.setText(archivedFormatSelect.getSelectedFormat().getName());
                List<String> setsToAdd = archivedFormatSelect.getSelectedFormat().getAllowedSetCodes();
                for (String setName:setsToAdd){
                    if(!unselectableSets.contains(setName)){
                        customFormatCodes.add(setName);
                    }
                }
            });
            Forge.openScreen(archivedFormatSelect);
        });

        btnPrizeSelectFormat.setCommand(event -> {
            ArchivedFormatSelect archivedFormatSelect = new ArchivedFormatSelect();
            archivedFormatSelect.setOnCloseCallBack(() -> {
                customPrizeFormatCodes.clear();
                btnPrizeSelectFormat.setText(archivedFormatSelect.getSelectedFormat().getName());
                List<String> setsToAdd = archivedFormatSelect.getSelectedFormat().getAllowedSetCodes();
                for (String setName:setsToAdd){
                    if(!unselectableSets.contains(setName)){
                        customPrizeFormatCodes.add(setName);
                    }
                }
            });
            Forge.openScreen(archivedFormatSelect);
        });

        // Fantasy box enabled by Default
        cbFantasy.setSelected(true);
        cbFantasy.setEnabled(true);
        cbCommander.setSelected(false);
        cbCommander.setCommand(event -> {
            if (!isCommander())
                return;
            cbxStartingWorld.setSelectedItem(FModel.getWorlds().get("Random Commander"));
        });

    }

    private void updateStartingPoolOptions() {
        StartingPoolType newVal = getStartingPoolType();
        lblUnrestricted.setVisible(newVal == StartingPoolType.Complete);

        lblPreconDeck.setVisible(newVal == StartingPoolType.Precon);
        cbxPreconDeck.setVisible(newVal == StartingPoolType.Precon);

        lblFormat.setVisible(newVal == StartingPoolType.Sanctioned);
        cbxFormat.setVisible(newVal == StartingPoolType.Sanctioned);

        btnSelectFormat.setVisible(newVal == StartingPoolType.Casual);

        boolean usesDeckList = newVal == StartingPoolType.SealedDeck || newVal == StartingPoolType.DraftDeck || newVal == StartingPoolType.Cube;
        lblCustomDeck.setVisible(usesDeckList);
        cbxCustomDeck.setVisible(usesDeckList);

        if (usesDeckList) {
            cbxCustomDeck.removeAllItems();
            CardCollections decks = FModel.getDecks();
            switch (newVal) {
            case SealedDeck:
                for (DeckGroup d : decks.getSealed()) {
                    cbxCustomDeck.addItem(d.getHumanDeck());
                }
                break;
            case DraftDeck:
                for (DeckGroup d : decks.getDraft()) {
                    cbxCustomDeck.addItem(d.getHumanDeck());
                }
                break;
            case Cube:
                for (Deck d : decks.getCubes()) {
                    cbxCustomDeck.addItem(d);
                }
                break;
            default:
                break;
            }
        }
    }

    private void updatePrizeOptions() {
        StartingPoolType newVal = getPrizedPoolType();
        lblPrizeUnrestricted.setVisible(newVal == StartingPoolType.Complete);
        cbAllowUnlocks.setVisible(newVal != StartingPoolType.Complete);

        lblPrizeFormat.setVisible(newVal == StartingPoolType.Sanctioned);
        cbxPrizeFormat.setVisible(newVal == StartingPoolType.Sanctioned);
        btnPrizeSelectFormat.setVisible(newVal == StartingPoolType.Casual);
        lblPrizeSameAsStarting.setVisible(newVal == null);

        scroller.revalidate();
    }

    /**
     * Aux function for enabling or disabling the format selection according to world selection.
     */
    private void updateEnabledFormats() {
        final QuestWorld qw = FModel.getWorlds().get(getStartingWorldName());
        if (qw != null) {
            cbxStartingPool.setEnabled(qw.getFormat() == null);
            cbxFormat.setEnabled(qw.getFormat() == null);
            cbxCustomDeck.setEnabled(qw.getFormat() == null);
            btnSelectFormat.setEnabled(qw.getFormat() == null);

            // Do NOT disable the following...
            // cbxPrizeFormat.setEnabled(qw.getFormat() == null);
            // cboAllowUnlocks.setEnabled(qw.getFormat() == null);
            // cbxPrizedCards.setEnabled(qw.getFormat() == null);
        }
    }

    public int getSelectedDifficulty() {
        int difficulty = cbxDifficulty.getSelectedIndex();
        if (difficulty < 0) {
            difficulty = 0;
        }
        return difficulty;
    }

    public String getSelectedPrecon() {
        return cbxPreconDeck.getSelectedItem();
    }

    public Deck getSelectedDeck() {
        Object sel = cbxCustomDeck.getSelectedItem();
        return sel != null ? (Deck) sel : null;
    }

    public boolean isUnlockSetsAllowed() {
        return cbAllowUnlocks.isSelected();
    }

    public boolean startWithCompleteSet() {
        return cbCompleteSet.isSelected();
    }

    public boolean allowDuplicateCards() {
        return cbDuplicateCards.isSelected();
    }

    public StartingPoolType getStartingPoolType() {
        return cbxStartingPool.getSelectedItem();
    }

    public StartingPoolType getPrizedPoolType() {
         Object v = cbxPrizedCards.getSelectedItem();
         return v instanceof StartingPoolType ? (StartingPoolType) v : null;
    }

    public String getStartingWorldName() {
        return cbxStartingWorld.getSelectedItem().toString();
    }

    public boolean isFantasy() {
        return cbFantasy.isSelected();
    }
    public boolean isCommander() { return cbCommander.isSelected(); }

    public PoolType getPoolType() {
        if (radSurpriseMe.isSelected()) {
            return PoolType.RANDOM_BALANCED;
        }
        if (radRandom.isSelected()) {
            return PoolType.RANDOM;
        }
        if (radBoosters.isSelected()){
            return PoolType.BOOSTERS;
        }
        return PoolType.BALANCED;
    }

    public List<Byte> getPreferredColors() {

        List<Byte> preferredColors = new ArrayList<>();

        if (cbBlack.isSelected()) {
            preferredColors.add(MagicColor.BLACK);
        }
        if (cbBlue.isSelected()) {
            preferredColors.add(MagicColor.BLUE);
        }
        if (cbGreen.isSelected()) {
            preferredColors.add(MagicColor.GREEN);
        }
        if (cbRed.isSelected()) {
            preferredColors.add(MagicColor.RED);
        }
        if (cbWhite.isSelected()) {
            preferredColors.add(MagicColor.WHITE);
        }
        if (cbColorless.isSelected()) {
            preferredColors.add(MagicColor.COLORLESS);
        }

        return preferredColors;

    }

    public GameFormat getSanctionedFormat() {
        return cbxFormat.getSelectedItem();
    }

    public GameFormat getPrizedRotatingFormat() {
        return cbxPrizeFormat.getSelectedItem();
    }

    @Override
    protected void doLayout(float startY, float width, float height) {
        btnEmbark.setBounds(PADDING, height - EMBARK_BTN_HEIGHT - PADDING, width - 2 * PADDING, EMBARK_BTN_HEIGHT);
        scroller.setBounds(0, startY, width, btnEmbark.getTop() - startY);
    }

    /**
     * The actuator for new quests.
     */
    private void newQuest() {
        Deck dckStartPool = null;
        GameFormat fmtStartPool = null;
        QuestWorld startWorld = FModel.getWorlds().get(getStartingWorldName());
        GameFormat worldFormat = (startWorld == null ? null : startWorld.getFormat());

        if (worldFormat == null) {
            switch(getStartingPoolType()) {
            case Sanctioned:
                fmtStartPool = getSanctionedFormat();
                break;

            case Casual:
            case CustomFormat:
                if (customFormatCodes.isEmpty()) {
                    if (!SOptionPane.showConfirmDialog(
                            Forge.getLocalizer().getMessage("lblCustomFormatWithoutRestrictionGame"))) {
                        return;
                    }
                }
                fmtStartPool = customFormatCodes.isEmpty() ? null : new GameFormatQuest("Custom", customFormatCodes, null); // chosen sets and no banend cards
                break;

            case DraftDeck:
            case SealedDeck:
            case Cube:
                dckStartPool = getSelectedDeck();
                if (dckStartPool == null) {
                    SOptionPane.showMessageDialog(
                            Forge.getLocalizer().getMessage("lbldckStartPool"), Forge.getLocalizer().getMessage("lblCannotStartaQuest"), SOptionPane.ERROR_ICON);
                    return;
                }
                break;

            case Precon:
                dckStartPool = QuestController.getPrecons().get(getSelectedPrecon()).getDeck();
                break;

            case Complete:
            default:
                // leave everything as nulls
                break;
            }
        }
        else {
            fmtStartPool = worldFormat;
        }

        GameFormat fmtPrizes;

        // The starting QuestWorld format should NOT affect what you get if you travel to a world that doesn't have one...
        // if (worldFormat == null) {
        StartingPoolType prizedPoolType = getPrizedPoolType();
        if (null == prizedPoolType) {
            fmtPrizes = fmtStartPool;
            if (null == fmtPrizes && dckStartPool != null) { // build it form deck
                Set<String> sets = new HashSet<>();
                for(PaperCard card : dckStartPool.getAllCardsInASinglePool().toFlatList()) {
                    sets.add(card.getEdition());
                }
                fmtPrizes = new GameFormat("From deck", sets, null);
            }
        }
        else {
            switch(prizedPoolType) {
            case Complete:
                fmtPrizes = null;
                break;
            case Casual:
            case CustomFormat:
                if (customPrizeFormatCodes.isEmpty()) {
                    if (!SOptionPane.showConfirmDialog(
                            Forge.getLocalizer().getMessage("lblCustomFormatWithoutRestrictionPrized"))) {
                        return;
                    }
                }
                fmtPrizes = customPrizeFormatCodes.isEmpty() ? null : new GameFormat("Custom Prizes", customPrizeFormatCodes, null); // chosen sets and no banned cards
                break;
            case Sanctioned:
                fmtPrizes = getPrizedRotatingFormat();
                break;
            default:
                throw new RuntimeException("Should not get this result");
            }
        }

        String questName;
        while (true) {
            questName = SOptionPane.showInputDialog(Forge.getLocalizer().getMessage("MsgQuestNewName") + ":", Forge.getLocalizer().getMessage("TitQuestNewName"));
            if (questName == null) { return; }

            questName = QuestUtil.cleanString(questName);

            if (questName.isEmpty()) {
                SOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblQuestNameEmpty"));
                continue;
            }
            if (FileUtil.doesFileExist(ForgeConstants.QUEST_SAVE_DIR + questName + ".dat")) {
                SOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblQuestExists"));
                continue;
            }
            break;
        }

        startNewQuest(questName, fmtPrizes, dckStartPool, fmtStartPool);
    }

    private void startNewQuest(final String questName, final GameFormat fmtPrizes, final Deck dckStartPool, final GameFormat fmtStartPool) {
        FThreads.invokeInEdtLater(() -> LoadingOverlay.show(Forge.getLocalizer().getMessage("lblCreatingNewQuest"), true, () -> {
            final QuestMode mode = isFantasy() ? QuestMode.Fantasy : QuestMode.Classic;
            final StartingPoolPreferences userPrefs =
                    new StartingPoolPreferences(getPoolType(), getPreferredColors(), cbIncludeArtifacts.isSelected(), startWithCompleteSet(), allowDuplicateCards(), numberOfBoostersField.getValue());
            QuestController qc = FModel.getQuest();

            DeckConstructionRules dcr = isCommander() ?  DeckConstructionRules.Commander: DeckConstructionRules.Default;

            qc.newGame(questName, getSelectedDifficulty(), mode, fmtPrizes, isUnlockSetsAllowed(), dckStartPool, fmtStartPool, getStartingWorldName(), userPrefs, dcr);
            qc.save();

            // Save in preferences.
            FModel.getQuestPreferences().setPref(QPref.CURRENT_QUEST, questName + ".dat");
            FModel.getQuestPreferences().save();

            QuestMenu.launchQuestMode(LaunchReason.NewQuest, isCommander()); //launch quest mode for new quest
        }));
    }
}
