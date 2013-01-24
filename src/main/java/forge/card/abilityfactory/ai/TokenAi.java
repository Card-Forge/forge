package forge.card.abilityfactory.ai;

import java.util.Random;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellAiLogic;
import forge.card.cost.Cost;
import forge.card.cost.CostUtil;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.ai.ComputerUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

/**
 * <p>
 * AbilityFactory_Token class.
 * </p>
 * 
 * @author Forge
 * @version $Id: AbilityFactoryToken.java 17656 2012-10-22 19:32:56Z Max mtg $
 */
public class TokenAi extends SpellAiLogic {


    private String tokenAmount;
    private String tokenName;
    private String[] tokenTypes;
    private String[] tokenKeywords;
    private String tokenPower;
    private String tokenToughness;
    /**
     * <p>
     * Constructor for AbilityFactory_Token.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     */
    private void readParameters(final SpellAbility mapParams) {
        String[] keywords;

        if (mapParams.hasParam("TokenKeywords")) {
            // TODO: Change this Split to a semicolon or something else
            keywords = mapParams.getParam("TokenKeywords").split("<>");
        } else {
            keywords = new String[0];
        }


        this.tokenAmount = mapParams.getParam("TokenAmount");
        this.tokenPower = mapParams.getParam("TokenPower");
        this.tokenToughness = mapParams.getParam("TokenToughness");
        this.tokenName = mapParams.getParam("TokenName");
        this.tokenTypes = mapParams.getParam("TokenTypes").split(",");
        this.tokenKeywords = keywords;

    }

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        readParameters(sa);

        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        Player opp = ai.getOpponent();
        for (final String type : this.tokenTypes) {
            if (type.equals("Legendary")) {
                // Don't kill AIs Legendary tokens
                if (ai.getCardsIn(ZoneType.Battlefield, this.tokenName).size() > 0) {
                    return false;
                }
            }
        }

        boolean haste = false;
        boolean oneShot = false;
        for (final String kw : this.tokenKeywords) {
            if (kw.equals("Haste")) {
                haste = true;
            }
            if (kw.equals("At the beginning of the end step, exile CARDNAME.")
                    || kw.equals("At the beginning of the end step, sacrifice CARDNAME.")) {
                oneShot = true;
            }
        }

        PhaseHandler ph = Singletons.getModel().getGame().getPhaseHandler();
        // Don't generate tokens without haste before main 2 if possible
        if (ph.getPhase().isBefore(PhaseType.MAIN2)
                && ph.isPlayerTurn(ai) && !haste
                && !sa.hasParam("ActivationPhases")) {
            return false;
        }
        if ((ph.isPlayerTurn(ai)
                || ph.getPhase().isBefore(
                        PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY))
                && !sa.hasParam("ActivationPhases") && !sa.hasParam("PlayerTurn")
                && !AbilityFactory.isSorcerySpeed(sa) && !haste) {
            return false;
        }
        if ((ph.getPhase().isAfter(PhaseType.COMBAT_BEGIN) || Singletons.getModel().getGame().getPhaseHandler().isPlayerTurn(
                opp))
                && oneShot) {
            return false;
        }

        // prevent run-away activations - first time will always return true
        final Random r = MyRandom.getRandom();
        final Card source = sa.getSourceCard();

        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                tgt.addTarget(opp);
            } else {
                tgt.addTarget(ai);
            }
        }

        if (cost != null) {
            if (!CostUtil.checkLifeCost(ai, cost, source, 4, null)) {
                return false;
            }

            if (!CostUtil.checkDiscardCost(ai, cost, source)) {
                return false;
            }

            if (!CostUtil.checkSacrificeCost(ai, cost, source)) {
                return false;
            }

            if (!CostUtil.checkRemoveCounterCost(cost, source)) {
                return false;
            }
        }

        if (this.tokenAmount.equals("X") || this.tokenPower.equals("X") || this.tokenToughness.equals("X")) {
            int x = AbilityFactory.calculateAmount(sa.getSourceCard(), this.tokenAmount, sa);
            if (source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                x = ComputerUtil.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(x));
            }
            if (x <= 0) {
                return false;
            }
        }

        if (AbilityFactory.playReusable(ai, sa)) {
            return true;
        }

        if (Singletons.getModel().getGame().getPhaseHandler().is(PhaseType.COMBAT_DECLARE_ATTACKERS_INSTANT_ABILITY)) {
            return true;
        }
        if (sa.isAbility()) {
            return (r.nextFloat() < .9);
        }

        return (r.nextFloat() < .8);
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        readParameters(sa);
        final Card source = sa.getSourceCard();
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgt.resetTargets();
            if (tgt.canOnlyTgtOpponent()) {
                tgt.addTarget(ai.getOpponent());
            } else {
                tgt.addTarget(ai);
            }
        }
        if (this.tokenAmount.equals("X") || this.tokenPower.equals("X") || this.tokenToughness.equals("X")) {
            int x = AbilityFactory.calculateAmount(source, this.tokenAmount, sa);
            if (source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                x = ComputerUtil.determineLeftoverMana(sa, ai);
                source.setSVar("PayX", Integer.toString(x));
            }
            if (x <= 0) {
                return false;
            }
        }

        return true;
    }

}
