package forge;

/**
 * <p>
 * Computer interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public interface Computer {
    /**
     * <p>
     * main1.
     * </p>
     */
    void main1();

    /**
     * <p>
     * begin_combat.
     * </p>
     */
    void beginCombat();

    /**
     * <p>
     * declare_attackers.
     * </p>
     */
    void declareAttackers();

    /**
     * <p>
     * declare_attackers_after.
     * </p>
     */
    void declareAttackersAfter(); // can play Instants and Abilities

    /**
     * <p>
     * declare_blockers.
     * </p>
     */
    void declareBlockers(); // this is called after when the Human or Computer
                             // blocks

    /**
     * <p>
     * declare_blockers_after.
     * </p>
     */
    void declareBlockersAfter(); // can play Instants and Abilities

    /**
     * <p>
     * end_of_combat.
     * </p>
     */
    void endOfCombat();

    /**
     * <p>
     * main2.
     * </p>
     */
    void main2();

    /**
     * <p>
     * end_of_turn.
     * </p>
     */
    void endOfTurn(); // end of Human's turn

    /**
     * <p>
     * stack_not_empty.
     * </p>
     */
    void stackNotEmpty();

}
