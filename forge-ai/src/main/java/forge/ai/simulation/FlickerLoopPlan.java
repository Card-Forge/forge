package forge.ai.simulation;

import forge.ai.RunawayLoop;
import forge.card.MagicColor;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.function.Predicate;

/**
 * Flicker loops that go round a creature which untaps lands as it enters (Peregrine Drake: five, Palinchron: seven).
 * <ul>
 * <li><b>Deadeye Navigator.</b> Paired with such a creature, both have "{1}{U}: exile this creature, then return it".
 * Every flicker costs two and gives back up to five lands.</li>
 * <li><b>Ghostly Flicker.</b> With a creature that returns an instant from the graveyard as it enters (Archaeomancer,
 * Mnemonic Wall) next to the untapper, Ghostly Flicker ({2}{U}) flickers both, and the Archaeomancer brings the
 * Flicker back to the hand: three for up to five lands, round and round.</li>
 * </ul>
 * <b>Net.</b> The loop is gone round only when the lands it untaps are more than what the flicker costs, with every land
 * tapped for mana before: so the lands are floated one by one into the pool first, then the flicker is paid from the pool,
 * and the lands that come back are tapped again. Only with something to spend the mana on, and only until the pool holds
 * what the hand costs (the Flicker itself is not counted: it is part of the loop).
 */
public final class FlickerLoopPlan {
    private static final int LIMIT = 80;

    private FlickerLoopPlan() {
    }

    /** The lands the creature untaps as it enters, or 0. */
    static int untaps(final Card c) {
        int best = 0;
        try {
            for (final Trigger t : c.getTriggers()) {
                if (!"ChangesZone".equals(t.getParam("Mode")) || !"Battlefield".equals(t.getParam("Destination"))
                        || !"Card.Self".equals(t.getParam("ValidCard"))) {
                    continue;
                }
                final SpellAbility sa = t.ensureAbility();
                if (sa != null && sa.getApi() == ApiType.Untap && "Land".equals(sa.getParam("UntapType"))) {
                    best = Math.max(best, Integer.parseInt(sa.getParamOrDefault("Amount", "1")));
                }
            }
        } catch (final RuntimeException e) {
            return 0;
        }
        return best;
    }

    private static boolean returnsInstant(final Card c) {
        return c.getName().equals("Archaeomancer") || c.getName().equals("Mnemonic Wall");
    }

    private static int cmc(final SpellAbility sa) {
        return sa.getPayCosts() == null ? 0 : sa.getPayCosts().getTotalMana().getCMC();
    }

    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty()
                || !(ph.is(PhaseType.MAIN1) || ph.is(PhaseType.MAIN2))) {
            return null;
        }
        Card untapper = null;
        int lands = 0;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.isCreature() && untaps(c) > lands) {
                untapper = c;
                lands = untaps(c);
            }
        }
        if (untapper == null) {
            return null;
        }
        final boolean flickerSpell = LoopRunner.inHand(ai, "Ghostly Flicker");
        final int goal = ManaLoopPlan.goal(ai, flickerSpell ? "Ghostly Flicker" : null);
        if (goal <= 0 || ai.getManaPool().totalMana() >= goal) {
            return null;
        }
        final SpellAbility flick = flicker(ai, candidates, untapper, flickerSpell);
        if (flick == null) {
            return null;
        }
        final int cost = cmc(flick);
        if (lands <= cost) {
            return null; // the lands that come back do not pay for the flicker
        }
        final SpellAbility float1 = float1(ai, candidates, approve);
        if (float1 != null) {
            return float1;
        }
        if (ai.getManaPool().totalMana() >= cost && ai.getManaPool().getAmountOfColor(MagicColor.BLUE) >= 1
                && !RunawayLoop.exceeded(flick, LIMIT)) {
            flick.setActivatingPlayer(ai);
            if (approve.test(flick)) {
                VetoLog.note(ai, "flicker-loop | " + untapper.getName() + " back for " + lands + " lands, pool "
                        + ai.getManaPool().totalMana() + " of " + goal);
                return flick;
            }
        }
        return null;
    }

    /** The flicker: Deadeye's ability on the untapper, or Ghostly Flicker aimed at the untapper and its partner. */
    private static SpellAbility flicker(final Player ai, final List<SpellAbility> candidates, final Card untapper,
                                        final boolean spell) {
        if (!spell) {
            for (final SpellAbility sa : candidates) {
                if (sa != null && sa.getHostCard() == untapper && sa.isActivatedAbility() && sa.getApi() == ApiType.ChangeZone
                        && "Exile".equals(sa.getParam("Destination")) && !sa.usesTargeting()) {
                    return sa;
                }
            }
            return null;
        }
        Card partner = null;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c != untapper && c.isCreature() && returnsInstant(c)) {
                partner = c;
            }
        }
        final SpellAbility sa = LoopRunner.castFromHand(ai, candidates, "Ghostly Flicker");
        if (partner == null || sa == null) {
            return null;
        }
        sa.setActivatingPlayer(ai);
        sa.resetTargets();
        if (!sa.canTarget(untapper) || !sa.canTarget(partner)) {
            return null;
        }
        sa.getTargets().add(untapper);
        sa.getTargets().add(partner);
        return sa;
    }

    /** One untapped land into the pool, blue first while the pool has no blue. */
    private static SpellAbility float1(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final boolean needBlue = ai.getManaPool().getAmountOfColor(MagicColor.BLUE) < 1;
        SpellAbility any = null;
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !sa.isManaAbility() || !host.isLand() || host.isTapped() || !ai.equals(host.getController())
                    || !host.isInZone(ZoneType.Battlefield)) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            if (!approve.test(sa)) {
                continue;
            }
            if (needBlue && (ManaConversion.producedColors(sa) & MagicColor.BLUE) != 0) {
                return sa;
            }
            if (any == null) {
                any = sa;
            }
        }
        return any;
    }
}
