package forge.gui.home.quest;

import java.io.File;

import forge.AllZone;
import forge.Command;
import forge.Singletons;
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
public enum CSubmenuDuels implements ICSubmenu {
    /** */
    SINGLETON_INSTANCE;

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#getMenuCommand()
     */
    @Override
    public Command getMenuCommand() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#initialize()
     */
    @Override
    public void initialize() {
        VSubmenuDuels.SINGLETON_INSTANCE.populate();
        CSubmenuDuels.SINGLETON_INSTANCE.update();
    }

    /* (non-Javadoc)
     * @see forge.control.home.IControlSubmenu#update()
     */
    @Override
    public void update() {
        QuestData qData = AllZone.getQuestData();
        if (qData == null) {
            final String questname = Singletons.getModel()
                    .getQuestPreferences().getPreference(QPref.CURRENT_QUEST);

            qData = QuestDataIO.loadData(new File(
                    ForgeProps.getFile(NewConstants.Quest.DATA_DIR) + questname + ".dat"));
            System.out.println("asdf current quest data and credits: " + questname + " " + qData.getCredits());
        }

        //lblTitle.setText("Duels: " + control.getRankString());
    }
}
