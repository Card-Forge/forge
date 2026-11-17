package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.PlaceholderTargetAi;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import java.util.List;
import java.util.Map;

public class SacrificeAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        return sacrificeTgtAI(ai, sa, false);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        // AI should only activate this during Human's turn

        return sacrificeTgtAI(ai, sa, sa.isTrigger() && !sa.isOptionalTrigger());
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        AiAbilityDecision decision = sacrificeTgtAI(ai, sa, mandatory);
        if (decision.willingToPlay()) {
            return decision;
        }

        if (mandatory) {
            return new AiAbilityDecision(50, AiPlayDecision.MandatoryPlay);
        }
        return decision;
    }

    private AiAbilityDecision sacrificeTgtAI(final Player ai, final SpellAbility sa, boolean mandatory) {
        final Card source = sa.getHostCard();
        final boolean destroy = sa.hasParam("Destroy");
        final String aiLogic = sa.getParamOrDefault("AILogic", "");
        final String valid = sa.getParamOrDefault("SacValid", "Self");

        if (sa.usesTargeting()) {
            final PlayerCollection targetableOpps = ai.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
            if (targetableOpps.isEmpty()) {
                // TODO also check if own SacMe makes this a reasonable (or even better) choice
                if (mandatory && sa.canTarget(ai)) {
                    sa.resetTargets();
                    sa.getTargets().add(ai);
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
            final Player opp = targetableOpps.max(PlayerPredicates.compareByLife());
            sa.resetTargets();
            sa.getTargets().add(opp);
            if (mandatory) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
            String num = sa.getParamOrDefault("Amount" , "1");
            final int amount = AbilityUtils.calculateAmount(source, num, sa);

            List<Card> list = CardLists.getValidCards(opp.getCardsIn(ZoneType.Battlefield), valid, sa.getActivatingPlayer(), source, sa);

            for (Card c : list) {
                if (c.hasSVar("SacMe") && Integer.parseInt(c.getSVar("SacMe")) > 3) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }
            if (!destroy) {
                list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(sa, true));
            } else {
                if (!CardLists.getKeyword(list, Keyword.INDESTRUCTIBLE).isEmpty()) {
                    // human can choose to destroy indestructibles
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            }

            if (list.isEmpty()) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }

            if (num.equals("X") && sa.getSVar(num).equals("Count$xPaid")) {
                sa.setXManaCostPaid(Math.min(ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger()), amount));
            }

            final int half = (amount / 2) + (amount % 2); // Half of amount rounded up

            // If the Human has at least half rounded up of the amount to be
            // sacrificed, cast the spell
            if (list.size() < half) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        final String defined = sa.getParamOrDefault("Defined", "You");
        final String targeted = sa.getParamOrDefault("ValidTgts", "");
        if (valid.equals("Self")) {
            // Self Sacrifice.
        } else if (defined.equals("Player") || targeted.equals("Player") || targeted.equals("Opponent")
                || ((defined.equals("Player.Opponent") || defined.equals("Opponent")) && !mandatory)) {
            // is either "Defined$ Player.Opponent" or "Defined$ Opponent" obsolete?

            // If Sacrifice hits both players:
            // Only cast it if Human has the full amount of valid
            // Only cast it if AI doesn't have the full amount of Valid
            // TODO: Cast if the type is favorable: my "worst" valid is worse than his "worst" valid
            final String num = sa.getParamOrDefault("Amount", "1");
            int amount = AbilityUtils.calculateAmount(source, num, sa);

            if (num.equals("X") && sa.getSVar(num).equals("Count$xPaid")) {
                amount = Math.min(ComputerUtilCost.setMaxXValue(sa, ai, sa.isTrigger()), amount);
            }

            List<Card> humanList = CardLists.getValidCards(ai.getStrongestOpponent().getCardsIn(ZoneType.Battlefield), valid, sa.getActivatingPlayer(), source, sa);

            // Since all of the cards have AI:RemoveDeck:All, I enabled 1 for 1
            // (or X for X) trades for special decks
            return humanList.size() >= amount ? new AiAbilityDecision(100, AiPlayDecision.WillPlay) : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        } else if (defined.equals("You") && !mandatory) {
            List<Card> computerList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid, sa.getActivatingPlayer(), source, sa);
            for (Card c : computerList) {
                if ("Lethal".equals(aiLogic)) {
                    boolean isLethal = false;
                    for (Player opp : ai.getOpponents()) {
                        if (opp.canLoseLife() && !opp.cantLoseForZeroOrLessLife() && c.getNetPower() >= opp.getLife()) {
                            isLethal = true;
                            break;
                        }
                    }
                    for (Card creature : ai.getOpponents().getCreaturesInPlay()) {
                        if (creature.canBeDestroyed() && c.getNetPower() >= creature.getNetToughness()) {
                            isLethal = true;
                            break;
                        }
                    }

                    if (c.hasSVar("SacMe") || isLethal) {
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                if (c.hasSVar("SacMe") || ComputerUtilCard.evaluateCreature(c) <= 135) {
                    return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                }
            }
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        if ("BirthingRitual".equals(sa.getParam("AILogic"))) {
            return birthingRitualPaysOff(player, sa);
        }
        if (SacrificeWorth.applies(sa)) {
            return SacrificeWorth.confirm(player, sa);
        }
        return !sacrificesOnlyItselfForItsOwnBenefit(player, sa);
    }

    /**
     * "You may sacrifice a creature. If you do, put a +1/+1 counter on CARDNAME" (Crovax the Cursed): when the creature
     * the AI would give up is the source itself, the counter it would get is for a card that is gone. The same goes for
     * any other reward that only the source itself could use.
     */
    private static boolean sacrificesOnlyItselfForItsOwnBenefit(final Player ai, final SpellAbility sa) {
        final Card host = sa.getHostCard();
        if (host == null || !sa.hasParam("Optional") || "Self".equals(sa.getParamOrDefault("SacValid", "Self"))) {
            return false;
        }
        boolean benefitsSource = false;
        for (SpellAbility sub = sa.getSubAbility(); sub != null; sub = sub.getSubAbility()) {
            final String defined = sub.getParam("Defined");
            if (sub.getApi() == forge.game.ability.ApiType.PutCounter && (defined == null || "Self".equals(defined))
                    || sub.getApi() == forge.game.ability.ApiType.Pump && "Self".equals(defined)) {
                benefitsSource = true;
                break;
            }
        }
        if (!benefitsSource) {
            return false;
        }
        final CardCollection options = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield),
                c -> c.isValid(sa.getParam("SacValid"), ai, host, sa) && c.canBeSacrificedBy(sa, true));
        if (options.isEmpty()) {
            return true;
        }
        final CardCollection sac = forge.ai.ComputerUtil.choosePermanentsToSacrifice(ai, options, 1, sa, false, true);
        return sac == null || sac.isEmpty() || sac.get(0) == host;
    }

    /**
     * Birthing Ritual: "look at the top seven, then you may sacrifice a creature; if you do, you may put a creature card
     * with mana value 1 greater than the sacrificed creature's or less from among them onto the battlefield". Sacrificing
     * without a card to put in its place is just throwing a creature away, so only do it when one of the seven cards
     * (the top of the library has not moved yet) is a creature worth more than the creature that is given up.
     */
    public static boolean birthingRitualPaysOff(final Player ai, final SpellAbility sa) {
        final int look = 7;
        final CardCollection top = new CardCollection();
        for (final Card c : ai.getCardsIn(ZoneType.Library)) {
            if (top.size() >= look) {
                break;
            }
            top.add(c);
        }
        final CardCollection mine = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield),
                c -> c.isCreature() && c.canBeSacrificedBy(sa, true));
        // the creature the AI would sacrifice (its least valuable one)
        final CardCollection sac = forge.ai.ComputerUtil.choosePermanentsToSacrifice(ai, mine, 1, sa, false, true);
        if (sac == null || sac.isEmpty()) {
            return false;
        }
        final Card given = sac.get(0);
        final int limit = given.getCMC() + 1;
        final int giveUp = ComputerUtilCard.evaluateCreature(given);
        for (final Card c : top) {
            if (c.isCreature() && c.getCMC() <= limit && ComputerUtilCard.evaluateCreature(c) > giveUp) {
                return true;
            }
        }
        return false;
    }

    public static AiAbilityDecision doSacOneEachLogic(Player ai, SpellAbility sa) {
        Game game = ai.getGame();
        sa.resetTargets();
        for (Player p : game.getPlayers()) {
            CardCollection targetable = CardLists.filter(p.getCardsIn(ZoneType.Battlefield), CardPredicates.isTargetableBy(sa));
            if (!targetable.isEmpty()) {
                CardCollection priorityTgts = new CardCollection();
                if (p.isOpponentOf(ai)) {
                    priorityTgts.addAll(CardLists.filter(targetable, CardPredicates.canBeSacrificedBy(sa, true)));
                    if (!priorityTgts.isEmpty()) {
                        sa.getTargets().add(ComputerUtilCard.getBestAI(priorityTgts));
                    } else {
                        sa.getTargets().add(ComputerUtilCard.getBestAI(targetable));
                    }
                } else {
                    for (Card c : targetable) {
                        if (c.canBeSacrificedBy(sa, true) && (c.hasSVar("SacMe") || (c.isCreature() && ComputerUtilCard.evaluateCreature(c) <= 135)) && !c.equals(sa.getHostCard())) {
                            priorityTgts.add(c);
                        }
                    }
                    if (!priorityTgts.isEmpty()) {
                        sa.getTargets().add(ComputerUtilCard.getWorstPermanentAI(priorityTgts, false, false, false, false));
                    } else {
                        targetable.remove(sa.getHostCard());
                        if (!targetable.isEmpty()) {
                            sa.getTargets().add(ComputerUtilCard.getWorstPermanentAI(targetable, true, true, true, false));
                        } else {
                            sa.getTargets().add(sa.getHostCard()); // sac self only as a last resort
                        }
                    }
                }
            }
        }
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean willPayUnlessCost(Player payer, SpellAbility sa, Cost cost, boolean alreadyPaid, FCollectionView<Player> payers) {
        if ("PayIfBigger".equals(sa.getParam("AILogic"))) {
            return !alreadyPaid && PlaceholderTargetAi.worthPaying(payer, sa, cost);
        }
        // Icy Prison
        if (payers.size() > 1) {
            final Player p = sa.getActivatingPlayer();
            // not me or team mate
            if (!p.sameTeam(payer)) {
                return false;
            }
        }

        // don't keep paying for an "exile until leaves" permanent that no longer exiles anything (e.g. Static Prison)
        final Card source = sa.getHostCard();
        if (source.hasSVar("OblivionRing") && "Self".equals(sa.getParamOrDefault("SacValid", "Self"))
                && source.getUntilLeavesBattlefield().isEmpty()) {
            return false;
        }

        return super.willPayUnlessCost(payer, sa, cost, alreadyPaid, payers);
    }
}
