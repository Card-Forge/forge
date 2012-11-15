package forge.gui.home.quest;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import com.sun.mail.iap.Argument;

import forge.Command;
import forge.deck.Deck;
import forge.Singletons;
import forge.game.GameFormat;
import forge.gui.framework.ICDoc;
import forge.item.CardPrinted;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.QuestController;
import forge.quest.QuestMode;
import forge.quest.StartingPoolType;
import forge.quest.data.GameFormatQuest;
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
    private final List<String> customFormatCodes = new ArrayList<String>();
    private final List<String> customPrizeFormatCodes = new ArrayList<String>();
    
    private final Command cmdQuestSelect = new Command() { @Override
        public void execute() { changeQuest(); } };

    private final Command cmdQuestDelete = new Command() { @Override
        public void execute() { update(); } };

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void initialize() {
        view.getBtnEmbark().setCommand(
                new Command() { @Override public void execute() { newQuest(); } });

        view.getBtnCustomFormat().setCommand( new Command() { @Override public void execute() { 
            new DialogCustomFormat(customFormatCodes);
        }});
        
        view.getBtnPrizeCustomFormat().setCommand( new Command() { @Override public void execute() { 
            new DialogCustomFormat(customPrizeFormatCodes);
        }});        
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
        int difficulty = view.getSelectedDifficulty();

        final QuestMode mode = view.isFantasy() ? QuestMode.Fantasy : QuestMode.Classic;

        Deck dckStartPool = null;
        GameFormat fmtStartPool = null;
        switch(view.getStartingPoolType()) {
            case Rotating:
                fmtStartPool = view.getRotatingFormat();
                break;
        
            case CustomFormat:
                if ( customFormatCodes.isEmpty() )
                {
                    int answer = JOptionPane.showConfirmDialog(null, "You have defined custom format as containing no sets.\nThis will start a game without restriction.\n\nContinue?");
                    if ( JOptionPane.YES_OPTION != answer )
                        return;
                }
                fmtStartPool = customFormatCodes.isEmpty() ? null : new GameFormatQuest("Custom", customFormatCodes, null); // chosen sets and no banend cards
                break;

            case SealedDeck:
                dckStartPool = view.getSelectedDeck();
                if ( null == dckStartPool )
                {
                    JOptionPane.showMessageDialog(null, "You have not selected a deck to start", "Cannot start a quest", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                break;
                
            case DraftDeck:
                dckStartPool = view.getSelectedDeck();
                if ( null == dckStartPool )
                {
                    JOptionPane.showMessageDialog(null, "You have not selected a deck to start", "Cannot start a quest", JOptionPane.ERROR_MESSAGE);
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

        GameFormat fmtPrizes = null;
        StartingPoolType prizedPoolType = view.getPrizedPoolType();
        if ( null == prizedPoolType ) {
            fmtPrizes = fmtStartPool;
            if ( null == fmtPrizes && dckStartPool != null) { // build it form deck
                List<String> sets = new ArrayList<String>();
                for(Entry<CardPrinted, Integer> c : dckStartPool.getMain()) {
                    String edition = c.getKey().getEdition();
                    if ( !sets.contains(edition) )
                        sets.add(edition);
                }
                for(Entry<CardPrinted, Integer> c : dckStartPool.getSideboard()) {
                    String edition = c.getKey().getEdition();
                    if ( !sets.contains(edition) )
                        sets.add(edition);
                }
                fmtPrizes = new GameFormat("From deck", sets, null);
            }
        } else
            switch(prizedPoolType) {
                case Complete:
                    fmtPrizes = null;
                    break;
                case CustomFormat:
                    if ( customPrizeFormatCodes.isEmpty() )
                    {
                        int answer = JOptionPane.showConfirmDialog(null, "You have defined custom format as containing no sets.\nThis will choose all editions without restriction as prized.\n\nContinue?");
                        if ( JOptionPane.YES_OPTION != answer )
                            return;
                    }
                    fmtPrizes = customPrizeFormatCodes.isEmpty() ? null : new GameFormat("Custom Prizes", customPrizeFormatCodes, null); // chosen sets and no banend cards
                    break;
                case Rotating:
                    fmtPrizes = view.getPrizedRotatingFormat();
                    break;
                default: 
                    throw new RuntimeException("Should not get this result");
            }
            

        final Object o = JOptionPane.showInputDialog(null, "Poets will remember your quest as:", "Quest Name", JOptionPane.OK_CANCEL_OPTION);
        if (o == null) { return; }

        final String questName = SSubmenuQuestUtil.cleanString(o.toString());

        if (getAllQuests().get(questName) != null || questName.equals("")) {
            JOptionPane.showMessageDialog(null, "Please pick another quest name, a quest already has that name.");
            return;
        }



        
        QuestController qc = Singletons.getModel().getQuest(); 
        
        qc.newGame(questName, difficulty, mode, fmtPrizes, view.isUnlockSetsAllowed(), dckStartPool, fmtStartPool);
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

        //SSubmenuQuestUtil.updateQuestInfo();

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
