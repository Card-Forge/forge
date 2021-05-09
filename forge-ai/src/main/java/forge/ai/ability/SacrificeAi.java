package forge.ai.ability;

import java.util.List;
import java.util.Map;

import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class SacrificeAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {

        return sacrificeTgtAI(ai, sa);
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        // AI should only activate this during Human's turn

        return sacrificeTgtAI(ai, sa);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (sa.hasParam("AILogic")) {
            if ("OpponentOnly".equals(sa.getParam("AILogic"))) {
                if (sa.getActivatingPlayer() == ai) {
                	return false;
                }
            }
        }

        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something to sacrifice

        // Eventually, we can call the trigger of ETB abilities with not
        // mandatory as part of the checks to cast something

        return sacrificeTgtAI(ai, sa) || mandatory;
    }

    private boolean sacrificeTgtAI(final Player ai, final SpellAbility sa) {
        final Card source = sa.getHostCard();
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final boolean destroy = sa.hasParam("Destroy");

        Player opp = ai.getStrongestOpponent();

        if (tgt != null) {
            sa.resetTargets();
            if (!opp.canBeTargetedBy(sa)) {
                return false;
            }
            sa.getTargets().add(opp);
            final String valid = sa.getParam("SacValid");
            String num = sa.getParam("Amount");
            num = (num == null) ? "1" : num;
            final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), num, sa);

            List<Card> list = null;
            try {
                list = CardLists.getValidCards(opp.getCardsIn(ZoneType.Battlefield), valid.split(","), sa.getActivatingPlayer(), sa.getHostCard(), sa);
            } catch (NullPointerException e) {
                return false;
            } finally {
                if (list == null)
                    return false;
            }//prevent NPE on MoJhoSto

            for (Card c : list) {
                if (c.hasSVar("SacMe") && Integer.parseInt(c.getSVar("SacMe")) > 3) {
                	return false;
                }
            }
            if (!destroy) {
                list = CardLists.filter(list, CardPredicates.canBeSacrificedBy(sa));
            } else {
                if (!CardLists.getKeyword(list, Keyword.INDESTRUCTIBLE).isEmpty()) {
                    // human can choose to destroy indestructibles
                    return false;
                }
            }

            if (list.isEmpty()) {
                return false;
            }

            if (num.equals("X") && sa.getSVar(num).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                sa.setXManaCostPaid(Math.min(ComputerUtilCost.getMaxXValue(sa, ai), amount));
            }

            final int half = (amount / 2) + (amount % 2); // Half of amount rounded up

            // If the Human has at least half rounded up of the amount to be
            // sacrificed, cast the spell
            if (!sa.isTrigger() && list.size() < half) {
                return false;
            }
        }

        final String defined = sa.getParam("Defined");
        final String valid = sa.getParam("SacValid");
        if (defined == null) {
            // Self Sacrifice.
        } else if (defined.equals("Player")
                || ((defined.equals("Player.Opponent") || defined.equals("Opponent")) && !sa.isTrigger())) {
            // is either "Defined$ Player.Opponent" or "Defined$ Opponent" obsolete?
            
            // If Sacrifice hits both players:
            // Only cast it if Human has the full amount of valid
            // Only cast it if AI doesn't have the full amount of Valid
            // TODO: Cast if the type is favorable: my "worst" valid is worse than his "worst" valid
            final String num = sa.hasParam("Amount") ? sa.getParam("Amount") : "1";
            int amount = AbilityUtils.calculateAmount(source, num, sa);

            if (num.equals("X") && sa.getSVar(num).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                amount = Math.min(ComputerUtilMana.determineLeftoverMana(sa, ai), amount);
            }

            List<Card> humanList = null;
            try {
                humanList = CardLists.getValidCards(opp.getCardsIn(ZoneType.Battlefield), valid.split(","), sa.getActivatingPlayer(), sa.getHostCard(), sa);
            } catch (NullPointerException e) {
                return false;
            } finally {
                if (humanList == null)
                    return false;
            }//prevent NPE on MoJhoSto

            // Since all of the cards have AI:RemoveDeck:All, I enabled 1 for 1
            // (or X for X) trades for special decks
            return humanList.size() >= amount;
        } else if (defined.equals("You")) {

            List<Card> computerList = null;
            try {
                computerList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid.split(","), sa.getActivatingPlayer(), sa.getHostCard(), sa);
            } catch (NullPointerException e) {
                return false;
            } finally {
                if (computerList == null)
                    return false;
            }//prevent NPE on MoJhoSto

            for (Card c : computerList) {
                if (c.hasSVar("SacMe") || ComputerUtilCard.evaluateCreature(c) <= 135) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }

    public static boolean doSacOneEachLogic(Player ai, SpellAbility sa) {
        Game game = ai.getGame();

        sa.resetTargets();
        for (Player p : game.getPlayers()) {
            CardCollection targetable = CardLists.filter(p.getCardsIn(ZoneType.Battlefield), CardPredicates.isTargetableBy(sa));
            if (!targetable.isEmpty()) {
                CardCollection priorityTgts = new CardCollection();
                if (p.isOpponentOf(ai)) {
                    priorityTgts.addAll(CardLists.filter(targetable, CardPredicates.canBeSacrificedBy(sa)));
                    if (!priorityTgts.isEmpty()) {
                        sa.getTargets().add(ComputerUtilCard.getBestAI(priorityTgts));
                    } else {
                        sa.getTargets().add(ComputerUtilCard.getBestAI(targetable));
                    }
                } else {
                    for (Card c : targetable) {
                        if (c.canBeSacrificedBy(sa) && (c.hasSVar("SacMe") || (c.isCreature() && ComputerUtilCard.evaluateCreature(c) <= 135)) && !c.equals(sa.getHostCard())) {
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
        return true;
    }

}
