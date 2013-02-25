package forge.card.ability.ai;

import java.util.List;
import forge.Card;
import forge.CardLists;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtilCard;
import forge.game.ai.ComputerUtilMana;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class SacrificeAi extends SpellAbilityAi {
    // **************************************************************
    // *************************** Sacrifice ***********************
    // **************************************************************

    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        boolean chance = sacrificeTgtAI(ai, sa);

        // Some additional checks based on what is being sacrificed, and who is
        // sacrificing
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            final String valid = sa.getParam("SacValid");
            String num = sa.getParam("Amount");
            num = (num == null) ? "1" : num;
            final int amount = AbilityUtils.calculateAmount(sa.getSourceCard(), num, sa);

            List<Card> list =
                    CardLists.getValidCards(ai.getOpponent().getCardsIn(ZoneType.Battlefield), valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());

            if (list.size() == 0) {
                return false;
            }

            final Card source = sa.getSourceCard();
            if (num.equals("X") && source.getSVar(num).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int xPay = Math.min(ComputerUtilMana.determineLeftoverMana(sa, ai), amount);
                source.setSVar("PayX", Integer.toString(xPay));
            }

            final int half = (amount / 2) + (amount % 2); // Half of amount
                                                          // rounded up

            // If the Human has at least half rounded up of the amount to be
            // sacrificed, cast the spell
            if (list.size() < half) {
                return false;
            }
        }

        return chance;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        // AI should only activate this during Human's turn

        return sacrificeTgtAI(ai, sa);
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {
        // AI should only activate this during Human's turn
        boolean chance = sacrificeTgtAI(ai, sa);

        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something
        // to sacrifice

        // Eventually, we can call the trigger of ETB abilities with not
        // mandatory as part of the checks to cast something

        return chance || mandatory;
    }

    private boolean sacrificeTgtAI(final Player ai, final SpellAbility sa) {

        final Card card = sa.getSourceCard();
        final Target tgt = sa.getTarget();

        Player opp = ai.getOpponent();
        if (tgt != null) {
            tgt.resetTargets();
            if (opp.canBeTargetedBy(sa)) {
                tgt.addTarget(opp);
                return true;
            } else {
                return false;
            }
        }

        final String defined = sa.getParam("Defined");
        final String valid = sa.getParam("SacValid");
        if (defined == null) {
            // Self Sacrifice.
        } else if (defined.equals("Each")
                || (defined.equals("Opponent") && !sa.isTrigger())) {
            // If Sacrifice hits both players:
            // Only cast it if Human has the full amount of valid
            // Only cast it if AI doesn't have the full amount of Valid
            // TODO: Cast if the type is favorable: my "worst" valid is
            // worse than his "worst" valid
            final String num = sa.hasParam("Amount") ? sa.getParam("Amount") : "1";
            int amount = AbilityUtils.calculateAmount(card, num, sa);

            final Card source = sa.getSourceCard();
            if (num.equals("X") && source.getSVar(num).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                amount = Math.min(ComputerUtilMana.determineLeftoverMana(sa, ai), amount);
            }

            List<Card> humanList =
                    CardLists.getValidCards(opp.getCardsIn(ZoneType.Battlefield), valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());

            // Since all of the cards have remAIDeck:True, I enabled 1 for 1
            // (or X for X) trades for special decks
            if (humanList.size() < amount) {
                return false;
            }
        } else if (defined.equals("You")) {
            List<Card> computerList =
                    CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());
            for (Card c : computerList) {
                if (!c.getSVar("SacMe").equals("") || ComputerUtilCard.evaluateCreature(c) <= 135) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

}
