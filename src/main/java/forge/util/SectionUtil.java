/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Divides file into sections and joins them back to stringlist to save.
 * 
 */
public class SectionUtil {

    /**
     * Parses the sections.
     *
     * @param source the source
     * @return the map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, List<String>> parseSections(final List<String> source) {
        final Map<String, List<String>> result = new HashMap<String, List<String>>();
        String currentSection = "";
        List<String> currentList = null;

        for (final String s : source) {
            final String st = s.trim();
            if (st.length() == 0) {
                continue;
            }
            if (st.startsWith("[") && st.endsWith("]")) {
                if ((currentList != null) && (currentList.size() > 0)) {
                    final Object oldVal = result.get(currentSection);
                    if ((oldVal != null) && (oldVal instanceof List<?>)) {
                        currentList.addAll((List<String>) oldVal);
                    }
                    result.put(currentSection, currentList);
                }

                final String newSection = st.substring(1, st.length() - 1);
                currentSection = newSection;
                currentList = null;
            } else {
                if (currentList == null) {
                    currentList = new ArrayList<String>();
                }
                currentList.add(st);
            }
        }

        // save final block
        if ((currentList != null) && (currentList.size() > 0)) {
            final Object oldVal = result.get(currentSection);
            if ((oldVal != null) && (oldVal instanceof List<?>)) {
                currentList.addAll((List<String>) oldVal);
            }
            result.put(currentSection, currentList);
        }

        return result;
    }
}
