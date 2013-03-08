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

import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.ICardCharacteristics;
import forge.gui.GuiDisplayUtil;
import forge.item.CardDb;
import forge.item.IPaperCard;
import forge.properties.NewConstants;

/**
 * <p>
 * Gui_DownloadPictures_LQ class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
@SuppressWarnings("serial")
public class GuiDownloadPicturesLQ extends GuiDownloader {
    private ArrayList<DownloadObject> downloads;

    /**
     * 
     * TODO: Write javadoc for this method.
     */
    public GuiDownloadPicturesLQ() {
        super();
    }

    /**
     * <p>
     * getNeededCards.
     * </p>
     * 
     * @return an array of {@link forge.gui.download.GuiDownloader.DownloadObject} objects.
     */
    @Override
    protected final ArrayList<DownloadObject> getNeededImages() {
        downloads = new ArrayList<DownloadObject>();

        for (final IPaperCard c : CardDb.instance().getUniqueCards()) {
            CardRules cardRules = c.getRules();
            if (cardRules != null && cardRules.getSplitType() == CardSplitType.Split && cardRules.getOtherPart() != null) {
                this.createDLObjects(cardRules.getPictureUrl(), String.format("%s%s", cardRules.getMainPart().getName(), cardRules.getOtherPart().getName()));
            } else {
                this.createDLObjects(cardRules.getPictureUrl(), cardRules.getMainPart().getName());
            }

            ICardCharacteristics secondSide = cardRules.getOtherPart();
            if (secondSide != null && cardRules.getSplitType() == CardSplitType.Transform) {
                this.createDLObjects(cardRules.getPictureOtherSideUrl(), secondSide.getName());
            }
        }

        // Add missing tokens to the list of things to download.
        for (final DownloadObject element : GuiDownloader.readFileWithNames(NewConstants.IMAGE_LIST_TOKENS_FILE, NewConstants.CACHE_TOKEN_PICS_DIR)) {
            if (!element.getDestination().exists()) {
                downloads.add(element);
            }
        }

        return downloads;
    } // getNeededImages()

    private void createDLObjects(final String url, final String cardName) {

        if (url != null && !url.isEmpty()) {
            final String[] urls = url.split("\\\\");

            final String sName = GuiDisplayUtil.cleanString(cardName);
            addDownloadObject(urls[0], new File(NewConstants.CACHE_CARD_PICS_DIR, sName + ".jpg"));

            for (int j = 1; j < urls.length; j++) {
                addDownloadObject(urls[j], new File(NewConstants.CACHE_CARD_PICS_DIR, sName + j + ".jpg"));
            }
        }
    }

    private void addDownloadObject(String url, File destFile) {
        if (!destFile.exists()) {
            downloads.add(new DownloadObject(url, destFile));
        }
    }
}
