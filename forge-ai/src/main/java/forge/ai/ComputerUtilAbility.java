package forge.ai;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;

import com.google.common.collect.Multimap;
import forge.card.CardStateName;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameTraceDescriptors;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.cost.CostPart;
import forge.game.cost.CostPayEnergy;
import forge.game.cost.CostPutCounter;
import forge.game.cost.CostRemoveCounter;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityStackInstance;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.perf.PerfCounter;
import forge.util.perf.PerfProbe;
import forge.util.perf.PerfTimer;
import forge.util.perf.TraceCategory;

public class ComputerUtilAbility {
    public static CardCollection getAvailableLandsToPlay(final Game game, final Player player) {
        if (!game.getStack().isEmpty() || !game.getPhaseHandler().getPhase().isMain()) {
            return null;
        }

        //filter out cards that can't be played
        CardCollection landList = CardLists.filter(player.getCardsIn(ZoneType.Hand), c -> {
            if (!c.hasPlayableLandFace()) {
                return false;
            }
            return player.canPlayLand(c, false, c.getFirstSpellAbility());
        });

        final CardCollection landsNotInHand = new CardCollection(player.getCardsIn(ZoneType.Graveyard));
        landsNotInHand.addAll(game.getCardsIn(ZoneType.Exile));
        if (!player.getCardsIn(ZoneType.Library).isEmpty()) {
            landsNotInHand.add(player.getCardsIn(ZoneType.Library).get(0));
        }
        for (final Card crd : landsNotInHand) {
            if (!(crd.hasPlayableLandFace() || (crd.isFaceDown() && crd.getState(CardStateName.Original).getType().isLand()))) {
                continue;
            }
            if (!crd.mayPlay(player).isEmpty()) {
                landList.add(crd);
            }
        }
        return landList;
    }

    public static CardCollection getAvailableCards(final Game game, final Player player) {
        final long token = PerfProbe.start(PerfTimer.CANDIDATE_GENERATION);
        try {
            final CardCollection available = collectAvailableCards(game, player);
            PerfProbe.count(PerfCounter.CANDIDATE_CARDS, available.size());
            return available;
        } finally {
            PerfProbe.stop(PerfTimer.CANDIDATE_GENERATION, token);
        }
    }

    private static CardCollection collectAvailableCards(final Game game, final Player player) {
        CardCollection all = new CardCollection(player.getCardsIn(ZoneType.Hand));

        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        for (Player p : game.getPlayers()) {
            if (!p.getCardsIn(ZoneType.Library).isEmpty()) {
                all.add(p.getCardsIn(ZoneType.Library).get(0));
            }
        }
        all.addAll(game.getCardsIn(ZoneType.Command));
        all.addAll(game.getCardsIn(ZoneType.Exile));
        all.addAll(game.getCardsIn(ZoneType.Battlefield));
        return all;
    }

    public static List<SpellAbility> getSpellAbilities(final CardCollectionView all, final Player activator) {
        final long token = PerfProbe.start(PerfTimer.CANDIDATE_GENERATION);
        try {
            final List<SpellAbility> abilities = collectSpellAbilities(all, activator);
            PerfProbe.count(PerfCounter.CANDIDATE_ABILITIES, abilities.size());
            return abilities;
        } finally {
            PerfProbe.stop(PerfTimer.CANDIDATE_GENERATION, token);
        }
    }

    private static List<SpellAbility> collectSpellAbilities(final CardCollectionView all, final Player activator) {
        final List<SpellAbility> spellAbilities = Lists.newArrayList();
        for (final Card c : all) {
            Multimap<SpellAbility, SpellAbility> unhiddenAltCost = ArrayListMultimap.create();
            List<SpellAbility> possible = c.getAllPossibleAbilities(activator, false, unhiddenAltCost);
            for (SpellAbility sa : unhiddenAltCost.keySet()) {
                if (possible.contains(sa)) {
                    // when SA can also be played as basic exclude its AltCost to prevent redundant check later
                    possible.removeAll(unhiddenAltCost.get(sa));
                }
            }
            spellAbilities.addAll(possible);
        }
        return spellAbilities;
    }

    public static List<SpellAbility> getOriginalAndAltCostAbilities(final List<SpellAbility> originList, final Player activator) {
        final long token = PerfProbe.start(PerfTimer.CANDIDATE_GENERATION);
        try {
            final List<SpellAbility> expanded = collectOriginalAndAltCostAbilities(originList, activator);
            if (PerfProbe.isEnabled()) {
                PerfProbe.count(PerfCounter.CANDIDATE_ABILITIES_WITH_ALT_COSTS, expanded.size());
                if (PerfProbe.isTracing()) {
                    for (final SpellAbility variant : expanded) {
                        PerfProbe.trace(TraceCategory.ALT_COST, GameTraceDescriptors.describe(variant));
                    }
                }
            }
            return expanded;
        } finally {
            PerfProbe.stop(PerfTimer.CANDIDATE_GENERATION, token);
        }
    }

    private static List<SpellAbility> collectOriginalAndAltCostAbilities(final List<SpellAbility> originList, final Player activator) {
        List<SpellAbility> originListWithAddCosts = Lists.newArrayList();
        for (SpellAbility sa : originList) {
            // If this spell has alternative additional costs, add them instead of the unmodified SA itself
            sa.setActivatingPlayer(activator);
            originListWithAddCosts.addAll(GameActionUtil.getAdditionalCostSpell(sa));
        }

        final List<SpellAbility> newAbilities = Lists.newArrayList();
        for (SpellAbility sa : originListWithAddCosts) {
            // determine which alternative costs are cheaper than the original and prioritize them
            List<SpellAbility> saAltCosts = GameActionUtil.getAlternativeCosts(sa, activator, false);
            List<SpellAbility> priorityAltSa = Lists.newArrayList();
            List<SpellAbility> otherAltSa = Lists.newArrayList();
            for (SpellAbility altSa : saAltCosts) {
                if (sa.getPayCosts().isOnlyManaCost()
                        && altSa.getPayCosts().isOnlyManaCost() && sa.getPayCosts().getTotalMana().compareTo(altSa.getPayCosts().getTotalMana()) == 1) {
                    // the alternative cost is strictly cheaper, so why not? (e.g. Omniscience etc.)
                    priorityAltSa.add(altSa);
                } else {
                    otherAltSa.add(altSa);
                }
            }

            // add alternative costs as additional spell abilities
            newAbilities.addAll(priorityAltSa);
            newAbilities.add(sa);
            newAbilities.addAll(otherAltSa);
        }

        final List<SpellAbility> result = Lists.newArrayList();
        for (SpellAbility sa : newAbilities) {
            sa.setActivatingPlayer(activator);

            // Optional cost selection through the AI controller
            boolean choseOptCost = false;
            List<OptionalCostValue> list = GameActionUtil.getOptionalCostValues(sa);
            if (!list.isEmpty()) {
                list = activator.getController().chooseOptionalCosts(sa, list);
                if (!list.isEmpty()) {
                    // still check base spell first in case of Promise Gift
                    if (list.stream().anyMatch(ocv -> ocv.getType().equals(OptionalCost.PromiseGift))) {
                        result.add(sa);
                    }
                    result.add(GameActionUtil.addOptionalCosts(sa, list));
                    choseOptCost = true;
                }
            }

            // Add only one ability: either the one with preferred optional costs, or the original one if there are none
            if (!choseOptCost) {
                result.add(sa);
            }
        }

        return result;
    }

    public static SpellAbility getTopSpellAbilityOnStack(Game game, SpellAbility sa) {
        Iterator<SpellAbilityStackInstance> it = game.getStack().iterator();

        if (!it.hasNext()) {
            return null;
        }

        SpellAbility tgtSA = it.next().getSpellAbility();
        // Grab the topmost spellability that isn't this SA and use that for comparisons
        if (sa.equals(tgtSA) && game.getStack().size() > 1) {
            if (!it.hasNext()) {
                return null;
            }
            tgtSA = it.next().getSpellAbility();
        }
        return tgtSA;
    }

    public static SpellAbility getFirstCopySASpell(List<SpellAbility> spells) {
        SpellAbility sa = null;
        for (SpellAbility spell : spells) {
            if (spell.getApi() == ApiType.CopySpellAbility) {
                sa = spell;
                break;
            }
        }
        return sa;
    }

    public static Card getAbilitySource(SpellAbility sa) {
        return sa.getOriginalHost() != null ? sa.getOriginalHost() : sa.getHostCard();
    }

    public static String getAbilitySourceName(SpellAbility sa) {
        final Card c = getAbilitySource(sa);
        return c != null ? c.getName() : "";
    }

    public static CardCollection getCardsTargetedWithApi(Player ai, CardCollection cardList, SpellAbility sa, ApiType api) {
        // Returns a collection of cards which have already been targeted with the given API either in the parent ability,
        // in the sub ability, or by something on stack. If "sa" is specified, the parent and sub abilities of this SA will
        // be checked for targets. If "sa" is null, only the stack instances will be checked.
        CardCollection targeted = new CardCollection();
        if (sa != null) {
            SpellAbility saSub = sa.getRootAbility();
            while (saSub != null) {
                if (saSub.getApi() == api && saSub.getTargets() != null) {
                    for (Card c : cardList) {
                        if (saSub.getTargets().getTargetCards().contains(c)) {
                            // Was already targeted with this API in a parent or sub SA
                            targeted.add(c);
                        }
                    }
                }
                saSub = saSub.getSubAbility();
            }
        }
        for (SpellAbilityStackInstance si : ai.getGame().getStack()) {
            SpellAbility ab = si.getSpellAbility();
            if (ab != null && ab.getApi() == api && si.getTargetChoices() != null) {
                for (Card c : cardList) {
                    // TODO: somehow ensure that the detected SA won't be countered
                    if (si.getTargetChoices().getTargetCards().contains(c)) {
                        // Was already targeted by a spell ability instance on stack
                        targeted.add(c);
                    }
                }
            }
        }

        return targeted;
    }

    public static boolean isFullyTargetable(SpellAbility sa) {
        SpellAbility sub = sa;
        while (sub != null) {
            if (sub.usesTargeting() && !sub.getTargetRestrictions().hasAtLeastCandidates(sub, sub.getMinTargets())) {
                return false;
            }
            sub = sub.getSubAbility();
        }
        return true;
    }

    public final static saComparator saEvaluator = new saComparator();

    /**
     * Comparator facts that are stable for the length of one candidate ordering.
     *
     * <p>{@link saComparator} derives the same handful of values from an ability on every one of the
     * {@code O(n log n)} comparisons it takes part in, and some of them — the priority value above
     * all, which walks the host card's triggers and static abilities — are not cheap. Nothing in a
     * sort mutates the game, so those values cannot change while the sort runs; this keeps them for
     * exactly as long as that holds and is then discarded.</p>
     *
     * <p>It is a cache, not a policy: every value is produced by the code the comparator would have
     * run inline, so an ordering made with facts must be identical to one made without them. Create
     * one per decision, never share one across decisions, and never hold one past the sort.</p>
     */
    public static final class SortFacts {
        private final Map<SpellAbility, SaFacts> perAbility = new IdentityHashMap<>();
        private final Map<Game, Boolean> planarDieDeprioritized = new IdentityHashMap<>();

        private SaFacts get(final SpellAbility sa) {
            SaFacts f = perAbility.get(sa);
            if (f == null) {
                PerfProbe.count(PerfCounter.SORT_FACTS_COMPUTED);
                f = new SaFacts();
                perAbility.put(sa, f);
            } else {
                PerfProbe.count(PerfCounter.SORT_FACT_HITS);
            }
            return f;
        }

        boolean isPlanarDieDeprioritized(final Game game) {
            Boolean known = planarDieDeprioritized.get(game);
            if (known == null) {
                known = saComparator.computePlanarDieDeprioritized(game);
                planarDieDeprioritized.put(game, known);
            }
            return known;
        }
    }

    /** The lazily derived facts about one ability. All of them are read-only queries. */
    private static final class SaFacts {
        private int cmc = -1;
        private int energy = -1;
        private Boolean activateLast;
        private Boolean freeSpellHost;
        private Integer priority;
        private Integer creatureScore;
    }

    // not sure "playing biggest spell" matters?
    public final static class saComparator implements Comparator<SpellAbility> {
        /** Optional per-sort fact cache; {@code null} means every value is derived on demand. */
        private final SortFacts facts;

        public saComparator() {
            this(null);
        }
        public saComparator(final SortFacts facts) {
            this.facts = facts;
        }

        @Override
        public int compare(final SpellAbility a, final SpellAbility b) {
            return compareEvaluator(a, b, false);
        }
        public int compareEvaluator(final SpellAbility a, final SpellAbility b, boolean safeToEvaluateCreatures) {
            // sort from highest cost to lowest
            // we want the highest costs first
            int a1 = cmc(a);
            int b1 = cmc(b);

            // deprioritize SAs explicitly marked as preferred to be activated last compared to all other SAs
            if (activateLast(a) && !activateLast(b)) {
                return 1;
            } else if (activateLast(b) && !activateLast(a)) {
                return -1;
            }

            // deprioritize planar die roll marked with AIRollPlanarDieParams:LowPriority$ True
            if (ApiType.RollPlanarDice == a.getApi() || ApiType.RollPlanarDice == b.getApi()) {
                Card hostCardForGame = a.getHostCard();
                if (hostCardForGame == null) {
                    if (b.getHostCard() != null) {
                        hostCardForGame = b.getHostCard();
                    } else {
                        return 0; // fallback if neither SA have a host card somehow
                    }
                }
                if (planarDieDeprioritized(hostCardForGame.getGame())) {
                    if (ApiType.RollPlanarDice == a.getApi()) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            }

            // deprioritize pump spells with pure energy cost (can be activated last,
            // since energy is generally scarce, plus can benefit e.g. Electrostatic Pummeler)
            int a2 = energy(a), b2 = energy(b);
            if (a2 == 0 && b2 > 0) {
                return -1;
            } else if (b2 == 0 && a2 > 0) {
                return 1;
            }

            // cast 0 mana cost spells first (might be a Mox)
            if (a1 == 0 && b1 > 0 && ApiType.Mana != a.getApi()) {
                return -1;
            } else if (a1 > 0 && b1 == 0 && ApiType.Mana != b.getApi()) {
                return 1;
            }

            if (freeSpellHost(a)) {
                return -1;
            } else if (freeSpellHost(b)) {
                return 1;
            }

            if (a.getHostCard().equals(b.getHostCard()) && a.getApi() == b.getApi()) {
                // Cheaper Spectacle costs should be preferred
                // FIXME: Any better way to identify that these are the same ability, one with Spectacle and one not?
                // (looks like it's not a full-fledged alternative cost as such, and is not processed with other alt costs)
                if (a.isSpectacle() && !b.isSpectacle() && a1 < b1) {
                    return 1;
                } else if (b.isSpectacle() && !a.isSpectacle() && b1 < a1) {
                    return 1;
                }
            }

            a1 += priority(a);
            b1 += priority(b);

            // if both are creature spells sort them after
            if (safeToEvaluateCreatures) {
                // try to align the scales: if priority swings in either direction extra evaluation matters less
                a1 += Math.round(creatureScore(a) / (10.5f + Math.abs(a1)));
                b1 += Math.round(creatureScore(b) / (10.5f + Math.abs(b1)));
            }

            return b1 - a1;
        }

        private int cmc(final SpellAbility sa) {
            if (facts == null) {
                return sa.getPayCosts().getTotalMana().getCMC();
            }
            final SaFacts f = facts.get(sa);
            if (f.cmc < 0) {
                f.cmc = sa.getPayCosts().getTotalMana().getCMC();
            }
            return f.cmc;
        }

        private boolean activateLast(final SpellAbility sa) {
            if (facts == null) {
                return sa.hasParam("AIActivateLast");
            }
            final SaFacts f = facts.get(sa);
            if (f.activateLast == null) {
                f.activateLast = sa.hasParam("AIActivateLast");
            }
            return f.activateLast;
        }

        private int energy(final SpellAbility sa) {
            if (facts == null) {
                return computeEnergy(sa);
            }
            final SaFacts f = facts.get(sa);
            if (f.energy < 0) {
                f.energy = computeEnergy(sa);
            }
            return f.energy;
        }

        private static int computeEnergy(final SpellAbility sa) {
            if (sa.getApi() == ApiType.Pump && sa.getPayCosts().getCostEnergy() != null
                    && sa.getPayCosts().hasOnlySpecificCostType(CostPayEnergy.class)) {
                return sa.getPayCosts().getCostEnergy().convertAmount();
            }
            return 0;
        }

        private boolean freeSpellHost(final SpellAbility sa) {
            if (facts == null) {
                return sa.getHostCard() != null && sa.getHostCard().hasSVar("FreeSpellAI");
            }
            final SaFacts f = facts.get(sa);
            if (f.freeSpellHost == null) {
                f.freeSpellHost = sa.getHostCard() != null && sa.getHostCard().hasSVar("FreeSpellAI");
            }
            return f.freeSpellHost;
        }

        private int priority(final SpellAbility sa) {
            if (facts == null) {
                return getSpellAbilityPriority(sa);
            }
            final SaFacts f = facts.get(sa);
            if (f.priority == null) {
                f.priority = getSpellAbilityPriority(sa);
            }
            return f.priority;
        }

        private int creatureScore(final SpellAbility sa) {
            if (facts == null) {
                return ComputerUtilCard.evaluateCreature(sa);
            }
            final SaFacts f = facts.get(sa);
            if (f.creatureScore == null) {
                f.creatureScore = ComputerUtilCard.evaluateCreature(sa);
            }
            return f.creatureScore;
        }

        private boolean planarDieDeprioritized(final Game game) {
            return facts == null ? computePlanarDieDeprioritized(game) : facts.isPlanarDieDeprioritized(game);
        }

        private static boolean computePlanarDieDeprioritized(final Game game) {
            if (game.getActivePlanes() != null) {
                for (Card c : game.getActivePlanes()) {
                    if (c.hasSVar("AIRollPlanarDieParams") && c.getSVar("AIRollPlanarDieParams").toLowerCase().matches(".*lowpriority\\$\\s*true.*")) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static int getSpellAbilityPriority(SpellAbility sa) {
            int p = 0;
            Card source = sa.getHostCard();
            final Player ai = source == null ? sa.getActivatingPlayer() : source.getController();
            if (ai == null) {
                System.err.println("Error: couldn't figure out the activating player and host card for SA: " + sa);
                return 0;
            }
            final boolean noCreatures = ai.getCreaturesInPlay().isEmpty();

            if (source != null) {
                // puts creatures in front of spells
                if (source.isCreature()) {
                    p += 1;
                }
                if (source.hasSVar("AIPriorityModifier")) {
                    p += Integer.parseInt(source.getSVar("AIPriorityModifier"));
                }
                // try to use it before it's gone
                if (source.isInPlay() && source.hasSVar("EndOfTurnLeavePlay")) {
                    p += 1;
                }
                if (ComputerUtilCard.isCardRemAIDeck(sa.getOriginalHost() != null ? sa.getOriginalHost() : source)) {
                    p -= 10;
                }
                // don't play equipment before having any creatures
                if (source.isEquipment() && noCreatures) {
                    p -= 9;
                }
                // don't equip stuff in main 2 if there's more stuff to cast at the moment
                if (sa.getApi() == ApiType.Attach && !sa.isCurse() && source.getGame().getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    p -= 1;
                }
                // 1. increase chance of using Surge effects
                // 2. non-surged versions are usually inefficient
                if (source.hasKeyword(Keyword.SURGE) && !sa.isSurged()) {
                    p -= 9;
                }
                // move snap-casted spells to front
                if (source.isInZone(ZoneType.Graveyard) && source.mayPlay(sa.getMayPlay()) != null) {
                    p += 50;
                }
                // if the profile specifies it, deprioritize Storm spells in an attempt to build up storm count
                if (source.hasKeyword(Keyword.STORM) && ai.getController() instanceof PlayerControllerAi) {
                    p -= (((PlayerControllerAi) ai.getController()).getAi().getIntProperty(AiProps.PRIORITY_REDUCTION_FOR_STORM_SPELLS));
                }

                for (Trigger trig : source.getTriggers()) {
                    if (!"Battlefield".equals(trig.getParam("TriggerZones"))) {
                        continue;
                    }
                    final TriggerType mode = trig.getMode();
                    // benefit from Magecraft abilities
                    if ((mode == TriggerType.SpellCast || mode == TriggerType.SpellCastOrCopy) && "You".equals(sa.getParam("ValidActivatingPlayer"))) {
                        p += 1;
                    }
                }

                for (StaticAbility sta : source.getStaticAbilities()) {
                    final Set<StaticAbilityMode> mode = sta.getMode();
                    // reduce cost to enable more plays
                    if (mode.contains(StaticAbilityMode.ReduceCost) && "You".equals(sta.getParam("Activator"))) {
                        p += 1;
                    }
                }
            }

            // use Surge and Prowl costs when able to
            if (sa.isSurged() || sa.isProwl()) {
                p += 9;
            }
            // sort planeswalker abilities with most costly first
            if (sa.isPwAbility()) {
                final CostPart cost = sa.getPayCosts().getCostParts().get(0);
                if (cost instanceof CostRemoveCounter) {
                    p += cost.convertAmount() == null ? 1 : cost.convertAmount();
                } else if (cost instanceof CostPutCounter) {
                    p -= cost.convertAmount();
                }
                if (sa.hasParam("Ultimate")) {
                    p += 9;
                }
            }

            if (ApiType.DestroyAll == sa.getApi()) {
                // check boardwipe earlier
                p += 4;
            } else if (ApiType.Mana == sa.getApi()) {
                // keep mana abilities for paying
                p -= 9;
            }

            // try to cast mana ritual spells before casting spells to maximize potential mana
            if ("ManaRitual".equals(sa.getParam("AILogic"))) {
                p += 9;
            }

            if ((sa.isPlotting() || sa.isForetelling() || sa.isKeyword(Keyword.SUSPEND)) && ai.getTurn() > 10) {
                // less time in late game, prefer something that affects board right away
                p -= 1;
            }

            return p;
        }
    }

    public static List<SpellAbility> sortCreatureSpells(final List<SpellAbility> all) {
        return sortCreatureSpells(all, null);
    }
    /**
     * @param facts optional per-decision comparator facts, shared with the preceding
     *     {@link #saEvaluator} pass so that a creature's evaluation is paid for once
     */
    public static List<SpellAbility> sortCreatureSpells(final List<SpellAbility> all, final SortFacts facts) {
        // try to smoothen power creep by making CMC less of a factor
        final List<SpellAbility> creatures = AiController.filterListByApi(Lists.newArrayList(all), ApiType.PermanentCreature);
        if (creatures.size() <= 1) {
            return all;
        }
        // TODO this doesn't account for nearly identical creatures where one is a newer but more cost efficient variant
        creatures.sort(facts == null ? ComputerUtilCard.EvaluateCreatureSpellComparator
                : ComputerUtilCard.evaluateCreatureSpellComparator(facts));
        int idx = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getApi() == ApiType.PermanentCreature) {
                all.set(i, creatures.get(idx));
                idx++;
            }
        }
        return all;
    }
}
