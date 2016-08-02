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
package forge.deck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.ImmutablePair;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;

import forge.StaticData;
import forge.card.CardRules;
import forge.card.CardType;
import forge.card.ICardFace;
import forge.deck.generation.DeckGenPool;
import forge.deck.generation.DeckGeneratorBase.FilterCMC;
import forge.deck.generation.IDeckGenPool;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.util.Aggregates;

/**
 * GameType is an enum to determine the type of current game. :)
 */
public enum DeckFormat {
    //               Main board: allowed size             SB: restriction   Max distinct non basic cards
    Constructed    ( Range.between(60, Integer.MAX_VALUE), Range.between(0, 15), 4),
    QuestDeck      ( Range.between(40, Integer.MAX_VALUE), Range.between(0, 15), 4),
    Limited        ( Range.between(40, Integer.MAX_VALUE), null, Integer.MAX_VALUE),
    Commander      ( Range.is(99),                         Range.between(0, 10), 1),
    TinyLeaders    ( Range.is(49),                         Range.between(0, 10), 1, new Predicate<CardRules>() {
        private final Set<String> bannedCards = new HashSet<String>(Arrays.asList(
                "Ancestral Recall", "Balance", "Black Lotus", "Black Vise", "Channel", "Chaos Orb", "Contract From Below", "Counterbalance", "Darkpact", "Demonic Attorney", "Demonic Tutor", "Earthcraft", "Edric, Spymaster of Trest", "Falling Star",
                "Fastbond", "Flash", "Goblin Recruiter", "Grindstone", "Hermit Druid", "Imperial Seal", "Jeweled Bird", "Karakas", "Library of Alexandria", "Mana Crypt", "Mana Drain", "Mana Vault", "Metalworker", "Mind Twist", "Mishra's Workshop",
                "Mox Emerald", "Mox Jet", "Mox Pearl", "Mox Ruby", "Mox Sapphire", "Necropotence", "Shahrazad", "Skullclamp", "Sol Ring", "Strip Mine", "Survival of the Fittest", "Sword of Body and Mind", "Time Vault", "Time Walk", "Timetwister",
                "Timmerian Fiends", "Tolarian Academy", "Umezawa's Jitte", "Vampiric Tutor", "Wheel of Fortune", "Yawgmoth's Will"));

        @Override
        public boolean apply(CardRules rules) {
            // Check for split cards explicitly, as using rules.getManaCost().getCMC()
            // will return the sum of the costs, which is not what we want.
            if (rules.getMainPart().getManaCost().getCMC() > 3) {
                return false; //only cards with CMC less than 3 are allowed
            }
            ICardFace otherPart = rules.getOtherPart();
            if (otherPart != null && otherPart.getManaCost().getCMC() > 3) {
                return false; //only cards with CMC less than 3 are allowed
            }
            if (bannedCards.contains(rules.getName())) {
                return false;
            }
            return true;
        }
    }) {
        private final ImmutableSet<String> bannedCommanders = ImmutableSet.of("Derevi, Empyrial Tactician", "Erayo, Soratami Ascendant", "Rofellos, Llanowar Emissary");

        @Override
        public boolean isLegalCommander(CardRules rules) {
            return super.isLegalCommander(rules) && !bannedCommanders.contains(rules.getName());
        }

        @Override
        public void adjustCMCLevels(List<ImmutablePair<FilterCMC, Integer>> cmcLevels) {
            cmcLevels.clear();
            cmcLevels.add(ImmutablePair.of(new FilterCMC(0, 1), 3));
            cmcLevels.add(ImmutablePair.of(new FilterCMC(2, 2), 3));
            cmcLevels.add(ImmutablePair.of(new FilterCMC(3, 3), 3));
        }
    },
    PlanarConquest ( Range.between(40, Integer.MAX_VALUE), Range.is(0), 1),
    Vanguard       ( Range.between(60, Integer.MAX_VALUE), Range.is(0), 4),
    Planechase     ( Range.between(60, Integer.MAX_VALUE), Range.is(0), 4),
    Archenemy      ( Range.between(60, Integer.MAX_VALUE), Range.is(0), 4);

    private final Range<Integer> mainRange;
    private final Range<Integer> sideRange; // null => no check
    private final int maxCardCopies;
    private final Predicate<CardRules> cardPoolFilter;
    private final static String ADVPROCLAMATION = "Advantageous Proclamation";

    private DeckFormat(Range<Integer> mainRange0, Range<Integer> sideRange0, int maxCardCopies0) {
        this(mainRange0, sideRange0, maxCardCopies0, null);
    }
    private DeckFormat(Range<Integer> mainRange0, Range<Integer> sideRange0, int maxCardCopies0, Predicate<CardRules> cardPoolFilter0) {
        mainRange = mainRange0;
        sideRange = sideRange0;
        maxCardCopies = maxCardCopies0;
        cardPoolFilter = cardPoolFilter0;
    }

    private boolean hasCommander() {
        return this == Commander || this == TinyLeaders;
    }

    /**
     * Smart value of.
     *
     * @param value the value
     * @param defaultValue the default value
     * @return the game type
     */
    public static DeckFormat smartValueOf(final String value, DeckFormat defaultValue) {
        if (value == null) {
            return defaultValue;
        }

        final String valToCompate = value.trim();
        for (final DeckFormat v : DeckFormat.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        throw new IllegalArgumentException("No element named " + value + " in enum GameType");
    }

    /**
     * @return the sideRange
     */
    public Range<Integer> getSideRange() {
        return sideRange;
    }

    /**
     * @return the mainRange
     */
    public Range<Integer> getMainRange() {
        return mainRange;
    }

    /**
     * @return the maxCardCopies
     */
    public int getMaxCardCopies() {
        return maxCardCopies;
    }

    public String getDeckConformanceProblem(Deck deck) {
        if (deck == null) {
            return "is not selected";
        }

        int deckSize = deck.getMain().countAll();

        int min = getMainRange().getMinimum();
        int max = getMainRange().getMaximum();

        // Adjust minimum base on number of Advantageous Proclamation or similar cards
        CardPool conspiracies = deck.get(DeckSection.Conspiracy);
        if (conspiracies != null) {
            min -= (5 * conspiracies.countByName(ADVPROCLAMATION, true));
        }

        if (deckSize < min) {
            return String.format("should have at least %d cards", min);
        }

        if (deckSize > max) {
            return String.format("should have no more than %d cards", max);
        }

        if (hasCommander()) { //Must contain exactly 1 legendary Commander and a sideboard of 10 or zero cards.
            final PaperCard commander = deck.getCommander();
            if (commander == null) {
                return "is missing a commander";
            }
            if (!isLegalCommander(commander.getRules())) {
                return "has an illegal commander";
            }

            final byte cmdCI = commander.getRules().getColorIdentity().getColor();
            final List<PaperCard> erroneousCI = new ArrayList<PaperCard>();

            for (final Entry<PaperCard, Integer> cp : deck.get(DeckSection.Main)) {
                if (!cp.getKey().getRules().getColorIdentity().hasNoColorsExcept(cmdCI)) {
                    erroneousCI.add(cp.getKey());
                }
            }
            if (deck.has(DeckSection.Sideboard)) {
                for (final Entry<PaperCard, Integer> cp : deck.get(DeckSection.Sideboard)) {
                    if (!cp.getKey().getRules().getColorIdentity().hasNoColorsExcept(cmdCI)) {
                        erroneousCI.add(cp.getKey());
                    }
                }
            }

            if (erroneousCI.size() > 0) {
                StringBuilder sb = new StringBuilder("contains one or more cards that do not match the commanders color identity:");

                for (PaperCard cp : erroneousCI) {
                    sb.append("\n").append(cp.getName());
                }

                return sb.toString();
            }
        }

        if (cardPoolFilter != null) {
            final List<PaperCard> erroneousCI = new ArrayList<PaperCard>();
            for (final Entry<PaperCard, Integer> cp : deck.getAllCardsInASinglePool()) {
                if (!cardPoolFilter.apply(cp.getKey().getRules())) {
                    erroneousCI.add(cp.getKey());
                }
            }
            if (erroneousCI.size() > 0) {
                final StringBuilder sb = new StringBuilder("contains the following illegal cards:\n");

                for (final PaperCard cp : erroneousCI) {
                    sb.append("\n").append(cp.getName());
                }

                return sb.toString();
            }
        }

        final int maxCopies = getMaxCardCopies();
        if (maxCopies < Integer.MAX_VALUE) {
            //Must contain no more than 4 of the same card
            //shared among the main deck and sideboard, except
            //basic lands, Shadowborn Apostle and Relentless Rats

            final CardPool allCards = deck.getAllCardsInASinglePool(hasCommander());
            final ImmutableSet<String> limitExceptions = ImmutableSet.of("Relentless Rats", "Shadowborn Apostle");

            // should group all cards by name, so that different editions of same card are really counted as the same card
            for (final Entry<String, Integer> cp : Aggregates.groupSumBy(allCards, PaperCard.FN_GET_NAME)) {
                final IPaperCard simpleCard = StaticData.instance().getCommonCards().getCard(cp.getKey());
                if (simpleCard == null) {
                    return String.format("contains the nonexisting card %s", cp.getKey());
                }

                final boolean canHaveMultiple = simpleCard.getRules().getType().isBasicLand() || limitExceptions.contains(cp.getKey());
                if (!canHaveMultiple && cp.getValue() > maxCopies) {
                    return String.format("must not contain more than %d copies of the card %s", maxCopies, cp.getKey());
                }
            }
        }

        // The sideboard must contain either 0 or 15 cards
        int sideboardSize = deck.has(DeckSection.Sideboard) ? deck.get(DeckSection.Sideboard).countAll() : 0;
        Range<Integer> sbRange = getSideRange();
        if (sbRange != null && sideboardSize > 0 && !sbRange.contains(sideboardSize)) {
            return sbRange.getMinimum() == sbRange.getMaximum()
            ? String.format("must have a sideboard of %d cards or no sideboard at all", sbRange.getMaximum())
            : String.format("must have a sideboard of %d to %d cards or no sideboard at all", sbRange.getMinimum(), sbRange.getMaximum());
        }

        return null;
    }

    public static String getPlaneSectionConformanceProblem(final CardPool planes) {
    	//Must contain at least 10 planes/phenomenons, but max 2 phenomenons. Singleton.
    	if (planes == null || planes.countAll() < 10) {
    		return "should have at least 10 planes";
    	}
    	int phenoms = 0;
    	for (Entry<PaperCard, Integer> cp : planes) {
    		if (cp.getKey().getRules().getType().hasType(CardType.CoreType.Phenomenon)) {
    			phenoms++;
    		}
    		if (cp.getValue() > 1) {
    			return "must not contain multiple copies of any Plane or Phenomena";
    		}
    	}
    	if (phenoms > 2) {
    		return "must not contain more than 2 Phenomena";
    	}
        return null;
    }

    public static String getSchemeSectionConformanceProblem(final CardPool schemes) {
        //Must contain at least 20 schemes, max 2 of each.
        if (schemes == null || schemes.countAll() < 20) {
            return "must contain at least 20 schemes";
        }

        for (Entry<PaperCard, Integer> cp : schemes) {
            if (cp.getValue() > 2) {
                return String.format("must not contain more than 2 copies of any Scheme, but has %d of '%s'", cp.getValue(), cp.getKey().getName());
            }
        }
        return null;
    }

    public IDeckGenPool getCardPool(IDeckGenPool basePool) {
        if (cardPoolFilter == null) {
            return basePool;
        }
        DeckGenPool filteredPool = new DeckGenPool();
        for (PaperCard pc : basePool.getAllCards()) {
            if (cardPoolFilter.apply(pc.getRules())) {
                filteredPool.add(pc);
            }
        }
        return filteredPool;
    }

    public void adjustCMCLevels(List<ImmutablePair<FilterCMC, Integer>> cmcLevels) {
        //not needed by default
    }

    public boolean isLegalCard(PaperCard pc) {
        if (cardPoolFilter == null) {
            return true;
        }
        return cardPoolFilter.apply(pc.getRules());
    }

    public boolean isLegalCommander(CardRules rules) {
        if (cardPoolFilter != null && !cardPoolFilter.apply(rules)) {
            return false;
        }
        return rules.canBeCommander();
    }

    public Predicate<Deck> isLegalDeckPredicate() {
        return new Predicate<Deck>() {
            @Override
            public boolean apply(Deck deck) {
                return getDeckConformanceProblem(deck) == null;
            }
        };
    }

    public Predicate<Deck> hasLegalCardsPredicate() {
        return new Predicate<Deck>() {
            @Override
            public boolean apply(Deck deck) {
                if (cardPoolFilter != null) {
                    for (final Entry<PaperCard, Integer> cp : deck.getAllCardsInASinglePool()) {
                        if (!cardPoolFilter.apply(cp.getKey().getRules())) {
                            return false;
                        }
                    }
                }
                return true;
            }
        };
    }

    public Predicate<PaperCard> isLegalCardPredicate() {
        return new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard card) {
                return isLegalCard(card);
            }
        };
    }

    public Predicate<PaperCard> isLegalCommanderPredicate() {
        return new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard card) {
                return isLegalCommander(card.getRules());
            }
        };
    }

    public Predicate<PaperCard> isLegalCardForCommanderPredicate(PaperCard commander) {
        final byte cmdCI = commander.getRules().getColorIdentity().getColor(); 
        return new Predicate<PaperCard>() {
            @Override
            public boolean apply(PaperCard card) {
                return card.getRules().getColorIdentity().hasNoColorsExcept(cmdCI);
            }
        };
    }
}
