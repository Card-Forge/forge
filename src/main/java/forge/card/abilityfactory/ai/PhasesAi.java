package forge.card.abilityfactory.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class PhasesAi extends SpellAiLogic
{
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public boolean canPlayAI(Player aiPlayer, Map<String, String> params, SpellAbility sa) {
        // This still needs to be fleshed out
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn() + 1);

        ArrayList<Card> tgtCards;
        if (tgt == null) {
            tgtCards = AbilityFactory.getDefinedCards(source, params.get("Defined"), sa);
            if (tgtCards.contains(source)) {
                // Protect it from something
            } else {
                // Card def = tgtCards.get(0);
                // Phase this out if it might attack me, or before it can be
                // declared as a blocker
            }

            return false;
        } else {
            if (!phasesPrefTargeting(tgt, sa, false)) {
                return false;
            }
        }

        final AbilitySub subAb = sa.getSubAbility();
        if (subAb != null) {
            randomReturn &= subAb.chkAIDrawback();
        }

        return randomReturn;
    }

    /**
     * <p>
     * phasesTrigger.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    
    @Override
    public boolean doTriggerAI(Player aiPlayer, java.util.Map<String,String> params, SpellAbility sa, boolean mandatory) {
        final Target tgt = sa.getTarget();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            return false;
        } else {
            if (phasesPrefTargeting(tgt, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return phasesUnpreferredTargeting(sa, mandatory);
            }
        }

        return false;
    }

    /**
     * <p>
     * phasesPlayDrawbackAI.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    @Override
    public boolean chkAIDrawback(java.util.Map<String,String> params, SpellAbility sa, Player aiPlayer) {
        final Target tgt = sa.getTarget();

        boolean randomReturn = true;

        if (tgt == null) {

        } else {
            if (!phasesPrefTargeting(tgt, sa, false)) {
                return false;
            }
        }
        return randomReturn;
    }

    /**
     * <p>
     * phasesPrefTargeting.
     * </p>
     * 
     * @param tgt
     *            a {@link forge.card.spellability.Target} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean phasesPrefTargeting(final Target tgt, final SpellAbility sa,
            final boolean mandatory) {
        // Card source = sa.getSourceCard();

        // List<Card> phaseList =
        // AllZoneUtil.getCardsIn(Zone.Battlefield).getTargetableCards(source)
        // .getValidCards(tgt.getValidTgts(), source.getController(), source);

        // List<Card> aiPhaseList =
        // phaseList.getController(AllZone.getComputerPlayer());

        // If Something in the Phase List might die from a bad combat, or a
        // spell on the stack save it

        // List<Card> humanPhaseList =
        // phaseList.getController(AllZone.getHumanPlayer());

        // If something in the Human List is causing issues, phase it out

        return false;
    }

    /**
     * <p>
     * phasesUnpreferredTargeting.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param mandatory
     *            a boolean.
     * @return a boolean.
     */
    private boolean phasesUnpreferredTargeting(final SpellAbility sa,
            final boolean mandatory) {
        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();

        List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        list = CardLists.getTargetableCards(CardLists.getValidCards(list, tgt.getValidTgts(), source.getController(), source), sa);

        return false;
    }

    /**
     * <p>
     * phasesStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
}