package forge.quest.data;

/**
 * <p>QuestMatchState class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class QuestMatchState {
    //the way wins were achieved:
    //Damage
    //Poison Counters
    //Battle of Wits
    //Mortal Combat
    //Milled
    //Felidar Sovereign
    //...
    //
    private String[] winMethods = new String[2];
    private int[] winTurns = new int[2];

    private boolean[] mulliganedToZero = new boolean[2];

    private int win;
    private int lose;
    private boolean winRecently;

    /**
     * <p>reset.</p>
     */
    public void reset() {
        win = 0;
        lose = 0;
        winMethods = new String[2];
    }

    /**
     * <p>addWin.</p>
     */
    public void addWin() {
        win++;
        winRecently = true;
    }

    /**
     * <p>addLose.</p>
     */
    public void addLose() {
        lose++;
        winRecently = false;
    }

    /**
     * <p>Getter for the field <code>win</code>.</p>
     *
     * @return a int.
     */
    public int getWin() {
        return win;
    }

    /**
     * <p>Getter for the field <code>lose</code>.</p>
     *
     * @return a int.
     */
    public int getLose() {
        return lose;
    }

    /**
     * <p>countWinLose.</p>
     *
     * @return a int.
     */
    public int countWinLose() {
        return win + lose;
    }

    /**
     * <p>setWinMethod.</p>
     *
     * @param gameNumber a int.
     * @param method a {@link java.lang.String} object.
     */
    public void setWinMethod(int gameNumber, String method) {
        winMethods[gameNumber] = method;
    }

    /**
     * <p>Getter for the field <code>winMethods</code>.</p>
     *
     * @return an array of {@link java.lang.String} objects.
     */
    public String[] getWinMethods() {
        return winMethods;
    }

    /**
     * <p>setWinTurn.</p>
     *
     * @param gameNumber a int.
     * @param turns a int.
     */
    public void setWinTurn(int gameNumber, int turns) {
        winTurns[gameNumber] = turns;
    }


    /**
     * <p>Getter for the field <code>winTurns</code>.</p>
     *
     * @return an array of int.
     */
    public int[] getWinTurns() {
        return winTurns;
    }

    /**
     * <p>Getter for the field <code>mulliganedToZero</code>.</p>
     *
     * @return an array of boolean.
     */
    public boolean[] getMulliganedToZero() {
        return mulliganedToZero;
    }

    /**
     * <p>Setter for the field <code>mulliganedToZero</code>.</p>
     *
     * @param gameNumber a int.
     * @param b a boolean.
     */
    public void setMulliganedToZero(int gameNumber, boolean b) {
        mulliganedToZero[gameNumber] = b;
    }

    /**
     * <p>didWinRecently.</p>
     *
     * @return a boolean.
     */
    public boolean didWinRecently() {
        return winRecently;
    }
}
