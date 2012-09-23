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

import forge.util.closures.Predicate;

/**
 * <p>
 * Predicate<Card> interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class CardPredicates {
    /**
     * a Predicate<Card> to get all cards that are tapped.
     */
    public static Predicate<Card> TAPPED = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isTapped();
        }
    };

    /**
     * a Predicate<Card> to get all cards that are untapped.
     */
    public static Predicate<Card> UNTAPPED = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isUntapped();
        }
    };

    /**
     * a Predicate<Card> to get all creatures.
     */
    public static Predicate<Card> CREATURES = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isCreature();
        }
    };

    /**
     * a Predicate<Card> to get all enchantments.
     */
    public static Predicate<Card> ENCHANTMENTS = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isEnchantment();
        }
    };

    /**
     * a Predicate<Card> to get all equipment.
     */
    public static Predicate<Card> EQUIPMENT = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isEquipment();
        }
    };

    /**
     * a Predicate<Card> to get all unenchanted cards in a list.
     */
    public static Predicate<Card> UNENCHANTED = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return !c.isEnchanted();
        }
    };

    /**
     * a Predicate<Card> to get all enchanted cards in a list.
     */
    public static Predicate<Card> ENCHANTED = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isEnchanted();
        }
    };

    /**
     * a Predicate<Card> to get all nontoken cards.
     */
    public static Predicate<Card> NON_TOKEN = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return !c.isToken();
        }
    };

    /**
     * a Predicate<Card> to get all token cards.
     */
    public static Predicate<Card> TOKEN = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isToken();
        }
    };

    /**
     * a Predicate<Card> to get all nonbasic lands.
     */
    public static Predicate<Card> NON_BASIC_LAND = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return !c.isBasicLand();
        }
    };

    /**
     * a Predicate<Card> to get all basicLands.
     */
    public static Predicate<Card> BASIC_LANDS = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            // the isBasicLand() check here may be sufficient...
            return c.isLand() && c.isBasicLand();
        }
    };

    /**
     * a Predicate<Card> to get all artifacts.
     */
    public static Predicate<Card> ARTIFACTS = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isArtifact();
        }
    };

    /**
     * a Predicate<Card> to get all nonartifacts.
     */
    public static Predicate<Card> NON_ARTIFACTS = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return !c.isArtifact();
        }
    };

    /**
     * a Predicate<Card> to get all lands.
     */
    public static Predicate<Card> LANDS = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isLand();
        }
    };

    /**
     * a Predicate<Card> to get all nonlands.
     */
    public static Predicate<Card> NON_LANDS = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return !c.isLand();
        }
    };

    /**
     * a Predicate<Card> to get all cards that are black.
     */
    public static Predicate<Card> BLACK = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isBlack();
        }
    };

    /**
     * a Predicate<Card> to get all cards that are blue.
     */
    public static Predicate<Card> BLUE = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isBlue();
        }
    };

    /**
     * a Predicate<Card> to get all cards that are green.
     */
    public static Predicate<Card> GREEN = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isGreen();
        }
    };

    /**
     * a Predicate<Card> to get all cards that are red.
     */
    public static Predicate<Card> RED = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isRed();
        }
    };

    /**
     * a Predicate<Card> to get all cards that are white.
     */
    public static Predicate<Card> WHITE = new Predicate<Card>() {
        @Override
        public boolean isTrue(Card c) {
            return c.isWhite();
        }
    };

}
