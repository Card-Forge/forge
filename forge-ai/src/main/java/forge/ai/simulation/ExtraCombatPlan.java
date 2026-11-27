package forge.ai.simulation;

import forge.ai.ComputerUtilCombat;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.function.Predicate;

/**
 * Extra combat phases: Aurelia, the Warleader and Godo, Bandit Warlord untap and add a combat the first time that they
 * attack each turn, and with a Helm of the Host the beginning of every combat makes one more of them with haste.
 * <ul>
 * <li><b>The engine attacks.</b> A creature whose first attack adds a combat phase, or whose damage does (The Incredible
 * Hulk is dealt it by a Caltrops, Bumi, Unleashed deals it to a player and is a land creature with Ashaya), is sent to attack even when the
 * attack logic would have left it at home, as long as nothing that can block it kills it: its attack is worth the
 * combats that follow, and it untaps (Aurelia has vigilance as well).</li>
 * <li><b>The Helm is worn.</b> A Helm of the Host in play that no creature wears is equipped to the engine in the first
 * main phase, when the equip can be paid: the copies of the engine that the Helm makes are what the extra combats are for.</li>
 * </ul>
 */
public final class ExtraCombatPlan {
    private ExtraCombatPlan() {
    }

    /** Whether the creature adds a combat phase when it attacks: a trigger of attacking whose abilities include AddPhase. */
    public static boolean isEngine(final Card c) {
        if (c == null || !c.isCreature()) {
            return false;
        }
        try {
            for (final Trigger t : c.getTriggers()) {
                if (!"Attacks".equals(t.getParam("Mode"))) {
                    continue;
                }
                SpellAbility sa = t.ensureAbility();
                while (sa != null) {
                    if (sa.getApi() == ApiType.AddPhase && "Combat".equals(sa.getParam("ExtraPhase"))) {
                        return true;
                    }
                    sa = sa.getSubAbility();
                }
            }
            return damageEngine(c);
        } catch (final RuntimeException e) {
            return false;
        }
    }

    private static boolean addsACombat(final Trigger t) {
        SpellAbility sa = t.ensureAbility();
        while (sa != null) {
            if (sa.getApi() == ApiType.AddPhase && "Combat".equals(sa.getParam("ExtraPhase"))) {
                return true;
            }
            sa = sa.getSubAbility();
        }
        return false;
    }

    /** Whether something in play deals damage to each creature that attacks (Caltrops). */
    private static boolean pingsAttackers(final Card c) {
        for (final Card other : c.getGame().getCardsIn(ZoneType.Battlefield)) {
            for (final Trigger t : other.getTriggers()) {
                if (!"Attacks".equals(t.getParam("Mode"))) {
                    continue;
                }
                for (SpellAbility sa = t.ensureAbility(); sa != null; sa = sa.getSubAbility()) {
                    if (sa.getApi() == ApiType.DealDamage && sa.getParamOrDefault("Defined", "").contains("TriggeredAttacker")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * A creature whose combat damage adds a combat phase: The Incredible Hulk is dealt damage (by Caltrops, as it attacks),
     * and Bumi, Unleashed deals it to a player (and only land creatures attack in the extra combat, so Bumi has to be one,
     * Ashaya, Soul of the Wild makes it so).
     */
    private static boolean damageEngine(final Card c) {
        for (final Trigger t : c.getTriggers()) {
            final String mode = t.getParam("Mode");
            if (!"DamageDone".equals(mode) && !"DamageDoneOnce".equals(mode) || !addsACombat(t)) {
                continue;
            }
            if ("DamageDoneOnce".equals(mode) && "Card.Self".equals(t.getParam("ValidTarget"))) {
                return pingsAttackers(c);
            }
            if ("DamageDone".equals(mode) && "Player".equals(t.getParam("ValidTarget"))) {
                return c.isLand();
            }
        }
        return false;
    }

    /** An engine that has to deal its damage to a player to add the combat. */
    private static boolean mustConnect(final Card c) {
        for (final Trigger t : c.getTriggers()) {
            if ("DamageDone".equals(t.getParam("Mode")) && "Player".equals(t.getParam("ValidTarget")) && addsACombat(t)) {
                return true;
            }
        }
        return false;
    }

    /** The creatures of the AI's that are engines and can attack now. */
    public static CardCollection engines(final Player ai, final Iterable<Card> attackers) {
        final CardCollection out = new CardCollection();
        for (final Card c : attackers) {
            if (isEngine(c) && ai.equals(c.getController())) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * Whether the engine can attack safely, taking the worst case in which every creature that can block it does so
     * together: their damage added up must not kill it, none of them may have deathtouch, and an engine that has to hurt the
     * player needs trample with more power than the toughness that they add up to (without trample one blocker stops it).
     */
    public static boolean safeToAttack(final Card engine, final Iterable<Card> blockers) {
        final boolean connect = mustConnect(engine);
        int damage = 0;
        int toughness = 0;
        boolean blocked = false;
        for (final Card b : blockers) {
            if (!CombatUtil.canBlock(engine, b)) {
                continue;
            }
            if (b.hasKeyword(forge.game.keyword.Keyword.DEATHTOUCH)) {
                return false;
            }
            blocked = true;
            damage += Math.max(0, b.getNetCombatDamage());
            toughness += Math.max(0, b.getLethalDamage());
        }
        if (!blocked) {
            return true;
        }
        if (damage >= ComputerUtilCombat.getDamageToKill(engine, false)) {
            return false;
        }
        return !connect || engine.hasKeyword(forge.game.keyword.Keyword.TRAMPLE) && engine.getNetCombatDamage() > toughness;
    }

    /** The equip that puts a Helm of the Host that nobody wears on the engine, or null. */
    static SpellAbility next(final Player ai, final List<SpellAbility> candidates, final Predicate<SpellAbility> approve) {
        final forge.game.phase.PhaseHandler ph = ai.getGame().getPhaseHandler();
        if (!ph.isPlayerTurn(ai) || !ai.getGame().getStack().isEmpty() || !ph.is(PhaseType.MAIN1)) {
            return null;
        }
        Card engine = null;
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (isEngine(c) && !c.isEquipped()) {
                engine = c;
            }
        }
        if (engine == null) {
            return null;
        }
        for (final SpellAbility sa : candidates) {
            final Card host = sa == null ? null : sa.getHostCard();
            if (host == null || !host.getName().equals("Helm of the Host") || !host.isInZone(ZoneType.Battlefield)
                    || host.getEquipping() != null || sa.getApi() != ApiType.Attach || !sa.isActivatedAbility()) {
                continue;
            }
            sa.setActivatingPlayer(ai);
            sa.resetTargets();
            if (!sa.canTarget(engine) || !forge.ai.ComputerUtilMana.canPayManaCost(sa, ai, 0, false)) {
                continue;
            }
            sa.getTargets().add(engine);
            if (approve.test(sa)) {
                VetoLog.note(ai, "extra-combat | the Helm goes on " + engine.getName());
                return sa;
            }
        }
        return null;
    }
}
