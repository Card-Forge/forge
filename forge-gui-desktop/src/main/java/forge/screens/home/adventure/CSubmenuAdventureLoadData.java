package forge.screens.home.adventure;

import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.data.QuestData;
import forge.gamemodes.quest.io.QuestDataIO;
import forge.gui.UiCommand;
import forge.gui.framework.ICDoc;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Controls the adventure data submenu in the home UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
@SuppressWarnings("serial")
public enum CSubmenuAdventureLoadData implements ICDoc {
    SINGLETON_INSTANCE;

    private final Map<String, QuestData> arrAdventures = new HashMap<>();

    private final UiCommand cmdAdventureSelect = new UiCommand() {
        @Override public void run() {
            changeAdventure();
        }
    };
    private final UiCommand cmdAdventureUpdate = new UiCommand() {
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

        final VSubmenuAdventureLoadData view = VSubmenuAdventureLoadData.SINGLETON_INSTANCE;
        final File dirAdventures = new File(ForgeConstants.QUEST_SAVE_DIR);
        final QuestController qc = FModel.getQuest();
        ArrayList<String> restorableAdventures = new ArrayList<>();

        // Iterate over files and load adventure data for each.
        final FilenameFilter takeDatFiles = new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".dat");
            }
        };
        final File[] arrFiles = dirAdventures.listFiles(takeDatFiles);
        arrAdventures.clear();
        for (final File f : arrFiles) {
            try {
                System.out.println(String.format("About to load adventure (%s)... ", f.getName()));
                arrAdventures.put(f.getName(), QuestDataIO.loadData(f));
            } catch(IOException ex) {
                ex.printStackTrace();
                System.out.println(String.format("Error loading adventure data (%s).. skipping for now..", f.getName()));
                restorableAdventures.add(f.getName());
            }
        }


    }

    /** Changes between adventure data files. */
    private void changeAdventure() {


    }

    private Map<String, QuestData> getAllAdventures() {
        return arrAdventures;
    }

}
