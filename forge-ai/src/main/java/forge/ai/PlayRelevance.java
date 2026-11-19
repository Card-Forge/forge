package forge.ai;

import java.util.ArrayList;
import java.util.List;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Whether a temporary pump can matter at this moment, for the AI's own plays (a Colossus pumped while it is tapped and
 * not in the fight) and for the opponent's (a pump spell cast to bait a counterspell). A "gets +X/+X until end of turn"
 * is worth something only to a creature that is, or can still become, an attacker or a blocker this turn; the stack must
 * be empty of anything it could answer (a burn spell that the toughness could survive is another matter).
 */
public final class PlayRelevance {
    private PlayRelevance() {
    }

    /** The creature is in the fight, or can still be put into it, this turn. */
    public static boolean combatRelevant(final Card c) {
        if (c == null || !c.isCreature()) {
            return true; // not judged here
        }
        final Game game = c.getGame();
        final PhaseHandler ph = game.getPhaseHandler();
        final PhaseType phase = ph.getPhase();
        if (phase == null) {
            return true;
        }
        final Combat combat = ph.getCombat();
        final boolean controllersTurn = c.getController().equals(ph.getPlayerTurn());
        switch (phase) {
            case COMBAT_DECLARE_BLOCKERS:
            case COMBAT_FIRST_STRIKE_DAMAGE:
            case COMBAT_DAMAGE:
                return combat != null && (combat.isAttacking(c) || combat.isBlocking(c));
            case COMBAT_DECLARE_ATTACKERS:
                if (controllersTurn) {
                    return combat != null && combat.isAttacking(c);
                }
                return CombatUtil.canBlock(c); // the blockers are not declared yet: only an untapped creature can still block
            case COMBAT_END:
            case MAIN2:
            case END_OF_TURN:
            case CLEANUP:
                return false;
            default:
                // before the attackers: the creature of the player whose turn it is can attack, the other one can block
                return controllersTurn ? CombatUtil.canAttack(c) : CombatUtil.canBlock(c);
        }
    }

    /**
     * An activated ability or spell of a plain "+X/+X until end of turn" whose creatures cannot take part in combat any
     * more this turn (tapped, not blocking, not attacking, or the fight is over), with nothing on the stack to answer.
     *
     * @return "pump-outside-combat", else null
     */
    public static String pointlessPump(final Player ai, final SpellAbility sa) {
        if (!isPlainPump(sa) || !ai.getGame().getStack().isEmpty()) {
            return null;
        }
        final List<Card> affected = new ArrayList<>();
        if (sa.usesTargeting()) {
            if (sa.getTargets() != null && !sa.getTargets().getTargetCards().isEmpty()) {
                affected.addAll(sa.getTargets().getTargetCards());
            } else {
                for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                    if (c.isCreature() && sa.canTarget(c)) {
                        affected.add(c);
                    }
                }
            }
        } else if (sa.hasParam("Defined")) {
            affected.addAll(AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParam("Defined"), sa));
        }
        if (affected.isEmpty()) {
            return null;
        }
        for (final Card c : affected) {
            if (combatRelevant(c)) {
                return null;
            }
        }
        return "pump-outside-combat";
    }

    private static boolean isPlainPump(final SpellAbility sa) {
        if (sa == null || sa.getApi() != ApiType.Pump || sa.isTrigger() || sa.isCurse() || sa.getSubAbility() != null
                || sa.getHostCard() == null) {
            return false;
        }
        if (sa.hasParam("Duration") && !"UntilEndOfTurn".equals(sa.getParam("Duration"))) {
            return false;
        }
        if (sa.hasParam("AtEOT") || sa.hasParam("RememberObjects") || sa.hasParam("Optional")) {
            return false;
        }
        // a keyword that opens the combat to the creature (haste) or is not about the fight at all is not judged here
        final String kw = sa.getParamOrDefault("KW", "");
        return !kw.contains("Haste");
    }

    /**
     * The opponent's pump spell on the stack that changes nothing: it is alone on the stack and the creatures it pumps are
     * not part of the fight. Countering it spends a counterspell on a bluff.
     */
    public static boolean isBait(final Player ai, final SpellAbility spell) {
        if (spell == null || spell.getActivatingPlayer() == null || !spell.getActivatingPlayer().isOpponentOf(ai)
                || !isPlainPump(spell)) {
            return false;
        }
        final Game game = ai.getGame();
        if (game.getStack().size() != 1) {
            return false; // it answers something: a toughness boost against burn is a real play
        }
        final List<Card> affected = new ArrayList<>();
        if (spell.usesTargeting()) {
            affected.addAll(spell.getTargets().getTargetCards());
        } else if (spell.hasParam("Defined")) {
            affected.addAll(AbilityUtils.getDefinedCards(spell.getHostCard(), spell.getParam("Defined"), spell));
        }
        if (affected.isEmpty()) {
            return false;
        }
        for (final Card c : affected) {
            if (combatRelevant(c)) {
                return false;
            }
        }
        return true;
    }

    /** What an opposing spell or ability on the stack is worth to its caster, for choosing the one to counter. */
    public static int stackValue(final Player ai, final SpellAbility spell) {
        final Card host = spell.getHostCard();
        if (host == null) {
            return 0;
        }
        if (isBait(ai, spell)) {
            return 0;
        }
        int value = 60 + 40 * Math.max(0, spell.getPayCosts() != null && spell.getPayCosts().getTotalMana() != null
                ? spell.getPayCosts().getTotalMana().getCMC() : host.getCMC());
        if (spell.isSpell() && host.isCreature()) {
            value = Math.max(value, ComputerUtilCard.evaluateCreature(host));
        }
        final ApiType api = spell.getApi();
        if (api == ApiType.Destroy || api == ApiType.DestroyAll || api == ApiType.DealDamage || api == ApiType.DamageAll
                || api == ApiType.ChangeZone && spell.usesTargeting() || api == ApiType.Sacrifice) {
            value += 60; // aimed at the AI's side: a counter saves a card of its own
        }
        return value;
    }
}
