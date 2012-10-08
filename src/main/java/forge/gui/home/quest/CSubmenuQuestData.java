package forge.gui.home.quest;

import static forge.quest.QuestStartPool.Complete;
import static forge.quest.QuestStartPool.Precon;
import static forge.quest.QuestStartPool.Rotating;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import forge.Command;
import forge.Singletons;
import forge.gui.framework.ICDoc;
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
public enum CSubmenuQuestData implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final Map<String, QuestData> arrQuests = new HashMap<String, QuestData>();

    private final VSubmenuQuestData view = VSubmenuQuestData.SINGLETON_INSTANCE;

    private final Command cmdQuestSelect = new Command() { @Override
        public void execute() { changeQuest(); } };

    private final Command cmdQuestDelete = new Command() { @Override
        public void execute() { update(); } };

    private final ActionListener preconListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            view.getCbxFormat().setEnabled(view.getRadPreconStart().isSelected());
            view.getCbxPrecon().setEnabled(view.getRadRotatingStart().isSelected());
            view.getBoxPersist().setEnabled(view.getRadRotatingStart().isSelected());
        }
    };

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        view.getBtnEmbark().setCommand(
                new Command() { @Override public void execute() { newQuest(); } });

        view.getRadUnrestricted().addActionListener(preconListener);
        view.getRadRotatingStart().addActionListener(preconListener);
        view.getRadPreconStart().addActionListener(preconListener);
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        final VSubmenuQuestData view = VSubmenuQuestData.SINGLETON_INSTANCE;
        final File dirQuests = ForgeProps.getFile(NewConstants.Quest.DATA_DIR);
        final QuestController qc = Singletons.getModel().getQuest();

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

        final QuestMode mode = view.getBoxFantasy().isSelected() ? QuestMode.Fantasy : QuestMode.Classic;

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
        final String rotatingFormat = view.getFormat();
        if (view.getRadUnrestricted().isSelected()) {
            startPool = Complete;
        } else if (view.getRadRotatingStart().isSelected()) {
            startPool = Rotating;
        } else {
            startPool = Precon;
        }

        final Object o = JOptionPane.showInputDialog(null, "Poets will remember your quest as:", "Quest Name", JOptionPane.OK_CANCEL_OPTION);

        if (o == null) { return; }

        final String questName = SSubmenuQuestUtil.cleanString(o.toString());

        if (getAllQuests().get(questName) != null || questName.equals("")) {
            JOptionPane.showMessageDialog(null, "Please pick another quest name, a quest already has that name.");
            return;
        }

        // Give the user a few cards to build a deck
        Singletons.getModel().getQuest().newGame(questName, difficulty, mode, startPool, rotatingFormat, startPrecon, view.getBoxPersist().isSelected());
        Singletons.getModel().getQuest().save();

        // Save in preferences.
        Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_QUEST, questName + ".dat");
        Singletons.getModel().getQuestPreferences().save();

        update();
    }   // New Quest

    /** Changes between quest data files. */
    private void changeQuest() {
        Singletons.getModel().getQuest().load(VSubmenuQuestData.SINGLETON_INSTANCE
                .getLstQuests().getSelectedQuest());

        // Save in preferences.
        Singletons.getModel().getQuestPreferences().setPreference(QPref.CURRENT_QUEST,
                Singletons.getModel().getQuest().getName() + ".dat");
        Singletons.getModel().getQuestPreferences().save();

        SSubmenuQuestUtil.updateStatsAndPet();

        CSubmenuDuels.SINGLETON_INSTANCE.update();
        CSubmenuChallenges.SINGLETON_INSTANCE.update();
        CSubmenuQuestDecks.SINGLETON_INSTANCE.update();
    }

    /** @return  */
    private Map<String, QuestData> getAllQuests() {
        return arrQuests;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }
}
