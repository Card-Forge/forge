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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import forge.card.CardRules;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.properties.NewConstants;

@SuppressWarnings("serial")
public class GuiDownloadPicturesLQ extends GuiDownloader {
    public GuiDownloadPicturesLQ() {
        super();
    }

    @Override
    protected final ArrayList<DownloadObject> getNeededImages() {
        ArrayList<DownloadObject> downloads = new ArrayList<DownloadObject>();
        Set<String> filenames = new HashSet<String>();

        for (final CardPrinted c : CardDb.instance().getUniqueCards()) {
            CardRules cardRules = c.getRules();
            addDLObject(cardRules.getPictureUrl(), c.getImageFilename(), downloads, filenames);

            String backFaceImage = c.getBackFaceImageFilename();
            if (backFaceImage != null) {
                addDLObject(cardRules.getPictureOtherSideUrl(), backFaceImage, downloads, filenames);
            }
        }

        // Add missing tokens to the list of things to download.
        for (final DownloadObject element : GuiDownloader.readFileWithNames(NewConstants.IMAGE_LIST_TOKENS_FILE, NewConstants.CACHE_TOKEN_PICS_DIR)) {
            if (!element.getDestination().exists()) {
                downloads.add(element);
            }
        }

        return downloads;
    }

    private void addDLObject(String url, String filename, ArrayList<DownloadObject> downloads, Set<String> filenames) {
        if (StringUtils.isEmpty(url) || filenames.contains(filename)) {
            return;
        }
        filenames.add(filename);
        
        // remove set path prefix from card filename
        if (filename.contains("/")) {
            filename = filename.substring(filename.indexOf('/') + 1);
        }
        
        File destFile = new File(NewConstants.CACHE_CARD_PICS_DIR, filename + ".jpg");
        if (!destFile.exists()) {
            downloads.add(new DownloadObject(url, destFile));
        }
    }
}
