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
public class Card_Keywords implements Comparable<Card_Keywords> {
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
        return timeStamp;
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
    Card_Keywords(final ArrayList<String> keywordList, final ArrayList<String> removeKeywordList,
            final boolean removeAll, final long stamp) {
        keywords = keywordList;
        removeKeywords = removeKeywordList;
        removeAllKeywords = removeAll;
        timeStamp = stamp;
    }

    /**
     * 
     * getKeywords.
     * 
     * @return ArrayList<String>
     */
    public final ArrayList<String> getKeywords() {
        return keywords;
    }

    /**
     * 
     * getRemoveKeywords.
     * 
     * @return ArrayList<String>
     */
    public final ArrayList<String> getRemoveKeywords() {
        return removeKeywords;
    }

    /**
     * 
     * isRemoveAllKeywords.
     * 
     * @return boolean
     */
    public final boolean isRemoveAllKeywords() {
        return removeAllKeywords;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public final int compareTo(final Card_Keywords anotherCardKeywords) {
        int returnValue = 0;
        long anotherTimeStamp = anotherCardKeywords.getTimestamp();
        if (this.timeStamp < anotherTimeStamp) {
            returnValue = -1;
        } else if (this.timeStamp > anotherTimeStamp) {
            returnValue = 1;
        }
        return returnValue;
    }

}
