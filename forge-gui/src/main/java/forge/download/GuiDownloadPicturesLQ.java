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
package forge.download;

import forge.card.CardRules;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.util.ImageUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

public class GuiDownloadPicturesLQ extends GuiDownloadService {
    @Override
    public String getTitle() {
        return "Download LQ Card Pictures";
    }

    @Override
    protected final Map<String, String> getNeededFiles() {
        final Map<String, String> downloads = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (final PaperCard c : FModel.getMagicDb().getCommonCards().getAllCards()) {
            addDLObject(c, downloads, false);
            if (ImageUtil.hasBackFacePicture(c)) {
                addDLObject(c, downloads, true);
            }
        }

        for (final PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
            addDLObject(c, downloads, false);
        }

        // Add missing tokens to the list of things to download.
        addMissingItems(downloads, ForgeConstants.IMAGE_LIST_TOKENS_FILE, ForgeConstants.CACHE_TOKEN_PICS_DIR);

        return downloads;
    }

    private static void addDLObject(final PaperCard c, final Map<String, String> downloads, final boolean backFace) {
        final CardRules cardRules = c.getRules();
        final String urls = cardRules.getPictureUrl(backFace);
        if (StringUtils.isEmpty(urls)) {
            return;
        }

        String filename = ImageUtil.getImageKey(c, backFace, false);
        final File destFile = new File(ForgeConstants.CACHE_CARD_PICS_DIR, filename + ".jpg");
        if (destFile.exists()) {
            return;
        }

        filename = destFile.getAbsolutePath();

        if (downloads.containsKey(filename)) {
            return;
        }

        final String urlToDownload;
        int urlIndex = 0;
        int allUrlsLen = 1;
        if (!urls.contains("\\")) {
            urlToDownload = urls;
        } else {
            final String[] allUrls = urls.split("\\\\");
            allUrlsLen = allUrls.length;
            urlIndex = (c.getArtIndex()-1) % allUrlsLen;
            urlToDownload = allUrls[urlIndex];
        }

        System.out.println(c.getName() + "|" + c.getEdition() + " - " + c.getArtIndex() + " -> " + urlIndex + "/" + allUrlsLen + " === " + filename + " <<< " + urlToDownload);
        downloads.put(destFile.getAbsolutePath(), urlToDownload);
    }
}
