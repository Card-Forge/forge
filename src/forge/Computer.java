package forge;

/**
 * <p>Computer interface.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public interface Computer {
    /**
     * <p>main1.</p>
     */
    public void main1();

    /**
     * <p>begin_combat.</p>
     */
    public void begin_combat();

    /**
     * <p>declare_attackers.</p>
     */
    public void declare_attackers();

    /**
     * <p>declare_attackers_after.</p>
     */
    public void declare_attackers_after(); //can play Instants and Abilities

    /**
     * <p>declare_blockers.</p>
     */
    public void declare_blockers();//this is called after when the Human or Computer blocks

    /**
     * <p>declare_blockers_after.</p>
     */
    public void declare_blockers_after();//can play Instants and Abilities

    /**
     * <p>end_of_combat.</p>
     */
    public void end_of_combat();

    /**
     * <p>main2.</p>
     */
    public void main2();

    /**
     * <p>end_of_turn.</p>
     */
    public void end_of_turn();//end of Human's turn

    /**
     * <p>stack_not_empty.</p>
     */
    public void stack_not_empty();

}
