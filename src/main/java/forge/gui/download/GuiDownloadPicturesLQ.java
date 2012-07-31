/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.gui.download;

import java.io.File;
import java.util.ArrayList;
import javax.swing.JFrame;

import forge.card.CardRules;
import forge.gui.GuiDisplayUtil;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * Gui_DownloadPictures_LQ class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GuiDownloadPicturesLQ extends GuiDownloader {

    private static final long serialVersionUID = -2839597792999139007L;
    private String baseFolder;
    private ArrayList<DownloadObject> downloads;
    
    public GuiDownloadPicturesLQ(final JFrame frame) {
        super(frame);

    }

    /**
     * <p>
     * getNeededCards.
     * </p>
     * 
     * @return an array of {@link forge.gui.download.GuiDownloader.DownloadObject} objects.
     */
    @Override
    protected final DownloadObject[] getNeededImages() {

        // This is called as a virtual method from constructor.
        baseFolder = ForgeProps.getFile(NewConstants.IMAGE_BASE).getPath();
        downloads = new ArrayList<DownloadObject>();

        // It is already needed here as initialized variable - that was the best place to initialize
        
        for (final CardPrinted c : CardDb.instance().getAllUniqueCards()) {
            System.out.println(c.getName());
            CardRules firstSide = c.getCard();
            this.createDLObjects(firstSide);
            
            CardRules secondSide = firstSide.getSlavePart();
            if( secondSide != null )
                this.createDLObjects(secondSide);
        }


        // add missing tokens to the list of things to download
        for (final DownloadObject element : GuiDownloader.readFileWithNames(NewConstants.TOKEN_IMAGES,
                ForgeProps.getFile(NewConstants.IMAGE_TOKEN))) {
            if (!element.getDestination().exists()) {
                downloads.add(element);
            }
        }

        // return all card names and urls that are needed
        return downloads.toArray(new DownloadObject[downloads.size()]);
    } // getNeededImages()

    private void createDLObjects(final CardRules c) {

        final String url = c.getPictureUrl();
        if (url != null && !url.isEmpty()) {
            final String[] urls = url.split("\\\\");

            final String sName = GuiDisplayUtil.cleanString(c.getName());
            addDownloadObject(urls[0], new File(baseFolder, sName + ".jpg"));
            
            for (int j = 1; j < urls.length; j++) {
                addDownloadObject(urls[j], new File(baseFolder, sName + j + ".jpg"));
            }
        }
    }
    
    private void addDownloadObject(String url, File destFile) {
        if (!destFile.exists()) {
            downloads.add(new DownloadObject(url, destFile));
        }
    }

}
