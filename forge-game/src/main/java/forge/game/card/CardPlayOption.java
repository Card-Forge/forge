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
    private final boolean withFlash;

    public CardPlayOption(final boolean withoutManaCost, final boolean ignoreManaCostColor, final boolean withFlash) {
        this(withoutManaCost ? PayManaCost.NO : PayManaCost.YES, ignoreManaCostColor, withFlash);
    }
    private CardPlayOption(final PayManaCost payManaCost, final boolean ignoreManaCostColor, final boolean withFlash) {
        this.payManaCost = payManaCost;
        this.ignoreManaCostColor = ignoreManaCostColor;
        this.withFlash = withFlash;
    }

    public CardPlayOption add(final boolean payManaCost, final boolean ignoreManaCostColor, final boolean withFlash) {
        return new CardPlayOption(this.payManaCost.add(payManaCost), isIgnoreManaCostColor() || ignoreManaCostColor, isWithFlash() || withFlash);
    }

    public PayManaCost getPayManaCost() {
        return payManaCost;
    }

    public boolean isIgnoreManaCostColor() {
        return ignoreManaCostColor;
    }
    
    public boolean isWithFlash() {
    	return withFlash;
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
        	if (isWithFlash()) {
        		return " (without paying its mana cost and as though it has flash)";
        	} else {
        		return " (without paying its mana cost)";
        	}
        }
        return StringUtils.EMPTY;
    }

}
