/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2013  Forge Team
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

package forge.gui.toolbox.imaging;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

import forge.ImageCache;
import forge.Singletons;
import forge.card.CardCharacteristicName;
import forge.game.card.Card;
import forge.gui.toolbox.CardFaceSymbols;
import forge.properties.ForgePreferences;

/**
 * Common image-related routines specific to Forge images. 
 * 
 * @version $Id$
 * 
 */
public final class FImageUtil {  
    private FImageUtil() {}
    
    public static BufferedImage getImage(Card card, CardCharacteristicName state) {       
        BufferedImage image = ImageCache.getOriginalImage(card.getImageKey(state), true);
        int foilIndex = card.getFoil();
        if (image != null && foilIndex > 0) { 
            image = getImageWithFoilEffect(image, foilIndex);
        }
        return image;
    }
    
    /**
     * Gets the image associated with a card.
     * <p>
     * Adds a random foil effect if enabled.
     * <p>
     * For double-sided cards, returns the front-side image.<br>
     * For flip cards, returns the un-flipped image. 
     */
    public static BufferedImage getImage(Card card) {
        BufferedImage image = ImageCache.getOriginalImage(card.getImageKey(), true);
        int foilIndex = card.getFoil();
        if (image != null && foilIndex > 0) { 
            image = getImageWithFoilEffect(image, foilIndex);
        }
        return image;
    }
        
    /**
     * Applies a foil effect to a card image.
     */
    private static BufferedImage getImageWithFoilEffect(BufferedImage plainImage, int foilIndex) {
        if (!Singletons.getModel().getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_OVERLAY_FOIL_EFFECT)) {
            return plainImage;
        }

        ColorModel cm = plainImage.getColorModel();
        BufferedImage foilImage = new BufferedImage(cm, plainImage.copyData(null), cm.isAlphaPremultiplied(), null);
        final String fl = String.format("foil%02d", foilIndex);
        CardFaceSymbols.drawOther(foilImage.getGraphics(), fl, 0, 0, foilImage.getWidth(), foilImage.getHeight());
        return foilImage;                
    }
}
