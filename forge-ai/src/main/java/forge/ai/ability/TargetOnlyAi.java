package forge.ai.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.PlaceholderTargetAi;
import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

/**
 * The choice of a target for the effects that follow ({@code DB$ TargetOnly}): "choose target creature", then a
 * sub-ability does something to it (fight it, destroy it, exchange it, copy it ...). Nothing here pumps anything, and
 * nothing is inherited from {@link PumpAi}: the choice is made from what the rest of the ability does.
 * <p>
 * AILogic values: {@code Fight} / {@code PowerDmg} (a creature of the AI's that wins against one of theirs),
 * {@code Destroy} (their best permanent, else the AI's worst), {@code HighestPower}, {@code SameName},
 * {@code SacOneEach}, {@code PowerStruggle}, {@code Donate...} (an opponent to give a permanent to), {@code AnyPhase}
 * (played outside the usual phases too), {@code Never}, and the logics of {@link PlaceholderTargetAi}. Without a
 * logic, the target is an opponent's best one when the ability or what follows it hurts (a curse, removal, damage,
 * taking control), otherwise the AI's own best one.
 */
public class TargetOnlyAi extends SpellAbilityAi {

    private static String logic(final SpellAbility sa) {
        return sa.getParamOrDefault("AILogic", "");
    }

    private static boolean isFight(final String logic) {
        return "Fight".equals(logic) || "PowerDmg".equals(logic);
    }

    // ------------------------------------------------------------------------------------------ entry points

    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        final Game game = ai.getGame();
        final String logic = logic(sa);
        if (!game.getStack().isEmpty() || "AnyPhase".equals(logic) || isFight(logic)) {
            return true;
        }
        if (ph.getPhase().isBefore(PhaseType.COMBAT_BEGIN) || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            // a choice made for a trick waits for the last moment; one that removes or hurts can be made at once
            return sa.isCurse() || isSorcerySpeed(sa, ai)
                    || "Main1IfAble".equals(logic) && ph.is(PhaseType.MAIN1, ai);
        }
        return true;
    }

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        return choose(ai, sa, false, false);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        return choose(ai, sa, mandatory, true);
    }

    @Override
    public AiAbilityDecision chkDrawback(final Player ai, final SpellAbility sa) {
        return choose(ai, sa, false, true);
    }

    @Override
    public boolean confirmAction(final Player player, final SpellAbility sa, final PlayerActionConfirmMode mode,
            final String message, final Map<String, Object> params) {
        return true;
    }

    // ------------------------------------------------------------------------------------------ the choice

    private AiAbilityDecision choose(final Player ai, final SpellAbility sa, final boolean mandatory,
            final boolean immediately) {
        final String logic = logic(sa);
        if (PlaceholderTargetAi.handles(logic)) {
            return PlaceholderTargetAi.decide(ai, sa, mandatory);
        }
        if ("Never".equals(logic) && !mandatory) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        if (!sa.usesTargeting()) {
            return mandatory ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                    : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        final Game game = ai.getGame();
        if (!mandatory && !immediately && !ComputerUtil.playImmediately(ai, sa)
                && game.getPhaseHandler().getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                && !"AnyPhase".equals(logic) && !isFight(logic)) {
            return new AiAbilityDecision(0, AiPlayDecision.MissingPhaseRestrictions);
        }

        sa.resetTargets();
        if (sa.hasParam("TargetingPlayer") && sa.getActivatingPlayer().equals(ai) && !sa.isTrigger()) {
            // the player named chooses the target when the ability resolves
            final Player chooser = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("TargetingPlayer"), sa).get(0);
            sa.setTargetingPlayer(chooser);
            return CardLists.getTargetableCards(game.getCardsIn(sa.getTargetRestrictions().getZone()), sa).isEmpty()
                    ? new AiAbilityDecision(0, AiPlayDecision.TargetingFailed)
                    : new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }

        final AiAbilityDecision decision;
        if (logic.startsWith("Donate")) {
            // step one: an opponent to give the permanent to (the gain-control effect after it picks the permanent)
            decision = SpecialCardAi.Donate.considerTargetingOpponent(ai, sa);
        } else if (isFight(logic)) {
            decision = FightAi.canFight(ai, sa, 0, 0);
        } else if ("PowerStruggle".equals(logic)) {
            decision = SpecialCardAi.PowerStruggle.considerFirstTarget(ai, sa) ? yes() : no();
        } else if ("SacOneEach".equals(logic)) {
            // each player sacrifices one permanent: the worst for allies, the best for opponents
            decision = SacrificeAi.doSacOneEachLogic(ai, sa);
        } else if ("SameName".equals(logic)) {
            decision = sameName(ai, sa) ? yes() : no();
        } else if ("Destroy".equals(logic)) {
            decision = destroy(ai, sa) ? yes() : no();
        } else if ("HighestPower".equals(logic)) {
            decision = highestPower(ai, sa) ? yes() : no();
        } else {
            decision = generic(ai, sa, mandatory);
        }
        if (!decision.willingToPlay() && mandatory) {
            sa.resetTargets();
            return generic(ai, sa, true);
        }
        return decision;
    }

    private static AiAbilityDecision yes() {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    private static AiAbilityDecision no() {
        return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
    }

    // ------------------------------------------------------------------------------------------ general choice

    /** What follows the choice hurts the target (or takes it): the AI aims it at an opponent's. */
    static boolean aimedAtOpponents(final SpellAbility sa) {
        if (sa.isCurse()) {
            return true;
        }
        for (SpellAbility s = sa.getSubAbility(); s != null; s = s.getSubAbility()) {
            final ApiType api = s.getApi();
            if (api == ApiType.Destroy || api == ApiType.DestroyAll || api == ApiType.DealDamage
                    || api == ApiType.Sacrifice || api == ApiType.Fight || api == ApiType.Tap || api == ApiType.Counter
                    || api == ApiType.GainControl || api == ApiType.Debuff || api == ApiType.EachDamage) {
                return true;
            }
            if (api == ApiType.ChangeZone || api == ApiType.ChangeZoneAll) {
                if ("Battlefield".equals(s.getParam("Origin")) && !s.hasParam("GainControl")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean ours(final Player ai, final GameEntity e) {
        final Player p = e instanceof Player ? (Player) e : ((Card) e).getController();
        return p == ai || !ai.isOpponentOf(p);
    }

    private static GameEntity best(final Player ai, final List<GameEntity> list) {
        final CardCollection cards = new CardCollection();
        Player lowest = null;
        for (final GameEntity e : list) {
            if (e instanceof Card) {
                cards.add((Card) e);
            } else if (lowest == null || ((Player) e).getLife() < lowest.getLife()) {
                lowest = (Player) e;
            }
        }
        if (!cards.isEmpty()) {
            return ComputerUtilCard.getBestAI(cards);
        }
        return lowest;
    }

    private static GameEntity worst(final List<GameEntity> list) {
        final CardCollection cards = new CardCollection();
        for (final GameEntity e : list) {
            if (e instanceof Card) {
                cards.add((Card) e);
            }
        }
        return cards.isEmpty() ? list.get(0) : ComputerUtilCard.getWorstAI(cards);
    }

    private AiAbilityDecision generic(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final TargetRestrictions tr = sa.getTargetRestrictions();
        final boolean hostile = aimedAtOpponents(sa);
        final List<GameEntity> preferred = new ArrayList<>();
        final List<GameEntity> forced = new ArrayList<>();
        for (final GameEntity e : tr.getAllCandidates(sa)) {
            if (!sa.canTarget(e)) {
                continue;
            }
            if (e == sa.getHostCard() && sa.getPayCosts() != null && sa.getPayCosts().hasTapCost() && !hostile) {
                continue; // it is tapped to pay for this
            }
            (ours(ai, e) == hostile ? forced : preferred).add(e);
        }
        if (!hostile) {
            // do not aim a help at a card that is about to die anyway
            final CardCollection cards = new CardCollection();
            for (final GameEntity e : preferred) {
                if (e instanceof Card) {
                    cards.add((Card) e);
                }
            }
            final CardCollection safe = ComputerUtil.getSafeTargets(ai, sa, cards);
            preferred.removeIf(e -> e instanceof Card && !safe.contains(e));
        }
        final CardCollection filtered = new CardCollection();
        for (final GameEntity e : preferred) {
            if (e instanceof Card) {
                filtered.add((Card) e);
            }
        }
        final CardCollection allowed = ComputerUtil.filterAITgts(sa, ai, filtered, true);
        preferred.removeIf(e -> e instanceof Card && !allowed.contains(e));

        while (sa.canAddMoreTarget() && !preferred.isEmpty()) {
            final GameEntity pick = best(ai, preferred);
            preferred.remove(pick);
            if (pick == null || !sa.canTarget(pick)) {
                continue;
            }
            sa.getTargets().add(pick);
        }
        boolean forcedPick = false;
        while (mandatory && !sa.isMinTargetChosen() && !forced.isEmpty()) {
            final GameEntity pick = worst(forced);
            forced.remove(pick);
            if (sa.canTarget(pick)) {
                sa.getTargets().add(pick);
                forcedPick = true;
            }
        }
        if (!sa.isMinTargetChosen() || sa.getTargets().size() == 0 && !sa.isZeroTargets()) {
            sa.resetTargets();
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
        return new AiAbilityDecision(forcedPick ? 50 : 100,
                forcedPick ? AiPlayDecision.MandatoryPlay : AiPlayDecision.WillPlay);
    }

    // ------------------------------------------------------------------------------------------ special logics

    /** Their best permanent, else (nothing of theirs) the AI's worst. */
    private boolean destroy(final Player ai, final SpellAbility sa) {
        final CardCollection tgts = CardLists.getTargetableCards(ai.getGame().getCardsIn(ZoneType.Battlefield), sa);
        if (tgts.isEmpty()) {
            return false;
        }
        final CardCollection opp = CardLists.filter(tgts, CardPredicates.isControlledByAnyOf(ai.getOpponents()));
        final CardCollection own = CardLists.filter(tgts,
                CardPredicates.isControlledByAnyOf(ai.getAllies()).or(CardPredicates.isController(ai)));
        final Card pick = !opp.isEmpty() ? ComputerUtilCard.getBestAI(opp) : ComputerUtilCard.getWorstAI(own);
        if (pick == null) {
            return false;
        }
        sa.getTargets().add(pick);
        return true;
    }

    /** The creature with the highest power that can be targeted, never the one that pays for this by being sacrificed. */
    private boolean highestPower(final Player ai, final SpellAbility sa) {
        final Card source = sa.getHostCard();
        CardCollection list = CardLists.getValidCards(
                CardLists.filter(ai.getGame().getCardsIn(ZoneType.Battlefield), CardPredicates.CREATURES),
                sa.getTargetRestrictions().getValidTgts(), ai, source, sa);
        list = CardLists.getTargetableCards(list, sa);
        CardLists.sortByPowerDesc(list);
        if (list.contains(source) && sa.getPayCosts() != null
                && (sa.getPayCosts().hasTapCost() || forge.ai.ComputerUtilCost.isSacrificeSelfCost(sa.getPayCosts()))) {
            list.remove(source);
        }
        if (list.isEmpty()) {
            return false;
        }
        sa.getTargets().add(list.get(0));
        return true;
    }

    /** The name worth most among an opponent's creatures: the target, and all with its name are exiled by the sub-ability. */
    private boolean sameName(final Player ai, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Card source = sa.getHostCard();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final ZoneType origin = ZoneType.listValueOf(sa.getSubAbility().getParam("Origin")).get(0);
        CardCollection list = CardLists.getValidCards(game.getCardsIn(origin), tgt.getValidTgts(), ai, source, sa);
        list = CardLists.filterControlledBy(list, ai.getOpponents());
        if (list.isEmpty()) {
            return false;
        }
        final Map<Player, Map.Entry<String, Integer>> data = Maps.newHashMap();
        for (final Player opp : ai.getOpponents()) {
            CardCollection oppList = CardLists.filterControlledBy(list, opp);
            if (oppList.isEmpty()) {
                continue;
            }
            final Map<String, Integer> values = ComputerUtilCard.evaluateCreatureListByName(oppList);
            oppList = CardLists.filter(oppList, CardPredicates.isTargetableBy(sa));
            if (oppList.isEmpty()) {
                continue;
            }
            final List<String> toRemove = Lists.newArrayList();
            for (final String name : values.keySet()) {
                if (!oppList.anyMatch(CardPredicates.nameEquals(name))) {
                    toRemove.add(name);
                }
            }
            values.keySet().removeAll(toRemove);
            if (!values.isEmpty()) {
                data.put(opp, Collections.max(values.entrySet(), Map.Entry.comparingByValue()));
            }
        }
        if (data.isEmpty()) {
            return false;
        }
        final Map.Entry<Player, Map.Entry<String, Integer>> max = Collections.max(data.entrySet(),
                Comparator.comparingInt(o -> o.getValue().getValue()));
        final CardCollection named = CardLists.filter(CardLists.filterControlledBy(list, max.getKey()),
                CardPredicates.nameEquals(max.getValue().getKey()), CardPredicates.isTargetableBy(sa));
        for (final Card c : named) {
            sa.getTargets().add(c);
            return true;
        }
        return false;
    }
}
