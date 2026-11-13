package forge.ai.simulation;

import java.util.List;
import java.util.Map;

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
}
