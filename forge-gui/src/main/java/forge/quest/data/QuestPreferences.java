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
package forge.quest.data;

import forge.properties.ForgeConstants;
import forge.properties.PreferencesStore;
import forge.util.Localizer;
import forge.util.TextUtil;

import java.io.Serializable;

@SuppressWarnings("serial")
public class QuestPreferences extends PreferencesStore<QuestPreferences.QPref> implements Serializable {

    /**
     * Preference identifiers, and their default values.
     */
    public enum QPref {

        // How many of each rarity comes in a won booster pack
        BOOSTER_COMMONS("11"),
        BOOSTER_UNCOMMONS("3"),
        BOOSTER_RARES("1"),
        // The preferred format of the won booster pack
        BOOSTER_FORMAT("Standard"),

        SPECIAL_BOOSTERS("1"),

        // How many credits are lost for losing a match
        PENALTY_LOSS("15"),

        // Currently chosen quest and deck
        CURRENT_QUEST("DEFAULT"),
        CURRENT_DECK("DEFAULT"),

        // All of the rewards given out End of Match
        // Awarded to every match winner
        REWARDS_BASE("25"),
        // Didn't lose a game in the match
        REWARDS_UNDEFEATED("25"),
        // For each of your previous wins gain a small multiplier
        // This is here to award long quests with more money for buying expensive cards
        REWARDS_WINS_MULTIPLIER("0.3"),
        // Rewards stop increasing after this many wins
        REWARDS_WINS_MULTIPLIER_MAX("300"),

        // Winning each game by other means "Poison", "Milling" or "Alternative" Win
        REWARDS_POISON("50"),
        REWARDS_MILLED("40"),
        REWARDS_ALTERNATIVE("100"),

        // Max Bonus for health difference
        REWARDS_HEALTH_DIFF_MAX("750"),

        // If you Mulligan to 0 to start a game
        REWARDS_MULLIGAN0("500"),

        // How many turns it took you to win the game
        REWARDS_TURN15("5"),
        REWARDS_TURN10("50"),
        REWARDS_TURN5("250"),
        REWARDS_TURN1("1500"),

        // How many basic your starting pool has (if appropriate)
        //STARTING_BASIC_LANDS("20"),
        STARTING_SNOW_LANDS("5"),

        // Starting pool color bias effect
        STARTING_POOL_COLOR_BIAS("75"),

        // Commons in your starting pool, by difficulty
        STARTING_COMMONS_EASY("82"),
        STARTING_COMMONS_MEDIUM("80"),
        STARTING_COMMONS_HARD("78"),
        STARTING_COMMONS_EXPERT("76"),

        // Uncommons in your starting pool, by difficulty
        STARTING_UNCOMMONS_EASY("40"),
        STARTING_UNCOMMONS_MEDIUM("36"),
        STARTING_UNCOMMONS_HARD("32"),
        STARTING_UNCOMMONS_EXPERT("28"),

        // Rares in your starting pool, by difficulty
        STARTING_RARES_EASY("20"),
        STARTING_RARES_MEDIUM("18"),
        STARTING_RARES_HARD("16"),
        STARTING_RARES_EXPERT("15"),

        // Credits you start the quest with, by difficulty
        STARTING_CREDITS_EASY("250"),
        STARTING_CREDITS_MEDIUM("200"),
        STARTING_CREDITS_HARD("150"),
        STARTING_CREDITS_EXPERT("100"),
        
        // Matches won per challenge unlock
        WINS_NEW_CHALLENGE("10"),

        // Matches won per draft unlock
        WINS_NEW_DRAFT("5"),
        WINS_ROTATE_DRAFT("15"),

        // Matches won per booster award, by difficulty
        WINS_BOOSTER_EASY("1"),
        WINS_BOOSTER_MEDIUM("1"),
        WINS_BOOSTER_HARD("2"),
        WINS_BOOSTER_EXPERT("2"),

        // Matches won per increased rank, by difficulty
        // Rank affects how many packs are opened for singles in the spell shop
        WINS_RANKUP_EASY("3"),
        WINS_RANKUP_MEDIUM("4"),
        WINS_RANKUP_HARD("5"),
        WINS_RANKUP_EXPERT("6"),

        // Matches won to unlock Medium Opponents, by difficulty
        WINS_MEDIUMAI_EASY("20"),
        WINS_MEDIUMAI_MEDIUM("15"),
        WINS_MEDIUMAI_HARD("12"),
        WINS_MEDIUMAI_EXPERT("10"),

        // Matches won to unlock Hard Opponents, by difficulty
        WINS_HARDAI_EASY("40"),
        WINS_HARDAI_MEDIUM("30"),
        WINS_HARDAI_HARD("25"),
        WINS_HARDAI_EXPERT("20"),

        // Matches won to unlock Expert Opponents, by difficulty
        WINS_EXPERTAI_EASY("80"),
        WINS_EXPERTAI_MEDIUM("60"),
        WINS_EXPERTAI_HARD("50"),
        WINS_EXPERTAI_EXPERT("40"),
        
        WINS_UNLOCK_SET("20"),
        // if enabled, any set can be unlocked, but higher distances raise the price exponentially
        UNLIMITED_UNLOCKING("0"),
        UNLOCK_DISTANCE_MULTIPLIER("1.25"),

        // Maximum amount of "Packs" opened by the Shop and available as singles
        SHOP_MAX_PACKS("7"),
        SHOP_MIN_PACKS("3"),

        // Rarity distribution of Singles in an Opened Shop Pack
        SHOP_SINGLES_COMMON("7"),
        SHOP_SINGLES_UNCOMMON("3"),
        SHOP_SINGLES_RARE("1"),

        // How many wins it takes to open an additional pack in the shop
        SHOP_WINS_FOR_ADDITIONAL_PACK("10"),
        // How many packs the shop start with.
        SHOP_STARTING_PACKS("5"),

        // Value * .20 + Wins
        SHOP_SELLING_PERCENTAGE_BASE("20"),
        SHOP_SELLING_PERCENTAGE_MAX("60"),

        // Maximum selling price in a spell shop
        SHOP_MAX_SELLING_PRICE("1000"),
        // Wins until the selling price limit is removed
        SHOP_WINS_FOR_NO_SELL_LIMIT("50"),
        // Duels of the current difficulty only, or that and all difficulties below it?
        MORE_DUEL_CHOICES("0"),
        WILD_OPPONENTS_MULTIPLIER("2.0"),
        WILD_OPPONENTS_NUMBER("0"),

        // The number of cards to keep before selling
        PLAYSET_SIZE("4"),
        PLAYSET_ANY_NUMBER_SIZE("500"),
        PLAYSET_BASIC_LAND_SIZE("50"),

        ITEM_LEVEL_RESTRICTION("1"),

        SIMULATE_AI_VS_AI_RESULTS("0"),
        DRAFT_ROTATION("0"),
        FOIL_FILTER_DEFAULT("0"),
        RATING_FILTER_DEFAULT("1"),

        // Exclude promos from the random reward pool
        EXCLUDE_PROMOS_FROM_POOL("1");

        private final String strDefaultVal;

        /**
         * Instantiates a new q pref.
         *
         * @param s0
         *            &emsp; {@link java.lang.String}
         */
        QPref(final String s0) {
            this.strDefaultVal = s0;
        }

        /**
         * Gets the default.
         *
         * @return {@link java.lang.String}
         */
        public String getDefault() {
            return this.strDefaultVal;
        }
    }

    public enum DifficultyPrefs {
        STARTING_COMMONS,
        STARTING_UNCOMMONS,
        STARTING_RARES,
        STARTING_CREDITS,
        WINS_BOOSTER,
        WINS_RANKUP,
        WINS_MEDIUMAI,
        WINS_HARDAI,
        WINS_EXPERTAI
    }

    /** Instantiates a QuestPreferences object. */
    public QuestPreferences() {
        super(ForgeConstants.QUEST_PREFS_FILE, QPref.class);
    }

    @Override
    protected QPref[] getEnumValues() {
        return QPref.values();
    }

    @Override
    protected QPref valueOf(final String name) {
        try {
            return QPref.valueOf(name);
        }
        catch (final Exception e) {
            return null;
        }
    }

    @Override
    protected String getPrefDefault(final QPref key) {
        return key.getDefault();
    }

    /**
     * Returns a preference value according to a difficulty index.
     */
    public String getPref(final DifficultyPrefs pref, final int difficultyIndex) {
        String newQPref = pref.toString();

        switch (difficultyIndex) {
        case 0:
            newQPref += "_EASY";
            break;
        case 1:
            newQPref += "_MEDIUM";
            break;
        case 2:
            newQPref += "_HARD";
            break;
        case 3:
            newQPref += "_EXPERT";
            break;
        default:
            throw new IllegalArgumentException(TextUtil.concatNoSpace("Difficulty index ", String.valueOf(difficultyIndex), " out of bounds, preference ", newQPref));
        }

        return getPref(QPref.valueOf(newQPref));
    }

    /**
     * Returns a difficulty-indexed preference value, as an int.
     */
    public int getPrefInt(final DifficultyPrefs pref, final int difficultyIndex) {
        return Integer.parseInt(this.getPref(pref, difficultyIndex));
    }

    public String validatePreference(final QPref qpref, final int val) {
        switch (qpref) {
            case STARTING_POOL_COLOR_BIAS:
                if (val < 1) {
                    return "Bias value too small (minimum 1).";
                } else if (val > 100) {
                    return "Bias value too large (maximum 100).";
                }
                break;
            case DRAFT_ROTATION:
            case SPECIAL_BOOSTERS:
            case FOIL_FILTER_DEFAULT:
            case RATING_FILTER_DEFAULT:
            case ITEM_LEVEL_RESTRICTION:
            case EXCLUDE_PROMOS_FROM_POOL:
                if (val != 0 && val != 1) {
                    return "Only values 0 or 1 are acceptable; 1 for enabled, 0 for disabled.";
                }
                break;

            case SHOP_MAX_PACKS:
            case SHOP_MAX_SELLING_PRICE:
            case SHOP_SELLING_PERCENTAGE_BASE:
            case SHOP_SELLING_PERCENTAGE_MAX:
            case SHOP_WINS_FOR_ADDITIONAL_PACK:
            case PLAYSET_SIZE:
            case PLAYSET_ANY_NUMBER_SIZE:
            case WINS_NEW_CHALLENGE:
            case WINS_NEW_DRAFT:
            case WINS_ROTATE_DRAFT:
            case WINS_UNLOCK_SET:
            case UNLOCK_DISTANCE_MULTIPLIER:
                if (val < 1) {
                    return "Value too small (minimum 1).";
                }
                break;
            case WILD_OPPONENTS_NUMBER:
                if(val < 0 || val > 3) {
                    return Localizer.getInstance().getMessage("lblWildOpponentNumberError");
                }
                break;
            case BOOSTER_COMMONS:
            case BOOSTER_UNCOMMONS:
            case BOOSTER_RARES:
            case PLAYSET_BASIC_LAND_SIZE:
            case STARTING_CREDITS_EASY:
            case STARTING_CREDITS_MEDIUM:
            case STARTING_CREDITS_HARD:
            case STARTING_CREDITS_EXPERT:
            case REWARDS_WINS_MULTIPLIER:
            case REWARDS_WINS_MULTIPLIER_MAX:
            case REWARDS_MILLED:
            case REWARDS_MULLIGAN0:
            case REWARDS_ALTERNATIVE:
            case REWARDS_TURN5:
            case REWARDS_TURN1:
            case REWARDS_HEALTH_DIFF_MAX:
            case SHOP_MIN_PACKS:
            case SHOP_STARTING_PACKS:
            case SHOP_SINGLES_COMMON:
            case SHOP_SINGLES_UNCOMMON:
            case SHOP_SINGLES_RARE:
            case SHOP_WINS_FOR_NO_SELL_LIMIT:
            case MORE_DUEL_CHOICES:
            case UNLIMITED_UNLOCKING:
            default:
                if (val < 0) {
                    return "Value too small (minimum 0).";
                }
                break;
        }
        return null;
    }
}
