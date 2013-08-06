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

package forge.gui;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import forge.Card;
import forge.ImageCache;
import forge.gui.toolbox.CardFaceSymbols;
import forge.gui.toolbox.imaging.FImagePanel;
import forge.item.InventoryItem;
import java.awt.image.ColorModel;

/**
 * Displays image associated with a card or inventory item.
 * 
 * @version $Id$
 * 
 */
public final class CardPicturePanel extends JPanel {
    /** Constant <code>serialVersionUID=-3160874016387273383L</code>. */
    private static final long serialVersionUID = -3160874016387273383L;

    private Object displayed;

    private final FImagePanel panel;
    private BufferedImage currentImage;

    public CardPicturePanel() {
        super(new BorderLayout());

        this.panel = new FImagePanel();
        this.add(this.panel);
    }

    public void update() {
        this.setImage();
    }

    public void setCard(final InventoryItem cp) {
        this.displayed = cp;
        update();
    }

    //@Override
    public void setCard(final Card c) {
        this.displayed = c;
        update();
    }

    public void setImage() {        
        BufferedImage image = getImage();
        if (image != null && image != this.currentImage) {
            this.currentImage = image;
            this.panel.setImage(image);
        }        
    }
    
    public BufferedImage getImage() {

        BufferedImage image = null;
        int foilIndex = 0;
        
        if (displayed instanceof InventoryItem) {
            InventoryItem item = (InventoryItem) displayed;
            image = ImageCache.getOriginalImage(ImageCache.getImageKey(item, false), true);
        
        } else if (displayed instanceof Card) {
            Card item = (Card) displayed;
            image = ImageCache.getOriginalImage(item.getImageKey(), true);
            foilIndex = ((Card)this.displayed).getFoil();
        }

        if (image != null && foilIndex > 0) { 
            image = getFoiledImage(image, foilIndex);
        }
        
        return image;
    }
    
    private BufferedImage getFoiledImage(BufferedImage plainImage, int foilIndex) {
        ColorModel cm = plainImage.getColorModel();
        BufferedImage foilImage = new BufferedImage(cm, plainImage.copyData(null), cm.isAlphaPremultiplied(), null);
        final String fl = String.format("foil%02d", foilIndex);
        CardFaceSymbols.drawOther(foilImage.getGraphics(), fl, 0, 0, foilImage.getWidth(), foilImage.getHeight());
        return foilImage;                
    }
    
}
