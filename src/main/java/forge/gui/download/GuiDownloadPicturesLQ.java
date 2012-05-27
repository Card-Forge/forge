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
import java.util.List;

import javax.swing.JFrame;

import forge.AllZone;
import forge.Card;
import forge.CardCharactersticName;
import forge.card.CardCharacteristics;
import forge.gui.GuiDisplayUtil;
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

    /**
     * <p>
     * Constructor for GuiDownloadQuestImages.
     * </p>
     * 
     * @param frame
     *            a array of {@link javax.swing.JFrame} objects.
     */
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
        // read token names and urls
        final ArrayList<DownloadObject> cList = new ArrayList<DownloadObject>();

        final String base = ForgeProps.getFile(NewConstants.IMAGE_BASE).getPath();
        for (final Card c : AllZone.getCardFactory()) {
            cList.addAll(this.createDLObjects(c, base));
        }

        final ArrayList<DownloadObject> list = new ArrayList<DownloadObject>();

        for (final DownloadObject element : cList) {
            if (!element.getDestination().exists()) {
                list.add(element);
            }
        }

        // add missing tokens to the list of things to download
        for (final DownloadObject element : GuiDownloader.readFileWithNames(NewConstants.TOKEN_IMAGES,
                ForgeProps.getFile(NewConstants.IMAGE_TOKEN))) {
            if (!element.getDestination().exists()) {
                list.add(element);
            }
        }

        // return all card names and urls that are needed
        return list.toArray(new DownloadObject[list.size()]);
    } // getNeededImages()

    private List<DownloadObject> createDLObjects(final Card c, final String base) {
        final ArrayList<DownloadObject> ret = new ArrayList<DownloadObject>();

        for (final CardCharactersticName state : c.getStates()) {
            CardCharacteristics stateCharacteristics = c.getState(state);
            final String url = stateCharacteristics.getSVar("Picture");
            if (!url.isEmpty()) {
                final String[] urls = url.split("\\\\");

                final String iName = GuiDisplayUtil.cleanString(stateCharacteristics.getImageName());
                ret.add(new DownloadObject(urls[0], new File(base, iName + ".jpg")));

                for (int j = 1; j < urls.length; j++) {
                    ret.add(new DownloadObject(urls[j], new File(base, iName + j + ".jpg")));
                }
            }
        }

        return ret;
    }

}
