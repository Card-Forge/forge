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
package forge.item;

import java.util.List;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Predicate;
import forge.SetUtils;
import forge.card.BoosterGenerator;
import forge.card.CardRules;
import forge.card.CardSet;

/**
 * TODO Write javadoc for this type.
 * 
 */
public class BoosterPack implements InventoryItemFromSet {

    /** The Constant fnFromSet. */
    public static final Lambda1<BoosterPack, CardSet> FN_FROM_SET = new Lambda1<BoosterPack, CardSet>() {
        @Override
        public BoosterPack apply(final CardSet arg1) {
            return new BoosterPack(arg1);
        }
    };

    private final CardSet cardSet;
    private final String name;

    private List<CardPrinted> cards = null;

    /**
     * Instantiates a new booster pack.
     * 
     * @param set
     *            the set
     */
    public BoosterPack(final String set) {
        this(SetUtils.getSetByCodeOrThrow(set));
    }

    /**
     * Instantiates a new booster pack.
     * 
     * @param set
     *            the set
     */
    public BoosterPack(final CardSet set) {
        this.cardSet = set;
        this.name = this.cardSet.getName() + " Booster Pack";
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItemFromSet#getSet()
     */
    /**
     * Gets the sets the.
     * 
     * @return String
     */
    @Override
    public final String getSet() {
        return this.cardSet.getCode();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItemFromSet#getName()
     */
    /**
     * Gets the name.
     * 
     * @return String
     */
    @Override
    public final String getName() {
        return this.name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItemFromSet#getImageFilename()
     */
    /**
     * Gets the image filename.
     * 
     * @return String
     */
    @Override
    public final String getImageFilename() {
        return "booster/" + this.cardSet.getCode() + ".png";
    }

    private CardPrinted getRandomBasicLand(final CardSet set) {
        return Predicate.and(CardPrinted.Predicates.printedInSets(set.getCode()),
                CardRules.Predicates.Presets.IS_BASIC_LAND, CardPrinted.FN_GET_RULES).random(
                CardDb.instance().getAllCards());
    }

    private CardPrinted getLandFromNearestSet() {
        final List<CardSet> sets = SetUtils.getAllSets();
        final int iThisSet = sets.indexOf(this.cardSet);
        for (int iSet = iThisSet; iSet < sets.size(); iSet++) {
            final CardPrinted land = this.getRandomBasicLand(sets.get(iSet));
            if (null != land) {
                return land;
            }
        }
        // if not found (though that's impossible)
        return this.getRandomBasicLand(SetUtils.getSetByCode("M12"));
    }

    private void generate() {
        final BoosterGenerator gen = new BoosterGenerator(this.cardSet);
        this.cards = gen.getBoosterPack();

        final int cntLands = this.cardSet.getBoosterData().getLand();
        if (cntLands > 0) {
            this.cards.add(this.getLandFromNearestSet());
        }
    }

    /**
     * Gets the cards.
     * 
     * @return the cards
     */
    public final List<CardPrinted> getCards() {
        if (null == this.cards) {
            this.generate();
        }
        return this.cards;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    /**
     * Hash code.
     * 
     * @return int
     */
    @Override
    public final int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((this.cardSet == null) ? 0 : this.cardSet.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        final BoosterPack other = (BoosterPack) obj;
        if (this.cardSet == null) {
            if (other.cardSet != null) {
                return false;
            }
        } else if (!this.cardSet.equals(other.cardSet)) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItem#getType()
     */
    /**
     * Gets the type.
     * 
     * @return String
     */
    @Override
    public final String getType() {
        return "Booster Pack";
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone()
     */
    /**
     * Clone.
     * 
     * @return Object
     */
    @Override
    public final Object clone() {
        return new BoosterPack(this.cardSet); // it's ok to share a reference to
        // cardSet which is static anyway
    }

}
