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

import java.util.ArrayList;
import java.util.List;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/** */
@SuppressWarnings("serial")
public class GuiDownloadQuestImages extends GuiDownloader {
    /**
     * <p>
     * Constructor for GuiDownloadQuestImages.
     * </p>
     */
    public GuiDownloadQuestImages() {
        super();
    }

    /**
     * <p>
     * getNeededCards.
     * </p>
     * 
     * @return an array of {@link forge.gui.download.GuiDownloadSetPicturesLQ} objects.
     */
    @Override
    protected final DownloadObject[] getNeededImages() {
        // read all card names and urls
        final List<DownloadObject> urls = new ArrayList<DownloadObject>();

        for (final DownloadObject questOpponent : GuiDownloader.readFile(NewConstants.Quest.OPPONENT_ICONS, ForgeProps.getFile(NewConstants.Quest.OPPONENT_DIR))) {
            if (!questOpponent.getDestination().exists()) {
                urls.add(questOpponent);
            }
        }

        for (final DownloadObject boosterImage : GuiDownloader.readFile(NewConstants.PICS_BOOSTER_IMAGES, ForgeProps.getFile(NewConstants.IMAGE_SEALED_PRODUCT))) {
            if (!boosterImage.getDestination().exists()) {
                urls.add(boosterImage);
            }
        }

        for (final DownloadObject petIcon : GuiDownloader.readFileWithNames(NewConstants.Quest.PET_SHOP_ICONS, ForgeProps.getFile(NewConstants.IMAGE_ICON))) {
            if (!petIcon.getDestination().exists()) {
                urls.add(petIcon);
            }
        }

        for (final DownloadObject questPet : GuiDownloader.readFileWithNames(NewConstants.Quest.PET_TOKEN_IMAGES, ForgeProps.getFile(NewConstants.IMAGE_TOKEN))) {
            if (!questPet.getDestination().exists()) {
                urls.add(questPet);
            }
        }

        // return all card names and urls that are needed
        final DownloadObject[] out = new DownloadObject[urls.size()];
        urls.toArray(out);

        return out;
    } // getNeededCards()

} // end class GuiDownloadQuestImages
