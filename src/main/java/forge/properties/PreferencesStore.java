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
package forge.properties;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.util.FileUtil;

/**
 * Holds default preference values in an enum.
 * Loads preferred values when instantiated.
 * If a requested value is not present, default is returned.
 */
public abstract class PreferencesStore<T extends Enum<?>> {
    private final Map<T, String> preferenceValues = new HashMap<T, String>();
    private final String filename;

    public PreferencesStore(String filename0) {
        filename = filename0;
        
        List<String> lines = FileUtil.readFile(filename);
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#") || (line.isEmpty())) {
                continue;
            }

            String[] split = line.split("=");
            T pref = valueOf(split[0]);
            
            if (null == pref) {
                System.out.println("unknown preference: " + line);
                continue;
            }
            
            if (split.length == 2) {
                this.setPref(pref, split[1]);
            } else if (split.length == 1 && line.endsWith("=")) {
                this.setPref(pref, "");
            }
        }
    }
    
    protected abstract T[] getEnumValues();
    protected abstract T valueOf(String name);
    protected abstract String getPrefDefault(T key);
    
    public final void save() {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            for (T key : getEnumValues()) {
                writer.write(key + "=" + getPref(key));
                writer.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (null != writer) {
                try { writer.close(); }
                catch (IOException e) { System.out.println("error while closing " + filename); }
            }
        }
    }

    public final void reset() {
        this.preferenceValues.clear();
    }

    public final void setPref(T q0, String s0) {
        preferenceValues.put(q0, s0);
    }

    public final void setPref(T q0, boolean val) {
        setPref(q0, String.valueOf(val));
    }

    public final String getPref(T fp0) {
        String val;

        val = preferenceValues.get(fp0);
        if (val == null) { val = getPrefDefault(fp0); }

        return val;
    }

    public final int getPrefInt(T fp0) {
        return Integer.parseInt(getPref(fp0));
    }

    public final boolean getPrefBoolean(T fp0) {
        return Boolean.parseBoolean(getPref(fp0));
    }
}
