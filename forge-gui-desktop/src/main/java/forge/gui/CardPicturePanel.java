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

import forge.ImageCache;
import forge.ImageKeys;
import forge.game.card.CardView.CardStateView;
import forge.item.InventoryItem;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.toolbox.imaging.FImagePanel;
import forge.toolbox.imaging.FImagePanel.AutoSizeImageMode;
import forge.toolbox.imaging.FImageUtil;

/**
 * Displays image associated with a card or inventory item.
 *
 * @version $Id: CardPicturePanel.java 25265 2014-03-27 02:18:47Z drdev $
 *
 */
public final class CardPicturePanel extends JPanel {
    /** Constant <code>serialVersionUID=-3160874016387273383L</code>. */
    private static final long serialVersionUID = -3160874016387273383L;

    private Object displayed;
    private boolean mayView = true;

    private final FImagePanel panel;
    private BufferedImage currentImage;

    public CardPicturePanel() {
        super(new BorderLayout());

        this.panel = new FImagePanel();
        this.add(this.panel);
    }

    public void setItem(final InventoryItem item) {
        setImage(item ,true);
    }

    public void setCard(final CardStateView c) {
        setCard(c, true);
    }
    public void setCard(final CardStateView c, final boolean mayView) {
        setImage(c, mayView);
    }

    private void setImage(final Object display, final boolean mayView) {
        this.displayed = display;
        this.mayView = mayView;

        final BufferedImage image = getImage();
        if (image != null && image != this.currentImage) {
            this.currentImage = image;
            this.panel.setImage(image, getAutoSizeImageMode());
        }
    }

    private BufferedImage getImage() {
        if (!mayView) {
            return ImageCache.getOriginalImage(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD), true);
        }

        if (displayed instanceof InventoryItem) {
            final InventoryItem item = (InventoryItem) displayed;
            return ImageCache.getOriginalImage(ImageKeys.getImageKey(item, false), true);
        } else if (displayed instanceof CardStateView) {
            return FImageUtil.getImage((CardStateView)displayed);
        }
        return null;
    }

    private static AutoSizeImageMode getAutoSizeImageMode() {
        return (isUIScaleLarger() ? AutoSizeImageMode.PANEL : AutoSizeImageMode.SOURCE);
    }

    private static boolean isUIScaleLarger() {
        return FModel.getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER);
    }

}
