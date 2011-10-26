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
    public String[] restrictions;

    /** The trigger. */
    public ZCTrigger trigger;

    /** The todo. */
    public Command todo;

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
        setTrigger(true);
        todo = sourceCommand;
        trigger = situation;
        if (todo instanceof Ability_Triggered) {
            setStackDescription(((SpellAbility) todo).getStackDescription());
            restrictions = ((Ability_Triggered) todo).restrictions;
        } else {
            setStackDescription("Triggered ability: " + sourceCard + " " + situation);
            restrictions = new String[] {"named " + sourceCard.getName()};
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
        todo.execute();
    }

    /**
     * <p>
     * execute.
     * </p>
     */
    public final void execute() {
        resolve();
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
        return !(new CardList(c)).getValidCards(restrictions, c.getController(), c).isEmpty();
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
        return trigger.triggerOn(sourceZone, destinationZone);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) // TODO: triggers affecting other cards
    {
        if (!(o instanceof Ability_Triggered)) {
            return false;
        }
        Ability_Triggered tmp = (Ability_Triggered) o;
        return tmp.getSourceCard().equals(getSourceCard()) && tmp.trigger.equals(trigger) && tmp.todo.equals(todo)
                && Arrays.equals(tmp.restrictions, restrictions);
    }

    /**
     * <p>
     * isBasic.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isBasic() {
        return restrictions.length == 1 && restrictions[0].equals("named " + getSourceCard().getName());
    }
}
