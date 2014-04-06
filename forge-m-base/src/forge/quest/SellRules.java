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
package forge.quest;

import forge.quest.data.QuestAchievements;
import forge.util.FileSection;

import java.util.List;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class SellRules {

    private int minWins = 0;
    private int cost = 250;
    private int minDifficulty = 0;
    private int maxDifficulty = 5;

    /**
     * Instantiates a new sell rules.
     *
     * @param questShop the quest shop
     */
    public SellRules(List<String> questShop) {
        if (null == questShop || questShop.isEmpty()) {
            return;
        }

        FileSection section = FileSection.parse(questShop, "=");
        minWins = section.getInt("WinsToUnlock");
        cost = section.getInt("Credits", 250);
        maxDifficulty = section.getInt("MaxDifficulty", 5);
        minDifficulty = section.getInt("MinDifficulty", 0);
    }

    /**
     * Meets requiremnts.
     *
     * @param quest the quest
     * @return true, if successful
     */
    public boolean meetsRequiremnts(QuestAchievements quest) {
        if (quest.getWin() < minWins) {
            return false;
        }
        if (quest.getDifficulty() < minDifficulty || quest.getDifficulty() > maxDifficulty) {
            return false;
        }

        return true;
    }

    /**
     * Gets the cost.
     *
     * @return the cost
     */
    public final int getCost() {
        return cost;
    }

    /**
     * Gets the minWins.
     *
     * @return the minWins
     */
    public final int getMinWins() {
        return minWins;
    }

}
