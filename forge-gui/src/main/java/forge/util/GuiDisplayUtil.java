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
package forge.util;

import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;

public final class GuiDisplayUtil {

    private GuiDisplayUtil() {
    }

    public static String getPlayerName() {
        return FModel.getPreferences().getPref(FPref.PLAYER_NAME);
    }

    public static String personalizeHuman(final String text) {
        final String playerName = FModel.getPreferences().getPref(FPref.PLAYER_NAME);
        return text.replaceAll("(?i)human", playerName);
    }

    public static String getRandomAiName() {
        return NameGenerator.getRandomName("Any", "Generic", getPlayerName());
    }

} // end class GuiDisplayUtil
