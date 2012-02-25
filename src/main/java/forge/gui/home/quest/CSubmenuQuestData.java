package forge.gui.home.quest;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.Command;
import forge.Singletons;
import forge.gui.GuiUtils;
import forge.gui.home.ICSubmenu;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestData;
import forge.quest.data.QuestDataIO;
import forge.quest.data.QuestPreferences.QPref;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum CSubmenuQuestData implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    private final Map<String, QuestData> arrQuests = new HashMap<String, QuestData>();

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        VSubmenuQuestData.SINGLETON_INSTANCE.populate();
        CSubmenuQuestData.SINGLETON_INSTANCE.update();

        VSubmenuQuestData.SINGLETON_INSTANCE.getBtnEmbark().setCommand(
                new Command() { @Override public void execute() { newQuest(); } });
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getCommand()
     */
    @Override
    public Command getMenuCommand() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        refreshQuests();
    }

    /**
     * The actuator for new quests.
     */
    private void newQuest() {
        final VSubmenuQuestData view = VSubmenuQuestData.SINGLETON_INSTANCE;
        int difficulty = 0;
        QuestData newdata = new QuestData();

        final String mode = view.getRadFantasy().isSelected()
                ? forge.quest.data.QuestData.FANTASY
                : forge.quest.data.QuestData.CLASSIC;

        if (view.getRadEasy().isSelected()) {
            difficulty = 0;
        } else if (view.getRadMedium().isSelected()) {
            difficulty = 1;
        } else if (view.getRadHard().isSelected()) {
            difficulty = 2;
        } else if (view.getRadExpert().isSelected()) {
            difficulty = 3;
        } else {
            throw new IllegalStateException(
                    "ControlQuest() > newQuest(): Error starting new quest!");
        }

        final Object o = JOptionPane.showInputDialog(null, "Poets will remember your quest as:", "Quest Name", JOptionPane.OK_CANCEL_OPTION);

        if (o == null) { return; }

        final String questName = GuiUtils.cleanString(o.toString());

        if (getAllQuests().get(questName) != null || questName.equals("")) {
            JOptionPane.showMessageDialog(null, "Please pick another quest name, a quest already has that name.");
            return;
        }

        // Give the user a few cards to build a deck
        newdata.newGame(difficulty, mode, view.getCbStandardStart().isSelected());
        newdata.setName(questName);
        newdata.saveData();

        // Save in preferences.
        Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_QUEST, questName + ".dat");
        Singletons.getModel().getQuestPreferences().save();

        Singletons.getView().getViewHome().resetQuest();
    }   // New Quest

    /** Changes between quest data files. */
    private void changeQuest() {
        AllZone.setQuestData(VSubmenuQuestData.SINGLETON_INSTANCE
                .getLstQuests().getSelectedQuest());

        // Save in preferences.
        Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_QUEST,
                AllZone.getQuestData().getName() + ".dat");
        Singletons.getModel().getQuestPreferences().save();

        //refreshDecks();
        //refreshStats();
    }

    /** Resets quests, then retrieves and sets current quest. */
    public void refreshQuests() {
        final VSubmenuQuestData view = VSubmenuQuestData.SINGLETON_INSTANCE;
        File dirQuests = ForgeProps.getFile(NewConstants.Quest.DATA_DIR);

        // Temporary transition code between v1.2.2 and v1.2.3.
        // Can be safely deleted after release of 1.2.3.
        if (!dirQuests.exists()) {
            dirQuests.mkdirs();
        }
        File olddata = new File("res/quest/questData.dat");
        File newpath = new File(dirQuests.getPath() + "/questData.dat");

        if (olddata.exists()) { olddata.renameTo(newpath); }
        // end block which can be deleted

        // Iterate over files and load quest datas for each.
        FilenameFilter takeDatFiles = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".dat");
            }
        };
        File[] arrFiles = dirQuests.listFiles(takeDatFiles);
        arrQuests.clear();
        for (File f : arrFiles) {
            arrQuests.put(f.getName(), QuestDataIO.loadData(f));
        }

        // Populate list with available quest datas.
        view.getLstQuests().setQuests(arrQuests.values().toArray(new QuestData[0]));

        // If there are quests available, force select.
        if (arrQuests.size() > 0) {
            final String questname = Singletons.getModel().getQuestPreferences()
                    .getPreference(QPref.CURRENT_QUEST);

            // Attempt to select previous quest.
            if (arrQuests.get(questname) != null) {
                view.getLstQuests().setSelectedQuestData(arrQuests.get(questname));
            }
            else {
                view.getLstQuests().setSelectedIndex(0);
            }

            // Drop into AllZone.
            AllZone.setQuestData(view.getLstQuests().getSelectedQuest());
        }
        else {
            AllZone.setQuestData(null);
        }
    }

    /** @return  */
    private Map<String, QuestData> getAllQuests() {
        return arrQuests;
    }
}
