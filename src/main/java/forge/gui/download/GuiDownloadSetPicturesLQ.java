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

import org.apache.commons.lang3.StringUtils;

import forge.CardUtil;
import forge.Singletons;
import forge.card.CardEdition;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.Base64Coder;

/**
 * <p>
 * Gui_DownloadSetPictures_LQ class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class GuiDownloadSetPicturesLQ extends GuiDownloader {

    private static final long serialVersionUID = -7890794857949935256L;
    private String picturesPath;

    /**
     * <p>
     * Constructor for Gui_DownloadSetPictures_LQ.
     * </p>
     * 
     * @param frame
     *            a {@link javax.swing.JFrame} object.
     */
    public GuiDownloadSetPicturesLQ(final JFrame frame) {
        super(frame);

    }

    /**
     * Adds the card to list.
     * 
     * @param cList
     *            the c list
     * @param c
     *            the c
     * @param cardName
     *            the card name
     */
    protected final void addCardToList(final ArrayList<DownloadObject> cList, final CardPrinted c, final String cardName) {
        final String urlBase = ForgeProps.getProperty(NewConstants.CARDFORGE_URL) + "/fpics/";

        final String setCode3 = c.getEdition();
        final CardEdition thisSet = Singletons.getModel().getEditions().get(setCode3);
        final String setCode2 = thisSet.getCode2();

        final String imgFN = CardUtil.buildFilename(c, cardName);
        final boolean foundSetImage = imgFN.contains(setCode3) || imgFN.contains(setCode2);

        if (!foundSetImage) {
            final int artsCnt = c.getCard().getEditionInfo(setCode3).getCopiesCount();
            final String filename = CardUtil.buildIdealFilename(cardName, c.getArtIndex(), artsCnt);
            String url = urlBase + setCode2 + "/" + Base64Coder.encodeString(filename, true);
            cList.add(new DownloadObject(url, new File(this.picturesPath + File.separator + setCode3, filename)));

            System.out.println(String.format("%s [%s - %s]", cardName, setCode3, thisSet.getName()));
        }
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
        if (this.picturesPath == null) {
            this.picturesPath = ForgeProps.getFile(NewConstants.IMAGE_BASE).getPath();
        }
        // read token names and urls
        final ArrayList<DownloadObject> cList = new ArrayList<DownloadObject>();

        for (final CardPrinted c : CardDb.instance().getAllCards()) {
            final String setCode3 = c.getEdition();
            if (StringUtils.isBlank(setCode3) || "???".equals(setCode3)) {
                continue; // we don't want cards from unknown sets
            }

            this.addCardToList(cList, c, c.getCard().getName());
            if (c.getCard().isDoubleFaced()) {
                this.addCardToList(cList, c, c.getCard().getSlavePart().getName());
            }
        }

        // add missing tokens to the list of things to download
        for (final DownloadObject element : GuiDownloader.readFileWithNames(NewConstants.TOKEN_IMAGES, ForgeProps.getFile(NewConstants.IMAGE_TOKEN))) {
            if (!element.getDestination().exists()) {
                cList.add(element);
            }
        }

        // return all card names and urls that are needed
        final DownloadObject[] out = new DownloadObject[cList.size()];
        cList.toArray(out);

        return out;
    } // getNeededImages()

} // end class Gui_DownloadSetPictures_LQ
