package forge.screens.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.text.WordUtils;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.FThreads;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.item.PreconDeck;
import forge.model.CardCollections;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.quest.QuestController;
import forge.quest.QuestMode;
import forge.quest.QuestUtil;
import forge.quest.QuestWorld;
import forge.quest.StartingPoolPreferences;
import forge.quest.StartingPoolType;
import forge.quest.data.GameFormatQuest;
import forge.quest.data.QuestPreferences.QPref;
import forge.screens.FScreen;
import forge.screens.LoadingOverlay;
import forge.toolbox.FCheckBox;
import forge.toolbox.FComboBox;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FScrollPane;
import forge.util.FileUtil;
import forge.util.ThreadUtil;
import forge.util.Utils;
import forge.util.gui.SOptionPane;
import forge.util.storage.IStorage;

public class NewQuestScreen extends FScreen {
    private static final float EMBARK_BTN_HEIGHT = 2 * Utils.AVG_FINGER_HEIGHT;
    private static final float PADDING = FOptionPane.PADDING;

    private final List<String> customFormatCodes = new ArrayList<String>();
    private final List<String> customPrizeFormatCodes = new ArrayList<String>();

    private final FScrollPane scroller = add(new FScrollPane() {
        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float x = PADDING;
            float y = PADDING;
            float right = visibleWidth - PADDING;
            float h = cbxStartingWorld.getHeight();

            for (FDisplayObject obj : getChildren()) {
                if (!obj.isVisible()) { continue; }

                if (obj instanceof FLabel) {
                    //display labels inline before object following them
                    FLabel lbl = (FLabel)obj;
                    if (lbl.getText().endsWith(":")) {
                        obj.setBounds(x, y, visibleWidth / 2 - x, h); //make label take up half of line so combo boxes all the same width
                        x += obj.getWidth();
                        continue;
                    }
                    else if (lbl.getAlignment() == HAlignment.RIGHT) {
                        y -= PADDING; //remove most of the padding above description text
                    }
                }

                //other objects should take up remainder of current line
                obj.setBounds(x, y, right - x, h);
                x = PADDING;
                y += h + PADDING;
            }
            return new ScrollBounds(visibleWidth, y);
        }
    });

    @SuppressWarnings("unused")
    private final FLabel lblStartingWorld = scroller.add(new FLabel.Builder().text("Starting world:").build());
    private final FComboBox<QuestWorld> cbxStartingWorld = scroller.add(new FComboBox<QuestWorld>());

    @SuppressWarnings("unused")
    private final FLabel lblDifficulty = scroller.add(new FLabel.Builder().text("Difficulty:").build());
    private final FComboBox<String> cbxDifficulty = scroller.add(new FComboBox<String>(new String[]{ "Easy", "Medium", "Hard", "Expert" }));

    @SuppressWarnings("unused")
    private final FLabel lblPreferredColor = scroller.add(new FLabel.Builder().text("Starting pool colors:").build());
    private final FComboBox<String> cbxPreferredColor = scroller.add(new FComboBox<String>());
    private final String stringBalancedDistribution = new String("balanced distribution");
    private final String stringRandomizedDistribution = new String("randomized distribution");
    private final String stringBias = new String(" bias");
    
    @SuppressWarnings("unused")
    private final FLabel lblStartingPool = scroller.add(new FLabel.Builder().text("Starting pool:").build());
    private final FComboBox<StartingPoolType> cbxStartingPool = scroller.add(new FComboBox<StartingPoolType>());

    private final FLabel lblUnrestricted = scroller.add(new FLabel.Builder().align(HAlignment.RIGHT).font(FSkinFont.get(12)).text("All cards will be available to play.").build());

    private final FLabel lblPreconDeck = scroller.add(new FLabel.Builder().text("Starter/Event deck:").build());
    private final FComboBox<String> cbxPreconDeck = scroller.add(new FComboBox<String>());

    private final FLabel lblFormat = scroller.add(new FLabel.Builder().text("Sanctioned format:").build());
    private final FComboBox<GameFormat> cbxFormat = scroller.add(new FComboBox<GameFormat>());

    private final FLabel lblCustomDeck = scroller.add(new FLabel.Builder().text("Custom deck:").build());
    private final FComboBox<Deck> cbxCustomDeck = scroller.add(new FComboBox<Deck>());

    private final FLabel btnDefineCustomFormat = scroller.add(new FLabel.ButtonBuilder().text("Define custom format").build());

    @SuppressWarnings("unused")
    private final FLabel lblPrizedCards = scroller.add(new FLabel.Builder().text("Prized cards:").build());
    private final FComboBox<Object> cbxPrizedCards = scroller.add(new FComboBox<Object>());

    private final FLabel lblPrizeFormat = scroller.add(new FLabel.Builder().text("Sanctioned format:").build());
    private final FComboBox<GameFormat> cbxPrizeFormat = scroller.add(new FComboBox<GameFormat>());

    private final FLabel lblPrizeUnrestricted = scroller.add(new FLabel.Builder().align(HAlignment.RIGHT).font(FSkinFont.get(12)).text("All cards will be available to win.").build());
    private final FLabel lblPrizeSameAsStarting = scroller.add(new FLabel.Builder().align(HAlignment.RIGHT).font(FSkinFont.get(12)).text("Only sets found in starting pool will be available.").build());
    private final FLabel btnPrizeDefineCustomFormat = scroller.add(new FLabel.ButtonBuilder().text("Define custom format").build());

    private final FCheckBox cbAllowUnlocks = scroller.add(new FCheckBox("Allow unlock of additional editions"));
    private final FCheckBox cbFantasy = scroller.add(new FCheckBox("Fantasy Mode"));

    private final FLabel btnEmbark = add(new FLabel.ButtonBuilder()
            .font(FSkinFont.get(22)).text("Embark!").icon(FSkinImage.QUEST_ZEP).command(new FEventHandler() {
                @Override
                public void handleEvent(FEvent e) {
                    //create new quest in game thread so option panes can wait for input
                    ThreadUtil.invokeInGameThread(new Runnable() {
                        @Override
                        public void run() {
                            newQuest();
                        }
                    });
                }
            }).build());

    public NewQuestScreen() {
        super("Start a New Quest");

        cbxStartingPool.addItem(StartingPoolType.Complete);
        cbxStartingPool.addItem(StartingPoolType.Rotating);
        cbxStartingPool.addItem(StartingPoolType.CustomFormat);
        cbxStartingPool.addItem(StartingPoolType.Precon);
        cbxStartingPool.addItem(StartingPoolType.DraftDeck);
        cbxStartingPool.addItem(StartingPoolType.SealedDeck);
        cbxStartingPool.addItem(StartingPoolType.Cube);
        cbxStartingPool.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                updateStartingPoolOptions();
                scroller.revalidate();
            }
        });

        cbxPrizedCards.addItem("Same as starting pool");
        cbxPrizedCards.addItem(StartingPoolType.Complete);
        cbxPrizedCards.addItem(StartingPoolType.Rotating);
        cbxPrizedCards.addItem(StartingPoolType.CustomFormat);
        cbxPrizedCards.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                updatePrizeOptions();
                scroller.revalidate();
            }
        });

        for (GameFormat gf : FModel.getFormats()) {
            cbxFormat.addItem(gf);
            cbxPrizeFormat.addItem(gf);
        }

        // Initialize color balance selection
        cbxPreferredColor.addItem(stringBalancedDistribution);
        cbxPreferredColor.addItem(stringRandomizedDistribution);
        cbxPreferredColor.addItem(MagicColor.Constant.WHITE + stringBias);
        cbxPreferredColor.addItem(MagicColor.Constant.BLUE + stringBias);
        cbxPreferredColor.addItem(MagicColor.Constant.BLACK + stringBias);
        cbxPreferredColor.addItem(MagicColor.Constant.RED + stringBias);
        cbxPreferredColor.addItem(MagicColor.Constant.GREEN + stringBias);
        cbxPreferredColor.addItem(MagicColor.Constant.COLORLESS + stringBias);

        for (QuestWorld qw : FModel.getWorlds()) {
            cbxStartingWorld.addItem(qw);
        }
        // Default to 'Main world'
        cbxStartingWorld.setSelectedItem(FModel.getWorlds().get("Main world"));

        cbxStartingWorld.setChangedHandler(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                updateEnabledFormats();
            }
        });

        updateStartingPoolOptions();
        updatePrizeOptions();
        updateEnabledFormats();

        cbAllowUnlocks.setSelected(true);

        final Map<String, String> preconDescriptions = new HashMap<String, String>();
        IStorage<PreconDeck> preconDecks = QuestController.getPrecons();

        for (PreconDeck preconDeck : preconDecks) {
            if (QuestController.getPreconDeals(preconDeck).getMinWins() > 0) {
                continue;
            }
            String name = preconDeck.getName();
            cbxPreconDeck.addItem(name);
            String description = preconDeck.getDescription();
            description = "<html>" + WordUtils.wrap(description, 40, "<br>", false) + "</html>";
            preconDescriptions.put(name, description);
        }

        // disable the very powerful sets -- they can be unlocked later for a high price
        final List<String> unselectableSets = new ArrayList<String>();
        unselectableSets.add("LEA");
        unselectableSets.add("LEB");
        unselectableSets.add("MBP");
        unselectableSets.add("VAN");
        unselectableSets.add("ARC");
        unselectableSets.add("PC2");

        btnDefineCustomFormat.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                /*final DialogChooseSets dialog = new DialogChooseSets(customFormatCodes, unselectableSets, false);
                dialog.setOkCallback(new Runnable() {
                    @Override
                    public void run() {
                        customFormatCodes.clear();
                        customFormatCodes.addAll(dialog.getSelectedSets());
                    }
                });*/
            }
        });

        btnPrizeDefineCustomFormat.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                /*final DialogChooseSets dialog = new DialogChooseSets(customPrizeFormatCodes, unselectableSets, false);
                dialog.setOkCallback(new Runnable() {
                    @Override
                    public void run() {
                        customPrizeFormatCodes.clear();
                        customPrizeFormatCodes.addAll(dialog.getSelectedSets());
                    }
                });*/
            }
        });

        // Fantasy box enabled by Default
        cbFantasy.setSelected(true);
        cbFantasy.setEnabled(true);

        cbxPreferredColor.setEnabled(true);
    }

    private void updateStartingPoolOptions() {
        StartingPoolType newVal = getStartingPoolType();
        lblUnrestricted.setVisible(newVal == StartingPoolType.Complete);

        lblPreconDeck.setVisible(newVal == StartingPoolType.Precon);
        cbxPreconDeck.setVisible(newVal == StartingPoolType.Precon);

        lblFormat.setVisible(newVal == StartingPoolType.Rotating);
        cbxFormat.setVisible(newVal == StartingPoolType.Rotating);

        btnDefineCustomFormat.setVisible(newVal == StartingPoolType.CustomFormat);

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

        lblPrizeFormat.setVisible(newVal == StartingPoolType.Rotating);
        cbxPrizeFormat.setVisible(newVal == StartingPoolType.Rotating);
        btnPrizeDefineCustomFormat.setVisible(newVal == StartingPoolType.CustomFormat);
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
        return cbxPreconDeck.getSelectedItem().toString();
    }

    public Deck getSelectedDeck() {
        Object sel = cbxCustomDeck.getSelectedItem();
        return sel instanceof Deck ? (Deck) sel : null;
    }

    public boolean isUnlockSetsAllowed() {
        return cbAllowUnlocks.isSelected();
    }

    public StartingPoolType getStartingPoolType() {
        return (StartingPoolType) cbxStartingPool.getSelectedItem();
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

    public boolean randomizeColorDistribution() {
        return stringRandomizedDistribution.equals(cbxPreferredColor.getSelectedItem());
    }

    public byte getPreferredColor() {
        if (stringBalancedDistribution.equals(cbxPreferredColor.getSelectedItem())
                || stringRandomizedDistribution.equals(cbxPreferredColor.getSelectedItem())) {
            return MagicColor.ALL_COLORS;
        }
        return MagicColor.fromName(cbxPreferredColor.getSelectedItem().split(" ")[0]);
    }

    public GameFormat getRotatingFormat() {
        return (GameFormat) cbxFormat.getSelectedItem();
    }

    public GameFormat getPrizedRotatingFormat() {
        return (GameFormat) cbxPrizeFormat.getSelectedItem();
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
            case Rotating:
                fmtStartPool = getRotatingFormat();
                break;

            case CustomFormat:
                if (customFormatCodes.isEmpty()) {
                    if (!SOptionPane.showConfirmDialog("You have defined a custom format that doesn't contain any sets.\nThis will start a game without restriction.\n\nContinue?")) {
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
                    SOptionPane.showMessageDialog("You have not selected a deck to start.", "Cannot start a quest", SOptionPane.ERROR_ICON);
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

        GameFormat fmtPrizes = null;

        // The starting QuestWorld format should NOT affect what you get if you travel to a world that doesn't have one...
        // if (worldFormat == null) {
        StartingPoolType prizedPoolType = getPrizedPoolType();
        if (null == prizedPoolType) {
            fmtPrizes = fmtStartPool;
            if (null == fmtPrizes && dckStartPool != null) { // build it form deck
                Set<String> sets = new HashSet<String>();
                for (Entry<PaperCard, Integer> c : dckStartPool.getMain()) {
                    sets.add(c.getKey().getEdition());
                }
                if (dckStartPool.has(DeckSection.Sideboard)) {
                    for (Entry<PaperCard, Integer> c : dckStartPool.get(DeckSection.Sideboard)) {
                        sets.add(c.getKey().getEdition());
                    }
                }
                fmtPrizes = new GameFormat("From deck", sets, null);
            }
        }
        else {
            switch(prizedPoolType) {
            case Complete:
                fmtPrizes = null;
                break;
            case CustomFormat:
                if (customPrizeFormatCodes.isEmpty()) {
                    if (!SOptionPane.showConfirmDialog("You have defined custom format as containing no sets.\nThis will choose all editions without restriction as prized.\n\nContinue?")) {
                        return;
                    }
                }
                fmtPrizes = customPrizeFormatCodes.isEmpty() ? null : new GameFormat("Custom Prizes", customPrizeFormatCodes, null); // chosen sets and no banend cards
                break;
            case Rotating:
                fmtPrizes = getPrizedRotatingFormat();
                break;
            default:
                throw new RuntimeException("Should not get this result");
            }
        }

        String questName;
        while (true) {
            questName = SOptionPane.showInputDialog("Poets will remember your quest as:", "Quest Name");
            if (questName == null) { return; }

            questName = QuestUtil.cleanString(questName);

            if (questName.isEmpty()) {
                SOptionPane.showMessageDialog("Please specify a quest name.");
                continue;
            }
            if (FileUtil.doesFileExist(ForgeConstants.QUEST_SAVE_DIR + questName + ".dat")) {
                SOptionPane.showMessageDialog("A quest already exists with that name. Please pick another quest name.");
                continue;
            }
            break;
        }

        startNewQuest(questName, fmtPrizes, dckStartPool, fmtStartPool);
    }

    private void startNewQuest(final String questName, final GameFormat fmtPrizes, final Deck dckStartPool, final GameFormat fmtStartPool) {
        FThreads.invokeInEdtLater(new Runnable() {
            @Override
            public void run() {
                LoadingOverlay.show("Starting new quest...", new Runnable() {
                    @Override
                    public void run() {
                        final QuestMode mode = isFantasy() ? QuestMode.Fantasy : QuestMode.Classic;
                        final StartingPoolPreferences userPrefs = new StartingPoolPreferences(randomizeColorDistribution(), getPreferredColor());
                        QuestController qc = FModel.getQuest();
                        qc.newGame(questName, getSelectedDifficulty(), mode, fmtPrizes, isUnlockSetsAllowed(), dckStartPool, fmtStartPool, getStartingWorldName(), userPrefs);
                        qc.save();

                        // Save in preferences.
                        FModel.getQuestPreferences().setPref(QPref.CURRENT_QUEST, questName + ".dat");
                        FModel.getQuestPreferences().save();

                        QuestMenu.launchQuestMode(); //launch quest mode for new quest
                    }
                });
            }
        });
    }
}
