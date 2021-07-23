package forge.game.card;

import org.apache.commons.lang3.StringUtils;

import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;

public final class CardPlayOption {
    public enum PayManaCost {
        /** Indicates the mana cost must be paid. */
        YES,
        /** Indicates the mana cost may not be paid. */
        NO
    }

    private final Player player;
    private final StaticAbility sta;
    private final PayManaCost payManaCost;
    private final boolean withFlash;
    private final boolean grantsZonePermissions;
    private final Cost altManaCost;

    public CardPlayOption(final Player player, final StaticAbility sta, final boolean withoutManaCost, final Cost altManaCost, final boolean withFlash, final boolean grantZonePermissions) {
        this(player, sta, withoutManaCost ? PayManaCost.NO : PayManaCost.YES, altManaCost, withFlash, grantZonePermissions);
    }
    private CardPlayOption(final Player player, final StaticAbility sta, final PayManaCost payManaCost, final Cost altManaCost, final boolean withFlash,
                           final boolean grantZonePermissions) {
        this.player = player;
        this.sta = sta;
        this.payManaCost = payManaCost;
        this.withFlash = withFlash;
        this.grantsZonePermissions = grantZonePermissions;
        this.altManaCost = altManaCost;
    }


    public Player getPlayer() {
        return player;
    }

    public Card getHost() {
        return sta.getHostCard();
    }

    public StaticAbility getAbility() {
        return sta;
    }

    public PayManaCost getPayManaCost() {
        return payManaCost;
    }

    public boolean isIgnoreManaCostColor() {
        return sta.hasParam("MayPlayIgnoreColor");
    }

    public boolean isIgnoreManaCostType() {
        return sta.hasParam("MayPlayIgnoreType");
    }

    public boolean isIgnoreSnowSourceManaCostColor() {
        return sta.hasParam("MayPlaySnowIgnoreColor");
    }

    public boolean isWithFlash() {
    	return withFlash;
    }

    public boolean grantsZonePermissions() { return grantsZonePermissions; }

    public Cost getAltManaCost() { return altManaCost; }

    private String getFormattedAltManaCost() {
        return altManaCost.toSimpleString();
    }

    @Override
    public String toString() {
        return toString(true);
    }

    public String toString(final boolean withPlayer) {
        StringBuilder sb = new StringBuilder(withPlayer ? this.player.toString() : StringUtils.EMPTY);

        switch (getPayManaCost()) {
            case YES:
                if (altManaCost != null) {
                    sb.append(" (by paying ").append(getFormattedAltManaCost()).append(" instead of paying its mana cost");
                    if (isWithFlash()) {
                        sb.append(" and as though it has flash");
                    }
                    sb.append(")");
                }
                if (isIgnoreManaCostColor()) {
                    sb.append(" (may spend mana as though it were mana of any color to cast it)");
                } else if (isIgnoreManaCostType()) {
                    sb.append(" (may spend mana as though it were mana of any type to cast it)");
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
