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

import javax.swing.JFrame;

import org.apache.commons.lang3.StringUtils;

import forge.card.CardSet;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

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
        final String urlBase = "http://cardforge.org/fpics/";

        final String setCode3 = c.getSet();
        final CardSet thisSet = SetUtils.getSetByCode(setCode3);
        final String setCode2 = thisSet.getCode2();

        final String imgFN = CardUtil.buildFilename(c, cardName);
        final boolean foundSetImage = imgFN.contains(setCode3) || imgFN.contains(setCode2);

        if (this.picturesPath == null) {
            System.out.println("Oh snap!");
        }
        if (!foundSetImage) {
            final int artsCnt = c.getCard().getSetInfo(setCode3).getCopiesCount();
            final String fn = CardUtil.buildIdealFilename(cardName, c.getArtIndex(), artsCnt);
            cList.add(new DownloadObject(fn, urlBase + setCode2 + "/" + Base64Coder.encodeString(fn, true),
                    this.picturesPath + File.separator + setCode3));
            System.out.println(String.format("%s [%s - %s]", cardName, setCode3, thisSet.getName()));
        }
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
        if (this.picturesPath == null) {
            this.picturesPath = ForgeProps.getFile(NewConstants.IMAGE_BASE).getPath();
        }
        // read token names and urls
        final DownloadObject[] cardTokenLQ = GuiDownloader.readFileWithNames(NewConstants.TOKEN_IMAGES,
                ForgeProps.getFile(NewConstants.IMAGE_TOKEN));
        final ArrayList<DownloadObject> cList = new ArrayList<DownloadObject>();

        for (final CardPrinted c : CardDb.instance().getAllCards()) {
            final String setCode3 = c.getSet();
            if (StringUtils.isBlank(setCode3) || "???".equals(setCode3)) {
                continue; // we don't want cards from unknown sets
            }

            this.addCardToList(cList, c, c.getCard().getName());
            if (c.getCard().isDoubleFaced()) {
                this.addCardToList(cList, c, c.getCard().getSlavePart().getName());
            }
        }

        // add missing tokens to the list of things to download
        File file;
        final File filebase = ForgeProps.getFile(NewConstants.IMAGE_TOKEN);
        for (final DownloadObject element : cardTokenLQ) {
            file = new File(filebase, element.getName());
            if (!file.exists()) {
                cList.add(element);
            }
        }

        // return all card names and urls that are needed
        final DownloadObject[] out = new DownloadObject[cList.size()];
        cList.toArray(out);

        return out;
    } // getNeededImages()

} // end class Gui_DownloadSetPictures_LQ
