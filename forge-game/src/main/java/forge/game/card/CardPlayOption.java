package forge.game.card;

import org.apache.commons.lang3.StringUtils;

public final class CardPlayOption {
    public enum PayManaCost {
        /** Indicates the mana cost must be payed. */
        YES,
        /** Indicates the mana cost must not be payed. */
        MAYBE,
        /** Indicates the mana cost may not be payed. */
        NO;

        private PayManaCost add(final boolean other) {
            if (this == MAYBE || (this == YES && other) || (this == NO && !other)) { 
                return this;
            }
            return MAYBE;
        }
    }

    private final PayManaCost payManaCost;
    private final boolean ignoreManaCostColor;

    public CardPlayOption(final boolean withoutManaCost, final boolean ignoreManaCostColor) {
        this(withoutManaCost ? PayManaCost.NO : PayManaCost.YES, ignoreManaCostColor);
    }
    private CardPlayOption(final PayManaCost payManaCost, final boolean ignoreManaCostColor) {
        this.payManaCost = payManaCost;
        this.ignoreManaCostColor = ignoreManaCostColor;
    }

    public CardPlayOption add(final boolean payManaCost, final boolean ignoreManaCostColor) {
        return new CardPlayOption(this.payManaCost.add(payManaCost), isIgnoreManaCostColor() || ignoreManaCostColor);
    }

    public PayManaCost getPayManaCost() {
        return payManaCost;
    }

    public boolean isIgnoreManaCostColor() {
        return ignoreManaCostColor;
    }

    @Override
    public String toString() {
        switch (getPayManaCost()) {
        case YES:
            if (isIgnoreManaCostColor()) {
                return " (may spend mana as though it were mana of any color to cast it)";
            }
            break;
        case MAYBE:
            if (isIgnoreManaCostColor()) {
                return " (with or without paying its mana cost, spending mana as though it were mana of any color to cast it)";
            } else {
                return " (with or without paying its mana cost)";
            }
        case NO:
            return " (without paying its mana cost)";
        }
        return StringUtils.EMPTY;
    }

}
