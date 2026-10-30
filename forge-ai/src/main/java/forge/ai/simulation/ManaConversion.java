package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * What the mana permanents of a player can really make, once the ones that have to be paid for are taken into account.
 * <p>
 * Most mana abilities are free: {@code {T}: Add {G}}. Some cost mana themselves, and fall in two groups:
 * <ul>
 *   <li>Signets ({@code {1},{T}: Add {W}{U}}) make more mana than they use, so they are a net gain of one mana, but only
 *       when something else pays the {1};</li>
 *   <li>filter lands such as Mystic Gate ({@code {W/U},{T}: Add {W}{W}, {W}{U} or {U}{U}}) make exactly as much as they
 *       take, they only change its colors. They make no mana by themselves, and the colors they promise are only there
 *       when the right color goes in.</li>
 * </ul>
 * The board evaluation used to count every one of them as if it were free: a lone Signet or a filter land with
 * nothing to feed it was worth the mana and the colors it can only make with help, and a rollout could not tell a
 * board where they work from one where they sit locked. This class resolves it the way the payment code does it: the
 * free sources first, then the converters, each one only when what is already there pays for it (so one land can
 * start a chain of Signets). Everything is read from the state of the game, nothing is cached.
 */
public final class ManaConversion {
    private ManaConversion() {
    }

    /** A mana ability that must be paid for with mana: a Signet or a filter land. */
    public static boolean isConverter(final SpellAbility ma) {
        if (ma == null || !ma.isManaAbility()) {
            return false;
        }
        final Cost cost = ma.getPayCosts();
        if (cost == null || !cost.hasManaCost()) {
            return false;
        }
        final ManaCost mana = cost.getCostMana().getMana();
        return mana.countX() == 0 && mana.getCMC() > 0;
    }

    /** The colors (a mask of {@link MagicColor#WUBRG}) the ability can make. */
    static byte producedColors(final SpellAbility ma) {
        byte colors = 0;
        for (final byte col : MagicColor.WUBRG) {
            // mana is described by its short letter ("W"), not the color name
            if (ma.canProduce(MagicColor.toShortString(col))) {
                colors |= col;
            }
        }
        return colors;
    }

    /** Whether the player has any Signet or filter land in play: when not, nothing here changes any evaluation. */
    public static boolean hasConverter(final Player p) {
        for (final Card c : p.getCardsIn(ZoneType.Battlefield)) {
            for (final SpellAbility ma : c.getManaAbilities()) {
                if (isConverter(ma)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Like {@link #analyze(Player, boolean)}, but {@link #NONE} for a player without a converter, so that the
     * evaluation of the common board costs a quick look at the mana abilities and nothing more.
     */
    public static Profile analyzeIfNeeded(final Player p, final boolean includeTapped) {
        return hasConverter(p) ? analyze(p, includeTapped) : NONE;
    }

    /** The result of {@link #analyze(Player, boolean)}. */
    public static final class Profile {
        /** The most mana the sources can make at once, converters that have nothing to pay them excluded. */
        public final int mana;
        /** The colors (a mask of {@link MagicColor#WUBRG}) that can be made, locked converters excluded. */
        public final byte colors;
        /** The converters that can be activated, because what is there pays for them. */
        public final List<SpellAbility> usable;
        /** The converters that can not: nothing, or nothing of the right color, is there to pay for them. */
        public final List<SpellAbility> locked;
        private final Set<SpellAbility> lockedSet;
        private final Set<Card> deadHosts;

        Profile(final int mana, final byte colors, final List<SpellAbility> usable, final List<SpellAbility> locked,
                final Set<Card> deadHosts) {
            this.mana = mana;
            this.colors = colors;
            this.usable = Collections.unmodifiableList(usable);
            this.locked = Collections.unmodifiableList(locked);
            this.lockedSet = Collections.newSetFromMap(new IdentityHashMap<SpellAbility, Boolean>());
            this.lockedSet.addAll(locked);
            this.deadHosts = deadHosts;
        }

        /** Whether this mana ability is a converter that nothing can pay for. */
        public boolean isLocked(final SpellAbility ma) {
            return lockedSet.contains(ma);
        }

        /** Whether the permanent makes no mana at all right now: every mana ability it has is a locked converter. */
        public boolean isDead(final Card host) {
            return deadHosts.contains(host);
        }

        public boolean canMake(final byte color) {
            return (colors & color) != 0;
        }
    }

    /** The profile of a player without converters: nothing is locked, nothing is dead. */
    public static final Profile NONE = new Profile(0, (byte) 0, new ArrayList<SpellAbility>(),
            new ArrayList<SpellAbility>(), Collections.<Card>emptySet());

    /**
     * Resolves the mana permanents of the player.
     *
     * @param includeTapped true to judge the mana base (what the player can make on a full set of untapped
     *                      permanents), false for what the untapped ones make right now
     */
    public static Profile analyze(final Player p, final boolean includeTapped) {
        final Map<Card, Integer> freeOut = new LinkedHashMap<>();
        final Map<Card, Byte> outColors = new LinkedHashMap<>();
        final List<SpellAbility> converters = new ArrayList<>();

        for (final Card c : p.getCardsIn(ZoneType.Battlefield)) {
            if (c.getManaAbilities().isEmpty() || !includeTapped && c.isTapped()) {
                continue;
            }
            int out = 0;
            byte colors = 0;
            boolean hasFree = false;
            for (final SpellAbility ma : c.getManaAbilities()) {
                ma.setActivatingPlayer(c.getController());
                if (isConverter(ma)) {
                    converters.add(ma);
                } else {
                    hasFree = true;
                    out = Math.max(out, ma.amountOfManaGenerated(true));
                    colors |= producedColors(ma);
                }
            }
            if (hasFree) {
                freeOut.put(c, out);
                outColors.put(c, colors);
            }
        }

        int pool = 0;
        for (final int out : freeOut.values()) {
            pool += out;
        }

        // the converters that add the most come first, so that one land can start a chain
        final Map<SpellAbility, Integer> net = new IdentityHashMap<>();
        for (final SpellAbility ma : converters) {
            net.put(ma, ma.amountOfManaGenerated(true) - cost(ma).getCMC());
        }
        converters.sort((a, b) -> Integer.compare(net.get(b), net.get(a)));

        final List<SpellAbility> usable = new ArrayList<>();
        final List<SpellAbility> locked = new ArrayList<>();
        final Set<Card> usedHosts = Collections.newSetFromMap(new IdentityHashMap<Card, Boolean>());
        for (final SpellAbility ma : converters) {
            final Card host = ma.getHostCard();
            final ManaCost cost = cost(ma);
            final int own = freeOut.containsKey(host) ? freeOut.get(host) : 0;
            // what the other permanents make: a permanent can not pay for its own ability
            final int others = pool - own;
            byte otherColors = 0;
            for (final Map.Entry<Card, Byte> e : outColors.entrySet()) {
                if (!e.getKey().equals(host)) {
                    otherColors |= e.getValue();
                }
            }
            if (usedHosts.contains(host) || others < cost.getCMC() || !canPayColors(cost, otherColors)) {
                locked.add(ma);
                continue;
            }
            usable.add(ma);
            usedHosts.add(host);
            // the host taps for this instead of its free ability, so it counts what it makes beyond what it did
            pool += Math.max(0, net.get(ma) - own);
            outColors.merge(host, producedColors(ma), (old, add) -> (byte) (old | add));
        }

        byte colors = 0;
        for (final byte b : outColors.values()) {
            colors |= b;
        }

        // a permanent is dead when every mana ability it has is a locked converter
        final Set<Card> dead = Collections.newSetFromMap(new IdentityHashMap<Card, Boolean>());
        for (final SpellAbility ma : locked) {
            final Card host = ma.getHostCard();
            if (!freeOut.containsKey(host) && !usedHosts.contains(host)) {
                dead.add(host);
            }
        }
        return new Profile(pool, colors, usable, locked, dead);
    }

    private static ManaCost cost(final SpellAbility ma) {
        return ma.getPayCosts().getCostMana().getMana();
    }

    /** Each colored part of the cost needs one of the colors that the other permanents can make. */
    private static boolean canPayColors(final ManaCost cost, final byte available) {
        for (final ManaCostShard shard : cost) {
            final int mask = shard.getColorMask();
            if (mask != 0 && (mask & available) == 0) {
                return false;
            }
        }
        return true;
    }

    /** The mana the player has, counting Signets and filter lands only where something pays for them. */
    public static int usableMana(final Player p) {
        return analyze(p, true).mana;
    }

    /**
     * The mana the player has when it has a converter, otherwise {@code fallback}: a board without Signets or filter
     * lands is judged the way it always was.
     */
    public static int usableManaOr(final Player p, final int fallback) {
        return hasConverter(p) ? Math.max(fallback, analyze(p, true).mana) : fallback;
    }
}
