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
package forge.card;

import forge.CardStorageReader;
import forge.card.CardRules;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.gui.SOptionPane;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CardScriptInfo {
    private String text;
    private File file;

    public CardScriptInfo(String text0, File file0) {
        text = text0;
        file = file0;
    }

    public String getText() {
        return text;
    }

    public File getFile() {
    	return file;
    }

    public boolean canEdit() {
    	return file != null;
    }

    public boolean trySetText(String text0) {
    	if (file == null) { return false; }

    	try {
    	    PrintWriter p = new PrintWriter(file);
    	    p.print(text0);
    	    p.close();

    	    text = text0;
    	    return true;
    	}
    	catch (final Exception ex) {
    		SOptionPane.showErrorDialog("Problem writing file - " + file + " : " + ex);
    		return false;
    	}
    }

    private static Map<String, CardScriptInfo> allScripts = new ConcurrentHashMap<>();
    public static void addCard(String name, String script, File file) {
        allScripts.put(name, new CardScriptInfo(script, file));
    }

    public static CardScriptInfo getScriptFor(String name) {
        CardScriptInfo script = allScripts.get(name);
        if (script == null) { //attempt to load script if not previously loaded
            String filename = name.toLowerCase().replace(' ', '_').replace('-', '_').replace("'", "").replace(",", "") + ".txt";
            File file = new File(ForgeConstants.CARD_DATA_DIR + filename.charAt(0) + File.separator + filename);
            if (file.exists()) {
                script = new CardScriptInfo(FileUtil.readFileToString(file), file);
                allScripts.put(name, script);
            }
        }
        return script;
    }

    public static CardStorageReader.Observer readerObserver = new CardStorageReader.Observer() {
        @Override
        public void cardLoaded(CardRules rules, List<String> lines, File fileOnDisk) {
            allScripts.put(rules.getName(), new CardScriptInfo(StringUtils.join(lines, '\n'), fileOnDisk));
        }
    };
}
