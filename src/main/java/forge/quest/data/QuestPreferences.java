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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;

import forge.properties.ForgeProps;
import forge.properties.NewConstants.Quest;

/**
 * <p>
 * QuestPreferences class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestPreferences implements Serializable {
    /** Constant <code>serialVersionUID=3266336025656577905L</code>. */
    private static final long serialVersionUID = 3266336025656577905L;

    /** Constant <code>numDiff=4</code>. */
    private static int numDiff = 4;

    // Descriptive difficulty names
    /** Constant <code>sDifficulty="{Easy, Normal, Hard, Very Hard}"</code>. */
    private static String[] sDifficulty = { "Easy", "Normal", "Hard", "Very Hard" };

    // Default match wins it takes to gain a booster
    /** Constant <code>winsForBooster={1, 1, 2, 2}</code>. */
    private static int[] winsForBooster = { 1, 1, 2, 2 };
    /** Constant <code>winsForRankIncrease={1, 2, 3, 4}</code>. */
    private static int[] winsForRankIncrease = { 1, 2, 3, 4 };
    /** Constant <code>winsForMediumAI={6, 6, 11, 11}</code>. */
    private static int[] winsForMediumAI = { 6, 6, 11, 11 };
    /** Constant <code>winsForHardAI={9, 9, 21, 21}</code>. */
    private static int[] winsForHardAI = { 9, 9, 21, 21 };
    /** Constant <code>winsForVeryHardAI={29, 29, 31, 31}</code>. */
    private static int[] winsForVeryHardAI = { 29, 29, 31, 31 };

    // Default starting land for a quest
    /** Constant <code>startingBasicLand=20</code>. */
    private static int startingBasicLand = 20;
    /** Constant <code>startingSnowBasicLand=20</code>. */
    private static int startingSnowBasicLand = 20;

    // Default starting amount of each rarity
    /** Constant <code>startingCommons={45, 40, 40, 40}</code>. */
    private static int[] startingCommons = { 45, 40, 40, 40 };
    /** Constant <code>startingUncommons={20, 15, 15, 15}</code>. */
    private static int[] startingUncommons = { 20, 15, 15, 15 };
    /** Constant <code>startingRares={10, 10, 10, 10}</code>. */
    private static int[] startingRares = { 10, 10, 10, 10 };

    /** Constant <code>startingCredits=250</code>. */
    private static int startingCredits = 250;

    /** Constant <code>boosterPackRare=1</code>. */
    private static int boosterPackRare = 1;
    /** Constant <code>boosterPackUncommon=3</code>. */
    private static int boosterPackUncommon = 3;
    /** Constant <code>boosterPackCommon=9</code>. */
    private static int boosterPackCommon = 9;

    /** Constant <code>matchRewardBase=10</code>. */
    private static int matchRewardBase = 10;
    /** Constant <code>matchRewardTotalWins=0.3</code>. */
    private static double matchRewardTotalWins = 0.3;
    /** Constant <code>matchRewardNoLosses=10</code>. */
    private static int matchRewardNoLosses = 10;

    /** Constant <code>matchRewardPoisonWinBonus=50</code>. */
    private static int matchRewardPoisonWinBonus = 50;
    /** Constant <code>matchRewardMilledWinBonus=40</code>. */
    private static int matchRewardMilledWinBonus = 40;
    /** Constant <code>matchRewardAltWinBonus=100</code>. */
    private static int matchRewardAltWinBonus = 100;

    /** Constant <code>matchRewardWinOnFirstTurn=1500</code>. */
    private static int matchRewardWinOnFirstTurn = 1500;
    /** Constant <code>matchRewardWinByTurnFive=250</code>. */
    private static int matchRewardWinByTurnFive = 250;
    /** Constant <code>matchRewardWinByTurnTen=50</code>. */
    private static int matchRewardWinByTurnTen = 50;
    /** Constant <code>matchRewardWinByTurnFifteen=5</code>. */
    private static int matchRewardWinByTurnFifteen = 5;
    /** Constant <code>matchRewardMullToZero=500</code>. */
    private static int matchRewardMullToZero = 500;

    static {
        // if quest.prefs exists
        QuestPreferences.grabPrefsFromFile();
    }

    /**
     * <p>
     * grabPrefsFromFile.
     * </p>
     */
    public static void grabPrefsFromFile() {
        try {
            final BufferedReader input = new BufferedReader(new FileReader(ForgeProps.getFile(Quest.PREFS)));
            String line = null;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("#") || (line.length() == 0)) {
                    continue;
                }
                final String[] split = line.split("=");

                if (split[0].equals("difficultyString")) {
                    QuestPreferences.setDifficulty(split[1]);
                } else if (split[0].equals("winsForBooster")) {
                    QuestPreferences.setWinsForBooster(split[1]);
                } else if (split[0].equals("winsForRankIncrease")) {
                    QuestPreferences.setWinsForRank(split[1]);
                } else if (split[0].equals("winsForMediumAI")) {
                    QuestPreferences.setWinsForMediumAI(split[1]);
                } else if (split[0].equals("winsForHardAI")) {
                    QuestPreferences.setWinsForHardAI(split[1]);
                } else if (split[0].equals("startingBasicLand")) {
                    QuestPreferences.setStartingBasic(split[1]);
                } else if (split[0].equals("startingSnowBasicLand")) {
                    QuestPreferences.setStartingSnowBasic(split[1]);
                } else if (split[0].equals("startingCommons")) {
                    QuestPreferences.setStartingCommons(split[1]);
                } else if (split[0].equals("startingUncommons")) {
                    QuestPreferences.setStartingUncommons(split[1]);
                } else if (split[0].equals("startingRares")) {
                    QuestPreferences.setStartingRares(split[1]);
                } else if (split[0].equals("startingCredits")) {
                    QuestPreferences.setStartingCredits(split[1]);
                } else if (split[0].equals("boosterPackCommon")) {
                    QuestPreferences.setNumCommon(split[1]);
                } else if (split[0].equals("boosterPackUncommon")) {
                    QuestPreferences.setNumUncommon(split[1]);
                } else if (split[0].equals("boosterPackRare")) {
                    QuestPreferences.setNumRares(split[1]);
                } else if (split[0].equals("matchRewardBase")) {
                    QuestPreferences.setMatchRewardBase(split[1]);
                } else if (split[0].equals("matchRewardTotalWins")) {
                    QuestPreferences.setMatchRewardTotalWins(split[1]);
                } else if (split[0].equals("matchRewardNoLosses")) {
                    QuestPreferences.setMatchRewardNoLosses(split[1]);
                } else if (split[0].equals("matchRewardMilledWinBonus")) {
                    QuestPreferences.setMatchRewardMilledWinBonus(split[1]);
                } else if (split[0].equals("matchRewardPoisonWinBonus")) {
                    QuestPreferences.setMatchRewardPoisonWinBonus(split[1]);
                } else if (split[0].equals("matchRewardAltWinBonus")) {
                    QuestPreferences.setMatchRewardAltWinBonus(split[1]);
                } else if (split[0].equals("matchRewardWinOnFirstTurn")) {
                    QuestPreferences.setMatchRewardWinFirst(split[1]);
                } else if (split[0].equals("matchRewardWinByTurnFive")) {
                    QuestPreferences.setMatchRewardWinByFifth(split[1]);
                } else if (split[0].equals("matchRewardWinByTurnTen")) {
                    QuestPreferences.setMatchRewardWinByTen(split[1]);
                } else if (split[0].equals("matchRewardWinByTurnFifteen")) {
                    QuestPreferences.setMatchRewardWinByFifteen(split[1]);
                } else if (split[0].equals("matchRewardMullToZero")) {
                    QuestPreferences.setMatchMullToZero(split[1]);
                }
            }
        } catch (final Exception e) {
            System.out.println("Trouble grabbing quest data preferences. Using default values.");
        }
    }

    /**
     * <p>
     * getDifficulty.
     * </p>
     * 
     * @return an array of {@link java.lang.String} objects.
     */
    public static String[] getDifficulty() {
        return QuestPreferences.sDifficulty;
    }

    /**
     * <p>
     * getDifficulty.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a {@link java.lang.String} object.
     */
    public static String getDifficulty(final int index) {
        return QuestPreferences.sDifficulty[index];
    }

    /**
     * <p>
     * Getter for the field <code>winsForBooster</code>.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a int.
     */
    public static int getWinsForBooster(final int index) {
        return QuestPreferences.winsForBooster[index];
    }

    /**
     * <p>
     * Getter for the field <code>winsForRankIncrease</code>.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a int.
     */
    public static int getWinsForRankIncrease(final int index) {
        return QuestPreferences.winsForRankIncrease[index];
    }

    /**
     * <p>
     * Getter for the field <code>winsForMediumAI</code>.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a int.
     */
    public static int getWinsForMediumAI(final int index) {
        return QuestPreferences.winsForMediumAI[index];
    }

    /**
     * <p>
     * Getter for the field <code>winsForHardAI</code>.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a int.
     */
    public static int getWinsForHardAI(final int index) {
        return QuestPreferences.winsForHardAI[index];
    }

    /**
     * <p>
     * Getter for the field <code>winsForVeryHardAI</code>.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a int.
     */
    public static int getWinsForVeryHardAI(final int index) {
        return QuestPreferences.winsForVeryHardAI[index];
    }

    /**
     * <p>
     * getStartingBasic.
     * </p>
     * 
     * @return a int.
     */
    public static int getStartingBasic() {
        return QuestPreferences.startingBasicLand;
    }

    /**
     * <p>
     * getStartingSnowBasic.
     * </p>
     * 
     * @return a int.
     */
    public static int getStartingSnowBasic() {
        return QuestPreferences.startingSnowBasicLand;
    }

    /**
     * <p>
     * Getter for the field <code>startingCommons</code>.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a int.
     */
    public static int getStartingCommons(final int index) {
        return QuestPreferences.startingCommons[index];
    }

    /**
     * <p>
     * Getter for the field <code>startingUncommons</code>.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a int.
     */
    public static int getStartingUncommons(final int index) {
        return QuestPreferences.startingUncommons[index];
    }

    /**
     * <p>
     * Getter for the field <code>startingRares</code>.
     * </p>
     * 
     * @param index
     *            a int.
     * @return a int.
     */
    public static int getStartingRares(final int index) {
        return QuestPreferences.startingRares[index];
    }

    /**
     * <p>
     * Getter for the field <code>startingCredits</code>.
     * </p>
     * 
     * @return a int.
     */
    public static int getStartingCredits() {
        return QuestPreferences.startingCredits;
    }

    /**
     * <p>
     * getNumCommon.
     * </p>
     * 
     * @return a int.
     */
    public static int getNumCommon() {
        return QuestPreferences.boosterPackCommon;
    }

    /**
     * <p>
     * getNumUncommon.
     * </p>
     * 
     * @return a int.
     */
    public static int getNumUncommon() {
        return QuestPreferences.boosterPackUncommon;
    }

    /**
     * <p>
     * getNumRare.
     * </p>
     * 
     * @return a int.
     */
    public static int getNumRare() {
        return QuestPreferences.boosterPackRare;
    }

    /**
     * <p>
     * Getter for the field <code>matchRewardBase</code>.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardBase() {
        return QuestPreferences.matchRewardBase;
    }

    /**
     * <p>
     * Getter for the field <code>matchRewardTotalWins</code>.
     * </p>
     * 
     * @return a double.
     */
    public static double getMatchRewardTotalWins() {
        return QuestPreferences.matchRewardTotalWins;
    }

    /**
     * <p>
     * Getter for the field <code>matchRewardNoLosses</code>.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardNoLosses() {
        return QuestPreferences.matchRewardNoLosses;
    }

    /**
     * <p>
     * Getter for the field <code>matchRewardPoisonWinBonus</code>.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardPoisonWinBonus() {
        return QuestPreferences.matchRewardPoisonWinBonus;
    }

    /**
     * <p>
     * Getter for the field <code>matchRewardMilledWinBonus</code>.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardMilledWinBonus() {
        return QuestPreferences.matchRewardMilledWinBonus;
    }

    /**
     * <p>
     * Getter for the field <code>matchRewardAltWinBonus</code>.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardAltWinBonus() {
        return QuestPreferences.matchRewardAltWinBonus;
    }

    /**
     * <p>
     * getMatchRewardWinFirst.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardWinFirst() {
        return QuestPreferences.matchRewardWinOnFirstTurn;
    }

    /**
     * <p>
     * getMatchRewardWinByFifth.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardWinByFifth() {
        return QuestPreferences.matchRewardWinByTurnFive;
    }

    /**
     * <p>
     * getMatchRewardWinByTen.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardWinByTen() {
        return QuestPreferences.matchRewardWinByTurnTen;
    }

    /**
     * <p>
     * getMatchRewardWinByFifteen.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardWinByFifteen() {
        return QuestPreferences.matchRewardWinByTurnFifteen;
    }

    /**
     * <p>
     * getMatchMullToZero.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchMullToZero() {
        return QuestPreferences.matchRewardMullToZero;
    }

    // setters
    /**
     * <p>
     * setDifficulty.
     * </p>
     * 
     * @param diff
     *            a {@link java.lang.String} object.
     */
    public static void setDifficulty(final String diff) {
        QuestPreferences.sDifficulty = diff.split(",");
    }

    /**
     * <p>
     * Setter for the field <code>winsForBooster</code>.
     * </p>
     * 
     * @param wins
     *            a {@link java.lang.String} object.
     */
    public static void setWinsForBooster(final String wins) {
        final String[] winsStr = wins.split(",");

        for (int i = 0; i < QuestPreferences.numDiff; i++) {
            QuestPreferences.winsForBooster[i] = Integer.parseInt(winsStr[i]);
        }
    }

    /**
     * <p>
     * setWinsForRank.
     * </p>
     * 
     * @param wins
     *            a {@link java.lang.String} object.
     */
    public static void setWinsForRank(final String wins) {
        final String[] winsStr = wins.split(",");

        for (int i = 0; i < QuestPreferences.numDiff; i++) {
            QuestPreferences.winsForRankIncrease[i] = Integer.parseInt(winsStr[i]);
        }
    }

    /**
     * <p>
     * Setter for the field <code>winsForMediumAI</code>.
     * </p>
     * 
     * @param wins
     *            a {@link java.lang.String} object.
     */
    public static void setWinsForMediumAI(final String wins) {
        final String[] winsStr = wins.split(",");

        for (int i = 0; i < QuestPreferences.numDiff; i++) {
            QuestPreferences.winsForMediumAI[i] = Integer.parseInt(winsStr[i]);
        }
    }

    /**
     * <p>
     * Setter for the field <code>winsForHardAI</code>.
     * </p>
     * 
     * @param wins
     *            a {@link java.lang.String} object.
     */
    public static void setWinsForHardAI(final String wins) {
        final String[] winsStr = wins.split(",");

        for (int i = 0; i < QuestPreferences.numDiff; i++) {
            QuestPreferences.winsForHardAI[i] = Integer.parseInt(winsStr[i]);
        }
    }

    /**
     * <p>
     * setStartingBasic.
     * </p>
     * 
     * @param land
     *            a {@link java.lang.String} object.
     */
    public static void setStartingBasic(final String land) {
        QuestPreferences.startingBasicLand = Integer.parseInt(land);
    }

    /**
     * <p>
     * setStartingSnowBasic.
     * </p>
     * 
     * @param land
     *            a {@link java.lang.String} object.
     */
    public static void setStartingSnowBasic(final String land) {
        QuestPreferences.startingSnowBasicLand = Integer.parseInt(land);
    }

    /**
     * <p>
     * Setter for the field <code>startingCommons</code>.
     * </p>
     * 
     * @param rarity
     *            a {@link java.lang.String} object.
     */
    public static void setStartingCommons(final String rarity) {
        final String[] splitStr = rarity.split(",");

        for (int i = 0; i < QuestPreferences.numDiff; i++) {
            QuestPreferences.startingCommons[i] = Integer.parseInt(splitStr[i]);
        }
    }

    /**
     * <p>
     * Setter for the field <code>startingUncommons</code>.
     * </p>
     * 
     * @param rarity
     *            a {@link java.lang.String} object.
     */
    public static void setStartingUncommons(final String rarity) {
        final String[] splitStr = rarity.split(",");

        for (int i = 0; i < QuestPreferences.numDiff; i++) {
            QuestPreferences.startingUncommons[i] = Integer.parseInt(splitStr[i]);
        }
    }

    /**
     * <p>
     * Setter for the field <code>startingRares</code>.
     * </p>
     * 
     * @param rarity
     *            a {@link java.lang.String} object.
     */
    public static void setStartingRares(final String rarity) {
        final String[] splitStr = rarity.split(",");

        for (int i = 0; i < QuestPreferences.numDiff; i++) {
            QuestPreferences.startingRares[i] = Integer.parseInt(splitStr[i]);
        }
    }

    /**
     * <p>
     * Setter for the field <code>startingCredits</code>.
     * </p>
     * 
     * @param credits
     *            a {@link java.lang.String} object.
     */
    public static void setStartingCredits(final String credits) {
        QuestPreferences.startingCredits = Integer.parseInt(credits);
    }

    /**
     * <p>
     * setNumCommon.
     * </p>
     * 
     * @param pack
     *            a {@link java.lang.String} object.
     */
    public static void setNumCommon(final String pack) {
        QuestPreferences.boosterPackCommon = Integer.parseInt(pack);
    }

    /**
     * <p>
     * setNumUncommon.
     * </p>
     * 
     * @param pack
     *            a {@link java.lang.String} object.
     */
    public static void setNumUncommon(final String pack) {
        QuestPreferences.boosterPackUncommon = Integer.parseInt(pack);
    }

    /**
     * <p>
     * setNumRares.
     * </p>
     * 
     * @param pack
     *            a {@link java.lang.String} object.
     */
    public static void setNumRares(final String pack) {
        QuestPreferences.boosterPackRare = Integer.parseInt(pack);
    }

    /**
     * <p>
     * Setter for the field <code>matchRewardBase</code>.
     * </p>
     * 
     * @param match
     *            a {@link java.lang.String} object.
     */
    public static void setMatchRewardBase(final String match) {
        QuestPreferences.matchRewardBase = Integer.parseInt(match);
    }

    /**
     * <p>
     * Setter for the field <code>matchRewardTotalWins</code>.
     * </p>
     * 
     * @param match
     *            a {@link java.lang.String} object.
     */
    public static void setMatchRewardTotalWins(final String match) {
        QuestPreferences.matchRewardTotalWins = Double.parseDouble(match);
    }

    /**
     * <p>
     * Setter for the field <code>matchRewardNoLosses</code>.
     * </p>
     * 
     * @param match
     *            a {@link java.lang.String} object.
     */
    public static void setMatchRewardNoLosses(final String match) {
        QuestPreferences.matchRewardNoLosses = Integer.parseInt(match);
    }

    /**
     * <p>
     * Setter for the field <code>matchRewardPoisonWinBonus</code>.
     * </p>
     * 
     * @param match
     *            a {@link java.lang.String} object.
     */
    public static void setMatchRewardPoisonWinBonus(final String match) {
        QuestPreferences.matchRewardPoisonWinBonus = Integer.parseInt(match);
    }

    /**
     * <p>
     * Setter for the field <code>matchRewardMilledWinBonus</code>.
     * </p>
     * 
     * @param match
     *            a {@link java.lang.String} object.
     */
    public static void setMatchRewardMilledWinBonus(final String match) {
        QuestPreferences.matchRewardMilledWinBonus = Integer.parseInt(match);
    }

    /**
     * <p>
     * Setter for the field <code>matchRewardAltWinBonus</code>.
     * </p>
     * 
     * @param match
     *            a {@link java.lang.String} object.
     */
    public static void setMatchRewardAltWinBonus(final String match) {
        QuestPreferences.matchRewardAltWinBonus = Integer.parseInt(match);
    }

    /**
     * <p>
     * setMatchRewardWinFirst.
     * </p>
     * 
     * @param match
     *            a {@link java.lang.String} object.
     */
    public static void setMatchRewardWinFirst(final String match) {
        QuestPreferences.matchRewardWinOnFirstTurn = Integer.parseInt(match);
    }

    /**
     * <p>
     * setMatchRewardWinByFifth.
     * </p>
     * 
     * @param match
     *            a {@link java.lang.String} object.
     */
    public static void setMatchRewardWinByFifth(final String match) {
        QuestPreferences.matchRewardWinByTurnFive = Integer.parseInt(match);
    }

    /**
     * <p>
     * setMatchRewardWinByTen.
     * </p>
     * 
     * @param match
     *            a {@link java.lang.String} object.
     */
    public static void setMatchRewardWinByTen(final String match) {
        QuestPreferences.matchRewardWinByTurnTen = Integer.parseInt(match);
    }

    /**
     * <p>
     * setMatchRewardWinByFifteen.
     * </p>
     * 
     * @param match
     *            a {@link java.lang.String} object.
     */
    public static void setMatchRewardWinByFifteen(final String match) {
        QuestPreferences.matchRewardWinByTurnFifteen = Integer.parseInt(match);
    }

    /**
     * <p>
     * setMatchMullToZero.
     * </p>
     * 
     * @param match
     *            a {@link java.lang.String} object.
     */
    public static void setMatchMullToZero(final String match) {
        QuestPreferences.matchRewardMullToZero = Integer.parseInt(match);
    }
}
