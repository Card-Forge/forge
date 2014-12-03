package forge.game.card;

import org.apache.commons.lang3.StringUtils;

public final class CardPlayOption {
    private final boolean withoutManaCost, ignoreManaCostColor;

    public CardPlayOption(final boolean withoutManaCost, final boolean ignoreManaCostColor) {
        this.withoutManaCost = withoutManaCost;
        this.ignoreManaCostColor = ignoreManaCostColor;
    }

    public CardPlayOption add(final boolean withoutManaCost, final boolean ignoreManaCostColor) {
        return new CardPlayOption(isWithoutManaCost() || withoutManaCost, isIgnoreManaCostColor() || ignoreManaCostColor);
    }

    public boolean isWithoutManaCost() {
        return withoutManaCost;
    }

    public boolean isIgnoreManaCostColor() {
        return ignoreManaCostColor;
    }

    @Override
    public String toString() {
        if (isWithoutManaCost()) {
            return " (without paying its mana cost)";
        }
        if (isIgnoreManaCostColor()) {
            return " (may spend mana as though it were mana of any color to cast it)";
        }
        return StringUtils.EMPTY;
    }

}
