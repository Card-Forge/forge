package forge.ai.simulation;

import java.util.ArrayList;
import java.util.List;

import forge.ai.ComputerUtilCard;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CounterEnumType;
import forge.game.cost.Cost;
import forge.game.cost.CostDiscard;
import forge.game.cost.CostExile;
import forge.game.cost.CostPart;
import forge.game.cost.CostPutCounter;
import forge.game.cost.CostRemoveCounter;
import forge.game.cost.CostReturn;
import forge.game.cost.CostSacrifice;
import forge.game.cost.CostTapType;
import forge.game.cost.CostUntapType;
import forge.game.GameEntity;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

/**
 * An activated ability that would do nothing: Oran-Rief, the Vastwood with no green creature that entered this turn, a
 * "tap target creature" with no creature to tap, a Planeswalker's "up to one target creature" with none in play.
 *
 * <p>The heuristics refuse these, but Flat Monte Carlo discovery treats a counter, token or animate effect as
 * development and re-adds it, and the rollout cannot see that the tap (and the mana) bought nothing. This guard reads
 * the ability itself, effect by effect, and says "idle" only when every effect in its chain is certain to do nothing
 * and nothing else is gained.
 *
 * <p>Each effect of the chain (the ability and its sub-abilities) is judged:
 * <ul>
 *   <li><b>targeted</b>: no legal candidate for the targets it needs (or, for "up to", no candidate at all), or fewer
 *   candidates than the minimum;</li>
 *   <li><b>"All" effects</b> (PutCounterAll, PumpAll, UntapAll, TapAll, DamageAll, DestroyAll, AnimateAll,
 *   ProtectionAll, RemoveCounterAll, SacrificeAll, TapOrUntapAll, ChangeZoneAll): the cards they name are not there (and for
 *   the one-sided ones, none on the side they would help or hurt);</li>
 *   <li><b>choices made on resolution</b>: a sacrifice with nothing to sacrifice, a card to choose with none to choose
 *   from, a zone change with nothing of the type in the zone it looks at;</li>
 *   <li>an effect that depends on one of those (it uses the target, the chosen or the remembered card) is idle with it;</li>
 *   <li>every other effect (draw, life, mana, tokens, ...) is taken to do something.</li>
 * </ul>
 * A Planeswalker ability that adds loyalty is never idle: the loyalty is worth the activation. Separately, an ability
 * whose cost cannot be paid (nothing to sacrifice, discard or tap for it) is refused.
 */
public final class ActivationGuard {
    private ActivationGuard() {
    }

    /** @return true for an activated, non-mana ability on the battlefield: the abilities this guard has an opinion about. */
    public static boolean isActivated(final SpellAbility sa) {
        return sa != null && sa.isActivatedAbility() && !sa.isManaAbility() && sa.getHostCard() != null
                && sa.getHostCard().isInZone(ZoneType.Battlefield);
    }

    /**
     * @return "no-valid-targets", "nothing-to-affect" or "cost-unpayable" when the ability would do nothing or cannot be
     * paid, else null
     */
    public static String problem(final Player ai, final SpellAbility sa) {
        if (ai == null || !isActivated(sa)) {
            return null;
        }
        try {
            return problemImpl(ai, sa);
        } catch (final RuntimeException e) {
            return null; // a refinement only: never let it break a turn
        }
    }

    private static String problemImpl(final Player ai, final SpellAbility sa) {
        if (costUnpayable(ai, sa)) {
            return "cost-unpayable";
        }
        if (onlyValuableSacrifice(ai, sa)) {
            return "valuable-sacrifice";
        }
        if (loyaltyGain(sa)) {
            return null; // a Planeswalker's plus ability is worth its loyalty whatever the effect does
        }
        boolean providerIdle = false;
        boolean rootTargeted = false;
        int index = 0;
        for (SpellAbility node = sa; node != null; node = node.getSubAbility(), index++) {
            if (node.getApi() == ApiType.Cleanup) {
                continue; // bookkeeping at the end of a chain (clear the remembered cards): not an effect
            }
            final Verdict v = judge(ai, node);
            if (v == Verdict.USEFUL && providerIdle && dependsOnProvider(node)) {
                continue; // it acts on what the idle effect would have chosen: idle with it
            }
            if (v == Verdict.USEFUL) {
                return null;
            }
            if (v == Verdict.IDLE_TARGETS && index == 0) {
                rootTargeted = true;
            }
            if (v == Verdict.IDLE_TARGETS || v == Verdict.IDLE_CHOICE) {
                providerIdle = true;
            }
        }
        return rootTargeted ? "no-valid-targets" : "nothing-to-affect";
    }

    private enum Verdict { USEFUL, IDLE_TARGETS, IDLE_CHOICE, IDLE }

    // ------------------------------------------------------------------------------------------ cost

    /** The costs that need a card to pay with: nothing to sacrifice, discard, tap or exile for them. */
    private static boolean costUnpayable(final Player ai, final SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        if (cost == null) {
            return false;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice || part instanceof CostDiscard || part instanceof CostTapType
                    || part instanceof CostUntapType || part instanceof CostReturn || part instanceof CostExile
                    || part instanceof CostRemoveCounter) {
                if (!part.canPay(sa, ai, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The most a creature may be worth to be sacrificed to an outlet for what the outlet gives (a weak token or creature). */
    static final int FODDER_VALUE = 140;

    /**
     * A sacrifice outlet (Viscera Seer, Ashnod's Altar, Carrion Feeder) offered to the rollouts only when there is fodder:
     * a token, a creature marked to be sacrificed, or a weak one. The cost is paid with the AI's worst creature, and
     * the rollouts cannot see what a sacrificed good creature was worth beyond the evaluator's number.
     */
    private static boolean onlyValuableSacrifice(final Player ai, final SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        if (cost == null) {
            return false;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (!(part instanceof CostSacrifice)) {
                continue;
            }
            final String type = part.getType();
            if (type == null || "CARDNAME".equals(type) || part.payCostFromSource()) {
                continue; // the permanent itself: a sacrifice of its own
            }
            boolean any = false;
            for (final Card c : CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), type.split("/")[0],
                    ai, sa.getHostCard(), sa)) {
                any = true;
                if (!c.isCreature() || c.isToken() || c.hasSVar("SacMe")
                        || ComputerUtilCard.evaluateCreature(c) <= FODDER_VALUE) {
                    return false;
                }
            }
            if (any) {
                return true;
            }
        }
        return false;
    }

    /** A loyalty ability that puts loyalty counters on the Planeswalker (+N, or an X it cannot read). */
    public static boolean gainsLoyalty(final SpellAbility sa) {
        return loyaltyGain(sa);
    }

    private static boolean loyaltyGain(final SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        if (cost == null || !sa.isPwAbility()) {
            return false;
        }
        for (final CostPart part : cost.getCostParts()) {
            if (part instanceof CostPutCounter && ((CostPutCounter) part).getCounter().is(CounterEnumType.LOYALTY)) {
                try {
                    return Integer.parseInt(part.getAmount().trim()) > 0;
                } catch (final NumberFormatException e) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------ one effect

    private static Verdict judge(final Player ai, final SpellAbility node) {
        if (node.usesTargeting()) {
            if (node.getTargetRestrictions() == null) {
                return Verdict.USEFUL;
            }
            final Verdict refined = targetedRefinement(ai, node);
            if (refined != null) {
                return refined;
            }
            final int min = node.getMinTargets();
            final boolean idle = min > 0
                    ? node.getTargetRestrictions().getNumCandidates(node) < min
                    : !node.getTargetRestrictions().hasCandidates(node);
            return idle ? Verdict.IDLE_TARGETS : Verdict.USEFUL;
        }
        final ApiType api = node.getApi();
        if (api == null || node.hasParam("Defined") && api != ApiType.Sacrifice) {
            return Verdict.USEFUL;
        }
        final Card host = node.getHostCard();
        switch (api) {
            case PutCounterAll:
            case PumpAll:
            case UntapAll:
            case TapAll:
            case DamageAll:
            case DestroyAll:
            case AnimateAll:
            case ProtectionAll:
            case RemoveCounterAll:
            case SacrificeAll:
            case TapOrUntapAll:
                return allEffect(ai, node, api, host);
            case ChangeZoneAll:
                return changeZoneAll(ai, node, host);
            case Sacrifice:
                return sacrifice(ai, node, host);
            case ChooseCard:
                return chooseCard(ai, node, host);
            case ChangeZone:
                return changeZone(ai, node, host);
            default:
                return Verdict.USEFUL;
        }
    }

    /**
     * The counters go on something that is not a creature yet, and a later part of the same ability makes it one
     * (Case of the Filched Falcon: counters on a noncreature artifact, which then becomes a 0/0 creature).
     */
    static boolean animatesTargetToCreature(final SpellAbility node) {
        for (SpellAbility a = node; a != null; a = a.getSubAbility()) {
            if (a.getApi() == ApiType.Animate && a.getParamOrDefault("Types", "").contains("Creature")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Two targeted effects whose candidates are not all worth the ability. A +1/+1 counter belongs on a creature
     * (Ozolith, the Shattered Spire and its like also name noncreature artifacts, and a counter there does nothing),
     * and an Equipment moved onto the creature it is already attached to does nothing, unless something in play
     * pays for every attachment (a "becomes attached" trigger), which is what makes an equip {0} loop worth going round.
     */
    private static Verdict targetedRefinement(final Player ai, final SpellAbility node) {
        final TargetRestrictions tr = node.getTargetRestrictions();
        final ApiType api = node.getApi();
        if (api == ApiType.PutCounter && "P1P1".equals(node.getParam("CounterType")) && !animatesTargetToCreature(node)) {
            for (final GameEntity e : tr.getAllCandidates(node)) {
                if (e instanceof Card && ((Card) e).isCreature()) {
                    return null; // a real target exists: the ordinary count decides
                }
            }
            return Verdict.IDLE_TARGETS;
        }
        if (api == ApiType.Attach && node.isEquip() && node.getHostCard().isEquipping()
                && !attachPayoff(ai, node.getHostCard())) {
            final Card current = node.getHostCard().getEquipping();
            for (final GameEntity e : tr.getAllCandidates(node)) {
                if (e != current) {
                    return null;
                }
            }
            return Verdict.IDLE_TARGETS;
        }
        return null;
    }

    /**
     * Moving an Equipment onto the creature it is already on does nothing by itself, so it is only worth doing as a step
     * of a combo the AI knows (ai-combos.txt) that is whole on its side of the table, with the Equipment or a permanent
     * that triggers when something becomes attached as one of its pieces. A "becomes attached" trigger on its own is no
     * reason: most of them pay once, and an equip {0} loop for a trickle of value is how the AI burns its turn.
     */
    private static boolean attachPayoff(final Player ai, final Card equipment) {
        final CardCollection mine = new CardCollection(ai.getCardsIn(ZoneType.Battlefield));
        for (final ComboCatalog.Entry e : ComboCatalog.entries()) {
            if (!e.assembledBy(mine)) {
                continue;
            }
            if (e.fillsSomeSlot(equipment)) {
                return true;
            }
            for (final Card c : e.fillers(mine)) {
                for (final forge.game.trigger.Trigger t : c.getTriggers()) {
                    if (t.getMode() == forge.game.trigger.TriggerType.Attached) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Verdict allEffect(final Player ai, final SpellAbility node, final ApiType api, final Card host) {
        if (!node.hasParam("ValidCards")) {
            return Verdict.USEFUL;
        }
        final String valid = node.getParam("ValidCards");
        if (readsRuntime(valid)) {
            return Verdict.USEFUL;
        }
        final ZoneType zone = node.hasParam("ValidZone") ? ZoneType.smartValueOf(node.getParam("ValidZone"))
                : ZoneType.Battlefield;
        if (zone == null) {
            return Verdict.USEFUL;
        }
        final CardCollection matches = CardLists.getValidCards(ai.getGame().getCardsIn(zone), valid, ai, host, node);
        if (matches.isEmpty()) {
            return Verdict.IDLE;
        }
        final boolean curse = node.isCurse();
        final boolean any;
        switch (api) {
            case PutCounterAll:
            case PumpAll:
            case AnimateAll:
            case ProtectionAll:
                // a boost helps its own side, a curse the opposing one
                any = matches.anyMatch(c -> curse ? c.getController().isOpponentOf(ai) : c.getController().equals(ai));
                break;
            case UntapAll:
                any = matches.anyMatch(c -> c.isTapped() && c.getController().equals(ai));
                break;
            case TapAll:
                any = matches.anyMatch(c -> !c.isTapped() && c.getController().isOpponentOf(ai));
                break;
            case DamageAll:
            case DestroyAll:
                any = matches.anyMatch(c -> c.getController().isOpponentOf(ai));
                break;
            default: // counters to remove, sacrifices, tap or untap: anything matching is something
                any = true;
                break;
        }
        return any ? Verdict.USEFUL : Verdict.IDLE;
    }

    private static Verdict changeZoneAll(final Player ai, final SpellAbility node, final Card host) {
        if (!node.hasParam("ChangeType") || !node.hasParam("Origin") || readsRuntime(node.getParam("ChangeType"))) {
            return Verdict.USEFUL;
        }
        final List<ZoneType> origin = ZoneType.listValueOf(node.getParam("Origin"));
        final CardCollection cards = new CardCollection();
        for (final ZoneType z : origin) {
            cards.addAll(ai.getGame().getCardsIn(z));
        }
        return CardLists.getValidCards(cards, node.getParam("ChangeType"), ai, host, node).isEmpty()
                ? Verdict.IDLE : Verdict.USEFUL;
    }

    /** "Sacrifice a creature": nothing to sacrifice. (The source itself, "Self", can always be.) */
    private static Verdict sacrifice(final Player ai, final SpellAbility node, final Card host) {
        if (node.hasParam("Defined") && !"You".equals(node.getParam("Defined"))) {
            return Verdict.USEFUL;
        }
        final String valid = node.getParamOrDefault("SacValid", "Self");
        if (valid.contains("Self") || readsRuntime(valid)) {
            return Verdict.USEFUL;
        }
        return CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid, ai, host, node).isEmpty()
                ? Verdict.IDLE_CHOICE : Verdict.USEFUL;
    }

    private static Verdict chooseCard(final Player ai, final SpellAbility node, final Card host) {
        if (!node.hasParam("Choices") || readsRuntime(node.getParam("Choices"))) {
            return Verdict.USEFUL;
        }
        final List<ZoneType> zones = node.hasParam("ChoiceZone") ? ZoneType.listValueOf(node.getParam("ChoiceZone"))
                : List.of(ZoneType.Battlefield);
        final CardCollection cards = new CardCollection();
        for (final ZoneType z : zones) {
            cards.addAll(ai.getGame().getCardsIn(z));
        }
        return CardLists.getValidCards(cards, node.getParam("Choices"), ai, host, node).isEmpty()
                ? Verdict.IDLE_CHOICE : Verdict.USEFUL;
    }

    /** A zone change that picks its card on resolution from a zone that is not the library. */
    private static Verdict changeZone(final Player ai, final SpellAbility node, final Card host) {
        if (!node.hasParam("Origin") || !node.hasParam("ChangeType") || readsRuntime(node.getParam("ChangeType"))) {
            return Verdict.USEFUL;
        }
        final List<ZoneType> origin = ZoneType.listValueOf(node.getParam("Origin"));
        if (origin.contains(ZoneType.Library) || origin.contains(ZoneType.Stack)) {
            return Verdict.USEFUL; // a search shuffles, and the stack is not read here
        }
        final CardCollection cards = new CardCollection();
        for (final ZoneType z : origin) {
            cards.addAll(ai.getGame().getCardsIn(z));
        }
        return CardLists.getValidCards(cards, node.getParam("ChangeType"), ai, host, node).isEmpty()
                ? Verdict.IDLE_CHOICE : Verdict.USEFUL;
    }

    /** A valid-cards string that depends on something decided while the ability resolves: not read. */
    private static boolean readsRuntime(final String valid) {
        return valid.contains("Targeted") || valid.contains("Remembered") || valid.contains("Imprinted")
                || valid.contains("Chosen") || valid.contains("ParentTarget") || valid.contains("TriggeredCard");
    }

    /** An effect that acts on the card or player an earlier effect of the chain would have targeted or chosen. */
    private static boolean dependsOnProvider(final SpellAbility node) {
        final List<String> values = new ArrayList<>(node.getMapParams().values());
        for (final String v : values) {
            if (v.contains("Targeted") || v.contains("ParentTarget") || v.contains("Chosen")
                    || v.contains("Remembered")) {
                return true;
            }
        }
        return false;
    }
}
