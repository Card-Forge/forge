package forge;


import forge.properties.ForgeProps;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;


/**
 * <p>Gui_DownloadPictures_LQ class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Gui_DownloadPictures_LQ extends GuiDownloader {

    private static final long serialVersionUID = -2839597792999139007L;

    /**
     * <p>Constructor for GuiDownloadQuestImages.</p>
     *
     * @param frame a array of {@link javax.swing.JFrame} objects.
     */
    public Gui_DownloadPictures_LQ(final JFrame frame) {
        super(frame);
    }

    /**
     * <p>getNeededCards.</p>
     *
     * @return an array of {@link forge.GuiDownloader.DownloadObject} objects.
     */
    protected final DownloadObject[] getNeededImages() {
        //read token names and urls
        DownloadObject[] cardTokenLQ = readFileWithNames(TOKEN_IMAGES, ForgeProps.getFile(IMAGE_TOKEN));
        ArrayList<DownloadObject> cList = new ArrayList<DownloadObject>();

        String base = ForgeProps.getFile(IMAGE_BASE).getPath();
        for (Card c : AllZone.getCardFactory()) {
            cList.addAll(createDLObjects(c,base));
            if(c.hasAlternateState()) {
                c.changeState();
                cList.addAll(createDLObjects(c,base));
            }
        }

        ArrayList<DownloadObject> list = new ArrayList<DownloadObject>();
        File file;

        DownloadObject[] a = {new DownloadObject("", "", "")};
        DownloadObject[] cardPlay = cList.toArray(a);
        //check to see which cards we already have
        for (int i = 0; i < cardPlay.length; i++) {
            file = new File(base, cardPlay[i].name);
            if (!file.exists()) {
                list.add(cardPlay[i]);
            }
        }

        //add missing tokens to the list of things to download
        File filebase = ForgeProps.getFile(IMAGE_TOKEN);
        for (int i = 0; i < cardTokenLQ.length; i++) {
            file = new File(filebase, cardTokenLQ[i].name);
            if (!file.exists()) {
                list.add(cardTokenLQ[i]);
            }
        }

        //return all card names and urls that are needed
        DownloadObject[] out = new DownloadObject[list.size()];
        list.toArray(out);

        return out;
    } //getNeededImages()
    
    private List<DownloadObject> createDLObjects(final Card c,final String base) {
        ArrayList<DownloadObject> ret = new ArrayList<DownloadObject>();
        
        String url = c.getSVar("Picture");
        String[] URLs = url.split("\\\\");

        String iName = GuiDisplayUtil.cleanString(c.getImageName());
        ret.add(new DownloadObject(iName + ".jpg", URLs[0], base));

        if (URLs.length > 1) {
            for (int j = 1; j < URLs.length; j++) {
                ret.add(new DownloadObject(iName + j + ".jpg", URLs[j], base));
            }
        }
        
        return ret;
    }

}
