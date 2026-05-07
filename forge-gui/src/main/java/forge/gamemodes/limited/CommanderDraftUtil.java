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
import forge.deck.CardPool;
import forge.gui.util.SGuiChoose;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.Localizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared utility methods for Commander Draft deck-building.
 *
 * <p>Contains logic for presenting the human player with a commander-selection
 * dialog drawn from their drafted card pool, as well as helper predicates used
 * by both the desktop and mobile UIs.</p>
 */
public final class CommanderDraftUtil {

    /** Universal fallback commander names available even when not drafted. */
    public static final String[] FREE_COMMANDER_FALLBACKS = {
            "The Prismatic Piper", "Faceless One"
    };

    private CommanderDraftUtil() {}

    /**
     * Interactively selects 1–2 commanders for a Commander Draft deck.
     *
     * <p>The candidate list is built from:</p>
     * <ol>
     *   <li>All {@code canBeCommander()} cards in {@code pool}.</li>
     *   <li>The edition's {@code freeCommanderName} (if not already in the pool).</li>
     *   <li>The universal fallbacks "The Prismatic Piper" / "Faceless One" (if not
     *       already present).</li>
     * </ol>
     *
     * <p>After picking a primary commander the method checks partner eligibility
     * per Commander Draft rules (≤1 color or explicit {@code Partner} keyword)
     * and optionally prompts for a second commander.</p>
     *
     * @param pool the full drafted card pool (normally {@code DeckSection.Sideboard})
     * @param freeCommanderName the edition's free commander card name, or {@code null}
     * @return list containing 1 commander or 2 partner commanders; never {@code null}
     */
    public static List<PaperCard> selectCommandersFromPool(final CardPool pool,
            final String freeCommanderName) {
        final Localizer localizer = Localizer.getInstance();

        // ---- Build primary commander candidate list ----
        List<PaperCard> candidates = pool.toFlatList().stream()
                .filter(c -> c.getRules().canBeCommander())
                .distinct()
                .collect(Collectors.toList());

        // Add out-of-pool free options
        List<PaperCard> freeOptions = buildFreeCommanderOptions(candidates, freeCommanderName);
        candidates.addAll(freeOptions);

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        // ---- Primary commander pick ----
        PaperCard commander = SGuiChoose.oneOrNone(
                localizer.getMessage("lblChooseCommanderFromPool"), candidates);
        if (commander == null) {
            // User dismissed — fall back to first candidate
            commander = candidates.get(0);
        }

        final List<PaperCard> result = new ArrayList<>();
        result.add(commander);

        // ---- Optional partner pick ----
        if (isPartnerEligible(commander)) {
            final PaperCard finalCommander = commander;

            // Collect drafted partner candidates
            List<PaperCard> partnerCandidates = pool.toFlatList().stream()
                    .filter(c -> c.getRules().canBePartnerCommanders(finalCommander.getRules()))
                    .distinct()
                    .collect(Collectors.toList());

            // Add free-option partners
            for (PaperCard freeOpt : freeOptions) {
                if (freeOpt.getRules().canBePartnerCommanders(commander.getRules())
                        && partnerCandidates.stream()
                                .noneMatch(c -> c.getName().equals(freeOpt.getName()))) {
                    partnerCandidates.add(freeOpt);
                }
            }

            if (!partnerCandidates.isEmpty()) {
                PaperCard partner = SGuiChoose.oneOrNone(
                        localizer.getMessage("lblChoosePartnerCommander"), partnerCandidates);
                if (partner != null) {
                    result.add(partner);
                }
            }
        }

        return result;
    }

    /**
     * Commander Draft rule: a commander is partner-eligible when it has ≤1 color
     * in its color identity, or when it already has some form of the Partner keyword.
     */
    public static boolean isPartnerEligible(final PaperCard commander) {
        return commander.getRules().getColorIdentity().countColors() <= 1
                || commander.getRules().canBePartnerCommander();
    }

    /**
     * Return the combined color identity of all supplied commander cards.
     */
    public static ColorSet getCommanderColorIdentity(final Iterable<PaperCard> commanders) {
        byte mask = 0;
        for (PaperCard cmd : commanders) {
            mask |= cmd.getRules().getColorIdentity().getColor();
        }
        return ColorSet.fromMask(mask);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Build the list of free-commander options that are not already present in
     * the drafted {@code existingCandidates} list.
     */
    private static List<PaperCard> buildFreeCommanderOptions(
            final List<PaperCard> existingCandidates, final String freeCommanderName) {
        List<PaperCard> freeOptions = new ArrayList<>();

        if (freeCommanderName != null && !freeCommanderName.isEmpty()) {
            addIfAbsent(freeOptions, freeCommanderName, existingCandidates);
        }
        for (String name : FREE_COMMANDER_FALLBACKS) {
            addIfAbsent(freeOptions, name, existingCandidates);
        }
        return freeOptions;
    }

    private static void addIfAbsent(final List<PaperCard> target, final String cardName,
            final List<PaperCard> existing) {
        boolean alreadyPresent = existing.stream().anyMatch(c -> c.getName().equals(cardName))
                || target.stream().anyMatch(c -> c.getName().equals(cardName));
        if (!alreadyPresent) {
            PaperCard card = FModel.getMagicDb().getCommonCards().getCard(cardName);
            if (card != null) {
                target.add(card);
            }
        }
    }
}

