package forge.card.spellability;


import forge.Card;
import forge.CardList;
import forge.Command;
import forge.ZCTrigger;

import java.util.Arrays;


/**
 * <p>Ability_Triggered class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Ability_Triggered extends Ability implements Command {


    /**
     *
     */
    private static final long serialVersionUID = 4970998845621323960L;

    public String[] restrictions;
    public ZCTrigger trigger;
    public Command todo;

    /**
     * <p>Constructor for Ability_Triggered.</p>
     *
     * @param sourceCard a {@link forge.Card} object.
     * @param sourceCommand a {@link forge.Command} object.
     * @param situation a {@link forge.ZCTrigger} object.
     */
    public Ability_Triggered(Card sourceCard, Command sourceCommand, ZCTrigger situation) {
        super(sourceCard, "0");
        setTrigger(true);
        todo = sourceCommand;
        trigger = situation;
        if (todo instanceof Ability_Triggered) {
            setStackDescription(((SpellAbility) todo).getStackDescription());
            restrictions = ((Ability_Triggered) todo).restrictions;
        } else {
            setStackDescription("Triggered ability: " + sourceCard + " " + situation);
            restrictions = new String[]{"named " + sourceCard.getName()};
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        return false;
    }//this is a triggered ability: it cannot be "played"

    /** {@inheritDoc} */
    @Override
    public boolean canAfford() {
        return false;
    }//this is a triggered ability: it cannot be "afforded"
    
    /** {@inheritDoc} */
    @Override
    public void resolve() {
        todo.execute();
    }

    /**
     * <p>execute.</p>
     */
    public void execute() {
        resolve();
    }

    /**
     * <p>triggerFor.</p>
     *
     * @param c a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean triggerFor(Card c) {
        return !(new CardList(c)).getValidCards(restrictions, c.getController(), c).isEmpty();
    }

    /**
     * <p>triggerOnZoneChange.</p>
     *
     * @param sourceZone a {@link java.lang.String} object.
     * @param destinationZone a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean triggerOnZoneChange(String sourceZone, String destinationZone) {
        return trigger.triggerOn(sourceZone, destinationZone);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o)//TODO: triggers affecting other cards
    {
        if (!(o instanceof Ability_Triggered)) return false;
        Ability_Triggered tmp = (Ability_Triggered) o;
        return tmp.getSourceCard().equals(getSourceCard()) && tmp.trigger.equals(trigger) && tmp.todo.equals(todo)
                && Arrays.equals(tmp.restrictions, restrictions);
    }

    /**
     * <p>isBasic.</p>
     *
     * @return a boolean.
     */
    public boolean isBasic() {
        return restrictions.length == 1 && restrictions[0].equals("named " + getSourceCard().getName());
    }
}
