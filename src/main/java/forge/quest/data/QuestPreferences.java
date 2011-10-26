package forge.quest.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;

import forge.properties.ForgeProps;
import forge.properties.NewConstants.QUEST;

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
    private static String[] sDifficulty = {"Easy", "Normal", "Hard", "Very Hard"};

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
        grabPrefsFromFile();
    }

    /**
     * <p>
     * grabPrefsFromFile.
     * </p>
     */
    public static void grabPrefsFromFile() {
        try {
            BufferedReader input = new BufferedReader(new FileReader(ForgeProps.getFile(QUEST.PREFS)));
            String line = null;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }
                String[] split = line.split("=");

                if (split[0].equals("difficultyString")) {
                    setDifficulty(split[1]);
                } else if (split[0].equals("winsForBooster")) {
                    setWinsForBooster(split[1]);
                } else if (split[0].equals("winsForRankIncrease")) {
                    setWinsForRank(split[1]);
                } else if (split[0].equals("winsForMediumAI")) {
                    setWinsForMediumAI(split[1]);
                } else if (split[0].equals("winsForHardAI")) {
                    setWinsForHardAI(split[1]);
                } else if (split[0].equals("startingBasicLand")) {
                    setStartingBasic(split[1]);
                } else if (split[0].equals("startingSnowBasicLand")) {
                    setStartingSnowBasic(split[1]);
                } else if (split[0].equals("startingCommons")) {
                    setStartingCommons(split[1]);
                } else if (split[0].equals("startingUncommons")) {
                    setStartingUncommons(split[1]);
                } else if (split[0].equals("startingRares")) {
                    setStartingRares(split[1]);
                } else if (split[0].equals("startingCredits")) {
                    setStartingCredits(split[1]);
                } else if (split[0].equals("boosterPackCommon")) {
                    setNumCommon(split[1]);
                } else if (split[0].equals("boosterPackUncommon")) {
                    setNumUncommon(split[1]);
                } else if (split[0].equals("boosterPackRare")) {
                    setNumRares(split[1]);
                } else if (split[0].equals("matchRewardBase")) {
                    setMatchRewardBase(split[1]);
                } else if (split[0].equals("matchRewardTotalWins")) {
                    setMatchRewardTotalWins(split[1]);
                } else if (split[0].equals("matchRewardNoLosses")) {
                    setMatchRewardNoLosses(split[1]);
                } else if (split[0].equals("matchRewardMilledWinBonus")) {
                    setMatchRewardMilledWinBonus(split[1]);
                } else if (split[0].equals("matchRewardPoisonWinBonus")) {
                    setMatchRewardPoisonWinBonus(split[1]);
                } else if (split[0].equals("matchRewardAltWinBonus")) {
                    setMatchRewardAltWinBonus(split[1]);
                } else if (split[0].equals("matchRewardWinOnFirstTurn")) {
                    setMatchRewardWinFirst(split[1]);
                } else if (split[0].equals("matchRewardWinByTurnFive")) {
                    setMatchRewardWinByFifth(split[1]);
                } else if (split[0].equals("matchRewardWinByTurnTen")) {
                    setMatchRewardWinByTen(split[1]);
                } else if (split[0].equals("matchRewardWinByTurnFifteen")) {
                    setMatchRewardWinByFifteen(split[1]);
                } else if (split[0].equals("matchRewardMullToZero")) {
                    setMatchMullToZero(split[1]);
                }
            }
        } catch (Exception e) {
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
        return sDifficulty;
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
        return sDifficulty[index];
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
        return winsForBooster[index];
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
        return winsForRankIncrease[index];
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
        return winsForMediumAI[index];
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
        return winsForHardAI[index];
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
        return winsForVeryHardAI[index];
    }

    /**
     * <p>
     * getStartingBasic.
     * </p>
     * 
     * @return a int.
     */
    public static int getStartingBasic() {
        return startingBasicLand;
    }

    /**
     * <p>
     * getStartingSnowBasic.
     * </p>
     * 
     * @return a int.
     */
    public static int getStartingSnowBasic() {
        return startingSnowBasicLand;
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
        return startingCommons[index];
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
        return startingUncommons[index];
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
        return startingRares[index];
    }

    /**
     * <p>
     * Getter for the field <code>startingCredits</code>.
     * </p>
     * 
     * @return a int.
     */
    public static int getStartingCredits() {
        return startingCredits;
    }

    /**
     * <p>
     * getNumCommon.
     * </p>
     * 
     * @return a int.
     */
    public static int getNumCommon() {
        return boosterPackCommon;
    }

    /**
     * <p>
     * getNumUncommon.
     * </p>
     * 
     * @return a int.
     */
    public static int getNumUncommon() {
        return boosterPackUncommon;
    }

    /**
     * <p>
     * getNumRare.
     * </p>
     * 
     * @return a int.
     */
    public static int getNumRare() {
        return boosterPackRare;
    }

    /**
     * <p>
     * Getter for the field <code>matchRewardBase</code>.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardBase() {
        return matchRewardBase;
    }

    /**
     * <p>
     * Getter for the field <code>matchRewardTotalWins</code>.
     * </p>
     * 
     * @return a double.
     */
    public static double getMatchRewardTotalWins() {
        return matchRewardTotalWins;
    }

    /**
     * <p>
     * Getter for the field <code>matchRewardNoLosses</code>.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardNoLosses() {
        return matchRewardNoLosses;
    }

    /**
     * <p>
     * Getter for the field <code>matchRewardPoisonWinBonus</code>.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardPoisonWinBonus() {
        return matchRewardPoisonWinBonus;
    }

    /**
     * <p>
     * Getter for the field <code>matchRewardMilledWinBonus</code>.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardMilledWinBonus() {
        return matchRewardMilledWinBonus;
    }

    /**
     * <p>
     * Getter for the field <code>matchRewardAltWinBonus</code>.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardAltWinBonus() {
        return matchRewardAltWinBonus;
    }

    /**
     * <p>
     * getMatchRewardWinFirst.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardWinFirst() {
        return matchRewardWinOnFirstTurn;
    }

    /**
     * <p>
     * getMatchRewardWinByFifth.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardWinByFifth() {
        return matchRewardWinByTurnFive;
    }

    /**
     * <p>
     * getMatchRewardWinByTen.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardWinByTen() {
        return matchRewardWinByTurnTen;
    }

    /**
     * <p>
     * getMatchRewardWinByFifteen.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchRewardWinByFifteen() {
        return matchRewardWinByTurnFifteen;
    }

    /**
     * <p>
     * getMatchMullToZero.
     * </p>
     * 
     * @return a int.
     */
    public static int getMatchMullToZero() {
        return matchRewardMullToZero;
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
        sDifficulty = diff.split(",");
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
        String[] winsStr = wins.split(",");

        for (int i = 0; i < numDiff; i++) {
            winsForBooster[i] = Integer.parseInt(winsStr[i]);
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
        String[] winsStr = wins.split(",");

        for (int i = 0; i < numDiff; i++) {
            winsForRankIncrease[i] = Integer.parseInt(winsStr[i]);
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
        String[] winsStr = wins.split(",");

        for (int i = 0; i < numDiff; i++) {
            winsForMediumAI[i] = Integer.parseInt(winsStr[i]);
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
        String[] winsStr = wins.split(",");

        for (int i = 0; i < numDiff; i++) {
            winsForHardAI[i] = Integer.parseInt(winsStr[i]);
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
        startingBasicLand = Integer.parseInt(land);
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
        startingSnowBasicLand = Integer.parseInt(land);
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
        String[] splitStr = rarity.split(",");

        for (int i = 0; i < numDiff; i++) {
            startingCommons[i] = Integer.parseInt(splitStr[i]);
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
        String[] splitStr = rarity.split(",");

        for (int i = 0; i < numDiff; i++) {
            startingUncommons[i] = Integer.parseInt(splitStr[i]);
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
        String[] splitStr = rarity.split(",");

        for (int i = 0; i < numDiff; i++) {
            startingRares[i] = Integer.parseInt(splitStr[i]);
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
        startingCredits = Integer.parseInt(credits);
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
        boosterPackCommon = Integer.parseInt(pack);
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
        boosterPackUncommon = Integer.parseInt(pack);
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
        boosterPackRare = Integer.parseInt(pack);
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
        matchRewardBase = Integer.parseInt(match);
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
        matchRewardTotalWins = Double.parseDouble(match);
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
        matchRewardNoLosses = Integer.parseInt(match);
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
        matchRewardPoisonWinBonus = Integer.parseInt(match);
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
        matchRewardMilledWinBonus = Integer.parseInt(match);
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
        matchRewardAltWinBonus = Integer.parseInt(match);
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
        matchRewardWinOnFirstTurn = Integer.parseInt(match);
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
        matchRewardWinByTurnFive = Integer.parseInt(match);
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
        matchRewardWinByTurnTen = Integer.parseInt(match);
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
        matchRewardWinByTurnFifteen = Integer.parseInt(match);
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
        matchRewardMullToZero = Integer.parseInt(match);
    }
}
