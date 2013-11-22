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
package forge.gui.workshop;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import forge.CardStorageReader;
import forge.card.CardRules;

public final class CardScriptInfo {
    private String cardScript;
    private File file;

    public CardScriptInfo(String text0, File file0) {
        this.cardScript = text0;
        this.file = file0;
    }

    public String getText() {
        return this.cardScript;
    }

    public File getFile() {
    	return this.file;
    }

    public boolean canEdit() {
    	return this.file != null;
    }

    private static Map<String, CardScriptInfo> allScrips = new ConcurrentHashMap<>();
    public static void addCard(String name, String script, File file) {
        allScrips.put(name, new CardScriptInfo(script, file));
    }
    
    public static CardScriptInfo getScriptFor(String name) {
        return allScrips.get(name);
    }
    
    public static CardStorageReader.Observer readerObserver = new CardStorageReader.Observer() {
        @Override
        public void cardLoaded(CardRules rules, List<String> lines, File fileOnDisk) {
            allScrips.put(rules.getName(), new CardScriptInfo(StringUtils.join(lines, '\n'), fileOnDisk));
        }
    };
}
