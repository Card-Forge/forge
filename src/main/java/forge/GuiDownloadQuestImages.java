package forge;


import forge.properties.ForgeProps;

import java.io.File;

import java.util.ArrayList;

import javax.swing.JFrame;


/**
 * <p>GuiDownloadQuestImages class.</p>
 *
 * @author Forge
 */
public class GuiDownloadQuestImages extends GuiDownloader {

    private static final long serialVersionUID = -8596808503046590349L;


    /**
     * <p>Constructor for GuiDownloadQuestImages.</p>
     *
     * @param frame a array of {@link javax.swing.JFrame} objects.
     */
    public GuiDownloadQuestImages(final JFrame frame) {
        super(frame);
    }

    /**
     * <p>getNeededCards.</p>
     *
     * @return an array of {@link forge.Gui_DownloadSetPictures_LQ.mCard} objects.
     */
    protected final DownloadObject[] getNeededImages() {
        //read all card names and urls
        DownloadObject[] questOpponents = readFile(QUEST.OPPONENT_ICONS, ForgeProps.getFile(QUEST.OPPONENT_DIR));
        DownloadObject[] boosterImages = readFile(PICS_BOOSTER_IMAGES, ForgeProps.getFile(PICS_BOOSTER));
        ArrayList<DownloadObject> urls = new ArrayList<DownloadObject>();

        File file;
        File dir = ForgeProps.getFile(QUEST.OPPONENT_DIR);
        for (int i = 0; i < questOpponents.length; i++) {
            file = new File(dir, questOpponents[i].name.replace("%20", " "));
            if (!file.exists()) {
                urls.add(questOpponents[i]);
            }
        }
        
        dir = ForgeProps.getFile(PICS_BOOSTER);
        for (int i = 0; i < boosterImages.length; i++) {
            file = new File(dir, boosterImages[i].name.replace("%20", " "));
            if (!file.exists()) {
                urls.add(boosterImages[i]);
            }
        }

        //return all card names and urls that are needed
        DownloadObject[] out = new DownloadObject[urls.size()];
        urls.toArray(out);

        return out;
    } //getNeededCards()

}  //end class GuiDownloadQuestImages
