package forge.screens.home.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.deck.Deck;
import forge.game.GameFormat;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestMode;
import forge.gamemodes.quest.QuestUtil;
import forge.gamemodes.quest.QuestWorld;
import forge.gamemodes.quest.StartingPoolPreferences;
import forge.gamemodes.quest.StartingPoolType;
import forge.gamemodes.quest.data.DeckConstructionRules;
import forge.gamemodes.quest.data.GameFormatQuest;
import forge.gamemodes.quest.data.QuestData;
import forge.gamemodes.quest.data.QuestPreferences;
import forge.gui.UiCommand;
import forge.gui.framework.ICDoc;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.screens.home.CHomeUI;
import forge.toolbox.FOptionPane;
import forge.util.Localizer;

/**
 * Controls the quest data submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuQuestStart implements ICDoc {
    SINGLETON_INSTANCE;

    private final Map<String, QuestData> arrQuests = new HashMap<>();

    private final VSubmenuQuestStart view = VSubmenuQuestStart.SINGLETON_INSTANCE;
    private final List<String> customFormatCodes = new ArrayList<>();
    private final List<String> customPrizeFormatCodes = new ArrayList<>();

    private List<Byte> preferredColors = new ArrayList<>();
    private StartingPoolPreferences.PoolType poolType = StartingPoolPreferences.PoolType.BALANCED;
    private boolean includeArtifacts = true;
    private int numberOfBoosters = 0;

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        view.getBtnEmbark().setCommand(
                new UiCommand() { @Override public void run() { newQuest(); } });

        // disable the very powerful sets -- they can be unlocked later for a high price
        final List<String> unselectableSets = new ArrayList<>();
        unselectableSets.add("LEA");
        unselectableSets.add("LEB");
        unselectableSets.add("MBP");
        unselectableSets.add("VAN");
        unselectableSets.add("ARC");
        unselectableSets.add("PC2");

        view.getBtnCustomFormat().setCommand(new UiCommand() {
            @Override
            public void run() {
                final DialogChooseSets dialog = new DialogChooseSets(customFormatCodes, unselectableSets, false);
                dialog.setOkCallback(new Runnable() {
                    @Override
                    public void run() {
                        customFormatCodes.clear();
                        customFormatCodes.addAll(dialog.getSelectedSets());
                    }
                });
            }
        });

        view.getBtnSelectFormat().setCommand(new UiCommand() {
            @Override
            public void run() {
                final DialogChooseFormats dialog = new DialogChooseFormats();
                dialog.setOkCallback(new Runnable() {
                    @Override
                    public void run() {
                        customFormatCodes.clear();
                        Set<String> sets = new HashSet<>();
                        for(GameFormat format:dialog.getSelectedFormats()){
                            sets.addAll(format.getAllowedSetCodes());
                        }
                        customFormatCodes.addAll(sets);
                    }
                });
            }
        });

        view.getBtnPrizeCustomFormat().setCommand(new UiCommand() {
            @Override
            public void run() {
                final DialogChooseSets dialog = new DialogChooseSets(customPrizeFormatCodes, unselectableSets, false);
                dialog.setOkCallback(new Runnable() {
                    @Override
                    public void run() {
                        customPrizeFormatCodes.clear();
                        customPrizeFormatCodes.addAll(dialog.getSelectedSets());
                    }
                });
            }
        });

        view.getBtnPrizeSelectFormat().setCommand(new UiCommand() {
            @Override
            public void run() {
                final DialogChooseFormats dialog = new DialogChooseFormats();
                dialog.setOkCallback(new Runnable() {
                    @Override
                    public void run() {
                        customPrizeFormatCodes.clear();
                        Set<String> sets = new HashSet<>();
                        for(GameFormat format:dialog.getSelectedFormats()){
                            sets.addAll(format.getAllowedSetCodes());
                        }
                        customPrizeFormatCodes.addAll(sets);
                    }
                });
            }
        });

        view.getBtnPreferredColors().setCommand(new UiCommand() {
            @Override
            public void run() {
                final DialogChoosePoolDistribution colorChooser = new DialogChoosePoolDistribution(preferredColors, poolType, includeArtifacts);
                colorChooser.show(new UiCommand() {
                    @Override
                    public void run() {
                        preferredColors = colorChooser.getPreferredColors();
                        poolType = colorChooser.getPoolType();
                        includeArtifacts = colorChooser.includeArtifacts();
                        numberOfBoosters = colorChooser.getNumberOfBoosters();
                    }
                });
            }
        });

    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
    }

    /**
     * The actuator for new quests.
     */
    private void newQuest() {
        final Localizer localizer = Localizer.getInstance();
        final VSubmenuQuestStart view = VSubmenuQuestStart.SINGLETON_INSTANCE;
        final int difficulty = view.getSelectedDifficulty();

        final QuestMode mode = view.isFantasy() ? QuestMode.Fantasy : QuestMode.Classic;

        Deck dckStartPool = null;
        GameFormat fmtStartPool = null;
        final QuestWorld startWorld = FModel.getWorlds().get(view.getStartingWorldName());

        final GameFormat worldFormat = (startWorld == null ? null : startWorld.getFormat());

        if (worldFormat == null) {
            switch(view.getStartingPoolType()) {
                case Sanctioned:
                    fmtStartPool = view.getRotatingFormat();
                    break;

                case Casual:
                case CustomFormat:
                    if (customFormatCodes.isEmpty()) {
                        if (!FOptionPane.showConfirmDialog(localizer.getMessage("lblNotFormatDefined"))) {
                            return;
                        }
                    }
                    fmtStartPool = customFormatCodes.isEmpty() ? null : new GameFormatQuest("Custom", customFormatCodes, null); // chosen sets and no banned cards
                    break;

                case DraftDeck:
                case SealedDeck:
                case Cube:
                    dckStartPool = view.getSelectedDeck();
                    if (null == dckStartPool) {
                        FOptionPane.showMessageDialog(localizer.getMessage("lbldckStartPool"), localizer.getMessage("lblCannotStartaQuest"), FOptionPane.ERROR_ICON);
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

        GameFormat fmtPrizes;

        final StartingPoolType prizedPoolType = view.getPrizedPoolType();
        if (null == prizedPoolType) {
            fmtPrizes = fmtStartPool;
            if (null == fmtPrizes && dckStartPool != null) { // build it form deck
                final Set<String> sets = new HashSet<>();

                for(Map.Entry<PaperCard, Integer> entry : dckStartPool.getAllCardsInASinglePool()) {
                    sets.add(entry.getKey().getEdition());
                }
                fmtPrizes = new GameFormat(localizer.getMessage("lblFromDeck"), sets, null);
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
                        if (!FOptionPane.showConfirmDialog(localizer.getMessage("lblNotFormatDefined"))) {
                            return;
                        }
                    }
                    fmtPrizes = customPrizeFormatCodes.isEmpty() ? null : new GameFormat("Custom Prizes", customPrizeFormatCodes, null); // chosen sets and no banned cards
                    break;
                case Sanctioned:
                    fmtPrizes = view.getPrizedRotatingFormat();
                    break;
                default:
                    throw new RuntimeException("Should not get this result");
            }
        }

        final StartingPoolPreferences userPrefs = new StartingPoolPreferences(poolType, preferredColors, includeArtifacts, view.startWithCompleteSet(), view.allowDuplicateCards(), numberOfBoosters);

        String questName;
        while (true) {
            questName = FOptionPane.showInputDialog(localizer.getMessage("MsgQuestNewName") + ":",  localizer.getMessage("TitQuestNewName"));
            if (questName == null) { return; }

            questName = QuestUtil.cleanString(questName);

            if (questName.isEmpty()) {
                FOptionPane.showMessageDialog(localizer.getMessage("lblQuestNameEmpty"));
                continue;
            }
            if (getAllQuests().get(questName + ".dat") != null) {
                FOptionPane.showMessageDialog(localizer.getMessage("lblQuestExists"));
                continue;
            }
            break;
        }

        //Apply the appropriate deck construction rules for this quest
        DeckConstructionRules dcr = DeckConstructionRules.Default;

        if(VSubmenuQuestStart.SINGLETON_INSTANCE.isCommander()){
            dcr = DeckConstructionRules.Commander;
        }

        final QuestController qc = FModel.getQuest();

        qc.newGame(questName, difficulty, mode, fmtPrizes, view.isUnlockSetsAllowed(), dckStartPool, fmtStartPool, view.getStartingWorldName(), userPrefs, dcr);
        FModel.getQuest().save();

        // Save in preferences.
        FModel.getQuestPreferences().setPref(QuestPreferences.QPref.CURRENT_QUEST, questName + ".dat");
        FModel.getQuestPreferences().save();

        // Change to QuestDecks screen
        CHomeUI.SINGLETON_INSTANCE.itemClick(VSubmenuQuestDecks.SINGLETON_INSTANCE.getDocumentID());
    }

    private Map<String, QuestData> getAllQuests() {
        return arrQuests;
    }

}
