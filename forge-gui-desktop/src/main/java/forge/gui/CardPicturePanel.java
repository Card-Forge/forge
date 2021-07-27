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
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

import javax.swing.JPanel;

import forge.ImageCache;
import forge.ImageKeys;
import forge.game.card.CardView.CardStateView;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.toolbox.CardFaceSymbols;
import forge.toolbox.imaging.FImagePanel;
import forge.toolbox.imaging.FImagePanel.AutoSizeImageMode;
import forge.toolbox.imaging.FImageUtil;
import forge.util.ImageFetcher;

/**
 * Displays image associated with a card or inventory item.
 *
 * @version $Id: CardPicturePanel.java 25265 2014-03-27 02:18:47Z drdev $
 *
 */
public final class CardPicturePanel extends JPanel implements ImageFetcher.Callback {

    /**
     * Constant <code>serialVersionUID=-3160874016387273383L</code>.
     */
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
        setImage(item, true);
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
            if (displayed instanceof PaperCard) {
                ColorModel cm = image.getColorModel();
                boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
                WritableRaster raster = image.copyData(null);
                final BufferedImage displayedimage = new BufferedImage(cm, raster, isAlphaPremultiplied, null)
                        .getSubimage(0, 0, image.getWidth(), image.getHeight());
                this.currentImage = displayedimage;
                this.panel.setImage(displayedimage, getAutoSizeImageMode());
                PaperCard card = (PaperCard) displayed;
                if (FModel.getPreferences().getPrefBoolean(FPref.UI_OVERLAY_FOIL_EFFECT)) {
                    if (card.isFoil()) {
                        CardFaceSymbols.drawOther(displayedimage.getGraphics(), String.format("foil%02d", 1), 0, 0,
                                displayedimage.getWidth(), displayedimage.getHeight());
                    }
                }
            } else {
                this.currentImage = image;
                this.panel.setImage(image, getAutoSizeImageMode());
            }
        }
    }

    private BufferedImage getImage() {
        if (!mayView) {
            return ImageCache.getOriginalImage(ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD), true, null);
        }

        if (displayed instanceof InventoryItem) {
            final InventoryItem item = (InventoryItem) displayed;
            BufferedImage image = ImageCache.getOriginalImage(item.getImageKey(false), true, null);
            if (ImageCache.isDefaultImage(image) && item instanceof PaperCard) {
                GuiBase.getInterface().getImageFetcher().fetchImage(item.getImageKey(false), this);
            }
            return image;
        } else if (displayed instanceof CardStateView) {
            CardStateView card = (CardStateView) displayed;
            BufferedImage image = ImageCache.getOriginalImage(card.getImageKey(), false, card.getCard());
            if (image == null) {
                GuiBase.getInterface().getImageFetcher().fetchImage(card.getImageKey(), this);
            }
            return FImageUtil.getImage((CardStateView) displayed);
        }
        return null;
    }

    @Override
    public void onImageFetched() {
        setImage(displayed, mayView);
        repaint();
    }

    private static AutoSizeImageMode getAutoSizeImageMode() {
        return (isUIScaleLarger() ? AutoSizeImageMode.PANEL : AutoSizeImageMode.SOURCE);
    }

    private static boolean isUIScaleLarger() {
        return FModel.getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER);
    }
}
