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
package forge.gui.card;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import forge.localinstance.properties.ForgeConstants;
import forge.util.FileUtil;

public final class CardScriptInfo {
    private String text;
    private final File file;

    public CardScriptInfo(final String text0, final File file0) {
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

    public boolean trySetText(final String text0) {
        if (file == null) { return false; }

        try (PrintWriter p = new PrintWriter(file)) {
            p.print(text0);
            if (!text0.endsWith(("\n"))){
                p.print("\n");
            }

            text = text0;
            return true;
        }
        catch (final Exception ex) {
            System.err.println("Problem writing file - " + file);
            ex.printStackTrace();
            return false;
        }
    }

    private static Map<String, CardScriptInfo> allScripts = new ConcurrentHashMap<>();
    public static CardScriptInfo getScriptFor(final String name) {
        CardScriptInfo script = allScripts.get(name);
        if (script == null) { //attempt to load script if not previously loaded
            final String filename = name.toLowerCase().replaceAll("[^-a-z0-9\\s]","").replaceAll("[-\\s]","_").replaceAll("__","_") + ".txt";
            String[] folders = { String.valueOf(filename.charAt(0)), "upcoming"};
            
            for (String folder : folders) {
               final File file = new File(ForgeConstants.CARD_DATA_DIR + folder + File.separator + filename);
               if (file.exists()) {
                   script = new CardScriptInfo(FileUtil.readFileToString(file), file);
                   allScripts.put(name, script);
                   break;
               }
            }
        }
        return script;
    }
}
