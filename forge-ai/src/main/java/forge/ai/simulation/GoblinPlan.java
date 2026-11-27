package forge.ai.simulation;

import forge.ai.RunawayLoop;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.cost.CostPart;
import forge.game.cost.CostSacrifice;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.function.Predicate;

/**
 * Conspicuous Snoop with Boggart Harbinger, Kiki-Jiki, Mirror Breaker and Sling-Gang Lieutenant. The Snoop has the activated
 * abilities of the Goblin on top of the library:
 * <ol>
 * <li>Boggart Harbinger puts Kiki-Jiki on top: the Snoop taps to copy itself (hasty), each copy taps to copy itself, and so
 * on: a Goblin for each activation, until there are as many as the opponent has life.</li>
 * <li>A last Snoop copies the Harbinger (or the Harbinger is cast again, when it is in the hand), its trigger puts
 * Sling-Gang Lieutenant on top: every Snoop has the ability to sacrifice a Goblin for a life lost and a life gained.</li>
 * <li>The Goblins go one at a time, to the opponent's life. Sling-Gang Lieutenant in the hand with the mana to cast it
 * does the same without the second step: it is cast, and its own ability is used.</li>
 * </ol>
 * A step is one ability or spell at an empty stack.
 */
public final class GoblinPlan {
    private static final String SNOOP = "Conspicuous Snoop";
    private static final String HARBINGER = "Boggart Harbinger";
    private static final String KIKI = "Kiki-Jiki, Mirror Breaker";
    private static final String SLING = "Sling-Gang Lieutenant";
    private static final int LIMIT = 150;

    private GoblinPlan() {
    }

    private static Card top(final Player ai) {
        final CardCollection library = new CardCollection(ai.getCardsIn(ZoneType.Library));
        return library.isEmpty() ? null : library.getFirst();
    }

    private static boolean isNamed(final Card c, final String name) {
        return c != null && c.getName().equals(name);
    }

    private static boolean inPlay(final Player ai, final String name) {
        return LoopRunner.inPlay(ai, name) != null;
    }

    /**
     * The card for Boggart Harbinger to put on top, with the Snoop in play: Kiki-Jiki when it is not on top yet, Sling-Gang
     * Lieutenant when it is (the copies are made, and the Goblins have to go).
     *
     * @return the card to choose, or null if this is not the plan's trigger
     */
    public static Card fetch(final Player ai, final SpellAbility sa, final Iterable<Card> options) {
        final Card host = sa == null ? null : sa.getHostCard();
        if (host == null || !host.getName().equals(HARBINGER) || !inPlay(ai, SNOOP) || !ai.equals(host.getController())) {
            return null;
        }
        final String want = isNamed(top(ai), KIKI) ? SLING : KIKI;
        for (final Card c : options) {
            if (c.getName().equals(want)) {
                return c;
            }
        }
        return null;
    }

    private static int goblins(final Player ai) {
        int n = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            n += c.isCreature() && c.getType().hasCreatureType("Goblin") ? 1 : 0;
        }
        return n;
    }

    /** A Snoop that can tap: untapped and not sick (the copies have haste). */
    private static Card readySnoop(final Player ai, final List<SpellAbility> candidates, final ApiType api) {
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host != null && host.getName().equals(SNOOP) && ai.equals(host.getController()) && sa.getApi() == api
                    && !host.isTapped() && !host.isSick()) {
                return host;
            }
        }
        return null;
    }

    /** The Snoop's copy ability aimed at the creature. */
    private static SpellAbility copy(final Player ai, final List<SpellAbility> candidates, final Card aim,
                                     final Predicate<SpellAbility> approve) {
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !host.getName().equals(SNOOP) || !ai.equals(host.getController())
                    || sa.getApi() != ApiType.CopyPermanent || host.isTapped() || host.isSick() || RunawayLoop.exceeded(sa, LIMIT)) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            sa.resetTargets();
            if (sa.canTarget(aim) && sa.getTargets().add(aim) && approve.test(sa)) {
                return sa;
            }
            sa.resetTargets();
        }
        return null;
    }

    private static boolean sacrificesAGoblin(final SpellAbility sa) {
        if (sa.getPayCosts() == null) {
            return false;
        }
        for (final CostPart part : sa.getPayCosts().getCostParts()) {
            if (part instanceof CostSacrifice && part.getType() != null && part.getType().contains("Goblin")) {
                return true;
            }
        }
        return false;
    }

    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(forge.game.phase.PhaseType.MAIN1) || ph.is(forge.game.phase.PhaseType.MAIN2))) {
            return null;
        }
        Card snoop = null;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals(SNOOP)) {
                snoop = c;
                break;
            }
        }
        Player opp = null;
        for (final Player p : ai.getOpponents()) {
            if (p.getLife() > 0) {
                opp = p;
                break;
            }
        }
        if (snoop == null || opp == null) {
            return null;
        }
        final Card top = top(ai);
        final boolean kikiTop = isNamed(top, KIKI);
        final boolean slingTop = isNamed(top, SLING);
        final int goblins = goblins(ai);

        // the Goblins go: the ability of the Snoops (Sling-Gang Lieutenant on top) or of the Lieutenant itself
        if (goblins >= opp.getLife() && ai.getLife() > 0) {
            for (final SpellAbility sa : candidates) {
                final Card host = sa == null ? null : sa.getHostCard();
                if (host == null || sa.getApi() != ApiType.LoseLife || !sa.isActivatedAbility() || !sacrificesAGoblin(sa)
                        || !ai.equals(host.getController())
                        || !(host.getName().equals(SLING) && host.isInZone(ZoneType.Battlefield)
                        || host.getName().equals(SNOOP) && slingTop)) {
                    continue;
                }
                Card victim = null;
                for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                    if (c.isToken() && c.isCreature() && c.getType().hasCreatureType("Goblin")) {
                        victim = c;
                        break;
                    }
                }
                if (victim != null) {
                    SacrificeLoopPlan.markVictim(ai, victim);
                }
                final SpellAbility shot = LoopRunner.aimed(ai, sa, opp, approve, LIMIT);
                if (shot != null) {
                    VetoLog.note(ai, "goblin-loop | a Goblin to the Lieutenant's ability, " + goblins + " against " + opp.getLife());
                    return shot;
                }
            }
        }

        if (kikiTop) {
            if (goblins < opp.getLife() + 1) {
                final Card aim = readySnoop(ai, candidates, ApiType.CopyPermanent);
                final SpellAbility copy = aim == null ? null : copy(ai, candidates, snoop, approve);
                if (copy != null) {
                    VetoLog.note(ai, "goblin-loop | the Snoop copies itself with Kiki-Jiki's ability, " + goblins + " Goblins of "
                            + (opp.getLife() + 1));
                    return copy;
                }
                return null;
            }
            // enough Goblins: the Lieutenant from the hand if it can be cast, else the Harbinger copied or cast again
            final SpellAbility lieutenant = LoopRunner.castFromHand(ai, candidates, SLING);
            if (lieutenant != null && forge.ai.ComputerUtilMana.canPayManaCost(lieutenant, ai, 0, false)) {
                lieutenant.setActivatingPlayer(ai);
                lieutenant.resetTargets();
                if (approve.test(lieutenant)) {
                    VetoLog.note(ai, "goblin-loop | Sling-Gang Lieutenant from the hand");
                    return lieutenant;
                }
            }
            final Card harbinger = LoopRunner.inPlay(ai, HARBINGER);
            if (harbinger != null) {
                final SpellAbility again = copy(ai, candidates, harbinger, approve);
                if (again != null) {
                    VetoLog.note(ai, "goblin-loop | the Snoop copies the Harbinger for Sling-Gang Lieutenant");
                    return again;
                }
            }
            final SpellAbility cast = LoopRunner.castFromHand(ai, candidates, HARBINGER);
            if (cast != null && forge.ai.ComputerUtilMana.canPayManaCost(cast, ai, 0, false)) {
                cast.setActivatingPlayer(ai);
                cast.resetTargets();
                if (approve.test(cast)) {
                    VetoLog.note(ai, "goblin-loop | the Harbinger again, for Sling-Gang Lieutenant");
                    return cast;
                }
            }
            return null;
        }

        // Kiki-Jiki is not on top: the Harbinger puts it there (when it is in the library)
        if (!slingTop) {
            boolean kikiInLibrary = false;
            for (final Card c : ai.getCardsIn(ZoneType.Library)) {
                kikiInLibrary |= c.getName().equals(KIKI);
            }
            final SpellAbility cast = kikiInLibrary ? LoopRunner.castFromHand(ai, candidates, HARBINGER) : null;
            if (cast != null && forge.ai.ComputerUtilMana.canPayManaCost(cast, ai, 0, false)) {
                cast.setActivatingPlayer(ai);
                cast.resetTargets();
                if (approve.test(cast)) {
                    VetoLog.note(ai, "goblin-loop | the Harbinger puts Kiki-Jiki on top");
                    return cast;
                }
            }
        }
        return null;
    }
}
