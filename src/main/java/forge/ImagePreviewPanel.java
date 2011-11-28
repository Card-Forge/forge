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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JPanel;

/**
 * <p>
 * ImagePreviewPanel class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ImagePreviewPanel extends JPanel implements PropertyChangeListener {

    /** Constant <code>serialVersionUID=2163809931940286240L</code>. */
    private static final long serialVersionUID = 2163809931940286240L;
    private int width, height;
    private ImageIcon icon;
    private Image image;
    /** Constant <code>ACCSIZE=155</code>. */
    private static final int ACCSIZE = 155;
    private final Color bg;

    /**
     * <p>
     * Constructor for ImagePreviewPanel.
     * </p>
     */
    public ImagePreviewPanel() {
        this.setPreferredSize(new Dimension(ImagePreviewPanel.ACCSIZE, -1));
        this.bg = this.getBackground();
    }

    /** {@inheritDoc} */
    @Override
    public final void propertyChange(final PropertyChangeEvent e) {
        final String propertyName = e.getPropertyName();

        // Make sure we are responding to the right event.
        if (propertyName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
            final File selection = (File) e.getNewValue();
            String name;

            if (selection == null) {
                return;
            } else {
                name = selection.getAbsolutePath();
            }

            /*
             * Make reasonably sure we have an image format that AWT can handle
             * so we don't try to draw something silly.
             */
            if ((name != null)
                    && (name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".jpeg")
                            || name.toLowerCase().endsWith(".gif") || name.toLowerCase().endsWith(".png"))) {
                this.icon = new ImageIcon(name);
                this.image = this.icon.getImage();
                this.scaleImage();
                this.repaint();
            }
        }
    }

    /**
     * <p>
     * scaleImage.
     * </p>
     */
    private void scaleImage() {
        this.width = this.image.getWidth(this);
        this.height = this.image.getHeight(this);
        double ratio = 1.0;

        /*
         * Determine how to scale the image. Since the accessory can expand
         * vertically make sure we don't go larger than 150 when scaling
         * vertically.
         */
        if (this.width >= this.height) {
            ratio = (double) (ImagePreviewPanel.ACCSIZE - 5) / this.width;
            this.width = ImagePreviewPanel.ACCSIZE - 5;
            this.height = (int) (this.height * ratio);
        } else {
            if (this.getHeight() > 150) {
                ratio = (double) (ImagePreviewPanel.ACCSIZE - 5) / this.height;
                this.height = ImagePreviewPanel.ACCSIZE - 5;
                this.width = (int) (this.width * ratio);
            } else {
                ratio = (double) this.getHeight() / this.height;
                this.height = this.getHeight();
                this.width = (int) (this.width * ratio);
            }
        }

        this.image = this.image.getScaledInstance(this.width, this.height, Image.SCALE_DEFAULT);
    }

    /** {@inheritDoc} */
    @Override
    public final void paintComponent(final Graphics g) {
        g.setColor(this.bg);

        /*
         * If we don't do this, we will end up with garbage from previous images
         * if they have larger sizes than the one we are currently drawing.
         * Also, it seems that the file list can paint outside of its rectangle,
         * and will cause odd behavior if we don't clear or fill the rectangle
         * for the accessory before drawing. This might be a bug in
         * JFileChooser.
         */
        g.fillRect(0, 0, ImagePreviewPanel.ACCSIZE, this.getHeight());
        g.drawImage(this.image, ((this.getWidth() / 2) - (this.width / 2)) + 5, (this.getHeight() / 2)
                - (this.height / 2), this);
    }

}
