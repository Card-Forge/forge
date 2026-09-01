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

import java.util.Map;
import java.util.function.Predicate;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.ColorSet;

/**
 * {@code DeckRule:ColorIdentity:Exempt$ <branches> | Disable$ True | AllowedAdditionalColor$
 * <n> | AllowedAdditionalColorType$ <branches>} - exempts matching cards from the deck's
 * color-identity check, disables it entirely, or grants a shared budget of {@code n} extra
 * colors (distinct colors, not card count) to cards matching a branch. Branch grammar/matching:
 * {@link CardRulesPredicates#restrictionList}.
 */
public class DeckRuleColorIdentity extends DeckRule {
    private final Predicate<CardRules> exemptPredicate;
    private final boolean disabled;
    private final Predicate<CardRules> additionalColorPredicate;
    private final int additionalColorCount;
    private byte additionalColorsApproved = 0;

    DeckRuleColorIdentity(final Map<String, String> params) {
        super(params);
        disabled = "True".equalsIgnoreCase(params.get("Disable"));
        final String exempt = params.get("Exempt");
        exemptPredicate = exempt != null ? CardRulesPredicates.restrictionList(exempt) : card -> false;
        final String additionalColor = params.get("AllowedAdditionalColor");
        additionalColorCount = additionalColor != null ? Integer.parseInt(additionalColor.trim()) : 0;
        final String additionalColorType = params.get("AllowedAdditionalColorType");
        additionalColorPredicate = additionalColorCount > 0 && additionalColorType != null
                ? CardRulesPredicates.restrictionList(additionalColorType) : card -> false;
    }

    /** True if color identity checking is waived entirely for cards this rule applies to (The Paradise Bird-style). */
    public boolean disablesColorIdentityCheck() {
        return disabled;
    }

    /** True if the candidate matches at least one {@code Exempt$} branch (or the check is fully disabled). */
    public boolean allowsOffColorIdentity(final CardRules candidate) {
        return disabled || exemptPredicate.test(candidate);
    }

    /** True if this rule grants any AllowedAdditionalColor$ budget at all. */
    public boolean hasAllowedAdditionalColorBudget() {
        return additionalColorCount > 0;
    }

    /** The additional colors approved so far against this rule's budget; only changes via {@link #tryApproveAdditionalColor}. */
    public byte getApprovedAdditionalColors() {
        return additionalColorsApproved;
    }

    /**
     * The approved-colors mask this rule would end up with if {@code candidate} were let through its
     * budget, or -1 if the candidate is out of scope or the budget can't cover it. Shared by
     * {@link #tryApproveAdditionalColor} and {@link #wouldApproveAdditionalColor}, which only differ
     * in whether they commit the result.
     */
    private byte wouldBeApprovedColors(final CardRules candidate, final byte commanderCI) {
        if (additionalColorCount == 0 || !additionalColorPredicate.test(candidate)) {
            return -1;
        }
        final byte baseline = (byte) (commanderCI | additionalColorsApproved);
        // 0 here means already covered by commander CI or a previously-approved color.
        final byte missing = candidate.getColorIdentity().getMissingColors(baseline).getColor();
        final byte wouldBeApproved = (byte) (additionalColorsApproved | missing);
        if (ColorSet.fromMask(wouldBeApproved).countColors() > additionalColorCount) {
            return -1; // would need more distinct extra colors than the budget allows
        }
        return wouldBeApproved;
    }

    /**
     * Approves the candidate against the {@code AllowedAdditionalColor$} budget, remembering any
     * newly-used color. Stateful - call once per candidate, only after
     * {@link #allowsOffColorIdentity} returns false; prime with cards already in the deck first.
     */
    public boolean tryApproveAdditionalColor(final CardRules candidate, final byte commanderCI) {
        final byte wouldBeApproved = wouldBeApprovedColors(candidate, commanderCI);
        if (wouldBeApproved < 0) {
            return false;
        }
        additionalColorsApproved = wouldBeApproved;
        return true;
    }

    /** Read-only {@link #tryApproveAdditionalColor}: same check, doesn't commit the color. */
    public boolean wouldApproveAdditionalColor(final CardRules candidate, final byte commanderCI) {
        return wouldBeApprovedColors(candidate, commanderCI) >= 0;
    }
}
