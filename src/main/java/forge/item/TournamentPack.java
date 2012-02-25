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
import forge.Singletons;
import forge.card.BoosterData;
import forge.card.BoosterGenerator;
import forge.card.CardEdition;

/**
 * TODO Write javadoc for this type.
 * 
 */
public class TournamentPack implements InventoryItemFromSet {

    /** The Constant fnFromSet. */
    public static final Lambda1<TournamentPack, CardEdition> FN_FROM_SET = new Lambda1<TournamentPack, CardEdition>() {
        @Override
        public TournamentPack apply(final CardEdition arg1) {
            BoosterData d = Singletons.getModel().getTournamentPacks().get(arg1.getCode());
            return new TournamentPack(arg1.getName(), d);
        }
    };

    private final BoosterData contents;
    private final String name;

    private List<CardPrinted> cards = null;


    /**
     * Instantiates a new booster pack.
     * 
     * @param set
     *            the set
     */
    public TournamentPack(final String name0, final BoosterData boosterData) {
        this.contents = boosterData;
        this.name = name0;
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
    public final String getEdition() {
        return this.contents.getEdition();
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
        return "tournamentpacks/" + this.contents.getEdition() + ".png";
    }

    private void generate() {
        final BoosterGenerator gen = new BoosterGenerator(this.contents.getEditionFilter());
        this.cards = gen.getBoosterPack(this.contents);
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
        result = (prime * result) + ((this.contents == null) ? 0 : this.contents.hashCode());
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
        final TournamentPack other = (TournamentPack) obj;
        if (this.contents == null) {
            if (other.contents != null) {
                return false;
            }
        } else if (!this.contents.equals(other.contents)) {
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
        return "Tournament Pack";
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
        return new TournamentPack(name, contents); 
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public int getTotalCards() {
        return contents.getTotal();
    }

}
