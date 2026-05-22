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
package forge.gamemodes.limited;

import com.google.common.collect.Lists;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.generation.DeckGeneratorBase;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.IterableUtil;
import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI deck builder for Commander Draft format.
 *
 * <p>Extends {@link LimitedDeckBuilder} to produce a 60-card Commander Draft deck:
 * 1–2 commanders in the Commander zone and 58–59 main-deck cards (approximately
 * 22 lands + 36–37 spells).</p>
 *
 * <h3>Commander selection</h3>
 * <p>If commanders are not supplied at construction time the builder scores every
 * commander-legal card in the drafted pool against the color distribution of the
 * non-commander cards, then picks the candidate whose color identity best covers
 * the colors already in the pool.  Per Commander Draft rules a commander with
 * ≤1 color identity (or explicit Partner) is partner-eligible; in that case a
 * second commander is sought whose combined identity extends the coverage.</p>
 */
public class CommanderDraftDeckBuilder extends LimitedDeckBuilder {

    /** Land count for a 60-card commander deck (≈40% of 60). */
    private static final int CMD_LAND_COUNT = 24;

    /** Main deck size when using exactly one commander (60 − 1). */
    private static final int MAIN_SINGLE = 59;

    /** Main deck size when using two partner commanders (60 − 2). */
    private static final int MAIN_PARTNER = 58;

    private final String freeCommanderName;
    private PaperCard selectedCommander;
    private PaperCard selectedPartner;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Build a commander-draft deck, selecting commanders from the pool at build
     * time based on the color distribution of the drafted cards.
     *
     * @param draftedCards      all cards the AI drafted (the sideboard pool)
     * @param deckColors        color preferences determined during the draft
     * @param freeCommanderName edition-specific free commander name, or {@code null}
     */
    public CommanderDraftDeckBuilder(final List<PaperCard> draftedCards,
            final DeckColors deckColors, final String freeCommanderName) {
        super(draftedCards, deckColors);
        this.freeCommanderName = freeCommanderName;
        this.selectedCommander = null;
        this.selectedPartner   = null;
    }

    /**
     * Build a commander-draft deck using commanders already chosen during drafting.
     *
     * @param draftedCards all cards the AI drafted (the sideboard pool)
     * @param deckColors   color preferences determined during the draft
     * @param commander    the primary (or only) commander
     * @param partner      the partner commander, or {@code null}
     */
    public CommanderDraftDeckBuilder(final List<PaperCard> draftedCards,
            final DeckColors deckColors,
            final PaperCard commander, final PaperCard partner) {
        super(draftedCards, deckColors);
        this.freeCommanderName = null;
        this.selectedCommander = commander;
        this.selectedPartner   = partner;
    }

    // -------------------------------------------------------------------------
    // Overrides
    // -------------------------------------------------------------------------

    @Override
    protected int getDeckSizeTarget() {
        return (selectedPartner != null) ? MAIN_PARTNER : MAIN_SINGLE;
    }

    @Override
    protected String generateName() {
        if (selectedCommander != null) {
            return selectedCommander.getName()
                    + (selectedPartner != null ? " + " + selectedPartner.getName() : "")
                    + " Commander Draft";
        }
        return super.generateName();
    }

    @Override
    public Deck buildDeck(final String landSetCode) {

        // 1. Select commander(s) from pool if not pre-assigned during the draft
        if (selectedCommander == null) {
            selectCommanders();
        }

        // 2. Lock the deck colors to the combined color identity of the commanders
        if (selectedCommander != null) {
            final List<PaperCard> cmdrs = new ArrayList<>();
            cmdrs.add(selectedCommander);
            if (selectedPartner != null) {
                cmdrs.add(selectedPartner);
            }
            colors = CommanderDraftUtil.getCommanderColorIdentity(cmdrs);
        }

        // 3. Remove commander(s) from the buildable card pool
        if (selectedCommander != null) {
            aiPlayables.remove(selectedCommander);
            availableList.remove(selectedCommander);
        }
        if (selectedPartner != null) {
            aiPlayables.remove(selectedPartner);
            availableList.remove(selectedPartner);
        }

        // 4. Set targets: 22 lands + 36-37 spells = 58-59 main cards
        final int mainTarget = getDeckSizeTarget();
        numSpellsNeeded = mainTarget - CMD_LAND_COUNT;  // 36 or 37
        landsNeeded     = CMD_LAND_COUNT;               // 22

        // Expanded CMC targets for a 60-card deck
        targetCMCs = java.util.Map.of(
                1, 3,
                2, 9,
                3, 10,
                4, 6,
                5, 4,
                6, 3);

        // 5. Build spells/lands using the same pipeline as LimitedDeckBuilder
        hasColor = new MatchColorIdentity(colors).or(DeckGeneratorBase.COLORLESS_CARDS);
        final Iterable<PaperCard> colorList = IterableUtil.filter(aiPlayables,
                PaperCardPredicates.fromRules(hasColor));
        rankedColorList = CardRanker.rankCardsInDeck(colorList);
        onColorCreatures = IterableUtil.filter(rankedColorList, PaperCardPredicates.IS_CREATURE);
        onColorNonCreatures = IterableUtil.filter(rankedColorList,
                PaperCardPredicates.fromRules(CardRulesPredicates.IS_NON_CREATURE_SPELL));

        // 5a. Planeswalkers
        final Iterable<PaperCard> onColorWalkers = IterableUtil.filter(colorList,
                PaperCardPredicates.fromRules(CardRulesPredicates.IS_PLANESWALKER));
        final List<PaperCard> walkers = Lists.newArrayList(onColorWalkers);
        deckList.addAll(walkers);
        aiPlayables.removeAll(walkers);
        rankedColorList.removeAll(walkers);

        // 5b. Creatures along the mana curve (~60% of non-land slots)
        addManaCurveCreatures(onColorCreatures, (int) Math.round(numSpellsNeeded * 0.60));

        // 5c. Non-creatures to fill up to numSpellsNeeded
        addNonCreatures(onColorNonCreatures, numSpellsNeeded - deckList.size());

        // 5d. More creatures if still short
        addCreatures(onColorCreatures, numSpellsNeeded - deckList.size());

        // 5e. Bonus card when average CMC is low
        if (deckList.size() == numSpellsNeeded && getAverageCMC(deckList) < 4) {
            final PaperCard extra = rankedColorList.stream()
                    .filter(PaperCardPredicates.IS_NON_LAND)
                    .findFirst().orElse(null);
            if (extra != null) {
                deckList.add(extra);
                aiPlayables.remove(extra);
                rankedColorList.remove(extra);
                landsNeeded--;
            }
        }

        // 5f. Non-basic lands from draft pool
        addNonBasicLands();

        // 5g. DeckNeeds / RemRandomDecks cleanup
        checkRemRandomDeckCards();

        // 5h. Basic lands
        final int[] clrCnts = calculateLandNeeds();
        if (landsNeeded > 0) {
            addLands(clrCnts, landSetCode);
        }

        // 6. Fix total main-deck size
        fixCommanderDeckSize(clrCnts, landSetCode, mainTarget);

        if (ForgePreferences.DEV_MODE) {
            System.out.println("CommanderDraftDeckBuilder: built " + deckList.size()
                    + " main cards, commander=" + selectedCommander
                    + (selectedPartner != null ? " + " + selectedPartner : ""));
        }

        // 7. Assemble the result deck
        final Deck result = new Deck(generateName());
        result.getMain().add(deckList);
        final CardPool sb = result.getOrCreate(DeckSection.Sideboard);
        sb.add(aiPlayables);
        sb.add(availableList);
        if (!draftedConspiracies.isEmpty()) {
            result.getOrCreate(DeckSection.Conspiracy).add(draftedConspiracies);
        }
        if (selectedCommander != null) {
            result.getOrCreate(DeckSection.Commander).add(selectedCommander);
        }
        if (selectedPartner != null) {
            result.getOrCreate(DeckSection.Commander).add(selectedPartner);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Commander selection
    // -------------------------------------------------------------------------

    /**
     * Pick the best 1–2 commanders from the drafted pool (plus free-commander
     * options) based on color-coverage scoring.
     *
     * <p>Scoring: a commander earns one point for each non-commander card in the
     * pool whose color identity fits within the commander's color identity, plus
     * a bonus proportional to the commander's raw card-quality score so that
     * high-impact legends are preferred among equally-covering candidates.</p>
     */
    private void selectCommanders() {
        // --- Gather candidates ---
        final List<PaperCard> candidates = aiPlayables.stream()
                .filter(c -> c.getRules().canBeCommander())
                .collect(Collectors.toList());

        addFreeCommanderIfAbsent(candidates, freeCommanderName);
        for (final String fallback : CommanderDraftUtil.FREE_COMMANDER_FALLBACKS) {
            addFreeCommanderIfAbsent(candidates, fallback);
        }

        if (candidates.isEmpty()) {
            return;
        }

        // --- Score and pick primary commander ---
        // Non-Background cards come first; Background enchantments are partners, not primary commanders.
        candidates.sort(Comparator
                .comparingInt((PaperCard c) -> c.getRules().canBeBackground() ? 1 : 0)
                .thenComparingInt((PaperCard c) -> scoreCommanderFit(c, null)).reversed());

        selectedCommander = candidates.get(0);

        if (ForgePreferences.DEV_MODE) {
            System.out.println("CommanderDraftDeckBuilder: selected commander "
                    + selectedCommander + " (identity="
                    + selectedCommander.getRules().getColorIdentity() + ")");
        }

        // --- Optionally pick a partner ---
        if (CommanderDraftUtil.isPartnerEligible(selectedCommander) && candidates.size() > 1) {
            PaperCard bestPartner = null;
            int bestScore = -1;
            for (int i = 1; i < candidates.size(); i++) {
                final PaperCard candidate = candidates.get(i);
                if (!candidate.getRules().canBePartnerCommanders(selectedCommander.getRules())) {
                    continue;
                }
                final int score = scoreCommanderFit(candidate, selectedCommander);
                if (score > bestScore) {
                    bestScore = score;
                    bestPartner = candidate;
                }
            }
            selectedPartner = bestPartner;
            if (selectedPartner != null && ForgePreferences.DEV_MODE) {
                System.out.println("CommanderDraftDeckBuilder: selected partner "
                        + selectedPartner);
            }
        }
    }

    /**
     * Score how well {@code candidate} covers the drafted pool, optionally
     * considering an already-chosen {@code primary} commander.
     *
     * @param candidate the commander being evaluated
     * @param primary   an existing first commander (for partner scoring), or {@code null}
     * @return coverage count + quality bonus
     */
    private int scoreCommanderFit(final PaperCard candidate, final PaperCard primary) {
        byte mask = candidate.getRules().getColorIdentity().getColor();
        if (primary != null) {
            mask |= primary.getRules().getColorIdentity().getColor();
        }
        final ColorSet combined = ColorSet.fromMask(mask);

        int score = 0;
        for (final PaperCard card : aiPlayables) {
            if (card == candidate || card == primary) { continue; }
            if (card.getRules().canBeCommander()) { continue; } // Don't count other candidates
            if (card.getRules().getColorIdentity().hasNoColorsExcept(combined)) {
                score++;
            }
        }
        // Quality bonus: prefer high-powered legends when coverage is equal
        score += (int) (CardRanker.getRawScore(candidate) * 2.0);
        return score;
    }

    private void addFreeCommanderIfAbsent(final List<PaperCard> candidates, final String name) {
        if (name == null || name.isEmpty()) { return; }
        final boolean present = candidates.stream().anyMatch(c -> c.getName().equals(name));
        if (!present) {
            final PaperCard card = FModel.getMagicDb().getCommonCards().getCard(name);
            if (card != null) {
                candidates.add(card);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Size-fixing
    // -------------------------------------------------------------------------

    /**
     * Trim or pad {@code deckList} until it equals {@code target} cards.
     * Mirrors the logic in {@link LimitedDeckBuilder#buildDeck} but with an
     * arbitrary target size.
     */
    private void fixCommanderDeckSize(final int[] clrCnts, final String landSetCode,
            final int target) {
        while (deckList.size() > target) {
            final PaperCard c = deckList.get(MyRandom.getRandom().nextInt(deckList.size() - 1));
            deckList.remove(c);
            aiPlayables.add(c);
        }
        while (deckList.size() < target) {
            if (!aiPlayables.isEmpty()) {
                final int idx = aiPlayables.size() > 1
                        ? MyRandom.getRandom().nextInt(aiPlayables.size() - 1)
                        : 0;
                final PaperCard c = aiPlayables.get(idx);
                deckList.add(c);
                aiPlayables.remove(c);
                rankedColorList.remove(c);
            } else {
                // Last resort: basic lands
                for (int i = 0; i < 5; i++) {
                    if (clrCnts[i] > 0) {
                        deckList.add(getBasicLand(i, landSetCode));
                        break;
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** @return the selected (or pre-supplied) primary commander, or {@code null}. */
    public PaperCard getSelectedCommander() { return selectedCommander; }

    /** @return the selected (or pre-supplied) partner commander, or {@code null}. */
    public PaperCard getSelectedPartner()   { return selectedPartner;   }
}


