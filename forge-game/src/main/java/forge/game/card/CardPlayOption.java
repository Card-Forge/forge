package forge.game.card;

import org.apache.commons.lang3.StringUtils;

import forge.game.player.Player;

public final class CardPlayOption {
    public enum PayManaCost {
        /** Indicates the mana cost must be payed. */
        YES,
        /** Indicates the mana cost may not be payed. */
        NO;
    }

    private final Player player;
    private final Card host;
    private final PayManaCost payManaCost;
    private final boolean ignoreManaCostColor;
    private final boolean withFlash;

    public CardPlayOption(final Player player, final Card host, final boolean withoutManaCost, final boolean ignoreManaCostColor, final boolean withFlash) {
        this(player, host, withoutManaCost ? PayManaCost.NO : PayManaCost.YES, ignoreManaCostColor, withFlash);
    }
    private CardPlayOption(final Player player, final Card host, final PayManaCost payManaCost, final boolean ignoreManaCostColor, final boolean withFlash) {
        this.player = player;
        this.host = host;
        this.payManaCost = payManaCost;
        this.ignoreManaCostColor = ignoreManaCostColor;
        this.withFlash = withFlash;
    }


    public Player getPlayer() {
        return player;
    }

    public Card getHost() {
        return host;
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
        return toString(true);
    }

    public String toString( final boolean withPlayer) {
        StringBuilder sb = new StringBuilder(withPlayer ? this.player.toString() : StringUtils.EMPTY);

        switch (getPayManaCost()) {
        case YES:
            if (isIgnoreManaCostColor()) {
                sb.append(" (may spend mana as though it were mana of any color to cast it)");
            }
            break;
        case NO:
            sb.append(" (without paying its mana cost");
            if (isWithFlash()) {
                sb.append(" and as though it has flash");
            }
            sb.append(")");
        }
        return sb.toString();
    }

}
