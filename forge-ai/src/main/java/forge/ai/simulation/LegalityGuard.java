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
import forge.game.card.CounterEnumType;
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
            if (host != null && sa.isSpell() && host.isInZone(ZoneType.Hand) && redundantCopy(ai, host)) {
                return "redundant-copy";
            }
            if (xIsZero(ai, sa)) {
                return "x-zero";
            }
            if (drawsPastTheLibrary(ai, sa)) {
                return "draw-would-lose";
            }
            if (takesAllItsLife(ai, sa)) {
                return "suicide";
            }
            return null;
        } catch (final RuntimeException e) {
            return null; // a refinement only: never let it break a turn
        }
    }

    /**
     * A spell or ability that makes the AI draw more cards than its library has loses the game at the next state check
     * (unless something says it cannot lose, or it is the plan: Laboratory Maniac, Jace, Wielder of Mysteries).
     */
    static boolean drawsPastTheLibrary(final Player ai, final SpellAbility sa) {
        if (ai.cantLose()) {
            return false;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Laboratory Maniac") || c.getName().equals("Jace, Wielder of Mysteries")) {
                return false;
            }
        }
        final int library = ai.getCardsIn(ZoneType.Library).size();
        for (SpellAbility node = sa; node != null; node = node.getSubAbility()) {
            if (node.getApi() != ApiType.Draw || node.hasParam("ConditionCheckSVar") || node.hasParam("ConditionPresent")
                    || node.hasParam("UnlessCost") || !affects(ai, node, "You")) {
                continue;
            }
            if (amountOf(node, "NumCards", "1") > library) {
                return true;
            }
        }
        return false;
    }

    /** Whether the step is aimed at the AI: its chosen targets, else the players it names ("You", "Player" for each). */
    static boolean affects(final Player ai, final SpellAbility node, final String defaultDefined) {
        try {
            if (node.usesTargeting()) {
                if (node.getTargets() == null) {
                    return false;
                }
                for (final Player p : node.getTargets().getTargetPlayers()) {
                    if (p.equals(ai)) {
                        return true;
                    }
                }
                return false;
            }
            final String defined = node.getParamOrDefault("Defined", defaultDefined);
            if (defined.isEmpty()) {
                return false;
            }
            return forge.game.ability.AbilityUtils.getDefinedPlayers(node.getHostCard(), defined, node).contains(ai);
        } catch (final RuntimeException e) {
            return false;
        }
    }

    /**
     * The number of a step as the game reads it now: a Count$ expression is counted on the board, so "draw a card for
     * each creature you control" is what it would be if cast now. An X paid in mana is not known before it is paid (the
     * AI's own X choices are capped by the library in DrawAi, and the rollouts do not offer X spells).
     */
    static int amountOf(final SpellAbility node, final String param, final String fallback) {
        return forge.game.ability.AbilityUtils.calculateAmount(node.getHostCard(), node.getParamOrDefault(param, fallback), node);
    }

    /** A step that makes the AI lose all the life it has (or more), with nothing in the same ability to gain it back. */
    static boolean takesAllItsLife(final Player ai, final SpellAbility sa) {
        if (!ai.canLoseLife() || ai.cantLoseForZeroOrLessLife()) {
            return false;
        }
        for (SpellAbility node = sa; node != null; node = node.getSubAbility()) {
            if (node.getApi() == ApiType.GainLife) {
                return false;
            }
        }
        for (SpellAbility node = sa; node != null; node = node.getSubAbility()) {
            final ApiType api = node.getApi();
            if (node.hasParam("ConditionCheckSVar") || node.hasParam("ConditionPresent")) {
                continue;
            }
            if ((api == ApiType.LoseLife && affects(ai, node, "") && amountOf(node, "LifeAmount", "0") >= ai.getLife())
                    || (api == ApiType.DealDamage && affects(ai, node, "") && amountOf(node, "NumDmg", "0") >= ai.getLife())) {
                return true;
            }
            if (api == ApiType.DamageAll && damagesAi(ai, node) && amountOf(node, "NumDmg", "0") >= ai.getLife()
                    && ai.getLife() <= ai.getOpponentsSmallestLifeTotal()) {
                return true; // each player takes it and the AI is the one who dies first (or with him)
            }
        }
        return false;
    }

    private static boolean damagesAi(final Player ai, final SpellAbility node) {
        final String valid = node.getParamOrDefault("ValidPlayers", "");
        return !valid.isEmpty() && ai.isValid(valid, node.getActivatingPlayer(), node.getHostCard(), node);
    }

    // ------------------------------------------------------------------------------------------ wasted burn

    /**
     * Damage that does nothing or far too much: all of it prevented, a creature that is already dead (damage marked
     * for its toughness), or a very big burn spell on a small creature (8 damage on a 2/2 when the opponent has
     * something worth the spell).
     *
     * @param aim the target it is going to be aimed at when the targets are not chosen yet, else null
     * @return "burn-prevented", "burn-dead-already" or "overkill", else null
     */
    public static String wastedBurn(final Player ai, final SpellAbility sa, final forge.game.GameObject aim) {
        for (SpellAbility node = sa; node != null; node = node.getSubAbility()) {
            if (node.getApi() != ApiType.DealDamage || !node.usesTargeting()) {
                continue;
            }
            final List<GameEntity> aimed = new java.util.ArrayList<>();
            if (aim instanceof GameEntity && node == FlatMonteCarloPicker.singleTargetingAbility(sa)) {
                aimed.add((GameEntity) aim);
            } else if (node.getTargets() != null) {
                for (final GameEntity t : node.getTargets().getTargetEntities()) {
                    aimed.add(t);
                }
            }
            if (aimed.isEmpty()) {
                continue;
            }
            final int damage = amountOf(node, "NumDmg", "0");
            if (damage <= 0) {
                continue;
            }
            for (final GameEntity e : aimed) {
                final int dealt = forge.ai.ComputerUtilCombat.predictDamageTo(e, damage, node.getHostCard(), false);
                if (dealt <= 0) {
                    return "burn-prevented";
                }
                if (e instanceof Card && ((Card) e).isCreature() && !((Card) e).isPlaneswalker()) {
                    final Card c = (Card) e;
                    final int lethal = c.getLethalDamage();
                    if (lethal <= 0) {
                        return "burn-dead-already";
                    }
                    if (dealt >= 6 && dealt >= lethal * 3 && forge.ai.ComputerUtilCard.evaluateCreature(c) < 250) {
                        return "overkill";
                    }
                }
            }
        }
        return null;
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

    // ------------------------------------------------------------------------------------------ defeating a battle

    /**
     * Attacking a battle of the AI's own that it can defeat now, when what it turns into is a legendary permanent that
     * the AI already controls (Invasion of Ikoria while Zilortha, Apex of Ikoria is in play; Fiora, Kaladesh, Segovia,
     * Theros, Ulgrotha): the second one only goes to the legend rule. Fine when the legend in play does nothing (it can
     * neither attack nor block: Pacifism, a stun counter), and when the attack cannot take the last defense counter.
     * A Planeswalker back (Invasion of New Phyrexia) is left to the usual rules: a new one with more loyalty is a plan.
     *
     * @param damage the combat damage the AI can attack the battle with
     */
    public static boolean redundantDefeat(final Player ai, final Card battle, final int damage) {
        try {
            if (battle == null || !battle.isBattle() || damage < battle.getCounters(CounterEnumType.DEFENSE)) {
                return false;
            }
            final forge.game.card.CardState back = battle.getAlternateState();
            if (back == null || !back.getType().isLegendary() || back.getType().isPlaneswalker()) {
                return false;
            }
            for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                if (c != battle && back.getName().equals(c.getName()) && !doesNothing(c)) {
                    return true;
                }
            }
            return false;
        } catch (final RuntimeException e) {
            return false;
        }
    }

    /** A creature that can neither attack nor block for the time being. */
    private static boolean doesNothing(final Card c) {
        return c.isCreature() && (c.getCounters(CounterEnumType.STUN) > 0
                || c.hasKeyword("CARDNAME can't attack or block."));
    }

    // ------------------------------------------------------------------------------------------ a second copy

    private static final String[] STACKING = { "AddPower", "AddToughness", "SetPower", "SetToughness", "AddHiddenKeyword",
            "AddCost", "RaiseCost", "Amount", "AddType", "RemoveType", "SetColor", "AddColor" };

    /**
     * A second copy of a permanent whose whole job is a static grant that does not add up (Resonating Lute: "lands you
     * control have ..."): the lands already have the ability, and a second one gives them nothing more. Equipment,
     * Auras, creatures and cards with an enters or other trigger have uses of their own and are left alone.
     */
    static boolean redundantCopy(final Player ai, final Card host) {
        try {
            if (host.isCreature() || host.isEquipment() || host.isAura() || host.isLand() || host.isPlaneswalker()
                    || host.isSaga() || host.getType().hasSubtype("Vehicle") || !host.getTriggers().isEmpty()
                    || host.getStaticAbilities().isEmpty()) {
                return false;
            }
            boolean copy = false;
            for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
                copy |= c != host && c.getName().equals(host.getName());
            }
            if (!copy) {
                return false;
            }
            for (final forge.game.staticability.StaticAbility st : host.getStaticAbilities()) {
                if (!"Continuous".equals(st.getParam("Mode")) || st.getParam("Affected") == null
                        || !(st.hasParam("AddAbility") || st.hasParam("AddKeyword") || st.hasParam("AddTrigger"))) {
                    return false;
                }
                final String affected = st.getParam("Affected");
                if (affected.contains("Self") || affected.contains("Equipped") || affected.contains("Enchanted")) {
                    return false;
                }
                for (final String key : STACKING) {
                    if (st.hasParam(key)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (final RuntimeException e) {
            return false;
        }
    }
}
