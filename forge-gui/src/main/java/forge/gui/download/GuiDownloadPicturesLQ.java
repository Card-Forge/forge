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

import java.util.*;

import forge.ImageKeys;
import forge.StaticData;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.ImageUtil;

public class GuiDownloadPicturesLQ extends GuiDownloadService {
    final Map<String, String> downloads = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> existingSets;

    @Override
    public String getTitle() {
        return "Download LQ Card Pictures";
    }

    @Override
    protected final Map<String, String> getNeededFiles() {
        existingSets = retrieveManifestDirectory();

        for (final PaperCard c : FModel.getMagicDb().getCommonCards().getAllCards()) {
            addDLObject(c, "");
            if (c.hasBackFace()) {
                addDLObject(c, "back");
            }
        }

        for (final PaperCard c : FModel.getMagicDb().getVariantCards().getAllCards()) {
            addDLObject(c, "");
        }

        // Add missing tokens to the list of things to download.
        addMissingItems(downloads, ForgeConstants.IMAGE_LIST_TOKENS_FILE, ForgeConstants.CACHE_TOKEN_PICS_DIR);

        return downloads;
    }

    private void addDLObject(final PaperCard c, final String face) {
        final String imageKey = ImageUtil.getImageKey(c, face, false);
        final String destPath = ForgeConstants.CACHE_CARD_PICS_DIR + imageKey  + ".jpg";

        if (ImageKeys.getImageFile(imageKey) != null) {
            return;
        }

        if (downloads.containsKey(destPath)) {
            return;
        }

        final String setCode3 = c.getEdition();
        final String setCode2 = StaticData.instance().getEditions().getCode2ByCode(setCode3);

        if (!(existingSets.contains(setCode3) || existingSets.contains(setCode2))) {
            // If set doesn't exist on server, don't try to download cards for it
            return;
        }

        downloads.put(destPath, ForgeConstants.URL_PIC_DOWNLOAD + ImageUtil.getDownloadUrl(c, face));
    }
}
