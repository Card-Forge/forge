package forge;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardDamageHistory {

    private boolean creatureAttackedThisTurn = false;
    private boolean creatureAttackedLastHumanTurn = false;
    private boolean creatureAttackedLastComputerTurn = false;
    private boolean creatureAttackedThisCombat = false;
    private boolean creatureBlockedThisCombat = false;
    private boolean creatureBlockedThisTurn = false;
    private boolean creatureGotBlockedThisCombat = false;
    private boolean creatureGotBlockedThisTurn = false;
    private boolean dealtDmgToHumanThisTurn = false;
    private boolean dealtDmgToComputerThisTurn = false;
    private boolean dealtDmgToHumanThisGame = false;
    private boolean dealtDmgToComputerThisGame = false;
    private boolean dealtCombatDmgToHumanThisTurn = false;
    private boolean dealtCombatDmgToComputerThisTurn = false;
    // used to see if an attacking creature with a triggering attack ability
    // triggered this phase:
    /**
     * <p>
     * Setter for the field <code>creatureAttackedThisCombat</code>.
     * </p>
     * 
     * @param hasAttacked
     *            a boolean.
     */
    public final void setCreatureAttackedThisCombat(final boolean hasAttacked, final Player controller) {
        this.creatureAttackedThisCombat = hasAttacked;
        if (hasAttacked) {
            this.setCreatureAttackedThisTurn(true);
            controller.setAttackedWithCreatureThisTurn(true);
        }
    }
    /**
     * <p>
     * Getter for the field <code>creatureAttackedThisCombat</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureAttackedThisCombat() {
        return this.creatureAttackedThisCombat;
    }
    /**
     * <p>
     * Setter for the field <code>creatureAttackedThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureAttackedThisTurn(final boolean b) {
        this.creatureAttackedThisTurn = b;
    }
    /**
     * <p>
     * Getter for the field <code>creatureAttackedThisTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureAttackedThisTurn() {
        return this.creatureAttackedThisTurn;
    }
    /**
     * <p>
     * Setter for the field <code>creatureAttackedLastTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureAttackedLastHumanTurn(final boolean b) {
        this.creatureAttackedLastHumanTurn = b;
    }
    /**
     * <p>
     * Getter for the field <code>creatureAttackedLastTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureAttackedLastHumanTurn() {
        return this.creatureAttackedLastHumanTurn;
    }
    /**
     * <p>
     * Setter for the field <code>creatureAttackedLastTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureAttackedLastComputerTurn(final boolean b) {
        this.creatureAttackedLastComputerTurn = b;
    }
    /**
     * <p>
     * Getter for the field <code>creatureAttackedLastTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureAttackedLastComputerTurn() {
        return this.creatureAttackedLastComputerTurn;
    }
    /**
     * <p>
     * Setter for the field <code>creatureBlockedThisCombat</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureBlockedThisCombat(final boolean b) {
        this.creatureBlockedThisCombat = b;
        if (b) {
            this.setCreatureBlockedThisTurn(true);
        }
    }
    /**
     * <p>
     * Getter for the field <code>creatureBlockedThisCombat</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureBlockedThisCombat() {
        return this.creatureBlockedThisCombat;
    }
    /**
     * <p>
     * Setter for the field <code>creatureBlockedThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureBlockedThisTurn(final boolean b) {
        this.creatureBlockedThisTurn = b;
    }
    /**
     * <p>
     * Getter for the field <code>creatureBlockedThisTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureBlockedThisTurn() {
        return this.creatureBlockedThisTurn;
    }
    /**
     * <p>
     * Setter for the field <code>creatureGotBlockedThisCombat</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureGotBlockedThisCombat(final boolean b) {
        this.creatureGotBlockedThisCombat = b;
        if (b) {
            this.setCreatureGotBlockedThisTurn(true);
        }
    }
    /**
     * <p>
     * Getter for the field <code>creatureGotBlockedThisCombat</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureGotBlockedThisCombat() {
        return this.creatureGotBlockedThisCombat;
    }
    /**
     * <p>
     * Setter for the field <code>creatureGotBlockedThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCreatureGotBlockedThisTurn(final boolean b) {
        this.creatureGotBlockedThisTurn = b;
    }
    /**
     * <p>
     * Getter for the field <code>creatureGotBlockedThisTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCreatureGotBlockedThisTurn() {
        return this.creatureGotBlockedThisTurn;
    }
    /**
     * <p>
     * Setter for the field <code>dealtDmgToHumanThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setDealtDmgToHumanThisTurn(final boolean b) {
        this.dealtDmgToHumanThisTurn = b;
        if (b) {
            this.setDealtDmgToHumanThisGame(true);
        }
    }
    /**
     * <p>
     * Getter for the field <code>dealtDmgToHumanThisTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getDealtDmgToHumanThisTurn() {
        return this.dealtDmgToHumanThisTurn;
    }
    /**
     * <p>
     * Setter for the field <code>dealtDmgToComputerThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setDealtDmgToComputerThisTurn(final boolean b) {
        this.dealtDmgToComputerThisTurn = b;
        if (b) {
            this.setDealtDmgToComputerThisGame(true);
        }
    }
    /**
     * <p>
     * Getter for the field <code>dealtCombatDmgToComputerThisGame</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getDealtDmgToComputerThisTurn() {
        return this.dealtDmgToComputerThisTurn;
    }
    /**
     * <p>
     * Setter for the field <code>dealtDmgToHumanThisGame</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setDealtDmgToHumanThisGame(final boolean b) {
        this.dealtDmgToHumanThisGame = b;
    }
    /**
     * <p>
     * Getter for the field <code>dealtDmgToHumanThisGame</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getDealtDmgToHumanThisGame() {
        return this.dealtDmgToHumanThisGame;
    }
    /**
     * <p>
     * Setter for the field <code>dealtDmgToComputerThisGame</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setDealtDmgToComputerThisGame(final boolean b) {
        this.dealtDmgToComputerThisGame = b;
    }
    /**
     * <p>
     * Getter for the field <code>dealtCombatDmgToComputerThisGame</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getDealtDmgToComputerThisGame() {
        return this.dealtDmgToComputerThisGame;
    }
    /**
     * <p>
     * Setter for the field <code>dealtCombatDmgToHumanThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setDealtCombatDmgToHumanThisTurn(final boolean b) {
        this.dealtCombatDmgToHumanThisTurn = b;
    }
    /**
     * <p>
     * Getter for the field <code>dealtDmgToHumanThisTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getDealtCombatDmgToHumanThisTurn() {
        return this.dealtCombatDmgToHumanThisTurn;
    }
    /**
     * <p>
     * Setter for the field <code>dealtCombatDmgToComputerThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setDealtCombatDmgToComputerThisTurn(final boolean b) {
        this.dealtCombatDmgToComputerThisTurn = b;
    }
    /**
     * <p>
     * Getter for the field <code>dealtDmgToComputerThisTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getDealtCombatDmgToComputerThisTurn() {
        return this.dealtCombatDmgToComputerThisTurn;
    }    
    
}
