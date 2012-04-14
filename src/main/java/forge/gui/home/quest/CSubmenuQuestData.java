package forge.gui.home.quest;

import static forge.quest.QuestStartPool.Complete;
import static forge.quest.QuestStartPool.Precon;
import static forge.quest.QuestStartPool.Standard;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
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
import forge.quest.QuestController;
import forge.quest.QuestMode;
import forge.quest.QuestStartPool;
import forge.quest.data.QuestData;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.io.QuestDataIO;

/** 
 * Controls the quest data submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuQuestData implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    private final Map<String, QuestData> arrQuests = new HashMap<String, QuestData>();

    private final Command cmdQuestSelect = new Command() { @Override
        public void execute() { changeQuest(); } };

    private final Command cmdQuestDelete = new Command() { @Override
        public void execute() { update(); } };

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
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
        final VSubmenuQuestData view = VSubmenuQuestData.SINGLETON_INSTANCE;
        final File dirQuests = ForgeProps.getFile(NewConstants.Quest.DATA_DIR);
        final QuestController qc = AllZone.getQuest();

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
        view.getLstQuests().setQuests(new ArrayList<QuestData>(arrQuests.values()));

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
            qc.load(view.getLstQuests().getSelectedQuest());
        }
        else {
            qc.load(null);
        }

        view.getLstQuests().setSelectCommand(cmdQuestSelect);
        view.getLstQuests().setDeleteCommand(cmdQuestDelete);
    }

    /**
     * The actuator for new quests.
     */
    private void newQuest() {
        final VSubmenuQuestData view = VSubmenuQuestData.SINGLETON_INSTANCE;
        int difficulty = 0;

        final QuestMode mode = view.getRadFantasy().isSelected() ? QuestMode.Fantasy : QuestMode.Classic;

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

        final QuestStartPool startPool;
        final String startPrecon  = view.getPrecon();
        if (view.getRadCompleteStart().isSelected()) {
            startPool = Complete;
        } else if (view.getRadStandardStart().isSelected()) {
            startPool = Standard;
        } else {
            startPool = Precon;
        }

        final Object o = JOptionPane.showInputDialog(null, "Poets will remember your quest as:", "Quest Name", JOptionPane.OK_CANCEL_OPTION);

        if (o == null) { return; }

        final String questName = GuiUtils.cleanString(o.toString());

        if (getAllQuests().get(questName) != null || questName.equals("")) {
            JOptionPane.showMessageDialog(null, "Please pick another quest name, a quest already has that name.");
            return;
        }

        // Give the user a few cards to build a deck
        AllZone.getQuest().newGame(questName, difficulty, mode, startPool, startPrecon);
        AllZone.getQuest().save();

        // Save in preferences.
        Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_QUEST, questName + ".dat");
        Singletons.getModel().getQuestPreferences().save();

        update();
    }   // New Quest

    /** Changes between quest data files. */
    private void changeQuest() {
        AllZone.getQuest().load(VSubmenuQuestData.SINGLETON_INSTANCE
                .getLstQuests().getSelectedQuest());

        // Save in preferences.
        Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_QUEST,
                AllZone.getQuest().getName() + ".dat");
        Singletons.getModel().getQuestPreferences().save();

        SubmenuQuestUtil.updateStatsAndPet();

        CSubmenuDuels.SINGLETON_INSTANCE.update();
        CSubmenuChallenges.SINGLETON_INSTANCE.update();
        CSubmenuQuestDecks.SINGLETON_INSTANCE.update();
    }

    /** @return  */
    private Map<String, QuestData> getAllQuests() {
        return arrQuests;
    }
}
