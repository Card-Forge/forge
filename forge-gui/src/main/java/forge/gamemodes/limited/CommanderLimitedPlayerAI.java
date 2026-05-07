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

import forge.card.ColorSet;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI limited player for Commander Draft format.
 *
 * <p>Pick phases:</p>
 * <ol>
 *   <li>No commander yet — scan the pack for legal commanders, choose the
 *       highest-ranked one and lock the deck's color identity.</li>
 *   <li>Partner eligible, no partner yet — look for a card that can partner
 *       with the chosen commander and draft it if one exists.</li>
 *   <li>Normal pick — delegate to {@link LimitedPlayerAI#chooseCard()} which
 *       uses the now-locked {@link CommanderDeckColors}.</li>
 * </ol>
 *
 * <p>Deck construction uses {@link CardThemedCommanderDeckBuilder} with
 * {@link DeckFormat#CommanderDraft} (60-card-minimum, non-singleton, color
 * identity enforced). If no commander was drafted the AI falls back to the
 * edition's free-commander name, then to "The Prismatic Piper" / "Faceless
 * One" as colorless commanders.</p>
 */
public class CommanderLimitedPlayerAI extends LimitedPlayerAI {

    /** Ordered list of universal fallback commander names (colorless). */
    private static final String[] FALLBACK_FREE_COMMANDERS = {
            "The Prismatic Piper", "Faceless One"
    };

    private PaperCard commander = null;
    private PaperCard partner = null;
    private boolean partnerEligible = false;

    /**
     * The free commander available from the edition (may be {@code null} if
     * the edition does not specify one).
     */
    private final String freeCommanderName;

    /**
     * Construct a commander-draft AI player.
     *
     * @param seatingOrder    seat index in the draft pod (1-based for AI)
     * @param draft           the owning {@link BoosterDraft}
     * @param freeCommanderName name of the edition's free commander card, or
     *                        {@code null} / empty if none
     */
    public CommanderLimitedPlayerAI(final int seatingOrder, final BoosterDraft draft,
            final String freeCommanderName) {
        super(seatingOrder, draft);
        this.freeCommanderName = (freeCommanderName != null && !freeCommanderName.isEmpty())
                ? freeCommanderName : null;
        // Replace the base DeckColors with a commander-identity-aware variant
        this.deckCols = new CommanderDeckColors();
    }

    // -------------------------------------------------------------------------
    // Picking
    // -------------------------------------------------------------------------

    @Override
    public PaperCard chooseCard() {
        if (packQueue.isEmpty()) {
            return null;
        }

        final DraftPack chooseFrom = packQueue.peek();
        if (chooseFrom.isEmpty()) {
            return null;
        }

        // Phase 1 — commander selection
        if (commander == null) {
            return pickCommander(chooseFrom);
        }

        // Phase 2 — partner selection (if eligible)
        if (partnerEligible && partner == null) {
            final PaperCard partnerPick = tryPickPartner(chooseFrom);
            if (partnerPick != null) {
                return partnerPick;
            }
        }

        // Phase 3 — normal pick using locked color identity
        return super.chooseCard();
    }

    /**
     * Choose a commander from the pack.  If no legal commander is present the
     * method falls back to a plain ranked pick and leaves {@code commander}
     * null so that the next pack can still provide a commander.
     */
    private PaperCard pickCommander(final DraftPack pack) {
        final List<PaperCard> candidates = pack.stream()
                .filter(c -> c.getRules().canBeCommander())
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            // No commander available — pick best card and try again next pack
            final List<PaperCard> ranked = CardRanker.rankCardsInPack(
                    pack,
                    deck.getOrCreate(DeckSection.Sideboard).toFlatList(),
                    ColorSet.WUBRG,
                    true);
            final PaperCard picked = ranked.get(0);
            debugPrint("Player[" + order + "] no commander in pack, picked: " + picked);
            return picked;
        }

        // Rank candidates by raw score (higher = better); pick the top one
        candidates.sort((a, b) -> Double.compare(
                CardRanker.getRawScore(b), CardRanker.getRawScore(a)));
        final PaperCard picked = candidates.get(0);

        commander = picked;
        final ColorSet identity = commander.getRules().getColorIdentity();
        ((CommanderDeckColors) deckCols).lockToColorIdentity(identity);

        // Commander Draft rule: treat commanders with ≤1 color as having Partner
        partnerEligible = identity.countColors() <= 1
                || commander.getRules().canBePartnerCommander();

        if (ForgePreferences.DEV_MODE) {
            System.out.println("Player[" + order + "] selected commander: " + commander
                    + " identity=" + identity
                    + " partnerEligible=" + partnerEligible);
        }
        return picked;
    }

    /**
     * Attempt to pick a partner commander from the pack.
     *
     * @return the partner card if one was found and selected, {@code null} otherwise
     */
    private PaperCard tryPickPartner(final DraftPack pack) {
        final List<PaperCard> candidates = pack.stream()
                .filter(c -> c.getRules().canBePartnerCommanders(commander.getRules()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort((a, b) -> Double.compare(
                CardRanker.getRawScore(b), CardRanker.getRawScore(a)));
        partner = candidates.get(0);
        ((CommanderDeckColors) deckCols).addColorIdentityOfPartner(partner);

        if (ForgePreferences.DEV_MODE) {
            System.out.println("Player[" + order + "] selected partner: " + partner);
        }
        return partner;
    }

    // -------------------------------------------------------------------------
    // Deck building
    // -------------------------------------------------------------------------

    @Override
    public Deck buildDeck(final String landSetCode) {
        PaperCard resolvedCommander = this.commander;

        // Resolve a fallback commander if none was drafted
        if (resolvedCommander == null) {
            resolvedCommander = resolveFallbackCommander();
            if (resolvedCommander != null) {
                ((CommanderDeckColors) deckCols).lockToColorIdentity(
                        resolvedCommander.getRules().getColorIdentity());
            }
        }

        // Last resort: use the plain limited builder
        if (resolvedCommander == null) {
            return super.buildDeck(landSetCode);
        }

        // Collect drafted cards, excluding commander(s) from the main pool
        final List<PaperCard> draftedCards = new ArrayList<>(
                deck.getOrCreate(DeckSection.Sideboard).toFlatList());
        draftedCards.remove(resolvedCommander);
        if (partner != null) {
            draftedCards.remove(partner);
        }

        final Deck result = new CardThemedCommanderDeckBuilder(
                resolvedCommander, partner, draftedCards, true, DeckFormat.CommanderDraft)
                .buildDeck();

        // Place commander(s) in the dedicated Commander section
        result.getOrCreate(DeckSection.Commander).add(resolvedCommander);
        if (partner != null) {
            result.getOrCreate(DeckSection.Commander).add(partner);
        }

        if (ForgePreferences.DEV_MODE) {
            System.out.println("Player[" + order + "] built commander draft deck: "
                    + result.getMain().countAll() + " main cards"
                    + ", commander: " + resolvedCommander
                    + (partner != null ? " + partner: " + partner : ""));
        }
        return result;
    }

    /**
     * Resolve the free commander available for this draft, trying the
     * edition-specific name first, then the universal fallbacks.
     *
     * @return a {@link PaperCard} for the free commander, or {@code null} if
     *         none could be found in the card database
     */
    private PaperCard resolveFallbackCommander() {
        if (freeCommanderName != null) {
            final PaperCard free = FModel.getMagicDb().getCommonCards().getCard(freeCommanderName);
            if (free != null) {
                return free;
            }
        }
        for (final String name : FALLBACK_FREE_COMMANDERS) {
            final PaperCard fallback = FModel.getMagicDb().getCommonCards().getCard(name);
            if (fallback != null) {
                return fallback;
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** @return the drafted commander card, or {@code null} if not yet chosen. */
    public PaperCard getCommander() {
        return commander;
    }

    /** @return the drafted partner commander, or {@code null} if none. */
    public PaperCard getPartner() {
        return partner;
    }
}

