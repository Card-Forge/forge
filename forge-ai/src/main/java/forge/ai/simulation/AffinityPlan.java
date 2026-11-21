package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The Affinity decks (Frogmite, Myr Enforcer, Thoughtcast, Master of Etherium, Mox Opal, Cranial Plating, Arcbound
 * Ravager). Every artifact in play makes the next one cheaper, so the order of the casts is the whole turn: the free and
 * cheap artifacts first, the cards that get cheaper with each of them (affinity) after them, Mox Opal once two other
 * artifacts are there (metalcraft), and Cranial Plating last, when there are creatures to hang it on and the mana left
 * over is its. The rollouts see a handful of plays; this is the order they would have to find by luck.
 */
public final class AffinityPlan {
    private static final Set<String> MARKERS = Set.of("Frogmite", "Myr Enforcer", "Thoughtcast", "Master of Etherium",
            "Mox Opal", "Cranial Plating", "Arcbound Ravager", "Springleaf Drum", "Somber Hoverguard", "Tangle Golem");
    private static final int MIN_ARTIFACTS = 16;

    private AffinityPlan() {
    }

    /** A deck of at least sixteen artifacts that has one of the Affinity cards. */
    public static boolean isAffinityDeck(final Player ai) {
        int artifacts = 0;
        boolean marker = false;
        for (final Card c : ai.getCardsIn(ZoneType.Hand, ZoneType.Library, ZoneType.Battlefield, ZoneType.Graveyard)) {
            if (c.isArtifact()) {
                artifacts++;
            }
            marker |= MARKERS.contains(c.getName());
        }
        return marker && artifacts >= MIN_ARTIFACTS;
    }

    private static int artifactsInPlay(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isArtifact()) {
                n++;
            }
        }
        return n;
    }

    private static boolean getsCheaper(final Card c) {
        final String text = c.getOracleText() == null ? "" : c.getOracleText();
        return text.contains("Affinity for artifacts") || text.contains("costs {1} less to cast for each artifact")
                || text.contains("costs {1} less to cast for each");
    }

    /** The position of a card in the casting order: lower is earlier. */
    static int rank(final Player ai, final Card c) {
        final String name = c.getName();
        if (name.equals("Cranial Plating")) {
            return 1000;
        }
        if (name.equals("Mox Opal")) {
            return artifactsInPlay(ai) >= 2 ? 10 : 900; // metalcraft needs three artifacts, itself included
        }
        if (getsCheaper(c)) {
            return 500 + c.getCMC();
        }
        return c.getCMC();
    }

    /** The next artifact of the hand to cast, in the order above, or null (not an Affinity deck, or none left). */
    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        if (!isAffinityDeck(ai)) {
            return null;
        }
        final List<SpellAbility> spells = new ArrayList<>();
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host != null && sa.isSpell() && !sa.isLandAbility() && host.isArtifact() && host.isPermanent()
                    && host.isInZone(ZoneType.Hand) && ai.equals(host.getController())
                    && !(host.getType().isLegendary() && LoopRunner.inPlay(ai, host.getName()) != null)) {
                spells.add(sa);
            }
        }
        spells.sort(Comparator.comparingInt((SpellAbility s) -> rank(ai, s.getHostCard()))
                .thenComparing(s -> s.getHostCard().getName()));
        for (final SpellAbility sa : spells) {
            sa.setActivatingPlayer(ai);
            sa.resetTargets();
            if (approve.test(sa)) {
                return sa;
            }
        }
        return null;
    }
}
