package forge.card.spellability;

import java.util.Arrays;

import forge.Card;
import forge.CardList;
import forge.Command;
import forge.ZCTrigger;

/**
 * <p>
 * Ability_Triggered class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Ability_Triggered extends Ability implements Command {

    /**
     *
     */
    private static final long serialVersionUID = 4970998845621323960L;

    /** The restrictions. */
    private String[] restrictions;

    /** The trigger. */
    private ZCTrigger trigger;

    /**
     * Gets the trigger.
     * 
     * @return the trigger
     */
    public final ZCTrigger getTrigger() {
        return this.trigger;
    }

    /**
     * Sets the trigger.
     * 
     * @param trigger
     *            the new trigger
     */
    public final void setTrigger(final ZCTrigger trigger) {
        this.trigger = trigger;
    }

    /** The todo. */
    private final Command todo;

    /**
     * <p>
     * Constructor for Ability_Triggered.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param sourceCommand
     *            a {@link forge.Command} object.
     * @param situation
     *            a {@link forge.ZCTrigger} object.
     */
    public Ability_Triggered(final Card sourceCard, final Command sourceCommand, final ZCTrigger situation) {
        super(sourceCard, "0");
        this.setTrigger(true);
        this.todo = sourceCommand;
        this.trigger = situation;
        if (this.todo instanceof Ability_Triggered) {
            this.setStackDescription(((SpellAbility) this.todo).getStackDescription());
            this.restrictions = ((Ability_Triggered) this.todo).restrictions;
        } else {
            this.setStackDescription("Triggered ability: " + sourceCard + " " + situation);
            this.restrictions = new String[] { "named " + sourceCard.getName() };
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean canPlay() {
        return false;
    } // this is a triggered ability: it cannot be "played"

    /** {@inheritDoc} */
    @Override
    public final boolean canAfford() {
        return false;
    } // this is a triggered ability: it cannot be "afforded"

    /** {@inheritDoc} */
    @Override
    public final void resolve() {
        this.todo.execute();
    }

    /**
     * <p>
     * execute.
     * </p>
     */
    @Override
    public final void execute() {
        this.resolve();
    }

    /**
     * <p>
     * triggerFor.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean triggerFor(final Card c) {
        return !(new CardList(c)).getValidCards(this.restrictions, c.getController(), c).isEmpty();
    }

    /**
     * <p>
     * triggerOnZoneChange.
     * </p>
     * 
     * @param sourceZone
     *            a {@link java.lang.String} object.
     * @param destinationZone
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean triggerOnZoneChange(final String sourceZone, final String destinationZone) {
        return this.trigger.triggerOn(sourceZone, destinationZone);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
     // TODO triggers affecting other
        // cards
        if (!(o instanceof Ability_Triggered)) {
            return false;
        }
        final Ability_Triggered tmp = (Ability_Triggered) o;
        return tmp.getSourceCard().equals(this.getSourceCard()) && tmp.trigger.equals(this.trigger)
                && tmp.todo.equals(this.todo) && Arrays.equals(tmp.restrictions, this.restrictions);
    }

    /**
     * <p>
     * isBasic.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isBasic() {
        return (this.restrictions.length == 1)
                && this.restrictions[0].equals("named " + this.getSourceCard().getName());
    }
}
