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

import java.awt.Dimension;

/**
 * Useful imaging routines. 
 * 
 * @version $Id$
 * 
 */
public final class ImageUtil {  
    private ImageUtil() {}
    
    /**
     * Gets the nearest rotation for a requested rotation.
     * <p>
     * For example, if {@code nearestRotation} is set to 90 degrees then
     * will return one of 0, 90, 180 or 270 degrees, whichever is nearest to
     * {@code requestedRotation}.
     * 
     */
    public static int getRotationToNearest(int requestedRotation, int nearestRotation) {
        // Ensure requested rotation falls within -360..0..360 degree range first.
        requestedRotation = requestedRotation - (360 * (requestedRotation / (int)360));
        return (int)(Math.rint((double) requestedRotation / nearestRotation) * nearestRotation);
    }        
    
    /**
     * Calculates the scale required to best fit contained into container 
     * whilst retaining the aspect ratio.
     */
    public static double getBestFitScale(int containedW, int containedH, int containerW, int containerH) {
        double scaleX = (double)containerW / containedW;
        double scaleY = (double)containerH / containedH;
        return Math.min(scaleX, scaleY);        
    }    
    /**
     * Calculates the scale required to best fit contained into container 
     * whilst retaining the aspect ratio.
     */
    public static double getBestFitScale(Dimension contained, Dimension container) {
        return getBestFitScale(contained.width, contained.height, container.width, container.height);
    }
    

}
