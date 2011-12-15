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

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;

import forge.FileUtil;
import forge.HttpUtil;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * Contains Maps of card ratings.
 */
public class CardRatings {
    private static Map<String, Integer> fullRatings = new TreeMap<String, Integer>();
    private static Map<String, Integer> blockRatings = new TreeMap<String, Integer>();
    private static Map<String, Integer> customRatings = new TreeMap<String, Integer>();

    private static ArrayList<String> tempRatings = new ArrayList<String>();

    /**
     * Instantiates a new card ratings.
     */
    public CardRatings() {
        if (CardRatings.fullRatings.size() < 1) {
            this.loadFullRatings();
        }

        if (CardRatings.blockRatings.size() < 1) {
            this.loadBlockRatings();
        }

        if (CardRatings.customRatings.size() < 1) {
            this.loadCustomRatings();
        }

        if (CardRatings.tempRatings.size() < 1) {
            CardRatings.tempRatings = FileUtil.readFile("res/draft/tempRatings.dat");
        }
    }

    private void loadFullRatings() {
        final ArrayList<String> sRatings = FileUtil.readFile("res/draft/fullRatings.dat");
        if (sRatings.size() > 1) {
            for (final String s : sRatings) {
                if (s.length() > 3) {
                    final String[] ss = s.split(":");
                    if (ss.length > 1) {
                        CardRatings.fullRatings.put(ss[0], new Integer(ss[1]));
                    }
                }
            }
        }
    }

    private void loadBlockRatings() {
        final ArrayList<String> sRatings = FileUtil.readFile("res/draft/blockRatings.dat");
        if (sRatings.size() > 1) {
            for (final String s : sRatings) {
                if (s.length() > 3) {
                    final String[] ss = s.split(":");
                    if (ss.length > 1) {
                        CardRatings.blockRatings.put(ss[0], new Integer(ss[1]));
                    }
                }
            }
        }
    }

    private void loadCustomRatings() {
        final ArrayList<String> sRatings = FileUtil.readFile("res/draft/customRatings.dat");
        if (sRatings.size() > 1) {
            for (final String s : sRatings) {
                if (s.length() > 3) {
                    final String[] ss = s.split(":");
                    if (ss.length > 1) {
                        CardRatings.customRatings.put(ss[0], new Integer(ss[1]));
                    }
                }
            }
        }
    }

    /**
     * Save ratings.
     */
    public final void saveRatings() {
        if (CardRatings.fullRatings.size() > 1) {
            final String[] keys = CardRatings.fullRatings.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
            final ArrayList<String> ratings = new ArrayList<String>();

            for (final String k : keys) {
                ratings.add(k + ":" + CardRatings.fullRatings.get(k));
            }

            FileUtil.writeFile("res/draft/fullRatings.dat", ratings);
        }

        if (CardRatings.blockRatings.size() > 1) {
            final String[] keys = CardRatings.blockRatings.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
            final ArrayList<String> ratings = new ArrayList<String>();

            for (final String k : keys) {
                ratings.add(k + ":" + CardRatings.blockRatings.get(k));
            }

            FileUtil.writeFile("res/draft/blockRatings.dat", ratings);
        }

        if (CardRatings.customRatings.size() > 1) {
            final String[] keys = CardRatings.customRatings.keySet().toArray(ArrayUtils.EMPTY_STRING_ARRAY);
            final ArrayList<String> ratings = new ArrayList<String>();

            for (final String k : keys) {
                ratings.add(k + ":" + CardRatings.customRatings.get(k));
            }

            FileUtil.writeFile("res/draft/customRatings.dat", ratings);
        }

        if (CardRatings.tempRatings.size() > 1) {
            FileUtil.writeFile("res/draft/tempRatings.dat", CardRatings.tempRatings);
        }
    }

    /**
     * Gets the full rating.
     * 
     * @param cardName
     *            the card name
     * @return the full rating
     */
    public final int getFullRating(final String cardName) {
        if (CardRatings.fullRatings.containsKey(cardName)) {
            return CardRatings.fullRatings.get(cardName);
        }

        return 0;
    }

    /**
     * Gets the block rating.
     * 
     * @param cardName
     *            the card name
     * @param setCode
     *            the set code
     * @return the block rating
     */
    public final int getBlockRating(final String cardName, final String setCode) {
        final String cNsC = cardName + "|" + setCode;
        if (CardRatings.blockRatings.containsKey(cNsC)) {
            return CardRatings.blockRatings.get(cNsC);
        }

        return 0;
    }

    /**
     * Gets the custom ratings.
     * 
     * @param cardName
     *            the card name
     * @param custName
     *            the cust name
     * @return the custom ratings
     */
    public final int getCustomRatings(final String cardName, final String custName) {
        final String cNcN = cardName + "|" + custName;
        if (CardRatings.customRatings.containsKey(cNcN)) {
            return CardRatings.customRatings.get(cNcN);
        }

        return 0;
    }

    /**
     * Put full rating.
     * 
     * @param cardName
     *            the card name
     * @param rating
     *            the rating
     */
    public final void putFullRating(final String cardName, final int rating) {
        if (CardRatings.fullRatings.containsKey(cardName)) {
            final int r = CardRatings.fullRatings.get(cardName);
            final int nr = (r + rating) / 2;
            CardRatings.fullRatings.put(cardName, nr);
        } else {
            CardRatings.fullRatings.put(cardName, rating);
        }

        CardRatings.tempRatings.add("Full:" + cardName + ":" + rating);
    }

    /**
     * Put block rating.
     * 
     * @param cardName
     *            the card name
     * @param setCode
     *            the set code
     * @param rating
     *            the rating
     */
    public final void putBlockRating(final String cardName, final String setCode, final int rating) {
        final String cNsC = cardName + "|" + setCode;
        if (CardRatings.blockRatings.containsKey(cNsC)) {
            final int r = CardRatings.blockRatings.get(cNsC);
            final int nr = (r + rating) / 2;
            CardRatings.blockRatings.put(cNsC, nr);
        } else {
            CardRatings.blockRatings.put(cNsC, rating);
        }

        CardRatings.tempRatings.add("Block:" + cNsC + ":" + rating);
    }

    /**
     * Put custom rating.
     * 
     * @param cardName
     *            the card name
     * @param custName
     *            the cust name
     * @param rating
     *            the rating
     */
    public final void putCustomRating(final String cardName, final String custName, final int rating) {
        final String cNcN = cardName + "|" + custName;
        if (CardRatings.customRatings.containsKey(cNcN)) {
            final int r = CardRatings.customRatings.get(cNcN);
            final int nr = (r + rating) / 2;
            CardRatings.customRatings.put(cNcN, nr);
        } else {
            CardRatings.customRatings.put(cNcN, rating);
        }

        CardRatings.tempRatings.add("Custom:" + cNcN + ":" + rating);
    }

    /**
     * Upload ratings.
     */
    public final void uploadRatings() {
        FileUtil.writeFile("res/draft/tempRatings.dat", CardRatings.tempRatings);

        final HttpUtil httpPost = new HttpUtil();
        String url = ForgeProps.getProperty(NewConstants.CARDFORGE_URL) + "/draftAI/submitRatingsData.php?";

        httpPost.upload(url, "res/draft/tempRatings.dat");

        FileUtil.writeFile("res/draft/tempRatings.dat", new ArrayList<String>());
    }

    /**
     * Download ratings.
     */
    public final void downloadRatings() {
        final HttpUtil httpGet = new HttpUtil();
        final ArrayList<String> tmpList = new ArrayList<String>();
        String tmpData = new String();

        String url = ForgeProps.getProperty(NewConstants.CARDFORGE_URL) + "/draftAI/getRatingsData.php?fmt=Full";
        tmpData = httpGet.getURL(url);
        tmpList.add(tmpData);
        FileUtil.writeFile("res/draft/fullRatings.dat", tmpList);
        CardRatings.fullRatings.clear();
        this.loadFullRatings();

        tmpList.clear();

        url = ForgeProps.getProperty(NewConstants.CARDFORGE_URL) + "/draftAI/getRatingsData.php?fmt=Block";
        tmpData = httpGet.getURL(url);
        tmpList.add(tmpData);
        FileUtil.writeFile("res/draft/blockRatings.dat", tmpList);
        CardRatings.blockRatings.clear();
        this.loadBlockRatings();

        tmpList.clear();

        url = ForgeProps.getProperty(NewConstants.CARDFORGE_URL) + "/draftAI/getRatingsData.php?fmt=Custom";
        tmpData = httpGet.getURL(url);
        tmpList.add(tmpData);
        FileUtil.writeFile("res/draft/customRatings.dat", tmpList);
        CardRatings.customRatings.clear();
        this.loadCustomRatings();
    }
}
