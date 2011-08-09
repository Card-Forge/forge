package forge;


/**
 * <p>Display interface.</p>
 *
 * @author Forge
 * @version $Id$
 */
public interface Display {
    /**
     * <p>showMessage.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void showMessage(String s);

    /**
     * <p>getButtonOK.</p>
     *
     * @return a {@link forge.MyButton} object.
     */
    public MyButton getButtonOK();

    /**
     * <p>getButtonCancel.</p>
     *
     * @return a {@link forge.MyButton} object.
     */
    public MyButton getButtonCancel();

    //    public void showStatus(String message);
    /**
     * <p>showCombat.</p>
     *
     * @param message a {@link java.lang.String} object.
     */
    public void showCombat(String message);

    /**
     * <p>setVisible.</p>
     *
     * @param b a boolean.
     */
    public void setVisible(boolean b);

    //assigns combat damage, used by Combat.setAssignedDamage()
    /**
     * <p>assignDamage.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @param blockers a {@link forge.CardList} object.
     * @param damage a int.
     */
    public void assignDamage(Card attacker, CardList blockers, int damage);
    //public void addAssignDamage(Card attacker, Card blocker, int damage);
    //public void addAssignDamage(Card attacker, int damage);

    /**
     * <p>stopAtPhase.</p>
     *
     * @param turn a {@link forge.Player} object.
     * @param phase a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean stopAtPhase(Player turn, String phase);

    /**
     * <p>loadPrefs.</p>
     *
     * @return a boolean.
     */
    public boolean loadPrefs();

    /**
     * <p>savePrefs.</p>
     *
     * @return a boolean.
     */
    public boolean savePrefs();

    /**
     * <p>canLoseByDecking.</p>
     *
     * @return a boolean.
     */
    public boolean canLoseByDecking();

    /**
     * <p>setCard.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void setCard(Card c);
}
