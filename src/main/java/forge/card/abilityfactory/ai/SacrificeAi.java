package forge.card.abilityfactory.ai;

import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardLists;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class SacrificeAi extends SpellAiLogic {
    // **************************************************************
    // *************************** Sacrifice ***********************
    // **************************************************************

    @Override
    public boolean canPlayAI(Player ai, java.util.Map<String,String> params, SpellAbility sa) {
        boolean chance = sacrificeTgtAI(ai, params, sa);

        // Some additional checks based on what is being sacrificed, and who is
        // sacrificing
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            final String valid = params.get("SacValid");
            String num = params.get("Amount");
            num = (num == null) ? "1" : num;
            final int amount = AbilityFactory.calculateAmount(sa.getSourceCard(), num, sa);

            List<Card> list = 
                    CardLists.getValidCards(ai.getOpponent().getCardsIn(ZoneType.Battlefield), valid.split(","), sa.getActivatingPlayer(), sa.getSourceCard());

            if (list.size() == 0) {
                return false;
            }

            final Card source = sa.getSourceCard();
            if (num.equals("X") && source.getSVar(num).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                final int xPay = Math.min(ComputerUtil.determineLeftoverMana(sa, ai), amount);
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

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player ai) {
        // AI should only activate this during Human's turn
        boolean chance = sacrificeTgtAI(ai, params, sa);

        // TODO: restrict the subAbility a bit

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance;
    }

    @Override
    public boolean doTriggerAINoCost(Player ai, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        // AI should only activate this during Human's turn
        boolean chance = sacrificeTgtAI(ai, params, sa);

        // Improve AI for triggers. If source is a creature with:
        // When ETB, sacrifice a creature. Check to see if the AI has something
        // to sacrifice

        // Eventually, we can call the trigger of ETB abilities with not
        // mandatory as part of the checks to cast something

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            chance &= subAb.chkAIDrawback();
        }

        return chance || mandatory;
    }

    private boolean sacrificeTgtAI(final Player ai, final Map<String, String> params, final SpellAbility sa) {

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

        final String defined = params.get("Defined");
        final String valid = params.get("SacValid");
        if (defined == null) {
            // Self Sacrifice.
        } else if (defined.equals("Each")
                || (defined.equals("Opponent") && !sa.isTrigger())) {
            // If Sacrifice hits both players:
            // Only cast it if Human has the full amount of valid
            // Only cast it if AI doesn't have the full amount of Valid
            // TODO: Cast if the type is favorable: my "worst" valid is
            // worse than his "worst" valid
            final String num = params.containsKey("Amount") ? params.get("Amount") : "1";
            int amount = AbilityFactory.calculateAmount(card, num, sa);

            final Card source = sa.getSourceCard();
            if (num.equals("X") && source.getSVar(num).equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                amount = Math.min(ComputerUtil.determineLeftoverMana(sa, ai), amount);
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
                if (!c.getSVar("SacMe").equals("") || CardFactoryUtil.evaluateCreature(c) <= 135) {
                    return true;
                }
            }
            return false;
        }

        return true;
    }

}