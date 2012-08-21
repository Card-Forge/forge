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
package forge.card.spellability;

import java.util.Arrays;

import forge.Card;
import forge.CardCharacteristicName;
import forge.CardList;
import forge.Command;
import forge.card.CardCharacteristics;
import forge.card.trigger.ZCTrigger;

/**
 * <p>
 * Ability_Triggered class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class AbilityTriggered extends Ability implements Command {

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
     *            a {@link forge.card.trigger.ZCTrigger} object.
     */
    public AbilityTriggered(final Card sourceCard, final Command sourceCommand, final ZCTrigger situation) {
        super(sourceCard, "0");
        this.setTrigger(true);
        this.todo = sourceCommand;
        this.trigger = situation;
        if (this.todo instanceof AbilityTriggered) {
            this.setStackDescription(((SpellAbility) this.todo).getStackDescription());
            this.restrictions = ((AbilityTriggered) this.todo).restrictions;
        } else {
            this.setStackDescription("Triggered ability: " + sourceCard + " " + situation);
            if (!sourceCard.isInAlternateState()) {
                this.restrictions = new String[] { "named " + sourceCard.getName() };
            }
            else {
                CardCharacteristics origChar = sourceCard.getState(CardCharacteristicName.Original);
                this.restrictions = new String[] { "named " + origChar.getName() };
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean canPlay() {
        return false;
    } // this is a triggered ability: it cannot be "played"

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
        if (!(o instanceof AbilityTriggered)) {
            return false;
        }
        final AbilityTriggered tmp = (AbilityTriggered) o;
        return tmp.getSourceCard().equals(this.getSourceCard()) && tmp.trigger.equals(this.trigger)
                && tmp.todo.equals(this.todo) && Arrays.equals(tmp.restrictions, this.restrictions);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (41 * (41 + this.getSourceCard().hashCode() + this.trigger.hashCode() + this.todo.hashCode() + this.restrictions
                .hashCode()));
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
