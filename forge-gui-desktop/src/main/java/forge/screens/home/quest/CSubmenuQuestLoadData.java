package forge.screens.home.quest;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.data.QuestData;
import forge.gamemodes.quest.data.QuestPreferences.QPref;
import forge.gamemodes.quest.io.QuestDataIO;
import forge.gui.UiCommand;
import forge.gui.framework.ICDoc;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.screens.bazaar.CBazaarUI;

/**
 * Controls the quest data submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuQuestLoadData implements ICDoc {
    SINGLETON_INSTANCE;

    private final Map<String, QuestData> arrQuests = new HashMap<>();

    private final UiCommand cmdQuestSelect = new UiCommand() {
        @Override public void run() {
            changeQuest();
        }
    };
    private final UiCommand cmdQuestUpdate = new UiCommand() {
        @Override public void run() {
            update();
        }
    };

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
    }

    /* (non-Javadoc)
     * @see forge.gui.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {

        final VSubmenuQuestLoadData view = VSubmenuQuestLoadData.SINGLETON_INSTANCE;
        final File dirQuests = new File(ForgeConstants.QUEST_SAVE_DIR);
        final QuestController qc = FModel.getQuest();
        ArrayList<String> restorableQuests = new ArrayList<>();

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
            try {
                System.out.println(String.format("About to load quest (%s)... ", f.getName()));
                arrQuests.put(f.getName(), QuestDataIO.loadData(f));
            } catch(IOException ex) {
                ex.printStackTrace();
                System.out.println(String.format("Error loading quest data (%s).. skipping for now..", f.getName()));
                restorableQuests.add(f.getName());
            }
        }

        // Populate list with available quest data.
        view.getLstQuests().setQuests(new ArrayList<>(arrQuests.values()));

        // If there are quests available, force select.
        if (!arrQuests.isEmpty()) {
            final String questName = FModel.getQuestPreferences().getPref(QPref.CURRENT_QUEST);

            // Attempt to select previous quest.
            if (arrQuests.get(questName) != null) {
                view.getLstQuests().setSelectedQuestData(arrQuests.get(questName));
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

    }

    /** Changes between quest data files. */
    private void changeQuest() {

        FModel.getQuest().load(VSubmenuQuestLoadData.SINGLETON_INSTANCE.getLstQuests().getSelectedQuest());

        // Save in preferences.
        FModel.getQuestPreferences().setPref(QPref.CURRENT_QUEST, FModel.getQuest().getName() + ".dat");
        FModel.getQuestPreferences().save();

        CSubmenuDuels.SINGLETON_INSTANCE.update();
        CSubmenuChallenges.SINGLETON_INSTANCE.update();
        CSubmenuQuestDecks.SINGLETON_INSTANCE.update();
        CSubmenuQuestDraft.SINGLETON_INSTANCE.update();
	    CBazaarUI.SINGLETON_INSTANCE.update();

    }

    private Map<String, QuestData> getAllQuests() {
        return arrQuests;
    }

}
