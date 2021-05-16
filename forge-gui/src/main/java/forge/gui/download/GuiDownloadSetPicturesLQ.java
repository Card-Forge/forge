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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;

import forge.StaticData;
import forge.card.CardEdition;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.ImageUtil;
import forge.util.TextUtil;

public class GuiDownloadSetPicturesLQ extends GuiDownloadService {
    @Override
    public String getTitle() {
        return "Download LQ Set Pictures";
    }

    @Override
    protected final Map<String, String> getNeededFiles() {
        final Map<String, String> downloads = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        final Map<String, String> setMapping = new HashMap<>();
        final HashSet<String> existingSets = retrieveManifestDirectory();
        final HashSet<String> missingSets = new HashSet<>();

        for (final PaperCard c : Iterables.concat(FModel.getMagicDb().getCommonCards().getAllCards(), FModel.getMagicDb().getVariantCards().getAllCards())) {
            final String setCode3 = c.getEdition();

            if (!setMapping.containsKey(setCode3)) {
                setMapping.put(setCode3, StaticData.instance().getEditions().getCode2ByCode(setCode3));
            }
            final String setCode2 = setMapping.get(setCode3);

            if (StringUtils.isBlank(setCode3) || CardEdition.UNKNOWN.getCode().equals(setCode3)) {
                // we don't want cards from unknown sets
                continue;
            }

            if (!(existingSets.contains(setCode3) || existingSets.contains(setCode2))) {
                // If set doesn't exist on server, don't try to download cards for it
                if (!missingSets.contains(setCode3)) {
                    missingSets.add(setCode3);
                    System.out.println(setCode3 + " not found on server. Ignoring downloads for this set.");
                }

                continue;
            }

            addDLObject(ImageUtil.getDownloadUrl(c, false), ImageUtil.getImageKey(c, false, true), downloads);

            if (ImageUtil.hasBackFacePicture(c)) {
                addDLObject(ImageUtil.getDownloadUrl(c, true), ImageUtil.getImageKey(c, true, true), downloads);
            }
        }

        // Add missing tokens to the list of things to download.
        addMissingItems(downloads, ForgeConstants.IMAGE_LIST_TOKENS_FILE, ForgeConstants.CACHE_TOKEN_PICS_DIR);

        // TODO Add TokenScript images via Editions files?

        return downloads;
    }

    private static void addDLObject(final String urlPath, final String filename, final Map<String, String> downloads) {
        final File destFile = new File(ForgeConstants.CACHE_CARD_PICS_DIR, filename + ".jpg");
        String modifier = !filename.contains(".full") ? ".fullborder" : "";
        final File fullborder = new File(ForgeConstants.CACHE_CARD_PICS_DIR, TextUtil.fastReplace(filename, ".full", ".fullborder") + modifier + ".jpg");

        if (fullborder.exists())
            return; //don't add on download if you have an existing fullborder image in this set...

        if (!destFile.exists()) {
            downloads.put(destFile.getAbsolutePath(), ForgeConstants.URL_PIC_DOWNLOAD + urlPath);
        }
    }
}
