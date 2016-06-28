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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.game.combat.CombatUtil;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.collect.FCollectionView;
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
    public static final Predicate<Card> isControlledByAnyOf(final FCollectionView<Player> pList) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return pList.contains(c.getController());
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
                return c.getType().hasStringType(cardType);
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
                return Iterables.any(c.getKeywords(), PredicateString.contains(keyword));
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

    public static final Predicate<Card> sharesNameWith(final Card name) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.sharesNameWith(name);
            }
        };
    }

    public static final Predicate<Card> sharesCMCWith(final Card cmc) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.sharesCMCWith(cmc);
            }
        };
    }

    public static final Predicate<Card> sharesColorWith(final Card color) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.sharesColorWith(color);
            }
        };
    }

    public static final Predicate<Card> sharesControllerWith(final Card color) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.sharesControllerWith(color);
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

    public static final Predicate<Card> restriction(final String[] restrictions, final Player sourceController, final Card source, final SpellAbility spellAbility) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return (c != null) && c.isValid(restrictions, sourceController, source, spellAbility);
            }
        };
    }

    public static final Predicate<Card> canBeSacrificedBy(final SpellAbility sa) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.canBeSacrificedBy(sa);
            }
        };
    };

    public static final Predicate<Card> isColor(final byte color) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return CardUtil.getColors(c).hasAnyColor(color);
            }
        };
    } // getColor()

    public static final Predicate<Card> isEquippedBy(final String name) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isEquippedBy(name);
            }
        };
    }

    public static final Predicate<Card> isEnchantedBy(final String name) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isEnchantedBy(name);
            }
        };
    }

    public static final Predicate<Card> hasCMC(final int cmc) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getCMC() == cmc;
            }
        };
    }

    public static final Predicate<Card> hasCounter(final CounterType type) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getCounters(type) > 0;
            }
        };
    }

    public static final Predicate<Card> hasGreaterPowerThan(final int minPower) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getNetPower() > minPower;
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

        public static final Predicate<Card> FACE_DOWN = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isFaceDown();
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
         * a Predicate<Card> to get all fortification.
         */
        public static final Predicate<Card> Fortification = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isFortification();
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
        public static final Predicate<Card> CAN_BE_DESTROYED = new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.canBeDestroyed();
            }
        };
    }

    public static class Accessors {
        public static final Function<Card, Integer> fnGetDefense = new Function<Card, Integer>() {
            @Override
            public Integer apply(Card a) {
                return a.getNetToughness();
            }
        };

        public static final Function<Card, Integer> fnGetNetPower = new Function<Card, Integer>() {
            @Override
            public Integer apply(Card a) {
                return a.getNetPower();
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
    }
}
