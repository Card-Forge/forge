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
package forge.game.card;

import forge.game.spellability.SpellAbility;

/**
 * <p>
 * Card_Shields class.
 * </p>
 * 
 * @author Forge
 * @version $Id: CardShields.java 23786 2013-11-24 06:59:42Z Max mtg $
 */
public class CardShields {
	// restore the regeneration shields
    private final SpellAbility sourceSA;
    private final SpellAbility triggerSA;

    /**
     * Instantiates a new CardShields.
     * 
     * @param sourceSA
     *            a SpellAbility
     */
    public CardShields(final SpellAbility sourceSA, final SpellAbility triggerSA) {
        this.sourceSA = sourceSA;
        this.triggerSA = triggerSA;
    }

    /**
     * 
     * getSourceSA.
     * 
     * @return sourceSA
     */
    public final SpellAbility getSourceSA() {
        return this.sourceSA;
    }

    /**
     * 
     * getTriggerSA.
     * 
     * @return triggerSA
     */
    public final SpellAbility getTriggerSA() {
        return this.triggerSA;
    }

    public final boolean hasTrigger() {
    	return this.triggerSA != null;
    }
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
    	String suffix = this.triggerSA != null ? " - " + triggerSA.getDescription() : "";
        return this.sourceSA.getHostCard().getName() + suffix;
    }
}
