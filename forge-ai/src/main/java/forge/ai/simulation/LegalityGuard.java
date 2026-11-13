package forge.ai.simulation;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import forge.ai.ComputerUtilMana;
import forge.game.card.CardLists;

import forge.game.GameEntity;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * Plays that are not legal, or that cannot do anything, whatever the rollouts think of them.
 * <ul>
 *   <li><b>Cast restrictions</b>: a spell or ability the game would not let the AI play right now
 *       ({@code SpellAbility.checkRestrictions}: Grand Abolisher, Teferi, Hero of Dominaria's static, Rule of Law ...).
 *       The heuristics check this, but extra candidates added by discovery (an extra counterspell, a flashback, a
 *       rollout variant) must not get around it: the AI cast Negate on the opponent's turn against Grand Abolisher.</li>
 *   <li><b>Mandatory targeted "enters" effects with nothing to aim at</b>: Journey to Nowhere, Oblivion Ring, Banisher
 *       Priest, Necromancy, Animate Dead-like permanents. A permanent whose enters trigger must target and has no legal
 *       target (or whose hostile trigger could only hit the AI's own permanents) is a wasted card: the rollouts liked
 *       it, because the permanent is a body for the mana.</li>
 * </ul>
 * Reads the real game state only; every question falls back to "no objection" on an unexpected state.
 */
public final class LegalityGuard {
    private LegalityGuard() {
    }

    /** Why the play is not legal or has no use, or null. */
    public static String problem(final Player ai, final SpellAbility sa) {
        if (ai == null || sa == null) {
            return null;
        }
        try {
            sa.setActivatingPlayer(ai);
            if (!sa.checkRestrictions(ai)) {
                return "cast-restricted";
            }
            final Card host = sa.getHostCard();
            if (host != null && sa.isSpell() && host.isPermanent() && !host.isLand() && !sa.isMutate()
                    && host.isInZone(ZoneType.Hand) && etbWithoutTarget(ai, host)) {
                return "etb-no-target";
            }
            if (host != null && sa.isSpell() && host.isPermanent() && host.isInZone(ZoneType.Hand)
                    && sacrificesItselfOnEntering(ai, host)) {
                return "etb-self-sacrifice";
            }
            if (xIsZero(ai, sa)) {
                return "x-zero";
            }
            return null;
        } catch (final RuntimeException e) {
            return null; // a refinement only: never let it break a turn
        }
    }

    /**
     * The permanent has a mandatory, aimed enters trigger and nothing the AI would want to aim it at: no legal target
     * at all, or (for a hostile effect) only permanents the AI controls itself.
     */
    public static boolean etbWithoutTarget(final Player ai, final Card card) {
        for (final Trigger t : card.getTriggers()) {
            final Map<String, String> params = t.getMapParams();
            if (!"ChangesZone".equals(params.get("Mode")) || !"Battlefield".equals(params.get("Destination"))
                    || params.get("ValidCard") == null || !params.get("ValidCard").contains("Self")
                    || params.get("Execute") == null) {
                continue;
            }
            final String script = card.getSVar(params.get("Execute"));
            if (script == null || !script.contains("ValidTgts$") || script.contains("TargetMin$ 0")
                    || script.contains("Optional") || params.containsKey("OptionalDecider")) {
                continue;
            }
            final SpellAbility ability = t.ensureAbility();
            if (ability == null || !ability.usesTargeting()) {
                continue;
            }
            ability.setActivatingPlayer(ai);
            final TargetRestrictions tr = ability.getTargetRestrictions();
            if (tr == null) {
                continue;
            }
            final List<GameEntity> candidates = tr.getAllCandidates(ability);
            if (candidates.isEmpty()) {
                // the permanent is still in the hand here, but it is on the battlefield when its trigger is aimed:
                // it can be its own target (Merfolk Skydiver: +1/+1 counter on target creature you control)
                if (!isHostile(ability) && card.isValid(tr.getValidTgts(), ai, card, ability)) {
                    continue;
                }
                return true;
            }
            if (isHostile(ability)) {
                boolean enemy = false;
                for (final GameEntity e : candidates) {
                    if (e instanceof Player) {
                        enemy |= ai.isOpponentOf((Player) e);
                    } else if (e instanceof Card) {
                        enemy |= ai.isOpponentOf(((Card) e).getController());
                    }
                }
                if (!enemy) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A removal effect on the battlefield: it helps the AI only when it hits something an opponent controls. */
    private static boolean isHostile(final SpellAbility ability) {
        final ApiType api = ability.getApi();
        if (api == ApiType.Destroy || api == ApiType.DealDamage || api == ApiType.Sacrifice || api == ApiType.Tap
                || api == ApiType.Fight || api == ApiType.Counter) {
            return true;
        }
        if (api == ApiType.ChangeZone || api == ApiType.ChangeZoneAll) {
            return "Battlefield".equals(ability.getParam("Origin")) && !ability.hasParam("GainControl");
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------ sacrifice unless

    private static final Pattern UNLESS_COST = Pattern.compile("UnlessCost\\$\\s*(Return|Sac)<(\\d+)/([^/>]+)");

    /**
     * Faerie Impostor and its kind: "when it enters, sacrifice it unless you return (sacrifice) another creature you
     * control". With no other permanent the AI could pay with, the permanent is cast only to be sacrificed.
     */
    static boolean sacrificesItselfOnEntering(final Player ai, final Card card) {
        for (final Trigger t : card.getTriggers()) {
            final Map<String, String> params = t.getMapParams();
            if (!"ChangesZone".equals(params.get("Mode")) || !"Battlefield".equals(params.get("Destination"))
                    || params.get("ValidCard") == null || !params.get("ValidCard").contains("Self")
                    || params.get("Execute") == null) {
                continue;
            }
            final String script = card.getSVar(params.get("Execute"));
            if (script == null || !script.contains("DB$ Sacrifice") || script.contains("Defined$ ")
                    && !script.contains("Defined$ Self")) {
                continue;
            }
            final Matcher m = UNLESS_COST.matcher(script);
            if (!m.find() || script.contains("UnlessPayer$ Opponent")) {
                continue;
            }
            final int need = Integer.parseInt(m.group(2));
            final int have = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), m.group(3), ai, card, null).size();
            if (have < need) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------ X of nothing

    /**
     * A spell whose cost has an X that the AI's mana cannot make one (a Hydra that enters with X counters, a clone that
     * copies X creatures, a token maker with X tokens): X of nothing is a card thrown away. Chalice of the Void is the
     * exception (X of zero is a real choice there), and so is a card that says so with {@code AIXZeroOk}.
     */
    static boolean xIsZero(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (host == null || !sa.isSpell() || !host.isInZone(ZoneType.Hand) || !sa.costHasManaX()
                || XChoice.isChalice(sa) || host.hasSVar("AIXZeroOk")
                || "ChaliceOfTheVoid".equals(host.getSVar("AICurseEffect"))) {
            return false;
        }
        final SpellAbility root = sa.getRootAbility();
        final Integer before = root.getXManaCostPaid();
        try {
            return ComputerUtilMana.determineLeftoverMana(root, ai, false) < 1;
        } finally {
            root.setXManaCostPaid(before);
        }
    }

    // ------------------------------------------------------------------------------------------ dormant lands

    private static final Pattern PRESENT_COMPARE = Pattern.compile("GE(\\d+)");

    /**
     * A land whose mana abilities all need more lands than the AI will have after this land drop (Temple of the False
     * God: "activate only if you control five or more lands"). Played first, it is a land that makes no mana for turns.
     */
    public static boolean isDormantLand(final Player ai, final Card land) {
        try {
            if (land == null || !land.isLand() || land.getManaAbilities().isEmpty()) {
                return false;
            }
            final int lands = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), Card::isLand).size() + 1;
            for (final SpellAbility m : land.getManaAbilities()) {
                final String present = m.getParamOrDefault("IsPresent", "");
                final Matcher n = PRESENT_COMPARE.matcher(m.getParamOrDefault("PresentCompare", ""));
                if (!present.startsWith("Land") || !n.matches() || Integer.parseInt(n.group(1)) <= lands) {
                    return false;
                }
            }
            return true;
        } catch (final RuntimeException e) {
            return false;
        }
    }
}
