package forge.ai;

import java.util.List;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardLists;
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

    /**
     * The creature is in the fight, or can still be put into it, this turn. The blockers are declared on entering the
     * declare-blockers step, so what has not blocked by then is out of the fight; before that (the declare-attackers step of
     * the opponent's turn) every untapped creature can still block.
     */
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
        final boolean controllersTurn = ph.isPlayerTurn(c.getController()); // a teammate's turn is ours too (Two-Headed Giant)
        switch (phase) {
            case COMBAT_DECLARE_BLOCKERS:
            case COMBAT_FIRST_STRIKE_DAMAGE:
            case COMBAT_DAMAGE:
                return combat != null && (combat.isAttacking(c) || combat.isBlocking(c));
            case COMBAT_DECLARE_ATTACKERS:
                if (controllersTurn) {
                    // declared attackers are in; before any are declared a creature that may attack still can
                    return combat != null && combat.isAttacking(c)
                            || (combat == null || combat.getAttackers().isEmpty()) && CombatUtil.canAttack(c);
                }
                return CombatUtil.canBlock(c); // the blockers are not declared yet: an untapped creature can still block
            case COMBAT_END:
            case MAIN2:
            case END_OF_TURN:
            case CLEANUP:
                return false;
            default:
                // before the attackers: the creature that may attack (summoning sickness, defender, tapped and the effects that
                // stop it count) on its controller's turn, the one that may block on the other's
                return controllersTurn ? CombatUtil.canAttack(c) : CombatUtil.canBlock(c);
        }
    }

    /** One of these creatures is part of, or can still join, the fight. */
    private static boolean anyRelevant(final Iterable<Card> cards) {
        for (final Card c : cards) {
            if (combatRelevant(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether the creatures that this pump is aimed at are all out of the fight, with an empty list meaning "nothing known".
     * Targets chosen are the answer; otherwise every creature of {@code owner} that it could be aimed at, or its defined cards.
     */
    private static boolean allOutOfTheFight(final Player owner, final SpellAbility sa) {
        if (sa.usesTargeting()) {
            if (sa.getTargets() != null && !sa.getTargets().getTargetCards().isEmpty()) {
                return !anyRelevant(sa.getTargets().getTargetCards());
            }
            boolean any = false;
            for (final Card c : owner.getCreaturesInPlay()) {
                if (sa.canTarget(c)) {
                    any = true;
                    if (combatRelevant(c)) {
                        return false;
                    }
                }
            }
            return any;
        }
        // a pump with neither a target nor a "Defined" is for its own creature (Putrid Leech's "Pay 2 life: +2/+2" has none)
        final List<Card> defined = AbilityUtils.getDefinedCards(sa.getHostCard(), sa.getParamOrDefault("Defined", "Self"), sa);
        return !defined.isEmpty() && !anyRelevant(defined);
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
        if (allOutOfTheFight(ai, sa)) {
            return "pump-outside-combat";
        }
        return lifeForNothing(ai, sa);
    }

    /**
     * A pump paid with life on an attacker nobody blocked (Putrid Leech): the extra damage is worth the life only when it
     * kills the opponent. The pump on a creature that is blocked, or that blocks, is the fight's business (it can save the
     * creature), and a pump paid with mana is left to the rollouts: spare mana for more damage costs nothing.
     *
     * @return "pump-life-unblocked", else null
     */
    private static String lifeForNothing(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (!sa.isActivatedAbility() || sa.usesTargeting() || !host.isCreature() || !ai.equals(host.getController())
                || !"Self".equals(sa.getParamOrDefault("Defined", "Self")) || sa.getPayCosts() == null
                || !sa.getPayCosts().hasSpecificCostType(forge.game.cost.CostPayLife.class)) {
            return null;
        }
        final Combat combat = ai.getGame().getPhaseHandler().getCombat();
        if (combat == null || !combat.isAttacking(host) || combat.isBlocked(host)) {
            return null;
        }
        final Player defender = combat.getDefenderPlayerByAttacker(host);
        final int att = sa.hasParam("NumAtt") ? AbilityUtils.calculateAmount(host, sa.getParam("NumAtt"), sa) : 0;
        return defender != null && host.getNetCombatDamage() + att < defender.getLife() ? "pump-life-unblocked" : null;
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
        // a keyword that opens the combat to the creature (haste) is not judged here
        if (sa.getParamOrDefault("KW", "").contains("Haste")) {
            return false;
        }
        // only a real boost: a grant of keywords alone (Arnjlot's Ascent) is the business of the other checks
        final Card host = sa.getHostCard();
        return sa.hasParam("NumAtt") && AbilityUtils.calculateAmount(host, sa.getParam("NumAtt"), sa) > 0
                || sa.hasParam("NumDef") && AbilityUtils.calculateAmount(host, sa.getParam("NumDef"), sa) > 0;
    }

    /**
     * The opponent's pump spell on the stack that changes nothing: it is alone on the stack (nothing for a toughness boost
     * or protection to answer) and the creatures it pumps are not part of the fight. Countering it spends a counterspell on a
     * bluff.
     */
    public static boolean isBait(final Player ai, final SpellAbility spell) {
        if (spell == null || spell.getActivatingPlayer() == null || !spell.getActivatingPlayer().isOpponentOf(ai)
                || !isPlainPump(spell) || ai.getGame().getStack().size() != 1) {
            return false;
        }
        return allOutOfTheFight(spell.getActivatingPlayer(), spell);
    }

    /**
     * Whether the spell or ability does harm to the AI: it is aimed at the AI or its permanents, or its effect reaches more
     * of the AI's permanents than of its caster's.
     */
    private static boolean threatensAi(final Player ai, final SpellAbility spell) {
        final ApiType api = spell.getApi();
        if (spell.usesTargeting()) {
            if (spell.getTargets() == null) {
                return false;
            }
            for (final Card c : spell.getTargets().getTargetCards()) {
                if (c.getController().equals(ai)) {
                    return true;
                }
            }
            for (final Player p : spell.getTargets().getTargetPlayers()) {
                if (p.equals(ai)) {
                    return true;
                }
            }
            return false;
        }
        if ((api == ApiType.DestroyAll || api == ApiType.DamageAll) && spell.hasParam("ValidCards")) {
            final String valid = spell.getParam("ValidCards");
            final Player caster = spell.getActivatingPlayer();
            final int mine = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid, caster, spell.getHostCard(), spell).size();
            final int theirs = CardLists.getValidCards(caster.getCardsIn(ZoneType.Battlefield), valid, caster, spell.getHostCard(), spell).size();
            return mine > theirs;
        }
        return spell.getParamOrDefault("Defined", "").contains("Opponent");
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
        if ((api == ApiType.Destroy || api == ApiType.DestroyAll || api == ApiType.DealDamage || api == ApiType.DamageAll
                || api == ApiType.ChangeZone && spell.usesTargeting() || api == ApiType.Sacrifice) && threatensAi(ai, spell)) {
            value += 60; // it hits the AI's side: a counter saves a card of its own
        }
        return value;
    }
}
