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

import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * GuiDownloadQuestImages class.
 * </p>
 * 
 * @author Forge
 */
public class GuiDownloadQuestImages extends GuiDownloader {

    private static final long serialVersionUID = -8596808503046590349L;

    /**
     * <p>
     * Constructor for GuiDownloadQuestImages.
     * </p>
     * 
     * @param frame
     *            a array of {@link javax.swing.JFrame} objects.
     */
    public GuiDownloadQuestImages(final JFrame frame) {
        super(frame);
    }

    /**
     * <p>
     * getNeededCards.
     * </p>
     * 
     * @return an array of {@link forge.GuiDownloadSetPicturesLQ} objects.
     */
    @Override
    protected final DownloadObject[] getNeededImages() {
        // read all card names and urls
        final DownloadObject[] questOpponents = GuiDownloader.readFile(NewConstants.Quest.OPPONENT_ICONS,
                ForgeProps.getFile(NewConstants.Quest.OPPONENT_DIR));
        final DownloadObject[] boosterImages = GuiDownloader.readFile(NewConstants.PICS_BOOSTER_IMAGES,
                ForgeProps.getFile(NewConstants.PICS_BOOSTER));
        final DownloadObject[] petIcons = GuiDownloader.readFileWithNames(NewConstants.Quest.PET_SHOP_ICONS,
                ForgeProps.getFile(NewConstants.IMAGE_ICON));
        final DownloadObject[] questPets = GuiDownloader.readFileWithNames(NewConstants.Quest.PET_TOKEN_IMAGES,
                ForgeProps.getFile(NewConstants.IMAGE_TOKEN));
        final ArrayList<DownloadObject> urls = new ArrayList<DownloadObject>();

        File file;
        File dir = ForgeProps.getFile(NewConstants.Quest.OPPONENT_DIR);
        for (final DownloadObject questOpponent : questOpponents) {
            file = new File(dir, questOpponent.getName().replace("%20", " "));
            if (!file.exists()) {
                urls.add(questOpponent);
            }
        }

        dir = ForgeProps.getFile(NewConstants.PICS_BOOSTER);
        for (final DownloadObject boosterImage : boosterImages) {
            file = new File(dir, boosterImage.getName().replace("%20", " "));
            if (!file.exists()) {
                urls.add(boosterImage);
            }
        }

        dir = ForgeProps.getFile(NewConstants.IMAGE_ICON);
        for (final DownloadObject petIcon : petIcons) {
            file = new File(dir, petIcon.getName().replace("%20", " "));
            if (!file.exists()) {
                urls.add(petIcon);
            }
        }

        dir = ForgeProps.getFile(NewConstants.IMAGE_TOKEN);
        for (final DownloadObject questPet : questPets) {
            file = new File(dir, questPet.getName().replace("%20", " "));
            if (!file.exists()) {
                urls.add(questPet);
            }
        }

        // return all card names and urls that are needed
        final DownloadObject[] out = new DownloadObject[urls.size()];
        urls.toArray(out);

        return out;
    } // getNeededCards()

} // end class GuiDownloadQuestImages
