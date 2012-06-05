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

import javax.swing.JFrame;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * Gui_DownloadPrices class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
@SuppressWarnings("serial")
public class GuiDownloadPrices extends GuiDownloader {

    /**
     * TODO: Write javadoc for Constructor.
     * @param frame {@link javax.swing.JFrame}
     */
    public GuiDownloadPrices(final JFrame frame) {
        super(frame);
    }

    /* (non-Javadoc)
     * @see forge.gui.download.GuiDownloader#getNeededImages()
     */
    @Override
    protected DownloadObject[] getNeededImages() {
        final File f = ForgeProps.getFile(NewConstants.Quest.PRICE);
        final String url = "http://www.cardforge.org/MagicInfo/pricegen.php";
        final DownloadObject[] objects = {new DownloadObject(url, f)};
        return objects;
    }

} // @jve:decl-index=0:visual-constraint="10,10"
