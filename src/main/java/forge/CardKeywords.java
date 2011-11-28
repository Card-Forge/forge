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

import java.util.ArrayList;

/**
 * <p>
 * Card_Keywords class.
 * </p>
 * 
 * @author Forge
 * @version $Id: Card_Keywords.java 10217 2011-09-04 10:14:19Z Sloth $
 */
public class CardKeywords implements Comparable<CardKeywords> {
    // takes care of individual card types
    private ArrayList<String> keywords = new ArrayList<String>();
    private ArrayList<String> removeKeywords = new ArrayList<String>();
    private boolean removeAllKeywords = false;
    private long timeStamp = 0;

    /**
     * <p>
     * getTimestamp.
     * </p>
     * 
     * @return a long.
     */
    public final long getTimestamp() {
        return this.timeStamp;
    }

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
    CardKeywords(final ArrayList<String> keywordList, final ArrayList<String> removeKeywordList,
            final boolean removeAll, final long stamp) {
        this.keywords = keywordList;
        this.removeKeywords = removeKeywordList;
        this.removeAllKeywords = removeAll;
        this.timeStamp = stamp;
    }

    /**
     * 
     * getKeywords.
     * 
     * @return ArrayList<String>
     */
    public final ArrayList<String> getKeywords() {
        return this.keywords;
    }

    /**
     * 
     * getRemoveKeywords.
     * 
     * @return ArrayList<String>
     */
    public final ArrayList<String> getRemoveKeywords() {
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public final int compareTo(final CardKeywords anotherCardKeywords) {
        int returnValue = 0;
        final long anotherTimeStamp = anotherCardKeywords.getTimestamp();
        if (this.timeStamp < anotherTimeStamp) {
            returnValue = -1;
        } else if (this.timeStamp > anotherTimeStamp) {
            returnValue = 1;
        }
        return returnValue;
    }

}
