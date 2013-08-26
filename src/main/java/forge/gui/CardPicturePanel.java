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
import forge.CardCharacteristicName;
import forge.ImageCache;
import forge.Singletons;
import forge.gui.toolbox.imaging.FImagePanel;
import forge.gui.toolbox.imaging.FImagePanel.AutoSizeImageMode;
import forge.gui.toolbox.imaging.FImageUtil;
import forge.item.InventoryItem;
import forge.properties.ForgePreferences.FPref;

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

    public void setCard(final InventoryItem cp) {
        this.displayed = cp;
        this.setImage();
    }

    //@Override
    public void setCard(final Card c) {
        this.displayed = c;
        this.setImage();
    }

    public void setCardImage(CardCharacteristicName flipState) {
        BufferedImage image = FImageUtil.getImage((Card)displayed, flipState);
        if (image != null && image != this.currentImage) {
            this.currentImage = image;
            this.panel.setImage(image, getAutoSizeImageMode());
        }
    }

    public void setImage() {
        BufferedImage image = getImage();
        if (image != null && image != this.currentImage) {
            this.currentImage = image;
            this.panel.setImage(image, getAutoSizeImageMode());
        }
    }

    public BufferedImage getImage() {

        BufferedImage image = null;

        if (displayed instanceof InventoryItem) {
            InventoryItem item = (InventoryItem) displayed;
            image = ImageCache.getOriginalImage(ImageCache.getImageKey(item, false), true);

        } else if (displayed instanceof Card) {
            image = FImageUtil.getImage((Card)displayed);
        }

        return image;
    }

    private AutoSizeImageMode getAutoSizeImageMode() {
        return (isUIScaleLarger() ? AutoSizeImageMode.PANEL : AutoSizeImageMode.SOURCE);
    }

    private boolean isUIScaleLarger() {
        return Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER);
    }

}
