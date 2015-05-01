package forge.screens.home.quest;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.SwingUtilities;

import forge.UiCommand;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.game.GameFormat;
import forge.gui.framework.ICDoc;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.quest.QuestController;
import forge.quest.QuestMode;
import forge.quest.QuestUtil;
import forge.quest.QuestWorld;
import forge.quest.StartingPoolPreferences;
import forge.quest.StartingPoolType;
import forge.quest.data.GameFormatQuest;
import forge.quest.data.QuestData;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.io.QuestDataIO;
import forge.toolbox.FOptionPane;

/**
 * Controls the quest data submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuQuestData implements ICDoc {
    SINGLETON_INSTANCE;

    private final Map<String, QuestData> arrQuests = new HashMap<String, QuestData>();

    private final VSubmenuQuestData view = VSubmenuQuestData.SINGLETON_INSTANCE;
    private final List<String> customFormatCodes = new ArrayList<String>();
    private final List<String> customPrizeFormatCodes = new ArrayList<String>();

    private final UiCommand cmdQuestSelect = new UiCommand() {
        @Override public final void run() {
            changeQuest();
        }
    };
    private final UiCommand cmdQuestUpdate = new UiCommand() {
        @Override public final void run() {
            update();
        }
    };

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        view.getBtnEmbark().setCommand(
                new UiCommand() { @Override public void run() { newQuest(); } });

        // disable the very powerful sets -- they can be unlocked later for a high price
        final List<String> unselectableSets = new ArrayList<String>();
        unselectableSets.add("LEA");
        unselectableSets.add("LEB");
        unselectableSets.add("MBP");
        unselectableSets.add("VAN");
        unselectableSets.add("ARC");
        unselectableSets.add("PC2");

        view.getBtnCustomFormat().setCommand(new UiCommand() { @Override public void run() {
            final DialogChooseSets dialog = new DialogChooseSets(customFormatCodes, unselectableSets, false);
            dialog.setOkCallback(new Runnable() {
                @Override
                public void run() {
                    customFormatCodes.clear();
                    customFormatCodes.addAll(dialog.getSelectedSets());
                }
            });
        } });

        view.getBtnPrizeCustomFormat().setCommand(new UiCommand() { @Override public void run() {
            final DialogChooseSets dialog = new DialogChooseSets(customPrizeFormatCodes, unselectableSets, false);
            dialog.setOkCallback(new Runnable() {
                @Override
                public void run() {
                    customPrizeFormatCodes.clear();
                    customPrizeFormatCodes.addAll(dialog.getSelectedSets());
                }
            });
        } });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuQuestData view = VSubmenuQuestData.SINGLETON_INSTANCE;
        final File dirQuests = new File(ForgeConstants.QUEST_SAVE_DIR);
        final QuestController qc = FModel.getQuest();

        // Iterate over files and load quest data for each.
        final FilenameFilter takeDatFiles = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".dat");
            }
        };
        final File[] arrFiles = dirQuests.listFiles(takeDatFiles);
        arrQuests.clear();
        for (final File f : arrFiles) {
            arrQuests.put(f.getName(), QuestDataIO.loadData(f));
        }

        // Populate list with available quest data.
        view.getLstQuests().setQuests(new ArrayList<QuestData>(arrQuests.values()));

        // If there are quests available, force select.
        if (arrQuests.size() > 0) {
            final String questname = FModel.getQuestPreferences().getPref(QPref.CURRENT_QUEST);

            // Attempt to select previous quest.
            if (arrQuests.get(questname) != null) {
                view.getLstQuests().setSelectedQuestData(arrQuests.get(questname));
            }
            else {
                view.getLstQuests().setSelectedIndex(0);
            }

            // Drop into AllZone.
            qc.load(view.getLstQuests().getSelectedQuest());
        }
        else {
            qc.load(null);
        }

        view.getLstQuests().setSelectCommand(cmdQuestSelect);
        view.getLstQuests().setDeleteCommand(cmdQuestUpdate);
        view.getLstQuests().setEditCommand(cmdQuestUpdate);

        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.getBtnEmbark().requestFocusInWindow(); }
        });
    }

    /**
     * The actuator for new quests.
     */
    private void newQuest() {
        final VSubmenuQuestData view = VSubmenuQuestData.SINGLETON_INSTANCE;
        final int difficulty = view.getSelectedDifficulty();

        final QuestMode mode = view.isFantasy() ? QuestMode.Fantasy : QuestMode.Classic;

        Deck dckStartPool = null;
        GameFormat fmtStartPool = null;
        final QuestWorld startWorld = FModel.getWorlds().get(view.getStartingWorldName());

        final GameFormat worldFormat = (startWorld == null ? null : startWorld.getFormat());

        if (worldFormat == null) {
            switch(view.getStartingPoolType()) {
            case Rotating:
                fmtStartPool = view.getRotatingFormat();
                break;

            case CustomFormat:
                if (customFormatCodes.isEmpty()) {
                    if (!FOptionPane.showConfirmDialog("You have defined a custom format that doesn't contain any sets.\nThis will start a game without restriction.\n\nContinue?")) {
                        return;
                    }
                }
                fmtStartPool = customFormatCodes.isEmpty() ? null : new GameFormatQuest("Custom", customFormatCodes, null); // chosen sets and no banend cards
                break;

            case DraftDeck:
            case SealedDeck:
            case Cube:
                dckStartPool = view.getSelectedDeck();
                if (null == dckStartPool) {
                    FOptionPane.showMessageDialog("You have not selected a deck to start.", "Cannot start a quest", FOptionPane.ERROR_ICON);
                    return;
                }
                break;

            case Precon:
                dckStartPool = QuestController.getPrecons().get(view.getSelectedPrecon()).getDeck();
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
        final StartingPoolType prizedPoolType = view.getPrizedPoolType();
        if (null == prizedPoolType) {
            fmtPrizes = fmtStartPool;
            if (null == fmtPrizes && dckStartPool != null) { // build it form deck
                final Set<String> sets = new HashSet<String>();
                for (final Entry<PaperCard, Integer> c : dckStartPool.getMain()) {
                    sets.add(c.getKey().getEdition());
                }
                if (dckStartPool.has(DeckSection.Sideboard)) {
                    for (final Entry<PaperCard, Integer> c : dckStartPool.get(DeckSection.Sideboard)) {
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
                    if (!FOptionPane.showConfirmDialog("You have defined custom format as containing no sets.\nThis will choose all editions without restriction as prized.\n\nContinue?")) {
                        return;
                    }
                }
                fmtPrizes = customPrizeFormatCodes.isEmpty() ? null : new GameFormat("Custom Prizes", customPrizeFormatCodes, null); // chosen sets and no banend cards
                break;
            case Rotating:
                fmtPrizes = view.getPrizedRotatingFormat();
                break;
            default:
                throw new RuntimeException("Should not get this result");
            }
        }
        // } else {
        //    fmtPrizes = worldFormat;
        // }
        final StartingPoolPreferences userPrefs = new StartingPoolPreferences(view.randomizeColorDistribution(), view.getPreferredColor(), view.startWithCompleteSet());

        String questName;
        while (true) {
            questName = FOptionPane.showInputDialog("Poets will remember your quest as:", "Quest Name");
            if (questName == null) { return; }

            questName = QuestUtil.cleanString(questName);

            if (questName.isEmpty()) {
                FOptionPane.showMessageDialog("Please specify a quest name.");
                continue;
            }
            if (getAllQuests().get(questName + ".dat") != null) {
                FOptionPane.showMessageDialog("A quest already exists with that name. Please pick another quest name.");
                continue;
            }
            break;
        }

        final QuestController qc = FModel.getQuest();

        qc.newGame(questName, difficulty, mode, fmtPrizes, view.isUnlockSetsAllowed(), dckStartPool, fmtStartPool, view.getStartingWorldName(), userPrefs);
        FModel.getQuest().save();

        // Save in preferences.
        FModel.getQuestPreferences().setPref(QPref.CURRENT_QUEST, questName + ".dat");
        FModel.getQuestPreferences().save();

        update();
    }   // New Quest

    /** Changes between quest data files. */
    private void changeQuest() {
        FModel.getQuest().load(VSubmenuQuestData.SINGLETON_INSTANCE
                .getLstQuests().getSelectedQuest());

        // Save in preferences.
        FModel.getQuestPreferences().setPref(QPref.CURRENT_QUEST,
                FModel.getQuest().getName() + ".dat");
        FModel.getQuestPreferences().save();

        //SSubmenuQuestUtil.updateQuestInfo();

        CSubmenuDuels.SINGLETON_INSTANCE.update();
        CSubmenuChallenges.SINGLETON_INSTANCE.update();
        CSubmenuQuestDecks.SINGLETON_INSTANCE.update();
        CSubmenuQuestDraft.SINGLETON_INSTANCE.update();
    }

    /** @return  */
    private Map<String, QuestData> getAllQuests() {
        return arrQuests;
    }

}
