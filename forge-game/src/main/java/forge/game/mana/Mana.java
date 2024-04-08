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
package forge.game.mana;

import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.card.Card;
import forge.game.card.CardCopyService;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;

/**
 * <p>
 * Mana class.
 * This represents a single mana 'globe' floating in a player's pool.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Mana {

    private byte color;
    private Card sourceCard = null;
    private AbilityManaPart manaAbility = null;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + color;
        result = prime * result + ((manaAbility == null) ? 0 : manaAbility.hashCode());
        result = prime * result + ((sourceCard == null) ? 0 : sourceCard.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Mana)) {
            return false;
        }
        Mana m2 = (Mana) other;

        if (color != m2.color) {
            return false;
        }

        AbilityManaPart mp = this.getManaAbility();
        AbilityManaPart mp2 = m2.getManaAbility();
        if ((mp == null) != (mp2 == null)) {
            return false;
        }

        if (!sourceCard.equals(m2.sourceCard) && mp != null) {
            if (addsKeywords(null) != m2.addsKeywords(null)) {
                return false;
            }
            if (addsCounters(null) != m2.addsCounters(null)) {
                return false;
            }
            if (mp.isCannotCounterPaidWith() != mp2.isCannotCounterPaidWith()) {
                return false;
            }
            if (mp.getTriggersWhenSpent() != mp2.getTriggersWhenSpent()) {
                return false;
            }
            if (mp.isPersistentMana() != mp2.isPersistentMana()) {
                return false;
            }
        }

        return mp == mp2 || (mp.getManaRestrictions().equals(mp2.getManaRestrictions()) && mp.getExtraManaRestriction().equals(mp2.getExtraManaRestriction()));
    }

    public Mana(final byte color, final Card source, final AbilityManaPart manaAbility) {
        this.color = color;
        this.manaAbility = manaAbility;
        this.sourceCard = source.isInPlay() ? CardCopyService.getLKICopy(source) : source.getGame().getChangeZoneLKIInfo(source);
    }

    @Override
    public final String toString() {
        return MagicColor.toShortString(color);
    }

    public final boolean isSnow() {
        return this.sourceCard.isSnow();
    }

    public final boolean isRestricted() {
        return this.manaAbility != null && (!manaAbility.getManaRestrictions().isEmpty() || !manaAbility.getExtraManaRestriction().isEmpty());
    }

    public final boolean addsNoCounterMagic(SpellAbility saBeingPaid) {
        return this.manaAbility != null && manaAbility.cannotCounterPaidWith(saBeingPaid);
    }

    public final boolean addsCounters(SpellAbility saBeingPaid) {
        return this.manaAbility != null && manaAbility.addsCounters(saBeingPaid);
    }

    public final boolean addsKeywords(SpellAbility saBeingPaid) {
        return this.manaAbility != null && manaAbility.addKeywords(saBeingPaid);
    }

    public final boolean addsKeywordsType() {
        return this.manaAbility != null && manaAbility.getAddsKeywordsType() != null;
    }
    
    public final boolean addsKeywordsUntil() {
        return this.manaAbility != null && manaAbility.getAddsKeywordsUntil() != null;
    }

    public final String getAddedKeywords() {
        return this.manaAbility.getKeywords();
    }

    public final boolean triggersWhenSpent() {
        return this.manaAbility != null && manaAbility.getTriggersWhenSpent();
    }

    public final byte getColor() {
        return this.color;
    }

    public final Card getSourceCard() {
        return this.sourceCard;
    }

    public final AbilityManaPart getManaAbility() {
        return this.manaAbility;
    }

    public boolean isColorless() {
        return color == (byte)ManaAtom.COLORLESS;
    }

}
