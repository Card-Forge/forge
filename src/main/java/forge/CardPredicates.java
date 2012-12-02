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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.game.phase.CombatUtil;
import forge.game.phase.Untap;
import forge.game.player.Player;
import forge.util.PredicateString;


/**
 * <p>
 * Predicate<Card> interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class CardPredicates {

    public static final Predicate<Card> isController(final Player p) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getController().equals(p);
            }
        };
    }
    public static final Predicate<Card> isOwner(final Player p) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getOwner().equals(p);
            }
        };
    }

    public static final Predicate<Card> isType(final String cardType) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isType(cardType);
            }
        };
    }

    public static final Predicate<Card> hasKeyword(final String keyword) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.hasKeyword(keyword);
            }
        };
    }

    public static final Predicate<Card> containsKeyword(final String keyword) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return Iterables.any(c.getKeyword(), PredicateString.contains(keyword));
            }
        };
    }

    public static final Predicate<Card> isTargetableBy(final SpellAbility source) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return source.canTarget(c);
            }
        };
    }

    public static final Predicate<Card> nameEquals(final String name) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.getName().equals(name);
            }
        };
    }

    public static final Predicate<Card> possibleBlockers(final Card attacker) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isCreature() && CombatUtil.canBlock(attacker, c);
            }
        };
    }

    public static final Predicate<Card> possibleBlockerForAtLeastOne(final Iterable<Card> attackers) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isCreature() && CombatUtil.canBlockAtLeastOne(c, attackers);
            }
        };
    }

    public static final Predicate<Card> isProtectedFrom(final Card source) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.hasProtectionFrom(source);
            }
        };
    }

    public static final Predicate<Card> restriction(final String[] restrictions, final Player sourceController, final Card source) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return (c != null) && c.isValid(restrictions, sourceController, source);
            }
        };
    }

    public static class Presets {

        /**
         * a Predicate<Card> to get all cards that are tapped.
         */
        public static final Predicate<Card> TAPPED = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isTapped();
            }
        };

        public static final Predicate<Card> CANUNTAP = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return Untap.canUntap(c);
            }
        };
        /**
         * a Predicate<Card> to get all cards that are untapped.
         */
        public static final Predicate<Card> UNTAPPED = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isUntapped();
            }
        };
        /**
         * a Predicate<Card> to get all creatures.
         */
        public static final Predicate<Card> CREATURES = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isCreature();
            }
        };

        /**
         * a Predicate<Card> to get all enchantments.
         */
        public static final Predicate<Card> ENCHANTMENTS = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isEnchantment();
            }
        };
        /**
         * a Predicate<Card> to get all equipment.
         */
        public static final Predicate<Card> EQUIPMENT = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isEquipment();
            }
        };
        /**
         * a Predicate<Card> to get all unenchanted cards in a list.
         */
        public static final Predicate<Card> UNENCHANTED = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return !c.isEnchanted();
            }
        };
        /**
         * a Predicate<Card> to get all enchanted cards in a list.
         */
        public static final Predicate<Card> ENCHANTED = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isEnchanted();
            }
        };
        /**
         * a Predicate<Card> to get all nontoken cards.
         */
        public static final Predicate<Card> NON_TOKEN = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return !c.isToken();
            }
        };
        /**
         * a Predicate<Card> to get all token cards.
         */
        public static final Predicate<Card> TOKEN = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isToken();
            }
        };
        /**
         * a Predicate<Card> to get all basicLands.
         */
        public static final Predicate<Card> BASIC_LANDS = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                // the isBasicLand() check here may be sufficient...
                return c.isLand() && c.isBasicLand();
            }
        };
        /**
         * a Predicate<Card> to get all artifacts.
         */
        public static final Predicate<Card> ARTIFACTS = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isArtifact();
            }
        };
        /**
         * a Predicate<Card> to get all nonartifacts.
         */
        public static final Predicate<Card> NON_ARTIFACTS = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return !c.isArtifact();
            }
        };
        /**
         * a Predicate<Card> to get all lands.
         */
        public static final Predicate<Card> LANDS = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isLand();
            }
        };

        /**
         * a Predicate<Card> to get all cards that are black.
         */
        public static final Predicate<Card> BLACK = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isBlack();
            }
        };
        /**
         * a Predicate<Card> to get all cards that are blue.
         */
        public static final Predicate<Card> BLUE = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isBlue();
            }
        };
        /**
         * a Predicate<Card> to get all cards that are green.
         */
        public static final Predicate<Card> GREEN = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isGreen();
            }
        };
        /**
         * a Predicate<Card> to get all cards that are red.
         */
        public static final Predicate<Card> RED = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isRed();
            }
        };
        /**
         * a Predicate<Card> to get all cards that are white.
         */
        public static final Predicate<Card> WHITE = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isWhite();
            }
        };


        public static final Predicate<Card> hasFirstStrike = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isCreature() && (c.hasFirstStrike() || c.hasDoubleStrike());
            }
        };
        public static final Predicate<Card> hasSecondStrike = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isCreature() && (!c.hasFirstStrike() || c.hasDoubleStrike());
            }
        };
        public static final Predicate<Card> SNOW_LANDS = new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isLand() && c.isSnow();
            }
        };
        public static final Predicate<Card> PLANEWALKERS = new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isPlaneswalker();
            }
        };
    }

    public static class Accessors {
        public static final Function<Card, Integer> fnGetDefense = new Function<Card, Integer>() {
            @Override
            public Integer apply(Card a) {
                return a.getNetDefense();
            }
        };

        public static final Function<Card, Integer> fnGetNetAttack = new Function<Card, Integer>() {
            @Override
            public Integer apply(Card a) {
                return a.getNetAttack();
            }
        };

        public static final Function<Card, Integer> fnGetAttack = new Function<Card, Integer>() {
            @Override
            public Integer apply(Card a) {
                return a.getNetCombatDamage();
            }
        };

        public static final Function<Card, Integer> fnGetCmc = new Function<Card, Integer>() {
            @Override
            public Integer apply(Card a) {
                return a.getCMC();
            }
        };

        public static final Function<Card, Integer> fnEvaluateCreature = new Function<Card, Integer>() {
            @Override
            public Integer apply(Card a) {
                return CardFactoryUtil.evaluateCreature(a);
            }
        };
    }
}
