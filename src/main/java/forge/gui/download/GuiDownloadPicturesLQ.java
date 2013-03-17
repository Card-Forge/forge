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
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import forge.ImageCache;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.properties.NewConstants;

@SuppressWarnings("serial")
public class GuiDownloadPicturesLQ extends GuiDownloader {
    public GuiDownloadPicturesLQ() {
        super();
    }

    @Override
    protected final Map<String, String> getNeededImages() {
        Map<String, String> downloads = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

        for (CardPrinted c : CardDb.instance().getAllCards()) {
            addDLObject(c, downloads, false);
            if ( c.getRules().getSplitType() == CardSplitType.Transform)
                addDLObject(c, downloads, true);
        }

        for (CardPrinted c : CardDb.variants().getAllCards()) {
            addDLObject(c, downloads, false);
        }
        
        // Add missing tokens to the list of things to download.
        addMissingItems(downloads, NewConstants.IMAGE_LIST_TOKENS_FILE, NewConstants.CACHE_TOKEN_PICS_DIR);

        return downloads;
    }

    private void addDLObject(CardPrinted c, Map<String, String> downloads, boolean backFace) {
        CardRules cardRules = c.getRules();
        String urls = backFace ? cardRules.getPictureOtherSideUrl() : cardRules.getPictureUrl();
        if (StringUtils.isEmpty(urls)) {
            return;
        }

        String filename = ImageCache.getImageKey(c, backFace, false);
        File destFile = new File(NewConstants.CACHE_CARD_PICS_DIR, filename + ".jpg");
        if (destFile.exists())
            return;
        
        filename = destFile.getAbsolutePath();
        
        if (downloads.containsKey(filename)) {
            return;
        }

        final String urlToDownload;
        int urlIndex = 0;
        int allUrlsLen = 1;
        if (urls.indexOf("\\\\") < 0)
            urlToDownload = urls;
        else {
            String[] allUrls = urls.split("\\\\");
            allUrlsLen = allUrls.length;
            urlIndex = c.getArtIndex() % allUrlsLen;
            urlToDownload = allUrls[urlIndex];
        }

        //System.out.println(c.getName() + "|" + c.getEdition() + " - " + c.getArtIndex() + " -> " + urlIndex + "/" + allUrlsLen + " === " + filename + " <<< " + urlToDownload);
        downloads.put(destFile.getAbsolutePath(), urlToDownload);
    }
}
