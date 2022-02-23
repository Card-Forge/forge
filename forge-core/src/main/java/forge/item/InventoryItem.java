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
package forge.item;

import forge.util.IHasName;

/**
 * Interface to define a player's inventory may hold. Should include
 * CardPrinted, Booster, Pets, Plants... etc
 */
public interface InventoryItem extends IHasName {
    String getItemType();
    String getImageKey(boolean altState);

    /**
     * Converts a card name to a sortable name.
     * Trim leading quotes, then move article last, then replace characters.
     * Because An-Havva Constable.
     * Capitals and lowercase sorted as one: "my deck" before "Myr Retribution"
     * Apostrophes matter, though: "D'Avenant" before "Danitha"
     * TO DO: Commas before apostrophes: "Rakdos, Lord of Riots" before "Rakdos's Return"
     *
     * @param printedName The name of the card.
     * @return A sortable name.
     */
    public static String toSortableName(String printedName) {
        if (printedName.startsWith("\"")) printedName = printedName.substring(1);
        return moveArticleToEnd(printedName).toLowerCase().replaceAll("[^\\s'0-9a-z]", "");
    }


    /**
     * Article words. These words get kicked to the end of a sortable name.
     * For localization, simply overwrite this array with appropriate words.
     * Words in this list are used by the method String moveArticleToEnd(String), useful
     * for alphabetizing phrases, in particular card or other inventory object names.
     */
    public static final String[] ARTICLE_WORDS = {
            "A",
            "An",
            "The"
    };

    /**
     * Detects whether a string begins with an article word
     *
     * @param str The name of the card.
     * @return The sort-friendly name of the card. Example: "The Hive" becomes "Hive The".
     */
    public static String moveArticleToEnd(String str) {
        String articleWord;
        for (int i = 0; i < ARTICLE_WORDS.length; i++) {
            articleWord = ARTICLE_WORDS[i];
            if (str.startsWith(articleWord + " ")) {
                str = str.substring(articleWord.length() + 1) + " " + articleWord;
                return str;
            }
        }
        return str;
    }
}
