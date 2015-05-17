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
package forge.download;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import forge.properties.ForgeConstants;

public class GuiDownloadPrices extends GuiDownloadService {
    @Override
    public String getTitle() {
        return "Download Card Prices";
    }

    @Override
    protected Map<String, String> getNeededFiles() {
        return ImmutableMap.of(ForgeConstants.QUEST_CARD_PRICE_FILE, ForgeConstants.URL_PRICE_DOWNLOAD);
    }
}
