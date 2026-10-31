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
import forge.game.spellability.TargetChoices;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.ComparatorUtil;
import forge.util.IterableUtil;

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
        CardCollection all = new CardCollection(player.getCardsIn(ZoneType.Hand));

        all.addAll(player.getCardsIn(ZoneType.Graveyard));
        for (Player p : game.getPlayers()) {
            if (!p.getCardsIn(ZoneType.Library).isEmpty()) {
                all.add(p.getCardsIn(ZoneType.Library).get(0));
            }
        }
        all.addAll(IterableUtil.filter(player.getCardsIn(ZoneType.Command), c -> !c.isImmutable() || c.isEmblem()));
        all.addAll(game.getCardsIn(ZoneType.Exile));
        all.addAll(game.getCardsIn(ZoneType.Battlefield));
        return all;
    }

    public static List<SpellAbility> getSpellAbilities(final CardCollectionView all, final Player activator) {
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
            for (SpellAbility ab = si.getSpellAbility(); ab != null; ab = ab.getSubAbility()) {
                TargetChoices tc = ab == si.getSpellAbility() ? si.getTargetChoices() : ab.getTargets();
                if (ab.getApi() == api && tc != null) {
                    for (Card c : cardList) {
                        // TODO: somehow ensure that the detected SA won't be countered
                        if (tc.getTargetCards().contains(c)) {
                            // Was already targeted by a spell ability instance on stack
                            targeted.add(c);
                        }
                    }
                }
            }
        }

        return targeted;
    }

    public static boolean isFullyTargetable(SpellAbility sa) {
        SpellAbility sub = sa;
        while (sub != null) {
            if (sub.usesTargeting() && sub.getTargetRestrictions().getNumCandidates(sub) < sub.getMinTargets()) {
                return false;
            }
            sub = sub.getSubAbility();
        }
        return true;
    }

    public final static saComparator saEvaluator = new saComparator();

    /**
     * Everything the evaluator order depends on, computed once per SpellAbility. Keys are compared
     * lexicographically, so the order is always a valid total preorder (antisymmetric and transitive), which
     * strict sorts such as Android's TimSort require. The previous version returned early from pairwise rules
     * (e.g. "FreeSpellAI" returned -1 in both directions) and so could contradict itself.
     */
    public static final class SortKey implements Comparable<SortKey> {
        private final int activateLast, lowPriorityDie, energyPump, zeroCmcFirst, freeSpellFirst, score, notSpectacle;

        private SortKey(SpellAbility sa, boolean safeToEvaluateCreatures) {
            // we want the highest costs first
            // TODO support alternative strategies like going wide with attackers
            final int cmc = sa.getPayCosts().getTotalMana().getCMC();

            // deprioritize SAs explicitly marked as preferred to be activated last compared to all other SAs
            activateLast = sa.hasParam("AIActivateLast") ? 1 : 0;
            // deprioritize planar die roll marked with AIRollPlanarDieParams:LowPriority$ True
            lowPriorityDie = isLowPriorityPlanarDieRoll(sa) ? 1 : 0;
            // deprioritize pump spells with pure energy cost (can be activated last,
            // since energy is generally scarce, plus can benefit e.g. Electrostatic Pummeler)
            energyPump = pureEnergyPumpAmount(sa) > 0 ? 1 : 0;
            // use 0 cmc abilities first (might be a Mox)
            zeroCmcFirst = cmc == 0 && ApiType.Mana != sa.getApi() ? 0 : 1;
            final Card host = sa.getHostCard();
            freeSpellFirst = host != null && host.hasSVar("FreeSpellAI") ? 0 : 1;

            int s = cmc + saComparator.getSpellAbilityPriority(sa);
            // if both are creature spells sort them after
            if (safeToEvaluateCreatures) {
                // try to align the scales: if priority swings in either direction extra evaluation matters less
                s += Math.round(ComputerUtilCard.evaluateCreature(sa) / (10.5f + Math.abs(s)));
            }
            score = s;
            // Cheaper Spectacle costs should be preferred (only as tie-break: a pairwise rule can't be transitive)
            notSpectacle = sa.isSpectacle() ? 0 : 1;
        }

        @Override
        public int compareTo(SortKey o) {
            int r = Integer.compare(activateLast, o.activateLast);
            if (r != 0) return r;
            r = Integer.compare(lowPriorityDie, o.lowPriorityDie);
            if (r != 0) return r;
            r = Integer.compare(energyPump, o.energyPump);
            if (r != 0) return r;
            r = Integer.compare(zeroCmcFirst, o.zeroCmcFirst);
            if (r != 0) return r;
            r = Integer.compare(freeSpellFirst, o.freeSpellFirst);
            if (r != 0) return r;
            r = Integer.compare(o.score, score); // higher score first
            if (r != 0) return r;
            return Integer.compare(notSpectacle, o.notSpectacle);
        }

        private static boolean isLowPriorityPlanarDieRoll(SpellAbility sa) {
            if (ApiType.RollPlanarDice != sa.getApi() || sa.getHostCard() == null) {
                return false;
            }
            final Game game = sa.getHostCard().getGame();
            if (game.getActivePlanes() == null) {
                return false;
            }
            for (Card c : game.getActivePlanes()) {
                if (c.hasSVar("AIRollPlanarDieParams") && c.getSVar("AIRollPlanarDieParams").toLowerCase().matches(".*lowpriority\\$\\s*true.*")) {
                    return true;
                }
            }
            return false;
        }

        private static int pureEnergyPumpAmount(SpellAbility sa) {
            if (sa.getApi() == ApiType.Pump && sa.getPayCosts().getCostEnergy() != null
                    && sa.getPayCosts().hasOnlySpecificCostType(CostPayEnergy.class)) {
                final Integer amount = sa.getPayCosts().getCostEnergy().convertAmount();
                return amount == null ? 0 : amount;
            }
            return 0;
        }
    }

    public final static class saComparator implements Comparator<SpellAbility> {
        @Override
        public int compare(final SpellAbility a, final SpellAbility b) {
            return compareEvaluator(a, b, false);
        }
        public int compareEvaluator(final SpellAbility a, final SpellAbility b, boolean safeToEvaluateCreatures) {
            return new SortKey(a, safeToEvaluateCreatures).compareTo(new SortKey(b, safeToEvaluateCreatures));
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
                if (ComputerUtilCard.isCardRemAIDeck(sa.getOriginalHost() != null ? sa.getOriginalHost() : source)) {
                    p -= 10;
                }
                if (source.hasSVar("AIPriorityModifier")) {
                    p += Integer.parseInt(source.getSVar("AIPriorityModifier"));
                }
                // try to use it before it's gone
                if (source.isInPlay() && source.hasSVar("EndOfTurnLeavePlay")) {
                    p += 1;
                }
                // prefer spells from hand when it can lower risk of discarding
                if (source.isInZone(ZoneType.Hand) && !ai.isUnlimitedHandSize()) {
                    p += Math.max(0, CardLists.count(ai.getCardsIn(ZoneType.Hand), c -> !c.hasSVar("DiscardMe")) - ai.getMaxHandSize());
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
            } else if (sa.isManaAbility()) {
                // keep mana abilities for paying
                p -= 9;
            }

            // try to use mana ritual before casting spells to maximize potential mana
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

    /**
     * A comparator for a single sort run: each SpellAbility's key is computed once and reused, which is both
     * faster (creature evaluation is expensive) and guarantees every comparison sees the same values.
     */
    public static Comparator<SpellAbility> cachedEvaluator(final boolean safeToEvaluateCreatures) {
        final Map<SpellAbility, SortKey> cache = new IdentityHashMap<>();
        return (a, b) -> cache.computeIfAbsent(a, sa -> new SortKey(sa, safeToEvaluateCreatures))
                .compareTo(cache.computeIfAbsent(b, sa -> new SortKey(sa, safeToEvaluateCreatures)));
    }

    /**
     * Puts the best spells first, then orders the creature spells among themselves. Falls back to a stable sort
     * instead of throwing if the ordering is ever inconsistent.
     *
     * @return true if the fallback sort was needed (the evaluator is inconsistent and should be reported)
     */
    public static boolean sortSpellAbilities(final List<SpellAbility> all) {
        final boolean fellBack = ComparatorUtil.safeSort(all, cachedEvaluator(false));
        return sortCreatureSpellsImpl(all) || fellBack;
    }

    public static List<SpellAbility> sortCreatureSpells(final List<SpellAbility> all) {
        sortCreatureSpellsImpl(all);
        return all;
    }

    private static boolean sortCreatureSpellsImpl(final List<SpellAbility> all) {
        // try to smoothen power creep by making CMC less of a factor
        final List<SpellAbility> creatures = AiController.filterListByApi(Lists.newArrayList(all), ApiType.PermanentCreature);
        if (creatures.size() <= 1) {
            return false;
        }
        // TODO this doesn't account for nearly identical creatures where one is a newer but more cost efficient variant
        final boolean fellBack = ComparatorUtil.safeSort(creatures, cachedEvaluator(true));
        int idx = 0;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getApi() == ApiType.PermanentCreature) {
                all.set(i, creatures.get(idx));
                idx++;
            }
        }
        return fellBack;
    }
}
