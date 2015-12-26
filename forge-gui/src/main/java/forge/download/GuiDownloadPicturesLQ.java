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

import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.util.ImageUtil;

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
        String[] result = ImageUtil.getDownloadUrlAndDestination(ForgeConstants.CACHE_CARD_PICS_DIR, c, backFace);
        if (result == null) {
            return;
        }
        final String urlToDownload = result[0];
        final String destPath = result[1];
        if (downloads.containsKey(destPath)) {
            return;
        }
        downloads.put(destPath, urlToDownload);
    }
}
