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

import java.util.List;
import java.util.ArrayList;

/**
 * <p>
 * Card_Keywords class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardKeywords {
    // takes care of individual card types
    private List<String> keywords = new ArrayList<String>();
    private List<String> removeKeywords = new ArrayList<String>();
    private boolean removeAllKeywords = false;

    /**
     * 
     * Card_Keywords.
     * 
     * @param keywordList
     *            an ArrayList<String>
     * @param removeKeywordList
     *            a ArrayList<String>
     * @param removeAll
     *            a boolean
     * @param stamp
     *            a long
     */
    CardKeywords(final List<String> keywordList, final List<String> removeKeywordList, final boolean removeAll) {
        this.keywords = keywordList;
        this.removeKeywords = removeKeywordList;
        this.removeAllKeywords = removeAll;
    }

    /**
     * 
     * getKeywords.
     * 
     * @return ArrayList<String>
     */
    public final List<String> getKeywords() {
        return this.keywords;
    }

    /**
     * 
     * getRemoveKeywords.
     * 
     * @return ArrayList<String>
     */
    public final List<String> getRemoveKeywords() {
        return this.removeKeywords;
    }

    /**
     * 
     * isRemoveAllKeywords.
     * 
     * @return boolean
     */
    public final boolean isRemoveAllKeywords() {
        return this.removeAllKeywords;
    }
}
