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
package forge;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

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
     * @return an array of {@link forge.GuiDownloader.DownloadObject} objects.
     */
    @Override
    protected final DownloadObject[] getNeededImages() {
        // read token names and urls
        final DownloadObject[] cardTokenLQ = GuiDownloader.readFileWithNames(NewConstants.TOKEN_IMAGES,
                ForgeProps.getFile(NewConstants.IMAGE_TOKEN));
        final ArrayList<DownloadObject> cList = new ArrayList<DownloadObject>();

        final String base = ForgeProps.getFile(NewConstants.IMAGE_BASE).getPath();
        for (final Card c : AllZone.getCardFactory()) {
            cList.addAll(this.createDLObjects(c, base));
        }

        final ArrayList<DownloadObject> list = new ArrayList<DownloadObject>();
        File file;

        final DownloadObject[] a = { new DownloadObject("", "", "") };
        final DownloadObject[] cardPlay = cList.toArray(a);
        // check to see which cards we already have
        for (final DownloadObject element : cardPlay) {
            file = new File(base, element.getName());
            if (!file.exists()) {
                list.add(element);
            }
        }

        // add missing tokens to the list of things to download
        final File filebase = ForgeProps.getFile(NewConstants.IMAGE_TOKEN);
        for (final DownloadObject element : cardTokenLQ) {
            file = new File(filebase, element.getName());
            if (!file.exists()) {
                list.add(element);
            }
        }

        // return all card names and urls that are needed
        final DownloadObject[] out = new DownloadObject[list.size()];
        list.toArray(out);

        return out;
    } // getNeededImages()

    private List<DownloadObject> createDLObjects(final Card c, final String base) {
        final ArrayList<DownloadObject> ret = new ArrayList<DownloadObject>();

        for (final String sVar : c.getSVars().keySet()) {

            if (!sVar.startsWith("Picture")) {
                continue;
            }

            final String url = c.getSVar(sVar);
            final String[] urls = url.split("\\\\");

            final String iName = GuiDisplayUtil.cleanString(c.getImageName());
            ret.add(new DownloadObject(iName + ".jpg", urls[0], base));

            if (urls.length > 1) {
                for (int j = 1; j < urls.length; j++) {
                    ret.add(new DownloadObject(iName + j + ".jpg", urls[j], base));
                }
            }
        }

        return ret;
    }

}
