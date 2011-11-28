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
package forge;

/**
 * <p>
 * CardListFilter interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public interface CardListFilter {
    /**
     * <p>
     * addCard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    boolean addCard(Card c);

    /**
     * a CardListFilter to get all cards that are tapped.
     */
    CardListFilter TAPPED = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isTapped();
        }
    };

    /**
     * a CardListFilter to get all cards that are untapped.
     */
    CardListFilter UNTAPPED = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isUntapped();
        }
    };

    /**
     * a CardListFilter to get all creatures.
     */
    CardListFilter CREATURES = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isCreature();
        }
    };

    /**
     * a CardListFilter to get all enchantments.
     */
    CardListFilter ENCHANTMENTS = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isEnchantment();
        }
    };

    /**
     * a CardListFilter to get all equipment.
     */
    CardListFilter EQUIPMENT = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isEquipment();
        }
    };

    /**
     * a CardListFilter to get all unenchanted cards in a list.
     */
    CardListFilter UNENCHANTED = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return !c.isEnchanted();
        }
    };

    /**
     * a CardListFilter to get all enchanted cards in a list.
     */
    CardListFilter ENCHANTED = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isEnchanted();
        }
    };

    /**
     * a CardListFilter to get all nontoken cards.
     */
    CardListFilter NON_TOKEN = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return !c.isToken();
        }
    };

    /**
     * a CardListFilter to get all token cards.
     */
    CardListFilter TOKEN = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isToken();
        }
    };

    /**
     * a CardListFilter to get all nonbasic lands.
     */
    CardListFilter NON_BASIC_LAND = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return !c.isBasicLand();
        }
    };

    /**
     * a CardListFilter to get all basicLands.
     */
    CardListFilter BASIC_LANDS = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            // the isBasicLand() check here may be sufficient...
            return c.isLand() && c.isBasicLand();
        }
    };

    /**
     * a CardListFilter to get all artifacts.
     */
    CardListFilter ARTIFACTS = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isArtifact();
        }
    };

    /**
     * a CardListFilter to get all nonartifacts.
     */
    CardListFilter NON_ARTIFACTS = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return !c.isArtifact();
        }
    };

    /**
     * a CardListFilter to get all lands.
     */
    CardListFilter LANDS = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isLand();
        }
    };

    /**
     * a CardListFilter to get all nonlands.
     */
    CardListFilter NON_LANDS = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return !c.isLand();
        }
    };

    /**
     * a CardListFilter to get all cards that are black.
     */
    CardListFilter BLACK = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isBlack();
        }
    };

    /**
     * a CardListFilter to get all cards that are blue.
     */
    CardListFilter BLUE = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isBlue();
        }
    };

    /**
     * a CardListFilter to get all cards that are green.
     */
    CardListFilter GREEN = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isGreen();
        }
    };

    /**
     * a CardListFilter to get all cards that are red.
     */
    CardListFilter RED = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isRed();
        }
    };

    /**
     * a CardListFilter to get all cards that are white.
     */
    CardListFilter WHITE = new CardListFilter() {
        @Override
        public boolean addCard(final Card c) {
            return c.isWhite();
        }
    };

}
