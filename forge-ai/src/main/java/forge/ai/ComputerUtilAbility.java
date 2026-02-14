package forge.ai;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class ComputerUtilAbility {
    public static CardCollection getAvailableLandsToPlay(final Game game, final Player player) {
        if (!game.getStack().isEmpty() || !game.getPhaseHandler().getPhase().isMain()) {
            return null;
        }
        CardCollection landList = new CardCollection(player.getCardsIn(ZoneType.Hand));

        //filter out cards that can't be played
        landList = CardLists.filter(landList, c -> {
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
        if (landList.isEmpty()) {
            return null;
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
        all.addAll(game.getCardsIn(ZoneType.Command));
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
            if (sub.usesTargeting() && sub.getTargetRestrictions().getNumCandidates(sub, true) < sub.getMinTargets()) {
                return false;
            }
            sub = sub.getSubAbility();
        }
        return true;
    }

    public final static saComparator saEvaluator = new saComparator();

    // not sure "playing biggest spell" matters?
    public final static class saComparator implements Comparator<SpellAbility> {
        @Override
        public int compare(final SpellAbility a, final SpellAbility b) {
            return compareEvaluator(a, b, false);
        }
        public int compareEvaluator(final SpellAbility a, final SpellAbility b, boolean safeToEvaluateCreatures) {
            // sort from highest cost to lowest
            // we want the highest costs first
            int a1 = a.getPayCosts().getTotalMana().getCMC();
            int b1 = b.getPayCosts().getTotalMana().getCMC();

            // deprioritize SAs explicitly marked as preferred to be activated last compared to all other SAs
            if (a.hasParam("AIActivateLast") && !b.hasParam("AIActivateLast")) {
                return 1;
            } else if (b.hasParam("AIActivateLast") && !a.hasParam("AIActivateLast")) {
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
                Game game = hostCardForGame.getGame();
                if (game.getActivePlanes() != null) {
                    for (Card c : game.getActivePlanes()) {
                        if (c.hasSVar("AIRollPlanarDieParams") && c.getSVar("AIRollPlanarDieParams").toLowerCase().matches(".*lowpriority\\$\\s*true.*")) {
                            if (ApiType.RollPlanarDice == a.getApi()) {
                                return 1;
                            } else {
                                return -1;
                            }
                        }
                    }
                }
            }

            // deprioritize pump spells with pure energy cost (can be activated last,
            // since energy is generally scarce, plus can benefit e.g. Electrostatic Pummeler)
            int a2 = 0, b2 = 0;
            if (a.getApi() == ApiType.Pump && a.getPayCosts().getCostEnergy() != null) {
                if (a.getPayCosts().hasOnlySpecificCostType(CostPayEnergy.class)) {
                    a2 = a.getPayCosts().getCostEnergy().convertAmount();
                }
            }
            if (b.getApi() == ApiType.Pump && b.getPayCosts().getCostEnergy() != null) {
                if (b.getPayCosts().hasOnlySpecificCostType(CostPayEnergy.class)) {
                    b2 = b.getPayCosts().getCostEnergy().convertAmount();
                }
            }
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

            if (a.getHostCard() != null && a.getHostCard().hasSVar("FreeSpellAI")) {
                return -1;
            } else if (b.getHostCard() != null && b.getHostCard().hasSVar("FreeSpellAI")) {
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

            a1 += getSpellAbilityPriority(a);
            b1 += getSpellAbilityPriority(b);

            // if both are creature spells sort them after
            if (safeToEvaluateCreatures) {
                // try to align the scales: if priority swings in either direction extra evaluation matters less
                a1 += Math.round(ComputerUtilCard.evaluateCreature(a) / (10.5f + Math.abs(a1)));
                b1 += Math.round(ComputerUtilCard.evaluateCreature(b) / (10.5f + Math.abs(b1)));
            }

            return b1 - a1;
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
                // avoid cards AI is bad at
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

            return p;
        }
    }

    public static List<SpellAbility> sortCreatureSpells(final List<SpellAbility> all) {
        // try to smoothen power creep by making CMC less of a factor
        final List<SpellAbility> creatures = AiController.filterListByApi(Lists.newArrayList(all), ApiType.PermanentCreature);
        if (creatures.size() <= 1) {
            return all;
        }
        // TODO this doesn't account for nearly identical creatures where one is a newer but more cost efficient variant
        creatures.sort(ComputerUtilCard.EvaluateCreatureSpellComparator);
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
