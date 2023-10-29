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

import java.util.Comparator;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.card.CardStateName;
import forge.game.CardTraitBase;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.PredicateString;
import forge.util.collect.FCollectionView;


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
                return p.equals(c.getOwner());
            }
        };
    }

    public static final Predicate<Card> ownerLives() {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return !c.getOwner().hasLost();
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

    public static final Predicate<Card> hasKeyword(final Keyword keyword) {
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
                if (Iterables.any(c.getHiddenExtrinsicKeywords(), PredicateString.contains(keyword))) {
                    return true;
                }

                for (KeywordInterface k : c.getKeywords()) {
                    if (k.getOriginal().contains(keyword)) {
                        return true;
                    }
                }
                return false;
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

    public static final Predicate<Card> sharesControllerWith(final Card card) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.sharesControllerWith(card);
            }
        };
    }

    public static Predicate<Card> sharesCardTypeWith(final Card card) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.sharesCardTypeWith(card);
            }
        };
    }

    public static Predicate<Card> sharesCreatureTypeWith(final Card card) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.sharesCreatureTypeWith(card);
            }
        };
    }

    public static Predicate<Card> sharesLandTypeWith(final Card card) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.sharesLandTypeWith(card);
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

    public static final Predicate<Card> restriction(final String[] restrictions, final Player sourceController, final Card source, final CardTraitBase spellAbility) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c != null && c.isValid(restrictions, sourceController, source, spellAbility);
            }
        };
    }

    public static final Predicate<Card> restriction(final String restrictions, final Player sourceController, final Card source, final CardTraitBase spellAbility) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c != null && c.isValid(restrictions, sourceController, source, spellAbility);
            }
        };
    }

    public static final Predicate<Card> canBeSacrificedBy(final SpellAbility sa, final boolean effect) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.canBeSacrificedBy(sa, effect);
            }
        };
    }

    public static final Predicate<Card> canBeAttached(final Card aura, final SpellAbility sa) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.canBeAttached(aura, sa);
            }
        };
    }

    public static final Predicate<Card> isColor(final byte color) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getColor().hasAnyColor(color);
            }
        };
    } // getColor()

    public static final Predicate<Card> isExactlyColor(final byte color) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getColor().hasExactlyColor(color);
            }
        };
    }

    public static final Predicate<Card> isColorless() {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getColor().isColorless();
            }
        };
    }

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
                return c.sharesCMCWith(cmc);
            }
        };
    }

    public static final Predicate<Card> greaterCMC(final int cmc) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                // do not check for Split card anymore
                return c.getCMC() >= cmc;
            }
        };
    }

    public static final Predicate<Card> lessCMC(final int cmc) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                // do not check for Split card anymore
                return c.getCMC() <= cmc;
            }
        };
    }

    public static final Predicate<Card> evenCMC() {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getCMC() % 2 == 0;
            }
        };
    }

    public static final Predicate<Card> oddCMC() {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getCMC() % 2 == 1;
            }
        };
    }

    public static final Predicate<Card> hasSuspend() {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.hasSuspend();
            }
        };
    }

    public static final Predicate<Card> hasCounters() {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.hasCounters();
            }
        };
    }

    public static final Predicate<Card> hasCounter(final CounterType type) {
        return hasCounter(type, 1);
    }
    public static final Predicate<Card> hasCounter(final CounterEnumType type) {
        return hasCounter(type, 1);
    }

    public static final Predicate<Card> hasCounter(final CounterType type, final int n) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getCounters(type) >= n;
            }
        };
    }
    public static final Predicate<Card> hasCounter(final CounterEnumType type, final int n) {
        return hasCounter(CounterType.get(type), n);
    }

    public static final Predicate<Card> hasLessCounter(final CounterType type, final int n) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                int x = c.getCounters(type);
                return x > 0 && x <= n;
            }
        };
    }
    public static final Predicate<Card> hasLessCounter(final CounterEnumType type, final int n) {
        return hasLessCounter(CounterType.get(type), n);
    }

    public static Predicate<Card> canReceiveCounters(final CounterType counter) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.canReceiveCounters(counter);
            }
        };
    }
    public static Predicate<Card> canReceiveCounters(final CounterEnumType counter) {
        return canReceiveCounters(CounterType.get(counter));
    }

    public static final Predicate<Card> hasGreaterPowerThan(final int minPower) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.getNetPower() > minPower;
            }
        };
    }

    public static final Comparator<Card> compareByCounterType(final CounterType type) {
        return new Comparator<Card>() {
            @Override
            public int compare(Card arg0, Card arg1) {
                return Integer.compare(arg0.getCounters(type),
                    arg1.getCounters(type));
            }
        };
    }
    public static final Comparator<Card> compareByCounterType(final CounterEnumType type) {
        return compareByCounterType(CounterType.get(type));
    }

    public static final Predicate<Card> hasSVar(final String name) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.hasSVar(name);
            }
        };
    }

    public static final Predicate<Card> isExiledWith(final Card card) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return card.equals(c.getExiledWith());
            }
        };
    }

    public static final Comparator<Card> compareByTimestamp() {
        return new Comparator<Card>() {
            @Override
            public int compare(Card arg0, Card arg1) {
                return Long.compare(arg0.getTimestamp(), arg1.getTimestamp());
            }
        };
    }

    public static final Predicate<Card> inZone(final ZoneType zt) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                Zone z = c.getLastKnownZone();
                return z != null && z.is(zt);
            }
        };
    }

    public static final Predicate<Card> inZone(final Iterable<ZoneType> zt) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                Zone z = c.getLastKnownZone();
                if (z != null) {
                    for (ZoneType t : zt) {
                        if (z.is(t)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

    public static final Predicate<Card> isRemAIDeck() {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c)
            {
                return c.getRules() != null && c.getRules().getAiHints().getRemAIDecks();
            }
        };
    }

    public static final Predicate<Card> castSA(final Predicate<SpellAbility> predSA) {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c)
            {
                if (c.getCastSA() == null) {
                    return false;
                }
                return predSA.apply(c.getCastSA());
            }
        };
    }

    public static final Predicate<Card> phasedIn() {
        return new Predicate<Card>() {
            @Override
            public boolean apply(final Card c)
            {
                return !c.isPhasedOut();
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
         * a Predicate<Card> to get all aura.
         */
        public static final Predicate<Card> AURA = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isAura();
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
        public static final Predicate<Card> FORTIFICATION = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isFortification();
            }
        };

        /**
         * a Predicate<Card> to get all curse.
         */
        public static final Predicate<Card> CURSE = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isCurse();
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
                return !(c.isToken() || c.isTokenCard());
            }
        };
        /**
         * a Predicate<Card> to get all token cards.
         */
        public static final Predicate<Card> TOKEN = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isToken() || c.isTokenCard();
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
                return c.isLand() || (!c.isInZone(ZoneType.Battlefield) && c.isModal() && c.getState(CardStateName.Modal).getType().isLand());
            }
        };
        /**
         * a Predicate<Card> to get all mana-producing lands.
         */
        public static final Predicate<Card> LANDS_PRODUCING_MANA = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isBasicLand() || (c.isLand() && !c.getManaAbilities().isEmpty());
            }
        };
        /**
         * a Predicate<Card> to get all permanents.
         */
        public static final Predicate<Card> PERMANENTS = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isPermanent();
            }
        };
        /**
         * a Predicate<Card> to get all nonland permanents.
         */
        public static final Predicate<Card> NONLAND_PERMANENTS = new Predicate<Card>() {
            @Override
            public boolean apply(Card c) {
                return c.isPermanent() && !c.isLand();
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
        public static final Predicate<Card> PLANESWALKERS = new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isPlaneswalker();
            }
        };
        public static final Predicate<Card> BATTLES = new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return c.isBattle();
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
        public static final Function<Card, Integer> fnGetNetPower = new Function<Card, Integer>() {
            @Override
            public Integer apply(Card a) {
                return a.getNetPower();
            }
        };

        public static final Function<Card, Integer> fnGetNetToughness = new Function<Card, Integer>() {
            @Override
            public Integer apply(Card a) {
                return a.getNetToughness();
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

        public static final Function<Card, String> fnGetNetName = new Function<Card, String>() {
            @Override
            public String apply(Card a) {
                return a.getName();
            }
        };
    }

}
