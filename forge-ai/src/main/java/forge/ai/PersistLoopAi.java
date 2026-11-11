package forge.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterEnumType;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostSacrifice;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

/**
 * free sacrifice outlet (Viscera Seer, Nantuko Husk, Bloodthrone Vampire) + persist creature that cannot
 * get -1/-1 counters (Melira, Sylvok Outcast) + damage when it enters (Murderous Redcap): endless damage. The AI keeps
 * sacrificing that creature, whatever the outlet does, as long as the opponent can lose life.
 * The loop stops when a full turn of it does not lower the opponent's life (Soul Warden, Auriok Champion gain it back):
 * otherwise the game runs until the time limit.
 */
public final class PersistLoopAi {
    private PersistLoopAi() { }

    /** Per game and AI player id: turn, game timestamp of the loop card, opponent's life when that copy entered the loop. */
    private static final Map<Game, Map<Integer, long[]>> LAST = new WeakHashMap<>();

    /** The outlet's cost is exactly "sacrifice one creature" (not itself), with no target to choose. */
    static CostSacrifice freeSacCost(final SpellAbility sa) {
        if (sa == null || !sa.isActivatedAbility() || sa.usesTargeting()) {
            return null;
        }
        final Cost cost = sa.getPayCosts();
        if (cost == null) {
            return null;
        }
        CostSacrifice sac = null;
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice cs && !cs.payCostFromSource() && "1".equals(cs.getAmount())) {
                sac = cs;
            } else {
                return null;
            }
        }
        return sac;
    }

    static boolean dealsDamageOnEnter(final Card c) {
        for (final Trigger t : c.getTriggers()) {
            if (t.getMode() != TriggerType.ChangesZone || !"Battlefield".equals(t.getParam("Destination"))
                    || !"Card.Self".equals(t.getParam("ValidCard"))) {
                continue;
            }
            final SpellAbility ab = t.ensureAbility();
            if (ab != null && ab.getApi() == ApiType.DealDamage && ab.usesTargeting()
                    && ab.getTargetRestrictions().canTgtPlayer() && c.getNetPower() > 0) {
                return true;
            }
            // "target opponent loses N life" (Geralf's Messenger) drains just as well as damage.
            if (ab != null && ab.getApi() == ApiType.LoseLife
                    && (ab.usesTargeting() ? ab.getTargetRestrictions().canTgtPlayer()
                            : ab.getParamOrDefault("Defined", "").contains("Opponent"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Persist brings the creature back with no -1/-1 counter, so it can die again. True when it cannot get
     * -1/-1 counters at all (Melira, static) or when a permanent of its controller replaces "N -1/-1 counters" with
     * "N minus one" on it (Vizier of Remedies, replacement effect: the single persist counter becomes none). Read on
     * the script's shape (AddCounter replacement, M1M1, ReplaceCounter with an amount "minus"), not on card names.
     * Without it, Murderous Redcap + Vizier of Remedies + Cartel Aristocrat attacked into walls instead of looping.
     */
    static boolean persistsClean(final Player ai, final Card c) {
        if (!c.canReceiveCounters(CounterEnumType.M1M1)) {
            return true;
        }
        for (final Card host : ai.getCardsIn(ZoneType.Battlefield)) {
            for (final ReplacementEffect re : host.getReplacementEffects()) {
                if (re.getMode() != ReplacementType.AddCounter || !"M1M1".equals(re.getParam("ValidCounterType"))
                        || !re.hasParam("ValidCard")
                        || !c.isValid(re.getParam("ValidCard").split(","), host.getController(), host, null)) {
                    continue;
                }
                final SpellAbility rsa = re.ensureAbility();
                if (rsa == null || rsa.getApi() != ApiType.ReplaceCounter) {
                    continue;
                }
                final String amount = rsa.getParamOrDefault("Amount", "");
                final String x = host.getSVar(amount);
                if ("0".equals(amount) || (x != null && x.contains("CounterNum/Minus."))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The creature comes back from the graveyard able to die again. Persist with no -1/-1 counter and
     * {@link #persistsClean}; or undying with no +1/+1 counter on a creature that cannot get +1/+1 counters (Solemnity,
     * static CantPutCounter): Geralf's Messenger + Solemnity + Cartel Aristocrat attacked into walls instead of looping.
     */
    public static boolean loopsClean(final Player ai, final Card c) {
        if (c.hasKeyword(Keyword.PERSIST) && c.getCounters(CounterEnumType.M1M1) == 0 && persistsClean(ai, c)) {
            return true;
        }
        return c.hasKeyword(Keyword.UNDYING) && c.getCounters(CounterEnumType.P1P1) == 0
                && !c.canReceiveCounters(CounterEnumType.P1P1);
    }

    /** The creature to sacrifice for the loop, or null when there is no winning loop with this ability. */
    public static Card loopCard(final Player ai, final SpellAbility sa) {
        final CostSacrifice sac = freeSacCost(sa);
        if (sac == null) {
            return null;
        }
        final Player opp = ai.getWeakestOpponent();
        if (opp == null || !opp.canLoseLife() || opp.cantLoseForZeroOrLessLife()) {
            return null;
        }
        final Card source = sa.getHostCard();
        CardCollection fodder = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), sac.getType().split(";"), ai, source, sa);
        fodder = CardLists.filter(fodder, CardPredicates.canBeSacrificedBy(sa, false));
        for (final Card c : fodder) {
            if (c != source && !c.isToken() && c.getOwner() == ai && loopsClean(ai, c)
                    && dealsDamageOnEnter(c)) {
                return c;
            }
        }
        return null;
    }

    /**
     * Altar of Dementia + Melira + Kitchen Finks: the outlet mills an opponent by the sacrificed creature's power and the
     * persist creature comes back each time. The creature to sacrifice, or null. Each activation empties the library a
     * bit more, so the loop always ends: no runaway check needed.
     */
    /**
     * cards our mill abilities already on the stack will take from this player's library.
     * Without it, the loop kept stacking Altar of Dementia activations (the persist trigger resolves
     * on top, the Altar abilities never do) until the stack passed 999 objects and Forge called a draw.
     */
    public static int pendingMill(final Player ai, final Player target) {
        int total = 0;
        for (final SpellAbilityStackInstance si : ai.getGame().getStack()) {
            final SpellAbility s = si.getSpellAbility();
            if (s == null || s.getApi() != ApiType.Mill || si.getActivatingPlayer() != ai
                    || si.getTargetChoices() == null) {
                continue;
            }
            for (final Player p : si.getTargetChoices().getTargetPlayers()) {
                if (p == target) {
                    int n = 1;
                    if (s.hasParam("NumCards")) {
                        try {
                            n = AbilityUtils.calculateAmount(s.getHostCard(), s.getParam("NumCards"), s);
                        } catch (RuntimeException e) {
                            n = 1;
                        }
                    }
                    total += Math.max(1, n);
                }
            }
        }
        return total;
    }

    public static Card millLoopCard(final Player ai, final SpellAbility sa) {
        if (sa == null || !sa.isActivatedAbility() || sa.getApi() != ApiType.Mill || !sa.usesTargeting()) {
            return null;
        }
        final Cost cost = sa.getPayCosts();
        if (cost == null) {
            return null;
        }
        CostSacrifice sac = null;
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice cs && !cs.payCostFromSource() && "1".equals(cs.getAmount())) {
                sac = cs;
            } else {
                return null;
            }
        }
        if (sac == null) {
            return null;
        }
        boolean target = false;
        for (final Player o : ai.getOpponents()) {
            if (sa.canTarget(o) && o.getCardsIn(ZoneType.Library).size() > pendingMill(ai, o)) {
                target = true;
            }
        }
        if (!target) {
            return null;
        }
        final Card source = sa.getHostCard();
        CardCollection fodder = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), sac.getType().split(";"), ai, source, sa);
        fodder = CardLists.filter(fodder, CardPredicates.canBeSacrificedBy(sa, false));
        for (final Card c : fodder) {
            if (c != source && !c.isToken() && c.getOwner() == ai && loopsClean(ai, c)
                    && c.getNetPower() > 0) {
                return c;
            }
        }
        return null;
    }

    /**
     * The life the AI loops up to with Melira + Kitchen Finks (each turn, as long as each loop gains it).
     * Tournament v20, Pod: Melira + Finks + Bloodthrone Vampire on the battlefield for 10 turns, never looped.
     */
    public static final int LIFE_GOAL = 60;

    static boolean gainsLifeOnEnter(final Card c) {
        for (final Trigger t : c.getTriggers()) {
            if (t.getMode() != TriggerType.ChangesZone || !"Battlefield".equals(t.getParam("Destination"))
                    || !"Card.Self".equals(t.getParam("ValidCard"))) {
                continue;
            }
            final SpellAbility ab = t.ensureAbility();
            if (ab != null && ab.getApi() == ApiType.GainLife && !ab.usesTargeting()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Free outlet + persist creature that cannot get -1/-1 counters and gains life when it enters (Kitchen
     * Finks + Melira): the creature to sacrifice while the AI is under {@link #LIFE_GOAL}, or null.
     */
    public static Card lifeLoopCard(final Player ai, final SpellAbility sa) {
        final CostSacrifice sac = freeSacCost(sa);
        if (sac == null || ai.getLife() >= LIFE_GOAL || !ai.canGainLife()) {
            return null;
        }
        final Card source = sa.getHostCard();
        CardCollection fodder = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), sac.getType().split(";"), ai, source, sa);
        fodder = CardLists.filter(fodder, CardPredicates.canBeSacrificedBy(sa, false));
        for (final Card c : fodder) {
            if (c != source && !c.isToken() && c.getOwner() == ai && loopsClean(ai, c)
                    && gainsLifeOnEnter(c)) {
                return c;
            }
        }
        return null;
    }

    /** Any winning loop with this ability (damage, mill, life): the creature to sacrifice, or null. */
    public static Card anyLoopCard(final Player ai, final SpellAbility sa) {
        Card c = loopCard(ai, sa);
        if (c == null) {
            c = millLoopCard(ai, sa);
        }
        if (c == null) {
            c = lifeLoopCard(ai, sa);
        }
        return c;
    }

    /** Activate the outlet now: the loop is on and nothing is waiting on the stack. */
    public static boolean shouldLoop(final Player ai, final SpellAbility sa) {
        if (!ai.getGame().getStack().isEmpty()) {
            return false;
        }
        final Card loop = loopCard(ai, sa);
        if (loop != null) {
            return makesProgress(ai, loop);
        }
        final Card life = lifeLoopCard(ai, sa);
        return life != null && gainsProgress(ai, life);
    }

    /** Per game and AI player id: turn, timestamp of the life loop card, AI life when that copy entered the loop. */
    private static final Map<Game, Map<Integer, long[]>> LAST_LIFE = new WeakHashMap<>();

    /** False once the life loop card came back in the same turn without the AI gaining life (Sulfuric Vortex). */
    private static boolean gainsProgress(final Player ai, final Card loop) {
        final int turn = ai.getGame().getPhaseHandler().getTurn();
        final long life = ai.getLife();
        synchronized (LAST_LIFE) {
            final Map<Integer, long[]> byPlayer = LAST_LIFE.computeIfAbsent(ai.getGame(), g -> new HashMap<>());
            final long[] prev = byPlayer.get(ai.getId());
            if (prev != null && prev[0] == turn) {
                if (prev[1] == loop.getGameTimestamp()) {
                    return true;
                }
                if (life <= prev[2]) {
                    return false;
                }
            }
            byPlayer.put(ai.getId(), new long[] {turn, loop.getGameTimestamp(), life});
            return true;
        }
    }

    /** False once the loop card came back (new timestamp) in the same turn without the opponent losing life. */
    private static boolean makesProgress(final Player ai, final Card loop) {
        final int turn = ai.getGame().getPhaseHandler().getTurn();
        final long life = ai.getWeakestOpponent().getLife();
        synchronized (LAST) {
            final Map<Integer, long[]> byPlayer = LAST.computeIfAbsent(ai.getGame(), g -> new HashMap<>());
            final long[] prev = byPlayer.get(ai.getId());
            if (prev != null && prev[0] == turn) {
                if (prev[1] == loop.getGameTimestamp()) {
                    return true; // same copy, evaluated again before it was sacrificed
                }
                if (life >= prev[2]) {
                    return false;
                }
            }
            byPlayer.put(ai.getId(), new long[] {turn, loop.getGameTimestamp(), life});
            return true;
        }
    }
}
