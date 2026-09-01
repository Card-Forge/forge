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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.card.CardRules;
import forge.card.CardRulesPredicates;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.util.ComparableOp;

/**
 * {@code DeckRule:ColorIdentity:Exempt$ Type:<branches> | Disable$ True | AllowedAdditionalColor$
 * <n>:Type:<branches>} - exempts matching cards from the deck's color-identity check, disables
 * that check entirely, or grants a shared budget of {@code n} extra colors (deck-wide DISTINCT
 * colors, not card count) to cards matching a {@code Type:} branch. Branches tokenize dot-then-plus
 * like {@code ValidCard$} restriction strings (type/supertype/subtype words, {@code Permanent}, or
 * a stat filter like {@code powerGE4}). A creature-type token also matches cards with Changeling,
 * which is every creature type per rule 702.73a (mirrors the same handling already established in
 * {@link forge.card.DeckHints}).
 */
public class DeckRuleColorIdentity extends DeckRule {
    private static final Pattern STAT_FILTER =
            Pattern.compile("^(power|toughness|cmc)(EQ|NE|GT|LT|GE|LE)(\\d+)$");

    /** One OR-branch: a set of type/stat requirements a card must ALL satisfy. */
    private static final class Branch {
        private final CardType typeSpec = new CardType(true);
        private boolean requiresPermanent = false;
        private final List<StatFilter> statFilters = new ArrayList<>();

        boolean matches(final CardRules card) {
            final CardType candidate = card.getType();
            for (final CardType.CoreType ct : typeSpec.getCoreTypes()) {
                if (!candidate.hasType(ct)) {
                    return false;
                }
            }
            for (final CardType.Supertype st : typeSpec.getSupertypes()) {
                if (!candidate.hasSupertype(st)) {
                    return false;
                }
            }
            for (final String sub : typeSpec.getSubtypes()) {
                if (!candidate.hasSubtype(sub) && !(CardType.isACreatureType(sub) && hasChangeling(card))) {
                    return false;
                }
            }
            if (requiresPermanent && !candidate.isPermanent()) {
                return false;
            }
            for (final StatFilter f : statFilters) {
                if (!f.test(card)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * True if {@code card} has Changeling, making it every creature type - rule 702.73a: "This ability
         * works everywhere, even outside the game." Same {@code Type:} branch a normal creature-type card
         * would need to match, just sourced from the keyword instead of the printed subtypes. Mirrors
         * {@link forge.card.DeckHints}'s existing {@code CardType.isACreatureType(p)} + {@code hasKeyword("Changeling")}
         * handling for the same problem.
         */
        private static boolean hasChangeling(final CardRules card) {
            return CardRulesPredicates.hasKeyword("Changeling").test(card);
        }
    }

    private static final class StatFilter {
        private final CardRulesPredicates.LeafNumber.CardField field;
        private final ComparableOp op;
        private final int value;

        StatFilter(final CardRulesPredicates.LeafNumber.CardField field0, final ComparableOp op0, final int value0) {
            field = field0;
            op = op0;
            value = value0;
        }

        boolean test(final CardRules card) {
            return new CardRulesPredicates.LeafNumber(field, op, value).test(card);
        }
    }

    private final List<Branch> branches = new ArrayList<>();
    private final boolean disabled;
    private final List<Branch> additionalColorBranches = new ArrayList<>();
    private final int additionalColorCount;
    private byte additionalColorsApproved = 0;

    DeckRuleColorIdentity(final Map<String, String> params) {
        super(params);
        disabled = "True".equalsIgnoreCase(params.get("Disable"));
        final String exempt = params.get("Exempt");
        if (exempt != null) {
            branches.addAll(parseBranchList(exempt));
        }
        final String additionalColor = params.get("AllowedAdditionalColor");
        int count = 0;
        if (additionalColor != null) {
            final int colonPos = additionalColor.indexOf(':');
            if (colonPos >= 0) {
                count = Integer.parseInt(additionalColor.substring(0, colonPos).trim());
                additionalColorBranches.addAll(parseBranchList(additionalColor.substring(colonPos + 1)));
            }
        }
        additionalColorCount = count;
    }

    /** Splits a {@code [Type:]Branch1,Branch2,...} value into parsed branches; the {@code Type:} prefix, if present, applies once to the whole value. */
    private static List<Branch> parseBranchList(final String rawValue) {
        final List<Branch> result = new ArrayList<>();
        String body = rawValue.trim();
        if (body.startsWith("Type:")) {
            body = body.substring("Type:".length());
        }
        for (final String rawBranch : body.split(",")) {
            final String trimmed = rawBranch.trim();
            if (!trimmed.isEmpty()) {
                result.add(parseBranch(trimmed));
            }
        }
        return result;
    }

    /** Tokenizes dot-then-plus, matching {@code Card.isValid}'s own restriction-string split. */
    private static Branch parseBranch(final String rawBranch) {
        final Branch branch = new Branch();
        final List<String> typeWords = new ArrayList<>();
        final String[] firstSplit = rawBranch.split("\\.", 2);
        final List<String> tokens = new ArrayList<>();
        tokens.add(firstSplit[0]);
        if (firstSplit.length > 1) {
            for (final String rest : firstSplit[1].split("\\+")) {
                tokens.add(rest);
            }
        }
        for (final String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            final Matcher m = STAT_FILTER.matcher(token);
            if (m.matches()) {
                branch.statFilters.add(new StatFilter(
                        fieldFor(m.group(1)), opFor(m.group(2)), Integer.parseInt(m.group(3))));
            } else if (token.equals("Permanent")) {
                branch.requiresPermanent = true;
            } else {
                typeWords.add(token);
            }
        }
        if (!typeWords.isEmpty()) {
            branch.typeSpec.addAll(CardType.parse(String.join(" ", typeWords), true));
        }
        return branch;
    }

    private static CardRulesPredicates.LeafNumber.CardField fieldFor(final String word) {
        switch (word) {
            case "power": return CardRulesPredicates.LeafNumber.CardField.POWER;
            case "toughness": return CardRulesPredicates.LeafNumber.CardField.TOUGHNESS;
            case "cmc": return CardRulesPredicates.LeafNumber.CardField.CMC;
            default: throw new IllegalArgumentException("Unrecognized DeckRule:ColorIdentity stat field: " + word);
        }
    }

    private static ComparableOp opFor(final String code) {
        switch (code) {
            case "EQ": return ComparableOp.EQUALS;
            case "NE": return ComparableOp.NOT_EQUALS;
            case "GT": return ComparableOp.GREATER_THAN;
            case "LT": return ComparableOp.LESS_THAN;
            case "GE": return ComparableOp.GT_OR_EQUAL;
            case "LE": return ComparableOp.LT_OR_EQUAL;
            default: throw new IllegalArgumentException("Unrecognized DeckRule:ColorIdentity comparator: " + code);
        }
    }

    /** True if color identity checking is waived entirely for cards this rule applies to (The Paradise Bird-style). */
    public boolean disablesColorIdentityCheck() {
        return disabled;
    }

    /** True if the candidate matches at least one {@code Exempt$} branch (or the check is fully disabled). */
    public boolean allowsOffColorIdentity(final CardRules candidate) {
        if (disabled) {
            return true;
        }
        for (final Branch b : branches) {
            if (b.matches(candidate)) {
                return true;
            }
        }
        return false;
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
        if (additionalColorCount == 0) {
            return -1;
        }
        boolean matchesScope = false;
        for (final Branch b : additionalColorBranches) {
            if (b.matches(candidate)) {
                matchesScope = true;
                break;
            }
        }
        if (!matchesScope) {
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
